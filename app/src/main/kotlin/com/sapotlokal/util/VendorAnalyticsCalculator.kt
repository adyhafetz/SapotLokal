package com.sapotlokal.util

import com.sapotlokal.data.model.Point
import com.sapotlokal.data.model.PointSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Numbers shown on the vendor Overview. Pure function (no Firebase) so it can be unit-tested.
 * A "visit" = one shop-visit points row for this shop (a customer scan that earned points).
 */
data class VendorAnalytics(
    val todayVisits: Int = 0,
    val todayCustomers: Int = 0,
    val yesterdayVisits: Int = 0,
    val monthVisits: Int = 0,
    val lastMonthVisits: Int = 0,
    val totalPointsIssued: Int = 0,
    val vouchersRedeemed: Int = 0,
    /** 8 two-hour buckets from 6am: 6,8,10,12,2,4,6,8 (pm from 12). */
    val hourly: List<Int> = List(8) { 0 },
    /** Visits per week of the current month (days 1-7, 8-14, 15-21, 22+). */
    val weekly: List<Int> = List(4) { 0 }
) {
    val busiestHourLabel: String?
        get() {
            val max = hourly.maxOrNull() ?: 0
            if (max <= 0) return null
            return HOUR_LABELS[hourly.indexOf(max)]
        }

    companion object {
        val HOUR_LABELS = listOf("6am", "8am", "10am", "12pm", "2pm", "4pm", "6pm", "8pm")
        val WEEK_LABELS = listOf("Week 1", "Week 2", "Week 3", "Week 4")
    }
}

object VendorAnalyticsCalculator {

    fun compute(
        points: List<Point>,
        vouchersRedeemed: Int,
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = MissionDateUtils.KL_ZONE
    ): VendorAnalytics {
        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMs), zone)
        val today: LocalDate = now.toLocalDate()
        val yesterday = today.minusDays(1)
        val thisMonth = today.withDayOfMonth(1)
        val lastMonth = thisMonth.minusMonths(1)

        var todayVisits = 0; var yesterdayVisits = 0
        var monthVisits = 0; var lastMonthVisits = 0
        var pointsIssued = 0
        val todayUsers = mutableSetOf<String>()
        val hourly = IntArray(8)
        val weekly = IntArray(4)

        for (p in points) {
            pointsIssued += p.pointEarnedAmount
            if (p.pointSource != PointSource.SHOP_VISIT || p.pointEarnedAmount <= 0 || p.timestamp <= 0L) continue

            val at = ZonedDateTime.ofInstant(Instant.ofEpochMilli(p.timestamp), zone)
            val day = at.toLocalDate()

            if (day == today) {
                todayVisits++
                if (p.userID.isNotBlank()) todayUsers.add(p.userID)
                hourly[((at.hour - 6) / 2).coerceIn(0, 7)]++
            }
            if (day == yesterday) yesterdayVisits++
            if (day.withDayOfMonth(1) == thisMonth) {
                monthVisits++
                weekly[((day.dayOfMonth - 1) / 7).coerceIn(0, 3)]++
            }
            if (day.withDayOfMonth(1) == lastMonth) lastMonthVisits++
        }

        return VendorAnalytics(
            todayVisits = todayVisits,
            todayCustomers = todayUsers.size,
            yesterdayVisits = yesterdayVisits,
            monthVisits = monthVisits,
            lastMonthVisits = lastMonthVisits,
            totalPointsIssued = pointsIssued,
            vouchersRedeemed = vouchersRedeemed,
            hourly = hourly.toList(),
            weekly = weekly.toList()
        )
    }
}
