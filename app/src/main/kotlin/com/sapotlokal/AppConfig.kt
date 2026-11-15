package com.sapotlokal

/**
 * AppConfig — the single source of truth for every tunable number in SapotLokal.
 *
 * WHY THIS EXISTS: The build guide (Rule 8) requires that points per visit, level
 * thresholds, spin ranges, and voucher expiry live in one clearly named place so
 * they can be changed without hunting through screens.
 *
 * HOW TO USE: Import this object anywhere in the app and read its properties.
 * Never hardcode these numbers in an Activity, Fragment, or service.
 *
 * OPEN DECISIONS (from build guide Section 5):
 *   - SPIN_WHEEL_MIN / SPIN_WHEEL_MAX & MEMORY_MATCH_REWARD: confirmed by the game prototype;
 *     flagged for developer sign-off in Phase 6.
 *   - VOUCHER_EXPIRY_DAYS: developer must confirm in Phase 5 before auto-voucher is built.
 */
object AppConfig {

    // ── Firebase Realtime Database ─────────────────────────────────────────────

    /**
     * Regional database URL for the SapotLokal Firebase project.
     *
     * The database was created in the asia-southeast1 region.
     * Using the default URL (no region) causes the server to forcefully close
     * the connection with "Database lives in a different region".
     *
     * Always obtain the database reference via:
     *   FirebaseDatabase.getInstance(AppConfig.DATABASE_URL).reference
     * Never call FirebaseDatabase.getInstance() with no argument.
     */
    const val DATABASE_URL =
        "https://sapotlokal-app-default-rtdb.asia-southeast1.firebasedatabase.app"

    // ── Shop points (REFRACTOR.md §3) ────────────────────────────────────────

    /** The points-per-visit values a shop owner can pick from. Security rules allow exactly these. */
    val ALLOWED_POINTS_PER_VISIT: List<Int> = listOf(5, 10, 15, 20, 25, 30, 40, 50)

    /** Points per visit for a shop that hasn't picked a value. */
    const val DEFAULT_POINTS_PER_VISIT: Int = 10

    /** Shop points for one visit: the shop's points per visit times the customer's rank bonus, halves rounded up. */
    fun shopPointsForVisit(pointsPerVisit: Int, rank: Rank): Int =
        kotlin.math.floor(pointsPerVisit * rank.pointsBonus + 0.5).toInt()

    // ── XP (REFRACTOR.md §3.1) ───────────────────────────────────────────────

    /** XP for every valid shop visit. Flat: the rank bonus never applies to XP. */
    const val VISIT_XP: Int = 15

    /** Spin the Wheel segments, in wheel order, as XP. One is picked at random, once a day. */
    val SPIN_XP_SEGMENTS: List<Int> = listOf(2, 3, 4, 5, 6, 8, 10, 15)

    /** XP for a Memory Match win. */
    const val MEMORY_WIN_XP: Int = 5

    /** How many Memory Match wins a day pay XP. Later wins are for fun. */
    const val MEMORY_XP_WINS_PER_DAY: Int = 2

    /** Weekly leaderboard XP for 1st, 2nd and 3rd place. */
    val LEADERBOARD_XP: List<Int> = listOf(150, 100, 75)

    /** How many past weeks a top-3 reward can still be claimed. */
    const val LEADERBOARD_CLAIM_WEEKS: Int = 4

    /**
     * Ranks (REFRACTOR.md §3.3). XP never goes down, so a rank is never lost.
     * Names are Malay in both languages (they're names, not sentences).
     * [pointsBonus] multiplies shop points only, never XP.
     */
    enum class Rank(val level: Int, val code: String, val title: String, val minXp: Int, val pointsBonus: Double) {
        JIRAN_BARU(1, "jiran_baru", "Jiran Baru", 0, 1.0),
        PELANGGAN_SETIA(2, "pelanggan_setia", "Pelanggan Setia", 400, 1.25),
        KAKI_LOKAL(3, "kaki_lokal", "Kaki Lokal", 1_500, 1.5),
        OTAI_LOKAL(4, "otai_lokal", "Otai Lokal", 3_500, 2.0),
        LEGENDA_SAPOT(5, "legenda_sapot", "Legenda Sapot", 7_000, 3.0);

        /** The next rank up, or null at the top. */
        val next: Rank? get() = entries.getOrNull(ordinal + 1)

        companion object {
            fun fromXp(xp: Int): Rank = entries.last { xp >= it.minXp }
            fun fromLevel(level: Int): Rank = entries.firstOrNull { it.level == level } ?: JIRAN_BARU
        }
    }

    /**
     * Largest amount a single XP event may carry, by event-key prefix. Mirrors the security rules
     * on /xpEvents (docs/firebase-rules.json); change both together.
     */
    val XP_EVENT_MAX: Map<String, Int> = mapOf(
        "visit_" to 15,
        "game_" to 15,
        "wmission_" to 40,
        "lmission_" to 300,
        "badge_" to 100,
        "leaderboard_" to 150
    )

    /** Largest single raise of users/{uid}/xp in one write. Mirrors the security rules. */
    const val XP_MAX_PER_WRITE: Int = 300

    // ── Points ────────────────────────────────────────────────────────────────

