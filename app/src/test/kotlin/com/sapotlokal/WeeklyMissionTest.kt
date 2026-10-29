package com.sapotlokal

import com.sapotlokal.data.model.BadgeDef
import com.sapotlokal.data.model.UserMission
import com.sapotlokal.data.model.WeeklyMissionWithProgress
import com.sapotlokal.data.repository.MissionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyMissionTest {

    @Test
    fun testAllSixWeeklyMissionsConfigured() {
        val missions = MissionRepository.DEFAULT_MISSIONS
        assertEquals(6, missions.size)

        val missionMap = missions.associateBy { it.missionId }

        // Mission 1: tapah_explorer
        val m1 = missionMap["tapah_explorer"]
        assertNotNull(m1)
        assertEquals(3, m1?.targetCount)
        assertEquals(100, m1?.rewardPoints)
        assertEquals(null, m1?.rewardBadgeKey)

        // Mission 2: loyal_supporter
        val m2 = missionMap["loyal_supporter"]
        assertNotNull(m2)
        assertEquals(2, m2?.targetCount)
        assertEquals(50, m2?.rewardPoints)
        assertEquals(BadgeDef.REGULAR.id, m2?.rewardBadgeKey)

        // Mission 3: weekend_warrior
        val m3 = missionMap["weekend_warrior"]
        assertNotNull(m3)
        assertEquals(2, m3?.targetCount)
        assertEquals(80, m3?.rewardPoints)
        assertEquals(null, m3?.rewardBadgeKey)

        // Mission 4: daily_tri_play
        val m4 = missionMap["daily_tri_play"]
        assertNotNull(m4)
        assertEquals(3, m4?.targetCount)
        assertEquals(75, m4?.rewardPoints)
        assertEquals(BadgeDef.COMBO_MASTER.id, m4?.rewardBadgeKey)

        // Mission 5: night_owl
        val m5 = missionMap["night_owl"]
        assertNotNull(m5)
        assertEquals(2, m5?.targetCount)
        assertEquals(60, m5?.rewardPoints)
        assertEquals(BadgeDef.HANTU_MALAM.id, m5?.rewardBadgeKey)

        // Mission 6: arcade_champion
        val m6 = missionMap["arcade_champion"]
        assertNotNull(m6)
        assertEquals(200, m6?.targetCount)
        assertEquals(80, m6?.rewardPoints)
        assertEquals(null, m6?.rewardBadgeKey)
    }

    @Test
    fun testWeeklyMissionWithProgressCalculations() {
        val mission = MissionRepository.DEFAULT_MISSIONS.first { it.missionId == "tapah_explorer" }

        // Case 1: In progress
        val inProgress = WeeklyMissionWithProgress(
            mission = mission,
            userMission = UserMission(currentProgress = 1)
        )
        assertEquals(1, inProgress.progress)
        assertFalse(inProgress.isCompleted)
        assertFalse(inProgress.isClaimed)

        // Case 2: Target met (completed, not claimed)
        val completed = WeeklyMissionWithProgress(
            mission = mission,
            userMission = UserMission(currentProgress = 3, isCompleted = true, isClaimed = false)
        )
        assertEquals(3, completed.progress)
        assertTrue(completed.isCompleted)
        assertFalse(completed.isClaimed)

        // Case 3: Claimed
        val claimed = WeeklyMissionWithProgress(
            mission = mission,
            userMission = UserMission(currentProgress = 3, isCompleted = true, isClaimed = true)
        )
        assertEquals(3, claimed.progress)
        assertTrue(claimed.isCompleted)
        assertTrue(claimed.isClaimed)
    }
}
