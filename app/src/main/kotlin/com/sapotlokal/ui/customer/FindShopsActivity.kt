package com.sapotlokal.ui.customer

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener
import com.sapotlokal.data.model.Vendor
import com.sapotlokal.data.repository.PointRepository
import com.sapotlokal.data.repository.VendorRepository
import com.sapotlokal.databinding.ActivityFindShopsBinding

/**
 * FindShopsActivity — Phase 5 shop list with live status indicators and detail navigation.
 *
 * WHAT IT DOES:
 *   1. Listens to all vendors live from /vendors, displaying their real-time OPEN/CLOSED status.
 *   2. Listens to the customer's per-shop balances from /points.
 *   3. Launches ShopDetailActivity when any shop card is tapped.
 */
class FindShopsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFindShopsBinding
    private val auth = FirebaseAuth.getInstance()

    private lateinit var shopAdapter: ShopAdapter
    private var vendorsListener: ValueEventListener? = null
    private var pointsListener: ValueEventListener? = null
    private val currentVendors = mutableListOf<Vendor>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFindShopsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        startLiveListeners()
    }

    private fun setupRecyclerView() {
        shopAdapter = ShopAdapter(emptyMap()) { vendorID ->
            openShopDetail(vendorID)
        }
        binding.rvShops.layoutManager = LinearLayoutManager(this)
        binding.rvShops.adapter = shopAdapter
    }

    private fun startLiveListeners() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvShops.visibility     = View.GONE
        binding.tvEmpty.visibility     = View.GONE

        val userID = auth.currentUser?.uid ?: return

        // 1. Listen live to customer balances
        pointsListener = PointRepository.listenToPointsForUser(
            userID = userID,
            onUpdate = { perShopBalance, _, _ ->
                shopAdapter.updateBalances(perShopBalance)
            },
            onError = { _ ->
                shopAdapter.updateBalances(emptyMap())
            }
        )

        // 2. Listen live to all vendors (live status sync)
        vendorsListener = VendorRepository.listenToAllVendors(
            onUpdate = { vendors ->
                currentVendors.clear()
                currentVendors.addAll(vendors)

                binding.progressBar.visibility = View.GONE
                if (vendors.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.rvShops.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.rvShops.visibility = View.VISIBLE
                    shopAdapter.submitList(vendors)
                }
            },
            onError = { _ ->
                binding.progressBar.visibility = View.GONE
                if (currentVendors.isEmpty()) {
                    binding.tvEmpty.text = "Could not load shops. Please check your connection."
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            }
        )
    }

    private fun openShopDetail(vendorID: String) {
        val intent = Intent(this, ShopDetailActivity::class.java).apply {
            putExtra(ShopDetailActivity.EXTRA_VENDOR_ID, vendorID)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        vendorsListener?.let { VendorRepository.removeAllVendorsListener(it) }
        vendorsListener = null
        pointsListener = null
    }
}
