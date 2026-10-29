# SapotLokal — Phase 8 Implementation Guide: Gamification Overhaul & Merchant Live State

_Document Version:_ 2.0  
_Target Environment:_ `c:\dev\sapotlokal` (`feature/games-merge` or working branch)  
_Stack:_ Android Native, Kotlin 2.0, Gradle 9.6.0, AGP 9.4.0, JDK 21, compileSdk 37, minSdk 26, Firebase RTDB (`asia-southeast1`), Firebase Auth, FCM, SoundPool.

---

## Architecture Overview & Pre-requisites

This specification guides the step-by-step implementation of the revised gamification engine, shop operational statuses, weekly missions, sound effects, badge modals, and customer profile/leaderboard screens.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        SAPOTLOKAL ARCHITECTURE                         │
├───────────────────────────────────┬────────────────────────────────────┤
│           CUSTOMER TIER           │            VENDOR TIER             │
├───────────────────────────────────┼────────────────────────────────────┤
│ • 5 Tiers (Freshie ➔ Sapot GOAT)   │ • Manual OPEN/CLOSED Switch        │
│ • 6 Weekly Missions (Sun Reset)   │ • Operating Hours Configuration    │
│ • 8 Collectible Badges + Modal    │ • FCM Topic Broadcast on OPEN      │
│ • "Ka-ching!" Sound FX on Claim   │ • Live Status Sync to Shoppers     │
│ • Lifetime vs Spendable Points    │                                    │
│ • Customer Profile & Badges Grid  │                                    │
│ • Clickable Shop Details Screen   │                                    │
│ • Leaderboard (Top 3 Podium)      │                                    │
└───────────────────────────────────┴────────────────────────────────────┘
```

---

## Phase 1: Tier Architecture Overhaul & Points Dual Ledger

### 1.1 Objective
Replace the legacy Bronze–Platinum tier model with the 5 Malaysian localized tiers based on **Lifetime Accumulated Points**, separating lifetime points (tier progression) from spendable points (voucher redemption).

### 1.2 File Changes & Logic

1. **Update `app/src/main/java/com/sapotlokal/app/AppConfig.kt`:**
   * Define enum / data class for Tiers:
     ```kotlin
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

         companion object {
             fun fromLifetimePoints(points: Int): CustomerTier {
                 return entries.lastOrNull { points >= it.minPoints } ?: FRESHIE
             }
         }
     }
     ```
2. **Update `app/src/main/java/com/sapotlokal/app/data/model/User.kt`:**
   * Add fields:
     ```kotlin
     val lifetimePoints: Int = 0,
     val currentTier: String = CustomerTier.FRESHIE.code,
     val unlockedBadges: Map<String, Boolean> = emptyMap()
     ```
3. **Update `app/src/main/java/com/sapotlokal/app/data/repository/PointRepository.kt`:**
   * In all point-awarding functions (`recordShopVisit`, `awardMiniGamePoints`, `claimMissionReward`):
     * Calculate earned points using the user's current tier multiplier: `earnedPoints = (basePoints * tier.multiplier).toInt()`.
     * Atomically increment `/users/{userID}/lifetimePoints` using `ServerValue.increment(earnedPoints)`.
     * Calculate the new tier from total `lifetimePoints` and update `/users/{userID}/currentTier`.
   * When vouchers are redeemed in `VoucherRepository.redeemVoucher()`:
     * Only append to `/points` ledger (deducting spendable balance). **Do NOT decrease `lifetimePoints`**.
4. **Update `activity_customer_home.xml` & `CustomerHomeActivity.kt`:**
   * In the top greeting bar, bind the user's name with their tier emoji: `"${user.userName} ${tier.emoji}"`.
   * Display both metrics in the points hero card:
     * **Spendable Balance:** (e.g. `120 pts available`)
     * **Lifetime Level Progress:** (e.g. `Level: Otai Lokal 👑 • 1,650 Total Pts`)
   * Update the tier `ProgressBar` to interpolate within the active tier's point span (`minPoints` to `maxPoints`).

### 1.3 Expected Output Checklist (Phase 1)
- [ ] User with 0–199 total points shows `🔰 Freshie`.
- [ ] Crossing 200 points immediately upgrades status to `🛒 Steady Buyer`.
- [ ] Crossing 600 points upgrades to `🔥 Trend Setter`.
- [ ] Crossing 1,500 points upgrades to `👑 Otai Lokal`.
- [ ] Crossing 2,500 points upgrades to `🐐 Sapot GOAT`.
- [ ] Redeeming a voucher decreases spendable balance but leaves `lifetimePoints` and tier unchanged.
- [ ] Customer Home displays tier emoji next to the customer's name.

---

## Phase 2: Audio FX & Badge Pop-up Engine

### 2.1 Objective
Provide audio feedback on reward claims and an animated visual modal when unlocking badges.

### 2.2 File Changes & Logic

1. **Add Sound Resource:**
   * Place a cash register sound effect file at `app/src/main/res/raw/sound_kaching.mp3`.
2. **Create Audio Manager `app/src/main/java/com/sapotlokal/app/util/SoundManager.kt`:**
   * Implement singleton with `SoundPool` (supporting Android SDK 26+):
     ```kotlin
     class SoundManager(context: Context) {
         private val soundPool: SoundPool = SoundPool.Builder().setMaxStreams(2).build()
         private val kachingSoundId: Int = soundPool.load(context, R.raw.sound_kaching, 1)

         fun playKaching() {
             soundPool.play(kachingSoundId, 1f, 1f, 1, 0, 1f)
         }
     }
     ```
3. **Badge Definitions `app/src/main/java/com/sapotlokal/app/data/model/Badge.kt`:**
   * Define enum covering the 8 official badges:
     ```kotlin
     enum class BadgeDef(
         val id: String,
         val title: String,
         val description: String,
         val iconRes: Int
     ) {
         LANGKAH_KANAN("langkah_kanan", "Langkah Kanan", "Scanned your first shop visit.", R.drawable.ic_badge_first_scan),
         TAPAH_RANGER("tapah_ranger", "Tapah Ranger", "Visited 10 local shops.", R.drawable.ic_badge_shops_10),
         NASIB_BAIK("nasib_baik", "Nasib Baik", "Hit highest points on the Spin Wheel.", R.drawable.ic_badge_wheel_max),
         SHARP_BRAIN("sharp_brain", "Sharp Brain", "Achieved perfect score in Memory Match 10 times.", R.drawable.ic_badge_memory_perfect),
         HANTU_MALAM("hantu_malam", "Hantu Malam", "Scanned a shop visit after 8:00 PM.", R.drawable.ic_badge_night_owl),
         REGULAR("regular", "Regular", "Visited the same shop twice in one week.", R.drawable.ic_badge_repeat_visit),
         COMBO_MASTER("combo_master", "Combo Master", "Visit + Spin + Memory Match in one day.", R.drawable.ic_badge_combo),
         RAJA_SAPOT("raja_sapot", "Raja Sapot [Monthly]", "Completed all weekly challenges in a week.", R.drawable.ic_badge_raja);
     }
     ```
4. **Create Badge Unlock Dialog:**
   * Create `app/src/main/res/layout/dialog_badge_unlocked.xml`:
     * Rounded corner container (`@drawable/bg_dialog_rounded`).
     * Large centered badge `ImageView` with glow effect.
     * `TextView` for *"NEW BADGE UNLOCKED!"* header.
     * `TextView` for Badge Title and Description.
     * Primary action button *"Awesome!"* to dismiss.
   * Create helper `app/src/main/java/com/sapotlokal/app/util/BadgeDialogHelper.kt`:
     * Displays `AlertDialog` with custom view.
     * Triggers a subtle scale-up bounce animation (`res/anim/badge_bounce.xml`).
5. **Create Badge Unlock Dispatcher `app/src/main/java/com/sapotlokal/app/data/repository/BadgeRepository.kt`:**
   * `unlockBadge(activity: Activity, userId: String, badge: BadgeDef)`:
     * Reads `/users/{userId}/unlockedBadges/{badge.id}`.
     * If false/absent: writes `true` to Firebase, then calls `BadgeDialogHelper.show(activity, badge)`.

### 2.3 Expected Output Checklist (Phase 2)
- [ ] Triggering `SoundManager.playKaching()` produces the cash-register audio cue.
- [ ] Calling `unlockBadge()` for an unearned badge writes to Firebase and immediately presents the animated modal dialog.
- [ ] Tapping *"Awesome!"* dismisses the dialog cleanly.
- [ ] Subsequent triggers for an already-unlocked badge do not re-show the modal.

---

## Phase 3: Weekly Missions Engine

### 3.1 Objective
Replace one-time challenges with 6 automated recurring weekly missions that reset every Sunday at 23:59:59 `Asia/Kuala_Lumpur`.

### 3.2 File Changes & Logic

1. **Mission Date Utilities `app/src/main/java/com/sapotlokal/app/util/MissionDateUtils.kt`:**
   * `getWeekYearKey(): String`: Returns ISO week identifier in KL timezone (e.g. `"2026-W44"`).
   * `getWeekExpiresAtMillis(): Long`: Returns Sunday 23:59:59 epoch timestamp for current week.
   * `formatTimeRemaining(expiresAt: Long): String`: Returns `"X days left"` or `"X hours left"`.
2. **Weekly Mission Data Model `app/src/main/java/com/sapotlokal/app/data/model/WeeklyMission.kt`:**
   ```kotlin
   data class WeeklyMission(
       val missionId: String = "",
       val title: String = "",
       val description: String = "",
       val targetCount: Int = 1,
       val rewardPoints: Int = 0,
       val rewardBadgeKey: String? = null
   )
   ```
3. **Seed Static Missions in `app/src/main/java/com/sapotlokal/app/data/repository/MissionRepository.kt`:**
   Define the 6 standard missions:
   * **Mission 1 (`tapah_explorer`):** "Visit 3 different local shops in Tapah this week" | Target: 3 | 100 pts.
   * **Mission 2 (`loyal_supporter`):** "Visit the same shop twice in one week" | Target: 2 | 50 pts | Badge: `regular`.
   * **Mission 3 (`weekend_warrior`):** "Scan a shop visit on both Saturday and Sunday" | Target: 2 | 80 pts.
   * **Mission 4 (`daily_tri_play`):** "Scan a shop, spin the wheel, and play memory match on the same day" | Target: 3 | 75 pts | Badge: `combo_master`.
   * **Mission 5 (`night_owl`):** "Scan a shop visit after 8:00 PM on two different days" | Target: 2 | 60 pts | Badge: `hantu_malam`.
   * **Mission 6 (`arcade_champion`):** "Accumulate 200 points purely from mini-games before week ends" | Target: 200 | 80 pts.
4. **Mission Progress Evaluation Hooks:**
   * **In `ScanActivity.kt` on successful shop visit:**
     * Call `MissionRepository.recordShopVisitEvent(userId, vendorId, timestamp)`.
     * If timestamp is after 8:00 PM (20:00 local), increment Mission 5.
     * Check if day of week is Saturday/Sunday, evaluate Mission 3.
     * Evaluate Mission 1 (distinct vendor count) and Mission 2 (repeat visit).
     * Check first scan milestone ➔ unlock `langkah_kanan` badge.
     * Check total visits == 10 ➔ unlock `tapah_ranger` badge.
   * **In `SpinWheelActivity.kt` on win:**
     * Increment Mission 4 mini-game component.
     * Add earned points to Mission 6 progress.
     * If hit max segment (50 pts) ➔ unlock `nasib_baik` badge.
   * **In `MemoryMatchActivity.kt` on win:**
     * Increment Mission 4 mini-game component.
     * Add earned points to Mission 6 progress.
     * If 10th perfect game ➔ unlock `sharp_brain` badge.
5. **Reward Claim Flow in `ChallengesActivity.kt` (or `WeeklyMissionsActivity.kt`):**
   * Update item layout `item_challenge_card.xml`:
     * Displays title, description, reward points, progress bar (`current/target`), badge preview pill (if badge attached), and countdown timer (`"2 days left"`).
     * Action button:
       * When `currentProgress < targetCount`: Shows *"Start Now"* (routes to `ScanActivity` or `GamesMenuActivity`).
       * When `currentProgress >= targetCount && !isClaimed`: Shows enabled *"Claim Reward"* button.
       * When `isClaimed`: Shows *"Claimed"* disabled state.
   * **On Tapping "Claim Reward":**
     1. Mark user mission as `isClaimed = true` at `/userMissions/{userId}_{missionId}_{weekYearKey}`.
     2. Call `PointRepository.addPoints(userId, rewardPoints, PointSource.CHALLENGE)`.
     3. Call `SoundManager.playKaching()`.
     4. If mission has `rewardBadgeKey`, call `BadgeRepository.unlockBadge(this, userId, badgeDef)`.
     5. Check if all 6 weekly missions are completed ➔ unlock `raja_sapot` badge.

### 3.3 Expected Output Checklist (Phase 3)
- [ ] Weekly missions display current week countdown timer.
- [ ] Performing qualifying actions (e.g. night scan after 8:00 PM) increments progress on the respective mission.
- [ ] Tapping *"Claim Reward"* plays `sound_kaching.mp3`, awards points to user balance, updates tier progression, and displays badge modal if a badge was earned.
- [ ] Missions automatically reset progress under a new `weekYearKey` when the calendar advances past Sunday midnight.

---

## Phase 4: Customer Profile Screen & Dashboard Points Separation

### 4.1 Objective
Build the Customer Profile screen with badge collection showcase, and clearly separate Lifetime Accumulated Points from Available Spendable Points on the dashboard.

### 4.2 File Changes & Logic

1. **Create `app/src/main/res/layout/activity_customer_profile.xml`:**
   * **Profile Header:** Circular avatar, user name, email, and current tier pill (e.g. `👑 Otai Lokal`).
   * **Dual Points Metric Card:**
     * Left Column: `Spendable Points` (large font).
     * Right Column: `Lifetime Points` (large font).
   * **Tier Level Progress Bar:**
     * Visual meter showing points to next tier (e.g. `1,650 / 2,500 pts to Sapot GOAT`).
   * **Badges Showcase Grid:**
     * 2-column or 4-column `RecyclerView` (`item_badge_showcase.xml`).
     * Displays all 8 badges: Full color for unlocked badges; 40% opacity grayscale with lock icon for unearned badges.
     * Tapping any badge shows a toast or dialog with its unlock criteria.
   * **Actions:** Edit Profile button, Sign Out button.
2. **Implement `app/src/main/java/com/sapotlokal/app/ui/customer/CustomerProfileActivity.kt`:**
   * Fetch `/users/{userId}` to populate user details and `unlockedBadges` map.
   * Bind `BadgeAdapter` to render badge collection.
   * Sign Out button calls `FirebaseAuth.getInstance().signOut()` and routes to `RolePickerActivity`.
3. **Update Customer Home Navigation:**
   * Connect top profile icon or bottom navigation "Profile" tab to launch `CustomerProfileActivity`.

### 4.3 Expected Output Checklist (Phase 4)
- [ ] Navigating to Profile displays user info and current level badge beside their name.
- [ ] Dual metrics display correctly: Lifetime Points matches total points ever earned; Spendable Points matches current balance.
- [ ] Badge showcase displays all 8 badges, showing unlocked vs locked states accurately based on database flags.
- [ ] Tapping Sign Out clears session and returns user to onboarding.

---

## Phase 5: Shop Details Screen & Live Status Indicator

### 5.1 Objective
Make shop items in `FindShopsActivity` clickable, opening a rich Shop Details screen showing real-time OPEN/CLOSED status, operating hours, address, and available vouchers.

### 5.2 File Changes & Logic

1. **Update `item_shop_card.xml` in `FindShopsActivity`:**
   * Add a real-time status chip:
     * `OPEN`: Green background (`#10B981`), text `"● OPEN"`.
     * `CLOSED`: Red background (`#EF4444`), text `"● CLOSED"`.
   * Set root container to `android:clickable="true"` and `android:focusable="true"`.
