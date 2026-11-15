package com.sapotlokal.ui.customer

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.sapotlokal.R
import com.sapotlokal.databinding.LayoutCustomerBottomNavBinding

/**
 * CustomerNav — the customer bottom bar: Home · Shops · Scan · Rewards · Play.
 * Home is the root of the customer back stack; the other four tabs replace each other, so the
 * stack never grows and Back always returns to Home. Me (profile) opens from Home and Play,
 * not from the bar. [Tab.NONE] shows the bar with nothing highlighted.
 */
object CustomerNav {

    enum class Tab { HOME, SHOPS, SCAN, REWARDS, PLAY, NONE }

    private const val MAX_CONTENT_DP = 640

    fun bind(activity: AppCompatActivity, nav: LayoutCustomerBottomNavBinding, selected: Tab) {
        adaptForTablet(activity, nav)
        highlight(activity, nav, selected)

        fun go(target: Tab, cls: Class<*>) {
            if (target == selected) return
            if (target == Tab.HOME) {
                // Home is always underneath: finishing returns to it.
                if (activity !is CustomerHomeActivity) {
                    activity.finish()
                    activity.overridePendingTransition(0, 0)
                }
                return
            }
            activity.startActivity(Intent(activity, cls).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
            if (selected != Tab.HOME && selected != Tab.NONE) activity.finish()
            activity.overridePendingTransition(0, 0)
        }

        nav.tabHome.setOnClickListener { go(Tab.HOME, CustomerHomeActivity::class.java) }
        nav.tabShops.setOnClickListener { go(Tab.SHOPS, FindShopsActivity::class.java) }
        nav.tabScan.setOnClickListener { go(Tab.SCAN, ScanActivity::class.java) }
        nav.scanFab.setOnClickListener { go(Tab.SCAN, ScanActivity::class.java) }
        nav.tabRewards.setOnClickListener { go(Tab.REWARDS, MyVouchersActivity::class.java) }
        nav.tabPlay.setOnClickListener { go(Tab.PLAY, ChallengesActivity::class.java) }
    }

    /**
     * Tablets (smallest width ≥ 600dp) get a navigation rail on the start edge instead of a
     * phone bottom bar, with Scan as the raised button at the top of the rail.
     */
    private fun adaptForTablet(activity: AppCompatActivity, nav: LayoutCustomerBottomNavBinding) {
        if (activity.resources.configuration.smallestScreenWidthDp < 600) return
        val root = nav.root
        val parent = root.parent as? android.widget.LinearLayout ?: return
        if (parent.orientation == android.widget.LinearLayout.HORIZONTAL) return
        val d = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val match = android.view.ViewGroup.LayoutParams.MATCH_PARENT

        parent.orientation = android.widget.LinearLayout.HORIZONTAL
        parent.removeView(root)
        parent.addView(root, 0, android.widget.LinearLayout.LayoutParams(dp(96), match))
        for (i in 1 until parent.childCount) {
            val child = parent.getChildAt(i)
            child.layoutParams = android.widget.LinearLayout.LayoutParams(0, match, 1f)
            // Keep reading width comfortable: centre a column of at most 640dp beside the rail.
            val spare = activity.resources.configuration.screenWidthDp - 96 - MAX_CONTENT_DP
            if (spare > 0 && child is android.view.ViewGroup && child.childCount > 0) {
                val inner = child.getChildAt(0)
                val side = dp(spare / 2)
                inner.setPaddingRelative(inner.paddingStart + side, inner.paddingTop, inner.paddingEnd + side, inner.paddingBottom)
            }
        }

        nav.navBar.orientation = android.widget.LinearLayout.VERTICAL
        nav.navBar.layoutParams = android.widget.FrameLayout.LayoutParams(match, match)
        nav.navBar.setBackgroundResource(R.drawable.bg_hu_rail)
        nav.navBar.setPadding(0, dp(108), 0, 0)
        listOf(nav.tabHome, nav.tabShops, nav.tabRewards, nav.tabPlay).forEach {
            it.layoutParams = android.widget.LinearLayout.LayoutParams(match, dp(84))
        }
        nav.tabScan.visibility = View.GONE
        nav.scanFab.layoutParams = android.widget.FrameLayout.LayoutParams(
            dp(64), dp(64), android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
        ).apply { topMargin = dp(24) }
    }

    private fun highlight(activity: AppCompatActivity, nav: LayoutCustomerBottomNavBinding, selected: Tab) {
        val active = ContextCompat.getColor(activity, R.color.hu_ink)
        val idle = ContextCompat.getColor(activity, R.color.hu_soft)

        class Item(val tab: Tab, val bar: View, val icon: ImageView?, val label: TextView)
        listOf(
            Item(Tab.HOME, nav.barHome, nav.iconHome, nav.labelHome),
            Item(Tab.SHOPS, nav.barShops, nav.iconShops, nav.labelShops),
            Item(Tab.SCAN, nav.barScan, null, nav.labelScan),
            Item(Tab.REWARDS, nav.barRewards, nav.iconRewards, nav.labelRewards),
            Item(Tab.PLAY, nav.barPlay, nav.iconPlay, nav.labelPlay)
        ).forEach {
            val on = it.tab == selected
            it.bar.visibility = if (on && it.tab != Tab.SCAN) View.VISIBLE else View.INVISIBLE
            it.icon?.setColorFilter(if (on) active else idle)
            it.label.setTextColor(if (on) active else idle)
            it.label.isSelected = on
            (it.label.parent as View).contentDescription =
                if (on) activity.getString(R.string.nav_tab_selected_cd, it.label.text) else it.label.text
        }
    }
}
