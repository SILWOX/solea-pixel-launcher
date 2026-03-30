import { useCallback, useEffect, useRef, useState } from 'react'
import { useI18n } from './i18n/I18nContext'
import type { DebugSnapshotUi } from './debugTypes'
import './debug.css'

function formatBytes(n: number): string {
  if (!Number.isFinite(n) || n < 0) return '—'
  const u = ['B', 'KB', 'MB', 'GB', 'TB']
  let v = n
  let i = 0
  while (v >= 1024 && i < u.length - 1) {
    v /= 1024
    i++
  }
  return `${v < 10 && i > 0 ? v.toFixed(1) : Math.round(v)} ${u[i]}`
}

function formatUptimeSec(sec: number, formatInt: (n: number) => string): string {
  const s = Math.max(0, Math.floor(sec))
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const rs = s % 60
  if (h > 0) return `${formatInt(h)}h ${formatInt(m)}m ${formatInt(rs)}s`
  if (m > 0) return `${formatInt(m)}m ${formatInt(rs)}s`
  return `${formatInt(rs)}s`
}

type PerfMem = { usedJSHeapSize: number; totalJSHeapSize: number; jsHeapSizeLimit: number }

const REFRESH_OPTIONS_MS = [0, 500, 1000, 2000, 5000] as const