2. **Create `app/src/main/res/layout/activity_shop_detail.xml`:**
   * Hero store image / category banner.
   * Shop Name with Live Status Chip (`OPEN` / `CLOSED`) directly beside the title.
   * Category tag (e.g. "Food & Beverage", "Bakery").
   * **Operating Hours Section:** Displays `operatingHours` string with clock icon.
   * **Location Section:** Full street address + *"Open in Google Maps"* button.
   * **Customer Shop Loyalty Card:** Displays customer's current points earned at this specific shop.
   * **Available Vouchers at this Shop:** Horizontal or vertical list of active reward rules from `/vouchers` for this vendor.
3. **Implement `app/src/main/java/com/sapotlokal/app/ui/customer/ShopDetailActivity.kt`:**
   * Receives `vendorId` via Intent extra.
   * Attaches live `ValueEventListener` to `/vendors/{vendorId}` to update `isOpen`, `operatingHours`, and details in real time.
   * Queries customer's visit points at this shop from `/points`.
   * Queries active vouchers from `/vouchers` where `vendorID == vendorId && isActive == true`.
   * *"Open in Google Maps"* button fires `ACTION_VIEW` intent with `geo:0,0?q={vendorAddress}`.
4. **Update `FindShopsActivity.kt` & `ShopAdapter.kt`:**
   * Pass click listener from adapter to launch `ShopDetailActivity` with the selected `vendorId`.

