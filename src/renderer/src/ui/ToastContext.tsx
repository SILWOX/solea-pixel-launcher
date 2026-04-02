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

export type ToastKind = 'info' | 'success' | 'error'

export type ToastAction = { label: string; onClick: () => void }

type ToastItem = { id: number; message: string; kind: ToastKind; action?: ToastAction }

type ToastCtx = {
  pushToast: (message: string, kind?: ToastKind, durationMs?: number, action?: ToastAction) => void
}

const Ctx = createContext<ToastCtx | null>(null)

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
        {items.map((t) => (
          <div
            key={t.id}
            className={`toast-item toast-item-${t.kind}${t.action ? ' toast-item--with-action' : ''}`}
            role="status"
          >
            <span className="toast-item-text">{t.message}</span>
            {t.action ? (
              <button
                type="button"
                className="toast-item-action"
                onClick={() => {
                  t.action?.onClick()
                  remove(t.id)
                }}
              >
                {t.action.label}
              </button>
            ) : null}
          </div>
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
