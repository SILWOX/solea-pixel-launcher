/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { useEffect, useRef, useState } from 'react'
import { useI18n } from './i18n/I18nContext'
import { useFocusTrap } from './a11y/useFocusTrap'
import type { ReinstallPreserveOptions } from './launcherTypes'

export type PackMaintConfirmKind = 'reinstall' | 'uninstall'

type PackMaintConfirmModalBase = {
  open: boolean
  onCancel: () => void
}

export type PackMaintConfirmModalProps = PackMaintConfirmModalBase &
  (
    | { variant: 'reinstall'; onConfirm: (preserve: ReinstallPreserveOptions) => void }
    | { variant: 'uninstall'; onConfirm: () => void }
  )

const defaultPreserve: ReinstallPreserveOptions = {
  keepSaves: true,
  keepScreenshots: true,
  keepOptions: true
}

export function PackMaintConfirmModal(props: PackMaintConfirmModalProps) {
  const { open, variant, onCancel } = props
  const { t } = useI18n()
  const dialogRef = useRef<HTMLDivElement>(null)
  const [preserve, setPreserve] = useState<ReinstallPreserveOptions>(defaultPreserve)
  useFocusTrap(open, dialogRef, { onEscape: onCancel })

  useEffect(() => {
    if (open && variant === 'reinstall') setPreserve({ ...defaultPreserve })
  }, [open, variant])

  if (!open) return null

  const describedBy =
    variant === 'reinstall'
      ? 'pack-confirm-desc pack-confirm-preserve-hint'
      : 'pack-confirm-desc'

  return (
    <div className="pack-confirm-backdrop" role="presentation" onClick={onCancel}>
      <div
        ref={dialogRef}
        className={`pack-confirm-modal pack-confirm-modal--${variant}`}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="pack-confirm-title"
        aria-describedby={describedBy}
        onClick={(e) => e.stopPropagation()}
      >
        <p className="pack-confirm-eyebrow">{t('confirm.packDialogEyebrow')}</p>
        <h2 id="pack-confirm-title" className="pack-confirm-title">
          {t('confirm.packSureQuestion')}
        </h2>
        <p id="pack-confirm-desc" className="pack-confirm-body">
          {variant === 'reinstall' ? t('confirm.packReinstallDetail') : t('confirm.packUninstallDetail')}
        </p>
        {variant === 'reinstall' ? (
          <div
            id="pack-confirm-preserve-hint"
            className="pack-reinstall-preserve"
            role="group"
            aria-label={t('confirm.packReinstallPreserveGroup')}
          >
            <p className="pack-reinstall-preserve-lead">{t('confirm.packReinstallPreserveLead')}</p>
            <label className="pack-reinstall-preserve-row">
              <input
                type="checkbox"
                checked={preserve.keepSaves}
                onChange={(e) => setPreserve((p) => ({ ...p, keepSaves: e.target.checked }))}
              />
              <span>{t('confirm.packReinstallKeepSaves')}</span>
            </label>
            <label className="pack-reinstall-preserve-row">
              <input
                type="checkbox"
                checked={preserve.keepScreenshots}
                onChange={(e) => setPreserve((p) => ({ ...p, keepScreenshots: e.target.checked }))}
              />
              <span>{t('confirm.packReinstallKeepScreenshots')}</span>
            </label>
            <label className="pack-reinstall-preserve-row">
              <input
                type="checkbox"
                checked={preserve.keepOptions}
                onChange={(e) => setPreserve((p) => ({ ...p, keepOptions: e.target.checked }))}
              />
              <span>{t('confirm.packReinstallKeepOptions')}</span>
            </label>
          </div>
        ) : null}
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
            onClick={() => {
              if (variant === 'reinstall') props.onConfirm(preserve)
              else props.onConfirm()
            }}
          >
            {variant === 'reinstall' ? t('confirm.packConfirmReinstall') : t('confirm.packConfirmUninstall')}
          </button>
        </div>
      </div>
    </div>
  )
}
