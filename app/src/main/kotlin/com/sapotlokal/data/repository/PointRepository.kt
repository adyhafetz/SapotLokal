package com.sapotlokal.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.sapotlokal.AppConfig
import com.sapotlokal.AppConfig.CustomerTier
import com.sapotlokal.data.model.Game
import com.sapotlokal.data.model.Point
import com.sapotlokal.data.model.PointSource
import com.sapotlokal.data.model.XpEventKey
import com.sapotlokal.data.model.XpSource
import java.time.LocalDate
import java.time.ZoneId

/**
 * PointRepository — all reads and writes to the /points node in Firebase Realtime Database.
 *
 * WHY A REPOSITORY: The build guide (Rule 9) says every points change is a ledger row.
 * Centralising those writes here means every screen uses the same logic — no duplicated
 * Firebase paths or business rules scattered across Activities.
 *
 * DUAL LEDGER ARCHITECTURE (Phase 1):
 * - Lifetime Points: Sum of all earned points (never decreased by redemptions). Determines Tier.
 * - Spendable Balance: Total earned minus total redeemed. Used for voucher claims.
 * - Tier Multiplier: Applied dynamically to base points upon earning.
 */
object PointRepository {

    private val db = com.google.firebase.database.FirebaseDatabase
        .getInstance(AppConfig.DATABASE_URL)
        .reference

    // ── Write ──────────────────────────────────────────────────────────────────

    /** Game points belong to no shop. See docs/erd-changes.md Change 5. */
    const val NO_VENDOR_ID = ""

    /** Builds (but does not write) a ledger row for game points. Returns null if no push key could be made. */
    fun newGamePointEntry(userID: String, source: String, amount: Int): Point? {
        val key = db.child("points").push().key ?: return null
        return Point(
            pointID             = key,
            userID              = userID,
            vendorID            = NO_VENDOR_ID,
            pointEarnedAmount   = amount,
            pointRedeemedAmount = 0,
            pointSource         = source
        )
    }

