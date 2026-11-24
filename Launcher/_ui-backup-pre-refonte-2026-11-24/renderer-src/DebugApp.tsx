/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { useCallback, useEffect, useRef, useState } from 'react'
import { useFocusTrap } from './a11y/useFocusTrap'
import { applyAppearanceSettings } from './appearance'
import { useI18n } from './i18n/I18nContext'
import type { UiLanguage } from './launcherTypes'
import type { DebugSnapshotUi } from './debugTypes'
import { HomeTagPill } from './HomeTagPill'
import { ActuSyntaxDebugSection } from './actus/ActuSyntaxDebugSection'
import './actus/actus.css'
import { HOME_TAG_PRESET_IDS } from './homeTagPresets'
import { useToast } from './ui/ToastContext'
import './App.css'
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

function DebugTitleBar() {
  const { t } = useI18n()
  const [maximized, setMaximized] = useState(false)

  useEffect(() => {
    void window.solea.windowIsMaximized().then(setMaximized)
    return window.solea.onWindowMaximized(setMaximized)
  }, [])

  const toggleMax = async () => {
    const r = await window.solea.windowToggleMaximize()
    setMaximized(r.maximized)
  }

  return (
    <header className="titlebar debug-titlebar">
      <div
        className="titlebar-drag"
        role="presentation"
        onDoubleClick={() => void toggleMax()}
      >
        <span className="titlebar-title debug-titlebar-product">{t('debug.chromeTitle')}</span>
        <span className="debug-titlebar-badge">{t('debug.chromeBadge')}</span>
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
  )
}

type FakeModalKind = 'updates' | 'cache' | 'uninstall' | 'loginInfo' | 'reinstall' | null

