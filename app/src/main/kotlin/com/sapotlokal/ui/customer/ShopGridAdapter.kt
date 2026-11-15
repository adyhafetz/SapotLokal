package com.sapotlokal.ui.customer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sapotlokal.data.model.Vendor
import com.sapotlokal.data.repository.VendorPhotoRepository
import com.sapotlokal.databinding.ItemShopGridBinding
import com.sapotlokal.util.StatusPill

/** ShopGridAdapter — the two-column shop grid on the Explore screen. */
class ShopGridAdapter(
    private val onShopClick: (Vendor) -> Unit
) : ListAdapter<Vendor, ShopGridAdapter.ViewHolder>(Diff) {

    private var balances: Map<String, Int> = emptyMap()

    /** The customer's points per shop, shown as a small pill on shops they have visited. */
    fun updateBalances(map: Map<String, Int>) {
        balances = map
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemShopGridBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val b: ItemShopGridBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(v: Vendor) {
            b.tvName.text = v.vendorName.ifBlank { b.root.context.getString(com.sapotlokal.R.string.k_shop) }
            b.tvCategory.text = listOf(com.sapotlokal.data.model.ShopCategory.label(b.root.context, v.vendorCategory), v.vendorAddress)
                .filter { it.isNotBlank() }.joinToString(" · ")
            StatusPill.bind(b.tvStatusPill, v.isOpen)
            b.root.setOnClickListener { onShopClick(v) }

            val points = balances[v.vendorID] ?: 0
            if (points > 0) {
                b.tvPoints.text = b.root.context.getString(com.sapotlokal.R.string.k_n_points, points)
                b.tvPoints.visibility = View.VISIBLE
            } else {
                b.tvPoints.visibility = View.GONE
            }

            b.ivPhoto.tag = v.vendorID
            b.ivPhoto.setImageDrawable(null)
            b.placeholder.visibility = View.VISIBLE
            VendorPhotoRepository.load(v.vendorID) { bmp ->
                if (bmp != null && b.ivPhoto.tag == v.vendorID) {
                    b.ivPhoto.setImageBitmap(bmp)
                    b.placeholder.visibility = View.GONE
                }
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Vendor>() {
        override fun areItemsTheSame(a: Vendor, b: Vendor) = a.vendorID == b.vendorID
        override fun areContentsTheSame(a: Vendor, b: Vendor) = a == b
    }
}
