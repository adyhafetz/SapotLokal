package com.sapotlokal.ui.customer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sapotlokal.AppConfig
import com.sapotlokal.R
import com.sapotlokal.data.model.BadgeDef
import com.sapotlokal.data.model.UserMission
import com.sapotlokal.data.model.WeeklyMissionWithProgress
import com.sapotlokal.data.repository.GamesRepository
import com.sapotlokal.data.repository.MissionRepository
import com.sapotlokal.databinding.ActivityChallengesBinding
import com.sapotlokal.service.SapotFcmService
import com.sapotlokal.ui.games.MemoryMatchActivity
import com.sapotlokal.ui.games.SpinWheelActivity

/**
 * Play tab (Hadiah Utama). Everything fun in one place: level score with a ticked bar, this
 * week's missions (start / collect), the two games, badges, the leaderboard, and a way to Me.
 * Level score never buys rewards; that rule is explained by "How points work".
 */
class ChallengesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChallengesBinding
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: ChallengeAdapter
    private var missionsListener: ValueEventListener? = null
    private var xpListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChallengesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        CustomerNav.bind(this, binding.customerNav, CustomerNav.Tab.PLAY)

        val user = auth.currentUser ?: run {
            Toast.makeText(this, getString(R.string.k_please_log_in), Toast.LENGTH_SHORT).show()
            finish(); return
        }

        binding.btnHowPoints.setOnClickListener { HowPoints.show(this) }
        binding.cardSpin.setOnClickListener { startActivity(Intent(this, SpinWheelActivity::class.java)) }
        binding.cardMemory.setOnClickListener { startActivity(Intent(this, MemoryMatchActivity::class.java)) }
        binding.rowLeaderboard.setOnClickListener { startActivity(Intent(this, LeaderboardActivity::class.java)) }
        binding.rowMe.setOnClickListener { openMe() }

        adapter = ChallengeAdapter(
            onStartClick = { item -> handleStartMission(item) },
            onClaimClick = { item -> claimReward(user.uid, item) }
        )
        binding.rvChallenges.layoutManager = LinearLayoutManager(this)
        binding.rvChallenges.adapter = adapter

        binding.progressBar.visibility = View.VISIBLE
        MissionRepository.syncUserMissions(user.uid) { runOnUiThread { binding.progressBar.visibility = View.GONE } }
        missionsListener = MissionRepository.listenToWeeklyMissions(
            userId = user.uid,
            onUpdate = { list ->
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(list.sortedBy { it.isClaimed })
                    val left = list.filter { !it.isClaimed }
                    binding.tvBannerSubtext.text =
                        if (list.isNotEmpty() && left.isEmpty()) getString(R.string.k_all_collected)
                        else getString(R.string.k_collect_up_to, left.sumOf { it.mission.rewardPoints })
                    list.firstOrNull()?.let {
                        binding.tvBannerTimer.text = com.sapotlokal.util.MissionDateUtils.formatTimeRemaining(this, it.weekExpiresAtMillis)
                    }
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvChallenges.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                }
            },
            onError = { err ->
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, getString(R.string.k_error_loading_missions, err), Toast.LENGTH_SHORT).show()
                }
            }
        )

        xpListener = com.sapotlokal.data.repository.XpRepository.listenStanding(user.uid) { standing ->
            RankCard.bind(binding.rankCard, standing)
            RankCard.maybeShowRankUp(this, user.uid, standing)
        }

        val badgeAdapter = BadgeAdapter { badge, unlocked -> showBadge(badge, unlocked) }
        binding.rvBadges.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 2)
        binding.rvBadges.adapter = badgeAdapter
        fun bindBadges(unlockedIds: Set<String>) {
            badgeAdapter.submitList(BadgeDef.entries.map { BadgeShowcaseItem(it, it.id in unlockedIds) })
            binding.tvBadges.text = getString(R.string.hu_play_badges, unlockedIds.size, BadgeDef.entries.size)
        }
        bindBadges(emptySet())
        FirebaseDatabase.getInstance(AppConfig.DATABASE_URL).reference.child("users").child(user.uid)
            .child("unlockedBadges").get().addOnSuccessListener { snap ->
                bindBadges(snap.children.filter { it.getValue(Boolean::class.java) == true }.mapNotNull { it.key }.toSet())
            }
    }

    override fun onResume() {
        super.onResume()
        val uid = auth.currentUser?.uid ?: return
        GamesRepository.fetchStatus(uid) { status ->
            if (isFinishing || isDestroyed) return@fetchStatus
            if (status == null) {
                binding.tvSpinSub.text = ""; binding.tvMemorySub.text = ""; return@fetchStatus
            }
            binding.tvSpinSub.text = getString(if (status.canSpinToday) R.string.hu_spin_ready else R.string.games_spin_used)
            binding.tvMemorySub.text = getString(R.string.hu_memory_today, status.memoryPointsToday, AppConfig.MEMORY_MATCH_DAILY_CAP_POINTS)
        }
    }

    private fun showBadge(badge: BadgeDef, unlocked: Boolean) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(com.sapotlokal.util.ContentText.badgeTitle(this, badge))
            .setMessage(
                com.sapotlokal.util.ContentText.badgeDescription(this, badge) + "\n\n" +
                    getString(if (unlocked) R.string.k_status_unlocked_pill else R.string.k_status_locked_pill)
            )
            .setPositiveButton(R.string.hu_got_it, null)
            .show()
    }

    private fun openMe() = startActivity(Intent(this, CustomerProfileActivity::class.java))

    private fun handleStartMission(item: WeeklyMissionWithProgress) {
        // Game missions start at the games; shop-visit missions start at Scan.
        val target = when (item.mission.missionId) {
            "arcade_champion", "daily_tri_play" -> com.sapotlokal.ui.games.GamesMenuActivity::class.java
            else -> ScanActivity::class.java
        }
        startActivity(Intent(this, target))
    }

    private fun claimReward(userId: String, item: WeeklyMissionWithProgress) {
        val userMission = item.userMission ?: UserMission(
            userMissionId = "${userId}_${item.mission.missionId}",
            userId = userId,
            missionId = item.mission.missionId,
            currentProgress = item.mission.targetCount,
            isCompleted = true
        )
        MissionRepository.claimMissionReward(activity = this, userId = userId, mission = item.mission, userMission = userMission) { success, errorMsg ->
            if (success) {
                val reward = item.mission.rewardPoints
                Toast.makeText(this, getString(R.string.k_claimed_bonus, reward), Toast.LENGTH_LONG).show()
                SapotFcmService.showLocalNotification(
                    context = this,
                    title = getString(R.string.k_mission_done_title),
                    body = getString(R.string.k_mission_done_body, reward, com.sapotlokal.util.ContentText.missionTitle(this, item.mission)),
                    channelId = SapotFcmService.CHANNEL_REWARDS
                )
            } else {
                Toast.makeText(this, errorMsg ?: getString(R.string.k_could_not_claim), Toast.LENGTH_SHORT).show()
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val uid = auth.currentUser?.uid ?: return
        missionsListener?.let { MissionRepository.removeListener(uid, it) }
        com.sapotlokal.data.repository.XpRepository.stopListening(uid, xpListener)
    }
}
