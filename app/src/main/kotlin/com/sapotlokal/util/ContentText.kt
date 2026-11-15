package com.sapotlokal.util

import android.content.Context
import com.sapotlokal.R
import com.sapotlokal.data.model.BadgeDef
import com.sapotlokal.data.model.WeeklyMission

/**
 * Text for missions and badges in the language the customer chose. Mission titles and descriptions
 * and badge descriptions come from string resources; badge titles are names (like "Tapah Ranger") and
 * stay as they are. Anything unknown falls back to the text stored with the item.
 */
object ContentText {

    fun missionTitle(context: Context, mission: WeeklyMission): String = when (mission.missionId) {
        "tapah_explorer" -> context.getString(R.string.m_tapah_explorer_title)
        "loyal_supporter" -> context.getString(R.string.m_loyal_supporter_title)
        "weekend_warrior" -> context.getString(R.string.m_weekend_warrior_title)
        "daily_tri_play" -> context.getString(R.string.m_daily_tri_play_title)
        "night_owl" -> context.getString(R.string.m_night_owl_title)
        "arcade_champion" -> context.getString(R.string.m_arcade_champion_title)
        else -> mission.title
    }

    fun missionDescription(context: Context, mission: WeeklyMission): String = when (mission.missionId) {
        "tapah_explorer" -> context.getString(R.string.m_tapah_explorer_desc)
        "loyal_supporter" -> context.getString(R.string.m_loyal_supporter_desc)
        "weekend_warrior" -> context.getString(R.string.m_weekend_warrior_desc)
        "daily_tri_play" -> context.getString(R.string.m_daily_tri_play_desc)
        "night_owl" -> context.getString(R.string.m_night_owl_desc)
        "arcade_champion" -> context.getString(R.string.m_arcade_champion_desc)
        else -> mission.description
    }

    @Suppress("UNUSED_PARAMETER")
    fun badgeTitle(context: Context, badge: BadgeDef): String = badge.title

    fun badgeDescription(context: Context, badge: BadgeDef): String = when (badge) {
        BadgeDef.LANGKAH_KANAN -> context.getString(R.string.b_langkah_kanan_desc)
        BadgeDef.TAPAH_RANGER -> context.getString(R.string.b_tapah_ranger_desc)
        BadgeDef.NASIB_BAIK -> context.getString(R.string.b_nasib_baik_desc)
        BadgeDef.SHARP_BRAIN -> context.getString(R.string.b_sharp_brain_desc)
        BadgeDef.HANTU_MALAM -> context.getString(R.string.b_hantu_malam_desc)
        BadgeDef.REGULAR -> context.getString(R.string.b_regular_desc)
        BadgeDef.COMBO_MASTER -> context.getString(R.string.b_combo_master_desc)
        BadgeDef.RAJA_SAPOT -> context.getString(R.string.b_raja_sapot_desc)
    }
}
