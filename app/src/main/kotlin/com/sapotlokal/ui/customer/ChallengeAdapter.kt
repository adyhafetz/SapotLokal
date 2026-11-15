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

            binding.tvChallengeTitle.text = com.sapotlokal.util.ContentText.missionTitle(binding.root.context, mission)
            binding.tvChallengeDesc.text = com.sapotlokal.util.ContentText.missionDescription(binding.root.context, mission)
            binding.tvRewardBadge.text = binding.root.context.getString(com.sapotlokal.R.string.k_plus_level_points, mission.rewardPoints)
            binding.tvMissionType.text = typeOf(binding.root.context, mission.missionId)
            binding.ivMissionIcon.setImageResource(iconOf(mission.missionId))

            binding.pbChallenge.max = if (target > 0) target else 1
            binding.pbChallenge.progress = current
            binding.tvProgressText.text = if (target <= 10) binding.root.context.getString(com.sapotlokal.R.string.k_progress_completed, current, target) else "$current / $target"

            // Right-hand status text
            binding.tvCountdownTimer.text = when {
                item.isClaimed -> binding.root.context.getString(com.sapotlokal.R.string.k_mission_collected)
                item.isCompleted -> binding.root.context.getString(com.sapotlokal.R.string.k_mission_completed)
                else -> binding.root.context.getString(com.sapotlokal.R.string.k_mission_in_progress)
            }

            // Attached badge preview pill
            if (!mission.rewardBadgeKey.isNullOrBlank()) {
                val badge = BadgeDef.fromId(mission.rewardBadgeKey)
                if (badge != null) {
                    binding.tvBadgeRewardPill.text = binding.root.context.getString(com.sapotlokal.R.string.k_badge_prefix, com.sapotlokal.util.ContentText.badgeTitle(binding.root.context, badge))
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
                    binding.tvStatusBadge.text = binding.root.context.getString(com.sapotlokal.R.string.k_reward_collected)
                }
                item.isCompleted -> {
                    binding.btnClaimReward.visibility = View.VISIBLE
                    binding.btnStartNow.visibility = View.GONE
                    binding.tvStatusBadge.visibility = View.GONE
                    binding.btnClaimReward.isEnabled = true
                    binding.btnClaimReward.setOnClickListener {
                        // Switch the button off at once so a double tap can't claim twice.
                        binding.btnClaimReward.isEnabled = false
                        onClaimClick(item)
                    }
                }
                else -> {
                    // In Progress: Show "Start Now"
                    binding.btnClaimReward.visibility = View.GONE
                    binding.btnStartNow.visibility = View.VISIBLE
                    binding.btnStartNow.setText(if (item.progress > 0) com.sapotlokal.R.string.hu_keep_going else com.sapotlokal.R.string.challenge_card_get_started)
                    binding.tvStatusBadge.visibility = View.GONE
                    binding.btnStartNow.setOnClickListener {
                        onStartClick(item)
                    }
                }
            }

            applyClaimedStyle(item.isClaimed)
        }

        /**
         * A mission whose reward was already collected goes inactive: grey and faded, nothing to tap.
         * Every other card is reset to its normal look (cards are reused as the list scrolls).
         */
        private fun applyClaimedStyle(claimed: Boolean) {
            val ctx = binding.root.context
            val primary = androidx.core.content.ContextCompat.getColor(ctx, com.sapotlokal.R.color.hu_ink)
            val muted = androidx.core.content.ContextCompat.getColor(ctx, com.sapotlokal.R.color.hu_soft)
            val greyBar = android.graphics.Color.parseColor("#C9C9C9")
            val pillBg = if (claimed) com.sapotlokal.R.drawable.bg_pill_gray else com.sapotlokal.R.drawable.bg_hu_icon_tile

            binding.root.alpha = if (claimed) 0.75f else 1f
            binding.root.setCardBackgroundColor(
                if (claimed) android.graphics.Color.parseColor("#F4F4F4") else android.graphics.Color.WHITE
            )
            binding.missionIconBox.setBackgroundResource(pillBg)
            binding.ivMissionIcon.imageTintList = android.content.res.ColorStateList.valueOf(if (claimed) muted else primary)
            binding.tvRewardBadge.setBackgroundResource(pillBg)
            binding.tvRewardBadge.setTextColor(if (claimed) muted else primary)
            binding.tvBadgeRewardPill.setBackgroundResource(pillBg)
            binding.tvBadgeRewardPill.setTextColor(if (claimed) muted else primary)
            binding.tvChallengeTitle.setTextColor(if (claimed) muted else primary)
            binding.tvProgressText.setTextColor(if (claimed) muted else primary)
            binding.pbChallenge.progressTintList =
                if (claimed) android.content.res.ColorStateList.valueOf(greyBar) else null
        }
    }

    private fun typeOf(ctx: android.content.Context, id: String) = when (id) {
        "tapah_explorer" -> ctx.getString(com.sapotlokal.R.string.k_type_explore)
        "loyal_supporter" -> ctx.getString(com.sapotlokal.R.string.k_type_loyalty)
        "weekend_warrior" -> ctx.getString(com.sapotlokal.R.string.k_type_weekend)
        "daily_tri_play" -> ctx.getString(com.sapotlokal.R.string.k_type_daily)
        "night_owl" -> ctx.getString(com.sapotlokal.R.string.k_type_night)
        "arcade_champion" -> ctx.getString(com.sapotlokal.R.string.k_type_games)
        else -> ctx.getString(com.sapotlokal.R.string.k_type_mission)
    }

    private fun iconOf(id: String) = when (id) {
        "tapah_explorer" -> com.sapotlokal.R.drawable.ic_compass
        "loyal_supporter" -> com.sapotlokal.R.drawable.ic_heart
        "weekend_warrior" -> com.sapotlokal.R.drawable.ic_star
        "daily_tri_play" -> com.sapotlokal.R.drawable.ic_gift
        "night_owl" -> com.sapotlokal.R.drawable.ic_clock
        "arcade_champion" -> com.sapotlokal.R.drawable.ic_wheel
        else -> com.sapotlokal.R.drawable.ic_store
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
