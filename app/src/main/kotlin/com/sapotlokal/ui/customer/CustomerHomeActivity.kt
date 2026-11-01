package com.sapotlokal.ui.customer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sapotlokal.AppConfig
import com.sapotlokal.AppConfig.CustomerTier
import com.sapotlokal.R
import com.sapotlokal.data.model.Vendor
import com.sapotlokal.data.repository.ChallengeRepository
import com.sapotlokal.data.repository.PointRepository
import com.sapotlokal.data.repository.VendorRepository
import com.sapotlokal.databinding.ActivityCustomerHomeBinding
import com.sapotlokal.service.SapotFcmService
import com.sapotlokal.ui.auth.LoginActivity
import com.sapotlokal.ui.games.GamesMenuActivity
import java.text.NumberFormat
import java.util.Locale

/**
 * CustomerHomeActivity — Phase 3 real implementation adapted for the new SapotLokal UI.
 *
 * WHAT IT DOES:
 * 1. Identifies the logged-in customer from FirebaseAuth.
 * 2. Attaches a live listener to /points filtered by userID.
 * 3. Computes lifetimeEarned → level + progress bar fill via AppConfig.
 * 4. Navigates to FindShopsActivity, ScanActivity, GamesMenuActivity, ChallengesActivity, or MyVouchersActivity.
 * 5. Handles bottom navigation and profile/logout menu.
 * 6. Syncs FCM registration token & prompts for push notification permissions (Phase 6).
 */
class CustomerHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerHomeBinding
    private var pointsListener: ValueEventListener? = null
    private var userListener: ValueEventListener? = null
    private val auth = FirebaseAuth.getInstance()
    private var currentUserName: String = ""
    private var currentUserTier: CustomerTier = CustomerTier.FRESHIE

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            auth.currentUser?.uid?.let { SapotFcmService.syncTokenForUser(it) }
        }
    }

    private var shopNotificationListener: com.google.firebase.database.ChildEventListener? = null
    private val sessionStartTime = System.currentTimeMillis()

    /** Adapter for the "Around your corner" recently-visited shops list (Phase 3 Task 3.2). */
    private val recentShopAdapter = RecentShopAdapter { vendorId ->
        val intent = Intent(this, ShopDetailActivity::class.java).apply {
            putExtra(ShopDetailActivity.EXTRA_VENDOR_ID, vendorId)
        }
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = auth.currentUser
        if (user == null) {
            // Defensive: if session expired, go back to login
            goToLogin()
            return
        }

        // Set up the recent shops RecyclerView (Phase 3 Task 3.2)
        binding.rvRecentShops.layoutManager = LinearLayoutManager(this)
        binding.rvRecentShops.adapter = recentShopAdapter

        // Load cached user name & tier to prevent greeting flicker
        val prefs = getSharedPreferences("sapot_user_prefs", MODE_PRIVATE)
        val cachedName = prefs.getString("cached_user_name", "") ?: ""
        val cachedTierCode = prefs.getString("cached_user_tier", CustomerTier.FRESHIE.code)
        currentUserTier = CustomerTier.entries.firstOrNull { it.code == cachedTierCode } ?: CustomerTier.FRESHIE
        if (cachedName.isNotBlank()) {
            currentUserName = cachedName
        }
        updateGreeting(currentUserTier)

        startListeningToUser(user.uid)
        startListeningToPoints(user.uid)

        // Quick Actions & Navigation
        binding.btnScan.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }
        binding.btnGames.setOnClickListener {
            startActivity(Intent(this, GamesMenuActivity::class.java))
        }
        binding.btnChallenges.setOnClickListener {
            startActivity(Intent(this, ChallengesActivity::class.java))
        }
        binding.btnMyVouchers.setOnClickListener {
            startActivity(Intent(this, MyVouchersActivity::class.java))
        }
        binding.btnLeaderboard.setOnClickListener {
            startActivity(Intent(this, LeaderboardActivity::class.java))
        }
        binding.btnExplore.setOnClickListener {
            startActivity(Intent(this, FindShopsActivity::class.java))
        }

        // Notification permission & FCM token synchronization (Phase 6)
        checkNotificationPermission(user.uid)
        startListeningToShopNotifications()
        ChallengeRepository.syncUserProgress(user.uid)
        com.sapotlokal.data.repository.MissionRepository.syncUserMissions(user.uid)

        // Bottom Navigation
        binding.bottomNav.tabHome.setOnClickListener {
            // Already on Home
        }
        binding.bottomNav.tabDiscover.setOnClickListener {
            startActivity(Intent(this, FindShopsActivity::class.java))
        }
        binding.bottomNav.tabMyShops.setOnClickListener {
            startActivity(Intent(this, MyVouchersActivity::class.java))
        }
        binding.cardRewardNudge.setOnClickListener {
            startActivity(Intent(this, MyVouchersActivity::class.java))
        }
        binding.bottomNav.tabProfile.setOnClickListener {
            startActivity(Intent(this, CustomerProfileActivity::class.java))
        }

        // Hero Points Card navigates directly to Profile & Badges showcase
        binding.cardPointsHero.setOnClickListener {
            startActivity(Intent(this, CustomerProfileActivity::class.java))
        }

        // Top notification bell options
        binding.btnNotification.setOnClickListener { view ->
            showProfileMenu(view)
        }
    }

    private fun showProfileMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_customer_home, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_profile -> {
                    startActivity(Intent(this, CustomerProfileActivity::class.java))
                    true
                }
                R.id.action_leaderboard -> {
                    startActivity(Intent(this, LeaderboardActivity::class.java))
                    true
                }
                R.id.action_logout -> {
                    auth.signOut()
                    goToLogin()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // ── Points listener ───────────────────────────────────────────────────────

    private fun startListeningToUser(uid: String) {
        val userRef = FirebaseDatabase.getInstance(AppConfig.DATABASE_URL)
            .reference.child("users").child(uid)
        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("userName").getValue(String::class.java)?.trim() ?: ""
                if (name.isNotBlank()) {
                    currentUserName = name
                }
                val dbLifetime = snapshot.child("lifetimePoints").getValue(Long::class.java)?.toInt() ?: 0
                if (dbLifetime > 0) {
                    val tierFromLifetime = CustomerTier.fromLifetimePoints(dbLifetime)
                    if (tierFromLifetime.ordinal >= currentUserTier.ordinal) {
                        currentUserTier = tierFromLifetime
                    }
                }
                updateGreeting(currentUserTier)
                getSharedPreferences("sapot_user_prefs", MODE_PRIVATE).edit()
                    .putString("cached_user_name", currentUserName)
                    .putString("cached_user_tier", currentUserTier.code)
                    .apply()
            }

            override fun onCancelled(error: DatabaseError) {
                // Keep placeholder on network error
            }
        }
        userRef.addValueEventListener(userListener!!)
    }

    private fun updateGreeting(tier: CustomerTier) {
        val displayName = if (currentUserName.isNotBlank()) currentUserName else "Shopper"
        binding.tvGreeting.text = "$displayName ${tier.emoji}"
    }

    private fun startListeningToPoints(userID: String) {
        pointsListener = PointRepository.listenToPointsForUser(
            userID = userID,
            onUpdate = { perShopBalance, lifetimeEarned, spendableBalance ->
                updateUi(lifetimeEarned, spendableBalance)
                loadRecentShops(perShopBalance)
            },
            onError  = { /* keep showing last values; network error handled in ScanActivity */ }
        )
    }

    /**
     * Given the per-shop balance map (vendorID → balance), fetches each vendor's
     * name and category from /vendors and submits a sorted list to the adapter.
     *
     * Vendors with a zero-or-negative balance are excluded (shouldn't happen in normal
     * flow, but guards against stale data).
     *
     * The list is sorted descending by balance so the shop with the most points
     * appears first — motivating the customer to keep earning.
     */
    private fun loadRecentShops(perShopBalance: Map<String, Int>) {
        val visitedVendorIDs = perShopBalance.keys.filter { it.isNotBlank() && (perShopBalance[it] ?: 0) > 0 }

        if (visitedVendorIDs.isEmpty()) {
            recentShopAdapter.submitList(emptyList())
            binding.tvNoShops.visibility    = View.VISIBLE
            binding.rvRecentShops.visibility = View.GONE
            return
        }

        // Accumulate resolved items; submit once all lookups complete.
        val resolved = mutableListOf<RecentShopItem>()
        var remaining = visitedVendorIDs.size

        for (vendorID in visitedVendorIDs) {
            VendorRepository.fetchVendorById(
                vendorID = vendorID,
                onResult = { vendor ->
                    val balance = perShopBalance[vendorID] ?: 0
                    if (vendor != null && balance > 0) {
                        synchronized(resolved) {
                            resolved.add(
                                RecentShopItem(
                                    vendorID  = vendorID,
                                    shopName  = vendor.vendorName.ifBlank { "Unknown Shop" },
                                    category  = vendor.vendorCategory.ifBlank { "Shop" },
                                    balance   = balance,
                                    isOpen    = vendor.isOpen
                                )
                            )
                        }
                    }
                    synchronized(resolved) {
                        remaining--
                        if (remaining == 0) {
                            // All lookups done — sort and submit on the main thread
                            val sorted = resolved.sortedByDescending { it.balance }
                            runOnUiThread {
                                recentShopAdapter.submitList(sorted)
                                binding.tvNoShops.visibility     = View.GONE
                                binding.rvRecentShops.visibility = View.VISIBLE
                            }
                        }
                    }
                },
                onError = { _ ->
                    synchronized(resolved) {
                        remaining--
                        if (remaining == 0) {
                            val sorted = resolved.sortedByDescending { it.balance }
                            runOnUiThread {
                                recentShopAdapter.submitList(sorted)
                                binding.tvNoShops.visibility     = if (sorted.isEmpty()) View.VISIBLE else View.GONE
                                binding.rvRecentShops.visibility = if (sorted.isEmpty()) View.GONE else View.VISIBLE
                            }
                        }
                    }
                }
            )
        }
    }

    private fun updateUi(lifetimeEarned: Int, spendableBalance: Int) {
        val tier = CustomerTier.fromLifetimePoints(lifetimeEarned)
        currentUserTier = tier
        updateGreeting(tier)

        getSharedPreferences("sapot_user_prefs", MODE_PRIVATE).edit()
            .putString("cached_user_name", currentUserName)
            .putString("cached_user_tier", currentUserTier.code)
            .apply()

        auth.currentUser?.uid?.let { uid ->
            val updates = hashMapOf<String, Any>(
                "lifetimePoints" to lifetimeEarned,
                "currentTier" to tier.code
            )
            FirebaseDatabase.getInstance(AppConfig.DATABASE_URL).reference
                .child("users").child(uid).updateChildren(updates)
        }

        // 1. Spendable Balance Metric
        binding.tvSpendablePoints.text = spendableBalance.toString()
        binding.tvSpendableLabel.text = "pts available"
        binding.tvLifetimePoints.text = lifetimeEarned.toString()

        // 2. Lifetime Level Progress Metric
        val formattedLifetime = NumberFormat.getNumberInstance(Locale.US).format(lifetimeEarned)
        binding.tvLifetimeLevelProgress.text = "Level: ${tier.title} ${tier.emoji} • $formattedLifetime Total Pts"
        binding.chipLevel.text = "${tier.title} ${tier.emoji} · ${tier.multiplier}x Bonus"

        // 3. Tier Progress Bar (Interpolate within active tier's point span)
        val nextTier = tier.nextTier
        if (nextTier == null) {
            // Maximum tier reached (Sapot GOAT)
            binding.progressLevel.progress = 100
            binding.tvProgressLabel.text = "🏆 Max Tier Reached (${tier.title} ${tier.emoji})"
            binding.tvProgressCount.text = "$formattedLifetime pts"
            binding.tvMaxLevel.visibility = View.VISIBLE
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
            binding.tvProgressLabel.text = "$ptsNeeded pts to ${nextTier.title} ${nextTier.emoji}"
            binding.tvProgressCount.text = "$formattedLifetime / $formattedNext pts"
            binding.tvMaxLevel.visibility = View.GONE
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onStop() {
        super.onStop()
        val uid = auth.currentUser?.uid ?: return
        pointsListener?.let { PointRepository.removePointsListener(uid, it) }
        pointsListener = null
        userListener?.let {
            FirebaseDatabase.getInstance(AppConfig.DATABASE_URL).reference
                .child("users").child(uid).removeEventListener(it)
        }
        userListener = null
    }

    override fun onStart() {
        super.onStart()
        val uid = auth.currentUser?.uid ?: return
        if (pointsListener == null) {
            startListeningToPoints(uid)
        }
        if (userListener == null) {
            startListeningToUser(uid)
        }
        ChallengeRepository.syncUserProgress(uid)
        com.sapotlokal.data.repository.MissionRepository.syncUserMissions(uid)
    }

    override fun onResume() {
        super.onResume()
        updateGreeting(currentUserTier)
    }

    private fun checkNotificationPermission(userID: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!isGranted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                SapotFcmService.syncTokenForUser(userID)
            }
        } else {
            SapotFcmService.syncTokenForUser(userID)
        }
    }

    private fun startListeningToShopNotifications() {
        if (shopNotificationListener != null) return
        val notifRef = com.google.firebase.database.FirebaseDatabase
            .getInstance(AppConfig.DATABASE_URL)
            .reference
            .child("shopNotifications")

        val listener = object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                if (timestamp < sessionStartTime) return // Only notify for real-time events during this session

                val isOpen = snapshot.child("isOpen").getValue(Boolean::class.java) ?: false
                if (!isOpen) return

                val vendorId = snapshot.child("vendorId").getValue(String::class.java)
                    ?: snapshot.child("vendorID").getValue(String::class.java).orEmpty()
                val vendorName = snapshot.child("vendorName").getValue(String::class.java).orEmpty().ifBlank { "A shop you follow" }

                SapotFcmService.showShopStatusNotification(this@CustomerHomeActivity, vendorId, vendorName)
            }

            override fun onChildChanged(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}
            override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }

        notifRef.limitToLast(10).addChildEventListener(listener)
        shopNotificationListener = listener
    }

    override fun onDestroy() {
        super.onDestroy()
        shopNotificationListener?.let {
            com.google.firebase.database.FirebaseDatabase
                .getInstance(AppConfig.DATABASE_URL)
                .reference
                .child("shopNotifications")
                .removeEventListener(it)
        }
        shopNotificationListener = null
    }

    private fun goToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
}
