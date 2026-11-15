import { useEffect, useId, useRef, type ReactNode } from "react"

const paths: Record<string, ReactNode> = {
  home: (
    <>
      <path d="m3 10 9-7 9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1Z" />
    </>
  ),
  challenge: (
    <>
      <path d="M8 3h8v5a4 4 0 0 1-8 0Z M8 5H4v2a4 4 0 0 0 5 4m7-6h4v2a4 4 0 0 1-5 4M12 12v6m-4 3h8m-7-3h6v3" />
    </>
  ),
  scan: (
    <>
      <path d="M8 3H4a1 1 0 0 0-1 1v4m13-5h4a1 1 0 0 1 1 1v4M3 16v4a1 1 0 0 0 1 1h4m8 0h4a1 1 0 0 0 1-1v-4M3 12h18" />
      <path d="M7 7h3v3H7zm7 0h3v3h-3zM7 15h3v3H7zm7 0h3v3h-3z" />
    </>
  ),
  voucher: (
    <>
      <path d="M3 5h18v5a2 2 0 0 0 0 4v5H3v-5a2 2 0 0 0 0-4Zm12 0v3m0 3v2m0 3v3" />
    </>
  ),
  user: (
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21v-2a8 8 0 0 1 16 0v2" />
    </>
  ),
  arrow: (
    <>
      <path d="M4 12h16m-6-6 6 6-6 6" />
    </>
  ),
  chevron: <path d="m9 5 7 7-7 7" />,
  back: <path d="m14 5-7 7 7 7" />,
  bell: (
    <>
      <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9m-12 9a3 3 0 0 0 6 0" />
    </>
  ),
  pin: (
    <>
      <path d="M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0Z" />
      <circle cx="12" cy="10" r="2.5" />
    </>
  ),
  star: (
    <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2L12 17.3l-5.6 2.9 1.1-6.2L3 9.6l6.2-.9Z" />
  ),
  coin: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="m12 7 1.5 3.5L17 12l-3.5 1.5L12 17l-1.5-3.5L7 12l3.5-1.5Z" />
    </>
  ),
  coffee: (
    <>
      <path d="M4 8h12v6a6 6 0 0 1-12 0Zm12 1h2a3 3 0 0 1 0 6h-2M3 22h16M7 2v2m5-2v2" />
    </>
  ),
  shop: (
    <>
      <path d="M4 10v11h16V10M3 6l2-4h14l2 4v4a3 3 0 0 1-4 0 3 3 0 0 1-5 0 3 3 0 0 1-5 0 3 3 0 0 1-4 0Zm6 15v-7h6v7" />
    </>
  ),
  heart: (
    <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z" />
  ),
  wheel: (
    <>
      <circle cx="12" cy="12" r="9" />
      <circle cx="12" cy="12" r="2" />
      <path d="m6 6 4.5 4.5m3 3L18 18M12 3v7m0 4v7M3 12h7m4 0h7M6 18l4.5-4.5m3-3L18 6" />
    </>
  ),
  grid: (
    <>
      <rect x="3" y="3" width="7" height="7" rx="2" />
      <rect x="14" y="3" width="7" height="7" rx="2" />
      <rect x="3" y="14" width="7" height="7" rx="2" />
      <rect x="14" y="14" width="7" height="7" rx="2" />
    </>
  ),
  search: (
    <>
      <circle cx="10.5" cy="10.5" r="7" />
      <path d="m16 16 5 5" />
    </>
  ),
  clock: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 6v6l4 2" />
    </>
  ),
  close: <path d="m6 6 12 12M6 18 18 6" />,
  check: <path d="m5 12 4 4L19 6" />,
  plus: <path d="M12 4v16M4 12h16" />,
  edit: (
    <>
      <path d="m14 5 5 5M4 20l5-1L21 7l-5-5L4 14Z" />
    </>
  ),
  trash: (
    <>
      <path d="M3 6h18M8 6V3h8v3M5 6l1 15h12l1-15M10 10v7m4-7v7" />
    </>
  ),
  logout: (
    <>
      <path d="M9 3H3v18h6m6-15 6 6-6 6M8 12h13" />
    </>
  ),
  flash: <path d="m13 2-9 12h7l-1 8 10-12h-7Z" />,
  camera: (
    <>
      <path d="M3 6h4l2-3h6l2 3h4v15H3Z" />
      <circle cx="12" cy="13" r="4" />
    </>
  ),
  leaf: (
    <>
      <path d="M20 3C7 2 2 9 6 16s15 5 14-13ZM4 22 16 9" />
    </>
  ),
  help: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M9 9a3 3 0 1 1 4 3c-1 .4-1 1-1 2m0 3h.01" />
    </>
  ),
  phone: (
    <path d="m4 3 4-1 3 6-3 2a16 16 0 0 0 6 6l2-3 6 3-1 4c-1 5-20-3-18-15Z" />
  ),
  file: (
    <>
      <path d="M5 2h9l5 5v15H5ZM14 2v6h5M8 13h8m-8 4h5" />
    </>
  ),
  chart: (
    <>
      <path d="M3 3v18h18M7 16l4-6 4 3 6-8" />
    </>
  ),
  shield: (
    <>
      <path d="m12 2 9 4v6c0 5-9 10-9 10S3 17 3 12V6Z" />
      <path d="m8 12 3 3 5-6" />
    </>
  ),
}

