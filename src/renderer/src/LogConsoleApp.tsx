import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useI18n } from './i18n/I18nContext'
import './logConsole.css'

function lineLooksLikeError(line: string): boolean {
  const s = line.toLowerCase()
  return (
    /\berror\b/.test(s) ||
    /\bexception\b/.test(s) ||
    /\bfatal\b/.test(s) ||
    /\[erreur\]/.test(s) ||
    /\bwarn(ing)?\b/.test(s) ||
    s.includes('failed') ||
    s.includes('échec')
  )
}

/** Fenêtre secondaire : même bundle + preload que le launcher (`index.html?solea=log`). */
export function LogConsoleApp() {
  const { t, setLocale } = useI18n()
  const preRef = useRef<HTMLPreElement>(null)
  const [text, setText] = useState('')
  const [errorsOnly, setErrorsOnly] = useState(false)

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

  const displayText = useMemo(() => {
    if (!errorsOnly) return text
    return text
      .split('\n')
      .filter((line) => line.trim() && lineLooksLikeError(line))
      .join('\n')
  }, [text, errorsOnly])

  const refresh = () => {
    void window.solea.getGameLogSnapshot().then((snap) => {
      setText(snap ?? '')
      requestAnimationFrame(scrollBottom)
    })
  }

  const copyAll = async () => {
    try {
      await navigator.clipboard.writeText(displayText || text)
    } catch {
      /* ignore */
    }
  }

  const openCrash = async () => {
    const r = await window.solea.openLatestCrashReport()
    if (!r.ok) {
      window.alert(r.error)
    }
  }

  return (
    <div className="log-console-root">
      <header className="log-console-header">
        <h1 className="log-console-title">{t('logConsole.heading')}</h1>
        <div className="log-console-actions log-console-actions-wrap">
          <label className="log-console-filter">
            <input
              type="checkbox"
              checked={errorsOnly}
              onChange={(e) => setErrorsOnly(e.target.checked)}
            />
            {t('logConsole.errorsOnly')}
          </label>
          <button type="button" className="log-console-btn" onClick={() => void copyAll()}>
            {t('logConsole.copy')}
          </button>
          <button type="button" className="log-console-btn" onClick={() => void openCrash()}>
            {t('logConsole.openCrash')}
          </button>
          <button type="button" className="log-console-btn" onClick={refresh}>
            {t('logConsole.refresh')}
          </button>
          <button type="button" className="log-console-btn log-console-btn-primary" onClick={() => window.close()}>
            {t('logConsole.close')}
          </button>
        </div>
      </header>
      <pre ref={preRef} className="log-console-pre">
        {displayText}
      </pre>
    </div>
  )
}