### 5.3 Expected Output Checklist (Phase 5)
- [ ] Every shop card in `FindShopsActivity` displays a real-time `OPEN` or `CLOSED` chip.
- [ ] Tapping any shop card opens `ShopDetailActivity`.
- [ ] Shop Details displays address, operating hours, customer's points at that store, and active vouchers.
- [ ] Tapping *"Open in Google Maps"* launches the external map application with the store's address preloaded.

---

## Phase 6: Vendor Manual Status Toggle & Customer Notification

### 6.1 Objective
Allow vendors to manually flip their shop status between OPEN and CLOSED on their dashboard, automatically notifying customers when the store opens.

### 6.2 File Changes & Logic

1. **Update `activity_vendor_home.xml`:**
   * Add a prominent **Store Status Card** at the top of the vendor dashboard:
     * Header: *"Store Operating Status"*
     * Large Toggle Switch or custom Dual-Segment Button (`OPEN` vs `CLOSED`).
     * Current status indicator:
       * When OPEN: Green container border, text *"Store is currently OPEN for customers"*.
       * When CLOSED: Red container border, text *"Store is currently CLOSED"*.
2. **Update `VendorHomeActivity.kt`:**
   * On toggle state change:
     * Write update to `/vendors/{vendorId}/isOpen` (`true` or `false`) and `/vendors/{vendorId}/lastStatusChangedAt = ServerValue.TIMESTAMP`.
     * If state switched to `true` (OPEN):
       * Trigger push notification dispatch to topic `/topics/shop_{vendorId}`.
