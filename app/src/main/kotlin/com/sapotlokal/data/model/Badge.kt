package com.sapotlokal.data.model

import com.sapotlokal.R

/**
 * 8 Official SapotLokal Gamification Badges.
 */
enum class BadgeDef(
    val id: String,
    val title: String,
    val description: String,
    val iconRes: Int
) {
    LANGKAH_KANAN("langkah_kanan", "Langkah Kanan", "Scanned your first shop visit.", R.drawable.ic_badge_first_scan),
    TAPAH_RANGER("tapah_ranger", "Tapah Ranger", "Visited 10 local shops.", R.drawable.ic_badge_shops_10),
    NASIB_BAIK("nasib_baik", "Nasib Baik", "Hit highest points on the Spin Wheel.", R.drawable.ic_badge_wheel_max),
    SHARP_BRAIN("sharp_brain", "Sharp Brain", "Achieved perfect score in Memory Match 10 times.", R.drawable.ic_badge_memory_perfect),
    HANTU_MALAM("hantu_malam", "Hantu Malam", "Scanned a shop visit after 8:00 PM.", R.drawable.ic_badge_night_owl),
    REGULAR("regular", "Regular", "Visited the same shop twice in one week.", R.drawable.ic_badge_repeat_visit),
    COMBO_MASTER("combo_master", "Combo Master", "Visit + Spin + Memory Match in one day.", R.drawable.ic_badge_combo),
    RAJA_SAPOT("raja_sapot", "Raja Sapot [Monthly]", "Completed all weekly challenges in a week.", R.drawable.ic_badge_raja);

    companion object {
        fun fromId(id: String): BadgeDef? = entries.find { it.id == id }
    }
}

