# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

Native Android, Kotlin + XML layouts with Material Design components, Android 8.0 (API 26) or newer. No iOS, Compose, or cross-platform work unless the developer explicitly asks. `src/` holds a React/Vite Figma Make prototype that serves as a design reference for the Android screens. It is not a shipped surface.

## Users

- **Customers:** everyday residents of a Malaysian neighbourhood (taman), all ages, including older and less tech-savvy people. They visit nearby independent shops (kopitiam, cafés, bakeries, kedai runcit, small boutiques). They open the app at the counter to scan a shop's QR code, check their points, and show a voucher. Between visits they play the daily games and missions.
- **Shop Owners (Vendors):** busy owners of small independent shops, working the counter themselves and often using the phone one-handed mid-service. At the counter they show the rotating shop QR and scan or type in voucher codes. Away from it they set up reward rules once and check visit and redemption stats.

Each account has exactly one role, chosen once at sign-up and never asked again at login.

## Product Purpose

SapotLokal rewards people for supporting local shops. Customers earn points by visiting shops (scanning the shop's QR code) and by playing in-app games and missions. When their points at a shop cross one of that shop's reward thresholds, the reward becomes claimable. The customer taps Claim to turn it into a voucher. Its point cost is deducted at claim time, and the voucher is valid for a set number of days after claiming. They redeem it at that shop.

Success means customers keep returning to independent neighbourhood shops instead of chains. It also means shop owners get a loyalty programme with no setup burden: set rules once, then just scan at the counter.

Intended for public production release (Play Store), not just a demo.

## Positioning

- **One wallet, many small shops.** Independent local shops share one app and one customer identity, instead of each running its own stamp card. Points stay per shop, though: points earned at Shop A never count toward Shop B's rewards.
- **Play, not just points.** Levels from lifetime points, Spin the Wheel, Memory Match, weekly missions, challenges, badges, and a community leaderboard make supporting local feel like a game.
- **Fair by design.** The shop QR rotates every 60 seconds (TOTP-style), so screenshots can't be reused. Every check runs against the live database, and daily game limits can't be bypassed by changing the phone clock.
- **Pride in local.** "Sapot Lokal" (support local) is a civic message about backing Malaysian small businesses. The tagline is "Sokong tempatan, kutip ganjaran."

## Operating Context

- **The two QR codes:** the Shop Code is shown by the shop owner, scanned by the customer, and earns points. The Voucher Code is shown by the customer and scanned by the shop owner, or typed in by hand as a fallback, and claims a reward.
- **Counter moments are fast and public.** Scanning, the "+N points" confirmation, and the "Give: Free Cake" instruction on redemption all happen in front of another person, often in a noisy, bright shop.
- **The core loop:** sign up → scan shop code → points → reward claimable → customer claims voucher → show voucher QR → shop scans or enters code → voucher used and points deducted.
- **Vendor side:** reward rules page (many rules per shop, each can be edited or turned off), rotating QR display, scan-to-redeem desk, shop settings (name, category, address, logo, open/closed status), daily visit and redemption stats.
- **Customer side:** home (level, progress, per-shop points), find shops, shop detail, scan, my vouchers grouped by shop (with "Best Choice" labelling and locked vouchers the customer can't currently afford), challenges and weekly missions, games, profile and badges, leaderboard, notifications.
- Bilingual: English and Bahasa Melayu (`values/` and `values-ms/`).

## Capabilities and Constraints

- **Stack:** Firebase Auth and Realtime Database (`asia-southeast1`), CameraX + ML Kit barcode scanning, and FCM for notifications. Auto-voucher creation runs client-side, protected by Security Rules.
- **Terminology:** use "reward rule" in UI text and explanations (`VOUCHER` is the data name) and "voucher" for a customer's issued copy (`VOUCHER_USER`).
- **Rules:**
  - A voucher belongs to one shop, can be redeemed exactly once, and disappears from active vouchers after use.
  - Claiming is a deliberate customer step (confirmed November 2026), replacing the build guide's original auto-issue rule. Claiming spends the reward's points immediately (`VoucherRepository` writes a redemption ledger row and sets `pointsDeducted`) and starts the voucher's validity period. Older vouchers issued before Claim existed still deduct at redemption. No refund on expiry is implemented; whether expired claimed vouchers return points is an open decision.
  - All qualifying vouchers stay visible. One the customer can't currently afford shows as locked, not deleted.
  - Level is derived from lifetime points earned, so spending never lowers a customer's level.
  - Tunable numbers (points per visit, game rewards, level thresholds, voucher expiry) live in config (`AppConfig.kt`), never hardcoded.
- **Shop owner sign-up** requires an SSM Registration Number. It is stored but not verified against any database. Shops sign up freely, with no approval step.
- **Scope source of truth:** `SAPOTLOKAL_BUILD_GUIDE.md` and its Open Decisions table. Don't invent features, screens, or tools that aren't in the source documents.

## Brand Commitments

- **Name:** SapotLokal. **Tagline:** "Sokong tempatan, kutip ganjaran."
- **Brand logo:** `app/src/main/res/drawable/ic_brand_logo.xml`.
- **Signature sound:** a "ka-ching" sound on earning (`app/src/main/res/raw/sound_kaching.mp3`).
- **Language:** English and Bahasa Melayu are both first-class. New UI text goes into string resources for both locales.

## Evidence on Hand

- The shops in the Figma prototype are placeholders: Kopi Kita, The Daily Knead, Ruang & Co., and Bunga Studio, all in Johor Bahru (`src/data.ts`, `public/images/`). There are no real partner shops, testimonials, customer counts, or usage statistics yet. Don't present the placeholders as real partners, and don't fabricate any of these.
- Design references: the Figma Make prototype in `src/`, the ERD and flowchart in `docs/`, and the progress reports in `docs/`.

## Product Principles

1. **The counter moment comes first.** Scanning, confirming points, and redeeming must be instant and readable at a glance, for a customer of any age and a shop owner with one free hand.
2. **Fairness is part of the product.** Anti-cheat guarantees (rotating QR, live-database checks, single-use vouchers) are never traded away for convenience.
3. **Never make the shop owner remember.** The app tells them what to give, tracks the rules, and does the voucher work automatically.
4. **Play rewards loyalty without replacing it.** Games and missions bring customers back to real shops. They never make the shop visit itself optional.
5. **Local and bilingual by default.** The product speaks to Malaysian neighbourhoods in both English and Bahasa Melayu, with pride rather than novelty.

## Accessibility & Inclusion

- The customer base spans all ages, including older and less tech-savvy users. Plain language, large touch targets, and readable text sizes matter, and the app should respect the system font scale.
- Shop owners use the app one-handed mid-service, so key vendor actions must be reachable and operable with one thumb.
- Counter screens (shop QR, voucher QR, redemption result) must stay legible in bright, busy shop environments.
- Every user-facing string must be available in both English and Bahasa Melayu.
