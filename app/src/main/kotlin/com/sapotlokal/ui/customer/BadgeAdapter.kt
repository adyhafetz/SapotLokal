package com.sapotlokal.ui.customer

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sapotlokal.R
import com.sapotlokal.data.model.BadgeDef
import com.sapotlokal.databinding.ItemBadgeShowcaseBinding

data class BadgeShowcaseItem(
    val badge: BadgeDef,
    val isUnlocked: Boolean
)

/**
 * Adapter for rendering the 8 collectible badges in CustomerProfileActivity.
 * Unlocked badges display in full vibrant color.
 * Locked badges display at 40% opacity with grayscale filter and a lock icon overlay.
 */
class BadgeAdapter(
    private val onBadgeClick: (BadgeDef, Boolean) -> Unit
) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    private val items = mutableListOf<BadgeShowcaseItem>()
    private val grayscaleFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })

    fun submitList(newItems: List<BadgeShowcaseItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val binding = ItemBadgeShowcaseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BadgeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class BadgeViewHolder(
        private val binding: ItemBadgeShowcaseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BadgeShowcaseItem) {
            val badge = item.badge
            binding.tvBadgeTitle.text = com.sapotlokal.util.ContentText.badgeTitle(binding.root.context, badge)
            binding.tvBadgeDesc.text = com.sapotlokal.util.ContentText.badgeDescription(binding.root.context, badge)
            binding.ivBadgeIcon.setImageResource(badge.iconRes)

            val context = binding.root.context
            val accent = BadgeStyle.accent(badge)
            if (item.isUnlocked) {
                // Full colour with a soft glow in the badge's own colour.
                binding.layoutBadgeContent.alpha = 1.0f
                binding.ivBadgeIcon.colorFilter = null
                binding.ivBadgeLock.visibility = View.GONE
                binding.ivBadgeGlow.background = BadgeStyle.halo(context, accent)
                binding.ivBadgeGlow.visibility = View.VISIBLE
                binding.cardBadge.setCardBackgroundColor(androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 18))
                binding.cardBadge.strokeColor = androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 110)
                binding.cardBadge.strokeWidth = (1.5f * context.resources.displayMetrics.density).toInt()
                binding.tvBadgeStatus.text = binding.root.context.getString(com.sapotlokal.R.string.k_unlocked)
                binding.tvBadgeStatus.background = BadgeStyle.pill(context, accent)
                binding.tvBadgeStatus.setTextColor(accent)
            } else {
                // Greyed out with a lock, no glow.
                binding.layoutBadgeContent.alpha = 0.5f
                binding.ivBadgeIcon.colorFilter = grayscaleFilter
                binding.ivBadgeLock.visibility = View.VISIBLE
                binding.ivBadgeGlow.visibility = View.GONE
                binding.cardBadge.setCardBackgroundColor(ContextCompat.getColor(context, R.color.hu_bg))
                binding.cardBadge.strokeColor = ContextCompat.getColor(context, R.color.hu_line)
                binding.cardBadge.strokeWidth = context.resources.displayMetrics.density.toInt()
                binding.tvBadgeStatus.text = binding.root.context.getString(com.sapotlokal.R.string.k_locked)
                binding.tvBadgeStatus.setBackgroundResource(R.drawable.bg_pill_gray)
                binding.tvBadgeStatus.setTextColor(ContextCompat.getColor(context, R.color.hu_soft))
            }
            binding.cardBadge.setOnClickListener {
                onBadgeClick(badge, item.isUnlocked)
            }
        }
    }
}

