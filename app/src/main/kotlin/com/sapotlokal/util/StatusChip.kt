package com.sapotlokal.util

import android.widget.TextView
import androidx.core.content.ContextCompat
import com.sapotlokal.R

/**
 * StatusChip — the ONE place that styles an Open / Closed pill, so every screen matches:
 *   Open   → app primary (cocoa) with light text
 *   Closed → disabled grey with muted text
 */
object StatusChip {
    fun bind(view: TextView, isOpen: Boolean) {
        val ctx = view.context
        if (isOpen) {
            view.text = ctx.getString(R.string.status_open)
            view.setBackgroundResource(R.drawable.bg_status_chip_open)
            view.setTextColor(ContextCompat.getColor(ctx, R.color.sapot_on_primary))
        } else {
            view.text = ctx.getString(R.string.status_closed)
            view.setBackgroundResource(R.drawable.bg_status_chip_closed)
            view.setTextColor(ContextCompat.getColor(ctx, R.color.status_closed_text))
        }
    }
}
