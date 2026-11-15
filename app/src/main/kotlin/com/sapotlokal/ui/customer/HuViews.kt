package com.sapotlokal.ui.customer

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.sapotlokal.AppConfig
import com.sapotlokal.R
import com.sapotlokal.data.model.Vendor
import com.sapotlokal.data.model.Voucher
import com.sapotlokal.databinding.ItemHuRewardCardBinding
import com.sapotlokal.databinding.ItemHuShopCardBinding
import kotlin.math.abs

/**
 * Hadiah Utama building blocks shared by the customer screens: each shop's fixed colour,
 * the stacked shop card with kopi-cup stamps, the reward card, and the ticked progress bar.
 */
object ShopStyle {

    /** A shop's colour and the text colour that reads on it. */
    data class Colors(val bg: Int, val on: Int)

    private val palette = listOf(
        R.color.hu_shop_pink to false,
        R.color.hu_shop_indigo to false,
        R.color.hu_shop_purple to false,
        R.color.hu_shop_green to false,
        R.color.hu_shop_blue to false,
        R.color.hu_shop_teal to false,
        R.color.hu_shop_berry to false,
        R.color.hu_shop_coral to false
    )

    /** Fixed per shop: the same vendor always gets the same colour. */
    fun colors(context: Context, vendorId: String): Colors {
        val (res, darkText) = palette[abs(vendorId.hashCode()) % palette.size]
        val on = ContextCompat.getColor(context, if (darkText) R.color.hu_ink else R.color.hu_bg)
        return Colors(ContextCompat.getColor(context, res), on)
    }

    /** Two-letter logo for shops without a picture: "Kopi Kita Cafe" → "KK". */
    fun initials(name: String): String {
        val words = name.split(' ', '&').filter { it.isNotBlank() }
        return when {
            words.isEmpty() -> "?"
            words.size == 1 -> words[0].take(2).uppercase()
            else -> (words[0].take(1) + words[1].take(1)).uppercase()
        }
    }

    const val MAX_STAMPS = 8

    /** Shop points this customer gets per visit at [vendor]: the shop's rate × their rank bonus. */
    fun perVisit(vendor: Vendor?, rank: AppConfig.Rank): Int {
        val rate = vendor?.pointsPerVisit?.takeIf { it in AppConfig.ALLOWED_POINTS_PER_VISIT } ?: AppConfig.DEFAULT_POINTS_PER_VISIT
        return AppConfig.shopPointsForVisit(rate, rank)
    }

    /** Draws a row of kopi-cup stamps into [row]. */
    fun fillStamps(row: LinearLayout, total: Int, filled: Int, tint: Int) {
        row.removeAllViews()
        val size = (22 * row.resources.displayMetrics.density).toInt()
        val gap = (8 * row.resources.displayMetrics.density).toInt()
        repeat(total) { i ->
            val cup = ImageView(row.context).apply {
                setImageResource(if (i < filled) R.drawable.ic_cup_full else R.drawable.ic_cup_empty)
                imageTintList = ColorStateList.valueOf(tint)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            row.addView(cup, LinearLayout.LayoutParams(size, size).apply { if (i > 0) marginStart = gap })
        }
    }
}

/**
 * The stacked shop card: shop colour, logo, name, points, cup stamps and one status line.
 * [nextReward] is the cheapest reward the customer hasn't reached; null when the shop has none.
 */
object ShopCard {