    /**
     * Points a Customer earns each time they scan a valid Shop QR code.
     * Source: Flowchart says 10; Simple Guide says "e.g. 20". Default = 10 (Flowchart wins).
     * Build guide Open Decision #1.
     */
    const val POINTS_PER_SHOP_VISIT: Int = 10

    // ── Games ─────────────────────────────────────────────────────────────────

    /** Point values on the Spin the Wheel segments, in wheel order. One is picked uniformly at random. */
    val SPIN_WHEEL_SEGMENTS: List<Int> = listOf(5, 10, 15, 20, 25, 30, 40, 50)

    /** Derived from the segments so they can never disagree with the wheel. */
    val SPIN_WHEEL_MIN_POINTS: Int get() = SPIN_WHEEL_SEGMENTS.min()
    val SPIN_WHEEL_MAX_POINTS: Int get() = SPIN_WHEEL_SEGMENTS.max()

    /** Points for one Memory Match win (before the daily cap). */
    const val MEMORY_MATCH_REWARD_POINTS: Int = 20

    /** Max Memory Match points a customer can earn per calendar day (3 wins at 20). */
    const val MEMORY_MATCH_DAILY_CAP_POINTS: Int = 60

    /** Timezone used to decide when "a new day" starts for both games. */
    const val GAMES_RESET_TIME_ZONE: String = "Asia/Kuala_Lumpur"

    // ── Shop visit cooldown ───────────────────────────────────────────────────

    /**
     * Minimum minutes between two point-earning scans of the SAME shop by the same customer.
     * Stops one QR code being scanned over and over for points.
     * Set to 0 to disable while testing (then set it back before release).
     */
    const val SHOP_VISIT_COOLDOWN_MINUTES: Int = 720

    // ── Shop QR Code ──────────────────────────────────────────────────────────

    /**
     * How often the Shop QR code rotates, in seconds.
     * 60 seconds = TOTP-style rotation. Old screenshots can't be reused.
     */
    const val QR_ROTATION_SECONDS: Long = 60L

    // ── Vouchers ──────────────────────────────────────────────────────────────

    /**
     * How many days a customer voucher (VOUCHER_USER) is valid after it is created.
     * Build guide Open Decision #8 — no source specified. AI will ask the developer
     * to confirm this in Phase 5 before auto-voucher logic is built.
     * Placeholder: 30 days.
     */
    const val VOUCHER_EXPIRY_DAYS: Int = 30

    // ── Shop bonuses ──────────────────────────────────────────────────────────

    /** Extra shop points the first time a customer scans at a shop. */
    const val FIRST_VISIT_BONUS_POINTS: Int = 20

    /** Visits on this many different days in one week at the same shop earn the weekly bonus. */
    const val WEEKLY_STREAK_VISIT_DAYS: Int = 3

    /** Extra shop points for completing the weekly visit streak at a shop (once per shop per week). */
    const val WEEKLY_STREAK_BONUS_POINTS: Int = 20

    // ── Tiers (Gamification Overhaul) ─────────────────────────────────────────

    /**
     * Customer loyalty tiers based on Lifetime Accumulated Points.
     * Separated from spendable points balance (which decreases upon voucher redemption).
     */
    enum class CustomerTier(
        val code: String,
        val title: String,
        val emoji: String,
        val minPoints: Int,
        val maxPoints: Int,
        val multiplier: Double
    ) {
        FRESHIE("freshie", "Freshie", "🔰", 0, 199, 1.0),
        STEADY_BUYER("steady_buyer", "Steady Buyer", "🛒", 200, 599, 1.0),
        TREND_SETTER("trend_setter", "Trend Setter", "🔥", 600, 1499, 1.2),
        OTAI_LOKAL("otai_lokal", "Otai Lokal", "👑", 1500, 2499, 1.5),
        SAPOT_GOAT("sapot_goat", "Sapot GOAT", "🐐", 2500, Int.MAX_VALUE, 2.0);

        val nextTier: CustomerTier?
            get() = entries.getOrNull(ordinal + 1)

        companion object {
            fun fromLifetimePoints(points: Int): CustomerTier {
                return entries.lastOrNull { points >= it.minPoints } ?: FRESHIE
            }
        }
    }

    // ── Levels ────────────────────────────────────────────────────────────────

    /**
     * Lifetime-earned points required to reach each tier.
     * Level/Tier is computed from EARNED points only - it never goes down when a voucher
     * is redeemed.
     */
    val LEVEL_THRESHOLDS: List<Int> = CustomerTier.entries.map { it.minPoints }

    /**
     * Returns the level (1-indexed) for a given lifetime-earned point total.
     */
    fun levelForPoints(lifetimePoints: Int): Int {
        return CustomerTier.fromLifetimePoints(lifetimePoints).ordinal + 1
    }

    /**
     * Returns the point threshold to reach the NEXT tier, or null if already at max.
     */
    fun nextLevelThreshold(lifetimePoints: Int): Int? {
        return CustomerTier.fromLifetimePoints(lifetimePoints).nextTier?.minPoints
    }

    // ── Spin the Wheel daily limit ─────────────────────────────────────────────

    /**
     * A customer may spin the wheel at most once per calendar day.
     * The check is done against the database (build guide Rule 43 — "every check
     * runs against the live database, never data stored on the phone").
     */
    const val SPIN_WHEEL_DAILY_LIMIT: Int = 1
}
