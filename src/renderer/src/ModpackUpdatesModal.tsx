/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { useRef } from 'react'
import type { ModpackActionInfoRow } from './launcherTypes'
import { useI18n } from './i18n/I18nContext'
import { useFocusTrap } from './a11y/useFocusTrap'

type Props = {
  open: boolean
  packs: ModpackActionInfoRow[]
  onClose: () => void
}

export function ModpackUpdatesModal({ open, packs, onClose }: Props) {
  const { t } = useI18n()
  const dialogRef = useRef<HTMLDivElement>(null)
  useFocusTrap(open, dialogRef, { onEscape: onClose })

  if (!open) return null

  const lines = packs.filter((p) => p.needsUpdate && !p.needsInstall)

  return (
    <div className="pack-confirm-backdrop" role="presentation" onClick={onClose}>
      <div
        ref={dialogRef}
        className="pack-confirm-modal pack-confirm-modal--updates"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="modpack-updates-title"
        aria-describedby="modpack-updates-desc"
        onClick={(e) => e.stopPropagation()}
      >
        <p className="pack-confirm-eyebrow">{t('home.updatesModalEyebrow')}</p>
        <h2 id="modpack-updates-title" className="pack-confirm-title">
          {t('home.updatesModalTitle')}
        </h2>
        <p id="modpack-updates-desc" className="pack-confirm-body">
          {t('home.updatesModalLead')}
        </p>
        <ul className="modpack-updates-modal-list">
          {lines.map((p) => (
            <li key={p.id}>
              {t('home.updatesModalLine', {
                name: p.displayName,
                installed: p.installedVersionNumber ?? '—',
                latest: p.latestVersionNumber ?? '—'
              })}
            </li>
          ))}
        </ul>
        <div className="pack-confirm-actions">
          <button type="button" className="btn-save pack-confirm-btn-primary" onClick={onClose}>
            {t('home.updatesModalOk')}
          </button>
        </div>
      </div>
    </div>
  )
}
