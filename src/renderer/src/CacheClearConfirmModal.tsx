import { useRef } from 'react'
import { useI18n } from './i18n/I18nContext'
import { useFocusTrap } from './a11y/useFocusTrap'

export type CacheClearKind = 'gradle' | 'logs'

type CacheClearConfirmModalProps = {
  open: boolean
  kind: CacheClearKind
  onConfirm: () => void
  onCancel: () => void
}

export function CacheClearConfirmModal({
  open,
  kind,
  onConfirm,
  onCancel
}: CacheClearConfirmModalProps) {
  const { t } = useI18n()
  const dialogRef = useRef<HTMLDivElement>(null)
  useFocusTrap(open, dialogRef, { onEscape: onCancel })

  if (!open) return null

  return (
    <div className="pack-confirm-backdrop" role="presentation" onClick={onCancel}>
      <div
        ref={dialogRef}
        className="pack-confirm-modal pack-confirm-modal--cache"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="cache-clear-confirm-title"
        aria-describedby="cache-clear-confirm-desc"
        onClick={(e) => e.stopPropagation()}
      >
        <p className="pack-confirm-eyebrow">{t('settings.cacheMaintenance')}</p>
        <h2 id="cache-clear-confirm-title" className="pack-confirm-title">
          {t('confirm.packSureQuestion')}
        </h2>
        <p id="cache-clear-confirm-desc" className="pack-confirm-body">
          {kind === 'gradle' ? t('settings.cacheClearGradleConfirm') : t('settings.cacheClearLogsConfirm')}
        </p>
        <div className="pack-confirm-actions">
          <button type="button" className="btn-muted pack-confirm-btn-cancel" onClick={onCancel}>
            {t('confirm.packCancel')}
          </button>
          <button type="button" className="btn-save pack-confirm-btn-primary" onClick={onConfirm}>
            {kind === 'gradle' ? t('settings.cacheClearGradle') : t('settings.cacheClearLogs')}
          </button>
        </div>
      </div>
    </div>
  )
}