3. **Update `ShopSettingsActivity.kt`:**
   * Add an editable text field for `operatingHours` (e.g. `"8:00 AM - 10:00 PM"`).
   * Save `operatingHours` to `/vendors/{vendorId}/operatingHours`.
4. **Customer Push Subscription:**
   * In `ScanActivity.kt` or `ShopDetailActivity.kt`:
     * When a customer scans at or favorites a shop, subscribe them to the FCM topic:  
       `FirebaseMessaging.getInstance().subscribeToTopic("shop_${vendorId}")`.
5. **Notification Handler in `SapotFcmService.kt`:**
   * When an FCM payload arrives with `type == "shop_status"`:
     * Display a system notification: `"${vendorName} is now OPEN! Drop by today to earn your loyalty points."`
     * Tapping notification launches `ShopDetailActivity` with `vendorId`.

### 6.3 Expected Output Checklist (Phase 6)
- [ ] Vendor can flip the OPEN/CLOSE switch on `VendorHomeActivity`.
- [ ] Toggling status updates `/vendors/{vendorId}/isOpen` in Firebase immediately.
- [ ] When vendor flips to OPEN, subscribed customer devices receive a push notification.
- [ ] When vendor changes status, any open customer `ShopDetailActivity` or `FindShopsActivity` screen updates the status chip from green to red (or vice-versa) in real time without refreshing.
- [ ] Vendor can update `operatingHours` in Shop Settings, and the updated hours reflect on the customer side.

