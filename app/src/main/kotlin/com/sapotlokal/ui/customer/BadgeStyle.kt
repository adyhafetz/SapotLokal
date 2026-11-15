package com.sapotlokal.ui.customer

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.ColorUtils
import com.sapotlokal.data.model.BadgeDef

/**
 * BadgeStyle — how a badge LOOKS (accent colour and glow). It does not decide which badges
 * exist or how they are earned; that stays in BadgeDef and the mission/game code.
 * Each accent matches the colour already used inside that badge's own icon.
 */
object BadgeStyle {

    fun accent(badge: BadgeDef): Int = when (badge) {
        BadgeDef.LANGKAH_KANAN -> 0xFF2E8B63.toInt()   // green
        BadgeDef.TAPAH_RANGER  -> 0xFFE65100.toInt()   // orange
        BadgeDef.NASIB_BAIK    -> 0xFFFF8F00.toInt()   // amber
        BadgeDef.SHARP_BRAIN   -> 0xFF5E35B1.toInt()   // purple
        BadgeDef.HANTU_MALAM   -> 0xFF3949AB.toInt()   // indigo
        BadgeDef.REGULAR       -> 0xFFC2185B.toInt()   // pink
        BadgeDef.COMBO_MASTER  -> 0xFFFF5722.toInt()   // deep orange
        BadgeDef.RAJA_SAPOT    -> 0xFFF2B01E.toInt()   // gold
    }

    /** Soft radial glow in [accent], fading to transparent at the edge. */
    fun halo(context: Context, accent: Int, strength: Int = 120): GradientDrawable {
        val density = context.resources.displayMetrics.density
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(ColorUtils.setAlphaComponent(accent, strength), ColorUtils.setAlphaComponent(accent, 0))
        ).apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = 90f * density
            shape = GradientDrawable.OVAL
        }
    }

    /** Rounded pill filled with a light tint of [accent]. */
    fun pill(context: Context, accent: Int): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = 20f * context.resources.displayMetrics.density
            setColor(ColorUtils.setAlphaComponent(accent, 40))
        }
}
