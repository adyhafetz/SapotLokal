package com.sapotlokal.ui.customer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener
import com.sapotlokal.R
import com.sapotlokal.data.model.ShopCategory
import com.sapotlokal.data.model.Vendor
import com.sapotlokal.data.model.Voucher
import com.sapotlokal.data.model.VoucherStatus
import com.sapotlokal.data.model.VoucherUser
import com.sapotlokal.data.repository.PointRepository
import com.sapotlokal.data.repository.VendorRepository
import com.sapotlokal.data.repository.VoucherRepository
import com.sapotlokal.databinding.ActivityFindShopsBinding
import com.sapotlokal.util.ServerClock

/**
 * Shops tab (Hadiah Utama). One job: "where can I earn?"
 * Search, a scope filter (My shops · Open now · All), category chips built from the shops that
 * exist, then one stacked colour card per shop: points, cup stamps, open status, next reward.
 * Shops where the customer holds points come first; ready ones first of all.
 */
class FindShopsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFindShopsBinding
    private val auth = FirebaseAuth.getInstance()

    private var vendorsListener: ValueEventListener? = null
    private var pointsListener: ValueEventListener? = null
    private var rulesListener: ValueEventListener? = null
    private var vouchersListener: ValueEventListener? = null
    private var userId = ""

    private var allVendors: List<Vendor> = emptyList()
    private var perShop: Map<String, Int> = emptyMap()
    private var rules: List<Voucher> = emptyList()
    private var vouchers: List<VoucherUser> = emptyList()
    private var query = ""
    private var myRank = com.sapotlokal.AppConfig.Rank.JIRAN_BARU
    private var category = ALL
    private var scope = Scope.ALL

    private enum class Scope { MINE, OPEN, ALL }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFindShopsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnHowPoints.setOnClickListener { HowPoints.show(this) }
        binding.etSearch.doAfterTextChanged { query = it?.toString().orEmpty().trim(); render() }
        userId = auth.currentUser?.uid.orEmpty()
        buildScopeChips()
        startListeners()
        CustomerNav.bind(this, binding.customerNav, CustomerNav.Tab.SHOPS)
    }

    private fun buildScopeChips() {
        binding.chipScope.removeAllViews()
        listOf(
            Scope.MINE to R.string.hu_filter_mine,
            Scope.OPEN to R.string.hu_filter_open,
            Scope.ALL to R.string.hu_filter_all
        ).forEach { (s, label) ->
            binding.chipScope.addView(HuChip.make(this, getString(label), s == scope) { scope = s; render() })
        }
    }

    private fun startListeners() {
        binding.progressBar.visibility = View.VISIBLE
        if (userId.isNotBlank()) {
            com.sapotlokal.data.repository.XpRepository.fetchStanding(userId) { myRank = it.rank; render() }
            pointsListener = PointRepository.listenToPointsForUser(
                userID = userId,
                onUpdate = { shopPoints, _, _ ->
                    val first = perShop.isEmpty() && shopPoints.any { it.value > 0 }
                    perShop = shopPoints
                    if (first) { scope = Scope.MINE; buildScopeChips() }
                    render()
                },
                onError = { render() }
            )
            vouchersListener = VoucherRepository.listenToVouchersForUser(
                userID = userId, onUpdate = { vouchers = it; render() }, onError = { }
            )
        }
        rulesListener = VoucherRepository.listenToAllActiveRules(onUpdate = { rules = it; render() }, onError = { })
        vendorsListener = VendorRepository.listenToAllVendors(
            onUpdate = { v ->
                allVendors = v
                binding.progressBar.visibility = View.GONE
                rebuildCategoryChips()
                render()
            },
            onError = {
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.text = getString(R.string.k_could_not_load_shops)
                binding.tvEmpty.visibility = View.VISIBLE
            }
        )
    }

    /** One chip per category that exists, most common first, after "All". */
    private fun rebuildCategoryChips() {
        val categories = allVendors.map { categoryKey(it.vendorCategory) }.filter { it.isNotBlank() }
            .groupingBy { it.lowercase() }.eachCount().entries.sortedByDescending { it.value }
            .map { e -> allVendors.map { categoryKey(it.vendorCategory) }.first { it.lowercase() == e.key } }
        if (category != ALL && categories.none { it.equals(category, true) }) category = ALL
        binding.chipGroupCategories.removeAllViews()
        (listOf(ALL) + categories).forEach { key ->
            val label = if (key == ALL) getString(R.string.hu_all_types) else ShopCategory.label(this, key)
            binding.chipGroupCategories.addView(HuChip.make(this, label, key.equals(category, true)) { category = key; render() })
        }
    }

    private fun render() {
        if (allVendors.isEmpty()) return
        val now = ServerClock.now()
        val live = vouchers.filter {
            it.voucherStatus == VoucherStatus.ACTIVE && (it.voucherExpiryDate == 0L || it.voucherExpiryDate > now)
        }
        val held = live.map { it.voucherID }.toSet()
        fun readyRule(id: String): Voucher? = rules.filter {
            it.vendorID == id && it.voucherID !in held && it.pointsRequired > 0 && (perShop[id] ?: 0) >= it.pointsRequired
        }.minByOrNull { it.pointsRequired }
        fun nextRule(id: String): Voucher? = rules.filter { it.vendorID == id && it.pointsRequired > (perShop[id] ?: 0) }
            .minByOrNull { it.pointsRequired }

        val q = query.lowercase()
        val list = allVendors.filter { v ->
            val bal = perShop[v.vendorID] ?: 0
            val catText = ShopCategory.label(this, v.vendorCategory)
            (scope != Scope.MINE || bal > 0) &&
                (scope != Scope.OPEN || v.isOpen) &&
                (category == ALL || categoryKey(v.vendorCategory).equals(category, true)) &&
                (q.isEmpty() || "${v.vendorName} ${v.vendorAddress} ${v.vendorCategory} $catText".lowercase().contains(q))
        }.sortedWith(
            compareByDescending<Vendor> { readyRule(it.vendorID) != null || live.any { l -> l.vendorID == it.vendorID } }
                .thenByDescending { perShop[it.vendorID] ?: 0 }
                .thenByDescending { it.isOpen }
                .thenBy { it.vendorName.lowercase() }
        )

        val container = binding.shopCards
        container.removeAllViews()
        list.forEach { v ->
            val bal = perShop[v.vendorID] ?: 0
            val ready = readyRule(v.vendorID)
            val isReady = ready != null || live.any { it.vendorID == v.vendorID }
            val meta = listOf(
                ShopCategory.label(this, v.vendorCategory).ifBlank { null },
                getString(if (v.isOpen) R.string.hu_open else R.string.hu_closed)
            ).filterNotNull().joinToString(" · ")
            val card = ShopCard.inflate(container)
            ShopCard.bind(card, v, v.vendorID, bal, isReady, nextRule(v.vendorID), readyReward = ready, meta = meta, perVisit = ShopStyle.perVisit(v, myRank))
            if (bal == 0 && !isReady) card.tvStatus.text = "$meta · ${getString(R.string.hu_new_to_you)}"
            card.root.setOnClickListener {
                startActivity(Intent(this, ShopDetailActivity::class.java).putExtra(ShopDetailActivity.EXTRA_VENDOR_ID, v.vendorID))
            }
            container.addView(card.root, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (12 * resources.displayMetrics.density).toInt() })
        }
        binding.tvEmpty.text = getString(if (scope == Scope.MINE && q.isEmpty()) R.string.hu_no_shops_mine else R.string.hu_no_shops_match)
        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        vendorsListener?.let { VendorRepository.removeAllVendorsListener(it) }
        pointsListener?.let { if (userId.isNotBlank()) PointRepository.removePointsListener(userId, it) }
        rulesListener?.let { VoucherRepository.removeAllRulesListener(it) }
        vouchersListener?.let { if (userId.isNotBlank()) VoucherRepository.removeVouchersListener(userId, it) }
    }

    private fun categoryKey(raw: String): String = ShopCategory.fromStored(raw)?.value ?: raw.trim()

    private companion object {
        const val ALL = "All shops"
    }
}
