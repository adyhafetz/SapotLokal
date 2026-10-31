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
class ShopVoucherAdapter : RecyclerView.Adapter<ShopVoucherAdapter.ViewHolder>() {

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
        holder.bind(items[position], customerPoints)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemShopVoucherBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(voucher: Voucher, customerPoints: Int) {
            binding.tvVoucherTitle.text = voucher.voucherName
            binding.tvVoucherDesc.text = voucher.voucherDescription.ifBlank { "Reward at this shop." }
            binding.tvPointsRequired.text = "${voucher.pointsRequired} pts"

            val context = binding.root.context
            if (customerPoints >= voucher.pointsRequired) {
                binding.tvEligibility.text = "✓ Eligible! Redeem at checkout"
                binding.tvEligibility.setTextColor(ContextCompat.getColor(context, R.color.sapot_success_text))
            } else {
                val needed = voucher.pointsRequired - customerPoints
                binding.tvEligibility.text = "Need $needed more pts"
                binding.tvEligibility.setTextColor(ContextCompat.getColor(context, R.color.sapot_text_secondary))
            }
        }
    }
}

