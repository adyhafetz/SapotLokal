package com.sapotlokal

import com.sapotlokal.AppConfig.CustomerTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomerTierTest {

    @Test
    fun testFreshieBoundaries() {
        assertEquals(CustomerTier.FRESHIE, CustomerTier.fromLifetimePoints(0))
        assertEquals(CustomerTier.FRESHIE, CustomerTier.fromLifetimePoints(199))
        assertEquals("Freshie", CustomerTier.FRESHIE.title)
        assertEquals("🔰", CustomerTier.FRESHIE.emoji)
        assertEquals(1.0, CustomerTier.FRESHIE.multiplier, 0.0)
    }

    @Test
    fun testSteadyBuyerBoundaries() {
        assertEquals(CustomerTier.STEADY_BUYER, CustomerTier.fromLifetimePoints(200))
        assertEquals(CustomerTier.STEADY_BUYER, CustomerTier.fromLifetimePoints(599))
        assertEquals("Steady Buyer", CustomerTier.STEADY_BUYER.title)
        assertEquals("🛒", CustomerTier.STEADY_BUYER.emoji)
        assertEquals(1.0, CustomerTier.STEADY_BUYER.multiplier, 0.0)
    }

    @Test
    fun testTrendSetterBoundaries() {
        assertEquals(CustomerTier.TREND_SETTER, CustomerTier.fromLifetimePoints(600))
        assertEquals(CustomerTier.TREND_SETTER, CustomerTier.fromLifetimePoints(1499))
        assertEquals("Trend Setter", CustomerTier.TREND_SETTER.title)
        assertEquals("🔥", CustomerTier.TREND_SETTER.emoji)
        assertEquals(1.2, CustomerTier.TREND_SETTER.multiplier, 0.0)
    }

    @Test
    fun testOtaiLokalBoundaries() {
        assertEquals(CustomerTier.OTAI_LOKAL, CustomerTier.fromLifetimePoints(1500))
        assertEquals(CustomerTier.OTAI_LOKAL, CustomerTier.fromLifetimePoints(2499))
        assertEquals("Otai Lokal", CustomerTier.OTAI_LOKAL.title)
        assertEquals("👑", CustomerTier.OTAI_LOKAL.emoji)
        assertEquals(1.5, CustomerTier.OTAI_LOKAL.multiplier, 0.0)
    }

    @Test
    fun testSapotGoatBoundaries() {
        assertEquals(CustomerTier.SAPOT_GOAT, CustomerTier.fromLifetimePoints(2500))
        assertEquals(CustomerTier.SAPOT_GOAT, CustomerTier.fromLifetimePoints(100000))
        assertEquals("Sapot GOAT", CustomerTier.SAPOT_GOAT.title)
        assertEquals("🐐", CustomerTier.SAPOT_GOAT.emoji)
        assertEquals(2.0, CustomerTier.SAPOT_GOAT.multiplier, 0.0)
        assertNull(CustomerTier.SAPOT_GOAT.nextTier)
    }

    @Test
    fun testTierMultipliersAppliedToBasePoints() {
        val basePoints = 10
        assertEquals(10, (basePoints * CustomerTier.FRESHIE.multiplier).toInt())
        assertEquals(10, (basePoints * CustomerTier.STEADY_BUYER.multiplier).toInt())
        assertEquals(12, (basePoints * CustomerTier.TREND_SETTER.multiplier).toInt())
        assertEquals(15, (basePoints * CustomerTier.OTAI_LOKAL.multiplier).toInt())
        assertEquals(20, (basePoints * CustomerTier.SAPOT_GOAT.multiplier).toInt())
    }

    @Test
    fun testProgressionInterpolation() {
        // User with 1650 points in OTAI_LOKAL (1500 - 2499), next tier SAPOT_GOAT at 2500
        val points = 1650
        val tier = CustomerTier.fromLifetimePoints(points)
        assertEquals(CustomerTier.OTAI_LOKAL, tier)
        val nextTier = tier.nextTier!!
        assertEquals(CustomerTier.SAPOT_GOAT, nextTier)

        val span = nextTier.minPoints - tier.minPoints // 2500 - 1500 = 1000
        val earnedInTier = points - tier.minPoints     // 1650 - 1500 = 150
        val pct = (earnedInTier.toDouble() / span.toDouble() * 100).toInt()
        assertEquals(15, pct)
        val needed = nextTier.minPoints - points
        assertEquals(850, needed)
    }
}