export function DebugApp() {
  const { t, setLocale, formatInteger, formatDate } = useI18n()
  const [snap, setSnap] = useState<DebugSnapshotUi | null>(null)
  const [snapErr, setSnapErr] = useState<string | null>(null)
  const [fps, setFps] = useState(0)
  const [reloadFeedback, setReloadFeedback] = useState<null | { ok: boolean; text: string }>(null)
  const [refreshMs, setRefreshMs] = useState<number>(2000)
  const [showProcessTable, setShowProcessTable] = useState(true)
  const [copyFeedback, setCopyFeedback] = useState<string | null>(null)
  const [folderErr, setFolderErr] = useState<string | null>(null)
  const [lastPullAt, setLastPullAt] = useState<Date | null>(null)
  const copyTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    void window.solea.getSettings().then((s) => setLocale(s.uiLanguage))
  }, [setLocale])

  useEffect(() => {
    document.title = t('debug.windowTitle')
  }, [t])

  const pullSnapshot = useCallback(async () => {
    try {
      const s = await window.solea.getDebugSnapshot()
      setSnap(s)
      setSnapErr(null)
      setLastPullAt(new Date())
    } catch (e) {
      setSnapErr(e instanceof Error ? e.message : String(e))
    }
  }, [])

  useEffect(() => {
    void pullSnapshot()
  }, [pullSnapshot])

  useEffect(() => {
    if (refreshMs <= 0) return
    const id = window.setInterval(() => void pullSnapshot(), refreshMs)
    return () => window.clearInterval(id)
  }, [pullSnapshot, refreshMs])

  useEffect(() => {
    let frames = 0
    let last = performance.now()
    let rafId = 0
    const loop = (now: number) => {
      frames++
      const elapsed = now - last
      if (elapsed >= 1000) {
        setFps(Math.round((frames * 1000) / elapsed))
        frames = 0
        last = now
      }
      rafId = requestAnimationFrame(loop)
    }
    rafId = requestAnimationFrame(loop)
    return () => cancelAnimationFrame(rafId)
  }, [])

  useEffect(() => {
    return () => {
      if (copyTimerRef.current) clearTimeout(copyTimerRef.current)
    }
  }, [])

  const perfMem = (performance as unknown as { memory?: PerfMem }).memory

  const onReloadMain = async () => {
    setReloadFeedback(null)
    const r = await window.solea.reloadMainLauncher()
    if (r.ok) setReloadFeedback({ ok: true, text: t('debug.reloadOk') })
    else setReloadFeedback({ ok: false, text: r.error ?? t('debug.reloadFail') })
  }

  const onOpenFolder = async (kind: 'userData' | 'instanceRoot') => {
    setFolderErr(null)
    const r = await window.solea.debugOpenKnownFolder(kind)
    if (!r.ok) setFolderErr(r.error ?? t('debug.openFolderFail'))
  }

  const onCopySnapshot = async () => {
    setCopyFeedback(null)
    if (copyTimerRef.current) clearTimeout(copyTimerRef.current)
    const payload = {
      capturedAt: new Date().toISOString(),
      debugWindowFps: fps,
      snapshot: snap
    }
    try {
      await navigator.clipboard.writeText(JSON.stringify(payload, null, 2))
      setCopyFeedback(t('debug.copySnapshotOk'))
    } catch {
      setCopyFeedback(t('debug.copySnapshotFail'))
    }
    copyTimerRef.current = setTimeout(() => setCopyFeedback(null), 3500)
  }

  const refreshLabelKey = (ms: number): string => {
    if (ms === 0) return 'debug.refreshOff'
    if (ms === 500) return 'debug.refresh500ms'
    if (ms === 1000) return 'debug.refresh1s'
    if (ms === 2000) return 'debug.refresh2s'
    return 'debug.refresh5s'
  }

  const pm = snap?.processMemory
  const loadAvgActive = snap && snap.loadAvg.some((x) => x > 0)

  return (
    <div className="debug-layout">
      <header className="debug-header">
        <div className="debug-header-text">
          <h1 className="debug-title">{t('debug.heading')}</h1>
          <p className="debug-subtitle">{t('debug.subtitle')}</p>
        </div>
        <div className="debug-header-actions">
          <button type="button" className="debug-btn" onClick={() => void pullSnapshot()}>
            {t('debug.refresh')}
          </button>
          <button type="button" className="debug-btn debug-btn--accent" onClick={() => void onReloadMain()}>
            {t('debug.reloadLauncher')}
          </button>
          <button type="button" className="debug-btn" onClick={() => window.close()}>
            {t('debug.close')}
          </button>
        </div>
      </header>

      <section className="debug-toolbar" aria-labelledby="debug-tools-heading">
        <h2 id="debug-tools-heading" className="debug-toolbar-title">
          {t('debug.toolsTitle')}
        </h2>
        <div className="debug-toolbar-row">
          <label className="debug-field">
            <span className="debug-field-label">{t('debug.autoRefresh')}</span>
            <select
              className="debug-select"
              value={refreshMs}
              onChange={(e) => setRefreshMs(Number(e.target.value))}
            >
              {REFRESH_OPTIONS_MS.map((ms) => (
                <option key={ms} value={ms}>
                  {t(refreshLabelKey(ms))}
                </option>
              ))}
            </select>
          </label>
          <label className="debug-check">
            <input
              type="checkbox"
              checked={showProcessTable}
              onChange={(e) => setShowProcessTable(e.target.checked)}
            />
            <span>{t('debug.showProcesses')}</span>
          </label>
        </div>
        <div className="debug-toolbar-row debug-toolbar-row--wrap">
          <button type="button" className="debug-btn debug-btn--ghost" onClick={() => void onCopySnapshot()}>
            {t('debug.copySnapshot')}
          </button>
          <button type="button" className="debug-btn debug-btn--ghost" onClick={() => void onOpenFolder('userData')}>
            {t('debug.openUserData')}
          </button>
          <button type="button" className="debug-btn debug-btn--ghost" onClick={() => void onOpenFolder('instanceRoot')}>
            {t('debug.openInstance')}
          </button>
          <button
            type="button"
            className="debug-btn debug-btn--ghost"
            onClick={() => void window.solea.openGameLogWindow()}
          >
            {t('debug.openLogWindow')}
          </button>
        </div>
        {lastPullAt ? (
          <p className="debug-meta">
            {t('debug.lastUpdated', { time: formatDate(lastPullAt, { timeStyle: 'medium', dateStyle: 'short' }) })}
          </p>
        ) : null}
        {copyFeedback ? (
          <p className="debug-inline-ok" role="status">
            {copyFeedback}
          </p>
        ) : null}
        {folderErr ? (
          <p className="debug-inline-err" role="alert">
            {folderErr}
          </p>
        ) : null}
      </section>

      {reloadFeedback ? (
        <p
          className={reloadFeedback.ok ? 'debug-banner debug-banner--ok' : 'debug-err'}
          role="status"
        >
          {reloadFeedback.text}
        </p>
      ) : null}
      {snapErr ? <p className="debug-err debug-err--flush" role="alert">{snapErr}</p> : null}

      <div className="debug-body">
        <div className="debug-grid">
          <section className="debug-card debug-card--accent">
            <h2 className="debug-card-title">{t('debug.panelFps')}</h2>
            <div className="debug-fps">{fps}</div>
            <p className="debug-note">{t('debug.fpsNote')}</p>
          </section>

          <section className="debug-card">
            <h2 className="debug-card-title">{t('debug.panelRendererMem')}</h2>
            {perfMem ? (
              <dl className="debug-kv">
                <dt>{t('debug.heapUsed')}</dt>
                <dd>{formatBytes(perfMem.usedJSHeapSize)}</dd>
                <dt>{t('debug.heapTotal')}</dt>
                <dd>{formatBytes(perfMem.totalJSHeapSize)}</dd>
                <dt>{t('debug.heapLimit')}</dt>
                <dd>{formatBytes(perfMem.jsHeapSizeLimit)}</dd>
              </dl>
            ) : (
              <p className="debug-note">{t('debug.noHeapInfo')}</p>
            )}
          </section>

          <section className="debug-card">
            <h2 className="debug-card-title">{t('debug.panelMainProcess')}</h2>
            {snap && pm ? (
              <dl className="debug-kv">
                <dt>{t('debug.mainRss')}</dt>
                <dd>{formatBytes(pm.rss)}</dd>
                <dt>{t('debug.mainHeap')}</dt>
                <dd>
                  {formatBytes(pm.heapUsed)} / {formatBytes(pm.heapTotal)}
                </dd>
                <dt>{t('debug.mainExternal')}</dt>
                <dd>{formatBytes(pm.external)}</dd>
                <dt>{t('debug.mainArrayBuffers')}</dt>
                <dd>{formatBytes(pm.arrayBuffers)}</dd>
                <dt>{t('debug.cpuIntegral')}</dt>
                <dd>
                  {t('debug.cpuUser')} {formatInteger(snap.mainCpu.user)} μs · {t('debug.cpuSystem')}{' '}
                  {formatInteger(snap.mainCpu.system)} μs
                </dd>
              </dl>
            ) : (
              <p className="debug-note">—</p>
            )}
          </section>

          <section className="debug-card">
            <h2 className="debug-card-title">{t('debug.panelSystem')}</h2>
            {snap ? (
              <dl className="debug-kv">
                <dt>{t('debug.os')}</dt>
                <dd>
                  {snap.platform} ({snap.arch})
                </dd>
                <dt>{t('debug.machineRam')}</dt>
                <dd>
                  {t('debug.ramFreeOf', {
                    free: formatBytes(snap.freeMemBytes),
                    total: formatBytes(snap.totalMemBytes)
                  })}
                </dd>
                <dt>{t('debug.uptime')}</dt>
                <dd>
                  {formatUptimeSec(snap.uptimeSec, formatInteger)} · {snap.hostname}
                </dd>
                <dt>{t('debug.stackVersions')}</dt>
                <dd>
                  {snap.electronVersion} / {snap.chromeVersion} / {snap.nodeVersion}
                </dd>
                <dt>{t('debug.appVersion')}</dt>
                <dd>{snap.appVersion}</dd>
                <dt>{t('debug.testMode')}</dt>
                <dd>{snap.testMode ? t('debug.yes') : t('debug.no')}</dd>
              </dl>
            ) : null}
          </section>

          <section className="debug-card debug-card--wide">
            <h2 className="debug-card-title">{t('debug.panelLauncher')}</h2>
            {snap ? (
              <dl className="debug-kv">
                <dt>{t('debug.activePack')}</dt>
                <dd>{snap.activeModpackId}</dd>
                <dt>{t('debug.minecraft')}</dt>
                <dd>{snap.gameRunning ? t('debug.gameYes') : t('debug.gameNo')}</dd>
                <dt>{t('debug.pathUserData')}</dt>
                <dd>{snap.userData}</dd>
                <dt>{t('debug.pathInstance')}</dt>
                <dd>{snap.instanceRoot}</dd>
              </dl>
            ) : null}
            <p className="debug-note debug-note--hint">{t('debug.pathOpenHint')}</p>
          </section>
        </div>

        {snap ? (
          <div className="debug-load-card">
            <h2 className="debug-card-title">{t('debug.loadAvg')}</h2>
            {loadAvgActive ? (
              <p className="debug-load-text">
                {t('debug.loadAvgNix', {
                  a: snap.loadAvg[0].toFixed(2),
                  b: snap.loadAvg[1].toFixed(2),
                  c: snap.loadAvg[2].toFixed(2)
                })}
              </p>
            ) : (
              <p className="debug-note">{t('debug.loadAvgWin')}</p>
            )}
          </div>
        ) : null}

        {snap && showProcessTable && snap.appMetrics.length > 0 ? (
          <section className="debug-processes">
            <h2 className="debug-card-title">{t('debug.processesTitle')}</h2>
            <p className="debug-note debug-note--below-title">{t('debug.processesHelp')}</p>
            <div className="debug-table-wrap">
              <table className="debug-table">
                <thead>
                  <tr>
                    <th>{t('debug.colPid')}</th>
                    <th>{t('debug.colType')}</th>
                    <th>{t('debug.colName')}</th>
                    <th>{t('debug.colCpu')}</th>
                    <th>{t('debug.colWs')}</th>
                    <th>{t('debug.colPeakWs')}</th>
                  </tr>
                </thead>
                <tbody>
                  {snap.appMetrics.map((m) => (
                    <tr key={`${m.pid}-${m.type}`}>
                      <td>{m.pid}</td>
                      <td>{m.type}</td>
                      <td>{m.name ?? '—'}</td>
                      <td>{m.cpu.percentCPUUsage.toFixed(1)}</td>
                      <td>{formatInteger(m.memory.workingSetSize)}</td>
                      <td>{formatInteger(m.memory.peakWorkingSetSize)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        ) : null}
      </div>
    </div>
  )
}
