package com.sapotlokal.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * MissionDateUtils — Date and calendar utilities for SapotLokal Weekly Missions.
 *
 * Configured specifically for Asia/Kuala_Lumpur timezone (UTC+8).
 * Missions reset automatically every Sunday at 23:59:59 Asia/Kuala_Lumpur.
 */
object MissionDateUtils {

    val KL_ZONE: ZoneId = ZoneId.of("Asia/Kuala_Lumpur")

    /**
     * Returns the ISO-8601 week key in KL timezone, e.g. "2026-W44".
     */
    fun getWeekYearKey(timestampMillis: Long = System.currentTimeMillis()): String {
        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), KL_ZONE)
        val year = zdt.get(IsoFields.WEEK_BASED_YEAR)
        val week = zdt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return String.format(Locale.US, "%04d-W%02d", year, week)
    }

    /**
     * Returns the epoch milliseconds of Sunday 23:59:59 of the current ISO week in KL timezone.
     */
    fun getWeekExpiresAtMillis(timestampMillis: Long = System.currentTimeMillis()): Long {
        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), KL_ZONE)
        val sunday = zdt.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            .withHour(23)
            .withMinute(59)
            .withSecond(59)
            .withNano(999_000_000)
        return sunday.toInstant().toEpochMilli()
    }

    /**
     * Formats the remaining time until expiration into a human-readable string.
     * e.g. "3 days left", "1 day left", "5 hours left", "Ending soon".
     */
    fun formatTimeRemaining(expiresAt: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diff = expiresAt - nowMillis
        return when {
            diff <= 0L -> "Expired"
            diff >= 2 * 86_400_000L -> "${diff / 86_400_000L} days left"
            diff >= 86_400_000L -> "1 day left"
            diff >= 2 * 3_600_000L -> "${diff / 3_600_000L} hours left"
            diff >= 3_600_000L -> "1 hour left"
            diff >= 2 * 60_000L -> "${diff / 60_000L} mins left"
            else -> "Ending soon"
        }
    }

    /**
     * Returns "yyyy-MM-dd" date string in KL timezone.
     */
    fun getDayString(timestampMillis: Long = System.currentTimeMillis()): String {
        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), KL_ZONE)
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            zdt.year,
            zdt.monthValue,
            zdt.dayOfMonth
        )
    }

    /**
     * Returns true if the timestamp falls after 8:00 PM (20:00) in KL timezone.
     */
    fun isNightScan(timestampMillis: Long = System.currentTimeMillis()): Boolean {
        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), KL_ZONE)
        return zdt.hour >= 20
    }

    /**
     * Returns true if the timestamp falls on Saturday or Sunday in KL timezone.
     */
    fun isSaturdayOrSunday(timestampMillis: Long = System.currentTimeMillis()): Boolean {
        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), KL_ZONE)
        val dow = zdt.dayOfWeek
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY
    }

    /**
     * Returns the DayOfWeek in KL timezone.
     */
    fun getDayOfWeek(timestampMillis: Long = System.currentTimeMillis()): DayOfWeek {
        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), KL_ZONE)
        return zdt.dayOfWeek
    }
}
