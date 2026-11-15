package com.sapotlokal

import com.sapotlokal.data.model.Point
import com.sapotlokal.data.model.PointSource
import com.sapotlokal.util.VendorAnalyticsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class VendorAnalyticsTest {
    private val kl = ZoneId.of("Asia/Kuala_Lumpur")
    private fun ms(y: Int, m: Int, d: Int, h: Int) =
        ZonedDateTime.of(y, m, d, h, 0, 0, 0, kl).toInstant().toEpochMilli()

    private fun visit(user: String, at: Long) = Point(
        userID = user, vendorID = "v", pointEarnedAmount = 10,
        pointSource = PointSource.SHOP_VISIT, timestamp = at
    )

    @Test
    fun countsTodayYesterdayAndMonth() {
        val now = ms(2026, 11, 12, 13)
        val points = listOf(
            visit("a", ms(2026, 11, 12, 9)),   // today, 8am bucket (index 1)
            visit("b", ms(2026, 11, 12, 12)),  // today, 12pm bucket (index 3)
            visit("a", ms(2026, 11, 12, 12)),  // same customer again
            visit("c", ms(2026, 11, 11, 10)),  // yesterday
            visit("d", ms(2026, 10, 30, 10))   // last month
        )
        val r = VendorAnalyticsCalculator.compute(points, vouchersRedeemed = 2, nowMs = now, zone = kl)
        assertEquals(3, r.todayVisits)
        assertEquals(2, r.todayCustomers)
        assertEquals(1, r.yesterdayVisits)
        assertEquals(4, r.monthVisits)
        assertEquals(1, r.lastMonthVisits)
        assertEquals(2, r.vouchersRedeemed)
        assertEquals(1, r.hourly[1])
        assertEquals(2, r.hourly[3])
        assertEquals("12pm", r.busiestHourLabel)
    }

    @Test
    fun weeksOfMonthAndIgnoresNonVisits() {
        val now = ms(2026, 11, 28, 10)
        val redemption = Point(
            userID = "a", vendorID = "v", pointRedeemedAmount = 10,
            pointSource = PointSource.REDEMPTION, timestamp = ms(2026, 11, 28, 9)
        )
        val points = listOf(
            visit("a", ms(2026, 11, 3, 10)),   // week 1
            visit("a", ms(2026, 11, 20, 10)),  // week 3
            visit("a", ms(2026, 11, 30, 10)),  // week 4 (day 22+)
            redemption
        )
        val r = VendorAnalyticsCalculator.compute(points, 0, now, kl)
        assertEquals(listOf(1, 0, 1, 1), r.weekly)
        assertEquals(30, r.totalPointsIssued)
    }

    @Test
    fun emptyDataHasNoBusiestHour() {
        val r = VendorAnalyticsCalculator.compute(emptyList(), 0)
        assertNull(r.busiestHourLabel)
        assertEquals(0, r.monthVisits)
    }
}
