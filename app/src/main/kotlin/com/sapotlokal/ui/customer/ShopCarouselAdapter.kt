package com.sapotlokal.ui.customer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sapotlokal.data.model.Vendor
import com.sapotlokal.data.repository.VendorPhotoRepository
import com.sapotlokal.databinding.ItemShopCarouselBinding

/**
 * ShopCarouselAdapter — the horizontal "Open now" shop cards on the customer home screen.
 * Each card shows the shop's photo (loaded on demand) or a soft placeholder, its name and category.
 */
class ShopCarouselAdapter(
    private val onShopClick: (Vendor) -> Unit
) : ListAdapter<Vendor, ShopCarouselAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemShopCarouselBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val b: ItemShopCarouselBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(v: Vendor) {
            b.tvName.text = v.vendorName.ifBlank { b.root.context.getString(com.sapotlokal.R.string.k_shop) }
            b.tvCategory.text = com.sapotlokal.data.model.ShopCategory.label(b.root.context, v.vendorCategory)
            com.sapotlokal.util.StatusPill.bind(b.tvStatusPill, v.isOpen)
            b.root.setOnClickListener { onShopClick(v) }

            // Rows are recycled, so remember which shop this card is for before the photo arrives.
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