function DebugFakeModals({ kind, onClose }: { kind: FakeModalKind; onClose: () => void }) {
  const { t } = useI18n()
  const dialogRef = useRef<HTMLDivElement>(null)
  const open = kind !== null
  useFocusTrap(open, dialogRef, { onEscape: onClose })

  if (!kind) return null

  const modalClass =
    kind === 'updates'
      ? 'pack-confirm-modal pack-confirm-modal--updates'
      : kind === 'cache'
        ? 'pack-confirm-modal pack-confirm-modal--cache'
        : kind === 'uninstall'
          ? 'pack-confirm-modal pack-confirm-modal--uninstall'
          : kind === 'loginInfo'
            ? 'pack-confirm-modal pack-confirm-modal--login-info'
            : 'pack-confirm-modal pack-confirm-modal--reinstall'

  return (
    <div className="pack-confirm-backdrop" role="presentation" onClick={onClose}>
      <div
        ref={dialogRef}
        className={modalClass}
        role="alertdialog"
        aria-modal="true"
        onClick={(e) => e.stopPropagation()}
      >
        {kind === 'updates' ? (
          <>
            <p className="pack-confirm-eyebrow">{t('home.updatesModalEyebrow')}</p>
            <h2 className="pack-confirm-title">{t('home.updatesModalTitle')}</h2>
            <p id="debug-fake-updates-desc" className="pack-confirm-body">
              {t('home.updatesModalLead')}
            </p>
            <ul className="modpack-updates-modal-list" aria-describedby="debug-fake-updates-desc">
              <li>
                {t('home.updatesModalLine', {
                  name: 'Palamod Recreated',
                  installed: '1.2.0',
                  latest: '1.3.1'
                })}
              </li>
              <li>
                {t('home.updatesModalLine', {
                  name: 'Wither Storm',
                  installed: '2.0.0',
                  latest: '2.0.0'
                })}
              </li>
              <li>
                {t('home.updatesModalLine', {
                  name: 'MYTHIC TRIALS 1',
                  installed: '1.0.0',
                  latest: '1.0.0'
                })}
              </li>
              <li>
                {t('home.updatesModalLine', {
                  name: 'MYTHIC TRIALS 2',
                  installed: '1.0.0',
                  latest: '1.0.0'
                })}
              </li>
            </ul>
            <div className="pack-confirm-actions">
              <button type="button" className="btn-save pack-confirm-btn-primary" onClick={onClose}>
                {t('home.updatesModalOk')}
              </button>
            </div>
          </>
        ) : null}

        {kind === 'cache' ? (
          <>
            <p className="pack-confirm-eyebrow">{t('settings.cacheMaintenance')}</p>
            <h2 className="pack-confirm-title">{t('confirm.packSureQuestion')}</h2>
            <p className="pack-confirm-body">{t('settings.cacheClearLauncherConfirm')}</p>
            <div className="pack-confirm-actions">
              <button type="button" className="btn-muted pack-confirm-btn-cancel" onClick={onClose}>
                {t('confirm.packCancel')}
              </button>
              <button type="button" className="btn-save pack-confirm-btn-primary" onClick={onClose}>
                {t('settings.cacheClearLauncher')}
              </button>
            </div>
          </>
        ) : null}

        {kind === 'uninstall' ? (
          <>
            <p className="pack-confirm-eyebrow">{t('confirm.packDialogEyebrow')}</p>
            <h2 className="pack-confirm-title">{t('confirm.packSureQuestion')}</h2>
            <p className="pack-confirm-body">{t('confirm.packUninstallDetail')}</p>
            <div className="pack-confirm-actions">
              <button type="button" className="btn-muted pack-confirm-btn-cancel" onClick={onClose}>
                {t('confirm.packCancel')}
              </button>
              <button type="button" className="btn-save pack-confirm-btn-danger" onClick={onClose}>
                {t('confirm.packConfirmUninstall')}
              </button>
            </div>
          </>
        ) : null}

        {kind === 'loginInfo' ? (
          <>
            <p className="pack-confirm-eyebrow">{t('login.infoEyebrow')}</p>
            <h2 className="pack-confirm-title">{t('login.infoTitle')}</h2>
            <div className="login-info-modal-scroll">
              <p className="login-info-modal-lead">{t('debug.fakeLoginShortLead')}</p>
              <ul className="login-info-modal-points">
                <li>
                  <strong className="login-info-point-strong">{t('login.infoBullet1Strong')}</strong>
                  <span className="login-info-point-text">{t('login.infoBullet1')}</span>
                </li>
              </ul>
            </div>
            <div className="pack-confirm-actions pack-confirm-actions--login-info">
              <button type="button" className="btn-muted login-info-btn-close" onClick={onClose}>
                {t('login.infoClose')}
              </button>
            </div>
          </>
        ) : null}

        {kind === 'reinstall' ? (
          <>
            <p className="pack-confirm-eyebrow">{t('confirm.packDialogEyebrow')}</p>
            <h2 className="pack-confirm-title">{t('confirm.packSureQuestion')}</h2>
            <p className="pack-confirm-body">{t('confirm.packReinstallDetail')}</p>
            <div className="pack-reinstall-preserve" role="group" aria-label={t('confirm.packReinstallPreserveGroup')}>
              <p className="pack-reinstall-preserve-lead">{t('confirm.packReinstallPreserveLead')}</p>
              <label className="pack-reinstall-preserve-row">
                <input type="checkbox" defaultChecked />
                <span>{t('confirm.packReinstallKeepSaves')}</span>
              </label>
              <label className="pack-reinstall-preserve-row">
                <input type="checkbox" defaultChecked />
                <span>{t('confirm.packReinstallKeepScreenshots')}</span>
              </label>
              <label className="pack-reinstall-preserve-row">
                <input type="checkbox" defaultChecked />
                <span>{t('confirm.packReinstallKeepOptions')}</span>
              </label>
            </div>
            <div className="pack-confirm-actions">
              <button type="button" className="btn-muted pack-confirm-btn-cancel" onClick={onClose}>
                {t('confirm.packCancel')}
              </button>
              <button type="button" className="btn-save pack-confirm-btn-primary" onClick={onClose}>
                {t('confirm.packConfirmReinstall')}
              </button>
            </div>
          </>
        ) : null}
      </div>
    </div>
  )
}

