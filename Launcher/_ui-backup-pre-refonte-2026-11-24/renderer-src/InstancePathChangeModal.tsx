/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { useRef } from 'react'
import { useI18n } from './i18n/I18nContext'
import { useFocusTrap } from './a11y/useFocusTrap'

export type InstancePathChangeModalProps = {
  open: boolean
  busy: boolean
  newRootPreview: string
  onCancel: () => void
  onMove: () => void
  onReinstall: () => void
}

export function InstancePathChangeModal(props: InstancePathChangeModalProps) {
  const { open, busy, newRootPreview, onCancel, onMove, onReinstall } = props
  const { t } = useI18n()
  const dialogRef = useRef<HTMLDivElement>(null)
  useFocusTrap(open, dialogRef, { onEscape: busy ? undefined : onCancel })

  if (!open) return null

  return (
    <div className="pack-confirm-backdrop" role="presentation" onClick={busy ? undefined : onCancel}>
      <div
        ref={dialogRef}
        className="pack-confirm-modal pack-confirm-modal--support solea-modal-surface"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="instance-path-title"
        aria-describedby="instance-path-desc"
        onClick={(e) => e.stopPropagation()}
      >
        <p className="pack-confirm-eyebrow">{t('settings.instancePathModalEyebrow')}</p>
        <h2 id="instance-path-title" className="pack-confirm-title">
          {t('settings.instancePathModalTitle')}
        </h2>
        <p id="instance-path-desc" className="pack-confirm-body">
          {t('settings.instancePathModalLead')}
        </p>
        <p className="instance-path-preview" title={newRootPreview}>
          <code className="instance-path-preview-code">{newRootPreview}</code>
        </p>
        <p className="pack-confirm-body pack-confirm-body--tight">{t('settings.instancePathModalReinstallWarn')}</p>
        <div className="pack-confirm-actions instance-path-confirm-actions">
          <button type="button" className="btn-muted" disabled={busy} onClick={onCancel}>
            {t('settings.instancePathModalCancel')}
          </button>
          <button type="button" className="btn-danger-outline" disabled={busy} onClick={onReinstall}>
            {busy ? t('settings.instancePathModalWorking') : t('settings.instancePathModalReinstall')}
          </button>
          <button type="button" className="btn-save" disabled={busy} onClick={onMove}>
            {busy ? t('settings.instancePathModalWorking') : t('settings.instancePathModalMove')}
          </button>
        </div>
      </div>
    </div>
  )
}
