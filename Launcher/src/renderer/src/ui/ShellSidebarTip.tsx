import { useCallback, useEffect, useRef, useState, cloneElement, isValidElement, type ReactElement } from 'react'
import { createPortal } from 'react-dom'

type ShellSidebarTipProps = {
  /** Texte affiché (déjà traduit). */
  label: string
  /** Délai avant affichage au survol (ms). */
  delayMs?: number
  children: ReactElement
}

/**
 * Infobulle latérale thémée (pas le `title` natif du navigateur).
 * Position fixe via portail pour éviter le `overflow: hidden` de `.shell-sidebar`.
 */
export function ShellSidebarTip({ label, delayMs = 500, children }: ShellSidebarTipProps) {
  const wrapRef = useRef<HTMLSpanElement>(null)
  const [visible, setVisible] = useState(false)
  const [pos, setPos] = useState<{ left: number; top: number } | null>(null)
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const clearTimer = useCallback(() => {
    if (timerRef.current != null) {
      clearTimeout(timerRef.current)
      timerRef.current = null
    }
  }, [])

  const hide = useCallback(() => {
    clearTimer()
    setVisible(false)
    setPos(null)
  }, [clearTimer])

  const scheduleShow = useCallback(() => {
    clearTimer()
    timerRef.current = setTimeout(() => {
      const el = wrapRef.current
      if (!el) return
      const r = el.getBoundingClientRect()
      setPos({ left: r.right + 10, top: r.top + r.height / 2 })
      setVisible(true)
    }, delayMs)
  }, [clearTimer, delayMs])

  useEffect(() => () => hide(), [hide])

  const child = isValidElement(children)
    ? cloneElement(children as ReactElement<{ title?: string }>, { title: undefined })
    : children

  const bubble =
    visible && pos
      ? createPortal(
          <span
            className="shell-sidebar-tip-anchor"
            role="presentation"
            style={{
              position: 'fixed',
              left: pos.left,
              top: pos.top,
              transform: 'translateY(-50%)',
              zIndex: 100000
            }}
          >
            <span className="shell-sidebar-tip-bubble" role="tooltip">
              {label}
            </span>
          </span>,
          document.body
        )
      : null

  return (
    <>
      <span
        ref={wrapRef}
        className="shell-sidebar-tip-wrap"
        onMouseEnter={scheduleShow}
        onMouseLeave={hide}
      >
        {child}
      </span>
      {bubble}
    </>
  )
}
