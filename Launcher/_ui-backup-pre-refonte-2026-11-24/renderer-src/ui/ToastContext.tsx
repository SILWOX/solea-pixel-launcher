/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
  type ReactNode
} from 'react'
import { useI18n } from '../i18n/I18nContext'
import './toast-v3.css'

export type ToastKind = 'info' | 'success' | 'error'

export type ToastAction = { label: string; onClick: () => void }

const MAX_TOASTS = 4

type ToastItem = {
  id: number
  message: string
  kind: ToastKind
  action?: ToastAction
  /** Horodatage absolu (ms) de disparition automatique. */
  expiresAt: number
}

type ToastCtx = {
  pushToast: (message: string, kind?: ToastKind, durationMs?: number, action?: ToastAction) => void
}

const Ctx = createContext<ToastCtx | null>(null)

function ToastIcon({ kind }: { kind: ToastKind }) {
  const common = { className: 'toast-item-glyph-svg', viewBox: '0 0 24 24', 'aria-hidden': true as const }
  if (kind === 'success') {
    return (
      <svg {...common}>
        <path
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          d="M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z"
        />
        <path
          fill="none"
          stroke="currentColor"
          strokeWidth="2.2"
          strokeLinecap="round"
          strokeLinejoin="round"
          d="m8.5 12.5 2.2 2.2 5.3-6"
        />
      </svg>
    )
  }
  if (kind === 'error') {
    return (
      <svg {...common}>
        <path
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          d="M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z"
        />
        <path
          fill="none"
          stroke="currentColor"
          strokeWidth="2.2"
          strokeLinecap="round"
          d="M12 8v5M12 16.2h.01"
        />
      </svg>
    )
  }
  return (
    <svg {...common}>
      <circle cx="12" cy="12" r="9.25" fill="none" stroke="currentColor" strokeWidth="2" />
      <circle cx="12" cy="8" r="1.15" fill="currentColor" />
      <path fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" d="M12 11v7" />
    </svg>
  )
}

