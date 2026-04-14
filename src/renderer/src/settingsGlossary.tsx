/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import type { TFunction } from './i18n/I18nContext'

const GLOSSARY_POP_WIDTH = 280
const GLOSSARY_VIEWPORT_MARGIN = 10

export type SettingsGlossaryKey =
  | 'afterLaunch'
  | 'openLogConsole'
  | 'networkCard'
  | 'downloadThreads'
  | 'networkTimeout'
  | 'azureId'
  | 'networkSlow'
  | 'ram'
  | 'resolution'
  | 'instanceFolder'
  | 'verifyFiles'

export function SettingsGlossaryTrigger({
  gkey,
  openKey,
  setOpenKey,
  t,
  discordUrl
}: {
  gkey: SettingsGlossaryKey
  openKey: SettingsGlossaryKey | null
  setOpenKey: (k: SettingsGlossaryKey | null) => void
  t: TFunction
  discordUrl: string
}) {
  const open = openKey === gkey
  const wrapRef = useRef<HTMLDivElement>(null)
  const [popPos, setPopPos] = useState<{
    top: number
    left: number
    width: number
    maxHeight: number
  } | null>(null)
  const title = t(`settings.glossary.${gkey}.title`)
  const body = t(`settings.glossary.${gkey}.body`)

  useLayoutEffect(() => {
    if (!open) {
      setPopPos(null)
      return
    }
    const update = () => {
      const el = wrapRef.current
      if (!el) return
      const rect = el.getBoundingClientRect()
      const vw = window.innerWidth
      const vh = window.innerHeight
      const width = Math.min(GLOSSARY_POP_WIDTH, vw - GLOSSARY_VIEWPORT_MARGIN * 2)
      let left = rect.right - width
      left = Math.max(GLOSSARY_VIEWPORT_MARGIN, Math.min(left, vw - width - GLOSSARY_VIEWPORT_MARGIN))
      const top = rect.bottom + 6
      const maxHeight = Math.max(120, vh - top - GLOSSARY_VIEWPORT_MARGIN)
      setPopPos({ top, left, width, maxHeight })
    }
    update()
    window.addEventListener('resize', update)
    document.addEventListener('scroll', update, true)
    return () => {
      window.removeEventListener('resize', update)
      document.removeEventListener('scroll', update, true)
    }
  }, [open])

  const popContent =
    open && popPos ? (
      <div
        className="settings-glossary-pop settings-glossary-pop--portal"
        style={{
          top: popPos.top,
          left: popPos.left,
          width: popPos.width,
          maxHeight: popPos.maxHeight
        }}
        role="tooltip"
      >
        <p className="settings-glossary-pop-title">{title}</p>
        <p className="settings-glossary-pop-body">{body}</p>
        <button
          type="button"
          className="btn-linkish settings-glossary-more"
          onClick={() => void window.solea.openExternalUrl(discordUrl)}
        >
          {t('home.glossary.more')}
        </button>
      </div>
    ) : null

  return (
    <div className="settings-glossary-wrap" ref={wrapRef}>
      <button
        type="button"
        className="settings-glossary-btn"
        aria-expanded={open}
        aria-label={title}
        onClick={(e) => {
          e.preventDefault()
          e.stopPropagation()
          setOpenKey(open ? null : gkey)
        }}
      >
        <svg className="settings-glossary-ico" viewBox="0 0 24 24" fill="none" aria-hidden>
          <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" />
          <path
            d="M9.5 9.5a2.5 2.5 0 015 0c0 2-2.5 1.5-2.5 4M12 17h.01"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
          />
        </svg>
      </button>
      {popContent ? createPortal(popContent, document.body) : null}
    </div>
  )
}
