/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { useEffect } from 'react'
import { useI18n } from './i18n/I18nContext'
import { applyAppearanceSettings } from './appearance'
import './App.css'
import './alreadyRunning.css'

export function AlreadyRunningApp() {
  const { t, setLocale } = useI18n()

  useEffect(() => {
    document.title = t('alreadyRunning.windowTitle')
    void window.solea.getSettings().then((s) => {
      setLocale(s.uiLanguage)
      applyAppearanceSettings(s)
    })
  }, [t, setLocale])

  return (
    <div className="already-running-root">
      <header className="titlebar already-running-titlebar">
        <div className="titlebar-drag" role="presentation">
          <span className="titlebar-title already-running-titlebar-text">{t('alreadyRunning.titlebar')}</span>
        </div>
        <div className="titlebar-controls">
          <button
            type="button"
            className="win-btn minimize"
            title={t('titlebar.minimize')}
            aria-label={t('titlebar.minimize')}
            onClick={() => void window.solea.windowMinimize()}
          >
            <svg viewBox="0 0 12 12" aria-hidden>
              <path d="M2 6h8" stroke="currentColor" strokeWidth="1.2" fill="none" strokeLinecap="round" />
            </svg>
          </button>
          <button
            type="button"
            className="win-btn close"
            title={t('titlebar.close')}
            aria-label={t('titlebar.close')}
            onClick={() => void window.solea.windowClose()}
          >
            <svg viewBox="0 0 12 12" aria-hidden>
              <path d="M2.5 2.5l7 7M9.5 2.5l-7 7" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
            </svg>
          </button>
        </div>
      </header>

      <div className="already-running-body">
        <div className="already-running-card">
          <p className="already-running-eyebrow">{t('alreadyRunning.eyebrow')}</p>
          <h1 className="already-running-heading font-mc">{t('alreadyRunning.heading')}</h1>
          <p className="already-running-lead">{t('alreadyRunning.lead')}</p>
          <ul className="already-running-bullets">
            <li>{t('alreadyRunning.tip1')}</li>
            <li>{t('alreadyRunning.tip2')}</li>
          </ul>
          <button type="button" className="btn-save already-running-ok" onClick={() => void window.solea.windowClose()}>
            {t('alreadyRunning.ok')}
          </button>
        </div>
      </div>
    </div>
  )
}