    /**
     * Atomically awards points with the user's current tier multiplier:
     * 1. Multiplies base points by user tier multiplier: (base * multiplier).toInt()
     * 2. Appends an earned row to /points
     * 3. Atomically increments /users/{userID}/lifetimePoints via ServerValue.increment
     * 4. Updates /users/{userID}/currentTier
     */
    fun addPoints(
        userID: String,
        basePoints: Int,
        pointSource: String,
        vendorID: String = NO_VENDOR_ID,
        extraUpdates: Map<String, Any?> = emptyMap(),
        applyMultiplier: Boolean = true,
        onResult: (success: Boolean, earnedPoints: Int) -> Unit = { _, _ -> }
    ) {
        if (userID.isBlank()) {
            onResult(false, 0)
            return
        }
        if (basePoints <= 0) {
            onResult(true, 0)
            return
        }

        db.child("users").child(userID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentLifetime = snapshot.child("lifetimePoints").getValue(Long::class.java)?.toInt() ?: 0
                val tier = CustomerTier.fromLifetimePoints(currentLifetime)
                val earnedPoints = if (applyMultiplier) (basePoints * tier.multiplier).toInt().coerceAtLeast(1) else basePoints
                val newLifetime = currentLifetime + earnedPoints
                val newTier = CustomerTier.fromLifetimePoints(newLifetime)

                val pointRef = db.child("points").push()
                val pointID = pointRef.key ?: run {
                    onResult(false, 0)
                    return
                }

                val pointRow = Point(
                    pointID            = pointID,
                    userID             = userID,
                    vendorID           = vendorID,
                    pointEarnedAmount  = earnedPoints,
                    pointRedeemedAmount = 0,
                    pointSource        = pointSource,
                    timestamp          = System.currentTimeMillis()
                )

                // extraUpdates (e.g. a "claimed" flag) ride in the SAME atomic write, so a reward
                // can never be marked claimed without its points being recorded (or vice-versa).
                val updates = hashMapOf<String, Any?>(
                    "points/$pointID" to pointRow,
                    "users/$userID/lifetimePoints" to ServerValue.increment(earnedPoints.toLong()),
                    "users/$userID/currentTier" to newTier.code
                )
                updates.putAll(extraUpdates)

                db.updateChildren(updates)
                    .addOnSuccessListener {
                        onResult(true, earnedPoints)
                    }
                    .addOnFailureListener {
                        onResult(false, 0)
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(false, 0)
            }
        })
    }

    /** What one valid scan gave: shop points (with the rank bonus), XP, and the rank after it. */
    data class VisitResult(
        val points: Int,
        val basePoints: Int,
        val rankBonus: Double,
        val xp: Int,
        val rankBefore: AppConfig.Rank,
        val rankAfter: AppConfig.Rank,
        val badgesUnlocked: List<com.sapotlokal.data.model.BadgeDef> = emptyList()
    )

    /**
     * Records one shop visit in ONE atomic write (REFRACTOR.md §4.1):
     *   points/{id}                     shop points = shop's pointsPerVisit × the customer's rank bonus
     *   xpEvents + users/xp + users/rank AppConfig.VISIT_XP, flat (see XpRepository.awardPaths)
     *   userStats/{uid}                 totalVisits +1, shopsVisited/{vendorID}, lastVisitAt
     *   weeklyVisits/{week}/{uid}       visits +1 for the leaderboard
     * The caller checks the cooldown first (hasRecentShopVisit). [onResult] gets null on failure.
     */
    fun recordShopVisit(
        userID: String,
        vendorID: String,
        onResult: (VisitResult?) -> Unit
    ) {
        if (userID.isBlank() || vendorID.isBlank()) return onResult(null)
        db.child("users").child(userID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(user: DataSnapshot) {
                db.child("vendors").child(vendorID).child("pointsPerVisit")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(rate: DataSnapshot) {
                            val perVisit = rate.getValue(Long::class.java)?.toInt()
                                ?.takeIf { it in AppConfig.ALLOWED_POINTS_PER_VISIT }
                                ?: AppConfig.DEFAULT_POINTS_PER_VISIT
                            BadgeRepository.readContext(userID) { stats, badges ->
                                writeVisit(userID, vendorID, perVisit, user, stats, badges, onResult)
                            }
                        }
                        override fun onCancelled(error: DatabaseError) = onResult(null)
                    })
            }
            override fun onCancelled(error: DatabaseError) = onResult(null)
        })
    }

    private fun writeVisit(
        userID: String,
        vendorID: String,
        perVisit: Int,
        user: DataSnapshot,
        stats: DataSnapshot?,
        badges: DataSnapshot?,
        onResult: (VisitResult?) -> Unit
    ) {
        val pointID = db.child("points").push().key ?: return onResult(null)
        val now = System.currentTimeMillis()
        val standing = XpRepository.standingOf(user)
        val rank = standing.rank
        val points = AppConfig.shopPointsForVisit(perVisit, rank)
        val (xpPaths, rankAfter) = XpRepository.awardPaths(
            uid = userID,
            eventKey = XpEventKey.visit(pointID),
            amount = AppConfig.VISIT_XP,
            source = XpSource.VISIT,
            current = standing,
            now = now
        )
        val week = com.sapotlokal.util.MissionDateUtils.getWeekYearKey(now)
        val updates = hashMapOf<String, Any>(
            "points/$pointID" to Point(
                pointID = pointID,
                userID = userID,
                vendorID = vendorID,
                pointEarnedAmount = points,
                pointRedeemedAmount = 0,
                pointSource = PointSource.SHOP_VISIT,
                timestamp = now,
                basePoints = perVisit,
                rankBonus = rank.pointsBonus
            ),
            "userStats/$userID/totalVisits" to ServerValue.increment(1),
            "userStats/$userID/shopsVisited/$vendorID" to true,
            "userStats/$userID/lastVisitAt" to now,
            "weeklyVisits/$week/$userID/visits" to ServerValue.increment(1),
            "weeklyVisits/$week/$userID/lastVisitAt" to now,
            "weeklyVisits/$week/$userID/userName" to (user.child("userName").getValue(String::class.java) ?: "").take(60),
            "weeklyVisits/$week/$userID/avatar" to (user.child("avatar").getValue(String::class.java) ?: ""),
            "weeklyVisits/$week/$userID/rank" to rankAfter.level
        )
        updates.putAll(xpPaths)
        updates.putAll(MissionRepository.visitPaths(userID, vendorID, week))
        val (badgePaths, unlocked) = BadgeRepository.visitPaths(userID, vendorID, now, stats, badges)
        updates.putAll(badgePaths)

        db.updateChildren(updates)
            .addOnSuccessListener {
                onResult(VisitResult(points, perVisit, rank.pointsBonus, AppConfig.VISIT_XP, rank, rankAfter, unlocked))
            }
            .addOnFailureListener { onResult(null) }
    }

    /**
     * True when [userID] already earned a shop-visit reward at [vendorID] inside the cooldown
     * window (AppConfig.SHOP_VISIT_COOLDOWN_MINUTES), judged from the live ledger.
     * A cooldown of 0 disables the check (useful while testing).
     */
    fun hasRecentShopVisit(
        userID: String,
        vendorID: String,
        onResult: (recent: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        val cooldownMs = AppConfig.SHOP_VISIT_COOLDOWN_MINUTES * 60_000L
        if (cooldownMs <= 0L) {
            onResult(false)
            return
        }
        db.child("points").orderByChild("userID").equalTo(userID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val cutoff = System.currentTimeMillis() - cooldownMs
                    val recent = snapshot.children.any { child ->
                        val p = child.getValue(Point::class.java) ?: return@any false
                        p.pointSource == PointSource.SHOP_VISIT &&
                            p.vendorID == vendorID &&
                            p.timestamp >= cutoff
                    }
                    onResult(recent)
                }

                override fun onCancelled(error: DatabaseError) {
                    onError(error.message)
                }
            })
    }

    /**
     * Atomically records a mini-game play and awards points with tier multiplier.
     */
    fun awardMiniGamePoints(
        userID: String,
        basePoints: Int,
        gameType: String,
        attemptNumber: Int,
        maxEarnedPoints: Int = Int.MAX_VALUE,
        badgeKind: BadgeRepository.GameKind? = null,
        topSegment: Boolean = false,
        perfect: Boolean = false,
        onResult: (success: Boolean, earnedPoints: Int) -> Unit
    ) {
        if (userID.isBlank()) {
            onResult(false, 0)
            return
        }
        if (badgeKind == null) {
            writeGame(userID, basePoints, gameType, attemptNumber, maxEarnedPoints, emptyMap(), onResult)
            return
        }
        // Badges a play can earn (Nasib Baik, Otak Cerdas, Combo) ride in the same write.
        BadgeRepository.readContext(userID) { stats, badges ->
            val (paths, _) = BadgeRepository.gamePaths(userID, badgeKind, System.currentTimeMillis(), stats, badges, topSegment, perfect)
            writeGame(userID, basePoints, gameType, attemptNumber, maxEarnedPoints, paths, onResult)
        }
    }

    private fun writeGame(
        userID: String,
        basePoints: Int,
        gameType: String,
        attemptNumber: Int,
        maxEarnedPoints: Int,
        badgePaths: Map<String, Any>,
        onResult: (success: Boolean, earnedPoints: Int) -> Unit
    ) {

        val todayKey = LocalDate.now(ZoneId.of(AppConfig.GAMES_RESET_TIME_ZONE)).toString()
        val gameID = db.child("games").push().key ?: run {
            onResult(false, 0)
            return
        }

        if (basePoints <= 0) {
            val game = Game(
                gameID = gameID,
                userID = userID,
                gameType = gameType,
                gameAttempt = attemptNumber,
                gamePointsRewarded = 0,
                gameDay = todayKey,
                gamePlayedAt = System.currentTimeMillis()
            )
            val week = com.sapotlokal.util.MissionDateUtils.getWeekYearKey()
            val updates = hashMapOf<String, Any>("games/$gameID" to game)
            updates.putAll(MissionRepository.gamePaths(userID, todayKey, week))
            updates.putAll(badgePaths)
            db.updateChildren(updates)
                .addOnSuccessListener { onResult(true, 0) }
                .addOnFailureListener { onResult(false, 0) }
            return
        }

        db.child("users").child(userID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentLifetime = snapshot.child("lifetimePoints").getValue(Long::class.java)?.toInt() ?: 0
                val tier = CustomerTier.fromLifetimePoints(currentLifetime)
                // maxEarnedPoints enforces the daily cap AFTER the tier multiplier is applied.
                val earnedPoints = (basePoints * tier.multiplier).toInt().coerceAtLeast(1)
                    .coerceAtMost(maxEarnedPoints.coerceAtLeast(1))
                val newLifetime = currentLifetime + earnedPoints
                val newTier = CustomerTier.fromLifetimePoints(newLifetime)

                val pointID = db.child("points").push().key ?: run {
                    onResult(false, 0)
                    return
                }

                val game = Game(
                    gameID = gameID,
                    userID = userID,
                    gameType = gameType,
                    gameAttempt = attemptNumber,
                    gamePointsRewarded = earnedPoints,
                    gameDay = todayKey,
                    gamePlayedAt = System.currentTimeMillis()
                )

                val pointRow = Point(
                    pointID            = pointID,
                    userID             = userID,
                    vendorID           = NO_VENDOR_ID,
                    pointEarnedAmount  = earnedPoints,
                    pointRedeemedAmount = 0,
                    pointSource        = when (gameType) {
                        PointSource.SPIN_WHEEL -> PointSource.SPIN_WHEEL
                        else -> PointSource.MEMORY_MATCH
                    },
                    timestamp          = System.currentTimeMillis()
                )

                val updates = hashMapOf<String, Any?>(
                    "games/$gameID" to game,
                    "points/$pointID" to pointRow,
                    "users/$userID/lifetimePoints" to ServerValue.increment(earnedPoints.toLong()),
                    "users/$userID/currentTier" to newTier.code
                )
                updates.putAll(MissionRepository.gamePaths(userID, todayKey, com.sapotlokal.util.MissionDateUtils.getWeekYearKey()))
                updates.putAll(badgePaths)

                db.updateChildren(updates)
                    .addOnSuccessListener { onResult(true, earnedPoints) }
                    .addOnFailureListener {
                        onResult(false, 0)
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(false, 0)
            }
        })
    }


    // ── Read — one-shot & live ──────────────────────────────────────────────────

    /**
     * Reads all point rows for [userID] once and calls back with:
     *   - perShopBalance: Map<vendorID, balance>
     *   - lifetimeEarned: Int — used for tier progression
     *   - spendableBalance: Int — available points for redemption
     */
    fun fetchAllPointsForUser(
        userID: String,
        onResult: (perShopBalance: Map<String, Int>, lifetimeEarned: Int, spendableBalance: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        db.child("points")
            .orderByChild("userID")
            .equalTo(userID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val perShop = mutableMapOf<String, Int>()
                    var lifetimeEarned = 0
                    var totalRedeemed = 0

                    for (child in snapshot.children) {
                        val p = child.getValue(Point::class.java) ?: continue
                        lifetimeEarned += p.pointEarnedAmount
                        totalRedeemed += p.pointRedeemedAmount
                        if (p.vendorID.isNotBlank()) {
                            val current = perShop.getOrDefault(p.vendorID, 0)
                            perShop[p.vendorID] =
                                current + p.pointEarnedAmount - p.pointRedeemedAmount
                        }
                    }

                    val spendableBalance = (lifetimeEarned - totalRedeemed).coerceAtLeast(0)
                    onResult(perShop, lifetimeEarned, spendableBalance)
                }

                override fun onCancelled(error: DatabaseError) {
                    onError(error.message)
                }
            })
    }

    fun fetchAllPointsForUser(
        userID: String,
        onResult: (perShopBalance: Map<String, Int>, lifetimeEarned: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        fetchAllPointsForUser(
            userID = userID,
            onResult = { perShop, lifetime, _ -> onResult(perShop, lifetime) },
            onError = onError
        )
    }

    /**
     * Reads all point rows for [userID] and streams live updates.
     * Used by CustomerHomeActivity so the balances update in real-time.
     */
    fun listenToPointsForUser(
        userID: String,
        onUpdate: (perShopBalance: Map<String, Int>, lifetimeEarned: Int, spendableBalance: Int) -> Unit,
        onError: (String) -> Unit,
        onWeekly: ((earnedThisWeek: Int) -> Unit)? = null
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val perShop = mutableMapOf<String, Int>()
                var lifetimeEarned = 0
                var weekly = 0
                val weekStart = com.sapotlokal.util.MissionDateUtils.getWeekStartMillis()

                var totalRedeemed = 0

                for (child in snapshot.children) {
                    val p = child.getValue(Point::class.java) ?: continue
                    if (p.timestamp >= weekStart) weekly += p.pointEarnedAmount
                    lifetimeEarned += p.pointEarnedAmount
                    totalRedeemed += p.pointRedeemedAmount
                    if (p.vendorID.isNotBlank()) {
                        val current = perShop.getOrDefault(p.vendorID, 0)
                        perShop[p.vendorID] =
                            current + p.pointEarnedAmount - p.pointRedeemedAmount
                    }
                }

                val spendableBalance = (lifetimeEarned - totalRedeemed).coerceAtLeast(0)
                onWeekly?.invoke(weekly)
                onUpdate(perShop, lifetimeEarned, spendableBalance)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }

        db.child("points")
            .orderByChild("userID")
            .equalTo(userID)
            .addValueEventListener(listener)

        return listener
    }

    fun listenToPointsForUser(
        userID: String,
        onUpdate: (perShopBalance: Map<String, Int>, lifetimeEarned: Int) -> Unit,
        onError: (String) -> Unit
    ): ValueEventListener {
        return listenToPointsForUser(
            userID = userID,
            onUpdate = { perShop, lifetime, _ -> onUpdate(perShop, lifetime) },
            onError = onError
        )
    }

    /**
     * Removes a live listener previously returned by [listenToPointsForUser].
     * Call this in the Activity's onStop() to stop unnecessary reads.
     */
    fun removePointsListener(userID: String, listener: ValueEventListener) {
        db.child("points")
            .orderByChild("userID")
            .equalTo(userID)
            .removeEventListener(listener)
    }
}