    fun inflate(parent: ViewGroup): ItemHuShopCardBinding =
        ItemHuShopCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    fun bind(
        b: ItemHuShopCardBinding,
        vendor: Vendor?,
        vendorId: String,
        balance: Int,
        ready: Boolean,
        nextReward: Voucher?,
        showStatus: Boolean = true,
        readyReward: Voucher? = null,
        meta: String? = null,
        perVisit: Int = AppConfig.DEFAULT_POINTS_PER_VISIT
    ) {
        val ctx = b.root.context
        val c = ShopStyle.colors(ctx, vendorId)
        val name = vendor?.vendorName?.ifBlank { null } ?: ctx.getString(R.string.k_shop)
        b.root.backgroundTintList = ColorStateList.valueOf(c.bg)
        b.tvLogo.text = ShopStyle.initials(name)
        b.tvLogo.setTextColor(c.bg)
        b.tvName.text = name
        b.tvName.setTextColor(c.on)
        b.tvPoints.text = ctx.getString(R.string.hu_n_pts, balance)
        b.tvPoints.setTextColor(c.on)

        // One cup = one visit at this customer's rate here ([perVisit] = shop points per visit × rank
        // bonus), so "visits to go" is true for them. A ready shop shows the visits made, all full.
        val per = perVisit.coerceAtLeast(1)
        val cost = when {
            nextReward != null -> nextReward.pointsRequired
            readyReward != null -> readyReward.pointsRequired
            else -> per * 5
        }
        val earned = balance / per
        val (total, filled) = if (ready) {
            val n = earned.coerceIn(1, ShopStyle.MAX_STAMPS)
            n to n
        } else {
            val visits = ((cost + per - 1) / per).coerceIn(1, ShopStyle.MAX_STAMPS)
            visits to earned.coerceAtMost(visits)
        }
        ShopStyle.fillStamps(b.stamps, total, filled, c.on)

        val status = when {
            ready -> ctx.getString(R.string.hu_reward_ready)
            nextReward != null -> ctx.getString(R.string.hu_to_go_for, nextReward.pointsRequired - balance, nextReward.voucherName)
            vendor != null && !vendor.isOpen -> ctx.getString(R.string.hu_closed_now)
            else -> ctx.getString(R.string.hu_no_rewards_yet)
        }
        b.tvStatus.text = if (meta.isNullOrBlank()) status else "$meta · $status"
        b.tvStatus.setTextColor(ColorUtils.setAlphaComponent(c.on, 235))
        b.tvStatus.visibility = if (showStatus) View.VISIBLE else View.GONE
        b.tvReady.visibility = if (ready) View.VISIBLE else View.GONE
        b.tvReady.backgroundTintList = ColorStateList.valueOf(c.on)
        b.tvReady.setTextColor(c.bg)
        b.root.contentDescription = ctx.getString(R.string.hu_shop_card_cd, name, balance, status)
    }
}

/** The reward card: shop logo + name, cost, reward name, detail, photo, one action. */
object RewardCard {

    fun inflate(parent: ViewGroup): ItemHuRewardCardBinding =
        ItemHuRewardCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    fun bind(
        b: ItemHuRewardCardBinding,
        vendorId: String,
        shopName: String,
        cost: Int,
        title: String,
        detail: String,
        action: String?,
        onAction: (() -> Unit)?
    ) {
        val ctx = b.root.context
        val c = ShopStyle.colors(ctx, vendorId)
        b.root.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(c.bg, 34))
        b.tvLogo.text = ShopStyle.initials(shopName)
        b.tvLogo.backgroundTintList = ColorStateList.valueOf(c.bg)
        b.tvLogo.setTextColor(c.on)
        b.tvShop.text = shopName
        b.tvCost.text = ctx.getString(R.string.hu_n_pts, cost)
        b.tvTitle.text = title
        b.tvDetail.text = detail
        if (action != null && onAction != null) {
            b.btnAction.text = action
            b.btnAction.setOnClickListener { onAction() }
            b.btnAction.visibility = View.VISIBLE
        } else {
            b.btnAction.visibility = View.GONE
        }
        // Photo: the shop's own picture, otherwise its colour with its logo.
        b.photoFallback.setBackgroundColor(ColorUtils.setAlphaComponent(c.bg, 60))
        b.tvPhotoLogo.text = ShopStyle.initials(shopName)
        b.tvPhotoLogo.setTextColor(c.bg)
        // The photo area only appears when the shop has uploaded a picture.
        b.photoFrame.visibility = View.GONE
        b.ivPhoto.tag = vendorId
        b.ivPhoto.setImageDrawable(null)
        com.sapotlokal.data.repository.VendorPhotoRepository.load(vendorId) { bmp ->
            if (bmp != null && b.ivPhoto.tag == vendorId) {
                b.ivPhoto.setImageBitmap(bmp)
                b.photoFrame.visibility = View.VISIBLE
            }
        }
    }
}

/** The one "How points work" explanation, linked from Home, Shops, the Shop page, Rewards and Play. */
object HowPoints {
    fun show(context: Context) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle(R.string.hu_how_points_title)
            .setMessage(R.string.hu_how_points_body)
            .setPositiveButton(R.string.hu_got_it, null)
            .show()
    }
}

/** Level ticks and the line under the bar, shared by Home and Play. */
object LevelDisplay {
    fun bind(bar: HuTickBar, line: android.widget.TextView, lifetime: Int) {
        val ctx = bar.context
        val tier = AppConfig.CustomerTier.fromLifetimePoints(lifetime)
        val next = tier.nextTier
        val level = tier.ordinal + 1
        // One scale from 0 to the next level, ticked at every level threshold on the way.
        val thresholds = AppConfig.CustomerTier.entries.map { it.minPoints }
        if (next == null) {
            bar.set(1, 1, emptyList())
            line.text = ctx.getString(R.string.hu_level_top, level, tier.title)
        } else {
            bar.set(lifetime, next.minPoints, thresholds.filter { it <= next.minPoints })
            line.text = ctx.getString(R.string.hu_level_line, level, tier.title, next.minPoints - lifetime, next.title)
        }
    }
}

