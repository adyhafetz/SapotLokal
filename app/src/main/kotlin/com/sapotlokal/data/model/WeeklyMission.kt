package com.sapotlokal.data.model

import com.google.firebase.database.DataSnapshot

/**
 * Definition of an official SapotLokal weekly recurring mission.
 */
data class WeeklyMission(
    val missionId: String = "",
    val title: String = "",
    val description: String = "",
    val targetCount: Int = 1,
    val rewardPoints: Int = 0,
    val rewardBadgeKey: String? = null
)

/**
 * User progress on a weekly mission stored at:
 * /userMissions/{userId}_{missionId}_{weekYearKey}
 */
data class UserMission(
    val userMissionId: String = "",
    val userId: String = "",
    val missionId: String = "",
    val weekYearKey: String = "",
    val currentProgress: Int = 0,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val lastUpdated: Long = 0L
) {
    /**
     * Firebase cannot map Kotlin Boolean properties called "isXxx" (it looks for "claimed" /
     * "completed"), so a plain object would save under the wrong names and read back as false.
     * Records are therefore written from this map and read with [fromSnapshot].
     */
    fun toMap(): Map<String, Any> = mapOf(
        "userMissionId" to userMissionId,
        "userId" to userId,
        "missionId" to missionId,
        "weekYearKey" to weekYearKey,
        "currentProgress" to currentProgress,
        "isCompleted" to isCompleted,
        "isClaimed" to isClaimed,
        "lastUpdated" to lastUpdated
    )

    companion object {
        /** Reads a saved record. Also understands the older spelling ("claimed" / "completed"). */
        fun fromSnapshot(s: DataSnapshot): UserMission? {
            if (!s.exists()) return null
            fun flag(vararg names: String) = names.any { s.child(it).getValue(Boolean::class.java) == true }
            return UserMission(
                userMissionId = s.child("userMissionId").getValue(String::class.java) ?: s.key.orEmpty(),
                userId = s.child("userId").getValue(String::class.java).orEmpty(),
                missionId = s.child("missionId").getValue(String::class.java).orEmpty(),
                weekYearKey = s.child("weekYearKey").getValue(String::class.java).orEmpty(),
                currentProgress = s.child("currentProgress").getValue(Long::class.java)?.toInt() ?: 0,
                isCompleted = flag("isCompleted", "completed"),
                isClaimed = flag("isClaimed", "claimed"),
                lastUpdated = s.child("lastUpdated").getValue(Long::class.java) ?: 0L
            )
        }
    }
}

/**
 * UI representation binding static WeeklyMission with dynamic UserMission progress.
 */
data class WeeklyMissionWithProgress(
    val mission: WeeklyMission,
    val userMission: UserMission?,
    val weekExpiresAtMillis: Long = 0L
) {
    val progress: Int
        get() = userMission?.currentProgress ?: 0

    val isCompleted: Boolean
        get() = progress >= mission.targetCount ||
                (userMission?.isCompleted == true) ||
                (userMission?.isClaimed == true)

    val isClaimed: Boolean
        get() = userMission?.isClaimed == true
}

