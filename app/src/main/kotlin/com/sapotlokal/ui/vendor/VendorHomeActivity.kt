package com.sapotlokal.ui.vendor

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.sapotlokal.AppConfig
import com.sapotlokal.data.repository.VendorRepository
import com.sapotlokal.databinding.ActivityVendorHomeBinding
import com.sapotlokal.ui.auth.LoginActivity

/**
 * VendorHomeActivity — Phase 4 real implementation (replaces the Phase 2 placeholder).
 *
 * WHAT IT DOES:
 * 1. Identifies the logged-in vendor via FirebaseAuth uid → VendorRepository.
 * 2. Stores the vendorID as a companion-object value so other activities can use it
 *    without re-querying (within the same session).
 * 3. Navigation: Show QR Code, Reward Rules, Scan to Redeem.
 * 4. Log out clears the session.
 *
 * WHY STORE vendorID HERE: Every vendor screen needs the vendorID. Fetching it once
 * here and passing it via Intent extras avoids repeated database round-trips and
 * keeps the other activities simple.
 */
class VendorHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVendorHomeBinding
    private val auth = FirebaseAuth.getInstance()

    // Holds the vendor record for this session (set after the DB lookup)
    private var vendorID: String = ""
    private var currentVendor: com.sapotlokal.data.model.Vendor? = null
    private var vendorListener: ValueEventListener? = null
    private var isProgrammaticUpdate: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVendorHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val user = auth.currentUser
        if (user == null) { goToLogin(); return }

        setupStatusSwitch()
        refreshVendorProfile()
        user.let { com.sapotlokal.service.SapotFcmService.syncTokenForUser(it.uid) }

        binding.btnLogOut.setOnClickListener {
            auth.signOut()
            goToLogin()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshVendorProfile()
    }

    private fun setupStatusSwitch() {
        binding.switchStoreStatus.setOnCheckedChangeListener { _, isChecked ->
            if (isProgrammaticUpdate) return@setOnCheckedChangeListener
            val currentId = vendorID
            if (currentId.isBlank()) return@setOnCheckedChangeListener

            // Immediately provide responsive local UI feedback
            updateStatusCardUi(isChecked, currentVendor?.operatingHours.orEmpty())

            VendorRepository.updateVendorStatus(currentId, isChecked) { success, errorMsg ->
                if (success) {
                    val vendorName = currentVendor?.vendorName.orEmpty().ifBlank { "Your shop" }
                    if (isChecked) {
                        VendorRepository.broadcastShopStatus(currentId, vendorName, isOpen = true)
                        android.widget.Toast.makeText(
                            this,
                            "$vendorName is now OPEN! Customers notified. ✓",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        android.widget.Toast.makeText(
                            this,
                            "Store status updated to CLOSED.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    android.widget.Toast.makeText(
                        this,
                        "Failed to update status: ${errorMsg ?: "Network error"}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    // Revert switch to actual vendor state
                    currentVendor?.let {
                        updateStatusCardUi(it.isOpen, it.operatingHours)
                    }
                }
            }
        }
    }

    private fun updateStatusCardUi(isOpen: Boolean, operatingHours: String) {
        isProgrammaticUpdate = true
        binding.switchStoreStatus.isChecked = isOpen
        isProgrammaticUpdate = false

        if (isOpen) {
            binding.cardStoreStatus.strokeColor = androidx.core.content.ContextCompat.getColor(
                this,
                com.sapotlokal.R.color.status_open
            )
            binding.tvStatusBadge.text = getString(com.sapotlokal.R.string.store_status_open_badge)
            binding.tvStatusBadge.setBackgroundResource(com.sapotlokal.R.drawable.bg_status_chip_open)
            binding.tvStatusDescription.text = getString(com.sapotlokal.R.string.store_status_open_desc)
        } else {
            binding.cardStoreStatus.strokeColor = androidx.core.content.ContextCompat.getColor(
                this,
                com.sapotlokal.R.color.status_closed
            )
            binding.tvStatusBadge.text = getString(com.sapotlokal.R.string.store_status_closed_badge)
            binding.tvStatusBadge.setBackgroundResource(com.sapotlokal.R.drawable.bg_status_chip_closed)
            binding.tvStatusDescription.text = getString(com.sapotlokal.R.string.store_status_closed_desc)
        }

        if (operatingHours.isNotBlank()) {
            binding.tvOperatingHoursHint.text = "Hours: $operatingHours"
            binding.tvOperatingHoursHint.visibility = android.view.View.VISIBLE
        } else {
            binding.tvOperatingHoursHint.visibility = android.view.View.GONE
        }
    }

    private fun refreshVendorProfile() {
        val user = auth.currentUser ?: return

        // Look up the vendor record for this user
        VendorRepository.fetchVendorByUserId(
            userID   = user.uid,
            onResult = { vendor ->
                if (vendor == null) {
                    binding.tvShopName.text = "Shop details not found"
                    return@fetchVendorByUserId
                }
                currentVendor = vendor
                vendorID = vendor.vendorID
                binding.tvWelcome.text  = "Welcome back, ${vendor.vendorName}!"
                binding.tvShopName.text = "${vendor.vendorCategory} · ${vendor.vendorAddress}"

                updateStatusCardUi(vendor.isOpen, vendor.operatingHours)
                attachLiveVendorListener(vendor.vendorID)

                // Navigation — all screens need the vendorID
                binding.btnShowQr.setOnClickListener {
                    startActivity(
                        Intent(this, ShowQrActivity::class.java)
                            .putExtra(EXTRA_VENDOR_ID, vendorID)
                    )
                }
                binding.btnRewardRules.setOnClickListener {
                    startActivity(
                        Intent(this, RewardRulesActivity::class.java)
                            .putExtra(EXTRA_VENDOR_ID, vendorID)
                    )
                }
                binding.btnScanRedeem.setOnClickListener {
                    startActivity(
                        Intent(this, ScanToRedeemActivity::class.java)
                            .putExtra(EXTRA_VENDOR_ID, vendorID)
                    )
                }
                binding.btnStats.setOnClickListener {
                    startActivity(
                        Intent(this, VendorStatsActivity::class.java)
                            .putExtra(EXTRA_VENDOR_ID, vendorID)
                    )
                }
                binding.btnShopSettings.setOnClickListener {
                    startActivity(
                        Intent(this, ShopSettingsActivity::class.java)
                            .putExtra(EXTRA_VENDOR_ID, vendorID)
                    )
                }
            },
            onError = { err ->
                binding.tvShopName.text = "Could not load shop: $err"
            }
        )

        binding.btnLogOut.setOnClickListener {
            auth.signOut()
            goToLogin()
        }
    }

    private fun attachLiveVendorListener(targetVendorId: String) {
        if (vendorListener != null) return
        vendorListener = VendorRepository.listenToVendor(
            vendorID = targetVendorId,
            onUpdate = { updated ->
                if (updated != null) {
                    currentVendor = updated
                    binding.tvShopName.text = "${updated.vendorCategory} · ${updated.vendorAddress}"
                    updateStatusCardUi(updated.isOpen, updated.operatingHours)
                }
            },
            onError = { _ -> }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        vendorListener?.let {
            if (vendorID.isNotBlank()) {
                VendorRepository.removeVendorListener(vendorID, it)
            }
        }
        vendorListener = null
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(com.sapotlokal.R.menu.menu_vendor_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            com.sapotlokal.R.id.action_logout -> { auth.signOut(); goToLogin(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun goToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }

    companion object {
        const val EXTRA_VENDOR_ID = "extra_vendor_id"
    }
}
