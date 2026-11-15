package com.sapotlokal.ui.vendor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.sapotlokal.R
import com.sapotlokal.databinding.LayoutVendorBottomNavBinding

/**
 * VendorNav — wires the vendor bottom bar (Overview · Vouchers · Scanner · Shop profile).
 *
 * Overview is the root of the vendor back stack. The other three tabs replace each other, so
 * the stack never grows no matter how often the owner switches tabs, and Back always returns to
 * Overview.
 */
object VendorNav {

    enum class Tab { OVERVIEW, VOUCHERS, SCANNER, PROFILE }

    fun bind(
        activity: AppCompatActivity,
        nav: LayoutVendorBottomNavBinding,
        selected: Tab,
        vendorId: () -> String
    ) {
        highlight(activity, nav, selected)

        fun go(target: Tab, cls: Class<*>) {
            if (target == selected) return
            if (target == Tab.OVERVIEW) {
                // Overview is already underneath — just reveal it.
                activity.finish()
                activity.overridePendingTransition(0, 0)
                return
            }
            val id = vendorId()
            if (id.isBlank()) return
            activity.startActivity(
                Intent(activity, cls)
                    .putExtra(VendorHomeActivity.EXTRA_VENDOR_ID, id)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            )
            if (selected != Tab.OVERVIEW) activity.finish()
            activity.overridePendingTransition(0, 0)
        }

        nav.tabOverview.setOnClickListener { go(Tab.OVERVIEW, VendorHomeActivity::class.java) }
        nav.tabVouchers.setOnClickListener { go(Tab.VOUCHERS, RewardRulesActivity::class.java) }
        nav.tabScanner.setOnClickListener { go(Tab.SCANNER, ScanToRedeemActivity::class.java) }
        nav.tabProfile.setOnClickListener { go(Tab.PROFILE, ShopProfileActivity::class.java) }
    }

    private fun highlight(activity: AppCompatActivity, nav: LayoutVendorBottomNavBinding, selected: Tab) {
        val active = ContextCompat.getColor(activity, R.color.sapot_text_primary)
        val idle = ContextCompat.getColor(activity, R.color.sapot_text_secondary)

        data class Item(
            val tab: Tab,
            val pill: android.view.View,
            val icon: android.widget.ImageView,
            val label: android.widget.TextView
        )
        listOf(
            Item(Tab.OVERVIEW, nav.pillOverview, nav.iconOverview, nav.labelOverview),
            Item(Tab.VOUCHERS, nav.pillVouchers, nav.iconVouchers, nav.labelVouchers),
            Item(Tab.SCANNER, nav.pillScanner, nav.iconScanner, nav.labelScanner),
            Item(Tab.PROFILE, nav.pillProfile, nav.iconProfile, nav.labelProfile)
        ).forEach { item ->
            val on = item.tab == selected
            item.pill.setBackgroundResource(if (on) R.drawable.bg_nav_pill_active else 0)
            item.icon.setColorFilter(if (on) active else idle)
            item.label.setTextColor(if (on) active else idle)
            item.label.setTypeface(item.label.typeface, if (on) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
    }
}
