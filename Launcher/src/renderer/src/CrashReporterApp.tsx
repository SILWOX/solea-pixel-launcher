/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useI18n } from './i18n/I18nContext'
import { applyAppearanceSettings } from './appearance'
import type { CrashPayloadUi } from './crashTypes'
import './App.css'
import './crashReporter.css'

const DISCORD_INVITE_URL = 'https://discord.gg/jVGq5aZ6Wc'

const FAKE_DEMO_PAYLOAD: CrashPayloadUi = {
  source: 'renderer',
  message: 'TypeError: Cannot read properties of undefined (reading \'demo\')',
  stack:
    'TypeError: Cannot read properties of undefined (reading \'demo\')\n' +
    '    at FakeCrashDemo (crash-demo.ts:42:15)\n' +
    '    at Object.invoke (electron-ipc.ts:18:9)',
  reason: 'crashed',
  exitCode: -1,
  appVersion: '26.4.1',
  platform: 'win32',
  arch: 'x64',
  electronVersion: '33.x',
  occurredAt: new Date().toISOString(),
  isFakeDemo: true
}

function buildDiscordCrashBody(payload: CrashPayloadUi): string {
  const lines = [
    '**🔴 Launcher crash**',
    `**Source:** ${payload.source}`,
    `**When:** ${payload.occurredAt}`,
    `**Version:** ${payload.appVersion} · Electron ${payload.electronVersion}`,
    `**Platform:** ${payload.platform} (${payload.arch})`,
    `**Message:** ${payload.message.slice(0, 800)}`
  ]
  if (payload.reason) lines.push(`**reason:** ${payload.reason}`)
  if (payload.exitCode != null) lines.push(`**exitCode:** ${String(payload.exitCode)}`)
  if (payload.stack) {
    lines.push('', '```', payload.stack.slice(0, 1200), '```')
  }
  return lines.join('\n').slice(0, 1900)
}

