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

    /** Builds (but does not write) a ledger row for challenge points. */
    fun newChallengePointEntry(userID: String, amount: Int): Point? {
        val key = db.child("points").push().key ?: return null
        return Point(
            pointID             = key,
            userID              = userID,
            vendorID            = NO_VENDOR_ID,
            pointEarnedAmount   = amount,
            pointRedeemedAmount = 0,
            pointSource         = PointSource.CHALLENGE
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
                val earnedPoints = (basePoints * tier.multiplier).toInt().coerceAtLeast(1)
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
                    pointSource        = pointSource
                )

                val updates = hashMapOf<String, Any>(
                    "points/$pointID" to pointRow,
                    "users/$userID/lifetimePoints" to ServerValue.increment(earnedPoints.toLong()),
                    "users/$userID/currentTier" to newTier.code
                )

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

    /**
     * Appends a new earned-points ledger row for a shop visit with tier multiplier.
     * Called by ScanActivity after a QR code is validated.
     */
    fun recordShopVisit(
        userID: String,
        vendorID: String,
        onResult: (success: Boolean) -> Unit
    ) {
        addPoints(
            userID = userID,
            basePoints = AppConfig.POINTS_PER_SHOP_VISIT,
            pointSource = PointSource.SHOP_VISIT,
            vendorID = vendorID
        ) { success, _ ->
            onResult(success)
        }
    }

    /**
     * Atomically records a mini-game play and awards points with tier multiplier.
     */
    fun awardMiniGamePoints(
        userID: String,
        basePoints: Int,
        gameType: String,
        attemptNumber: Int,
        onResult: (success: Boolean, earnedPoints: Int) -> Unit
    ) {
        if (userID.isBlank()) {
            onResult(false, 0)
            return
        }

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
            db.child("games").child(gameID).setValue(game)
                .addOnSuccessListener {
                    ChallengeRepository.syncUserProgress(userID)
                    onResult(true, 0)
                }
                .addOnFailureListener { onResult(false, 0) }
            return
        }

        db.child("users").child(userID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentLifetime = snapshot.child("lifetimePoints").getValue(Long::class.java)?.toInt() ?: 0
                val tier = CustomerTier.fromLifetimePoints(currentLifetime)
                val earnedPoints = (basePoints * tier.multiplier).toInt().coerceAtLeast(1)
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
                    }
                )

                val updates = hashMapOf<String, Any?>(
                    "games/$gameID" to game,
                    "points/$pointID" to pointRow,
                    "users/$userID/lifetimePoints" to ServerValue.increment(earnedPoints.toLong()),
                    "users/$userID/currentTier" to newTier.code
                )

                db.updateChildren(updates)
                    .addOnSuccessListener {
                        ChallengeRepository.syncUserProgress(userID)
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

    /**
     * Claims a mission or challenge reward with tier multiplier.
     */
    fun claimMissionReward(
        userID: String,
        basePoints: Int,
        missionId: String,
        onResult: (success: Boolean, earnedPoints: Int) -> Unit = { _, _ -> }
    ) {
        addPoints(
            userID = userID,
            basePoints = basePoints,
            pointSource = PointSource.CHALLENGE,
            vendorID = NO_VENDOR_ID,
            onResult = onResult
        )
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
        onError: (String) -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {
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
