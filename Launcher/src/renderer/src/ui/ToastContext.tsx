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

export type ToastKind = 'info' | 'success' | 'error'

export type ToastAction = { label: string; onClick: () => void }

type ToastItem = { id: number; message: string; kind: ToastKind; action?: ToastAction }

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
  const idRef = useRef(0)
  const timers = useRef<Map<number, number>>(new Map())

  const remove = useCallback((id: number) => {
    const t = timers.current.get(id)
    if (t) window.clearTimeout(t)
    timers.current.delete(id)
    setItems((prev) => prev.filter((x) => x.id !== id))
  }, [])

  const pushToast = useCallback(
    (message: string, kind: ToastKind = 'info', durationMs = 5200, action?: ToastAction) => {
      const id = ++idRef.current
      const effectiveMs = action ? Math.max(durationMs, 16_000) : durationMs
      setItems((prev) => [...prev.slice(-4), { id, message, kind, action }])
      const tid = window.setTimeout(() => remove(id), effectiveMs) as unknown as number
      timers.current.set(id, tid)
    },
    [remove]
  )

  const value = useMemo(() => ({ pushToast }), [pushToast])

  return (
    <Ctx.Provider value={value}>
      {children}
      <div className="toast-stack" aria-live="polite">
        {items.map((item) => (
          <ToastChrome key={item.id} item={item} onDismiss={() => remove(item.id)} />
        ))}
      </div>
    </Ctx.Provider>
  )
}

export function useToast(): ToastCtx {
  const x = useContext(Ctx)
  if (!x) throw new Error('useToast outside ToastProvider')
  return x
}
