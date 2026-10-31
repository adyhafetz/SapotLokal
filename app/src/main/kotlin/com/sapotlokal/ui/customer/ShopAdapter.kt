package com.sapotlokal.ui.customer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sapotlokal.R
import com.sapotlokal.data.model.Vendor
import com.sapotlokal.databinding.ItemShopCardBinding

/**
 * ShopAdapter — binds a list of vendors to item_shop_card.xml cards for FindShopsActivity.
 *
 * Each card shows:
 * - Category avatar & icon
 * - Shop name
 * - Real-time OPEN / CLOSED status chip (Phase 5)
 * - Category and address subtitle
 * - Customer's current balance at that shop
 * - Click action navigating to ShopDetailActivity (Phase 5)
 */
class ShopAdapter(
    private var perShopBalance: Map<String, Int>,
    private val onShopClick: (vendorID: String) -> Unit = {}
) : RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {

    private val items = mutableListOf<Vendor>()

    fun submitList(vendors: List<Vendor>) {
        items.clear()
        items.addAll(vendors)
        notifyDataSetChanged()
    }

    fun updateBalances(balances: Map<String, Int>) {
        perShopBalance = balances
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val binding = ItemShopCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ShopViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ShopViewHolder(private val binding: ItemShopCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(vendor: Vendor) {
            binding.tvShopName.text = vendor.vendorName.ifBlank { "Local Merchant" }

            // Category & Address subtitle
            val subtitle = if (vendor.vendorAddress.isNotBlank()) {
                "${vendor.vendorCategory} • ${vendor.vendorAddress}"
            } else {
                vendor.vendorCategory.ifBlank { "Local Merchant" }
            }
            binding.tvShopSubtitle.text = subtitle

            // Real-time live status chip
            if (vendor.isOpen) {
                binding.tvShopStatus.text = "● OPEN"
                binding.tvShopStatus.setBackgroundResource(R.drawable.bg_status_chip_open)
            } else {
                binding.tvShopStatus.text = "● CLOSED"
                binding.tvShopStatus.setBackgroundResource(R.drawable.bg_status_chip_closed)
            }

            // Customer points earned at this shop
            val points = perShopBalance[vendor.vendorID] ?: 0
            binding.tvShopPoints.text = "$points pts"

            // Category avatar & icon
            val (bgRes, iconRes) = categoryDrawables(vendor.vendorCategory)
            binding.avatarContainer.setBackgroundResource(bgRes)
            binding.ivCategoryIcon.setImageResource(iconRes)

            // Click listener
            binding.root.setOnClickListener {
                onShopClick(vendor.vendorID)
            }
        }

        private fun categoryDrawables(category: String): Pair<Int, Int> {
            val c = category.lowercase()
            return when {
                c.contains("kopi") || c.contains("caf") || c.contains("coffee") || c.contains("tea") ->
                    R.drawable.bg_shop_icon_coffee to R.drawable.ic_coffee
                c.contains("baker") || c.contains("roti") || c.contains("cake") || c.contains("pastry") ->
                    R.drawable.bg_shop_icon_croissant to R.drawable.ic_croissant
                else ->
                    R.drawable.bg_shop_icon_croissant to R.drawable.ic_store
            }
        }
    }
}
