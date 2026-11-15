---
name: SapotLokal
description: Neighbourhood loyalty wallet for small local shops; the customer app's world is Hadiah Utama, white ground, ink text, one orange for actions and a fixed colour per shop.
colors:
  bg: "#FFFFFF"
  ink: "#1A1C21"
  soft: "#5A606B"
  mute: "#9AA0AA"
  line: "#ECEEF2"
  fill: "#F4F5F7"
  accent: "#F5A524"
  accent-text: "#8A5700"
  accent-tint: "#FFF3DA"
  ok: "#13734A"
  ok-tint: "#E3F6EC"
  danger: "#B3261E"
  ripple: "#1A1C211F"
  shop-pink: "#E0436A"
  shop-indigo: "#4B4FC4"
  shop-purple: "#7B4FD6"
  shop-green: "#1A9E63"
  shop-blue: "#2F6FDE"
  shop-teal: "#0F8C8C"
  shop-berry: "#A9367F"
  shop-coral: "#E86A4A"
typography:
  big:
    fontFamily: "Figtree, sans-serif"
    fontSize: "52px"
    fontWeight: 600
    letterSpacing: "-0.03em"
  screen:
    fontFamily: "Figtree, sans-serif"
    fontSize: "28px"
    fontWeight: 800
    letterSpacing: "-0.02em"
  reward:
    fontFamily: "Figtree, sans-serif"
    fontSize: "24px"
    fontWeight: 600
  title:
    fontFamily: "Figtree, sans-serif"
    fontSize: "18px"
    fontWeight: 700
  body:
    fontFamily: "Figtree, sans-serif"
    fontSize: "15px"
    fontWeight: 500
  small:
    fontFamily: "Figtree, sans-serif"
    fontSize: "14px"
    fontWeight: 500
  link:
    fontFamily: "Figtree, sans-serif"
    fontSize: "15px"
    fontWeight: 700
  button:
    fontFamily: "Figtree, sans-serif"
    fontSize: "16px"
    fontWeight: 800
    letterSpacing: "0em"
  label:
    fontFamily: "Figtree, sans-serif"
    fontSize: "12px"
    fontWeight: 700
rounded:
  tick: "3px"
  bar: "4px"
  segment: "11px"
  pill: "12px"
  control: "14px"
  photo: "16px"
  card: "18px"
  shop-card: "20px"
  reward-card: "22px"
  full: "9999px"
spacing:
  xs: "4px"
  sm: "8px"
  row-gap: "10px"
  stack: "12px"
  md: "16px"
  page: "20px"
  lg: "24px"
components:
  button-primary:
    backgroundColor: "{colors.accent}"
    textColor: "{colors.ink}"
    typography: "{typography.button}"
    rounded: "{rounded.control}"
    height: "52px"
  button-outlined:
    backgroundColor: "{colors.bg}"
    textColor: "{colors.ink}"
    typography: "{typography.link}"
    rounded: "{rounded.control}"
    height: "48px"
  card:
    backgroundColor: "{colors.bg}"
    textColor: "{colors.ink}"
    rounded: "{rounded.card}"
    padding: "16px"
  shop-card:
    backgroundColor: "{colors.shop-indigo}"
    textColor: "{colors.bg}"
    rounded: "{rounded.shop-card}"
    padding: "16px 18px 18px"
  ready-pill:
    backgroundColor: "{colors.bg}"
    textColor: "{colors.shop-indigo}"
    rounded: "{rounded.pill}"
    padding: "3px 10px"
  reward-card:
    textColor: "{colors.ink}"
    rounded: "{rounded.reward-card}"
    padding: "16px"
  reward-row:
    backgroundColor: "{colors.bg}"
    textColor: "{colors.ink}"
    rounded: "{rounded.card}"
    padding: "12px"
    height: "80px"
  chip:
    backgroundColor: "{colors.fill}"
    textColor: "{colors.ink}"
    typography: "{typography.link}"
    rounded: "{rounded.full}"
    height: "44px"
  chip-selected:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.bg}"
  search-field:
    backgroundColor: "{colors.fill}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "0 14px"
    height: "52px"
  segment-selected:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.bg}"
    rounded: "{rounded.segment}"
  segment-track:
    backgroundColor: "{colors.fill}"
    rounded: "{rounded.control}"
    padding: "4px"
  nav-bar:
    backgroundColor: "{colors.bg}"
    textColor: "{colors.soft}"
    typography: "{typography.label}"
    height: "72px"
  nav-tab-active:
    textColor: "{colors.ink}"
  scan-button:
    backgroundColor: "{colors.bg}"
    textColor: "{colors.ink}"
    rounded: "{rounded.full}"
    size: "60px"
  progress-bar:
    backgroundColor: "{colors.line}"
    rounded: "{rounded.bar}"
    height: "8px"
