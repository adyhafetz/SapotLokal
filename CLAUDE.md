# SapotLokal

Android loyalty app for small local shops in Malaysia. Customers scan a shop's QR to earn that shop's points (spent on rewards) and XP (moves them up 5 ranks). Shop owners manage rewards and redeem vouchers.

## Read first

- **[REFRACTOR.md](REFRACTOR.md)**: the active refactor plan. Work phase by phase, and tick each phase's "Done when" list.
- **[BRAND_DESIGN.md](BRAND_DESIGN.md)**: design tokens, components and copy rules. Every screen and string must follow it.

## Stack

- Kotlin, XML views with ViewBinding, minSdk 26. Single module `app`, package `com.sapotlokal`.
- Firebase via BoM: Auth (email/password), Realtime Database (asia-southeast1), Cloud Messaging.
- Firebase project: `sapotlokal-app` (see `.firebaserc`). The database URL lives in `AppConfig.DATABASE_URL`; always call `FirebaseDatabase.getInstance(AppConfig.DATABASE_URL)`.
- Security rules: `docs/firebase-rules.json`, deployed with `npx firebase-tools deploy --only database`.
- `functions/`: optional Cloud Function for shop-open push notifications (needs the Blaze plan).
- `tools/seed/`: Node script that seeds and cleans test data (see its README).

## Build and run

- Build: `./gradlew :app:assembleDebug`
- Unit tests: `./gradlew :app:testDebugUnitTest`
- Install on the connected phone: `./gradlew :app:installDebug`, then launch SapotLokal.
- `app/google-services.json` and `tools/seed/serviceAccountKey.json` are local secrets and are gitignored. Never commit them.

## Rules

1. Tunable numbers (points, XP, ranks, limits) live in `AppConfig.kt`, never in a screen.
2. Every reward write is one atomic multi-path `updateChildren`.
3. Checks (cooldowns, daily limits, claims) run against the database, never the phone's clock or local storage.
4. Every user-facing string exists in `values/` (English) and `values-ms/` (Malay).
5. When you replace code, delete the old code in the same change. No commented-out code, and no `Old`/`Legacy`/`V2` names.
