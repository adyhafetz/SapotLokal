// Seeds SapotLokal with test data from seed.json.
//
//   npm run seed -- --yes
//
// Needs two environment variables (see README.md):
//   GOOGLE_APPLICATION_CREDENTIALS  path to your Firebase service-account key (a secret, never commit it)
//   SEED_PASSWORD                   the password every seeded account will use (6+ characters)
//
// Every id this script creates starts with "seed_", so cleanup.js can remove exactly this data.
// Running it twice is safe: the same ids are overwritten (timestamps move to "now").

const fs = require("fs");
const path = require("path");
const admin = require("firebase-admin");

const DATABASE_URL = "https://sapotlokal-cd37b-default-rtdb.asia-southeast1.firebasedatabase.app";
const DAY = 24 * 60 * 60 * 1000;
const VOUCHER_EXPIRY_DAYS = 30; // same as AppConfig.VOUCHER_EXPIRY_DAYS in the app

// Same thresholds as AppConfig.CustomerTier.
const TIERS = [
  { code: "freshie", min: 0 },
  { code: "steady_buyer", min: 200 },
  { code: "trend_setter", min: 600 },
  { code: "otai_lokal", min: 1500 },
  { code: "sapot_goat", min: 2500 },
];
const tierFor = (points) => TIERS.filter((t) => points >= t.min).pop().code;

function fail(message) {
  console.error("\n" + message + "\n");
  process.exit(1);
}

