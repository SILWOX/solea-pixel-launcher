import type { CSSProperties } from 'react'
import { useCallback, useEffect, useId, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import './LauncherSelect.css'

export type LauncherSelectOption = { value: string; label: string }

export function LauncherSelect({
  value,
  onChange,
  options,
  disabled,
  className,
  compact,
  'aria-label': ariaLabel
}: {
  value: string
  onChange: (next: string) => void
  options: LauncherSelectOption[]
  disabled?: boolean
  className?: string
  compact?: boolean
  'aria-label'?: string
}) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)
  const triggerRef = useRef<HTMLButtonElement>(null)
  const listRef = useRef<HTMLDivElement>(null)
  const listId = useId()
  const [menuStyle, setMenuStyle] = useState<CSSProperties | null>(null)

  const selected = options.find((o) => o.value === value) ?? options[0]

  const updatePosition = useCallback(() => {
    const el = triggerRef.current
    if (!el) return
    const r = el.getBoundingClientRect()
    const gap = 5
    const top = r.bottom + gap
    const maxH = Math.max(120, Math.min(280, window.innerHeight - top - 10))
    setMenuStyle({
      position: 'fixed',
      top,
      left: r.left,
      width: r.width,
      maxHeight: maxH,
      zIndex: 30000
    })
  }, [])

  useLayoutEffect(() => {
    if (!open) {
      setMenuStyle(null)
      return
    }
    updatePosition()
    const onWin = () => updatePosition()
    window.addEventListener('resize', onWin)
    const ro =
      typeof ResizeObserver !== 'undefined' ? new ResizeObserver(onWin) : null
    ro?.observe(document.documentElement)
    window.addEventListener('scroll', onWin, true)
    return () => {
      window.removeEventListener('resize', onWin)
      ro?.disconnect()
      window.removeEventListener('scroll', onWin, true)
    }
  }, [open, updatePosition])

  useEffect(() => {
    if (!open) return
    const onDoc = (e: MouseEvent) => {
      const node = e.target as Node
      if (rootRef.current?.contains(node)) return
      if (listRef.current?.contains(node)) return
      setOpen(false)
    }
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onDoc)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDoc)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  const menu =
    open &&
    menuStyle &&
    createPortal(
      <div
        ref={listRef}
        id={listId}
        className="launcher-select-menu"
        role="listbox"
        style={menuStyle}
      >
        {options.map((o) => (
          <button
            key={o.value}
            type="button"
            role="option"
            aria-selected={o.value === value}
            className={`launcher-select-option${o.value === value ? ' is-selected' : ''}`}
            onClick={() => {
              onChange(o.value)
              setOpen(false)
            }}
          >
            {o.label}
          </button>
        ))}
      </div>,
      document.body
    )

  return (
    <div
      ref={rootRef}
      className={`launcher-select-root${compact ? ' launcher-select-root--compact' : ''}${className ? ` ${className}` : ''}`}
    >
      <button
        ref={triggerRef}
        type="button"
        className="launcher-select-trigger"
        disabled={disabled}
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={open ? listId : undefined}
        onClick={() => !disabled && setOpen((v) => !v)}
      >
        <span className="launcher-select-value">{selected?.label ?? value}</span>
        <span className="launcher-select-chev" aria-hidden />
      </button>
      {menu}
    </div>
  )
}
