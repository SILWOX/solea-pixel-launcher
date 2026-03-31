import { useRef } from 'react'
import { useI18n } from './i18n/I18nContext'
import { useFocusTrap } from './a11y/useFocusTrap'

/** Aide officielle Minecraft (hôte autorisé dans safeOpenExternal). */
const MINECRAFT_OFFICIAL_HELP_URL = 'https://www.minecraft.net/help'

type Props = {
  open: boolean
  onClose: () => void
}

const BULLET_KEYS: [string, string][] = [
  ['login.infoBullet1Strong', 'login.infoBullet1'],
  ['login.infoBullet2Strong', 'login.infoBullet2'],
  ['login.infoBullet3Strong', 'login.infoBullet3'],
  ['login.infoBullet4Strong', 'login.infoBullet4']
]

export function LoginInfoModal({ open, onClose }: Props) {
  const { t } = useI18n()
  const dialogRef = useRef<HTMLDivElement>(null)
  useFocusTrap(open, dialogRef, { onEscape: onClose })

  const openDocs = () => {
    void window.solea.openExternalUrl(MINECRAFT_OFFICIAL_HELP_URL)
  }

  if (!open) return null

  return (
    <div className="pack-confirm-backdrop" role="presentation" onClick={onClose}>
      <div
        ref={dialogRef}
        id="login-info-dialog"
        className="pack-confirm-modal pack-confirm-modal--login-info"
        role="dialog"
        aria-modal="true"
        aria-labelledby="login-info-title"
        aria-describedby="login-info-desc"
        onClick={(e) => e.stopPropagation()}
      >
        <p className="pack-confirm-eyebrow">{t('login.infoEyebrow')}</p>
        <h2 id="login-info-title" className="pack-confirm-title">
          {t('login.infoTitle')}
        </h2>
        <div className="login-info-modal-scroll" id="login-info-desc">
          <p className="login-info-modal-lead">{t('login.infoLead')}</p>
          <ul className="login-info-modal-points">
            {BULLET_KEYS.map(([strongKey, textKey]) => (
              <li key={strongKey}>
                <strong className="login-info-point-strong">{t(strongKey)}</strong>
                <span className="login-info-point-text">{t(textKey)}</span>
              </li>
            ))}
          </ul>
          <p className="login-info-modal-p login-info-modal-p--foot">{t('login.infoP5')}</p>
        </div>
        <div className="pack-confirm-actions pack-confirm-actions--login-info">
          <button
            type="button"
            className="login-info-btn-docs"
            aria-label={t('login.infoDocsAria')}
            onClick={(e) => {
              e.stopPropagation()
              openDocs()
            }}
          >
            {t('login.infoDocs')}
          </button>
          <button type="button" className="login-info-btn-close" onClick={onClose}>
            {t('login.infoClose')}
          </button>
        </div>
      </div>
    </div>
  )
}
