/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { useRef } from 'react'
import { useI18n } from './i18n/I18nContext'
import { useFocusTrap } from './a11y/useFocusTrap'

type CreditsModalProps = {
  open: boolean
  onClose: () => void
}

function IconCreditsSpark({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" width={22} height={22} fill="none" aria-hidden>
      <path
        d="M12 2l1.2 4.2L17.4 7.5l-4.2 1.2L12 12.9l-1.2-4.2L6.6 7.5l4.2-1.2L12 2z"
        fill="currentColor"
        opacity={0.95}
      />
      <path
        d="M18 14l.7 2.4 2.4.7-2.4.7L18 20l-.7-2.4-2.4-.7 2.4-.7L18 14zM6 15l.5 1.8 1.8.5-1.8.5L6 19.8l-.5-1.8-1.8-.5 1.8-.5L6 15z"
        fill="currentColor"
        opacity={0.55}
      />
    </svg>
  )
}

function IconCreditsPerson({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" width={20} height={20} fill="none" aria-hidden>
      <circle cx="12" cy="8" r="3.5" stroke="currentColor" strokeWidth="1.75" />
      <path
        d="M5.5 19.5c0-4 2.9-6.5 6.5-6.5s6.5 2.5 6.5 6.5"
        stroke="currentColor"
        strokeWidth="1.75"
        strokeLinecap="round"
      />
    </svg>
  )
}

function IconCreditsLayers({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" width={20} height={20} fill="none" aria-hidden>
      <path
        d="M12 3.5L4.5 7.25 12 11l7.5-3.75L12 3.5z"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinejoin="round"
      />
      <path
        d="M4.5 12.25L12 16l7.5-3.75M4.5 16.75L12 20.5l7.5-3.75"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function IconCreditsShield({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" width={20} height={20} fill="none" aria-hidden>
      <path
        d="M12 3.2l6.2 2.8v6.4c0 4.1-2.6 7.9-6.2 9.3-3.6-1.4-6.2-5.2-6.2-9.3V6l6.2-2.8z"
        stroke="currentColor"
        strokeWidth="1.65"
        strokeLinejoin="round"
      />
      <path
        d="M9.2 12.3l2.2 2.2 3.8-4.6"
        stroke="currentColor"
        strokeWidth="1.65"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

export function CreditsModal({ open, onClose }: CreditsModalProps) {
  const { t } = useI18n()
  const dialogRef = useRef<HTMLDivElement>(null)
  useFocusTrap(open, dialogRef, { onEscape: onClose })

  if (!open) return null

  return (
    <div className="pack-confirm-backdrop credits-backdrop" role="presentation" onClick={onClose}>
      <div
        ref={dialogRef}
        className="pack-confirm-modal credits-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="credits-studio-title"
        aria-describedby="credits-modal-desc"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="credits-modal-surface">
          <header className="credits-hero">
            <div className="credits-hero-accent" aria-hidden />
            <div className="credits-hero-noise" aria-hidden />
            <p className="credits-eyebrow">{t('settings.creditsEyebrow')}</p>
            <div className="credits-hero-icon-wrap" aria-hidden>
              <IconCreditsSpark className="credits-hero-icon" />
            </div>
            <h2 id="credits-studio-title" className="credits-studio-title">
              {t('settings.creditsStudio')}
            </h2>
            <p className="credits-tagline">{t('settings.creditsTagline')}</p>
            <div className="credits-hero-badges" aria-label={t('settings.creditsUiLine')}>
              <span className="credits-pill credits-pill--primary">{t('settings.creditsUiBadge')}</span>
              <span className="credits-pill credits-pill--ghost">{t('settings.creditsUiVersion')}</span>
            </div>
          </header>

          <div id="credits-modal-desc" className="credits-modal-body">
            <div className="credits-cards">
              <article className="credits-card">
                <div className="credits-card-icon" aria-hidden>
                  <IconCreditsPerson />
                </div>
                <div className="credits-card-main">
                  <h3 className="credits-card-title">{t('settings.creditsCreatorHeading')}</h3>
                  <p className="credits-creator-name">{t('settings.creditsCreatorName')}</p>
                  <p className="credits-creator-role">{t('settings.creditsCreatorRole')}</p>
                </div>
              </article>

              <article className="credits-card">
                <div className="credits-card-icon" aria-hidden>
                  <IconCreditsLayers />
                </div>
                <div className="credits-card-main">
                  <h3 className="credits-card-title">{t('settings.creditsAboutTitle')}</h3>
                  <p className="credits-card-text">{t('settings.creditsDetails')}</p>
                </div>
              </article>

              <article className="credits-card credits-card--ui">
                <div className="credits-card-icon credits-card-icon--ui" aria-hidden>
                  <span className="credits-ui-mini">◇</span>
                </div>
                <div className="credits-card-main">
                  <h3 className="credits-card-title">{t('settings.creditsUiTitle')}</h3>
                  <p className="credits-card-text credits-card-text--mono">{t('settings.creditsUiLine')}</p>
                </div>
              </article>
            </div>

            <aside className="credits-license" role="note">
              <div className="credits-license-head">
                <span className="credits-license-icon" aria-hidden>
                  <IconCreditsShield />
                </span>
                <p className="credits-license-title">{t('settings.creditsLicenseTitle')}</p>
              </div>
              <p className="credits-license-body">{t('settings.creditsLicenseBody')}</p>
            </aside>
          </div>

          <footer className="credits-modal-footer">
            <button type="button" className="credits-close-btn" onClick={onClose}>
              {t('settings.creditsClose')}
            </button>
          </footer>
        </div>
      </div>
    </div>
  )
}
