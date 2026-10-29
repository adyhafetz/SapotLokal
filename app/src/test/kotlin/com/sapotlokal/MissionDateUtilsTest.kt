package com.sapotlokal

import com.sapotlokal.util.MissionDateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class MissionDateUtilsTest {

    private val klZone = ZoneId.of("Asia/Kuala_Lumpur")

    @Test
    fun testGetWeekYearKeyFormat() {
        // Thursday, Oct 29, 2026, 12:00 KL time
        val ldt = LocalDateTime.of(2026, 10, 29, 12, 0, 0)
        val millis = ldt.atZone(klZone).toInstant().toEpochMilli()
        val key = MissionDateUtils.getWeekYearKey(millis)

        assertTrue("Key should match YYYY-Www format: $key", key.matches(Regex("""\d{4}-W\d{2}""")))
        assertEquals("2026-W44", key)
    }

    @Test
    fun testWeekExpiresAtSundayMidnight() {
        // Thursday, Oct 29, 2026
        val ldt = LocalDateTime.of(2026, 10, 29, 12, 0, 0)
        val millis = ldt.atZone(klZone).toInstant().toEpochMilli()

        val expiresAt = MissionDateUtils.getWeekExpiresAtMillis(millis)
        val expireZdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(expiresAt), klZone)

        assertEquals(DayOfWeek.SUNDAY, expireZdt.dayOfWeek)
        assertEquals(23, expireZdt.hour)
        assertEquals(59, expireZdt.minute)
        assertEquals(59, expireZdt.second)
        assertEquals(2026, expireZdt.year)
        assertEquals(11, expireZdt.monthValue) // Sunday Nov 1, 2026
        assertEquals(1, expireZdt.dayOfMonth)
    }

    @Test
    fun testFormatTimeRemaining() {
        val now = 1_000_000_000L

        // More than 2 days
        val threeDaysLater = now + (3 * 86_400_000L)
        assertEquals("3 days left", MissionDateUtils.formatTimeRemaining(threeDaysLater, now))

        // 1 day
        val oneDayLater = now + (25 * 3_600_000L)
        assertEquals("1 day left", MissionDateUtils.formatTimeRemaining(oneDayLater, now))

        // 5 hours
        val fiveHoursLater = now + (5 * 3_600_000L)
        assertEquals("5 hours left", MissionDateUtils.formatTimeRemaining(fiveHoursLater, now))

        // 45 minutes
        val fortyFiveMinsLater = now + (45 * 60_000L)
        assertEquals("45 mins left", MissionDateUtils.formatTimeRemaining(fortyFiveMinsLater, now))

        // Less than 2 minutes
        val oneMinLater = now + (50 * 1_000L)
        assertEquals("Ending soon", MissionDateUtils.formatTimeRemaining(oneMinLater, now))

        // Past expiration
        val past = now - 1000L
        assertEquals("Expired", MissionDateUtils.formatTimeRemaining(past, now))
    }

    @Test
    fun testIsNightScan() {
        // 7:59 PM KL time -> false
        val evening = LocalDateTime.of(2026, 10, 29, 19, 59, 0).atZone(klZone).toInstant().toEpochMilli()
        assertFalse(MissionDateUtils.isNightScan(evening))

        // 8:00 PM KL time -> true
        val night2000 = LocalDateTime.of(2026, 10, 29, 20, 0, 0).atZone(klZone).toInstant().toEpochMilli()
        assertTrue(MissionDateUtils.isNightScan(night2000))

        // 11:30 PM KL time -> true
        val lateNight = LocalDateTime.of(2026, 10, 29, 23, 30, 0).atZone(klZone).toInstant().toEpochMilli()
        assertTrue(MissionDateUtils.isNightScan(lateNight))
    }

    @Test
    fun testIsSaturdayOrSunday() {
        // Friday Oct 30, 2026 -> false
        val friday = LocalDateTime.of(2026, 10, 30, 15, 0, 0).atZone(klZone).toInstant().toEpochMilli()
        assertFalse(MissionDateUtils.isSaturdayOrSunday(friday))

        // Saturday Oct 31, 2026 -> true
        val saturday = LocalDateTime.of(2026, 10, 31, 15, 0, 0).atZone(klZone).toInstant().toEpochMilli()
        assertTrue(MissionDateUtils.isSaturdayOrSunday(saturday))

        // Sunday Nov 1, 2026 -> true
        val sunday = LocalDateTime.of(2026, 11, 1, 15, 0, 0).atZone(klZone).toInstant().toEpochMilli()
        assertTrue(MissionDateUtils.isSaturdayOrSunday(sunday))

        // Monday Nov 2, 2026 -> false
        val monday = LocalDateTime.of(2026, 11, 2, 10, 0, 0).atZone(klZone).toInstant().toEpochMilli()
        assertFalse(MissionDateUtils.isSaturdayOrSunday(monday))
    }
}

