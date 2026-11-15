package com.sapotlokal.util

import com.sapotlokal.data.model.Point
import com.sapotlokal.data.model.PointSource

/**
 * Numbers on the shop owner's Home (REFRACTOR.md Phase 10). Pure function (no Firebase) so it can
 * be unit-tested. A "visit" is one shop-visit points row for this shop (a customer scan).
 */
data class VendorAnalytics(
    /** Visits since Monday 00:00 Malaysia time. */
    val weekVisits: Int = 0,
    /** Different customers who visited this week. */
    val weekCustomers: Int = 0,
    /** Shop points customers earned here this week (with their rank bonus). */
    val weekPointsGiven: Int = 0,
    /** Vouchers for this shop marked used, all time. */
    val vouchersUsed: Int = 0,
    /** The most recent visits, newest first. */
    val recentVisits: List<Point> = emptyList()
)

object VendorAnalyticsCalculator {

    const val RECENT_LIMIT = 10

    fun compute(
        points: List<Point>,
        vouchersUsed: Int,
        nowMs: Long = ServerClock.now()
    ): VendorAnalytics {
        val weekStart = MissionDateUtils.getWeekStartMillis(nowMs)
        val visits = points.filter { it.pointSource == PointSource.SHOP_VISIT && it.pointEarnedAmount > 0 && it.timestamp > 0L }
        val thisWeek = visits.filter { it.timestamp >= weekStart }
        return VendorAnalytics(
            weekVisits = thisWeek.size,
            weekCustomers = thisWeek.map { it.userID }.filter { it.isNotBlank() }.toSet().size,
            weekPointsGiven = thisWeek.sumOf { it.pointEarnedAmount },
            vouchersUsed = vouchersUsed,
            recentVisits = visits.sortedByDescending { it.timestamp }.take(RECENT_LIMIT)
        )
    }
}
