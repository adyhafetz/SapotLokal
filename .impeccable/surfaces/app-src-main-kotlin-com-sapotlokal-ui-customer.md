---
version: 1
slug: "app-src-main-kotlin-com-sapotlokal-ui-customer"
primary_target: "app/src/main/kotlin/com/sapotlokal/ui/customer"
related_targets: []
---

# Customer app flow

Scope: the whole customer app (Android Views, XML + Kotlin): Home, Shops, Shop page, Scan, Rewards, Play (missions, games, badges, leaderboard), Me. Mode: Operate. Build path: code-led.
Audience: neighbourhood customers of all ages, many older. Jobs: see what is ready and claim it; find where to earn; scan; use a voucher; play.
Constraints: bilingual EN/MS; numbers from AppConfig; claiming spends shop points at claim time (VoucherClaimHelper); vendor screens untouched; legacy sapot_* tokens stay for vendor.
Navigation: bottom bar Home · Shops · Scan (raised) · Rewards · Play on every tab screen; Me opens from the name/avatar on Home and a row in Play.
Language rules: shop "points" always belong to one shop; "level score" is for missions, games, leaderboard and level; reward states Ready · Claimed · Used · Locked; Claim states its cost.
Open decisions: points refund on expired claimed vouchers (PRODUCT.md).

## Direction contract

THESIS: A rewards wallet for your taman: every screen answers one question, and the loop scan → points at a shop → claim → show QR has one home per step. It refuses the category template of a points ring over a carousel and a separate tab for every feature.
OWN-WORLD: Hadiah Utama: white ground, ink #1A1C21 text, one orange #F5A524 for actions only, each shop its own bold colour card with kopi-cup stamps (one cup = one visit), big medium-weight numbers over a ticked bar, photo reward cards with a full-width Claim, Figtree throughout, 18–22dp radii, soft offset shadows only under raised cards and the Scan button.
STORY: The customer opens Home, sees Free Kopi is ready and that claiming uses 10 of 20 Kopi Kita points, claims it, finds it under Rewards › My vouchers to show at the counter, and checks Shops to see how far each shop's next reward is.
FIRST VIEWPORT: Home: "Selamat pagi" with name and avatar (opens Me) left; "530 level score" big with a bar ticked at each level; one large reward card in the shop's tint with logo, cost, reward name, photo and "Claim for 10 points"; next rewards as small rows; a Today in Play strip; bottom bar with raised white Scan.
FORM: Hadiah Utama rewards app (user-picked style round, assigned card) on the "Play gets a tab" structure (assigned card, seed:35d7e83b623ddf94).
SIGNATURE: stacked shop colour cards with cup stamps that fill one cup per visit, the same card on Shops, Home and the Shop page.
FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
