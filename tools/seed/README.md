# SapotLokal seed data

Creates test data in your Firebase project: **10 shops, 15 rewards and 10 customers**, with logins, point
histories and a few claimed vouchers. One command removes it all again.

Everything the seeder creates has an id starting with `seed_`, so cleanup never touches real accounts.

## One-time setup

1. Install **Node.js 18 or newer** from https://nodejs.org (the LTS version is fine). Open a new PowerShell window afterwards.
2. In PowerShell:

   ```powershell
   cd C:\dev\sapotlokal\tools\seed
   npm install
   ```

3. Get a **service-account key**. It is a secret that gives full access to your Firebase project, so keep it out of git
   (this folder's `.gitignore` already ignores it) and never share it.
   - Firebase Console > Project settings (gear icon) > **Service accounts** > **Generate new private key**.
   - Save the file as `C:\dev\sapotlokal\tools\seed\serviceAccountKey.json`.

## Seed

```powershell
cd C:\dev\sapotlokal\tools\seed
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\dev\sapotlokal\tools\seed\serviceAccountKey.json"
$env:SEED_PASSWORD = "choose-a-password-6+"
npm run seed -- --yes
```

Without `--yes` it only prints what it would do. The password is only used for this run: it is never saved in a file.

## Logins

| Role | Email | Scenario |
|---|---|---|
| Vendor | `vendor01@seed.test` ... `vendor10@seed.test` | Owners of Kopi Kita Cafe, Roti Segar Bakery, Warung Mak Cik, Segar Mart, Bunga Florist, Buku Lokal, Gaya Salon, Manis Dessert, Jahit Pantas Tailor, Ping Hardware (in that order). Segar Mart and Gaya Salon start closed. |
| Customer | `customer01@seed.test` | Aisyah: brand new, no points |
| Customer | `customer02@seed.test` | Daniel: 10 points at Kopi Kita, 30 at Roti Segar |
| Customer | `customer03@seed.test` | Farah: 50 points at Kopi Kita (can claim either reward, not both) |
| Customer | `customer04@seed.test` | Kumar: holds an active claimed voucher; 35 points at Ping Hardware |
| Customer | `customer05@seed.test` | Mei Ling: one redeemed and one expired voucher |
| Customer | `customer06@seed.test` | Hakim: weekly-streak test, 2 visits this week at Manis Dessert. A scan on a third day earns the bonus |
| Customer | `customer07@seed.test` | Siti: Trend Setter, several claimable rewards and game points |
| Customer | `customer08@seed.test` | Raj: Otai Lokal level, mostly game and mission points |
| Customer | `customer09@seed.test` | Nurul: only game points, no shop points |
| Customer | `customer10@seed.test` | Chong Wei: small balances at several shops and an active voucher |

All use the password you set in `SEED_PASSWORD`.

Notes:
- Rewards: Gaya Salon's "Hair wash" is switched off on purpose.
- Timestamps are relative ("3 days ago"), so the data looks current whenever you seed. For the weekly-streak
  customer, seed on **Wednesday or later**, because his two visits must fall in the same Monday-to-Sunday week.
- Shop photos are not seeded; shops show the placeholder. Add real ones from the vendor app.
- A vendor's shop QR code is created only while that vendor has the Overview screen open, as usual.
- Re-running `seed` is safe. It overwrites the same ids and refreshes the timestamps.

## Change the data

Edit `seed.json` (shops, rewards, customers and their point histories), then run `seed` again. Each point row is
`{ "vendor": "v01", "source": "shop_visit", "earned": 10, "daysAgo": 3 }`. Use `"vendor": ""` for game or mission points.
A claim is `{ "reward": "r01", "status": "active", "daysAgo": 2 }` (status: `active`, `used` or `expired`); it also
writes the matching spend row, because points are spent when a voucher is claimed.

## Remove the seed data

```powershell
cd C:\dev\sapotlokal\tools\seed
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\dev\sapotlokal\tools\seed\serviceAccountKey.json"
npm run cleanup -- --yes
```

This deletes the seeded Auth accounts and every database record tied to a seeded account or shop. That includes points a
real customer earned at a seeded shop and vouchers they claimed there. Real accounts and real shops are not touched.

## When you are finished

Delete `serviceAccountKey.json` and, in the Firebase Console (Project settings > Service accounts), delete the key too.
The seeder writes with admin access, so it skips your security rules. After seeding, also sign in as a seeded customer and
vendor in the app to check the rules allow what the app does.
