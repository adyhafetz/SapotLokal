package com.sapotlokal.util

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import com.sapotlokal.R
import com.sapotlokal.data.model.BadgeDef
import com.sapotlokal.databinding.DialogBadgeUnlockedBinding

/**
 * Helper to display the animated "New Badge Unlocked!" modal dialog.
 */
object BadgeDialogHelper {

    fun show(activity: Activity, badge: BadgeDef, onDismiss: (() -> Unit)? = null): AlertDialog? {
        if (activity.isFinishing || activity.isDestroyed) return null

        val binding = DialogBadgeUnlockedBinding.inflate(LayoutInflater.from(activity))
        val dialog = AlertDialog.Builder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.ivBadgeIcon.setImageResource(badge.iconRes)
        binding.tvBadgeTitle.text = badge.title
        binding.tvBadgeDescription.text = badge.description

        val bounceAnim = AnimationUtils.loadAnimation(activity, R.anim.badge_bounce)
        binding.ivBadgeIcon.startAnimation(bounceAnim)
        binding.ivBadgeGlow.startAnimation(bounceAnim)

        binding.btnAwesome.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            onDismiss?.invoke()
        }

        dialog.show()
        return dialog
    }
}