---

## Phase 7: Community Leaderboard

### 7.1 Objective
Create a competitive ranking screen showcasing the top 3 customers on an elevated podium, a ranked list from 4th onwards, and a sticky "You" card pinned at the bottom.

### 7.2 File Changes & Logic

1. **Create `app/src/main/res/layout/activity_leaderboard.xml`:**
   * **Top Filter Tab:** Toggle between `"All-Time"` and `"This Week"`.
   * **Podium Container (Top 3):**
     * **1st Place (Center, tallest podium):** Gold crown icon, avatar with gold ring, customer name, tier badge, point count.
     * **2nd Place (Left, medium podium):** Silver medal, silver avatar ring, name, points.
     * **3rd Place (Right, lowest podium):** Bronze medal, bronze avatar ring, name, points.
   * **Ranked RecyclerView (4th place onwards):**
     * Item layout `item_leaderboard_row.xml`: Rank number, circular avatar, customer name with tier badge, points.
   * **Sticky "You" Card (Pinned at bottom of layout):**
     * Elevated card with distinct background tint.
     * Displays current user's rank (e.g. `#14`), avatar, name, tier badge, and points.
2. **Implement `app/src/main/java/com/sapotlokal/app/ui/customer/LeaderboardActivity.kt`:**
   * Query `/users` where `role == "customer"` ordered by `lifetimePoints` (descending, limited to top 50).
   * Populate 1st, 2nd, and 3rd place views with the top three users.
   * Populate `LeaderboardAdapter` with users ranked 4th to 50th.
   * Calculate current user's position in the dataset and bind to the sticky "You" bottom card.
   * Handle filter toggle:
     * When `"This Week"` is selected: calculate weekly points earned from `/points` within the current week's timestamp window.