function ToastChrome({ item, onDismiss }: { item: ToastItem; onDismiss: () => void }) {
  const { t } = useI18n()
  const kindLabel =
    item.kind === 'success'
      ? t('toast.kindSuccess')
      : item.kind === 'error'
        ? t('toast.kindError')
        : t('toast.kindInfo')

  return (
    <div
      className={`toast-item toast-item-${item.kind}${item.action ? ' toast-item--with-action' : ''}`}
      role={item.kind === 'error' ? 'alert' : 'status'}
    >
      <div className="toast-item-accent" aria-hidden />
      <div className="toast-item-surface">
        <div className={`toast-item-icon-wrap toast-item-icon-wrap--${item.kind}`} aria-hidden>
          <ToastIcon kind={item.kind} />
        </div>
        <div className="toast-item-main">
          <div className="toast-item-header">
            <span className="toast-item-kind font-mc">{kindLabel}</span>
            <button
              type="button"
              className="toast-item-dismiss"
              aria-label={t('toast.dismiss')}
              onClick={onDismiss}
            >
              <svg viewBox="0 0 24 24" width="14" height="14" aria-hidden>
                <path
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  d="M6 6l12 12M18 6L6 18"
                />
              </svg>
            </button>
          </div>
          <p className="toast-item-text">{item.message}</p>
          {item.action ? (
            <button
              type="button"
              className="toast-item-action"
              onClick={() => {
                item.action?.onClick()
                onDismiss()
              }}
            >
              {item.action.label}
            </button>
          ) : null}
        </div>
      </div>
    </div>
  )
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])
  const [stackHovered, setStackHovered] = useState(false)
  const idRef = useRef(0)
  const timers = useRef<Map<number, number>>(new Map())
  const pausedRef = useRef(false)
  const itemsRef = useRef<ToastItem[]>([])
  itemsRef.current = items

  const clearTimer = useCallback((id: number) => {
    const tid = timers.current.get(id)
    if (tid !== undefined) {
      window.clearTimeout(tid)
      timers.current.delete(id)
    }
  }, [])

  const remove = useCallback(
    (id: number) => {
      clearTimer(id)
      setItems((prev) => prev.filter((x) => x.id !== id))
    },
    [clearTimer]
  )

  const scheduleExpiry = useCallback(
    (id: number, delayMs: number) => {
      clearTimer(id)
      const ms = Math.max(120, delayMs)
      const tid = window.setTimeout(() => {
        if (!pausedRef.current) remove(id)
      }, ms) as unknown as number
      timers.current.set(id, tid)
    },
    [clearTimer, remove]
  )

  const rescheduleAll = useCallback(() => {
    const now = Date.now()
    for (const item of itemsRef.current) {
      const remaining = item.expiresAt - now
      if (remaining <= 0) remove(item.id)
      else scheduleExpiry(item.id, remaining)
    }
  }, [remove, scheduleExpiry])

  const pauseAll = useCallback(() => {
    if (pausedRef.current) return
    pausedRef.current = true
    const now = Date.now()
    setItems((prev) =>
      prev.map((item) => {
        clearTimer(item.id)
        const remaining = Math.max(0, item.expiresAt - now)
        return { ...item, expiresAt: now + remaining }
      })
    )
  }, [clearTimer])

  const resumeAll = useCallback(() => {
    if (!pausedRef.current) return
    pausedRef.current = false
    rescheduleAll()
  }, [rescheduleAll])

  const pushToast = useCallback(
    (message: string, kind: ToastKind = 'info', durationMs = 5200, action?: ToastAction) => {
      const id = ++idRef.current
      const effectiveMs = action ? Math.max(durationMs, 16_000) : durationMs
      const expiresAt = Date.now() + effectiveMs
      setItems((prev) => {
        const next = [...prev, { id, message, kind, action, expiresAt }]
        if (next.length <= MAX_TOASTS) return next
        const trimmed = next.slice(-MAX_TOASTS)
        const kept = new Set(trimmed.map((t) => t.id))
        for (const old of prev) {
          if (!kept.has(old.id)) clearTimer(old.id)
        }
        return trimmed
      })
      if (!pausedRef.current) scheduleExpiry(id, effectiveMs)
    },
    [scheduleExpiry, clearTimer]
  )

  const value = useMemo(() => ({ pushToast }), [pushToast])

  const stacked = items.length > 1
  const stackClass = [
    'toast-stack',
    items.length > 0 ? 'toast-stack--has-items' : '',
    stacked ? 'toast-stack--stacked' : '',
    stackHovered ? 'toast-stack--expanded' : ''
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <Ctx.Provider value={value}>
      {children}
      <div
        className={stackClass}
        aria-live="polite"
        style={
          stacked
            ? ({
                ['--toast-stack-count' as string]: String(Math.min(items.length, MAX_TOASTS))
              } as React.CSSProperties)
            : undefined
        }
        onMouseEnter={() => {
          setStackHovered(true)
          pauseAll()
        }}
        onMouseLeave={() => {
          setStackHovered(false)
          resumeAll()
        }}
        onFocusCapture={() => {
          setStackHovered(true)
          pauseAll()
        }}
        onBlurCapture={(e) => {
          if (!e.currentTarget.contains(e.relatedTarget as Node | null)) {
            setStackHovered(false)
            resumeAll()
          }
        }}
      >
        {stacked && !stackHovered ? (
          <span className="toast-stack-count" aria-hidden>
            {items.length}
          </span>
        ) : null}
        {items.map((item, index) => {
          const fromTop = items.length - 1 - index
          const isFront = fromTop === 0
          return (
            <div
              key={item.id}
              className={`toast-stack-slot${isFront ? ' toast-stack-slot--front' : ''}`}
              style={
                {
                  ['--toast-stack-index' as string]: String(index),
                  ['--toast-from-top' as string]: String(fromTop)
                } as React.CSSProperties
              }
            >
              <ToastChrome item={item} onDismiss={() => remove(item.id)} />
            </div>
          )
        })}
      </div>
    </Ctx.Provider>
  )
}

export function useToast(): ToastCtx {
  const x = useContext(Ctx)
  if (!x) throw new Error('useToast outside ToastProvider')
  return x
}