---

# Design System: SapotLokal

Scope: this file describes the customer app (Theme.SapotLokal.Customer, applied to every customer activity). Sizes are written in px in the frontmatter for tool portability; on Android read px as dp, and as sp for type. The vendor app still runs on the legacy `sapot_*` system and is not described here.

Scope honesty: Scan, Leaderboard and Games were restyled by token swap only and keep their older structure. Known gaps: no night theme; empty, loading, error, voucher-QR and claim-dialog states have not yet been seen on device; no shop photos exist in test data.

## Overview

**Creative North Star: "Hadiah Utama"**

A rewards wallet for the neighbourhood, set on a plain white ground. Ink text carries the reading, one orange carries every action, and each shop arrives as its own bold colour card stamped with kopi cups, one cup per visit. The screen's big number (level score on Home and Play, points on a shop) sits large in a medium-heavy weight over a ticked bar, so the answer to "how am I doing" is the first thing read.

Density is calm and generous: one question per screen, cards stacked in a single column, 48dp-plus targets, and type that follows the system font scale (labels wrap rather than clip). The audience includes many older customers, so contrast stays high: secondary text is never lighter than the soft grey, and orange used as text drops to its dark text shade.

The colour job is split cleanly. Orange means "do this". Shop colours mean "whose points these are". Ink and greys mean everything else. Nothing decorative competes with the Claim button.

**Key Characteristics:**
- White ground, ink text, hairline-bordered white cards with no elevation.
- One orange for actions only: Claim, Scan ring, active tab bar, progress fill.
- Eight fixed shop colours (no orange or yellow), white text, assigned per shop for good.
- Kopi-cup stamps on every shop card; the same card on Home, Shops and the Shop page.
- Figtree throughout, bundled, from medium to extrabold; sentence case everywhere.
- Bottom bar with a raised white Scan circle ringed in orange; a start-edge rail on tablets.

## Colors

A neutral white-and-ink wallet with one warm action colour and a fixed set of saturated shop identities.

### Primary
- **Kopi Orange** (accent): the only action colour. Filled Claim and primary buttons, the 3dp ring of the Scan button, the active tab's top bar, progress fills on the ticked bar, the scan viewfinder corners, the loading spinner.
- **Roasted Orange Text** (accent-text): orange as text or as an idle icon tint on white; links like "How points work" and "All shops", Ready reward states in rows, Play tile icons. The pale **Orange Wash** (accent-tint) backs icon tiles and soft pills.

### Secondary
- **Shop palette** (shop-pink, shop-indigo, shop-purple, shop-green, shop-blue, shop-teal, shop-berry, shop-coral): one colour per shop, chosen by vendor id and fixed forever. Full strength behind shop cards and logos with white text; a light wash (about 13% alpha) behind that shop's reward card; about 20-24% alpha behind its photo or thumbnail fallback with the initials in full colour.

### Neutral
- **Paper White** (bg): page ground, cards, bottom bar, rail, Scan circle.
- **Kopi Ink** (ink): all main text, button labels on orange, selected chips and the selected segment.
- **Soft Slate** (soft): secondary text, idle tab labels, tick labels, search hint (the lightest grey allowed for text).
- **Mute Grey** (mute): ticks, dividers, idle icons only. Never text.
- **Hairline** (line): card borders, bar track, top edge of the bottom bar, outlined button stroke.
- **Quiet Fill** (fill): search field, unselected chips, segment track.
- **Open Green** (ok) with its tint (ok-tint): "Open now" dot and success text. **Danger Red** (danger): errors.

