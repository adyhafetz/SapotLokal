package com.sapotlokal.ui.customer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.inSpans
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sapotlokal.R
import com.sapotlokal.data.model.VoucherStatus
import com.sapotlokal.data.model.VoucherUser
import com.sapotlokal.databinding.ItemVoucherCardBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UI presentation model for a voucher in the My Vouchers screen.
 */
data class VoucherUiModel(
    val voucherUser: VoucherUser,
    val vendorName: String,
    val isBestChoice: Boolean,
    val isLocked: Boolean,
    val currentBalance: Int,
    /** Non-null = this row is an AVAILABLE reward (a shop's rule the customer hasn't earned yet), not an issued voucher. */
    val catalogRule: com.sapotlokal.data.model.Voucher? = null,
    /** Shop heading shown above this card (first reward of each shop when listing all shops). */
    val groupHeader: String? = null
)

/**
 * VoucherAdapter — binds voucher cards with shop names, status indicators,
 * "Best Choice" highlights, and locked-state warnings.
 */
class VoucherAdapter(
    private val onVoucherClick: (VoucherUiModel) -> Unit
) : ListAdapter<VoucherUiModel, VoucherAdapter.VoucherViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
        val binding = ItemVoucherCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VoucherViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VoucherViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VoucherViewHolder(
        private val binding: ItemVoucherCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun bind(item: VoucherUiModel) {
            val vu = item.voucherUser
            binding.tvShopName.text = item.vendorName.ifBlank { binding.root.context.getString(com.sapotlokal.R.string.k_local_shop) }
            binding.tvGroupHeader.text = item.groupHeader.orEmpty()
            binding.tvGroupHeader.visibility = if (item.groupHeader != null) android.view.View.VISIBLE else android.view.View.GONE
            binding.tvRewardTitle.text = vu.voucherTitle.ifBlank { binding.root.context.getString(com.sapotlokal.R.string.k_reward_voucher) }
            binding.tvPointsRequired.text = androidx.core.text.buildSpannedString {
                inSpans(
                    android.text.style.AbsoluteSizeSpan(20, true),
                    android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    android.text.style.ForegroundColorSpan(
                        if (item.catalogRule != null && item.currentBalance < item.catalogRule.pointsRequired)
                            android.graphics.Color.parseColor("#91888A")
                        else android.graphics.Color.parseColor("#543A3A")
                    )
                ) { append(vu.pointsRequiredSnapshot.toString()) }
                append(" " + binding.root.context.getString(com.sapotlokal.R.string.pts_label_long))
            }
            binding.tvVoucherCode.text = vu.voucherCode

            val isExpired = vu.voucherExpiryDate in 1 until System.currentTimeMillis()
            val expiryStr = if (vu.voucherExpiryDate > 0L) {
                binding.root.context.getString(com.sapotlokal.R.string.k_expires_on, dateFormat.format(Date(vu.voucherExpiryDate)))
            } else {
                binding.root.context.getString(com.sapotlokal.R.string.k_no_expiry)
            }
            binding.tvExpiryDate.text = expiryStr

            val rule = item.catalogRule
            val claimable = rule != null && item.currentBalance >= rule.pointsRequired
            binding.btnViewQr.text = if (rule == null) binding.root.context.getString(com.sapotlokal.R.string.k_show_qr) else if (claimable) binding.root.context.getString(com.sapotlokal.R.string.k_claim) else binding.root.context.getString(com.sapotlokal.R.string.k_view_shop)

            when {
                rule != null -> { // AVAILABLE reward: progress towards it, tap to open the shop
                    val needed = (rule.pointsRequired - item.currentBalance).coerceAtLeast(0)
                    binding.tvStatusBadge.text = if (claimable) binding.root.context.getString(com.sapotlokal.R.string.k_ready_to_claim_pill) else binding.root.context.getString(com.sapotlokal.R.string.k_available)
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_leaf)
                    binding.tvBestChoiceBadge.visibility = View.GONE
                    binding.tvVoucherCode.text = rule.voucherDescription.ifBlank { binding.root.context.getString(com.sapotlokal.R.string.k_reward_at_shop) }
                    binding.tvExpiryDate.text = if (claimable) binding.root.context.getString(com.sapotlokal.R.string.k_valid_30_after_claim) else binding.root.context.getString(com.sapotlokal.R.string.k_earn_to_unlock)
                    binding.tvLockedNotice.text = if (needed > 0) {
                        binding.root.context.getString(com.sapotlokal.R.string.k_n_more_to_unlock, needed)
                    } else {
                        binding.root.context.getString(com.sapotlokal.R.string.k_enough_to_claim)
                    }
                    binding.tvLockedNotice.visibility = View.VISIBLE
                    binding.btnViewQr.isEnabled = true
                    binding.btnViewQr.alpha = 1f
                    binding.cardVoucher.alpha = 1f
                }
                vu.voucherStatus == VoucherStatus.USED -> {
                    binding.tvStatusBadge.text = binding.root.context.getString(com.sapotlokal.R.string.k_redeemed)
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_gray)
                    binding.tvBestChoiceBadge.visibility = View.GONE
                    binding.tvLockedNotice.visibility = View.GONE
                    binding.btnViewQr.isEnabled = false
                    binding.btnViewQr.alpha = 0.5f
                    binding.cardVoucher.alpha = 0.65f
                }
                isExpired || vu.voucherStatus == VoucherStatus.EXPIRED -> {
                    binding.tvStatusBadge.text = binding.root.context.getString(com.sapotlokal.R.string.k_expired)
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_gray)
                    binding.tvBestChoiceBadge.visibility = View.GONE
                    binding.tvLockedNotice.visibility = View.GONE
                    binding.btnViewQr.isEnabled = false
                    binding.btnViewQr.alpha = 0.5f
                    binding.cardVoucher.alpha = 0.65f
                }
                item.isLocked -> {
                    binding.tvStatusBadge.text = binding.root.context.getString(com.sapotlokal.R.string.k_locked)
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_gray)
                    binding.tvBestChoiceBadge.visibility = View.GONE
                    val needed = (vu.pointsRequiredSnapshot - item.currentBalance).coerceAtLeast(1)
                    binding.tvLockedNotice.text = binding.root.context.getString(com.sapotlokal.R.string.k_n_more_to_unlock, needed)
                    binding.btnViewQr.text = binding.root.context.getString(com.sapotlokal.R.string.k_not_enough_points)
                    binding.tvLockedNotice.visibility = View.VISIBLE
                    binding.btnViewQr.isEnabled = false
                    binding.btnViewQr.alpha = 0.5f
                    binding.cardVoucher.alpha = 0.85f
                }
                else -> { // ACTIVE and unlocked
                    binding.tvStatusBadge.text = binding.root.context.getString(com.sapotlokal.R.string.k_active)
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_leaf)
                    binding.tvBestChoiceBadge.visibility = if (item.isBestChoice) View.VISIBLE else View.GONE
                    binding.tvLockedNotice.text = binding.root.context.getString(com.sapotlokal.R.string.k_check_ready_use)
                    binding.tvLockedNotice.visibility = View.VISIBLE
                    binding.btnViewQr.isEnabled = true
                    binding.btnViewQr.alpha = 1f
                    binding.cardVoucher.alpha = 1f
                }
            }

            // Not claimable / usable yet → whole card goes grey so it reads as inactive.
            val inactive = (rule != null && !claimable) ||
                (rule == null && item.isLocked && vu.voucherStatus == VoucherStatus.ACTIVE && !isExpired)
            if (rule != null && !claimable) {
                val ctx = binding.root.context
                binding.tvStatusBadge.text = ctx.getString(R.string.k_locked)
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_gray)
                binding.btnViewQr.text = ctx.getString(R.string.k_locked)
                binding.tvExpiryDate.text = ctx.getString(R.string.k_valid_days_after_claim, com.sapotlokal.AppConfig.VOUCHER_EXPIRY_DAYS)
                binding.btnViewQr.isEnabled = false
                binding.btnViewQr.alpha = 0.5f
            }
            applyInactiveStyle(inactive)

            binding.cardVoucher.setOnClickListener { onVoucherClick(item) }
            binding.btnViewQr.setOnClickListener { onVoucherClick(item) }
        }

        /** Grey, faded look for rewards that can't be used yet. Everything else is reset (cards are reused). */
        private fun applyInactiveStyle(inactive: Boolean) {
            val ctx = binding.root.context
            val primary = androidx.core.content.ContextCompat.getColor(ctx, R.color.hu_ink)
            val muted = androidx.core.content.ContextCompat.getColor(ctx, R.color.hu_soft)
            val surface = androidx.core.content.ContextCompat.getColor(ctx, R.color.hu_bg)
            binding.cardVoucher.setCardBackgroundColor(if (inactive) android.graphics.Color.parseColor("#F1F1F1") else surface)
            if (inactive) binding.cardVoucher.alpha = 0.8f
            binding.iconBox.setBackgroundResource(if (inactive) R.drawable.bg_pill_gray else R.drawable.bg_nav_pill_active)
            binding.ivRewardIcon.imageTintList = android.content.res.ColorStateList.valueOf(if (inactive) muted else primary)
            binding.tvRewardTitle.setTextColor(if (inactive) muted else primary)
            binding.tvLockedNotice.setTextColor(if (inactive) muted else primary)
            if (inactive) binding.tvPointsRequired.setTextColor(muted)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<VoucherUiModel>() {
        override fun areItemsTheSame(oldItem: VoucherUiModel, newItem: VoucherUiModel): Boolean {
            return oldItem.voucherUser.voucherUserID == newItem.voucherUser.voucherUserID
        }

        override fun areContentsTheSame(oldItem: VoucherUiModel, newItem: VoucherUiModel): Boolean {
            return oldItem == newItem
        }
    }
}

