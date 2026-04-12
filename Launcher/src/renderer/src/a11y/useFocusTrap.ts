/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { type RefObject, useEffect, useRef } from 'react'

const FOCUSABLE_SELECTOR = [
  'button:not([disabled])',
  '[href]',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])'
].join(', ')

function visibleFocusables(root: HTMLElement): HTMLElement[] {
  return Array.from(root.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)).filter((el) => {
    const style = window.getComputedStyle(el)
    if (style.visibility === 'hidden' || style.display === 'none') return false
    return el.getClientRects().length > 0
  })
}

/**
 * Garde le focus clavier dans `containerRef` tant que `active` est vrai.
 * Remet le focus sur l’élément actif à l’ouverture à la fermeture.
 */
export function useFocusTrap(
  active: boolean,
  containerRef: RefObject<HTMLElement | null>,
  options?: {
    /** Élément à focus en premier (sinon premier focusable du conteneur). */
    initialFocusRef?: RefObject<HTMLElement | null>
    /** Ex. fermer la modale sur Échap. */
    onEscape?: () => void
  }
): void {
  const prevFocus = useRef<HTMLElement | null>(null)
  const onEscapeRef = useRef(options?.onEscape)
  onEscapeRef.current = options?.onEscape

  useEffect(() => {
    if (!active) return
    const root = containerRef.current
    if (!root) return

    const ae = document.activeElement
    prevFocus.current = ae instanceof HTMLElement ? ae : null

    const focusInitial = () => {
      const initial = options?.initialFocusRef?.current
      if (initial && root.contains(initial) && !initial.hasAttribute('disabled')) {
        initial.focus()
        return
      }
      const list = visibleFocusables(root)
      list[0]?.focus()
    }

    window.setTimeout(focusInitial, 0)

    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onEscapeRef.current?.()
        return
      }
      if (e.key !== 'Tab') return
      const list = visibleFocusables(root)
      if (list.length === 0) return
      const first = list[0]
      const last = list[list.length - 1]
      const cur = document.activeElement as HTMLElement | null
      if (!e.shiftKey && cur === last) {
        e.preventDefault()
        first.focus()
      } else if (e.shiftKey && cur === first) {
        e.preventDefault()
        last.focus()
      }
    }

    document.addEventListener('keydown', onKeyDown, true)
    return () => {
      document.removeEventListener('keydown', onKeyDown, true)
      prevFocus.current?.focus({ preventScroll: true })
      prevFocus.current = null
    }
  }, [active, containerRef, options?.initialFocusRef])
}