### Named Rules
**The One Orange Rule.** Orange is for actions only: Claim, Scan, the active tab and progress. Never use it for decoration, headings or a shop's identity. Orange as text always uses the accent-text shade.

**The Shop Owns Its Colour Rule.** A shop keeps the same palette colour on every screen. The shop palette excludes orange and yellow so a shop never competes with an action. On a shop card the Ready pill is white with the shop colour as its text, not orange.

## Typography

**Display Font:** Figtree (bundled variable font, OFL), with the system sans as fallback
**Body Font:** Figtree
**Label Font:** Figtree

**Character:** One friendly geometric sans across every role; hierarchy comes from weight (medium 500 to extrabold 800) and size, not from a second family.

### Hierarchy
- **Big** (600, 52, -0.03em, no font padding): the one big number per screen, level score on Home and Play.
- **Screen** (800, 28, -0.02em): screen titles such as Rewards, Shops, Play.
- **Reward** (600, 24): the reward name on a reward card.
- **Title** (700, 18; 17 on tiles and reward rows): card titles and section headings ("Your shops"), always sentence case.
- **Body** (500, 15, soft): supporting text; ink when it states a cost ("Uses 10 of your 20 points at Kopi Kita Cafe").
- **Small** (500, 14, soft): row subtitles and tile subtitles.
- **Link** (700, 15, accent-text): inline actions like "How points work"; given a 48dp tall hit area.
- **Button** (800, 16, no letter spacing, no caps): filled action buttons. Outlined buttons drop to 700 at 15.
- **Label** (700, 12): bottom bar and rail labels, tick labels under progress bars.

Shop cards use extrabold throughout on colour: name 18, points 22, initials 13, status line 700 at 15.

### Named Rules
**The Sentence Case Rule.** Section headings, buttons and labels are sentence case. No eyebrows or kickers above headings, no all-caps buttons.

**The Scale With The Phone Rule.** All type is in sp and follows the system font scale; containers wrap rather than clip at large sizes.

## Layout

A single column. Phone screens use a 20dp page edge, with cards and rows stacked at 10-16dp gaps and sections opened by a 22-26dp gap above a sentence-case heading. Inside cards, padding is 16dp (18dp horizontally on shop cards, 12dp on reward rows).

Every tab screen ends in the bottom bar (72dp) with five tabs: Home, Shops, Scan, Rewards, Play. Scan sits raised in the centre. Subpages (the Shop page, Me) use a back arrow and no bottom bar.

At smallest width of 600dp and up, the bottom bar becomes a 96dp navigation rail on the start edge with Scan on top (64dp circle) and 84dp tall tabs, and content is centred in a column of at most 640dp.

Every target is at least 48dp; primary buttons are 52dp tall, chips 44dp with touch-target expansion, list rows 72-80dp.

## Elevation & Depth

Flat by default. Cards and rows are white with a 1dp hairline and zero elevation; depth comes from colour, not shadow. Exactly two things lift: the shop colour card and the Scan button. Both use the platform's soft ambient elevation shadow.

### Shadow Vocabulary
- **Shop lift** (elevation 3dp): under shop colour cards so the stack reads as cards in a wallet.
- **Scan lift** (elevation 8dp): under the raised Scan circle on the bottom bar.

### Named Rules
**The Two Lifts Rule.** Only shop cards and the Scan button cast shadows. Everything else is flat with a hairline.

## Shapes

Gently rounded, getting rounder as surfaces get bigger: controls and fills 14dp, white cards and rows 18dp, shop cards 20dp, reward cards 22dp; photos clip at 16dp, pills at 12dp, the segment thumb at 11dp, progress bars at 4dp. Logos and the Scan button are full circles. Borders are 1dp hairlines on cards, 2dp on outlined buttons, 3dp orange on the Scan ring.

Icons are line icons drawn at a 2 unit stroke on a 24 unit grid, round caps and joins. The kopi cup is the system's own mark: outlined for a visit still to make, filled for a visit made.

## Components

### Buttons
Confident and wide; one per card.
- **Shape:** gently rounded (14dp).
- **Primary:** orange fill, ink label, extrabold 16, 52dp tall, usually full card width. Its label states the cost ("Claim for 10 points").
- **Pressed:** ink ripple at 12% over the fill; no elevation.
- **Outlined:** white with a 2dp hairline stroke, ink bold 15 label, 48dp tall; for secondary actions such as empty-state links.

