package com.sapotlokal.util

import android.widget.TextView
import com.sapotlokal.R

/**
 * StatusPill — the small "● Open" / "● Closed" label placed at the TOP-LEFT of a shop photo
 * (brand design). Brown dot = open, grey dot = closed.
 */
object StatusPill {
    fun bind(view: TextView, isOpen: Boolean) {
        view.setText(if (isOpen) R.string.status_open else R.string.status_closed)
        view.setBackgroundResource(R.drawable.bg_status_pill)
        view.compoundDrawablePadding = (6 * view.resources.displayMetrics.density).toInt()
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (isOpen) R.drawable.dot_open else R.drawable.dot_closed, 0, 0, 0
        )
    }
}
