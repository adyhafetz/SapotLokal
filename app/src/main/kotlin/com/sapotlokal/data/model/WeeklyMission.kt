package com.sapotlokal.data.model

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
)

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

