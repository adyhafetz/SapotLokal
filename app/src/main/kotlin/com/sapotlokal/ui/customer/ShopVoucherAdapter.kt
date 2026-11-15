package com.sapotlokal.ui.customer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sapotlokal.R
import com.sapotlokal.data.model.Voucher
import com.sapotlokal.databinding.ItemShopVoucherBinding

/**
 * ShopVoucherAdapter — renders available reward rules offered by a specific merchant
 * on the ShopDetailActivity screen.
 *
 * Shows voucher details and clearly informs the customer if they have already earned
 * enough points at this shop to unlock it.
 */
class ShopVoucherAdapter(
    private val onRewardClick: (reward: Voucher, customerPoints: Int) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<ShopVoucherAdapter.ViewHolder>() {

    private val items = mutableListOf<Voucher>()
    private var customerPoints: Int = 0

    fun submitList(vouchers: List<Voucher>) {
        items.clear()
        items.addAll(vouchers)
        notifyDataSetChanged()
    }

    fun updateCustomerPoints(points: Int) {
        if (customerPoints != points) {
            customerPoints = points
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShopVoucherBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], customerPoints, onRewardClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemShopVoucherBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(voucher: Voucher, customerPoints: Int, onClick: (Voucher, Int) -> Unit) {
            // Make the whole card tappable (it previously had no click handler at all).
            binding.root.isClickable = true
            binding.root.isFocusable = true
            binding.root.setOnClickListener { onClick(voucher, customerPoints) }

            binding.tvVoucherTitle.text = voucher.voucherName
            binding.tvVoucherDesc.text = voucher.voucherDescription.ifBlank { binding.root.context.getString(com.sapotlokal.R.string.k_reward_at_shop_dot) }
            binding.tvPointsRequired.text = binding.root.context.getString(com.sapotlokal.R.string.k_n_pts_required, voucher.pointsRequired)

            val context = binding.root.context
            if (customerPoints >= voucher.pointsRequired) {
                binding.tvEligibility.text = "✓ " + binding.root.context.getString(com.sapotlokal.R.string.k_ready_to_claim)
                binding.tvEligibility.setTextColor(ContextCompat.getColor(context, R.color.hu_ok))
            } else {
                val needed = voucher.pointsRequired - customerPoints
                binding.tvEligibility.text = binding.root.context.getString(com.sapotlokal.R.string.k_need_more_pts, needed)
                binding.tvEligibility.setTextColor(ContextCompat.getColor(context, R.color.hu_soft))
            }
        }
    }
}

