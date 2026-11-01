package com.sapotlokal.data.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sapotlokal.AppConfig
import com.sapotlokal.AppConfig.CustomerTier
import com.sapotlokal.data.model.LeaderboardMember
import com.sapotlokal.data.model.Point
import com.sapotlokal.data.model.User
import com.sapotlokal.util.MissionDateUtils

/**
 * LeaderboardRepository — Centralised data queries for Community Leaderboards.
 *
 * Implements Phase 7:
 * - All-Time ranking: ordered by lifetimePoints descending.
 * - This Week ranking: aggregated earned points from /points within the active ISO week window.
 */
object LeaderboardRepository {

    private const val TAG = "LeaderboardRepository"

    private val db get() = FirebaseDatabase
        .getInstance(AppConfig.DATABASE_URL)
        .reference

    /**
     * Fetches all-time customer rankings ordered by lifetimePoints descending.
     */
    fun fetchAllTimeLeaderboard(
        limit: Int = 50,
        onResult: (List<LeaderboardMember>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val customerList = mutableListOf<LeaderboardMember>()

                for (child in snapshot.children) {
                    val user = child.getValue(User::class.java) ?: continue
                    // Only include customers in customer leaderboard
                    if (user.role.equals("customer", ignoreCase = true)) {
                        val tier = CustomerTier.fromLifetimePoints(user.lifetimePoints)
                        val name = user.userName.ifBlank { "Shopper" }
                        customerList.add(
                            LeaderboardMember(
                                userId = user.userID,
                                userName = name,
                                points = user.lifetimePoints,
                                currentTier = tier.code,
                                rank = 0
                            )
                        )
                    }
                }

                // Sort descending by points, tie-break by name
                customerList.sortWith(
                    compareByDescending<LeaderboardMember> { it.points }
                        .thenBy { it.userName }
                )

                // Assign 1-indexed ranks
                val ranked = customerList.mapIndexed { index, member ->
                    member.copy(rank = index + 1)
                }

                onResult(ranked.take(limit))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "fetchAllTimeLeaderboard error: ${error.message}")
                onError(error.message)
            }
        })
    }

    /**
     * Fetches weekly customer rankings by aggregating earned points within the current ISO week.
     */
    fun fetchWeeklyLeaderboard(
        limit: Int = 50,
        onResult: (List<LeaderboardMember>) -> Unit,
        onError: (String) -> Unit
    ) {
        val weekStart = MissionDateUtils.getWeekStartMillis()
        val weekExpires = MissionDateUtils.getWeekExpiresAtMillis()

        // Step 1: Read all customer users first to have name & tier info
        db.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnap: DataSnapshot) {
                val customers = mutableMapOf<String, User>()
                for (child in userSnap.children) {
                    val user = child.getValue(User::class.java) ?: continue
                    if (user.role.equals("customer", ignoreCase = true)) {
                        customers[user.userID] = user
                    }
                }

                if (customers.isEmpty()) {
                    onResult(emptyList())
                    return
                }

                // Step 2: Read points to calculate weekly earned sums
                db.child("points").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(pointSnap: DataSnapshot) {
                        val weeklyPointsMap = mutableMapOf<String, Int>()

                        for (child in pointSnap.children) {
                            val p = child.getValue(Point::class.java) ?: continue
                            if (p.pointEarnedAmount <= 0) continue

                            val timestamp = if (p.timestamp > 0L) {
                                p.timestamp
                            } else {
                                MissionDateUtils.extractTimestampFromPushKey(p.pointID.ifBlank { child.key ?: "" })
                            }

                            if (timestamp in weekStart..weekExpires) {
                                val current = weeklyPointsMap.getOrDefault(p.userID, 0)
                                weeklyPointsMap[p.userID] = current + p.pointEarnedAmount
                            }
                        }

                        // Build ranked list for all customers
                        val memberList = customers.values.map { user ->
                            val weeklyPts = weeklyPointsMap.getOrDefault(user.userID, 0)
                            val tier = CustomerTier.fromLifetimePoints(user.lifetimePoints)
                            val name = user.userName.ifBlank { "Shopper" }
                            LeaderboardMember(
                                userId = user.userID,
                                userName = name,
                                points = weeklyPts,
                                currentTier = tier.code,
                                rank = 0
                            )
                        }.toMutableList()

                        // Sort descending by weekly points, tie-break by lifetime points
                        memberList.sortWith(
                            compareByDescending<LeaderboardMember> { it.points }
                                .thenByDescending { customers[it.userId]?.lifetimePoints ?: 0 }
                                .thenBy { it.userName }
                        )

                        val ranked = memberList.mapIndexed { index, member ->
                            member.copy(rank = index + 1)
                        }

                        onResult(ranked.take(limit))
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "fetchWeeklyLeaderboard points error: ${error.message}")
                        onError(error.message)
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "fetchWeeklyLeaderboard users error: ${error.message}")
                onError(error.message)
            }
        })
    }
}