### Chips
- **Style:** quiet fill with ink bold 15 text, no stroke, no check icon, 44dp tall.
- **State:** selected flips to ink fill with white text. Used for single-select scope and category filters on Shops.

### Cards / Containers
- **Corner Style:** 18dp for white cards, tiles and list rows.
- **Background:** white.
- **Shadow Strategy:** none (see Elevation & Depth).
- **Border:** 1dp hairline.
- **Internal Padding:** 16dp; Play and Me tiles are 120dp minimum with a 28dp orange-text icon; list rows are 72dp minimum with a trailing chevron.

### Inputs / Fields
- **Style:** search field on quiet fill, 14dp corners, 52dp tall, 14dp side padding, soft-slate search icon and hint, ink text at 16.
- **Focus / Error:** not yet styled beyond the platform default.

### Navigation
- **Bottom bar:** white, hairline on top, 72dp. Idle tabs are soft slate (bold 12 label, 24dp icon); the active tab turns ink and shows a 28 by 3dp orange bar at the top edge.
- **Scan:** a 60dp white circle with a 3dp orange ring and an ink scan icon, raised 8dp over the bar's centre.
- **Segmented switch (Rewards):** quiet fill track with 4dp padding; three equal-width segments (Ready, My vouchers, Working towards) with counts; the selected segment is ink with white text; labels wrap at large font sizes.
- **Subpages:** back arrow, no bottom bar. No overflow menus.

### Shop Card (signature)
The shop's full colour at 20dp corners with a 3dp lift. A white circular logo with initials in the shop colour, the shop name and its points in white extrabold, a row of 22dp kopi-cup stamps (8dp apart, at most 8), and one status line. A shop that is not ready shows the visits needed for its next reward, filled cups for visits made. A ready shop shows its earned visits as full cups and a white Ready pill with the shop colour as text. The same card appears on Home, Shops and the Shop page.

### Reward Card
A 22dp card washed in the shop's colour at low alpha. Logo, shop name and cost on top; reward name at 24; a line stating what claiming spends; a 140dp photo clipped at 16dp only when the shop has uploaded one; one full-width orange action.

### Reward Row
A white 18dp hairline card, 80dp minimum: a 56dp thumbnail (shop photo or tinted initials), name at 17, a small subtitle, a state line (accent-text when good, soft otherwise) and an optional compact orange button.

### Words on the surface
- **Points** always belong to one shop ("20 pts" on a shop card). **Level score** (MS "skor tahap") is the separate number for missions, games, the leaderboard and level.
- **Reward states:** Ready, Claimed, Used, Locked or Expired. Claiming spends the shop points at claim time.
- **Languages:** every string exists in English and Malay; the Home greeting ("Selamat pagi") is Malay in both locales.

### Ticked Progress Bar
An 8dp rounded bar, hairline track and orange fill, notched in white at each threshold with bold 12 soft labels below. Used for levels on Home and Play and for reward costs on a shop.

## Do's and Don'ts

### Do:
- **Do** keep orange (#F5A524) for actions only: Claim, Scan, the active tab, progress. Use #8A5700 whenever orange is text.
- **Do** give every shop its fixed palette colour by vendor id, with white text, on every screen it appears.
- **Do** make Claim state its cost, and say what it spends ("Uses 10 of your 20 points at Kopi Kita Cafe").
- **Do** use sentence case for headings, buttons and labels.
- **Do** keep every target at least 48dp and let type follow the system font scale.
- **Do** give subpages a back arrow and no bottom bar.
- **Do** keep the "How points work" link reachable from every main screen.

### Don't:
- **Don't** use orange or yellow as a shop colour, or orange for a shop's Ready pill.
- **Don't** add eyebrows or kickers above headings, or all-caps buttons.
- **Don't** put shadows on anything except shop cards and the Scan button.
- **Don't** use mute grey (#9AA0AA) for text.
- **Don't** give one destination two entry points; badges and leaderboard live in Play, log out lives in Me, and there are no overflow menus.
- **Don't** mix icon stroke weights; line icons are 2 units on a 24 grid.
