/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { useRef } from 'react'
import { useI18n } from './i18n/I18nContext'
import { useFocusTrap } from './a11y/useFocusTrap'

type MemoryClearConfirmModalProps = {
  open: boolean
  onConfirm: () => void
  onCancel: () => void
}

export function MemoryClearConfirmModal({ open, onConfirm, onCancel }: MemoryClearConfirmModalProps) {
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
        aria-labelledby="memory-clear-confirm-title"
        aria-describedby="memory-clear-confirm-desc"
        onClick={(e) => e.stopPropagation()}
      >
        <p className="pack-confirm-eyebrow">{t('settings.cacheMaintenance')}</p>
        <h2 id="memory-clear-confirm-title" className="pack-confirm-title">
          {t('confirm.packSureQuestion')}
        </h2>
        <p id="memory-clear-confirm-desc" className="pack-confirm-body">
          {t('settings.clearMemoryDataConfirm')}
        </p>
        <div className="pack-confirm-actions">
          <button type="button" className="btn-muted pack-confirm-btn-cancel" onClick={onCancel}>
            {t('confirm.packCancel')}
          </button>
          <button type="button" className="btn-save pack-confirm-btn-primary" onClick={onConfirm}>
            {t('settings.clearMemoryData')}
          </button>
        </div>
      </div>
    </div>
  )
}
