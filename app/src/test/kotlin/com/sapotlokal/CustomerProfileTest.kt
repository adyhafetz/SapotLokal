package com.sapotlokal

import com.sapotlokal.AppConfig.CustomerTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerProfileTest {

    @Test
    fun testDualLedgerSeparationLogic() {
        var lifetimeEarned = 0
        var totalRedeemed = 0

        // Customer earns 100 points
        val earned1 = 100
        lifetimeEarned += earned1

        // Customer earns 150 points
        val earned2 = 150
        lifetimeEarned += earned2

        // Customer redeems 80 points voucher
        val redeemed1 = 80
        totalRedeemed += redeemed1

        val spendableBalance = (lifetimeEarned - totalRedeemed).coerceAtLeast(0)

        // Lifetime points must NOT be decreased by voucher redemption
        assertEquals(250, lifetimeEarned)
        assertEquals(170, spendableBalance)

        // Tier is derived solely from lifetimeEarned
        val tier = CustomerTier.fromLifetimePoints(lifetimeEarned)
        assertEquals(CustomerTier.STEADY_BUYER, tier)
        assertEquals(CustomerTier.TREND_SETTER, tier.nextTier)
    }

    @Test
    fun testTierProgressionSpanCalculation() {
        val lifetimePoints = 400 // Within Steady Buyer (200..599)
        val currentTier = CustomerTier.fromLifetimePoints(lifetimePoints)
        val nextTier = currentTier.nextTier

        assertEquals(CustomerTier.STEADY_BUYER, currentTier)
        assertEquals(CustomerTier.TREND_SETTER, nextTier)

        val tierSpan = nextTier!!.minPoints - currentTier.minPoints // 600 - 200 = 400
        val earnedInTier = lifetimePoints - currentTier.minPoints // 400 - 200 = 200
        val pct = ((earnedInTier.toDouble() / tierSpan.toDouble()) * 100).toInt()

        assertEquals(50, pct)
        val ptsNeeded = nextTier.minPoints - lifetimePoints
        assertEquals(200, ptsNeeded)
    }

    @Test
    fun testMaxTierProgressionCalculation() {
        val lifetimePoints = 3000
        val currentTier = CustomerTier.fromLifetimePoints(lifetimePoints)

        assertEquals(CustomerTier.SAPOT_GOAT, currentTier)
        assertNull(currentTier.nextTier)
    }
}

