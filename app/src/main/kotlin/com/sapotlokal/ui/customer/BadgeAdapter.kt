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
            binding.tvBadgeTitle.text = badge.title
            binding.ivBadgeIcon.setImageResource(badge.iconRes)

            val context = binding.root.context
            if (item.isUnlocked) {
                // Full color, active status
                binding.layoutBadgeContent.alpha = 1.0f
                binding.ivBadgeIcon.colorFilter = null
                binding.ivBadgeLock.visibility = View.GONE
                binding.tvBadgeStatus.text = "Unlocked"
                binding.tvBadgeStatus.setBackgroundResource(R.drawable.bg_badge_success)
                binding.tvBadgeStatus.setTextColor(ContextCompat.getColor(context, R.color.sapot_success_text))
            } else {
                // 40% opacity grayscale with lock icon
                binding.layoutBadgeContent.alpha = 0.4f
                binding.ivBadgeIcon.colorFilter = grayscaleFilter
                binding.ivBadgeLock.visibility = View.VISIBLE
                binding.tvBadgeStatus.text = "Locked"
                binding.tvBadgeStatus.setBackgroundResource(R.drawable.bg_pill_gray)
                binding.tvBadgeStatus.setTextColor(ContextCompat.getColor(context, R.color.sapot_text_secondary))
            }

            binding.cardBadge.setOnClickListener {
                onBadgeClick(badge, item.isUnlocked)
            }
        }
    }
}

