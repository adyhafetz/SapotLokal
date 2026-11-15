package com.sapotlokal.ui.customer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sapotlokal.AppConfig
import com.sapotlokal.AppConfig.CustomerTier
import com.sapotlokal.R
import com.sapotlokal.data.model.LeaderboardMember
import com.sapotlokal.data.model.User
import com.sapotlokal.data.repository.LeaderboardRepository
import com.sapotlokal.databinding.ActivityLeaderboardBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * LeaderboardActivity — Phase 7 Community Leaderboard Screen.
 *
 * Displays:
 * 1. Top 3 customers on elevated podiums (Gold 1st center, Silver 2nd left, Bronze 3rd right).
 * 2. Ranked list for 4th place onwards.
 * 3. Sticky "You" bottom card pinned at all times.
 * 4. Toggle between All-Time and This Week rankings.
 */
class LeaderboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLeaderboardBinding
    private val auth = FirebaseAuth.getInstance()
    private val adapter = LeaderboardAdapter()

    private var currentFilterIsAllTime = true
    private var currentUserId: String = ""
    private var currentUserName: String = ""
    private var currentUserLifetimePoints: Int = 0
    private var currentUserAvatar: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = auth.currentUser?.uid ?: ""

        setupViews()
        setupListeners()
        loadCurrentUserProfile()
        loadLeaderboardData()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }

        binding.rvLeaderboard.layoutManager = LinearLayoutManager(this)
        binding.rvLeaderboard.adapter = adapter

        // Top toggle group
        binding.toggleFilter.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val isAllTime = (checkedId == R.id.btnFilterAllTime)
                if (isAllTime != currentFilterIsAllTime) {
                    currentFilterIsAllTime = isAllTime
                    loadLeaderboardData()
                }
            }
        }
    }

    private fun setupListeners() {
        // Additional UI listeners if needed
    }

    private fun loadCurrentUserProfile() {
        if (currentUserId.isBlank()) return

        FirebaseDatabase.getInstance(AppConfig.DATABASE_URL)
            .reference.child("users").child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        currentUserName = user.userName.ifBlank { getString(com.sapotlokal.R.string.k_you) }
                        currentUserLifetimePoints = user.lifetimePoints
                        currentUserAvatar = user.avatar
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Ignore, fallback defaults will be used
                }
            })
    }

    private fun loadLeaderboardData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyLeaderboard.visibility = View.GONE

        if (currentFilterIsAllTime) {
            LeaderboardRepository.fetchAllTimeLeaderboard(
                limit = 50,
                onResult = { members ->
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        bindLeaderboardData(members)
                    }
                },
                onError = { err ->
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, getString(com.sapotlokal.R.string.k_error_rankings, err), Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } else {
            LeaderboardRepository.fetchWeeklyLeaderboard(
                limit = 50,
                onResult = { members ->
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        bindLeaderboardData(members)
                    }
                },
                onError = { err ->
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, getString(com.sapotlokal.R.string.k_error_rankings, err), Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun bindLeaderboardData(members: List<LeaderboardMember>) {
        if (members.isEmpty()) {
            binding.tvEmptyLeaderboard.visibility = View.VISIBLE
            binding.rvLeaderboard.visibility = View.GONE
            clearPodium()
            bindStickyYouCard(null)
            return
        }

        binding.tvEmptyLeaderboard.visibility = View.GONE
        binding.rvLeaderboard.visibility = View.VISIBLE

        // Top 3 Podium
        val first = members.getOrNull(0)
        val second = members.getOrNull(1)
        val third = members.getOrNull(2)

        bind1stPlace(first)
        bind2ndPlace(second)
        bind3rdPlace(third)

        // 4th place onwards in RecyclerView
        val remaining = if (members.size > 3) members.subList(3, members.size) else emptyList()
        adapter.submitList(remaining)

        // Sticky Bottom "You" card
        val currentUserMember = members.firstOrNull { it.userId == currentUserId }
        bindStickyYouCard(currentUserMember)
    }

    private fun bind1stPlace(member: LeaderboardMember?) {
        if (member == null) {
            binding.tvName1st.text = "—"
            binding.tvTier1st.text = getString(com.sapotlokal.R.string.k_unclaimed)
            binding.tvPoints1st.text = getString(com.sapotlokal.R.string.k_n_pts_lb, "0")
            binding.tvInitials1st.visibility = View.GONE
            binding.ivAvatar1st.visibility = View.VISIBLE
            return
        }

        val tier = CustomerTier.entries.firstOrNull { it.code == member.currentTier }
            ?: CustomerTier.fromLifetimePoints(member.points)

        binding.tvName1st.text = "${member.userName} ${tier.emoji}"
        binding.tvTier1st.text = tier.title
        binding.tvPoints1st.text = getString(com.sapotlokal.R.string.k_n_pts_lb, formatPoints(member.points))

        val initials = getInitials(member.userName)
        val hasAvatar = com.sapotlokal.util.AvatarUi.bind(binding.ivAvatar1st, binding.tvInitials1st, member.avatar, 5)
        if (hasAvatar) {
            // avatar is showing
        } else if (initials.isNotBlank()) {
            binding.tvInitials1st.text = initials
            binding.tvInitials1st.visibility = View.VISIBLE
            binding.ivAvatar1st.visibility = View.GONE
        } else {
            binding.tvInitials1st.visibility = View.GONE
            binding.ivAvatar1st.visibility = View.VISIBLE
        }
    }

    private fun bind2ndPlace(member: LeaderboardMember?) {
        if (member == null) {
            binding.tvName2nd.text = "—"
            binding.tvTier2nd.text = getString(com.sapotlokal.R.string.k_unclaimed)
            binding.tvPoints2nd.text = getString(com.sapotlokal.R.string.k_n_pts_lb, "0")
            binding.tvInitials2nd.visibility = View.GONE
            binding.ivAvatar2nd.visibility = View.VISIBLE
            return
        }

        val tier = CustomerTier.entries.firstOrNull { it.code == member.currentTier }
            ?: CustomerTier.fromLifetimePoints(member.points)

        binding.tvName2nd.text = "${member.userName} ${tier.emoji}"
        binding.tvTier2nd.text = tier.title
        binding.tvPoints2nd.text = getString(com.sapotlokal.R.string.k_n_pts_lb, formatPoints(member.points))

        val initials = getInitials(member.userName)
        val hasAvatar = com.sapotlokal.util.AvatarUi.bind(binding.ivAvatar2nd, binding.tvInitials2nd, member.avatar, 4)
        if (hasAvatar) {
            // avatar is showing
        } else if (initials.isNotBlank()) {
            binding.tvInitials2nd.text = initials
            binding.tvInitials2nd.visibility = View.VISIBLE
            binding.ivAvatar2nd.visibility = View.GONE
        } else {
            binding.tvInitials2nd.visibility = View.GONE
            binding.ivAvatar2nd.visibility = View.VISIBLE
        }
    }

    private fun bind3rdPlace(member: LeaderboardMember?) {
        if (member == null) {
            binding.tvName3rd.text = "—"
            binding.tvTier3rd.text = getString(com.sapotlokal.R.string.k_unclaimed)
            binding.tvPoints3rd.text = getString(com.sapotlokal.R.string.k_n_pts_lb, "0")
            binding.tvInitials3rd.visibility = View.GONE
            binding.ivAvatar3rd.visibility = View.VISIBLE
            return
        }

        val tier = CustomerTier.entries.firstOrNull { it.code == member.currentTier }
            ?: CustomerTier.fromLifetimePoints(member.points)

        binding.tvName3rd.text = "${member.userName} ${tier.emoji}"
        binding.tvTier3rd.text = tier.title
        binding.tvPoints3rd.text = getString(com.sapotlokal.R.string.k_n_pts_lb, formatPoints(member.points))

        val initials = getInitials(member.userName)
        val hasAvatar = com.sapotlokal.util.AvatarUi.bind(binding.ivAvatar3rd, binding.tvInitials3rd, member.avatar, 4)
        if (hasAvatar) {
            // avatar is showing
        } else if (initials.isNotBlank()) {
            binding.tvInitials3rd.text = initials
            binding.tvInitials3rd.visibility = View.VISIBLE
            binding.ivAvatar3rd.visibility = View.GONE
        } else {
            binding.tvInitials3rd.visibility = View.GONE
            binding.ivAvatar3rd.visibility = View.VISIBLE
        }
    }

    private fun clearPodium() {
        bind1stPlace(null)
        bind2ndPlace(null)
        bind3rdPlace(null)
        adapter.submitList(emptyList())
    }

    private fun bindStickyYouCard(userMember: LeaderboardMember?) {
        val tier = CustomerTier.fromLifetimePoints(currentUserLifetimePoints)
        val initials = getInitials(currentUserName)
        val hasAvatar = com.sapotlokal.util.AvatarUi.bind(
            binding.ivStickyAvatar, binding.tvStickyInitials, userMember?.avatar ?: currentUserAvatar
        )

        if (hasAvatar) {
            // avatar is showing
        } else if (initials.isNotBlank()) {
            binding.tvStickyInitials.text = initials
            binding.tvStickyInitials.visibility = View.VISIBLE
            binding.ivStickyAvatar.visibility = View.GONE
        } else {
            binding.tvStickyInitials.visibility = View.GONE
            binding.ivStickyAvatar.visibility = View.VISIBLE
        }

        if (userMember != null) {
            binding.tvStickyRank.text = "#${userMember.rank}"
            binding.tvStickyName.text = getString(com.sapotlokal.R.string.k_you_name, userMember.userName, tier.emoji)
            binding.tvStickyTier.text = getString(com.sapotlokal.R.string.k_you_tier_rank, tier.title, userMember.rank)
            binding.tvStickyPoints.text = formatPoints(userMember.points)
        } else {
            // Not in top 50
            binding.tvStickyRank.text = "#50+"
            binding.tvStickyName.text = getString(com.sapotlokal.R.string.k_you_name, currentUserName.ifBlank { getString(com.sapotlokal.R.string.k_you) }, tier.emoji)
            val pts = if (currentFilterIsAllTime) currentUserLifetimePoints else 0
            binding.tvStickyTier.text = getString(com.sapotlokal.R.string.k_top50_not_reached, tier.title)
            binding.tvStickyPoints.text = formatPoints(pts)
        }
    }

    private fun formatPoints(points: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(points)
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