export function DebugApp() {
  const { t, locale, setLocale, formatInteger, formatDate } = useI18n()
  const { pushToast } = useToast()
  const [fakeModal, setFakeModal] = useState<FakeModalKind>(null)
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
    void window.solea.getSettings().then((s) => {
      setLocale(s.uiLanguage)
      applyAppearanceSettings(s)
    })
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

  const setLocalePersist = useCallback(
    async (l: UiLanguage) => {
      setLocale(l)
      await window.solea.saveSettings({ uiLanguage: l })
    },
    [setLocale]
  )

  const pm = snap?.processMemory
  const loadAvgActive = snap && snap.loadAvg.some((x) => x > 0)

  return (
    <div className="debug-app-shell">
      <DebugTitleBar />

      <div className="debug-layout">
        <header className="debug-header">
          <div className="debug-header-text">
            <p className="debug-eyebrow">{t('debug.pageEyebrow')}</p>
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
          </div>
        </header>

        <section className="debug-home-tags-panel" aria-labelledby="debug-home-tags-heading">
          <h2 id="debug-home-tags-heading" className="debug-toolbar-title">
            {t('debug.homeTagsTitle')}
          </h2>
          <p className="debug-home-tags-lead">{t('debug.homeTagsLead')}</p>
          <div className="debug-home-tags-preview" role="list">
            {HOME_TAG_PRESET_IDS.map((id) => (
              <HomeTagPill
                key={id}
                presetId={id}
                label={t(`home.tagPreset.${id}.label`)}
                description={t(`home.tagPreset.${id}.desc`)}
              />
            ))}
          </div>
        </section>

        <ActuSyntaxDebugSection />

      <section className="debug-toolbar" aria-labelledby="debug-tools-heading">
        <h2 id="debug-tools-heading" className="debug-toolbar-title">
          {t('debug.toolsTitle')}
        </h2>
        <div className="debug-toolbar-row debug-toolbar-row--lang">
          <div className="debug-lang">
            <span className="debug-field-label" id="debug-lang-label">
              {t('debug.language')}
            </span>
            <div
              className="debug-lang-toggle"
              role="group"
              aria-labelledby="debug-lang-label"
            >
              <button
                type="button"
                className={`debug-lang-btn${locale === 'en' ? ' debug-lang-btn--active' : ''}`}
                aria-pressed={locale === 'en'}
                onClick={() => void setLocalePersist('en')}
              >
                {t('settings.langEn')}
              </button>
              <button
                type="button"
                className={`debug-lang-btn${locale === 'fr' ? ' debug-lang-btn--active' : ''}`}
                aria-pressed={locale === 'fr'}
                onClick={() => void setLocalePersist('fr')}
              >
                {t('settings.langFr')}
              </button>
            </div>
          </div>
        </div>
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

        <div className="debug-fake-popups" aria-labelledby="debug-fake-heading">
          <h2 id="debug-fake-heading" className="debug-toolbar-title debug-toolbar-title--spaced">
            {t('debug.fakePopupsTitle')}
          </h2>
          <p className="debug-fake-hint">{t('debug.fakePopupsHint')}</p>
          <div className="debug-fake-row">
            <span className="debug-fake-row-label">{t('debug.fakeRowToasts')}</span>
            <div className="debug-fake-grid">
              <button
                type="button"
                className="debug-btn debug-btn--ghost debug-btn--compact"
                onClick={() => pushToast(t('debug.fakeToastSampleInfo'), 'info')}
              >
                {t('debug.fakeToastInfo')}
              </button>
              <button
                type="button"
                className="debug-btn debug-btn--ghost debug-btn--compact"
                onClick={() => pushToast(t('debug.fakeToastSampleSuccess'), 'success')}
              >
                {t('debug.fakeToastSuccess')}
              </button>
              <button
                type="button"
                className="debug-btn debug-btn--ghost debug-btn--compact"
                onClick={() => pushToast(t('debug.fakeToastSampleError'), 'error')}
              >
                {t('debug.fakeToastError')}
              </button>
              <button
                type="button"
                className="debug-btn debug-btn--ghost debug-btn--compact"
                onClick={() => pushToast(t('debug.fakeToastErrorLongBody'), 'error', 12_000)}
              >
                {t('debug.fakeToastErrorLong')}
              </button>
              <button
                type="button"
                className="debug-btn debug-btn--ghost debug-btn--compact"
                onClick={() =>
                  pushToast(t('updater.available', { version: '9.9.9' }), 'info', 18_000, {
                    label: t('updater.toastDownloadNow'),
                    onClick: () => {}
                  })
                }
              >
                {t('debug.fakeToastUpdaterAvail')}
              </button>
              <button
                type="button"
                className="debug-btn debug-btn--ghost debug-btn--compact"
                onClick={() =>
                  pushToast(t('updater.downloaded'), 'success', 14_000, {
                    label: t('updater.restartNow'),
                    onClick: () => {}
                  })
                }
              >
                {t('debug.fakeToastUpdaterDone')}
              </button>
              <button
                type="button"
                className="debug-btn debug-btn--ghost debug-btn--compact"
                onClick={() => pushToast(t('updater.checking'), 'info', 2800)}
              >
                {t('debug.fakeToastChecking')}
              </button>
            </div>
          </div>
          <div className="debug-fake-row">
            <span className="debug-fake-row-label">{t('debug.fakeRowDialogs')}</span>
            <div className="debug-fake-grid">
              <button
                type="button"
                className="debug-btn debug-btn--ghost debug-btn--compact"
                onClick={() => setFakeModal('updates')}
              >
                {t('debug.fakeModalUpdates')}
              </button>
              <button
                type="button"
                className="debug-btn debug-btn--ghost debug-btn--compact"
                onClick={() => setFakeModal('cache')}
              >
                {t('debug.fakeModalCache')}
              </button>
              <button
                type="button"
                className="debug-btn debug-btn--ghost debug-btn--compact"
                onClick={() => setFakeModal('uninstall')}
              >
                {t('debug.fakeModalUninstall')}
              </button>
              <button
                type="button"
                className="debug-btn debug-btn--ghost debug-btn--compact"
                onClick={() => setFakeModal('reinstall')}
              >
                {t('debug.fakeModalReinstall')}
              </button>
              <button
                type="button"
                className="debug-btn debug-btn--ghost debug-btn--compact"
                onClick={() => setFakeModal('loginInfo')}
              >
                {t('debug.fakeModalLogin')}
              </button>
            </div>
          </div>
          <div className="debug-fake-row">
            <span className="debug-fake-row-label">{t('debug.fakeRowMainHomeTransfer')}</span>
            <p className="debug-fake-hint">{t('debug.fakeRowMainHomeTransferHint')}</p>
            <div className="debug-fake-grid">
              <button
                type="button"
                className="debug-btn debug-btn--ghost debug-btn--compact"
                onClick={() =>
                  void window.solea.debugRequestFakeInstall('install').then((r) => {
                    if (!r.ok) pushToast(r.error, 'error', 6000)
                  })
                }
              >
                {t('debug.fakeHomeInstall')}
              </button>
              <button
                type="button"
                className="debug-btn debug-btn--ghost debug-btn--compact"
                onClick={() =>
                  void window.solea.debugRequestFakeInstall('update').then((r) => {
                    if (!r.ok) pushToast(r.error, 'error', 6000)
                  })
                }
              >
                {t('debug.fakeHomeUpdate')}
              </button>
            </div>
          </div>
          <div className="debug-fake-row">
            <span className="debug-fake-row-label">{t('debug.fakeRowLauncherWindows')}</span>
            <div className="debug-fake-grid">
              <button
                type="button"
                className="debug-btn debug-btn--ghost debug-btn--compact"
                onClick={() =>
                  void (async () => {
                    try {
                      await window.solea.debugShowAlreadyRunningOverlay()
                    } catch (e) {
                      pushToast(
                        `${t('debug.fakeAlreadyRunning')}: ${e instanceof Error ? e.message : String(e)}`,
                        'error',
                        10_000
                      )
                    }
                  })()
                }
              >
                {t('debug.fakeAlreadyRunning')}
              </button>
              <button
                type="button"
                className="debug-btn debug-btn--ghost debug-btn--compact"
                onClick={() =>
                  void (async () => {
                    try {
                      await window.solea.debugOpenFakeCrashWindow()
                    } catch (e) {
                      pushToast(
                        `${t('debug.fakeCrashReporter')}: ${e instanceof Error ? e.message : String(e)}`,
                        'error',
                        10_000
                      )
                    }
                  })()
                }
              >
                {t('debug.fakeCrashReporter')}
              </button>
            </div>
          </div>
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

      <DebugFakeModals kind={fakeModal} onClose={() => setFakeModal(null)} />
      </div>
    </div>
  )
}
