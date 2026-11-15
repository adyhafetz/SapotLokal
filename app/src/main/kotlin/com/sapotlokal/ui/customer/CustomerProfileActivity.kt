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
import com.sapotlokal.R
import com.sapotlokal.data.model.BadgeDef
import com.sapotlokal.databinding.ActivityCustomerProfileBinding
import com.sapotlokal.databinding.DialogBadgeDetailsBinding
import com.sapotlokal.ui.auth.LoginActivity
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

    private var currentUserId: String = ""
    private var currentUserName: String = ""
    private var unlockedBadgesMap: Map<String, Boolean> = emptyMap()

    private lateinit var badgeAdapter: BadgeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        com.sapotlokal.util.AppLanguage.bindToggle(binding.languageToggle.root)
        // Me is a page opened from Home or Play: back arrow, no bottom bar.
        binding.customerNav.root.visibility = android.view.View.GONE

        val currentUser = auth.currentUser
        if (currentUser == null) {
            goToLogin()
            return
        }

        currentUserId = currentUser.uid
        binding.tvUserEmail.text = currentUser.email ?: ""

        setupBadgesGrid()
        // Badges live in Play (one place for them); Me keeps the account only.
        binding.rvBadges.visibility = View.GONE
        (binding.tvBadgesUnlockedCount.parent as View).visibility = View.GONE
        setupListeners(currentUserId)
        bindMemberSince()
        // The rank card sits inside the profile card here, so it is drawn flat (no box in a box).
        binding.rankCard.root.apply { background = null; foreground = null; setPadding(0, 0, 0, 0) }
        setupActions()
    }

    private fun setupBadgesGrid() {
        badgeAdapter = BadgeAdapter { badge, isUnlocked ->
            showBadgeDetailsDialog(badge, isUnlocked)
        }
        binding.rvBadges.layoutManager = GridLayoutManager(this, 2)
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
        binding.tvBadgesUnlockedCount.text = getString(com.sapotlokal.R.string.k_n_unlocked, unlockedCount, items.size)
    }

    private fun setupListeners(userId: String) {
        // 1. User profile listener (/users/{userId})
        val userRef = FirebaseDatabase.getInstance(AppConfig.DATABASE_URL)
            .reference.child("users").child(userId)

        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("userName").getValue(String::class.java)?.trim() ?: ""
                currentUserName = name
                binding.tvUserName.text = if (name.isNotBlank()) name else getString(com.sapotlokal.R.string.k_shopper)

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

                currentAvatarId = snapshot.child("avatar").getValue(String::class.java).orEmpty()
                com.sapotlokal.util.AvatarUi.bind(binding.ivAvatar, binding.tvAvatarInitials, currentAvatarId)

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

                val standing = com.sapotlokal.data.repository.XpRepository.standingOf(snapshot)
                RankCard.bind(binding.rankCard, standing)
                RankCard.maybeShowRankUp(this@CustomerProfileActivity, userId, standing)
            }

            override fun onCancelled(error: DatabaseError) {
                // Network error fallback
            }
        }
        userRef.addValueEventListener(userListener!!)

    }

    private fun bindMemberSince() {
        auth.currentUser?.metadata?.creationTimestamp?.takeIf { it > 0L }?.let { created ->
            val month = java.text.SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(java.util.Date(created))
            binding.tvMemberSince.text = getString(com.sapotlokal.R.string.k_member_since, month)
        }
    }

    private fun setupActions() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnViewLeaderboard.setOnClickListener { HowPoints.show(this) }

        binding.btnEditProfile.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(com.sapotlokal.R.string.k_edit_profile))
                .setItems(arrayOf(getString(com.sapotlokal.R.string.k_change_name), getString(com.sapotlokal.R.string.k_choose_avatar))) { _, which ->
                    if (which == 0) showEditNameDialog() else pickAvatar()
                }
                .show()
        }
        // Tapping the picture opens the avatar picker too.
        binding.ivAvatar.setOnClickListener { pickAvatar() }
        binding.tvAvatarInitials.setOnClickListener { pickAvatar() }

        binding.btnSignOut.setOnClickListener {
            showSignOutConfirmation()
        }
        binding.rowChangePassword.setOnClickListener { com.sapotlokal.util.AccountActions.showChangePassword(this) }
        binding.rowDeleteAccount.setOnClickListener { com.sapotlokal.util.AccountActions.showDeleteAccount(this) }
    }

    private var currentAvatarId: String = ""

    private fun pickAvatar() {
        val uid = auth.currentUser?.uid ?: return
        com.sapotlokal.util.AvatarUi.showPicker(this, currentAvatarId) { chosen ->
            com.sapotlokal.util.AvatarUi.save(uid, chosen) { ok ->
                if (!ok) android.widget.Toast.makeText(this, getString(com.sapotlokal.R.string.k_avatar_save_failed), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditNameDialog() {
        val input = EditText(this).apply {
            setText(currentUserName)
            setSelection(text.length)
            setHint(getString(com.sapotlokal.R.string.k_enter_your_name))
            setSingleLine(true)
            setPadding(48, 36, 48, 36)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(com.sapotlokal.R.string.k_edit_name_title))
            .setView(input)
            .setPositiveButton(getString(com.sapotlokal.R.string.k_save)) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotBlank()) {
                    FirebaseDatabase.getInstance(AppConfig.DATABASE_URL)
                        .reference.child("users").child(currentUserId).child("userName")
                        .setValue(newName)
                        .addOnSuccessListener {
                            Toast.makeText(this, getString(com.sapotlokal.R.string.k_profile_updated), Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, getString(com.sapotlokal.R.string.k_profile_update_failed), Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, getString(com.sapotlokal.R.string.k_name_empty), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(com.sapotlokal.R.string.k_cancel), null)
            .show()
    }

    private fun showBadgeDetailsDialog(badge: BadgeDef, isUnlocked: Boolean) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogBadgeDetailsBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.tvBadgeTitle.text = com.sapotlokal.util.ContentText.badgeTitle(this, badge)
        dialogBinding.tvBadgeDescription.text = com.sapotlokal.util.ContentText.badgeDescription(this, badge)
        dialogBinding.ivBadgeIcon.setImageResource(badge.iconRes)

        if (isUnlocked) {
            dialogBinding.ivBadgeIcon.colorFilter = null
            dialogBinding.ivBadgeLockOverlay.visibility = View.GONE
            dialogBinding.tvBadgeStatusPill.text = getString(com.sapotlokal.R.string.k_status_unlocked_pill)
            dialogBinding.tvBadgeStatusPill.setBackgroundResource(R.drawable.bg_badge_success)
            dialogBinding.tvBadgeStatusPill.setTextColor(ContextCompat.getColor(this, R.color.hu_ok))
        } else {
            val matrix = ColorMatrix().apply { setSaturation(0f) }
            dialogBinding.ivBadgeIcon.colorFilter = ColorMatrixColorFilter(matrix)
            dialogBinding.ivBadgeLockOverlay.visibility = View.VISIBLE
            dialogBinding.tvBadgeStatusPill.text = getString(com.sapotlokal.R.string.k_status_locked_pill)
            dialogBinding.tvBadgeStatusPill.setBackgroundResource(R.drawable.bg_pill_gray)
            dialogBinding.tvBadgeStatusPill.setTextColor(ContextCompat.getColor(this, R.color.hu_soft))
        }

        dialogBinding.btnDismissBadgeDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSignOutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(com.sapotlokal.R.string.k_sign_out))
            .setMessage(getString(com.sapotlokal.R.string.k_sign_out_confirm))
            .setPositiveButton(getString(com.sapotlokal.R.string.k_sign_out)) { _, _ ->
                com.sapotlokal.util.SessionManager.signOut(this@CustomerProfileActivity)
                goToLogin()
            }
            .setNegativeButton(getString(com.sapotlokal.R.string.k_cancel), null)
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
        }
    }

    override fun onStart() {
        super.onStart()
        if (currentUserId.isNotBlank()) {
            if (userListener == null) {
                setupListeners(currentUserId)
            }
        }
    }
}
