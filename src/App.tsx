import { useEffect, useRef, useState } from "react"
import { Button, Icon, Modal, Progress, QR } from "./ui"
import { challenges, initialVouchers, shops, type Voucher } from "./data"

type Page = "home" | "challenges" | "scan" | "vouchers" | "profile" | "games" | "shops" | "shop" | "leaderboard"
type Dialog = "notifications" | "redeem" | "editProfile" | "password" | "createVoucher" | "editVoucher" | "deleteVoucher" | "deleteAccount" | "logout" | "help" | null
const customerNav = [
  { page: "home", label: "Home", icon: "home" },
  { page: "challenges", label: "Challenges", icon: "challenge" },
  { page: "scan", label: "Scan & earn", icon: "scan" },
  { page: "vouchers", label: "Vouchers", icon: "voucher" },
  { page: "profile", label: "My profile", icon: "user" },
]
const vendorNav = [
  { page: "home", label: "Overview", icon: "home" },
  { page: "vouchers", label: "Vouchers", icon: "voucher" },
  { page: "scan", label: "Scanner", icon: "scan" },
  { page: "profile", label: "Shop profile", icon: "shop" },
]
const dateKey = () => new Date().toLocaleDateString("en-CA")
const getDaily = (key: string) => {
  try {
    const value = JSON.parse(localStorage.getItem(key) || "{}")
    return value.date === dateKey() ? Number(value.value) || 0 : 0
  } catch {
    return 0
  }
}
const setDaily = (key: string, value: number) => {
  try {
    localStorage.setItem(key, JSON.stringify({ date: dateKey(), value }))
  } catch {
    /* Private browsing can disable storage. */
  }
}