export function CrashReporterApp() {
  const { t, setLocale } = useI18n()
  const [payload, setPayload] = useState<CrashPayloadUi | null>(null)
  const [sendState, setSendState] = useState<null | 'sending' | 'ok' | 'err'>(null)
  const [sendDetail, setSendDetail] = useState<string | null>(null)
  const [demoHint, setDemoHint] = useState<string | null>(null)
  const [payloadWaitTimedOut, setPayloadWaitTimedOut] = useState(false)

  useEffect(() => {
    document.title = t('crashReporter.windowTitle')
    void window.solea.getSettings().then((s) => {
      setLocale(s.uiLanguage)
      applyAppearanceSettings(s)
    })
  }, [t, setLocale])

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    if (params.get('fake') === '1') {
      setPayload(FAKE_DEMO_PAYLOAD)
      return
    }
    const remove = window.solea.onCrashPayload((p) => {
      setPayload(p)
      setPayloadWaitTimedOut(false)
    })
    const tmr = window.setTimeout(() => setPayloadWaitTimedOut(true), 8000)
    return () => {
      remove()
      window.clearTimeout(tmr)
    }
  }, [])

  const reportBody = useMemo(() => (payload ? buildDiscordCrashBody(payload) : ''), [payload])

  const onSendReport = useCallback(async () => {
    if (!payload) return
    setSendState('sending')
    setSendDetail(null)
    const r = await window.solea.submitReportDiscordWebhook(reportBody)
    if (r.ok) {
      setSendState('ok')
      setSendDetail(t('crashReporter.sentOk'))
    } else if (r.error === 'no_webhook_env') {
      setSendState('err')
      setSendDetail(t('crashReporter.noWebhook'))
    } else {
      setSendState('err')
      setSendDetail(r.error)
    }
  }, [payload, reportBody, t])

  const onRelaunch = async () => {
    if (!payload) return
    if (payload.isFakeDemo) {
      setDemoHint(t('crashReporter.demoRelaunch'))
      return
    }
    await window.solea.appRelaunch()
  }

  const onQuitLauncher = async () => {
    if (payload?.isFakeDemo) {
      setDemoHint(t('crashReporter.demoQuit'))
      return
    }
    await window.solea.appQuit()
  }

  const onCopy = async () => {
    if (!reportBody) return
    try {
      await navigator.clipboard.writeText(reportBody)
      setDemoHint(t('crashReporter.copied'))
      window.setTimeout(() => setDemoHint(null), 2500)
    } catch {
      setDemoHint(t('crashReporter.copyFail'))
      window.setTimeout(() => setDemoHint(null), 2500)
    }
  }

  return (
    <div className="crash-reporter-root">
      <header className="titlebar crash-reporter-titlebar">
        <div className="titlebar-drag" role="presentation">
          <span className="titlebar-title crash-reporter-titlebar-text">{t('crashReporter.titlebar')}</span>
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

      <div className="crash-reporter-scroll">
        {!payload && !payloadWaitTimedOut ? (
          <div className="crash-reporter-card crash-reporter-card--wait">
            <p className="crash-reporter-wait">{t('crashReporter.loadingPayload')}</p>
          </div>
        ) : null}
        {!payload && payloadWaitTimedOut ? (
          <div className="crash-reporter-card crash-reporter-card--wait">
            <p className="crash-reporter-wait">{t('crashReporter.payloadTimeout')}</p>
            <div className="crash-reporter-actions crash-reporter-actions--secondary" style={{ marginTop: '1rem' }}>
              <button type="button" className="btn-muted" onClick={() => void window.solea.openUserDataFolder()}>
                {t('crashReporter.openUserData')}
              </button>
              <button type="button" className="btn-save" onClick={() => void onQuitLauncher()}>
                {t('crashReporter.quit')}
              </button>
            </div>
          </div>
        ) : null}
        {payload ? (
          <>
            {payload.isFakeDemo ? (
              <p className="crash-reporter-demo-strip" role="status">
                {t('crashReporter.demoBanner')}
              </p>
            ) : null}

            <div className="crash-reporter-card">
              <p className="crash-reporter-eyebrow">{t('crashReporter.eyebrow')}</p>
              <h1 className="crash-reporter-heading font-mc">{t('crashReporter.heading')}</h1>
              <p className="crash-reporter-lead">{t('crashReporter.lead')}</p>

              <dl className="crash-reporter-kv">
                <dt>{t('crashReporter.fieldSource')}</dt>
                <dd>{payload.source}</dd>
                <dt>{t('crashReporter.fieldWhen')}</dt>
                <dd>{payload.occurredAt}</dd>
                <dt>{t('crashReporter.fieldVersion')}</dt>
                <dd>
                  {payload.appVersion} · Electron {payload.electronVersion}
                </dd>
                <dt>{t('crashReporter.fieldPlatform')}</dt>
                <dd>
                  {payload.platform} ({payload.arch})
                </dd>
                {payload.reason ? (
                  <>
                    <dt>{t('crashReporter.fieldReason')}</dt>
                    <dd>{payload.reason}</dd>
                  </>
                ) : null}
                {payload.exitCode != null ? (
                  <>
                    <dt>{t('crashReporter.fieldExit')}</dt>
                    <dd>{String(payload.exitCode)}</dd>
                  </>
                ) : null}
                <dt>{t('crashReporter.fieldMessage')}</dt>
                <dd className="crash-reporter-message">{payload.message}</dd>
              </dl>

              {payload.stack ? (
                <details className="crash-reporter-stack">
                  <summary>{t('crashReporter.stackToggle')}</summary>
                  <pre className="crash-reporter-pre">{payload.stack}</pre>
                </details>
              ) : null}

              {sendState === 'ok' ? (
                <p className="crash-reporter-feedback crash-reporter-feedback--ok">{sendDetail}</p>
              ) : null}
              {sendState === 'err' ? (
                <p className="crash-reporter-feedback crash-reporter-feedback--err">{sendDetail}</p>
              ) : null}
              {demoHint ? (
                <p className="crash-reporter-feedback crash-reporter-feedback--info">{demoHint}</p>
              ) : null}

              <div className="crash-reporter-actions">
                <button
                  type="button"
                  className="btn-save"
                  disabled={sendState === 'sending'}
                  onClick={() => void onSendReport()}
                >
                  {sendState === 'sending' ? t('crashReporter.sending') : t('crashReporter.sendReport')}
                </button>
                <button type="button" className="btn-muted" onClick={() => void onCopy()}>
                  {t('crashReporter.copyDetails')}
                </button>
              </div>

              <div className="crash-reporter-actions crash-reporter-actions--secondary">
                <button type="button" className="btn-save" onClick={() => void onRelaunch()}>
                  {t('crashReporter.relaunch')}
                </button>
                <button
                  type="button"
                  className="btn-muted"
                  onClick={() => void window.solea.openExternalUrl(DISCORD_INVITE_URL)}
                >
                  {t('crashReporter.joinDiscord')}
                </button>
                <button type="button" className="btn-muted" onClick={() => void window.solea.openUserDataFolder()}>
                  {t('crashReporter.openUserData')}
                </button>
                <button type="button" className="btn-muted crash-reporter-quit" onClick={() => void onQuitLauncher()}>
                  {t('crashReporter.quit')}
                </button>
              </div>
            </div>
          </>
        ) : null}
      </div>
    </div>
  )
}
