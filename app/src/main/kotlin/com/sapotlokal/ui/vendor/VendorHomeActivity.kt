package com.sapotlokal.ui.vendor

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener
import com.sapotlokal.R
import com.sapotlokal.data.model.Vendor
import com.sapotlokal.data.repository.VendorRepository
import com.sapotlokal.databinding.ActivityVendorHomeBinding
import com.sapotlokal.service.SapotFcmService
import com.sapotlokal.ui.auth.LoginActivity
import com.sapotlokal.util.StatusChip
import com.sapotlokal.util.VendorAnalytics

/**
 * VendorHomeActivity — the vendor "Overview" tab, the first screen after a shop owner logs in.
 * Shows shop status (with the open/closed switch), the numbers that matter today, the live
 * rotating shop QR code and two simple charts. Other tabs are reached from the bottom bar.
 */
class VendorHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVendorHomeBinding
    private val auth = FirebaseAuth.getInstance()

    private var vendorID: String = ""
    private var currentVendor: Vendor? = null
    private var vendorListener: ValueEventListener? = null
    private var analyticsListener: ValueEventListener? = null
    private val customerNames = mutableMapOf<String, String>()
    private var isProgrammaticUpdate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVendorHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = auth.currentUser
        if (user == null) {
            goToLogin()
            return
        }

        VendorNav.bind(this, binding.vendorNav, VendorNav.Tab.HOME) { vendorID }
        setupStatusSwitch()
        SapotFcmService.syncTokenForUser(user.uid)

        loadVendor(user.uid)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (vendorID.isNotBlank()) {
            vendorListener?.let { VendorRepository.removeVendorListener(vendorID, it) }
            analyticsListener?.let { VendorRepository.removeAnalyticsListener(vendorID, it) }
        }
        vendorListener = null
        analyticsListener = null
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    private fun loadVendor(uid: String) {
        VendorRepository.fetchVendorByUserId(
            userID = uid,
            onResult = { vendor ->
                if (vendor == null) {
                    binding.tvShopName.text = getString(com.sapotlokal.R.string.kv_shop_not_found)
                    return@fetchVendorByUserId
                }
                vendorID = vendor.vendorID
                bindVendor(vendor)
                attachListeners(vendor.vendorID)
            },
            onError = { err -> binding.tvShopName.text = getString(com.sapotlokal.R.string.kv_could_not_load_shop_n, err) }
        )
    }

    private fun attachListeners(id: String) {
        vendorListener = VendorRepository.listenToVendor(
            vendorID = id,
            onUpdate = { updated -> if (updated != null) bindVendor(updated) },
            onError = { }
        )
        analyticsListener = VendorRepository.listenToAnalytics(
            vendorID = id,
            onUpdate = { bindAnalytics(it) },
            onError = { }
        )
    }

    // ── Shop header + status ──────────────────────────────────────────────────

    private fun bindVendor(vendor: Vendor) {
        currentVendor = vendor
        binding.tvShopName.text = vendor.vendorName.ifBlank { getString(com.sapotlokal.R.string.kv_your_shop) }
        binding.tvShopMeta.text = listOf(com.sapotlokal.data.model.ShopCategory.label(this, vendor.vendorCategory), vendor.vendorAddress)
            .filter { it.isNotBlank() }.joinToString(" · ")
        updateStatusUi(vendor.isOpen, vendor.operatingHours)
    }

    private fun setupStatusSwitch() {
        binding.switchStoreStatus.setOnCheckedChangeListener { _, isChecked ->
            if (isProgrammaticUpdate) return@setOnCheckedChangeListener
            val id = vendorID
            if (id.isBlank()) return@setOnCheckedChangeListener

            updateStatusUi(isChecked, currentVendor?.operatingHours.orEmpty())

            VendorRepository.updateVendorStatus(id, isChecked) { success, errorMsg ->
                if (success) {
                    val name = currentVendor?.vendorName.orEmpty().ifBlank { getString(com.sapotlokal.R.string.kv_your_shop) }
                    // Customers who follow the shop are told about both open and closed.
                    VendorRepository.broadcastShopStatus(id, name, isOpen = isChecked)
                    Toast.makeText(
                        this,
                        if (isChecked) getString(com.sapotlokal.R.string.kv_shop_is_open) else getString(com.sapotlokal.R.string.kv_shop_is_closed),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(this, getString(com.sapotlokal.R.string.kv_could_not_update_n, errorMsg ?: getString(com.sapotlokal.R.string.kv_network_error)), Toast.LENGTH_SHORT).show()
                    currentVendor?.let { updateStatusUi(it.isOpen, it.operatingHours) }
                }
            }
        }
    }

    private fun updateStatusUi(isOpen: Boolean, hours: String) {
        isProgrammaticUpdate = true
        binding.switchStoreStatus.isChecked = isOpen
        isProgrammaticUpdate = false

        StatusChip.bind(binding.tvStatusBadge, isOpen)

        // Primary (cocoa) when open, disabled grey when closed.
        val track = ContextCompat.getColor(this, if (isOpen) R.color.hu_accent else R.color.hu_line)
        binding.switchStoreStatus.trackTintList = ColorStateList.valueOf(track)
        binding.switchStoreStatus.thumbTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.hu_bg))
        binding.cardStoreStatus.strokeColor =
            ContextCompat.getColor(this, if (isOpen) R.color.hu_accent else R.color.hu_line)

        binding.tvStatusDescription.text =
            getString(if (isOpen) R.string.store_status_open_desc else R.string.store_status_closed_desc)

        if (hours.isNotBlank()) {
            binding.tvOperatingHoursHint.text = getString(com.sapotlokal.R.string.kv_hours_n, hours)
            binding.tvOperatingHoursHint.visibility = View.VISIBLE
        } else {
            binding.tvOperatingHoursHint.visibility = View.GONE
        }
    }

    // ── This week and recent visits ───────────────────────────────────────────

    private fun bindAnalytics(a: VendorAnalytics) {
        val fmt = java.text.NumberFormat.getIntegerInstance()
        binding.tvStatVisits.text = fmt.format(a.weekVisits)
        binding.tvStatPoints.text = fmt.format(a.weekPointsGiven)
        binding.tvStatVouchers.text = fmt.format(a.vouchersUsed)

        binding.tvNoVisits.visibility = if (a.recentVisits.isEmpty()) View.VISIBLE else View.GONE
        binding.recentRows.removeAllViews()
        a.recentVisits.forEach { p ->
            val row = com.sapotlokal.databinding.ItemOwnerVisitBinding.inflate(layoutInflater, binding.recentRows, false)
            row.tvWhen.text = android.text.format.DateUtils.getRelativeTimeSpanString(
                p.timestamp, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS
            )
            row.tvPoints.text = getString(R.string.owner_plus_points, p.pointEarnedAmount)
            bindCustomer(row, p.userID)
            binding.recentRows.addView(row.root)
        }
    }

    /** Shows the customer's name (read once from their public profile) and initials. */
    private fun bindCustomer(row: com.sapotlokal.databinding.ItemOwnerVisitBinding, uid: String) {
        fun show(name: String) {
            row.tvName.text = name
            row.tvInitials.text = com.sapotlokal.ui.customer.ShopStyle.initials(name)
        }
        customerNames[uid]?.let { return show(it) }
        show(getString(R.string.owner_a_customer))
        com.google.firebase.database.FirebaseDatabase.getInstance(com.sapotlokal.AppConfig.DATABASE_URL).reference
            .child("users").child(uid).child("userName").get()
            .addOnSuccessListener { snap ->
                val name = snap.getValue(String::class.java)?.ifBlank { null } ?: return@addOnSuccessListener
                customerNames[uid] = name
                if (!isFinishing && !isDestroyed) show(name)
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
