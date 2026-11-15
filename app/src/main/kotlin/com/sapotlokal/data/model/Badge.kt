package com.sapotlokal.data.model

import com.sapotlokal.R

/**
 * The eight badges (REFRACTOR.md §3.5). Names are the same in both languages; [howToRes] is the
 * one line the badge sheet shows. A badge is unlocked by the write that earns it and pays [xp]
 * once, when the customer claims it (BadgeRepository).
 *
 * Badges never reward the same thing as a mission.
 */
enum class BadgeDef(
    val id: String,
    val title: String,
    val howToRes: Int,
    val xp: Int,
    val iconRes: Int
) {
    LANGKAH_KANAN("langkah_kanan", "Langkah Kanan", R.string.badge_how_langkah_kanan, 25, R.drawable.ic_badge_first_scan),
    REGULAR("regular", "Regular", R.string.badge_how_regular, 50, R.drawable.ic_badge_repeat_visit),
    HANTU_MALAM("hantu_malam", "Hantu Malam", R.string.badge_how_hantu_malam, 50, R.drawable.ic_badge_night_owl),
    COMBO("combo", "Combo", R.string.badge_how_combo, 50, R.drawable.ic_badge_combo),
    NASIB_BAIK("nasib_baik", "Nasib Baik", R.string.badge_how_nasib_baik, 50, R.drawable.ic_badge_wheel_max),
    OTAK_CERDAS("otak_cerdas", "Otak Cerdas", R.string.badge_how_otak_cerdas, 50, R.drawable.ic_badge_memory_perfect),
    BELANJA_DIRI("belanja_diri", "Belanja Diri", R.string.badge_how_belanja_diri, 50, R.drawable.ic_badge_treat),
    JUARA_MINGGU("juara_minggu", "Juara Minggu", R.string.badge_how_juara_minggu, 100, R.drawable.ic_badge_podium);

    companion object {
        fun fromId(id: String): BadgeDef? = entries.find { it.id == id }

        /** Visit on this many different days at one shop in one week for [REGULAR]. */
        const val REGULAR_DAYS = 3

        /** A visit at or after this hour (Malaysia time) earns [HANTU_MALAM]. */
        const val NIGHT_HOUR = 20
    }
}

/** One badge as the customer sees it. */
data class BadgeState(val def: BadgeDef, val unlockedAt: Long, val claimedAt: Long) {
    val unlocked: Boolean get() = unlockedAt > 0L
    val claimed: Boolean get() = claimedAt > 0L
    val claimable: Boolean get() = unlocked && !claimed
}