async function main() {
  const keyPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
  const password = process.env.SEED_PASSWORD;
  if (!keyPath || !fs.existsSync(keyPath)) {
    fail("Set GOOGLE_APPLICATION_CREDENTIALS to the path of your service-account key file (see README.md).");
  }
  if (!password || password.length < 6) {
    fail("Set SEED_PASSWORD to a password of at least 6 characters (see README.md).");
  }

  const seed = JSON.parse(fs.readFileSync(path.join(__dirname, "seed.json"), "utf8"));
  const credential = admin.credential.cert(JSON.parse(fs.readFileSync(keyPath, "utf8")));
  admin.initializeApp({ credential, databaseURL: DATABASE_URL });
  const projectId = JSON.parse(fs.readFileSync(keyPath, "utf8")).project_id;

  console.log(`Project:  ${projectId}`);
  console.log(`Database: ${DATABASE_URL}`);
  console.log(`Will create/update ${seed.vendors.length} shops, ${seed.rewards.length} rewards, ${seed.customers.length} customers.`);
  if (!process.argv.includes("--yes")) {
    fail("Nothing written. Run again with --yes to go ahead:  npm run seed -- --yes");
  }

  const now = Date.now();
  const domain = seed.emailDomain || "seed.test";
  const weekday = new Date().toLocaleDateString("en-GB", { weekday: "long", timeZone: "Asia/Kuala_Lumpur" });
  if (weekday === "Monday" || weekday === "Tuesday") {
    console.log(`\nNote: today is ${weekday}. The weekly-streak customer (Hakim) needs two visits earlier in the SAME week, so his data will not show the streak. Seed on Wednesday or later.`);
  }

  // ── 1. Auth accounts ───────────────────────────────────────────────────────
  const people = [
    ...seed.vendors.map((v, i) => ({
      uid: `seed_${v.key}`,
      email: `vendor${String(i + 1).padStart(2, "0")}@${domain}`,
      name: v.ownerName,
    })),
    ...seed.customers.map((c, i) => ({
      uid: `seed_${c.key}`,
      email: `customer${String(i + 1).padStart(2, "0")}@${domain}`,
      name: c.name,
    })),
  ];
  for (const p of people) {
    try {
      await admin.auth().getUser(p.uid);
      await admin.auth().updateUser(p.uid, { email: p.email, password, displayName: p.name, emailVerified: true });
    } catch (e) {
      if (e.code !== "auth/user-not-found") throw e;
      await admin.auth().createUser({ uid: p.uid, email: p.email, password, displayName: p.name, emailVerified: true });
    }
  }
  console.log(`Auth accounts ready: ${people.length}`);

  // ── 2. Database records, written in one batch ──────────────────────────────
  const updates = {};
  const vendorByKey = Object.fromEntries(seed.vendors.map((v) => [v.key, v]));
  const rewardByKey = Object.fromEntries(seed.rewards.map((r) => [r.key, r]));

  seed.vendors.forEach((v, i) => {
    const uid = `seed_${v.key}`;
    updates[`users/${uid}`] = {
      userID: uid,
      userName: v.ownerName,
      userEmail: `vendor${String(i + 1).padStart(2, "0")}@${domain}`,
      role: "vendor",
      lifetimePoints: 0,
      currentTier: "freshie",
    };
    updates[`vendors/${uid}`] = {
      vendorID: uid,
      userID: uid,
      vendorName: v.name,
      vendorCategory: v.category,
      vendorAddress: v.address,
      ssmNumber: v.ssm,
      vendorDescription: v.description,
      isOpen: v.isOpen,
      operatingHours: v.hours,
      lastStatusChangedAt: now,
    };
  });

  seed.rewards.forEach((r) => {
    const id = `seed_${r.key}`;
    updates[`vouchers/${id}`] = {
      voucherID: id,
      vendorID: `seed_${r.vendor}`,
      voucherName: r.name,
      voucherDescription: r.description,
      pointsRequired: r.points,
      isActive: r.active,
    };
  });

  let pointRows = 0;
  let voucherRows = 0;
  seed.customers.forEach((c, i) => {
    const uid = `seed_${c.key}`;
    let lifetime = 0;

    c.points.forEach((p, n) => {
      const pointID = `seed_p_${c.key}_${String(n + 1).padStart(2, "0")}`;
      lifetime += p.earned;
      updates[`points/${pointID}`] = {
        pointID,
        userID: uid,
        vendorID: p.vendor ? `seed_${p.vendor}` : "",
        pointEarnedAmount: p.earned,
        pointRedeemedAmount: 0,
        pointSource: p.source,
        timestamp: now - p.daysAgo * DAY,
      };
      pointRows++;
    });

    c.claims.forEach((claim, n) => {
      const reward = rewardByKey[claim.reward];
      const tag = `${c.key}_${String(n + 1).padStart(2, "0")}`;
      const claimedAt = now - claim.daysAgo * DAY;
      const claimPointID = `seed_p_${tag}_claim`;
      const voucherUserID = `seed_vu_${tag}`;
      // Points are spent when a voucher is claimed, so every claim has a matching spend row.
      updates[`points/${claimPointID}`] = {
        pointID: claimPointID,
        userID: uid,
        vendorID: `seed_${reward.vendor}`,
        pointEarnedAmount: 0,
        pointRedeemedAmount: reward.points,
        pointSource: "redemption",
        timestamp: claimedAt,
      };
      updates[`voucherUsers/${voucherUserID}`] = {
        voucherUserID,
        userID: uid,
        voucherID: `seed_${reward.key}`,
        vendorID: `seed_${reward.vendor}`,
        // Looks like the app's own codes: SAPOT- plus 6 characters (customer number, claim number, reward number).
        voucherCode: `SAPOT-S${String(i + 1).padStart(2, "0")}${n + 1}${reward.key.slice(1)}`,
        voucherStatus: claim.status === "used" ? "used" : claim.status === "expired" ? "expired" : "active",
        voucherExpiryDate: claimedAt + VOUCHER_EXPIRY_DAYS * DAY,
        pointsRequiredSnapshot: reward.points,
        voucherTitle: reward.name,
        createdAt: claimedAt,
        pointsDeducted: true,
        claimPointID,
      };
      pointRows++;
      voucherRows++;
    });

    updates[`users/${uid}`] = {
      userID: uid,
      userName: c.name,
      userEmail: `customer${String(i + 1).padStart(2, "0")}@${domain}`,
      role: "customer",
      lifetimePoints: lifetime,
      currentTier: tierFor(lifetime),
      ...(c.avatar ? { avatar: c.avatar } : {}),
    };
  });

  await admin.database().ref().update(updates);
  console.log(`Database written: ${seed.vendors.length} shops, ${seed.rewards.length} rewards, ${pointRows} point rows, ${voucherRows} vouchers.`);

  console.log("\nLogins (all use your SEED_PASSWORD):");
  people.forEach((p) => console.log(`  ${p.email}`));
  console.log("\nDone. Remove it all later with:  npm run cleanup -- --yes");
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
