/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { useEffect, useRef } from 'react'
import { useI18n } from './i18n/I18nContext'
import { useFocusTrap } from './a11y/useFocusTrap'
import type { UnsavedSettingsSection } from './settingsUnsavedDiff'
import { devPerfMark, devPerfMeasure } from './devPerf'

export type SettingsUnsavedModalProps = {
  open: boolean
  sections: UnsavedSettingsSection[]
  busy: boolean
  onSave: () => void | Promise<void>
  onDiscard: () => void
}

export function SettingsUnsavedModal({ open, sections, busy, onSave, onDiscard }: SettingsUnsavedModalProps) {
  const { t } = useI18n()
  const dialogRef = useRef<HTMLDivElement>(null)
  useFocusTrap(open, dialogRef)

  useEffect(() => {
    if (!open) return
    const start = devPerfMark('settings-unsaved-modal-open')
    const id = requestAnimationFrame(() => {
      devPerfMeasure('settings-unsaved-modal', start)
    })
    return () => cancelAnimationFrame(id)
  }, [open])

  if (!open) return null

  return (
    <div className="settings-unsaved-backdrop" role="presentation" aria-hidden={false}>
      <div
        ref={dialogRef}
        className="settings-unsaved-modal pack-confirm-modal solea-modal-surface"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="settings-unsaved-title"
        aria-describedby="settings-unsaved-desc"
        onClick={(e) => e.stopPropagation()}
      >
        <header className="settings-unsaved-header">
          <p className="pack-confirm-eyebrow">{t('settings.unsaved.eyebrow')}</p>
          <h2 id="settings-unsaved-title" className="pack-confirm-title">
            {t('settings.unsaved.title')}
          </h2>
          <p id="settings-unsaved-desc" className="settings-unsaved-lead">
            {t('settings.unsaved.lead')}
          </p>
        </header>
        <div className="settings-unsaved-sections" role="list">
          {sections.map((sec) => (
            <section key={sec.id} className="settings-unsaved-section" role="listitem">
              <h3 className="settings-unsaved-section-title">{sec.title}</h3>
              <ul className="settings-unsaved-items">
                {sec.items.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </section>
          ))}
        </div>
        <p className="settings-unsaved-hint" role="note">
          {t('settings.unsaved.mustChoose')}
        </p>
        <footer className="settings-unsaved-footer">
        <div className="pack-confirm-actions settings-unsaved-actions">
          <button
            type="button"
            className="btn-muted pack-confirm-btn-cancel"
            disabled={busy}
            onClick={() => onDiscard()}
          >
            {t('settings.unsaved.discard')}
          </button>
          <button type="button" className="btn-save pack-confirm-btn-primary" disabled={busy} onClick={() => void onSave()}>
            {busy ? t('settings.unsaved.saving') : t('settings.unsaved.save')}
          </button>
        </div>
        </footer>
      </div>
    </div>
  )
}
