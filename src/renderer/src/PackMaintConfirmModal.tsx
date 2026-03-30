import { useRef } from 'react'
import { useI18n } from './i18n/I18nContext'
import { useFocusTrap } from './a11y/useFocusTrap'

export type PackMaintConfirmKind = 'reinstall' | 'uninstall'

type PackMaintConfirmModalProps = {
  open: boolean
  variant: PackMaintConfirmKind
  onConfirm: () => void
  onCancel: () => void
}

export function PackMaintConfirmModal({
  open,
  variant,
  onConfirm,
  onCancel
}: PackMaintConfirmModalProps) {
  const { t } = useI18n()
  const dialogRef = useRef<HTMLDivElement>(null)
  useFocusTrap(open, dialogRef, { onEscape: onCancel })

  if (!open) return null

  return (
    <div className="pack-confirm-backdrop" role="presentation" onClick={onCancel}>
      <div
        ref={dialogRef}
        className={`pack-confirm-modal pack-confirm-modal--${variant}`}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="pack-confirm-title"
        aria-describedby="pack-confirm-desc"
        onClick={(e) => e.stopPropagation()}
      >
        <p className="pack-confirm-eyebrow">{t('confirm.packDialogEyebrow')}</p>
        <h2 id="pack-confirm-title" className="pack-confirm-title">
          {t('confirm.packSureQuestion')}
        </h2>
        <p id="pack-confirm-desc" className="pack-confirm-body">
          {variant === 'reinstall' ? t('confirm.packReinstallDetail') : t('confirm.packUninstallDetail')}
        </p>
        <div className="pack-confirm-actions">
          <button type="button" className="btn-muted pack-confirm-btn-cancel" onClick={onCancel}>
            {t('confirm.packCancel')}
          </button>
          <button
            type="button"
            className={
              variant === 'uninstall'
                ? 'btn-save pack-confirm-btn-danger'
                : 'btn-save pack-confirm-btn-primary'
            }
            onClick={onConfirm}
          >
            {variant === 'reinstall' ? t('confirm.packConfirmReinstall') : t('confirm.packConfirmUninstall')}
          </button>
        </div>
      </div>
    </div>
  )
}
