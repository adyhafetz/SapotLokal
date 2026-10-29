package com.sapotlokal

import com.sapotlokal.data.model.BadgeDef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BadgeDefTest {

    @Test
    fun testAllEightBadgesExist() {
        assertEquals(8, BadgeDef.entries.size)
    }

    @Test
    fun testBadgeIdsAndLookup() {
        val expected = listOf(
            "langkah_kanan" to "Langkah Kanan",
            "tapah_ranger" to "Tapah Ranger",
            "nasib_baik" to "Nasib Baik",
            "sharp_brain" to "Sharp Brain",
            "hantu_malam" to "Hantu Malam",
            "regular" to "Regular",
            "combo_master" to "Combo Master",
            "raja_sapot" to "Raja Sapot [Monthly]"
        )

        for ((id, title) in expected) {
            val badge = BadgeDef.fromId(id)
            assertNotNull("Badge should exist for id: $id", badge)
            assertEquals(title, badge?.title)
        }

        assertNull(BadgeDef.fromId("non_existent_badge"))
    }
}

