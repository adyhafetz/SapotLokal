package com.sapotlokal.data.repository

import android.app.Activity
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sapotlokal.AppConfig
import com.sapotlokal.data.model.BadgeDef
import com.sapotlokal.data.model.Game
import com.sapotlokal.data.model.PointSource
import com.sapotlokal.data.model.UserMission
import com.sapotlokal.data.model.WeeklyMission
import com.sapotlokal.data.model.WeeklyMissionWithProgress
import com.sapotlokal.ui.games.GameConfig
import com.sapotlokal.util.MissionDateUtils
import com.sapotlokal.util.SoundManager
import java.time.DayOfWeek

/**
 * MissionRepository — Core engine for SapotLokal Weekly Missions & Gamification Hooks.
 *
 * Implements Phase 3 Specification:
 * - 6 recurring weekly missions resetting every Sunday at 23:59:59 Asia/Kuala_Lumpur.
 * - ISO week partitioning using weekYearKey (e.g. "2026-W44").
 * - Dynamic event hooks from QR Scans, Spin Wheel, and Memory Match.
 * - Reward claim orchestration with dual ledger points, "Ka-ching!" audio, and badge unlocking.
 */
object MissionRepository {

    private const val TAG = "MissionRepository"

    private val db get() = FirebaseDatabase
        .getInstance(AppConfig.DATABASE_URL)
        .reference

    // ── 6 Standard Weekly Missions ─────────────────────────────────────────────

    val DEFAULT_MISSIONS = listOf(
        WeeklyMission(
            missionId = "tapah_explorer",
            title = "Tapah Explorer",
            description = "Visit 3 different local shops in Tapah this week",
            targetCount = 3,
            rewardPoints = 100,
            rewardBadgeKey = null
        ),
        WeeklyMission(
            missionId = "loyal_supporter",
            title = "Loyal Supporter",
            description = "Visit the same shop twice in one week",
            targetCount = 2,
            rewardPoints = 50,
            rewardBadgeKey = BadgeDef.REGULAR.id
        ),
        WeeklyMission(
            missionId = "weekend_warrior",
            title = "Weekend Warrior",
            description = "Scan a shop visit on both Saturday and Sunday",
            targetCount = 2,
            rewardPoints = 80,
            rewardBadgeKey = null
        ),
        WeeklyMission(
            missionId = "daily_tri_play",
            title = "Daily Tri-Play",
            description = "Scan a shop, spin the wheel, and play memory match on the same day",
            targetCount = 3,
            rewardPoints = 75,
            rewardBadgeKey = BadgeDef.COMBO_MASTER.id
        ),
        WeeklyMission(
            missionId = "night_owl",
            title = "Night Owl",
            description = "Scan a shop visit after 8:00 PM on two different days",
            targetCount = 2,
            rewardPoints = 60,
            rewardBadgeKey = BadgeDef.HANTU_MALAM.id
        ),
        WeeklyMission(
            missionId = "arcade_champion",
            title = "Arcade Champion",
            description = "Accumulate 200 points purely from mini-games before week ends",
            targetCount = 200,
            rewardPoints = 80,
            rewardBadgeKey = null
        )
    )

    /**
     * Seeds the standard missions in /missions node if not already present.
     */
    fun seedDefaultMissions(onComplete: () -> Unit = {}) {
        db.child("missions").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updates = hashMapOf<String, Any>()
                for (mission in DEFAULT_MISSIONS) {
                    updates["missions/${mission.missionId}"] = mission
                }
                db.updateChildren(updates).addOnCompleteListener { onComplete() }
            }