### 7.3 Expected Output Checklist (Phase 7)
- [ ] Leaderboard loads and renders top 3 players on distinct podium heights with gold, silver, and bronze badges.
- [ ] Players ranked 4th and below appear in the scrollable list with correct rank numbers.
- [ ] Current user's rank is always visible on the sticky bottom card, regardless of scroll position.
- [ ] Display names include tier emojis (e.g. `Amirul 👑`).
- [ ] Toggling between All-Time and This Week switches ranking calculations.

---

## Comprehensive End-to-End Verification Checklist

Once all 7 phases are completed, execute this end-to-end user journey test:

| Test Case | Steps | Expected Result | Pass? |
| :--- | :--- | :--- | :---: |
| **TC-1: Level Progression** | Earn points via scan or game across 200, 600, 1500, and 2500 thresholds. | Tier instantly updates: Freshie ➔ Steady Buyer ➔ Trend Setter ➔ Otai Lokal ➔ Sapot GOAT. Emoji updates everywhere. | [ ] |
| **TC-2: Sound & Pop-up** | Complete a weekly mission and tap "Claim Reward". | "Ka-ching!" sound plays. Points awarded. If badge earned, animated badge modal pops up. | [ ] |
| **TC-3: Dual Ledger** | View Customer Profile, earn points, then redeem a voucher. | Lifetime points only increase. Spendable points decrease on redemption. Tier remains intact. | [ ] |
| **TC-4: Vendor Live Toggle** | Vendor flips shop to CLOSED on dashboard. | Customer app immediately updates shop chip to red "CLOSED". | [ ] |
| **TC-5: Vendor Open Push** | Vendor flips shop to OPEN. | Customer receives push notification stating shop is now open. | [ ] |
| **TC-6: Shop Detail** | Tap shop in Find Shops list. | Opens details page with address, hours, status chip, shop points, and map launcher. | [ ] |
| **TC-7: Leaderboard** | Open Leaderboard from navigation. | Top 3 podium renders with avatars, ranked list below, and sticky "You" card pinned at bottom. | [ ] |