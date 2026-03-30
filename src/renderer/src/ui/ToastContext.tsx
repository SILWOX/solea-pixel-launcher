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

type ToastItem = { id: number; message: string; kind: ToastKind }

type ToastCtx = {
  pushToast: (message: string, kind?: ToastKind, durationMs?: number) => void
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
    (message: string, kind: ToastKind = 'info', durationMs = 5200) => {
      const id = ++idRef.current
      setItems((prev) => [...prev.slice(-4), { id, message, kind }])
      const tid = window.setTimeout(() => remove(id), durationMs) as unknown as number
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
          <div key={t.id} className={`toast-item toast-item-${t.kind}`} role="status">
            {t.message}
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
