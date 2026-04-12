/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { memo, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useI18n } from './i18n/I18nContext'
import { applyAppearanceSettings, subscribeSystemTheme } from './appearance'
import './App.css'
import './logConsole.css'

const MAX_TAIL_LINES = 8000

function lineLooksLikeError(line: string): boolean {
  const s = line.toLowerCase()
  return (
    /\berror\b/.test(s) ||
    /\bexception\b/.test(s) ||
    /\bfatal\b/.test(s) ||
    /\[erreur\]/.test(s) ||
    s.includes('failed') ||
    s.includes('échec')
  )
}

function lineLooksLikeWarn(line: string): boolean {
  const s = line.toLowerCase()
  return /\bwarn(ing)?\b/.test(s) || /\/warn\]/i.test(s) || /\battention\b/.test(s)
}

function getLineKind(line: string): 'err' | 'warn' | 'info' | 'plain' {
  if (lineLooksLikeError(line)) return 'err'
  if (lineLooksLikeWarn(line)) return 'warn'
  if (/\/(info|debug)\]/i.test(line) || /\[(main|client|server|render thread)\//i.test(line)) return 'info'
  return 'plain'
}

const LogLine = memo(function LogLine({ line }: { line: string }) {
  const k = getLineKind(line)
  return <div className={`log-console-line log-console-line--${k}`}>{line || '\u00a0'}</div>
})

type LogFont = 's' | 'm' | 'l'

/** Fenêtre secondaire : `index.html?solea=log`, barre titre custom AETHER. */
export function LogConsoleApp() {
  const { t, formatInteger, setLocale } = useI18n()
  const scrollRef = useRef<HTMLDivElement>(null)
  const [text, setText] = useState('')
  const [errorsOnly, setErrorsOnly] = useState(false)
  const [filterQuery, setFilterQuery] = useState('')
  const [wrapLines, setWrapLines] = useState(true)
  const [fontSize, setFontSize] = useState<LogFont>('m')
  const [autoScroll, setAutoScroll] = useState(true)
  const [nearBottom, setNearBottom] = useState(true)
  const [maximized, setMaximized] = useState(false)

  useEffect(() => {
    void window.solea.getSettings().then((s) => {
      setLocale(s.uiLanguage)
      applyAppearanceSettings(s)
    })
  }, [setLocale])

  useEffect(() => {
    return subscribeSystemTheme(() => {
      void window.solea.getSettings().then(applyAppearanceSettings)
    })
  }, [])

  useEffect(() => {
    void window.solea.windowIsMaximized().then(setMaximized)
    return window.solea.onWindowMaximized(setMaximized)
  }, [])

  const scrollToBottom = useCallback(() => {
    const el = scrollRef.current
    if (!el) return
    requestAnimationFrame(() => {
      el.scrollTop = el.scrollHeight
    })
  }, [])

  useEffect(() => {
    void window.solea.getGameLogSnapshot().then((snap) => {
      if (snap) setText(snap)
      requestAnimationFrame(scrollToBottom)
    })
    const off = window.solea.onGameLog((line) => {
      setText((p) => p + line)
    })
    return off
  }, [scrollToBottom])

  useEffect(() => {
    if (!autoScroll || !nearBottom) return
    scrollToBottom()
  }, [text, autoScroll, nearBottom, scrollToBottom])

  useEffect(() => {
    document.title = t('logConsole.windowTitle')
  }, [t])

  const onScroll = useCallback(() => {
    const el = scrollRef.current
    if (!el) return
    const dist = el.scrollHeight - el.scrollTop - el.clientHeight
    setNearBottom(dist < 80)
  }, [])

  const { visibleLines, truncated } = useMemo(() => {
    let lines = text.split('\n')
    if (errorsOnly) {
      lines = lines.filter((line) => line.trim() && (lineLooksLikeError(line) || lineLooksLikeWarn(line)))
    }
    const q = filterQuery.trim().toLowerCase()
    if (q) lines = lines.filter((line) => line.toLowerCase().includes(q))
    if (lines.length > MAX_TAIL_LINES) {
      return { visibleLines: lines.slice(-MAX_TAIL_LINES), truncated: true }
    }
    return { visibleLines: lines, truncated: false }
  }, [text, errorsOnly, filterQuery])

  const displayString = useMemo(() => visibleLines.join('\n'), [visibleLines])

  const refresh = () => {
    void window.solea.getGameLogSnapshot().then((snap) => {
      setText(snap ?? '')
      requestAnimationFrame(scrollToBottom)
    })
  }

  const copyVisible = async () => {
    try {
      await navigator.clipboard.writeText(displayString)
    } catch {
      /* ignore */
    }
  }

  const openCrash = async () => {
    const r = await window.solea.openLatestCrashReport()
    if (!r.ok) window.alert(r.error)
  }

  const clearBuffer = async () => {
    if (!window.confirm(t('logConsole.clearBufferConfirm'))) return
    await window.solea.clearGameLogBuffer()
    setText('')
  }

  const toggleMax = async () => {
    const r = await window.solea.windowToggleMaximize()
    setMaximized(r.maximized)
  }

  const jumpToLatest = () => {
    setNearBottom(true)
    setAutoScroll(true)
    scrollToBottom()
  }

  return (
    <div className="log-console-root">
      <header className="titlebar log-console-titlebar">
        <div className="titlebar-drag" role="presentation" onDoubleClick={() => void toggleMax()}>
          <span className="titlebar-title log-console-titlebar-product">{t('logConsole.titlebar')}</span>
          <span className="log-console-titlebar-badge">AETHER</span>
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
            className="win-btn maximize"
            title={maximized ? t('titlebar.restore') : t('titlebar.maximize')}
            aria-label={maximized ? t('titlebar.restore') : t('titlebar.maximize')}
            onClick={() => void toggleMax()}
          >
            {maximized ? (
              <svg viewBox="0 0 12 12" aria-hidden>
                <path
                  d="M4.5 2.5h5v5h-5v-5zM2.5 4.5v5h5"
                  stroke="currentColor"
                  strokeWidth="1.1"
                  fill="none"
                  strokeLinejoin="round"
                />
              </svg>
            ) : (
              <svg viewBox="0 0 12 12" aria-hidden>
                <rect x="2" y="2" width="8" height="8" stroke="currentColor" strokeWidth="1.1" fill="none" rx="0.5" />
              </svg>
            )}
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

      <div className="log-console-toolbar">
        <div className="log-console-toolbar-text">
          <h1 className="log-console-heading font-mc">{t('logConsole.heading')}</h1>
          <p className="log-console-sub">{t('logConsole.subtitle')}</p>
        </div>
        <div className="log-console-toolbar-meta" aria-live="polite">
          <span className="log-console-meta-pill">
            {t('logConsole.lineCount', { n: formatInteger(visibleLines.length) })}
          </span>
          {autoScroll && !nearBottom ? (
            <span className="log-console-meta-pill log-console-meta-pill--warn">{t('logConsole.pausedScroll')}</span>
          ) : null}
        </div>
      </div>

      <div className="log-console-controls">
        <label className="log-console-chip">
          <input
            type="checkbox"
            checked={errorsOnly}
            onChange={(e) => setErrorsOnly(e.target.checked)}
          />
          <span>{t('logConsole.errorsOnly')}</span>
        </label>
        <label className="log-console-chip">
          <input type="checkbox" checked={autoScroll} onChange={(e) => setAutoScroll(e.target.checked)} />
          <span>{t('logConsole.autoScroll')}</span>
        </label>
        <label className="log-console-chip">
          <input type="checkbox" checked={wrapLines} onChange={(e) => setWrapLines(e.target.checked)} />
          <span>{t('logConsole.wrap')}</span>
        </label>
        <span className="log-console-seg-label">{t('logConsole.fontSize')}</span>
        <div className="log-console-segment" role="group" aria-label={t('logConsole.fontSize')}>
          {(['s', 'm', 'l'] as const).map((k) => (
            <button
              key={k}
              type="button"
              className={`log-console-seg-btn${fontSize === k ? ' is-active' : ''}`}
              onClick={() => setFontSize(k)}
            >
              {t(`logConsole.font${k === 's' ? 'Small' : k === 'm' ? 'Medium' : 'Large'}`)}
            </button>
          ))}
        </div>
        <input
          type="search"
          className="log-console-search"
          placeholder={t('logConsole.searchPlaceholder')}
          value={filterQuery}
          onChange={(e) => setFilterQuery(e.target.value)}
          aria-label={t('logConsole.searchPlaceholder')}
        />
      </div>

      <div className="log-console-actions-row">
        <button type="button" className="btn-muted log-console-action-btn" onClick={() => void copyVisible()}>
          {t('logConsole.copy')}
        </button>
        <button type="button" className="btn-muted log-console-action-btn" onClick={() => void openCrash()}>
          {t('logConsole.openCrash')}
        </button>
        <button type="button" className="btn-muted log-console-action-btn" onClick={refresh}>
          {t('logConsole.refresh')}
        </button>
        <button type="button" className="btn-muted log-console-action-btn" onClick={jumpToLatest}>
          {t('logConsole.followTail')}
        </button>
        <button type="button" className="btn-muted log-console-action-btn" onClick={() => void clearBuffer()}>
          {t('logConsole.clearBuffer')}
        </button>
        <button type="button" className="btn-save log-console-action-btn" onClick={() => void window.solea.windowClose()}>
          {t('logConsole.close')}
        </button>
      </div>

      {truncated ? (
        <p className="log-console-truncate-note" role="status">
          {t('logConsole.truncated', { n: formatInteger(MAX_TAIL_LINES) })}
        </p>
      ) : null}

      <div
        ref={scrollRef}
        className={`log-console-viewport log-console-viewport--font-${fontSize}${wrapLines ? ' log-console-viewport--wrap' : ''}`}
        onScroll={onScroll}
      >
        {visibleLines.map((line, i) => (
          <LogLine key={i} line={line} />
        ))}
      </div>
    </div>
  )
}
