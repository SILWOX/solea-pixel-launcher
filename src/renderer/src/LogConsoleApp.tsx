import { useCallback, useEffect, useRef, useState } from 'react'
import { useI18n } from './i18n/I18nContext'
import './logConsole.css'

/** Fenêtre secondaire : même bundle + preload que le launcher (`index.html?solea=log`). */
export function LogConsoleApp() {
  const { t, setLocale } = useI18n()
  const preRef = useRef<HTMLPreElement>(null)
  const [text, setText] = useState('')

  useEffect(() => {
    void window.solea.getSettings().then((s) => setLocale(s.uiLanguage))
  }, [setLocale])

  const scrollBottom = useCallback(() => {
    const el = preRef.current
    if (el) el.scrollTop = el.scrollHeight
  }, [])

  useEffect(() => {
    void window.solea.getGameLogSnapshot().then((snap) => {
      if (snap) setText(snap)
      requestAnimationFrame(scrollBottom)
    })
    const off = window.solea.onGameLog((line) => {
      setText((p) => p + line)
      requestAnimationFrame(scrollBottom)
    })
    return off
  }, [scrollBottom])

  useEffect(() => {
    document.title = t('logConsole.windowTitle')
  }, [t])

  const refresh = () => {
    void window.solea.getGameLogSnapshot().then((snap) => {
      setText(snap ?? '')
      requestAnimationFrame(scrollBottom)
    })
  }

  return (
    <div className="log-console-root">
      <header className="log-console-header">
        <h1 className="log-console-title">{t('logConsole.heading')}</h1>
        <div className="log-console-actions">
          <button type="button" className="log-console-btn" onClick={refresh}>
            {t('logConsole.refresh')}
          </button>
          <button type="button" className="log-console-btn log-console-btn-primary" onClick={() => window.close()}>
            {t('logConsole.close')}
          </button>
        </div>
      </header>
      <pre ref={preRef} className="log-console-pre">
        {text}
      </pre>
    </div>
  )
}
