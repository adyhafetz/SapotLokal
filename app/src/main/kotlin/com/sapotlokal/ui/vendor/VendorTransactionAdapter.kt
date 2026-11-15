package com.sapotlokal.ui.vendor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sapotlokal.R
import com.sapotlokal.data.model.Point
import com.sapotlokal.data.model.PointSource
import com.sapotlokal.databinding.ItemVendorTransactionBinding

/**
 * VendorTransactionAdapter — displays recent ledger activity for a shop owner.
 */
class VendorTransactionAdapter :
    ListAdapter<Point, VendorTransactionAdapter.TransactionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemVendorTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemVendorTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(point: Point) {
            val context = itemView.context
            val isEarned = point.pointEarnedAmount > 0

            if (isEarned) {
                binding.ivTxIcon.setImageResource(R.drawable.ic_store)
                binding.iconContainer.background = ContextCompat.getDrawable(
                    context, R.drawable.bg_pill_yellow
                )
                binding.tvTxTitle.text = when (point.pointSource) {
                    PointSource.FIRST_VISIT_BONUS -> binding.root.context.getString(com.sapotlokal.R.string.kv_tx_first_visit)
                    PointSource.STREAK_BONUS -> binding.root.context.getString(com.sapotlokal.R.string.kv_tx_streak)
                    else -> binding.root.context.getString(com.sapotlokal.R.string.kv_tx_visit)
                }
                binding.tvTxPoints.text = binding.root.context.getString(com.sapotlokal.R.string.kv_plus_pts, point.pointEarnedAmount)
                binding.tvTxPoints.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
            } else {
                binding.ivTxIcon.setImageResource(R.drawable.ic_wallet)
                binding.iconContainer.background = ContextCompat.getDrawable(
                    context, R.drawable.bg_pill_bakery
                )
                binding.tvTxTitle.text = if (point.pointSource == PointSource.REDEMPTION) {
                    binding.root.context.getString(com.sapotlokal.R.string.kv_tx_redeemed)
                } else {
                    binding.root.context.getString(com.sapotlokal.R.string.kv_tx_spent)
                }
                binding.tvTxPoints.text = binding.root.context.getString(com.sapotlokal.R.string.kv_minus_pts, point.pointRedeemedAmount)
                binding.tvTxPoints.setTextColor(ContextCompat.getColor(context, R.color.colorError))
            }

            val shortUser = if (point.userID.length > 8) {
                point.userID.take(8) + "…"
            } else if (point.userID.isNotBlank()) {
                point.userID
            } else {
                binding.root.context.getString(com.sapotlokal.R.string.kv_guest)
            }
            binding.tvTxSubtitle.text = binding.root.context.getString(com.sapotlokal.R.string.kv_customer_id, shortUser)
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Point>() {
        override fun areItemsTheSame(oldItem: Point, newItem: Point): Boolean =
            oldItem.pointID == newItem.pointID

        override fun areContentsTheSame(oldItem: Point, newItem: Point): Boolean =
            oldItem == newItem
    }
}

