package com.sapotlokal

import com.sapotlokal.data.model.Vendor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Phase 6: Vendor Manual Status Toggle, Operating Hours & Topic Notifications.
 */
class VendorStatusTest {

    @Test
    fun testVendorDefaultStateIsOpen() {
        val vendor = Vendor(
            vendorID = "vendor_test_1",
            vendorName = "Kopi Tapah",
            vendorCategory = "Food & Beverage",
            vendorAddress = "No. 12, Jalan Besar, Tapah"
        )

        assertTrue("New vendor should default to OPEN", vendor.isOpen)
        assertEquals("", vendor.operatingHours)
        assertEquals(0L, vendor.lastStatusChangedAt)
    }

    @Test
    fun testVendorClosedStatusAndHoursUpdate() {
        val vendor = Vendor(
            vendorID = "vendor_test_2",
            vendorName = "Roti Canai Otai",
            vendorCategory = "Bakery",
            vendorAddress = "Medan Selera Tapah",
            isOpen = false,
            operatingHours = "7:00 AM - 2:00 PM (Tues-Sun)",
            lastStatusChangedAt = 1729000000000L
        )

        assertFalse("Vendor status should be CLOSED", vendor.isOpen)
        assertEquals("7:00 AM - 2:00 PM (Tues-Sun)", vendor.operatingHours)
        assertEquals(1729000000000L, vendor.lastStatusChangedAt)
    }

    @Test
    fun testShopStatusPayloadGeneration() {
        val vendorId = "vendor_abc_123"
        val vendorName = "Warisan Coffee"
        val topic = "shop_$vendorId"

        assertEquals("shop_vendor_abc_123", topic)

        val payload = mapOf(
            "vendorID" to vendorId,
            "vendorName" to vendorName,
            "type" to "shop_status",
            "isOpen" to true,
            "topic" to topic,
            "title" to "$vendorName is now OPEN! 🟢",
            "body" to "$vendorName is now OPEN! Drop by today to earn your loyalty points."
        )

        assertEquals("shop_status", payload["type"])
        assertEquals("Warisan Coffee is now OPEN! 🟢", payload["title"])
        assertEquals("Warisan Coffee is now OPEN! Drop by today to earn your loyalty points.", payload["body"])
        assertEquals("shop_vendor_abc_123", payload["topic"])
        assertEquals(true, payload["isOpen"])
    }
}