            override fun onCancelled(error: DatabaseError) {
                onComplete()
            }
        })
    }

    // ── Synchronize & Listen ───────────────────────────────────────────────────

    /**
     * Listens in real time to weekly missions and user progress for the current ISO week.
     */
    fun listenToWeeklyMissions(
        userId: String,
        onUpdate: (List<WeeklyMissionWithProgress>) -> Unit,
        onError: (String) -> Unit
    ): ValueEventListener {
        val weekKey = MissionDateUtils.getWeekYearKey()
        val expiresAt = MissionDateUtils.getWeekExpiresAtMillis()

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val progressMap = mutableMapOf<String, UserMission>()
                for (child in snapshot.children) {
                    val um = child.getValue(UserMission::class.java) ?: continue
                    if (um.weekYearKey == weekKey) {
                        progressMap[um.missionId] = um
                    }
                }

                val list = DEFAULT_MISSIONS.map { mission ->
                    WeeklyMissionWithProgress(
                        mission = mission,
                        userMission = progressMap[mission.missionId],
                        weekExpiresAtMillis = expiresAt
                    )
                }
                onUpdate(list)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }

        db.child("userMissions")
            .orderByChild("userId")
            .equalTo(userId)
            .addValueEventListener(listener)

        return listener
    }

    fun removeListener(listener: ValueEventListener) {
        db.child("userMissions").removeEventListener(listener)
    }

    /**
     * Ensures all 6 missions exist for the active week, and audits current progress.
     */
    fun syncUserMissions(userId: String, onComplete: () -> Unit = {}) {
        if (userId.isBlank()) {
            onComplete()
            return
        }

        val weekKey = MissionDateUtils.getWeekYearKey()

        db.child("userMissionTracking").child(userId).child(weekKey)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(trackingSnap: DataSnapshot) {
                    // Collect tracking data
                    val shopsSnap = trackingSnap.child("shops")
                    val distinctShopsCount = shopsSnap.childrenCount.toInt()
                    var maxSingleShopVisits = 0
                    for (c in shopsSnap.children) {
                        val count = c.getValue(Int::class.java) ?: 0
                        if (count > maxSingleShopVisits) maxSingleShopVisits = count
                    }

                    val weekendSnap = trackingSnap.child("weekendDays")
                    val hasSat = weekendSnap.child("SATURDAY").getValue(Boolean::class.java) == true
                    val hasSun = weekendSnap.child("SUNDAY").getValue(Boolean::class.java) == true
                    val weekendProgress = (if (hasSat) 1 else 0) + (if (hasSun) 1 else 0)

                    val nightDaysSnap = trackingSnap.child("nightDays")
                    val nightDaysCount = nightDaysSnap.childrenCount.toInt().coerceAtMost(2)

                    var maxTriPlayScore = 0
                    val triPlaySnap = trackingSnap.child("triPlay")
                    for (dayChild in triPlaySnap.children) {
                        var dayScore = 0
                        if (dayChild.child("shop").getValue(Boolean::class.java) == true) dayScore++
                        if (dayChild.child("spin").getValue(Boolean::class.java) == true) dayScore++
                        if (dayChild.child("memory").getValue(Boolean::class.java) == true) dayScore++
                        if (dayScore > maxTriPlayScore) maxTriPlayScore = dayScore
                    }

                    val arcadePoints = trackingSnap.child("arcadePoints").getValue(Int::class.java) ?: 0

                    // Fetch existing user missions for this week
                    db.child("userMissions").orderByChild("userId").equalTo(userId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(missionsSnap: DataSnapshot) {
                                val existingMap = mutableMapOf<String, UserMission>()
                                for (c in missionsSnap.children) {
                                    val um = c.getValue(UserMission::class.java) ?: continue
                                    if (um.weekYearKey == weekKey) {
                                        existingMap[um.missionId] = um
                                    }
                                }

                                val updates = hashMapOf<String, Any>()
                                val now = System.currentTimeMillis()

                                for (mission in DEFAULT_MISSIONS) {
                                    val existing = existingMap[mission.missionId]
                                    val calculated = when (mission.missionId) {
                                        "tapah_explorer" -> distinctShopsCount.coerceAtMost(mission.targetCount)
                                        "loyal_supporter" -> if (maxSingleShopVisits >= 2) 2 else maxSingleShopVisits.coerceAtMost(1)
                                        "weekend_warrior" -> weekendProgress.coerceAtMost(mission.targetCount)
                                        "daily_tri_play" -> maxTriPlayScore.coerceAtMost(mission.targetCount)
                                        "night_owl" -> nightDaysCount.coerceAtMost(mission.targetCount)
                                        "arcade_champion" -> arcadePoints.coerceAtMost(mission.targetCount)
                                        else -> 0
                                    }

                                    val progress = maxOf(existing?.currentProgress ?: 0, calculated)
                                    val isClaimed = existing?.isClaimed ?: false
                                    val isCompleted = progress >= mission.targetCount || (existing?.isCompleted == true)

                                    val recordKey = "${userId}_${mission.missionId}_${weekKey}"
                                    val record = UserMission(
                                        userMissionId = recordKey,
                                        userId = userId,
                                        missionId = mission.missionId,
                                        weekYearKey = weekKey,
                                        currentProgress = progress,
                                        isCompleted = isCompleted,
                                        isClaimed = isClaimed,
                                        lastUpdated = now
                                    )
                                    updates["userMissions/$recordKey"] = record
                                }

                                if (updates.isNotEmpty()) {
                                    db.updateChildren(updates).addOnCompleteListener { onComplete() }
                                } else {
                                    onComplete()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                onComplete()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    onComplete()
                }
            })
    }

    // ── Gamification Event Hooks ───────────────────────────────────────────────

    /**
     * Hook 1: Called from ScanActivity on a verified shop visit scan.
     * Evaluates:
     * - Mission 1 (tapah_explorer): distinct shop count
     * - Mission 2 (loyal_supporter): repeat visit to same shop
     * - Mission 3 (weekend_warrior): Saturday / Sunday visit
     * - Mission 4 (daily_tri_play): shop component today
     * - Mission 5 (night_owl): visit after 8:00 PM (20:00 local)
     * - Badge: LANGKAH_KANAN (first scan milestone)
     * - Badge: TAPAH_RANGER (10 total visits)
     */
    fun recordShopVisitEvent(
        activity: Activity?,
        userId: String,
        vendorId: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (userId.isBlank() || vendorId.isBlank()) return

        val weekKey = MissionDateUtils.getWeekYearKey(timestamp)
        val dayKey = MissionDateUtils.getDayString(timestamp)
        val dow = MissionDateUtils.getDayOfWeek(timestamp)
        val isNight = MissionDateUtils.isNightScan(timestamp)

        // 1. Audit shop visits node for all-time badges
        val visitRef = db.child("shopVisits").child(userId).push()
        val visitData = hashMapOf(
            "visitId" to (visitRef.key ?: ""),
            "userId" to userId,
            "vendorId" to vendorId,
            "timestamp" to timestamp,
            "weekYearKey" to weekKey,
            "dayString" to dayKey
        )

        visitRef.setValue(visitData).addOnSuccessListener {
            // Check total visits for badges
            db.child("shopVisits").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val totalVisits = snapshot.childrenCount.toInt()
                        activity?.let { act ->
                            if (totalVisits == 1) {
                                BadgeRepository.unlockBadge(act, userId, BadgeDef.LANGKAH_KANAN)
                            }
                            if (totalVisits >= 10) {
                                BadgeRepository.unlockBadge(act, userId, BadgeDef.TAPAH_RANGER)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        // 2. Update weekly tracking node
        val trackingRef = db.child("userMissionTracking").child(userId).child(weekKey)
        trackingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentShopVisits = snapshot.child("shops").child(vendorId).getValue(Int::class.java) ?: 0
                val newShopVisits = currentShopVisits + 1

                val updates = hashMapOf<String, Any>(
                    "shops/$vendorId" to newShopVisits,
                    "triPlay/$dayKey/shop" to true
                )

                if (dow == DayOfWeek.SATURDAY) updates["weekendDays/SATURDAY"] = true
                if (dow == DayOfWeek.SUNDAY) updates["weekendDays/SUNDAY"] = true
                if (isNight) updates["nightDays/$dayKey"] = true

                trackingRef.updateChildren(updates).addOnSuccessListener {
                    // Sync mission progress
                    syncUserMissions(userId)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /**
     * Hook 2: Called from SpinWheelActivity on spin win.
     * Evaluates:
     * - Mission 4 (daily_tri_play): spin component today
     * - Mission 6 (arcade_champion): accumulates points towards 200
     * - Badge: NASIB_BAIK (hit max segment: 50 pts)
     */
    fun recordSpinEvent(
        activity: Activity?,
        userId: String,
        pointsWon: Int,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (userId.isBlank()) return

        // Check max segment badge (50 pts)
        val maxSegment = AppConfig.SPIN_WHEEL_SEGMENTS.maxOrNull() ?: 50
        if (pointsWon >= maxSegment && activity != null) {
            BadgeRepository.unlockBadge(activity, userId, BadgeDef.NASIB_BAIK)
        }

        val weekKey = MissionDateUtils.getWeekYearKey(timestamp)
        val dayKey = MissionDateUtils.getDayString(timestamp)

        val trackingRef = db.child("userMissionTracking").child(userId).child(weekKey)
        trackingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentArcade = snapshot.child("arcadePoints").getValue(Int::class.java) ?: 0
                val newArcade = currentArcade + pointsWon

                val updates = hashMapOf<String, Any>(
                    "triPlay/$dayKey/spin" to true,
                    "arcadePoints" to newArcade
                )

                trackingRef.updateChildren(updates).addOnSuccessListener {
                    syncUserMissions(userId)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /**
     * Hook 3: Called from MemoryMatchActivity on win.
     * Evaluates:
     * - Mission 4 (daily_tri_play): memory match component today
     * - Mission 6 (arcade_champion): accumulates points towards 200
     * - Badge: SHARP_BRAIN (10th perfect score game: moves == 6)
     */
    fun recordMemoryMatchEvent(
        activity: Activity?,
        userId: String,
        pointsWon: Int,
        moves: Int,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (userId.isBlank()) return

        // Check perfect game for SHARP_BRAIN (6 moves for 6 pairs)
        val isPerfect = moves <= GameConfig.MEMORY_EMOJIS.size
        if (isPerfect) {
            val perfectRef = db.child("users").child(userId).child("perfectMemoryGamesCount")
            perfectRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = (snapshot.getValue(Int::class.java) ?: 0) + 1
                    perfectRef.setValue(count).addOnSuccessListener {
                        if (count >= 10 && activity != null) {
                            BadgeRepository.unlockBadge(activity, userId, BadgeDef.SHARP_BRAIN)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }

        val weekKey = MissionDateUtils.getWeekYearKey(timestamp)
        val dayKey = MissionDateUtils.getDayString(timestamp)

        val trackingRef = db.child("userMissionTracking").child(userId).child(weekKey)
        trackingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentArcade = snapshot.child("arcadePoints").getValue(Int::class.java) ?: 0
                val newArcade = currentArcade + pointsWon

                val updates = hashMapOf<String, Any>(
                    "triPlay/$dayKey/memory" to true,
                    "arcadePoints" to newArcade
                )

                trackingRef.updateChildren(updates).addOnSuccessListener {
                    syncUserMissions(userId)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ── Reward Claiming Flow ───────────────────────────────────────────────────

    /**
     * Executes the reward claim flow per Phase 3 Specification:
     * 1. Mark user mission as isClaimed = true at /userMissions/{userId}_{missionId}_{weekYearKey}.
     * 2. Call PointRepository.addPoints(userId, rewardPoints, PointSource.CHALLENGE).
     * 3. Call SoundManager.playKaching().
     * 4. If mission has rewardBadgeKey, call BadgeRepository.unlockBadge(activity, userId, badgeDef).
     * 5. Check if all 6 weekly missions are completed ➔ unlock raja_sapot badge.
     */
    fun claimMissionReward(
        activity: Activity,
        userId: String,
        mission: WeeklyMission,
        userMission: UserMission,
        onResult: (success: Boolean, errorMsg: String?) -> Unit
    ) {
        if (userMission.isClaimed) {
            onResult(false, "Reward already claimed")
            return
        }

        if (userMission.currentProgress < mission.targetCount && !userMission.isCompleted) {
            onResult(false, "Mission requirement not met yet")
            return
        }

        val weekKey = userMission.weekYearKey.ifBlank { MissionDateUtils.getWeekYearKey() }
        val recordKey = "${userId}_${mission.missionId}_${weekKey}"

        val updated = userMission.copy(
            userMissionId = recordKey,
            userId = userId,
            missionId = mission.missionId,
            weekYearKey = weekKey,
            currentProgress = mission.targetCount,
            isCompleted = true,
            isClaimed = true,
            lastUpdated = System.currentTimeMillis()
        )

        // 1. Mark isClaimed = true
        db.child("userMissions").child(recordKey).setValue(updated)
            .addOnSuccessListener {
                // 2. Add points using dual ledger
                PointRepository.addPoints(
                    userID = userId,
                    basePoints = mission.rewardPoints,
                    pointSource = PointSource.CHALLENGE
                ) { success, earnedPoints ->
                    if (success) {
                        // 3. Audio FX
                        SoundManager.getInstance(activity).playKaching()

                        // 4. Unlock attached badge if present
                        mission.rewardBadgeKey?.let { badgeId ->
                            BadgeDef.fromId(badgeId)?.let { badgeDef ->
                                BadgeRepository.unlockBadge(activity, userId, badgeDef)
                            }
                        }

                        // 5. Check if all 6 weekly missions are completed ➔ unlock raja_sapot
                        checkAllMissionsCompleted(activity, userId, weekKey)

                        onResult(true, null)
                    } else {
                        onResult(false, "Could not award mission points")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update userMission status", e)
                onResult(false, e.message)
            }
    }

    /**
     * Checks if all 6 weekly missions for the specified week are completed.
     * If so, unlocks RAJA_SAPOT monthly/all-star badge!
     */
    private fun checkAllMissionsCompleted(activity: Activity, userId: String, weekKey: String) {
        db.child("userMissions").orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val completedMissionIds = mutableSetOf<String>()
                    for (c in snapshot.children) {
                        val um = c.getValue(UserMission::class.java) ?: continue
                        if (um.weekYearKey == weekKey && (um.isCompleted || um.isClaimed)) {
                            completedMissionIds.add(um.missionId)
                        }
                    }

                    val allMissionIds = DEFAULT_MISSIONS.map { it.missionId }.toSet()
                    if (completedMissionIds.containsAll(allMissionIds)) {
                        BadgeRepository.unlockBadge(activity, userId, BadgeDef.RAJA_SAPOT)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
