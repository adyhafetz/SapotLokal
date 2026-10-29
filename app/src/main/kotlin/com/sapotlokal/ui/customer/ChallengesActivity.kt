package com.sapotlokal.ui.customer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener
import com.sapotlokal.data.model.UserMission
import com.sapotlokal.data.model.WeeklyMissionWithProgress
import com.sapotlokal.data.repository.MissionRepository
import com.sapotlokal.databinding.ActivityChallengesBinding
import com.sapotlokal.service.SapotFcmService
import com.sapotlokal.ui.games.GamesMenuActivity

/**
 * ChallengesActivity — Phase 3 Weekly Missions Screen.
 *
 * Displays the 6 weekly recurring missions with live progress,
 * attached badge previews, countdown timers, and "Start Now" / "Claim Reward" flows.
 */
class ChallengesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChallengesBinding
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: ChallengeAdapter
    private var missionsListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChallengesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        adapter = ChallengeAdapter(
            onStartClick = { item ->
                handleStartMission(item)
            },
            onClaimClick = { item ->
                claimReward(user.uid, item)
            }
        )

        binding.rvChallenges.layoutManager = LinearLayoutManager(this)
        binding.rvChallenges.adapter = adapter

        binding.progressBar.visibility = View.VISIBLE

        // Synchronize and seed weekly missions
        MissionRepository.syncUserMissions(user.uid) {
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
            }
        }

        // Real-time listener for current ISO week missions
        missionsListener = MissionRepository.listenToWeeklyMissions(
            userId = user.uid,
            onUpdate = { list ->
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(list)
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvChallenges.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                }
            },
            onError = { err ->
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error loading missions: $err", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun handleStartMission(item: WeeklyMissionWithProgress) {
        when (item.mission.missionId) {
            "arcade_champion" -> {
                startActivity(Intent(this, GamesMenuActivity::class.java))
            }
            "daily_tri_play" -> {
                startActivity(Intent(this, GamesMenuActivity::class.java))
            }
            else -> {
                // Shop visit missions: tapah_explorer, loyal_supporter, weekend_warrior, night_owl
                startActivity(Intent(this, ScanActivity::class.java))
            }
        }
    }

    private fun claimReward(userId: String, item: WeeklyMissionWithProgress) {
        val userMission = item.userMission ?: UserMission(
            userMissionId = "${userId}_${item.mission.missionId}",
            userId = userId,
            missionId = item.mission.missionId,
            currentProgress = item.mission.targetCount,
            isCompleted = true
        )

        MissionRepository.claimMissionReward(
            activity = this,
            userId = userId,
            mission = item.mission,
            userMission = userMission
        ) { success, errorMsg ->
            if (success) {
                val reward = item.mission.rewardPoints
                Toast.makeText(this, "🎉 Claimed +$reward bonus points!", Toast.LENGTH_LONG).show()

                // Trigger local notification
                SapotFcmService.showLocalNotification(
                    context = this,
                    title = "Mission Completed!",
                    body = "You earned +$reward bonus points for completing \"${item.mission.title}\"!",
                    channelId = SapotFcmService.CHANNEL_REWARDS
                )
            } else {
                Toast.makeText(this, errorMsg ?: "Could not claim reward", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        missionsListener?.let { MissionRepository.removeListener(it) }
    }
}
