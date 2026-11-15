package com.sapotlokal.ui.customer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener
import com.sapotlokal.AppConfig
import com.sapotlokal.R
import com.sapotlokal.data.model.ShopCategory
import com.sapotlokal.data.model.Vendor
import com.sapotlokal.data.model.Voucher
import com.sapotlokal.data.model.VoucherStatus
import com.sapotlokal.data.model.VoucherUser
import com.sapotlokal.data.repository.PointRepository
import com.sapotlokal.data.repository.VendorRepository
import com.sapotlokal.data.repository.VoucherRepository
import com.sapotlokal.databinding.ActivityShopDetailBinding
import com.sapotlokal.util.ServerClock
import com.sapotlokal.util.VoucherClaimHelper

/**
 * Shop page (Hadiah Utama). One job: "what can I get here?"
 * The shop's colour card (your points here, cup stamps, open status), a bar ticked at each of this
 * shop's reward costs, the rewards with Claim (spends points at claim time), then about, location,
 * hours and Maps. Viewing a shop follows it for "now open" alerts, as before.
 */
class ShopDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VENDOR_ID = "extra_vendor_id"
    }

    private lateinit var binding: ActivityShopDetailBinding
    private val auth = FirebaseAuth.getInstance()

    private var vendorId = ""
    private var vendor: Vendor? = null
    private var balance = 0
    private var myRank = AppConfig.Rank.JIRAN_BARU
    private var rules: List<Voucher> = emptyList()
    private var myVouchers: List<VoucherUser> = emptyList()

    private var vendorListener: ValueEventListener? = null
    private var pointsListener: ValueEventListener? = null
    private var rulesListener: ValueEventListener? = null
    private var vouchersListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vendorId = intent.getStringExtra(EXTRA_VENDOR_ID).orEmpty()
        if (vendorId.isBlank()) {
            Toast.makeText(this, getString(R.string.k_shop_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        com.sapotlokal.util.FollowedShops.follow(this, vendorId)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnHowPoints.setOnClickListener { HowPoints.show(this) }
        binding.btnOpenMaps.setOnClickListener {
            val address = vendor?.vendorAddress.orEmpty().trim()
            if (address.isBlank()) Toast.makeText(this, getString(R.string.k_shop_address_na), Toast.LENGTH_SHORT).show()
            else openInMaps(address)
        }
        loadData()

        // Subscribe to this shop's FCM topic so its "now open" alerts arrive.
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("shop_$vendorId")
    }

    private fun loadData() {
        vendorListener = VendorRepository.listenToVendor(
            vendorID = vendorId,
            onUpdate = { v ->
                if (v == null) {
                    Toast.makeText(this, getString(R.string.k_shop_not_available), Toast.LENGTH_SHORT).show()
                    finish()
                    return@listenToVendor
                }
                vendor = v
                binding.progressBar.visibility = View.GONE
                binding.layoutContent.visibility = View.VISIBLE
                render()
            },
            onError = { msg ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, getString(R.string.k_error_loading_shop, msg), Toast.LENGTH_SHORT).show()
            }
        )
        val uid = auth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            com.sapotlokal.data.repository.XpRepository.fetchStanding(uid) { myRank = it.rank; render() }
            pointsListener = PointRepository.listenToPointsForUser(
                userID = uid,
                onUpdate = { perShop, _, _ -> balance = perShop[vendorId] ?: 0; render() },
                onError = { render() }
            )
            vouchersListener = VoucherRepository.listenToVouchersForUser(
                userID = uid, onUpdate = { myVouchers = it; render() }, onError = { }
            )
        }
        rulesListener = VoucherRepository.listenToRulesForVendor(
            vendorID = vendorId,
            onUpdate = { r -> rules = r.filter { it.isActive }.sortedBy { it.pointsRequired }; render() },
            onError = { rules = emptyList(); render() }
        )
    }

    private fun render() {
        val v = vendor ?: return
        val name = v.vendorName.ifBlank { getString(R.string.k_local_shop) }
        binding.tvTitle.text = name

        val now = ServerClock.now()
        val held = myVouchers.filter {
            it.vendorID == vendorId && it.voucherStatus == VoucherStatus.ACTIVE &&
                (it.voucherExpiryDate == 0L || it.voucherExpiryDate > now)
        }.map { it.voucherID }.toSet()
        val claimable = rules.firstOrNull { it.voucherID !in held && it.pointsRequired > 0 && balance >= it.pointsRequired }
        val next = rules.firstOrNull { it.pointsRequired > balance }

        // Shop colour card
        binding.shopCardSlot.removeAllViews()
        val card = ShopCard.inflate(binding.shopCardSlot)
        val meta = listOf(
            ShopCategory.label(this, v.vendorCategory).ifBlank { null },
            getString(if (v.isOpen) R.string.hu_open else R.string.hu_closed)
        ).filterNotNull().joinToString(" · ")
        ShopCard.bind(card, v, vendorId, balance, claimable != null || held.isNotEmpty(), next, readyReward = claimable, meta = meta, perVisit = ShopStyle.perVisit(v, myRank))
        card.root.isClickable = false
        card.root.foreground = null
        binding.shopCardSlot.addView(card.root)

        val rate = v.pointsPerVisit.takeIf { it in AppConfig.ALLOWED_POINTS_PER_VISIT } ?: AppConfig.DEFAULT_POINTS_PER_VISIT
        val mine = ShopStyle.perVisit(v, myRank)
        binding.tvPointsRule.text = if (mine > rate) {
            getString(R.string.hu_points_rule_you, rate, mine)
        } else {
            getString(R.string.hu_points_rule, rate)
        }

        // Bar ticked at this shop's reward costs
        val costs = rules.map { it.pointsRequired }.filter { it > 0 }.distinct()
        if (costs.isEmpty()) {
            binding.rewardBar.visibility = View.GONE
        } else {
            binding.rewardBar.visibility = View.VISIBLE
            binding.rewardBar.set(balance, costs.max(), listOf(0) + costs)
        }

        // Rewards
        val rows = binding.rewardRows
        rows.removeAllViews()
        binding.tvNoVouchers.visibility = if (rules.isEmpty()) View.VISIBLE else View.GONE
        rules.forEach { rule ->
            val row = RewardRow.inflate(rows)
            val sub = listOf(getString(R.string.hu_n_pts, rule.pointsRequired), rule.voucherDescription.ifBlank { null })
                .filterNotNull().joinToString(" · ")
            when {
                rule.voucherID in held -> RewardRow.bind(
                    row, vendorId, name, rule.voucherName, sub, getString(R.string.hu_already_claimed), true,
                    getString(R.string.hu_show_qr)
                ) { startActivity(Intent(this, MyVouchersActivity::class.java)) }
                balance >= rule.pointsRequired && rule.pointsRequired > 0 -> RewardRow.bind(
                    row, vendorId, name, rule.voucherName, sub,
                    getString(R.string.hu_uses_points_at, rule.pointsRequired, balance, name), true,
                    getString(R.string.hu_claim, rule.pointsRequired)
                ) {
                    val uid = auth.currentUser?.uid.orEmpty()
                    VoucherClaimHelper.confirmAndClaim(this, uid, rule, name)
                }
                else -> RewardRow.bind(
                    row, vendorId, name, rule.voucherName, sub,
                    getString(R.string.hu_need_more, rule.pointsRequired - balance), false, null, null
                )
            }
            // A claim button says its cost on its own line here, so the row button stays short.
            if (row.btnAction.visibility == View.VISIBLE && rule.voucherID !in held) row.btnAction.text = getString(R.string.k_claim)
            rows.addView(row.root)
        }

        // About, location, hours, status
        binding.tvShopDescription.text = v.vendorDescription
        binding.tvShopDescription.visibility = if (v.vendorDescription.isBlank()) View.GONE else View.VISIBLE
        binding.tvShopAddress.text = v.vendorAddress.ifBlank { getString(R.string.k_address_not_specified) }
        binding.tvOperatingHours.text = getString(R.string.hu_hours, v.operatingHours.ifBlank { getString(R.string.k_hours_not_specified) })
        binding.tvShopStatus.text = getString(if (v.isOpen) R.string.hu_open else R.string.hu_closed)
        binding.tvShopStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
            getColor(if (v.isOpen) R.color.hu_ok_tint else R.color.hu_fill)
        )
        binding.tvShopStatus.setTextColor(getColor(if (v.isOpen) R.color.hu_ok else R.color.hu_soft))
    }

    private fun openInMaps(address: String) {
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps"))
        } catch (e: ActivityNotFoundException) {
            try { startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (e2: Exception) {
                Toast.makeText(this, getString(R.string.k_no_map_app), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        vendorListener?.let { VendorRepository.removeVendorListener(vendorId, it) }
        val uid = auth.currentUser?.uid
        if (uid != null) {
            pointsListener?.let { PointRepository.removePointsListener(uid, it) }
            vouchersListener?.let { VoucherRepository.removeVouchersListener(uid, it) }
        }
        rulesListener?.let { VoucherRepository.removeRulesListener(vendorId, it) }
    }
}