export default function App() {
  const [role, setRole] = useState<"customer" | "vendor">("customer")
  const [page, setPage] = useState<Page>("home")
  const [dialog, setDialog] = useState<Dialog>(null)
  const [toast, setToast] = useState("")
  const [points, setPoints] = useState(1240)
  const [name, setName] = useState("Aina")
  const [fullName, setFullName] = useState("Aina Abdullah")
  const [email, setEmail] = useState("aina.abdullah@gmail.com")
  const [phone, setPhone] = useState("+60 12-345 6789")
  const [shopName, setShopName] = useState("Kopi Kita")
  const [shopCategory, setShopCategory] = useState("Café / F&B")
  const [shopAddress, setShopAddress] = useState(
    "18, Jalan Dhoby, Bandar Johor Bahru",
  )
  const [shopHours, setShopHours] = useState("8:00 AM – 6:00 PM")
  const [ssmNumber, setSsmNumber] = useState("202301024589 (1520632-X)")
  const [document, setDocument] = useState("SSM_Registration_KopiKita.pdf")
  const [draftDocument, setDraftDocument] = useState(document)
  const [open, setOpen] = useState(true)
  const [vouchers, setVouchers] = useState<Voucher[]>(initialVouchers)
  const [selectedVoucher, setSelectedVoucher] = useState<Voucher | null>(null)
  const [redeemed, setRedeemed] = useState<number[]>([])
  const [shop, setShop] = useState(shops[0])
  const [filter, setFilter] = useState("All shops")
  const [category, setCategory] = useState("All shops")
  const [search, setSearch] = useState("")
  const [claimed, setClaimed] = useState(false)
  const [loggedOut, setLoggedOut] = useState(false)
  const [deleted, setDeleted] = useState(false)
  const [game, setGame] = useState<"overview" | "wheel" | "memory">("overview")
  const [spinUsed, setSpinUsed] = useState(() => getDaily("sapot-spin") > 0)
  const [spinning, setSpinning] = useState(false)
  const [spinResult, setSpinResult] = useState(0)
  const [spinAngle, setSpinAngle] = useState(0)
  const [memoryPoints, setMemoryPoints] = useState(() =>
    getDaily("sapot-memory"),
  )
  const [flipped, setFlipped] = useState<number[]>([])
  const [matched, setMatched] = useState<number[]>([])
  const [cards, setCards] = useState([
    "coffee",
    "heart",
    "leaf",
    "star",
    "leaf",
    "coffee",
    "star",
    "heart",
  ])
  const [flash, setFlash] = useState(false)
  const [cameraState, setCameraState] = useState<"idle" | "active" | "denied">(
    "idle",
  )
  const [scanCode, setScanCode] = useState("")
  const [scanSuccess, setScanSuccess] = useState(false)
  const [scanUsed, setScanUsed] = useState(false)
  const [vendorRedemptions, setVendorRedemptions] = useState(86)
  const [qrVersion, setQrVersion] = useState(1)
  const videoRef = useRef<HTMLVideoElement>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const cameraRequestRef = useRef(0)
  const nav = role === "customer" ? customerNav : vendorNav
  const notify = (message: string) => setToast(message)
  const go = (next: Page) => {
    setPage(next)
    setGame("overview")
    setScanSuccess(false)
    window.scrollTo({ top: 0, behavior: "smooth" })
  }
  const switchRole = (next: "customer" | "vendor") => {
    setRole(next)
    setPage("home")
    setFilter("All shops")
  }
  useEffect(() => {
    if (!toast) return
    const timer = setTimeout(() => setToast(""), 4200)
    return () => clearTimeout(timer)
  }, [toast])
  useEffect(() => {
    if (dialog === "editProfile") setDraftDocument(document)
  }, [dialog, document])
  useEffect(() => {
    if (role !== "vendor" || page !== "home") return
    const timer = setInterval(() => setQrVersion((value) => value + 1), 60000)
    return () => clearInterval(timer)
  }, [role, page])
  useEffect(() => {
    if (page !== "scan") {
      streamRef.current?.getTracks().forEach((track) => track.stop())
      streamRef.current = null
      setCameraState("idle")
    }
    return () => {
      cameraRequestRef.current += 1
      streamRef.current?.getTracks().forEach((track) => track.stop())
    }
  }, [page])
  useEffect(() => {
    if (cameraState === "active" && videoRef.current)
      videoRef.current.srcObject = streamRef.current
  }, [cameraState])
  useEffect(() => {
    if (flipped.length !== 2) return
    const timer = setTimeout(() => {
      if (cards[flipped[0]] === cards[flipped[1]]) {
        setMatched((prev) => [...prev, ...flipped])
        const reward = Math.min(15, 60 - memoryPoints)
        if (reward > 0) {
          setMemoryPoints((prev) => prev + reward)
          setDaily("sapot-memory", memoryPoints + reward)
          setPoints((prev) => prev + reward)
          notify(`A lovely match! +${reward} points`)
        } else notify("A perfect match! Daily points limit reached.")
      }
      setFlipped([])
    }, 700)
    return () => clearTimeout(timer)
  }, [flipped, cards, memoryPoints])

  const startCamera = async () => {
    const requestId = ++cameraRequestRef.current
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: "environment" },
      })
      if (requestId !== cameraRequestRef.current) {
        stream.getTracks().forEach((track) => track.stop())
        return
      }
      streamRef.current?.getTracks().forEach((track) => track.stop())
      streamRef.current = stream
      setCameraState("active")
    } catch {
      if (requestId === cameraRequestRef.current) setCameraState("denied")
    }
  }
  const spin = () => {
    if (spinUsed || spinning) return
    const prizes = [10, 25, 50, 100, 20, 40, 15, 30]
    const index = Math.floor(Math.random() * prizes.length)
    const reward = prizes[index]
    setSpinAngle(2160 - (index * 45 + 22.5))
    setSpinning(true)
    setSpinUsed(true)
    setDaily("sapot-spin", 1)
    setTimeout(() => {
      setSpinning(false)
      setSpinResult(reward)
      setPoints((prev) => prev + reward)
      notify(`A little luck, a little local love. +${reward} points!`)
    }, 3200)
  }
  const redeem = () => {
    if (
      !selectedVoucher ||
      points < selectedVoucher.points ||
      redeemed.includes(selectedVoucher.id)
    )
      return
    setPoints((prev) => prev - selectedVoucher.points)
    setRedeemed((prev) => [...prev, selectedVoucher.id])
    notify("Voucher redeemed! Show the demo code to the shop.")
  }
  const scanDemo = () => {
    if (role === "vendor" && scanSuccess) {
      notify("This demo voucher redemption has already been recorded.")
      return
    }
    if (
      scanCode.trim().toUpperCase() !==
      (role === "customer" ? "KOPIKITA" : "AINA200")
    ) {
      notify("Code not recognised. Try the demo code shown below.")
      return
    }
    if (role === "customer" && scanUsed) {
      notify("You have already checked in to Kopi Kita this session.")
      return
    }
    setScanSuccess(true)
    if (role === "customer") {
      setPoints((prev) => prev + 30)
      setScanUsed(true)
    } else setVendorRedemptions((prev) => prev + 1)
  }

  const SectionTitle = ({
    title,
    link,
    onClick,
  }: {
    title: string
    link?: string
    onClick?: () => void
  }) => (
    <div className="section-heading">
      <h2>{title}</h2>
      {link && (
        <button className="text-link" onClick={onClick}>
          {link}
          <Icon name="arrow" size={16} />
        </button>
      )}
    </div>
  )
  const PageTitle = ({
    eyebrow,
    title,
    subtitle,
    back = true,
  }: {
    eyebrow: string
    title: string
    subtitle: string
    back?: boolean
  }) => (
    <div className="page-title">
      {back && (
        <button className="back-button" onClick={() => go("home")}>
          <Icon name="back" size={16} /> Back to home
        </button>
      )}
      <div className="eyebrow">{eyebrow}</div>
      <h1>{title}</h1>
      <p>{subtitle}</p>
    </div>
  )
  const ShopCard = ({ item }: { item: typeof shops[0] }) => (
    <button
      className="shop-card"
      onClick={() => {
        setShop(item)
        go("shop")
      }}
    >
      <div className="shop-image">
        <img
          src={item.image}
          alt={
            item.name === "Kopi Kita"
              ? "Warm, welcoming local café interior"
              : `${item.name} ${item.category.toLowerCase()}`
          }
        />
        <span className="image-pill">
          <span
            className={`status-dot ${
              !(item.id === 1 ? open : item.open) ? "closed" : ""
            }`}
          />
          {(item.id === 1 ? open : item.open) ? "Open now" : "Closed"}
        </span>
        <span className="distance">{item.distance}</span>
      </div>
      <div className="shop-info">
        <div>
          <h3>{item.name}</h3>
          <span>
            {item.category} <span className="middot">·</span> {item.area}
          </span>
        </div>
        <span className="shop-arrow">
          <Icon name="arrow" size={17} />
        </span>
      </div>
      {page === "shops" && (
        <div className="shop-points">
          <Icon name="coin" size={16} /> {item.points} points earned here
        </div>
      )}
    </button>
  )
  const CustomerHome = () => (
    <>
      <div className="welcome-row">
        <div>
          <div className="eyebrow">A LITTLE LOCAL LOVE, EVERY DAY</div>
          <h1>
            Hello, {name}
            <span className="hello-dot">.</span>
          </h1>
          <p>Good things happen when you go local.</p>
        </div>
        <div className="date-display">
          <Icon name="leaf" size={17} />
          <span>Small steps. Local impact.</span>
        </div>
      </div>
      <div className="dashboard-grid">
        <div className="dashboard-main">
          <section className="points-card">
            <div className="points-content">
              <div className="label-with-icon">
                <span className="small-icon">
                  <Icon name="coin" size={17} />
                </span>{" "}
                YOUR LOCAL LOVE, ADDED UP
              </div>
              <div className="points-number">
                {points.toLocaleString()}
                <span>points</span>
              </div>
              <p>Every visit makes a difference.</p>
              <button className="points-link" onClick={() => go("profile")}>
                Local Supporter <span className="level-dot" /> Level 3
                <Icon name="chevron" size={14} />
              </button>
            </div>
            <div className="points-art" aria-hidden="true">
              <div className="orbit orbit-one" />
              <div className="orbit orbit-two" />
              <div className="coin-art coin-back">
                <Icon name="star" size={34} />
              </div>
              <div className="coin-art coin-front">
                <Icon name="star" size={49} />
              </div>
              <span className="art-spark spark-one">+</span>
              <span className="art-spark spark-two">+</span>
              <div className="art-dot" />
            </div>
            <div className="points-footer">
              <span>
                <Icon name="chart" size={15} />
                320 points earned this week
              </span>
              <button onClick={() => go("leaderboard")}>
                Keep it going
                <Icon name="arrow" size={14} />
              </button>
            </div>
          </section>
          <section className="walkin-card">
            <div className="feature-icon">
              <Icon name="scan" size={27} />
            </div>
            <div>
              <h3>Your next local stop comes with points.</h3>
              <p>Visit a local shop. Scan their QR. Feel the love.</p>
            </div>
            <Button onClick={() => go("scan")}>
              Scan now
              <Icon name="arrow" size={17} />
            </Button>
          </section>
          <section className="weekly-section">
            <SectionTitle
              title="Small challenges. Real impact."
              link="View challenges"
              onClick={() => go("challenges")}
            />
            <div className="challenge-preview">
              <div className="challenge-top">
                <div className="feature-icon subtle">
                  <Icon name="shop" size={23} />
                </div>
                <div className="challenge-preview-title">
                  <span className="tiny-label">THIS WEEK’S CHALLENGE</span>
                  <h3>The neighbourhood regular</h3>
                </div>
                <span className="reward-chip">+100 pts</span>
              </div>
              <p>Five local stops. A whole lot of good.</p>
              <div className="progress-label">
                <span>3 of 5 shops visited</span>
                <strong>60%</strong>
              </div>
              <Progress value={60} />
              <div className="challenge-bottom">
                <span>
                  <Icon name="clock" size={14} /> Resets in 3 days
                </span>
                <span>Two more. You’ve got this.</span>
              </div>
            </div>
          </section>
          <section className="explore-section">
            <SectionTitle
              title="Meet your neighbourhood."
              link="Explore more"
              onClick={() => go("shops")}
            />
            <p className="section-subtitle">
              Independent spots. Extraordinary people.
            </p>
            <div className="shop-grid home-shops">
              {shops.slice(0, 3).map((item) => (
                <ShopCard key={item.id} item={item} />
              ))}
            </div>
          </section>
          <div className="local-note">
            <Icon name="heart" size={17} />
            <p>Behind every local business is a neighbour with a dream.</p>
            <span>Let’s keep it local.</span>
          </div>
        </div>
        <aside className="dashboard-aside">
          <section className="rewards-preview">
            <SectionTitle title="A little something for you." />
            <p className="section-subtitle">
              Turn your points into happy moments.
            </p>
            <div className="voucher-preview">
              <div className="voucher-preview-top">
                <div className="coffee-illustration">
                  <Icon name="coffee" size={40} />
                </div>
                <span className="tiny-label">KOPI KITA</span>
                <h3>A coffee, on us.</h3>
                <p>Your next pick-me-up is waiting.</p>
              </div>
              <div className="ticket-divider">
                <span />
                <i />
                <span />
              </div>
              <div className="voucher-preview-bottom">
                <span>
                  <Icon name="coin" size={16} />
                  <strong>200</strong> points
                </span>
                <span className="eligible">
                  <Icon name="check" size={12} /> You’re eligible
                </span>
              </div>
            </div>
            <Button
              variant="secondary"
              className="full-width"
              onClick={() => go("vouchers")}
            >
              Explore vouchers
              <Icon name="arrow" size={16} />
            </Button>
          </section>
          <section className="games-preview">
            <SectionTitle title="A little play goes a long way." />
            <p className="section-subtitle">
              Take a break. Pick up some points.
            </p>
            <div className="game-mini-grid">
              <button
                onClick={() => {
                  go("games")
                  setGame("wheel")
                }}
              >
                <div className="mini-game-art wheel-mini">
                  <Icon name="wheel" size={39} />
                  <span className="mini-spark">+</span>
                </div>
                <h3>Spin the Wheel</h3>
                <span>
                  Try your daily luck
                  <Icon name="arrow" size={13} />
                </span>
              </button>
              <button
                onClick={() => {
                  go("games")
                  setGame("memory")
                }}
              >
                <div className="mini-game-art cards-mini">
                  <span />
                  <Icon name="grid" size={35} />
                </div>
                <h3>Memory Match</h3>
                <span>
                  Match. Play. Earn.
                  <Icon name="arrow" size={13} />
                </span>
              </button>
            </div>
          </section>
          <button className="ranking-card" onClick={() => go("leaderboard")}>
            <span className="ranking-icon">
              <Icon name="challenge" size={25} />
            </span>
            <span className="ranking-copy">
              <span className="tiny-label">YOUR LOCAL LEAGUE</span>
              <strong>Making your mark.</strong>
              <span>
                You’re <b>#12</b> in your community
              </span>
            </span>
            <Icon name="chevron" size={18} />
            <div className="ranking-footer">
              <span>{points.toLocaleString()} points of local love</span>
              <span>
                View leaderboard
                <Icon name="arrow" size={13} />
              </span>
            </div>
          </button>
          <div className="community-note">
            <div className="avatar-stack">
              <span>NH</span>
              <span>JL</span>
              <span>SK</span>
              <span>+2k</span>
            </div>
            <p>
              Little actions. A growing community.
              <br />
              <strong>2,480 neighbours supporting local.</strong>
            </p>
          </div>
        </aside>
      </div>
    </>
  )

  const ChallengesPage = () => (
    <>
      <PageTitle
        eyebrow="MAKE A LITTLE DIFFERENCE"
        title="Your weekly challenges."
        subtitle="Small steps for you. A big hello to your neighbourhood."
      />
      <div className="page-banner">
        <Icon name="challenge" size={28} />
        <div>
          <h3>A good week to go local.</h3>
          <p>Complete challenges and collect up to 480 bonus points.</p>
        </div>
        <span className="soft-pill">
          <Icon name="clock" size={14} /> 3 days left
        </span>
      </div>
      <div className="challenge-grid">
        {challenges.map((c, i) => (
          <article className="challenge-card" key={c.title}>
            <div className="row-between">
              <span className="feature-icon subtle">
                <Icon name={c.icon} />
              </span>
              <span className="reward-chip">+{c.reward} points</span>
            </div>
            <span className="tiny-label">{c.type}</span>
            <h3>{c.title}</h3>
            <p>{c.desc}</p>
            <div className="progress-label">
              <span>
                {c.value} / {c.target} {i === 5 ? "points" : "completed"}
              </span>
              <span>{c.value === c.target ? "Completed" : "In progress"}</span>
            </div>
            <Progress value={c.value} max={c.target} />
            {c.value === c.target ? (
              <Button
                variant="secondary"
                disabled={claimed}
                onClick={() => {
                  setClaimed(true)
                  setPoints((p) => p + c.reward)
                  notify(`${c.reward} challenge points collected!`)
                }}
              >
                {claimed ? (
                  <>
                    <Icon name="check" size={16} /> Reward collected
                  </>
                ) : (
                  "Collect your reward"
                )}
              </Button>
            ) : (
              <span className="challenge-status">
                <span className="status-dot" /> Keep going, you’re doing great.
              </span>
            )}
          </article>
        ))}
      </div>
    </>
  )

  const VouchersPage = () => (
    <>
      <PageTitle
        eyebrow={
          role === "customer"
            ? "YOUR POINTS, HAPPY MOMENTS"
            : "A LITTLE THANK-YOU TO YOUR CUSTOMERS"
        }
        title={
          role === "customer" ? "Available vouchers." : "Your shop vouchers."
        }
        subtitle={
          role === "customer"
            ? "A well-earned treat from the places you love."
            : "Create thoughtful rewards that keep your neighbours coming back."
        }
      />
      <div className="filter-toolbar">
        {role === "customer" ? (
          <div className="chips">
            {["All shops", ...shops.map((s) => s.name)].map((item) => (
              <button
                key={item}
                className={`chip ${filter === item ? "selected" : ""}`}
                onClick={() => setFilter(item)}
              >
                {item}
              </button>
            ))}
          </div>
        ) : (
          <span className="muted">
            {vouchers.filter((v) => v.shop === "Kopi Kita").length} active
            vouchers
          </span>
        )}
        <span className="points-balance">
          <Icon name="coin" size={17} />
          {role === "customer"
            ? `${points.toLocaleString()} points available`
            : shopName}
        </span>
      </div>
      <div className="voucher-grid">
        {vouchers
          .filter((v) =>
            role === "vendor"
              ? v.shop === "Kopi Kita"
              : filter === "All shops" || v.shop === filter,
          )
          .map((v) => {
            const eligible = points >= v.points || redeemed.includes(v.id)
            return (
              <article
                key={v.id}
                className={`voucher-card ${
                  role === "customer" && !eligible ? "voucher-disabled" : ""
                }`}
              >
                <div className="voucher-card-main">
                  <div className="row-between">
                    <span className="feature-icon subtle">
                      <Icon
                        name={v.shop === "Kopi Kita" ? "coffee" : "voucher"}
                        size={25}
                      />
                    </span>
                    <span className="soft-pill">{v.type}</span>
                  </div>
                  <span className="tiny-label">{v.shop}</span>
                  <h3>{v.name}</h3>
                  <p>
                    {role === "vendor"
                      ? v.requirements
                      : `${v.type} · A little reward for going local.`}
                  </p>
                  <div className="voucher-date">
                    <Icon name="clock" size={14} /> Valid until{" "}
                    {new Date(`${v.date}T00:00:00`).toLocaleDateString(
                      "en-GB",
                      { day: "numeric", month: "short", year: "numeric" },
                    )}
                  </div>
                </div>
                <div className="voucher-card-footer">
                  <span>
                    <strong>{v.points.toLocaleString()}</strong> points
                  </span>
                  {role === "vendor" ? (
                    <div className="inline-actions">
                      <Button
                        variant="secondary"
                        onClick={() => {
                          setSelectedVoucher(v)
                          setDialog("editVoucher")
                        }}
                      >
                        <Icon name="edit" size={15} /> Edit
                      </Button>
                      <button
                        className="icon-button"
                        aria-label={`Delete ${v.name}`}
                        onClick={() => {
                          setSelectedVoucher(v)
                          setDialog("deleteVoucher")
                        }}
                      >
                        <Icon name="trash" size={18} />
                      </button>
                    </div>
                  ) : (
                    <Button
                      disabled={!eligible}
                      variant={eligible ? "primary" : "secondary"}
                      onClick={() => {
                        setSelectedVoucher(v)
                        setDialog("redeem")
                      }}
                    >
                      {redeemed.includes(v.id)
                        ? "View voucher"
                        : eligible
                          ? "Redeem"
                          : "Not enough points"}
                      {eligible && <Icon name="arrow" size={15} />}
                    </Button>
                  )}
                </div>
                {role === "customer" && (
                  <div className="voucher-eligibility">
                    {redeemed.includes(v.id) ? (
                      "Redeemed · Ready for your next visit"
                    ) : eligible ? (
                      <>
                        <Icon name="check" size={13} /> You’re eligible for this
                        little treat
                      </>
                    ) : (
                      `${(v.points - points).toLocaleString()} more points to unlock`
                    )}
                  </div>
                )}
              </article>
            )
          })}
      </div>
      {role === "vendor" && (
        <button
          className="floating-add"
          onClick={() => {
            setSelectedVoucher(null)
            setDialog("createVoucher")
          }}
          aria-label="Create voucher"
        >
          <Icon name="plus" size={25} />
          <span>Create voucher</span>
        </button>
      )}
    </>
  )

  const ScannerPage = () => (
    <>
      <PageTitle
        eyebrow={
          role === "customer"
            ? "A VISIT WORTH SOMETHING"
            : "A REWARD WELL EARNED"
        }
        title={
          role === "customer"
            ? "Scan. Support. Earn."
            : "Scan a customer voucher."
        }
        subtitle={
          role === "customer"
            ? "Scan the shop's QR code to earn walk-in points."
            : "Scan customer's QR code to redeem their voucher."
        }
      />
      <div className="scanner-layout">
        <section className={`scanner-card ${flash ? "flash-on" : ""}`}>
          <div className="scanner-top">
            <span>
              <span className="status-dot" />
              {scanSuccess
                ? "Demo scan complete"
                : cameraState === "active"
                  ? "Camera active · use demo code below"
                  : "Camera preview"}
            </span>
            <button
              className={`icon-button ${flash ? "selected" : ""}`}
              onClick={async () => {
                const track = streamRef.current?.getVideoTracks()[0]
                if (track && "torch" in track.getCapabilities()) {
                  try {
                    await track.applyConstraints({
                      advanced: [{ torch: !flash } as MediaTrackConstraintSet],
                    })
                    setFlash(!flash)
                  } catch {
                    notify("Flashlight is not supported on this device.")
                  }
                } else notify("Flashlight requires a supported rear camera.")
              }}
              aria-label="Toggle flashlight"
            >
              <Icon name="flash" size={20} />
            </button>
          </div>
          <div className="camera-area">
            {cameraState === "active" && (
              <video ref={videoRef} autoPlay playsInline muted />
            )}
            <div className="scan-frame">
              <i />
              <i />
              <i />
              <i />
              {scanSuccess ? (
                <Icon name="check" size={65} />
              ) : cameraState !== "active" ? (
                <Icon name="scan" size={65} />
              ) : (
                <div className="scan-line" />
              )}
            </div>
            <p>
              {scanSuccess
                ? role === "customer"
                  ? "A little visit. +30 points!"
                  : "Voucher redemption recorded."
                : cameraState === "denied"
                  ? "Camera unavailable. You can use the demo code below."
                  : "Position the QR code inside the frame"}
            </p>
          </div>
          {cameraState !== "active" && !scanSuccess && (
            <Button onClick={startCamera}>
              <Icon name="camera" size={17} />
              {cameraState === "denied"
                ? "Try camera again"
                : "Allow camera access"}
            </Button>
          )}
          <p className="camera-privacy">
            <Icon name="shield" size={14} /> Your camera is only used for this
            preview.
          </p>
        </section>
        <div className="scan-help">
          <span className="feature-icon">
            <Icon name="scan" size={25} />
          </span>
          <h2>A simple way to go local.</h2>
          <p>
            {role === "customer"
              ? "Look for the SapotLokal QR at your favourite local shop. Every check-in earns 30 points."
              : "Ask your customer to open their redeemed voucher. Scan the code and confirm their reward."}
          </p>
          <div className="divider" />
          <h3>Try a demo check-in</h3>
          <p>
            This prototype uses manual demo codes. Enter{" "}
            <strong>{role === "customer" ? "KOPIKITA" : "AINA200"}</strong> to
            try the flow.
          </p>
          <label className="field-label" htmlFor="scan-code">
            Shop or voucher code
          </label>
          <input
            id="scan-code"
            className="input"
            value={scanCode}
            onChange={(e) => setScanCode(e.target.value)}
            placeholder={role === "customer" ? "e.g. KOPIKITA" : "e.g. AINA200"}
          />
          <Button className="full-width" onClick={scanDemo}>
            Confirm demo code
            <Icon name="arrow" size={16} />
          </Button>
          <span className="muted small">
            Demo only · No real QR recognition or payment.
          </span>
        </div>
      </div>
    </>
  )

  const ProfilePage = () => (
    <>
      <PageTitle
        eyebrow={
          role === "customer"
            ? "YOUR LITTLE CORNER"
            : "THE PEOPLE BEHIND THE PLACE"
        }
        title={
          role === "customer" ? "Made of local love." : "Your shop profile."
        }
        subtitle={
          role === "customer"
            ? "Your journey, your milestones, your neighbourhood."
            : "Keep your shop details up to date so your neighbours can find you."
        }
      />
      <div className="profile-layout">
        <section className="profile-card">
          <div className="large-avatar">
            {role === "customer" ? (
              name.slice(0, 1)
            ) : (
              <Icon name="coffee" size={40} />
            )}
          </div>
          <h2>{role === "customer" ? fullName : shopName}</h2>
          <p>{role === "customer" ? email : shopCategory}</p>
          <span className="soft-pill">
            <Icon name={role === "customer" ? "leaf" : "shop"} size={14} />
            {role === "customer"
              ? "Local Supporter · Level 3"
              : "Your independent neighbourhood café"}
          </span>
          {role === "customer" && (
            <>
              <div className="profile-points">
                {points.toLocaleString()}
                <span>points of local love</span>
              </div>
              <div className="progress-label">
                <span>Next up: Local Champion</span>
                <strong>{Math.min(points, 2000)} / 2,000</strong>
              </div>
              <Progress value={points} max={2000} />
              <p className="small">
                {Math.max(0, 2000 - points)} points to your next chapter.
              </p>
            </>
          )}
          <Button
            className="full-width"
            onClick={() => setDialog("editProfile")}
          >
            <Icon name="edit" size={16} />
            {role === "customer" ? "Edit profile" : "Update profile"}
          </Button>
          <div className="profile-account-actions">
            <button onClick={() => setDialog("logout")}>
              <Icon name="logout" size={17} />
              Log out
            </button>
            <button onClick={() => setDialog("deleteAccount")}>
              <Icon name="trash" size={16} />
              Delete account
            </button>
          </div>
        </section>
        <section className="profile-detail">
          {role === "customer" ? (
            <>
              <SectionTitle title="Little badges. Big heart." />
              <p className="section-subtitle">
                A collection of the good you’ve put into your community.
              </p>
              <div className="badge-grid">
                {[
                  {
                    icon: "leaf",
                    name: "First steps",
                    desc: "Your first local visit",
                  },
                  {
                    icon: "coffee",
                    name: "Coffee regular",
                    desc: "5 café check-ins",
                  },
                  {
                    icon: "heart",
                    name: "Local lover",
                    desc: "10 shops supported",
                  },
                  {
                    icon: "challenge",
                    name: "Challenge accepted",
                    desc: "3 challenges completed",
                  },
                  {
                    icon: "voucher",
                    name: "Treat yourself",
                    desc: "First voucher redeemed",
                  },
                  {
                    icon: "star",
                    name: "Local champion",
                    desc: "Reach level 4",
                    locked: true,
                  },
                ].map((b) => (
                  <div
                    className={`badge-card ${b.locked ? "locked" : ""}`}
                    key={b.name}
                  >
                    <span>
                      <Icon name={b.icon} size={30} />
                    </span>
                    <h3>{b.name}</h3>
                    <p>{b.desc}</p>
                    {b.locked && <small>Still growing</small>}
                  </div>
                ))}
              </div>
              <div className="profile-membership">
                <Icon name="heart" size={20} />
                <div>
                  <h3>A neighbour since January 2026.</h3>
                  <p>Thank you for making your community a little brighter.</p>
                </div>
              </div>
            </>
          ) : (
            <>
              <SectionTitle title="The details that matter." />
              <div className="detail-rows">
                {[
                  { icon: "shop", label: "Shop category", value: shopCategory },
                  { icon: "pin", label: "Location", value: shopAddress },
                  { icon: "clock", label: "Operating hours", value: shopHours },
                  {
                    icon: "shield",
                    label: "SSM registration number",
                    value: ssmNumber,
                  },
                ].map((d) => (
                  <div className="detail-row" key={d.label}>
                    <Icon name={d.icon} />
                    <div>
                      <span>{d.label}</span>
                      <strong>{d.value}</strong>
                    </div>
                  </div>
                ))}
              </div>
              <h3 className="document-heading">
                Business registration document
              </h3>
              <div className="document-card">
                <Icon name="file" size={27} />
                <div>
                  <strong>{document || "No document attached"}</strong>
                  <span>
                    {document
                      ? "PDF document · Registration attachment"
                      : "Add a PDF in Update Profile"}
                  </span>
                </div>
                <button
                  className="icon-button"
                  onClick={() => setDialog("editProfile")}
                  aria-label="Manage registration document"
                >
                  <Icon name="edit" size={18} />
                </button>
              </div>
              <p className="small muted">
                Your registration details are private and never shown to
                customers.
              </p>
            </>
          )}
        </section>
      </div>
    </>
  )

  const GamesPage = () => (
    <>
      <PageTitle
        eyebrow="A LITTLE PLAY, A LITTLE REWARD"
        title={
          game === "wheel"
            ? "Give good luck a spin."
            : game === "memory"
              ? "Make a little match."
              : "Play & earn more points."
        }
        subtitle={
          game === "memory"
            ? "A moment to reset. Match the pairs and collect a little local love."
            : "A small break in your day, with something good to take away."
        }
      />
      <div className="chips game-tabs">
        {[
          { id: "overview", label: "All games" },
          { id: "wheel", label: "Spin the Wheel" },
          { id: "memory", label: "Memory Match" },
        ].map((g) => (
          <button
            key={g.id}
            className={`chip ${game === g.id ? "selected" : ""}`}
            onClick={() => setGame(g.id as typeof game)}
          >
            {g.label}
          </button>
        ))}
      </div>
      {game === "overview" ? (
        <div className="game-overview-grid">
          <article className="game-overview-card">
            <div className="overview-art">
              <Icon name="wheel" size={110} />
            </div>
            <span className="soft-pill">Once a day, a little luck</span>
            <h2>Spin the Wheel</h2>
            <p>
              Your daily chance to win 10–100 points. One spin. A little
              surprise.
            </p>
            <Button onClick={() => setGame("wheel")}>
              Let’s give it a spin
              <Icon name="arrow" size={17} />
            </Button>
          </article>
          <article className="game-overview-card">
            <div className="overview-art">
              <Icon name="grid" size={95} />
            </div>
            <span className="soft-pill">Up to 60 points a day</span>
            <h2>Memory Match</h2>
            <p>
              Find matching pairs and earn 15 points a match. Keep playing just
              for fun.
            </p>
            <Button onClick={() => setGame("memory")}>
              Find your match
              <Icon name="arrow" size={17} />
            </Button>
          </article>
        </div>
      ) : game === "wheel" ? (
        <section className="game-play-card">
          <span className="soft-pill">
            {spinUsed ? "0 daily spins remaining" : "1 daily spin remaining"}
          </span>
          <div className="wheel-container">
            <span className="wheel-pointer" />
            <div
              className={`prize-wheel ${
                spinning || spinResult ? "is-spinning" : ""
              }`}
              style={{ transform: `rotate(${spinAngle}deg)` }}
            >
              {[10, 25, 50, 100, 20, 40, 15, 30].map((n, i) => (
                <span
                  key={n}
                  style={{ transform: `rotate(${i * 45 + 22.5}deg)` }}
                >
                  <b>{n}</b>
                </span>
              ))}
              <div className="wheel-center">
                <Icon name="star" size={31} />
              </div>
            </div>
          </div>
          {spinResult > 0 ? (
            <div className="spin-result">
              <h2>A lovely little win. +{spinResult} points!</h2>
              <p>They’ve been added to your balance. Come back tomorrow.</p>
            </div>
          ) : (
            <p>
              {spinUsed && !spinning
                ? "Your next little moment of luck is waiting tomorrow."
                : "One spin. All sorts of possibility."}
            </p>
          )}
          <Button onClick={spin} disabled={spinUsed || spinning}>
            {spinning
              ? "A little luck in motion…"
              : spinUsed
                ? "See you tomorrow"
                : "Spin the wheel"}
            {!spinUsed && <Icon name="wheel" size={18} />}
          </Button>
          <span className="small muted">
            One spin per device per day · 10–100 points
          </span>
        </section>
      ) : (
        <section className="game-play-card">
          <div className="memory-score">
            <div>
              <span className="tiny-label">COLLECTED TODAY</span>
              <h2>
                {memoryPoints}
                <span> / 60 points</span>
              </h2>
            </div>
            <div>
              <span className="tiny-label">PAIRS FOUND</span>
              <h2>
                {matched.length / 2}
                <span> / 4</span>
              </h2>
            </div>
          </div>
          <Progress value={memoryPoints} max={60} />
          <div className="memory-grid">
            {cards.map((c, i) => (
              <button
                key={i}
                aria-label={
                  matched.includes(i) || flipped.includes(i)
                    ? c
                    : `Reveal card ${i + 1}`
                }
                className={`memory-card ${
                  flipped.includes(i) || matched.includes(i) ? "revealed" : ""
                } ${matched.includes(i) ? "matched" : ""}`}
                disabled={
                  matched.includes(i) ||
                  flipped.includes(i) ||
                  flipped.length === 2
                }
                onClick={() => setFlipped((p) => [...p, i])}
              >
                <Icon
                  name={flipped.includes(i) || matched.includes(i) ? c : "leaf"}
                  size={34}
                />
                {matched.includes(i) && (
                  <span>
                    <Icon name="check" size={12} />
                  </span>
                )}
              </button>
            ))}
          </div>
          {matched.length === 8 && (
            <>
              <h2>All matched. Nicely done!</h2>
              <Button
                onClick={() => {
                  setCards((prev) => [...prev].sort(() => Math.random() - 0.5))
                  setMatched([])
                  setFlipped([])
                }}
              >
                Play another round
                <Icon name="arrow" size={16} />
              </Button>
            </>
          )}
          <p>
            {memoryPoints >= 60
              ? "Daily limit reached. Keep matching for the joy of it — no more points today."
              : "Find a pair. Earn 15 points. A little focus goes a long way."}
          </p>
          <span className="small muted">
            Maximum 60 points per device per day
          </span>
        </section>
      )}
    </>
  )

  const ShopsPage = () => (
    <>
      <PageTitle
        eyebrow="GOOD THINGS ARE JUST AROUND THE CORNER"
        title="Meet your neighbourhood."
        subtitle="Find your new favourite. Support someone’s dream."
      />
      <div className="shop-filter-bar">
        <div className="search-field">
          <Icon name="search" size={19} />
          <input
            aria-label="Search local shops"
            placeholder="Search shops, places, little favourites…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <span className="location-small">
          <Icon name="pin" size={16} />
          Johor Bahru, Malaysia
        </span>
      </div>
      <div className="chips shop-filter-chips">
        {["All shops", "Café", "Bakery", "Lifestyle", "Retail"].map((c) => (
          <button
            key={c}
            className={`chip ${category === c ? "selected" : ""}`}
            onClick={() => setCategory(c)}
          >
            {c}
          </button>
        ))}
      </div>
      <div className="shops-list-grid">
        {shops
          .filter(
            (s) =>
              (category === "All shops" || s.category === category) &&
              `${s.name} ${s.area} ${s.category}`
                .toLowerCase()
                .includes(search.toLowerCase()),
          )
          .map((s) => (
            <ShopCard key={s.id} item={s} />
          ))}
      </div>
      {!shops.some(
        (s) =>
          (category === "All shops" || s.category === category) &&
          `${s.name} ${s.area} ${s.category}`
            .toLowerCase()
            .includes(search.toLowerCase()),
      ) && (
        <div className="empty-state">
          <Icon name="search" size={35} />
          <h2>No shops just yet.</h2>
          <p>Try a different name or explore another category.</p>
          <Button
            variant="secondary"
            onClick={() => {
              setSearch("")
              setCategory("All shops")
            }}
          >
            Show all shops
          </Button>
        </div>
      )}
    </>
  )
  const ShopPage = () => (
    <>
      <button className="back-button" onClick={() => go("shops")}>
        <Icon name="back" size={16} />
        Back to local shops
      </button>
      <div className="shop-detail-hero">
        <img src={shop.image} alt={`${shop.name} interior`} />
        <span className="image-pill">
          <span
            className={`status-dot ${
              !(shop.id === 1 ? open : shop.open) ? "closed" : ""
            }`}
          />
          {(shop.id === 1 ? open : shop.open) ? "Open now" : "Currently closed"}
        </span>
      </div>
      <div className="shop-detail-grid">
        <section>
          <span className="eyebrow">
            {shop.category.toUpperCase()} · INDEPENDENT & LOCAL
          </span>
          <h1>{shop.name}</h1>
          <p className="shop-description">{shop.desc}</p>
          <div className="detail-rows">
            <div className="detail-row">
              <Icon name="clock" />
              <div>
                <span>Opening hours · Monday to Saturday</span>
                <strong>{shop.hours}</strong>
              </div>
            </div>
            <div className="detail-row">
              <Icon name="pin" />
              <div>
                <span>Find us here</span>
                <strong>{shop.address}</strong>
              </div>
            </div>
            <div className="detail-row">
              <Icon name="phone" />
              <div>
                <span>Say hello</span>
                <a href="tel:+6072234820">+60 7-223 4820</a>
              </div>
            </div>
          </div>
        </section>
        <aside>
          <div className="shop-earned-card">
            <Icon name="coin" size={28} />
            <span className="tiny-label">YOUR LOCAL LOVE HERE</span>
            <h2>
              {shop.points}
              <span>points</span>
            </h2>
            <p>Every visit has made a little difference.</p>
            <Button onClick={() => go("scan")} className="full-width">
              Visit & scan
              <Icon name="scan" size={17} />
            </Button>
            <Button
              variant="text"
              onClick={() => {
                setFilter(shop.name)
                go("vouchers")
              }}
            >
              View shop vouchers
              <Icon name="arrow" size={15} />
            </Button>
          </div>
        </aside>
      </div>
    </>
  )
  const LeaderboardPage = () => {
    const leaders = [
      { name: "Mei Ling", points: 3240, initials: "ML" },
      { name: "Daniel Tan", points: 2980, initials: "DT" },
      { name: "Nur Hani", points: 2760, initials: "NH" },
    ]
    return (
      <>
        <PageTitle
          eyebrow="A COMMUNITY THAT CARES"
          title="Your local league."
          subtitle="Not just points. A whole neighbourhood of good."
        />
        <div className="leaderboard-period">
          <span className="soft-pill">
            <Icon name="pin" size={14} />
            Johor Bahru
          </span>
          <span className="muted">This month · 2,480 local supporters</span>
        </div>
        <div className="podium">
          {[1, 0, 2].map((index) => (
            <div className={`podium-person place-${index + 1}`} key={index}>
              <span className="podium-avatar">
                {leaders[index].initials}
                {index === 0 && (
                  <span className="winner-crown">
                    <Icon name="challenge" size={23} />
                  </span>
                )}
              </span>
              <h3>{leaders[index].name}</h3>
              <p>{leaders[index].points.toLocaleString()} points</p>
              <div className="podium-block">
                <span>{index + 1}</span>
                <Icon name="leaf" size={22} />
              </div>
            </div>
          ))}
        </div>
        <div className="leaderboard-list">
          {[
            "Sarah Lim",
            "Amir Hakim",
            "Jia Wei",
            "Siti Nur",
            "Lucas Lee",
            "Farah Aziz",
            "Hafiz Rahman",
            "Chloe Wong",
            fullName,
            "Adam Iskandar",
          ].map((n, i) => (
            <div
              className={`leaderboard-row ${n === fullName ? "is-you" : ""}`}
              key={n}
            >
              <span className="rank-number">{i + 4}</span>
              <span className="list-avatar">
                {n
                  .split(" ")
                  .map((s) => s[0])
                  .slice(0, 2)
                  .join("")}
              </span>
              <strong>
                {n}
                {n === fullName && <span className="soft-pill">You</span>}
              </strong>
              <span className="rank-points">
                {(n === fullName ? points : 2600 - i * 143).toLocaleString()}
                <small>points</small>
              </span>
            </div>
          ))}
        </div>
        <p className="leaderboard-footnote">
          Every little act of support counts. Keep showing up for local.
        </p>
      </>
    )
  }

  const VendorHome = () => (
    <>
      <div className="welcome-row">
        <div>
          <div className="eyebrow">YOUR NEIGHBOURHOOD, IN NUMBERS</div>
          <h1>Welcome back, {shopName}.</h1>
          <p>Here’s the good your shop is doing today.</p>
        </div>
        <span className="soft-pill">
          <Icon name="clock" size={15} /> Today’s overview
        </span>
      </div>
      <div className="vendor-status">
        <div className="feature-icon">
          <Icon name="shop" size={27} />
        </div>
        <div>
          <h3>Your doors are {open ? "open" : "closed"}.</h3>
          <p>
            {open
              ? "Your neighbours can see you’re ready to welcome them."
              : "Take a breather. Customers will see you’re currently closed."}
          </p>
        </div>
        <button
          className={`toggle ${open ? "on" : ""}`}
          onClick={() => {
            setOpen(!open)
            notify(`Shop is now ${!open ? "open" : "closed"}.`)
          }}
          role="switch"
          aria-checked={open}
          aria-label="Shop operating status"
        >
          <span />
        </button>
        <span className="toggle-label">{open ? "Open" : "Closed"}</span>
      </div>
      <div className="vendor-stats">
        <div className="stat-card">
          <span>
            <Icon name="user" size={18} />
            TODAY’S WALK-INS
          </span>
          <h2>
            24<small>customers</small>
          </h2>
          <p>
            <Icon name="chart" size={14} /> 6 more than yesterday
          </p>
        </div>
        <div className="stat-card">
          <span>
            <Icon name="shop" size={18} />
            THIS MONTH’S VISITS
          </span>
          <h2>
            486<small>visits</small>
          </h2>
          <p>
            <Icon name="chart" size={14} /> 12% more local love this month
          </p>
        </div>
        <div className="stat-card">
          <span>
            <Icon name="coin" size={18} />
            TOTAL POINTS ISSUED
          </span>
          <h2>14,580</h2>
          <p>Little rewards. Lasting connections.</p>
        </div>
        <div className="stat-card">
          <span>
            <Icon name="voucher" size={18} />
            VOUCHERS REDEEMED
          </span>
          <h2>{vendorRedemptions}</h2>
          <p>Happy moments, made here.</p>
        </div>
      </div>
      <div className="vendor-chart-grid">
        <section className="chart-card">
          <SectionTitle title="A day in your neighbourhood." />
          <div className="row-between">
            <p className="section-subtitle">Today’s walk-in activity</p>
            <span className="soft-pill">24 customers</span>
          </div>
          <div className="bar-chart">
            {[25, 45, 85, 62, 100, 75, 43, 30].map((height, i) => (
              <div key={i}>
                <div className="bar-track">
                  <span style={{ height: `${height}%` }} />
                </div>
                <span>
                  {
                    ["8am", "9am", "10am", "11am", "12pm", "1pm", "2pm", "3pm"][
                      i
                    ]
                  }
                </span>
              </div>
            ))}
          </div>
          <div className="chart-caption">
            <span>
              <span className="legend-dot" />
              Customer walk-ins
            </span>
            <span>Busiest around 12 PM</span>
          </div>
        </section>
        <section className="qr-card">
          <span className="tiny-label">A LITTLE SCAN. A WARM WELCOME.</span>
          <h2>Your shop’s QR code.</h2>
          <QR seed={qrVersion} label="Rotating prototype shop QR code" />
          <p>Customers can scan to collect their walk-in points.</p>
          <span className="soft-pill">
            <Icon name="clock" size={13} /> Refreshes every 60s · Demo QR
          </span>
          <Button
            variant="secondary"
            onClick={() => {
              notify(
                "Your demo shop code is KOPIKITA. Try it in the customer scanner.",
              )
              setQrVersion((value) => value + 1)
            }}
          >
            Refresh demo QR
            <Icon name="scan" size={16} />
          </Button>
        </section>
      </div>
      <section className="chart-card monthly-chart">
        <div className="row-between">
          <div>
            <SectionTitle title="A month of showing up." />
            <p className="section-subtitle">
              Your community, one visit at a time.
            </p>
          </div>
          <span className="soft-pill">This month · 486 visits</span>
        </div>
        <div className="line-chart">
          <div className="y-labels">
            <span>150</span>
            <span>100</span>
            <span>50</span>
            <span>0</span>
          </div>
          <div className="line-chart-inner">
            <div className="chart-grid-lines">
              <i />
              <i />
              <i />
              <i />
            </div>
            <svg
              viewBox="0 0 800 140"
              preserveAspectRatio="none"
              aria-label="Monthly visits: week 1 86, week 2 118, week 3 134, week 4 148"
              role="img"
            >
              <path
                d="M0 120C70 120 75 65 140 72S230 100 280 65 360 88 420 45 490 68 560 35 680 60 800 9L800 140H0Z"
                fill="var(--blue)"
                fillOpacity=".5"
              />
              <path
                d="M0 120C70 120 75 65 140 72S230 100 280 65 360 88 420 45 490 68 560 35 680 60 800 9"
                stroke="var(--brown)"
                strokeWidth="2"
                fill="none"
              />
            </svg>
            <div className="x-labels">
              <span>Week 1</span>
              <span>Week 2</span>
              <span>Week 3</span>
              <span>Week 4</span>
            </div>
          </div>
        </div>
      </section>
    </>
  )

  if (loggedOut || deleted)
    return (
      <div className="auth-demo">
        <div className="brand">
          <span className="brand-icon">
            <Icon name="shop" size={24} />
          </span>
          Sapot<span>Lokal</span>
          <i />
        </div>
        <div className="auth-card">
          <Icon name={deleted ? "heart" : "leaf"} size={45} />
          <h1>
            {deleted
              ? "Your demo account is deleted."
              : "A little local love awaits."}
          </h1>
          <p>
            {deleted
              ? "Your in-memory session has been cleared. This prototype has no real account or server data."
              : "You’re logged out of the demo. Come back whenever you’re ready to go local."}
          </p>
          <Button
            onClick={() => {
              if (deleted) window.location.reload()
              else {
                setLoggedOut(false)
                setPage("home")
              }
            }}
          >
            {deleted ? "Start a fresh demo" : "Continue demo as Aina"}
            <Icon name="arrow" size={17} />
          </Button>
        </div>
      </div>
    )

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <button
          className="brand"
          onClick={() => go("home")}
          aria-label="SapotLokal home"
        >
          <span className="brand-icon">
            <Icon name="shop" size={24} />
          </span>
          Sapot<span>Lokal</span>
          <i />
        </button>
        <div className="sidebar-tagline">Little actions. Local impact.</div>
        <div className="role-switch" aria-label="Switch experience">
          <button
            className={role === "customer" ? "active" : ""}
            onClick={() => switchRole("customer")}
          >
            Customer
          </button>
          <button
            className={role === "vendor" ? "active" : ""}
            onClick={() => switchRole("vendor")}
          >
            Vendor
          </button>
        </div>
        <span className="nav-label">
          YOUR {role === "customer" ? "NEIGHBOURHOOD" : "BUSINESS"}
        </span>
        <nav className="side-nav">
          {nav.map((n) => (
            <button
              key={n.page}
              className={page === n.page ? "active" : ""}
              onClick={() => go(n.page as Page)}
            >
              <Icon name={n.icon} size={21} />
              <span>{n.label}</span>
              {page === n.page && <span className="nav-active-dot" />}
            </button>
          ))}
        </nav>
        {role === "customer" && (
          <>
            <div className="nav-divider" />
            <span className="nav-label">MORE TO LOVE</span>
            <nav className="side-nav secondary-nav">
              {[
                { page: "shops", label: "Local shops", icon: "shop" },
                { page: "games", label: "Play & earn", icon: "grid" },
                {
                  page: "leaderboard",
                  label: "Leaderboard",
                  icon: "challenge",
                },
              ].map((n) => (
                <button
                  key={n.page}
                  className={
                    page === n.page || (page === "shop" && n.page === "shops")
                      ? "active"
                      : ""
                  }
                  onClick={() => go(n.page as Page)}
                >
                  <Icon name={n.icon} size={21} />
                  <span>{n.label}</span>
                </button>
              ))}
            </nav>
          </>
        )}
        <div className="sidebar-bottom">
          <div className="local-promise">
            <Icon name="leaf" size={23} />
            <h3>
              Good for you.
              <br />
              Great for local.
            </h3>
            <p>Every little visit helps your neighbourhood thrive.</p>
            <button
              onClick={() => go(role === "customer" ? "shops" : "profile")}
            >
              Keep it local
              <Icon name="arrow" size={15} />
            </button>
          </div>
          <button className="help-link" onClick={() => setDialog("help")}>
            <Icon name="help" size={18} />A little help?
          </button>
          <span className="sidebar-copyright">
            Made for our neighbourhoods.
          </span>
        </div>
      </aside>
      <div className="main-shell">
        <header className="topbar">
          <div className="breadcrumb">
            <span>SapotLokal</span>
            <span>/</span>
            <strong>
              {role === "vendor" && page === "home"
                ? "Overview"
                : {
                    home: "Your neighbourhood",
                    challenges: "Weekly challenges",
                    scan: "QR scanner",
                    vouchers: "Vouchers",
                    profile: "Your profile",
                    games: "Play & earn",
                    shops: "Local shops",
                    shop: shop.name,
                    leaderboard: "Leaderboard",
                  }[page]}
            </strong>
          </div>
          <button className="mobile-brand" onClick={() => go("home")}>
            <Icon name="shop" size={23} />
            SapotLokal
            <span />
          </button>
          <div className="topbar-actions">
            <span className="top-location">
              <Icon name="pin" size={16} />
              Johor Bahru, MY
            </span>
            <button
              className="notification-button icon-button"
              onClick={() => setDialog("notifications")}
              aria-label="View notifications"
            >
              <Icon name="bell" size={21} />
              <span />
            </button>
            <div className="topbar-divider" />
            <button className="top-profile" onClick={() => go("profile")}>
              <span className="avatar">
                {role === "customer" ? (
                  name.slice(0, 1)
                ) : (
                  <Icon name="coffee" size={19} />
                )}
              </span>
              <span className="profile-text">
                <strong>{role === "customer" ? fullName : shopName}</strong>
                <small>
                  {role === "customer"
                    ? "Local Supporter"
                    : "Your shop dashboard"}
                </small>
              </span>
              <Icon name="chevron" size={14} />
            </button>
          </div>
        </header>
        <div className="mobile-role">
          <div className="role-switch">
            <button
              className={role === "customer" ? "active" : ""}
              onClick={() => switchRole("customer")}
            >
              Customer
            </button>
            <button
              className={role === "vendor" ? "active" : ""}
              onClick={() => switchRole("vendor")}
            >
              Vendor
            </button>
          </div>
          <span>
            <Icon name="pin" size={13} />
            Johor Bahru
          </span>
        </div>
        <main className={`main-content page-${page}`}>
          {page === "home"
            ? role === "customer"
              ? CustomerHome()
              : VendorHome()
            : page === "challenges"
              ? ChallengesPage()
              : page === "vouchers"
                ? VouchersPage()
                : page === "scan"
                  ? ScannerPage()
                  : page === "profile"
                    ? ProfilePage()
                    : page === "games"
                      ? GamesPage()
                      : page === "shops"
                        ? ShopsPage()
                        : page === "shop"
                          ? ShopPage()
                          : LeaderboardPage()}
          <footer className="page-footer">
            <span>SapotLokal</span>
            <span>A little support goes a long way.</span>
            <span>Made for local. Made for you.</span>
          </footer>
        </main>
      </div>
      <nav className="bottom-nav">
        {nav.map((n) => (
          <button
            key={n.page}
            onClick={() => go(n.page as Page)}
            className={page === n.page ? "active" : ""}
          >
            <span>
              <Icon name={n.icon} size={22} />
            </span>
            <small>
              {n.page === "scan"
                ? role === "customer"
                  ? "Scan"
                  : "Scanner"
                : n.page === "profile"
                  ? "Profile"
                  : n.page === "home"
                    ? "Home"
                    : n.label}
            </small>
          </button>
        ))}
      </nav>
      {toast && (
        <div className="toast" role="status">
          <span>
            <Icon name="check" size={17} />
          </span>
          {toast}
          <button
            onClick={() => setToast("")}
            aria-label="Dismiss notification"
          >
            <Icon name="close" size={16} />
          </button>
        </div>
      )}
      {dialog === "notifications" && (
        <Modal title="A few good things." onClose={() => setDialog(null)}>
          <p className="modal-intro">Little updates from your neighbourhood.</p>
          <div className="notification-item">
            <span className="feature-icon">
              <Icon name="voucher" />
            </span>
            <div>
              <h3>Your next coffee is waiting.</h3>
              <p>
                Kopi Kita has a new free coffee voucher. A little treat for 200
                points.
              </p>
              <button
                className="text-link"
                onClick={() => {
                  setDialog(null)
                  go("vouchers")
                }}
              >
                Take a look
                <Icon name="arrow" size={14} />
              </button>
            </div>
          </div>
          <div className="notification-item">
            <span className="feature-icon subtle">
              <Icon name="challenge" />
            </span>
            <div>
              <h3>Two more stops to go.</h3>
              <p>
                You’re close to completing this week’s neighbourhood challenge.
              </p>
              <button
                className="text-link"
                onClick={() => {
                  setDialog(null)
                  go("challenges")
                }}
              >
                View challenges
                <Icon name="arrow" size={14} />
              </button>
            </div>
          </div>
        </Modal>
      )}
      {dialog === "redeem" && selectedVoucher && (
        <Modal
          title={
            redeemed.includes(selectedVoucher.id)
              ? "Your little reward is ready."
              : "A well-earned little treat."
          }
          onClose={() => setDialog(null)}
        >
          <span className="soft-pill">{selectedVoucher.shop}</span>
          <h3 className="modal-voucher-title">{selectedVoucher.name}</h3>
          {redeemed.includes(selectedVoucher.id) ? (
            <div className="redeemed-qr">
              <QR label="Prototype voucher QR, use demo code AINA200" />
              <strong>AINA200</strong>
              <p>Show this demo code at {selectedVoucher.shop}.</p>
              <span className="small muted">
                Prototype only · No real-world monetary value
              </span>
            </div>
          ) : (
            <>
              <div className="redemption-cost">
                <span>Points to redeem</span>
                <strong>{selectedVoucher.points} points</strong>
              </div>
              <p className="modal-intro">{selectedVoucher.requirements}</p>
              <p className="small muted">
                Valid until{" "}
                {new Date(
                  `${selectedVoucher.date}T00:00:00`,
                ).toLocaleDateString("en-GB")}
                . Your points balance after redemption:{" "}
                {points - selectedVoucher.points}.
              </p>
              <Button className="full-width" onClick={redeem}>
                Redeem for {selectedVoucher.points} points
                <Icon name="arrow" size={16} />
              </Button>
              <Button
                className="full-width"
                variant="text"
                onClick={() => setDialog(null)}
              >
                Maybe another time
              </Button>
            </>
          )}
        </Modal>
      )}
      {dialog === "editProfile" && (
        <Modal
          title={
            role === "customer"
              ? "Make yourself at home."
              : "Keep your shop looking its best."
          }
          onClose={() => setDialog(null)}
        >
          <form
            className="form"
            onSubmit={(e) => {
              e.preventDefault()
              const data = new FormData(e.currentTarget)
              if (role === "customer") {
                const full = String(data.get("name"))
                setFullName(full)
                setName(full.split(" ")[0])
                setEmail(String(data.get("email")))
                setPhone(String(data.get("phone")))
              } else {
                setShopName(String(data.get("name")))
                setShopCategory(String(data.get("category")))
                setShopAddress(String(data.get("address")))
                setShopHours(String(data.get("hours")))
                setSsmNumber(String(data.get("ssm")))
                setDocument(draftDocument)
              }
              setDialog(null)
              notify("Your profile is all up to date.")
            }}
          >
            <label>
              {role === "customer" ? "Full name" : "Shop name"}
              <input
                name="name"
                className="input"
                defaultValue={role === "customer" ? fullName : shopName}
                required
              />
            </label>
            {role === "customer" ? (
              <>
                <label>
                  Email address
                  <input
                    className="input"
                    name="email"
                    type="email"
                    defaultValue={email}
                    required
                  />
                </label>
                <label>
                  Phone number
                  <input
                    className="input"
                    name="phone"
                    type="tel"
                    defaultValue={phone}
                  />
                </label>
                <button
                  type="button"
                  className="password-link"
                  onClick={() => setDialog("password")}
                >
                  <Icon name="shield" size={17} />
                  Change password
                  <Icon name="chevron" size={15} />
                </button>
              </>
            ) : (
              <>
                <label>
                  Shop category
                  <select
                    className="input"
                    name="category"
                    defaultValue={shopCategory}
                  >
                    <option>Café / F&B</option>
                    <option>Bakery</option>
                    <option>Retail</option>
                    <option>Lifestyle</option>
                  </select>
                </label>
                <label>
                  Location
                  <input
                    className="input"
                    name="address"
                    defaultValue={shopAddress}
                    required
                  />
                </label>
                <label>
                  Operating hours
                  <input
                    className="input"
                    name="hours"
                    defaultValue={shopHours}
                    required
                  />
                </label>
                <label>
                  SSM registration number
                  <input
                    className="input"
                    name="ssm"
                    defaultValue={ssmNumber}
                    required
                  />
                </label>
                <label>
                  SSM registration document · PDF
                  <input
                    className="input file-input"
                    type="file"
                    accept=".pdf,application/pdf"
                    onChange={(e) => {
                      const file = e.target.files?.[0]
                      if (
                        file &&
                        (file.type === "application/pdf" ||
                          file.name.toLowerCase().endsWith(".pdf"))
                      )
                        setDraftDocument(file.name)
                      else if (file) notify("Please choose a PDF document.")
                    }}
                  />
                </label>
                {draftDocument && (
                  <div className="document-card compact">
                    <Icon name="file" size={20} />
                    <span>{draftDocument}</span>
                    <button
                      className="icon-button"
                      type="button"
                      aria-label="Remove document"
                      onClick={() => setDraftDocument("")}
                    >
                      <Icon name="trash" size={16} />
                    </button>
                  </div>
                )}
                <p className="small muted">
                  Demo attachment: the filename is stored for this session.
                  Files are not uploaded.
                </p>
              </>
            )}
            <div className="form-actions">
              <Button variant="secondary" onClick={() => setDialog(null)}>
                Cancel
              </Button>
              <Button type="submit">
                Save changes
                <Icon name="check" size={16} />
              </Button>
            </div>
          </form>
        </Modal>
      )}
      {dialog === "password" && (
        <Modal
          title="A little extra peace of mind."
          onClose={() => setDialog("editProfile")}
        >
          <form
            className="form"
            onSubmit={(e) => {
              e.preventDefault()
              const data = new FormData(e.currentTarget)
              if (data.get("new") !== data.get("confirm")) {
                notify("Your new passwords don’t match.")
                return
              }
              notify(
                "Demo password change complete. No real account password is stored.",
              )
              setDialog("editProfile")
            }}
          >
            <p className="small muted">
              Prototype form only. Do not enter a real password.
            </p>
            <label>
              Current demo password
              <input className="input" type="password" required minLength={8} />
            </label>
            <label>
              New demo password
              <input
                className="input"
                name="new"
                type="password"
                required
                minLength={8}
                placeholder="At least 8 characters"
              />
            </label>
            <label>
              Confirm new password
              <input
                className="input"
                name="confirm"
                type="password"
                required
                minLength={8}
              />
            </label>
            <Button type="submit" className="full-width">
              Update demo password
            </Button>
          </form>
        </Modal>
      )}
      {(dialog === "createVoucher" || dialog === "editVoucher") && (
        <Modal
          title={
            dialog === "createVoucher"
              ? "Create a little reason to return."
              : "Make a good reward even better."
          }
          onClose={() => setDialog(null)}
        >
          <form
            className="form"
            onSubmit={(e) => {
              e.preventDefault()
              const data = new FormData(e.currentTarget)
              const item: Voucher = {
                id:
                  dialog === "editVoucher" && selectedVoucher
                    ? selectedVoucher.id
                    : Date.now(),
                name: String(data.get("name")),
                type: String(data.get("type")),
                shop: "Kopi Kita",
                date: String(data.get("date")),
                points: Number(data.get("points")),
                requirements: String(data.get("requirements")),
              }
              setVouchers((prev) =>
                dialog === "editVoucher"
                  ? prev.map((v) => (v.id === item.id ? item : v))
                  : [...prev, item],
              )
              notify(
                dialog === "editVoucher"
                  ? "Voucher updated. A good thing, made better."
                  : "Your new voucher is ready for your neighbours.",
              )
              setDialog(null)
            }}
          >
            <label>
              Voucher name
              <input
                className="input"
                name="name"
                defaultValue={selectedVoucher?.name || ""}
                placeholder="e.g. A little coffee on us"
                required
              />
            </label>
            <label>
              Voucher type
              <select
                className="input"
                name="type"
                defaultValue={selectedVoucher?.type || "Free beverage"}
              >
                {[
                  "Free beverage",
                  "RM5 off",
                  "RM20 off",
                  "10% off",
                  "Free brunch",
                  "Free item",
                ].map((t) => (
                  <option key={t}>{t}</option>
                ))}
              </select>
            </label>
            <div className="form-columns">
              <label>
                Eligibility end date
                <input
                  className="input"
                  name="date"
                  type="date"
                  defaultValue={selectedVoucher?.date || "2026-12-31"}
                  required
                />
              </label>
              <label>
                Points required
                <input
                  className="input"
                  name="points"
                  type="number"
                  min="1"
                  max="100000"
                  defaultValue={selectedVoucher?.points || 200}
                  required
                />
              </label>
            </div>
            <label>
              Requirements
              <textarea
                className="input"
                name="requirements"
                rows={3}
                defaultValue={selectedVoucher?.requirements || ""}
                placeholder="A few clear details about this reward…"
                required
              />
            </label>
            <div className="form-actions">
              <Button variant="secondary" onClick={() => setDialog(null)}>
                Cancel
              </Button>
              <Button type="submit">
                {dialog === "editVoucher" ? "Update voucher" : "Save voucher"}
                <Icon name="check" size={16} />
              </Button>
            </div>
          </form>
        </Modal>
      )}
      {dialog === "deleteVoucher" && selectedVoucher && (
        <Modal
          title="Say goodbye to this reward?"
          onClose={() => setDialog(null)}
        >
          <p className="modal-intro">
            “{selectedVoucher.name}” will be removed from your shop’s available
            vouchers. This can’t be undone in this demo.
          </p>
          <div className="form-actions">
            <Button variant="secondary" onClick={() => setDialog(null)}>
              Keep voucher
            </Button>
            <Button
              onClick={() => {
                setVouchers((prev) =>
                  prev.filter((v) => v.id !== selectedVoucher.id),
                )
                setDialog(null)
                notify("Voucher removed from your shop.")
              }}
            >
              <Icon name="trash" size={16} />
              Delete voucher
            </Button>
          </div>
        </Modal>
      )}
      {(dialog === "logout" || dialog === "deleteAccount") && (
        <Modal
          title={
            dialog === "logout"
              ? "Heading out for now?"
              : "Leave your local chapter behind?"
          }
          onClose={() => setDialog(null)}
        >
          <p className="modal-intro">
            {dialog === "logout"
              ? "You can come back to this demo whenever you like. Your neighbourhood will be here."
              : "This ends your demo session and clears the profile from view. No real account or backend data exists in this prototype."}
          </p>
          <div className="form-actions">
            <Button variant="secondary" onClick={() => setDialog(null)}>
              Stay a little longer
            </Button>
            <Button
              onClick={() => {
                if (dialog === "logout") setLoggedOut(true)
                else {
                  setDeleted(true)
                  setFullName("")
                  setEmail("")
                  setPoints(0)
                }
                setDialog(null)
              }}
            >
              {dialog === "logout" ? "Log out" : "Delete demo account"}
            </Button>
          </div>
        </Modal>
      )}
      {dialog === "help" && (
        <Modal
          title="A little help, right here."
          onClose={() => setDialog(null)}
        >
          <p className="modal-intro">
            SapotLokal makes it rewarding to show up for your neighbourhood.
          </p>
          <div className="help-steps">
            <div>
              <span>01</span>
              <div>
                <h3>Find a local favourite.</h3>
                <p>
                  Explore independent cafés, bakeries, and makers around Johor
                  Bahru.
                </p>
              </div>
            </div>
            <div>
              <span>02</span>
              <div>
                <h3>Visit, scan, collect.</h3>
                <p>Use the demo scanner code KOPIKITA for 30 walk-in points.</p>
              </div>
            </div>
            <div>
              <span>03</span>
              <div>
                <h3>Turn points into happy moments.</h3>
                <p>
                  Redeem vouchers, complete challenges, and enjoy a little play.
                </p>
              </div>
            </div>
          </div>
          <p className="small muted">
            This is an interactive prototype. Data stays in memory, except daily
            game allowances stored on this device. Switch to Vendor to explore
            the shop experience.
          </p>
          <Button className="full-width" onClick={() => setDialog(null)}>
            Let’s keep it local
            <Icon name="heart" size={16} />
          </Button>
        </Modal>
      )}
    </div>
  )
}