export function Icon({
  name,
  size = 22,
  className = "",
}: {
  name: string
  size?: number
  className?: string
}) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden="true"
    >
      {paths[name] || paths.star}
    </svg>
  )
}

export function Button({
  children,
  onClick,
  variant = "primary",
  className = "",
  disabled = false,
  type = "button",
}: {
  children: ReactNode
  onClick?: () => void
  variant?: "primary" | "secondary" | "text"
  className?: string
  disabled?: boolean
  type?: "button" | "submit"
}) {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`btn btn-${variant} ${className}`}
    >
      {children}
    </button>
  )
}

interface ProgressProps {
  value: number
  max?: number
}

export function Progress({ value, max = 100 }: ProgressProps) {
  return (
    <div
      className="progress"
      role="progressbar"
      aria-valuenow={value}
      aria-valuemin={0}
      aria-valuemax={max}
    >
      <span style={{ width: `${Math.min((value / max) * 100, 100)}%` }} />
    </div>
  )
}

export function Modal({
  title,
  children,
  onClose,
}: {
  title: string
  children: ReactNode
  onClose: () => void
}) {
  const ref = useRef<HTMLElement>(null)
  const closeRef = useRef(onClose)
  const titleId = useId()
  closeRef.current = onClose
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = "hidden"
    ref.current
      ?.querySelector<HTMLElement>("button, input, select, textarea")
      ?.focus()
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") closeRef.current()
      if (event.key !== "Tab") return
      const items = ref.current?.querySelectorAll<HTMLElement>(
        "button:not(:disabled), input, select, textarea, a[href]",
      )
      if (!items?.length) return
      const first = items[0],
        last = items[items.length - 1]
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first.focus()
      }
    }
    document.addEventListener("keydown", handleKey)
    return () => {
      document.body.style.overflow = previousOverflow
      document.removeEventListener("keydown", handleKey)
      previous?.focus()
    }
  }, [])
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <section
        ref={ref}
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-heading">
          <h2 id={titleId}>{title}</h2>
          <button
            className="icon-button"
            aria-label="Close dialog"
            onClick={onClose}
          >
            <Icon name="close" />
          </button>
        </div>
        {children}
      </section>
    </div>
  )
}

export function QR({
  label = "Demo shop QR code",
  seed = 0,
}: {
  label?: string
  seed?: number
}) {
  const cells = []
  for (let y = 0; y < 25; y++)
    for (let x = 0; x < 25; x++) {
      const finder = (ox: number, oy: number) => {
        const a = x - ox,
          b = y - oy
        return (
          a >= 0 &&
          a < 7 &&
          b >= 0 &&
          b < 7 &&
          (a === 0 ||
            a === 6 ||
            b === 0 ||
            b === 6 ||
            (a >= 2 && a <= 4 && b >= 2 && b <= 4))
        )
      }
      const reserved =
        (x < 8 && y < 8) || (x > 16 && y < 8) || (x < 8 && y > 16)
      if (
        finder(0, 0) ||
        finder(18, 0) ||
        finder(0, 18) ||
        (!reserved && (x * y + x * 3 + y * 7 + seed * (x + y + 1)) % 7 < 3)
      )
        cells.push(
          <rect key={`${x}-${y}`} x={x + 2} y={y + 2} width="1" height="1" />,
        )
    }
  return (
    <svg viewBox="0 0 29 29" className="qr-code" role="img" aria-label={label}>
      {cells}
    </svg>
  )
}
