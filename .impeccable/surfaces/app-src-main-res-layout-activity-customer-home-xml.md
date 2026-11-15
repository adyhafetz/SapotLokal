---
version: 1
slug: "app-src-main-res-layout-activity-customer-home-xml"
primary_target: "app/src/main/res/layout/activity_customer_home.xml"
related_targets: ["app/src/main/kotlin/com/sapotlokal/ui/customer/CustomerHomeActivity.kt"]
---

# Customer home

Scope: Customer home screen (Android Views, XML + Kotlin). Mode: Operate. Build path: code-led.
Audience: neighbourhood customers of all ages, many older, at the counter or between visits. Job: see which reward is ready, claim it, see how close they are at each shop.
Constraints: bilingual EN/MS; numbers from AppConfig; claim spends points at claim time (VoucherClaimHelper); keep the shared bottom nav; no black grounds.
Open decisions: points refund on expired claimed vouchers (PRODUCT.md).

## Direction contract

THESIS: Home is your shops' departure board in signal blue: each shop a row, its points in white flap cells, the reward you can claim lit by one yellow lamp. It refuses the rewards-app default of a points ring over a shop carousel.
OWN-WORLD: Signal-blue enamel frame (#1F4FB4) around white flaps with navy ink (#13203B) numerals split by a hairline; pale blue-grey page (#EEF2F8); yellow (#FFC20E) only on what can be pressed or claimed and the ready lamp. Barlow for text, Barlow Condensed for shop names and flap digits. No black anywhere.
STORY: The customer sees at once that Free Kopi is ready, understands claiming spends 10 of their 20 points, claims it, then scans the board for how far each shop's next reward is.
FIRST VIEWPORT: Blue top bar: time-of-day Malay greeting and name left, menu and a 56dp yellow Scan button right, level line with thin white progress below. Then the Now-ready card (white, yellow frame) with shop, reward, "Uses 10 of your 20 points" and a full "Claim for 10 points" button. Then the blue-framed board, header strip, one row per shop: lamp, condensed name, gap to next reward, flap digits. Tablet: card and board side by side.
FORM: Split-flap departure board (catalog challenger, fused, light colourway Biru), chosen by the user from hand three. seed:d82d51d3352909b6
SIGNATURE: when a shop's points rise, its flap digits flip cell by cell (rotationX, staggered) and the row stays highlighted until seen; instant change when animations are off.
FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
