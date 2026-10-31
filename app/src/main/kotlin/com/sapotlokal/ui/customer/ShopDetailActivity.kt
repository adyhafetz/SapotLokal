package com.sapotlokal.ui.customer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener
import com.sapotlokal.R
import com.sapotlokal.data.model.Vendor
import com.sapotlokal.data.repository.PointRepository
import com.sapotlokal.data.repository.VendorRepository
import com.sapotlokal.data.repository.VoucherRepository
import com.sapotlokal.databinding.ActivityShopDetailBinding

/**
 * ShopDetailActivity — Phase 5: Shop Details Screen & Live Status Indicator.
 *
 * WHAT IT DOES:
 *   1. Displays real-time vendor information: shop name, category, address, operating hours.
 *   2. Displays a real-time OPEN/CLOSED status indicator chip that responds live to merchant changes.
 *   3. Shows the customer's current points accumulated specifically at this shop.
 *   4. Lists active reward rules/vouchers offered by this vendor with eligibility indicators.
 *   5. "Open in Google Maps" button opens the shop's street address in Google Maps or a map viewer.
 */
class ShopDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VENDOR_ID = "extra_vendor_id"
    }

    private lateinit var binding: ActivityShopDetailBinding
    private val auth = FirebaseAuth.getInstance()
    private val voucherAdapter = ShopVoucherAdapter()

    private var vendorId: String = ""
    private var currentVendor: Vendor? = null

    private var vendorListener: ValueEventListener? = null
    private var pointsListener: ValueEventListener? = null
    private var rulesListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vendorId = intent.getStringExtra(EXTRA_VENDOR_ID) ?: ""
        if (vendorId.isBlank()) {
            Toast.makeText(this, "Shop not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        loadData()

        // Phase 6: Subscribe customer to FCM updates when viewing this shop
        com.google.firebase.messaging.FirebaseMessaging.getInstance()
            .subscribeToTopic("shop_${vendorId}")
            .addOnSuccessListener {
                android.util.Log.d("ShopDetailActivity", "Subscribed to FCM topic: shop_${vendorId}")
            }
            .addOnFailureListener { e ->
                android.util.Log.w("ShopDetailActivity", "Could not subscribe to FCM topic: shop_${vendorId}", e)
            }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        binding.rvShopVouchers.layoutManager = LinearLayoutManager(this)
        binding.rvShopVouchers.adapter = voucherAdapter
    }

    private fun setupListeners() {
        binding.btnOpenMaps.setOnClickListener {
            val address = currentVendor?.vendorAddress.orEmpty().trim()
            if (address.isBlank()) {
                Toast.makeText(this, "Shop address not available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            openInMaps(address)
        }
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutContent.visibility = View.GONE

        // 1. Live listener for Vendor data (name, category, isOpen, operatingHours, address)
        vendorListener = VendorRepository.listenToVendor(
            vendorID = vendorId,
            onUpdate = { vendor ->
                if (vendor == null) {
                    Toast.makeText(this, "Shop not available", Toast.LENGTH_SHORT).show()
                    finish()
                    return@listenToVendor
                }
                currentVendor = vendor
                bindVendorData(vendor)

                binding.progressBar.visibility = View.GONE
                binding.layoutContent.visibility = View.VISIBLE
            },
            onError = { errorMsg ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error loading shop: $errorMsg", Toast.LENGTH_SHORT).show()
            }
        )

        // 2. Live listener for Customer's Points at this shop
        val userId = auth.currentUser?.uid
        if (!userId.isNullOrBlank()) {
            pointsListener = PointRepository.listenToPointsForUser(
                userID = userId,
                onUpdate = { perShopBalance, _, _ ->
                    val balance = perShopBalance[vendorId] ?: 0
                    binding.tvShopLoyaltyPoints.text = "$balance pts"
                    voucherAdapter.updateCustomerPoints(balance)
                },
                onError = {
                    binding.tvShopLoyaltyPoints.text = "0 pts"
                }
            )
        } else {
            binding.tvShopLoyaltyPoints.text = "0 pts"
        }

        // 3. Live listener for active vouchers/reward rules for this vendor
        rulesListener = VoucherRepository.listenToRulesForVendor(
            vendorID = vendorId,
            onUpdate = { rules ->
                val activeRules = rules.filter { it.isActive }
                voucherAdapter.submitList(activeRules)
                binding.tvNoVouchers.visibility = if (activeRules.isEmpty()) View.VISIBLE else View.GONE
                binding.rvShopVouchers.visibility = if (activeRules.isEmpty()) View.GONE else View.VISIBLE
            },
            onError = { _ ->
                binding.tvNoVouchers.visibility = View.VISIBLE
                binding.rvShopVouchers.visibility = View.GONE
            }
        )
    }

    private fun bindVendorData(vendor: Vendor) {
        binding.toolbar.title = vendor.vendorName.ifBlank { "Shop Details" }
        binding.tvShopName.text = vendor.vendorName.ifBlank { "Local Merchant" }
        binding.tvHeroCategory.text = vendor.vendorCategory.ifBlank { "Local Shop" }
        binding.tvCategoryPill.text = vendor.vendorCategory.ifBlank { "Shop" }

        // Live OPEN/CLOSED status chip
        if (vendor.isOpen) {
            binding.tvShopStatus.text = "● OPEN"
            binding.tvShopStatus.setBackgroundResource(R.drawable.bg_status_chip_open)
        } else {
            binding.tvShopStatus.text = "● CLOSED"
            binding.tvShopStatus.setBackgroundResource(R.drawable.bg_status_chip_closed)
        }

        // Operating hours
        binding.tvOperatingHours.text = if (vendor.operatingHours.isNotBlank()) {
            vendor.operatingHours
        } else {
            "Hours not specified"
        }

        // Address
        binding.tvShopAddress.text = if (vendor.vendorAddress.isNotBlank()) {
            vendor.vendorAddress
        } else {
            "Address not specified"
        }

        // Optional Description
        if (vendor.vendorDescription.isNotBlank()) {
            binding.tvShopDescription.text = vendor.vendorDescription
            binding.tvShopDescription.visibility = View.VISIBLE
        } else {
            binding.tvShopDescription.visibility = View.GONE
        }

        // Category icon mapping
        val (bgRes, iconRes) = getCategoryVisuals(vendor.vendorCategory)
        binding.heroAvatarContainer.setBackgroundResource(bgRes)
        binding.ivHeroCategoryIcon.setImageResource(iconRes)
    }

    private fun getCategoryVisuals(category: String): Pair<Int, Int> {
        val c = category.lowercase()
        return when {
            c.contains("kopi") || c.contains("caf") || c.contains("coffee") || c.contains("tea") ->
                R.drawable.bg_shop_icon_coffee to R.drawable.ic_coffee
            c.contains("baker") || c.contains("roti") || c.contains("cake") || c.contains("pastry") ->
                R.drawable.bg_shop_icon_croissant to R.drawable.ic_croissant
            else ->
                R.drawable.bg_shop_icon_croissant to R.drawable.ic_store
        }
    }

    private fun openInMaps(address: String) {
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            startActivity(mapIntent)
        } catch (e: ActivityNotFoundException) {
            // Google Maps not installed; fall back to generic map viewer or browser
            val genericIntent = Intent(Intent.ACTION_VIEW, uri)
            try {
                startActivity(genericIntent)
            } catch (e2: Exception) {
                Toast.makeText(this, "No map application found on device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        vendorListener?.let { VendorRepository.removeVendorListener(vendorId, it) }
        // Clean up other listeners
        vendorListener = null
        pointsListener = null
        rulesListener = null
    }
}

