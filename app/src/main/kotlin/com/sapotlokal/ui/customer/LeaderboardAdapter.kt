package com.sapotlokal.ui.customer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sapotlokal.AppConfig.CustomerTier
import com.sapotlokal.data.model.LeaderboardMember
import com.sapotlokal.databinding.ItemLeaderboardRowBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * LeaderboardAdapter — RecyclerView adapter for customers ranked 4th onwards.
 */
class LeaderboardAdapter : ListAdapter<LeaderboardMember, LeaderboardAdapter.LeaderboardViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val binding = ItemLeaderboardRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LeaderboardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LeaderboardViewHolder(
        private val binding: ItemLeaderboardRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LeaderboardMember) {
            val tier = CustomerTier.entries.firstOrNull { it.code == item.currentTier }
                ?: CustomerTier.fromLifetimePoints(item.points)

            // Rank number
            binding.tvRank.text = "#${item.rank}"

            // Name with tier emoji
            binding.tvNameWithTier.text = "${item.userName} ${tier.emoji}"
            binding.tvTierTitle.text = tier.title

            // Points formatted
            val formattedPts = NumberFormat.getNumberInstance(Locale.US).format(item.points)
            binding.tvPoints.text = "$formattedPts pts"

            // Avatar Initials
            val initials = getInitials(item.userName)
            if (initials.isNotBlank()) {
                binding.tvInitials.text = initials
                binding.tvInitials.visibility = View.VISIBLE
                binding.ivAvatar.visibility = View.GONE
            } else {
                binding.tvInitials.visibility = View.GONE
                binding.ivAvatar.visibility = View.VISIBLE
            }
        }

        private fun getInitials(name: String): String {
            val cleaned = name.trim().replace(Regex("[^a-zA-Z0-9 ]"), "")
            val parts = cleaned.split(" ").filter { it.isNotBlank() }
            return when {
                parts.isEmpty() -> ""
                parts.size == 1 -> parts[0].take(2).uppercase(Locale.US)
                else -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase(Locale.US)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<LeaderboardMember>() {
        override fun areItemsTheSame(oldItem: LeaderboardMember, newItem: LeaderboardMember): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: LeaderboardMember, newItem: LeaderboardMember): Boolean {
            return oldItem == newItem
        }
    }
}

