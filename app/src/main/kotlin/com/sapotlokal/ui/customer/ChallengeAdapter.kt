package com.sapotlokal.ui.customer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sapotlokal.data.model.BadgeDef
import com.sapotlokal.data.model.WeeklyMissionWithProgress
import com.sapotlokal.databinding.ItemChallengeCardBinding
import com.sapotlokal.util.MissionDateUtils

/**
 * ChallengeAdapter — Binds WeeklyMissionWithProgress items to item_challenge_card.xml.
 *
 * Implements Phase 3 Specification:
 * - Displays title, description, reward points, progress bar (current/target).
 * - Badge preview pill (if badge attached).
 * - Countdown timer ("X days left" / "X hours left").
 * - Action button:
 *   - currentProgress < targetCount ➔ "Start Now ➔"
 *   - currentProgress >= targetCount && !isClaimed ➔ "Claim Reward 🎉"
 *   - isClaimed ➔ "✓ Claimed" disabled state
 */
class ChallengeAdapter(
    private val onStartClick: (WeeklyMissionWithProgress) -> Unit,
    private val onClaimClick: (WeeklyMissionWithProgress) -> Unit
) : ListAdapter<WeeklyMissionWithProgress, ChallengeAdapter.MissionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MissionViewHolder {
        val binding = ItemChallengeCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MissionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MissionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MissionViewHolder(
        private val binding: ItemChallengeCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WeeklyMissionWithProgress) {
            val mission = item.mission
            val target = mission.targetCount
            val current = item.progress.coerceAtMost(target)

            binding.tvChallengeTitle.text = mission.title
            binding.tvChallengeDesc.text = mission.description
            binding.tvRewardBadge.text = "+${mission.rewardPoints} pts"

            binding.pbChallenge.max = if (target > 0) target else 1
            binding.pbChallenge.progress = current
            binding.tvProgressText.text = "$current / $target"

            // Countdown timer
            binding.tvCountdownTimer.text = "⏱ ${MissionDateUtils.formatTimeRemaining(item.weekExpiresAtMillis)}"

            // Attached badge preview pill
            if (!mission.rewardBadgeKey.isNullOrBlank()) {
                val badge = BadgeDef.fromId(mission.rewardBadgeKey)
                if (badge != null) {
                    binding.tvBadgeRewardPill.text = "🎖️ ${badge.title}"
                    binding.tvBadgeRewardPill.visibility = View.VISIBLE
                } else {
                    binding.tvBadgeRewardPill.visibility = View.GONE
                }
            } else {
                binding.tvBadgeRewardPill.visibility = View.GONE
            }

            // Action button state
            when {
                item.isClaimed -> {
                    binding.btnClaimReward.visibility = View.GONE
                    binding.btnStartNow.visibility = View.GONE
                    binding.tvStatusBadge.visibility = View.VISIBLE
                    binding.tvStatusBadge.text = "✓ Claimed"
                }
                item.isCompleted -> {
                    binding.btnClaimReward.visibility = View.VISIBLE
                    binding.btnStartNow.visibility = View.GONE
                    binding.tvStatusBadge.visibility = View.GONE
                    binding.btnClaimReward.setOnClickListener {
                        onClaimClick(item)
                    }
                }
                else -> {
                    // In Progress: Show "Start Now"
                    binding.btnClaimReward.visibility = View.GONE
                    binding.btnStartNow.visibility = View.VISIBLE
                    binding.tvStatusBadge.visibility = View.GONE
                    binding.btnStartNow.setOnClickListener {
                        onStartClick(item)
                    }
                }
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<WeeklyMissionWithProgress>() {
        override fun areItemsTheSame(
            oldItem: WeeklyMissionWithProgress,
            newItem: WeeklyMissionWithProgress
        ): Boolean = oldItem.mission.missionId == newItem.mission.missionId

        override fun areContentsTheSame(
            oldItem: WeeklyMissionWithProgress,
            newItem: WeeklyMissionWithProgress
        ): Boolean = oldItem == newItem
    }
}