/**
 * Progress bar with labelled ticks (levels on Home and Play, reward costs on a shop page).
 * Values are absolute: [max] is the right end, [value] the fill, [ticks] the marks.
 */
class HuTickBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var max: Int = 100
    var value: Int = 0
    var ticks: List<Int> = emptyList()

    private val d = resources.displayMetrics.density
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.hu_line) }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.hu_accent) }
    private val notch = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.hu_bg) }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.hu_soft)
        textSize = 12 * resources.displayMetrics.scaledDensity
        typeface = ResourcesCompat.getFont(context, R.font.figtree_bold) ?: Typeface.DEFAULT_BOLD
    }

    fun set(value: Int, max: Int, ticks: List<Int>) {
        this.value = value; this.max = max.coerceAtLeast(1); this.ticks = ticks
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = (8 * d + 8 * d + label.textSize + 2 * d).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), h)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val barH = 8 * d
        val r = barH / 2
        canvas.drawRoundRect(RectF(0f, 0f, w, barH), r, r, track)
        val fw = (w * value.coerceIn(0, max) / max).coerceAtLeast(if (value > 0) barH else 0f)
        if (fw > 0) canvas.drawRoundRect(RectF(0f, 0f, fw, barH), r, r, fill)
        val y = barH + 6 * d + label.textSize
        ticks.forEach { t ->
            val x = w * t.coerceIn(0, max) / max
            if (t in 1 until max) canvas.drawRect(x - d, 0f, x + d, barH, notch)
            val text = t.toString()
            val tw = label.measureText(text)
            val tx = (x - tw / 2).coerceIn(0f, w - tw)
            canvas.drawText(text, tx, y, label)
        }
    }
}

/** Filter chip in the Hadiah Utama style: ink when selected, quiet grey fill otherwise. */
object HuChip {
    fun make(context: Context, label: String, checked: Boolean, onClick: () -> Unit): com.google.android.material.chip.Chip {
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        val ink = ContextCompat.getColor(context, R.color.hu_ink)
        val fill = ContextCompat.getColor(context, R.color.hu_fill)
        val white = ContextCompat.getColor(context, R.color.hu_bg)
        return com.google.android.material.chip.Chip(context).apply {
            text = label
            isCheckable = true
            isChecked = checked
            isCheckedIconVisible = false
            chipBackgroundColor = ColorStateList(states, intArrayOf(ink, fill))
            setTextColor(ColorStateList(states, intArrayOf(white, ink)))
            chipStrokeWidth = 0f
            textSize = 15f
            typeface = ResourcesCompat.getFont(context, R.font.figtree_bold)
            chipMinHeight = 44f * context.resources.displayMetrics.density
            setEnsureMinTouchTargetSize(true)
            setOnClickListener { onClick() }
        }
    }
}

/** Compact reward row used on the Shop page and in Rewards. */
object RewardRow {
    fun inflate(parent: ViewGroup): com.sapotlokal.databinding.ItemHuRewardRowBinding =
        com.sapotlokal.databinding.ItemHuRewardRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    fun bind(
        b: com.sapotlokal.databinding.ItemHuRewardRowBinding,
        vendorId: String,
        shopName: String,
        title: String,
        sub: String,
        state: String,
        stateIsGood: Boolean,
        action: String?,
        onAction: (() -> Unit)?
    ) {
        val ctx = b.root.context
        val c = ShopStyle.colors(ctx, vendorId)
        b.tvName.text = title
        b.tvSub.text = sub
        b.tvState.text = state
        b.tvState.setTextColor(ContextCompat.getColor(ctx, if (stateIsGood) R.color.hu_accent_text else R.color.hu_soft))
        b.tvThumbLogo.text = ShopStyle.initials(shopName)
        b.tvThumbLogo.setTextColor(c.bg)
        b.tvThumbLogo.setBackgroundColor(ColorUtils.setAlphaComponent(c.bg, 50))
        if (action != null && onAction != null) {
            b.btnAction.text = action
            b.btnAction.setOnClickListener { onAction() }
            b.btnAction.visibility = View.VISIBLE
        } else {
            b.btnAction.visibility = View.GONE
        }
        b.root.contentDescription = "$title, $sub, $state"
        b.ivThumb.tag = vendorId
        b.ivThumb.setImageDrawable(null)
        com.sapotlokal.data.repository.VendorPhotoRepository.load(vendorId) { bmp ->
            if (bmp != null && b.ivThumb.tag == vendorId) b.ivThumb.setImageBitmap(bmp)
        }
    }
}
