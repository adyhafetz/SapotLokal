package com.sapotlokal.ui.customer

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sapotlokal.AppConfig
import com.sapotlokal.AppConfig.CustomerTier
import com.sapotlokal.R
import com.sapotlokal.data.model.BadgeDef
import com.sapotlokal.data.repository.PointRepository
import com.sapotlokal.databinding.ActivityCustomerProfileBinding
import com.sapotlokal.databinding.DialogBadgeDetailsBinding
import com.sapotlokal.ui.auth.LoginActivity
import java.text.NumberFormat
import java.util.Locale

/**
 * CustomerProfileActivity — Phase 4 implementation.
 *
 * Displays:
 * 1. Profile header with user avatar, name, email, and current tier pill.
 * 2. Dual Points Metric Card clearly separating Spendable vs Lifetime Accumulated points.
 * 3. Tier level progression bar showing progress to the next Malaysian tier.
 * 4. Badges showcase grid displaying all 8 collectible badges (with color/grayscale distinction).
 * 5. Edit profile name functionality and sign out button routing to RolePickerActivity.
 */
class CustomerProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private var userListener: ValueEventListener? = null
    private var pointsListener: ValueEventListener? = null

    private var currentUserId: String = ""
    private var currentUserName: String = ""
    private var unlockedBadgesMap: Map<String, Boolean> = emptyMap()

    private lateinit var badgeAdapter: BadgeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            goToLogin()
            return
        }

        currentUserId = currentUser.uid
        binding.tvUserEmail.text = currentUser.email ?: ""

        setupBadgesGrid()
        setupListeners(currentUserId)
        setupActions()
    }

    private fun setupBadgesGrid() {
        badgeAdapter = BadgeAdapter { badge, isUnlocked ->
            showBadgeDetailsDialog(badge, isUnlocked)
        }
        binding.rvBadges.layoutManager = GridLayoutManager(this, 4)
        binding.rvBadges.adapter = badgeAdapter
        refreshBadgesGrid()
    }

    private fun refreshBadgesGrid() {
        val items = BadgeDef.entries.map { badge ->
            BadgeShowcaseItem(
                badge = badge,
                isUnlocked = unlockedBadgesMap[badge.id] == true
            )
        }
        badgeAdapter.submitList(items)
        val unlockedCount = items.count { it.isUnlocked }
        binding.tvBadgesUnlockedCount.text = "$unlockedCount / ${items.size} Unlocked"
    }

    private fun setupListeners(userId: String) {
        // 1. User profile listener (/users/{userId})
        val userRef = FirebaseDatabase.getInstance(AppConfig.DATABASE_URL)
            .reference.child("users").child(userId)

        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("userName").getValue(String::class.java)?.trim() ?: ""
                currentUserName = name
                binding.tvUserName.text = if (name.isNotBlank()) name else "Shopper"

                val email = snapshot.child("userEmail").getValue(String::class.java)?.trim() ?: ""
                if (email.isNotBlank()) {
                    binding.tvUserEmail.text = email
                }

                // Update initials
                if (name.isNotBlank()) {
                    val initials = name.split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .map { it.first().uppercaseChar() }
                        .joinToString("")
                    if (initials.isNotBlank()) {
                        binding.tvAvatarInitials.text = initials
                        binding.tvAvatarInitials.visibility = View.VISIBLE
                        binding.ivAvatar.visibility = View.GONE
                    } else {
                        binding.tvAvatarInitials.visibility = View.GONE
                        binding.ivAvatar.visibility = View.VISIBLE
                    }
                } else {
                    binding.tvAvatarInitials.visibility = View.GONE
                    binding.ivAvatar.visibility = View.VISIBLE
                }

                // Read unlocked badges
                val badgesSnapshot = snapshot.child("unlockedBadges")
                val updatedBadges = mutableMapOf<String, Boolean>()
                for (child in badgesSnapshot.children) {
                    val key = child.key
                    val isUnlocked = child.getValue(Boolean::class.java) ?: false
                    if (key != null) {
                        updatedBadges[key] = isUnlocked
                    }
                }
                unlockedBadgesMap = updatedBadges
                refreshBadgesGrid()
            }

            override fun onCancelled(error: DatabaseError) {
                // Network error fallback
            }
        }
        userRef.addValueEventListener(userListener!!)

        // 2. Dual Ledger Points listener
        pointsListener = PointRepository.listenToPointsForUser(
            userID = userId,
            onUpdate = { _, lifetimeEarned, spendableBalance ->
                updatePointsUi(lifetimeEarned, spendableBalance)
            },
            onError = { /* fallback */ }
        )
    }

    private fun updatePointsUi(lifetimeEarned: Int, spendableBalance: Int) {
        val tier = CustomerTier.fromLifetimePoints(lifetimeEarned)

        // Tier pill
        binding.tvTierPill.text = "${tier.emoji} ${tier.title} · ${tier.multiplier}x Multiplier"

        // Dual points metrics
        val formattedLifetime = NumberFormat.getNumberInstance(Locale.US).format(lifetimeEarned)
        val formattedSpendable = NumberFormat.getNumberInstance(Locale.US).format(spendableBalance)
        binding.tvSpendablePoints.text = formattedSpendable
        binding.tvLifetimePoints.text = formattedLifetime

        // Progress bar to next tier
        val nextTier = tier.nextTier
        if (nextTier == null) {
            // Max tier reached
            binding.progressLevel.progress = 100
            binding.tvNextTierBadge.text = "🏆 Max Tier"
            binding.tvProgressLabel.text = "Highest Rank: ${tier.title} ${tier.emoji}"
            binding.tvProgressCount.text = "$formattedLifetime pts"
            binding.tvMaxLevelNotice.visibility = View.VISIBLE
        } else {
            val tierSpan = nextTier.minPoints - tier.minPoints
            val earnedInTier = (lifetimeEarned - tier.minPoints).coerceAtLeast(0)
            val pct = if (tierSpan > 0) {
                ((earnedInTier.toDouble() / tierSpan.toDouble()) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }
            val ptsNeeded = nextTier.minPoints - lifetimeEarned
            val formattedNext = NumberFormat.getNumberInstance(Locale.US).format(nextTier.minPoints)

            binding.progressLevel.progress = pct
            binding.tvNextTierBadge.text = "Next: ${nextTier.title} ${nextTier.emoji}"
            binding.tvProgressLabel.text = "$ptsNeeded pts to ${nextTier.title} ${nextTier.emoji}"
            binding.tvProgressCount.text = "$formattedLifetime / $formattedNext pts"
            binding.tvMaxLevelNotice.visibility = View.GONE
        }
    }

    private fun setupActions() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnViewLeaderboard.setOnClickListener {
            startActivity(Intent(this, LeaderboardActivity::class.java))
        }

        binding.btnEditProfile.setOnClickListener {
            showEditNameDialog()
        }

        binding.btnSignOut.setOnClickListener {
            showSignOutConfirmation()
        }
    }

    private fun showEditNameDialog() {
        val input = EditText(this).apply {
            setText(currentUserName)
            setSelection(text.length)
            setHint("Enter your name")
            setSingleLine(true)
            setPadding(48, 36, 48, 36)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Profile Name")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotBlank()) {
                    FirebaseDatabase.getInstance(AppConfig.DATABASE_URL)
                        .reference.child("users").child(currentUserId).child("userName")
                        .setValue(newName)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBadgeDetailsDialog(badge: BadgeDef, isUnlocked: Boolean) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogBadgeDetailsBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.tvBadgeTitle.text = badge.title
        dialogBinding.tvBadgeDescription.text = badge.description
        dialogBinding.ivBadgeIcon.setImageResource(badge.iconRes)

        if (isUnlocked) {
            dialogBinding.ivBadgeIcon.colorFilter = null
            dialogBinding.ivBadgeLockOverlay.visibility = View.GONE
            dialogBinding.tvBadgeStatusPill.text = "✓ UNLOCKED"
            dialogBinding.tvBadgeStatusPill.setBackgroundResource(R.drawable.bg_badge_success)
            dialogBinding.tvBadgeStatusPill.setTextColor(ContextCompat.getColor(this, R.color.sapot_success_text))
        } else {
            val matrix = ColorMatrix().apply { setSaturation(0f) }
            dialogBinding.ivBadgeIcon.colorFilter = ColorMatrixColorFilter(matrix)
            dialogBinding.ivBadgeLockOverlay.visibility = View.VISIBLE
            dialogBinding.tvBadgeStatusPill.text = "🔒 LOCKED"
            dialogBinding.tvBadgeStatusPill.setBackgroundResource(R.drawable.bg_pill_gray)
            dialogBinding.tvBadgeStatusPill.setTextColor(ContextCompat.getColor(this, R.color.sapot_text_secondary))
        }

        dialogBinding.btnDismissBadgeDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSignOutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out of SapotLokal?")
            .setPositiveButton("Sign Out") { _, _ ->
                auth.signOut()
                goToLogin()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onStop() {
        super.onStop()
        if (currentUserId.isNotBlank()) {
            userListener?.let {
                FirebaseDatabase.getInstance(AppConfig.DATABASE_URL)
                    .reference.child("users").child(currentUserId)
                    .removeEventListener(it)
            }
            userListener = null
            pointsListener?.let {
                PointRepository.removePointsListener(currentUserId, it)
            }
            pointsListener = null
        }
    }

    override fun onStart() {
        super.onStart()
        if (currentUserId.isNotBlank()) {
            if (userListener == null || pointsListener == null) {
                setupListeners(currentUserId)
            }
        }
    }
}
