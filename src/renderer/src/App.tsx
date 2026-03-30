import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { LauncherSettingsUI, ModpackGameProfileUI, SkinViewerAnimation } from './launcherTypes'
import logoUrl from './assets/branding/logo.png?url'
import loginWallpaperUrl from './assets/branding/login-wallpaper.png?url'
import soleaWordmarkUrl from './assets/branding/solea-pixel-wordmark.png?url'
import bootLogoUrl from './assets/branding/boot-logo.png?url'
import './App.css'
import { MODPACK_THEME, isModpackId, type ModpackIdUi } from './modpackTheme'
import { AccountSkinViewer, type AccountSkinViewerHandle } from './AccountSkinViewer'
import { AccountCapeModal } from './AccountCapeModal'
import { applyAppearanceSettings, subscribeSystemTheme } from './appearance'
import { useI18n } from './i18n/I18nContext'
import { LauncherSelect } from './ui/LauncherSelect'
import { useToast } from './ui/ToastContext'
import { playUiSound, type UiSoundPrefs } from './ui/playUiSound'
import { useFocusTrap } from './a11y/useFocusTrap'

const LOGO = logoUrl
/** Fond dédié à l’écran Microsoft (distinct du fond Palamod sur l’accueil). */
const LOGIN_WALLPAPER = loginWallpaperUrl
const SOLEA_WORDMARK = soleaWordmarkUrl

/** Titre sur deux lignes (maquette) : coupe équilibrée pour les noms longs. */
function packTitleLines(displayName: string): { first: string; second: string | null } {
  const w = displayName.trim().split(/\s+/).filter(Boolean)
  if (w.length === 0) return { first: displayName, second: null }
  if (w.length === 1) return { first: w[0]!, second: null }
  if (w.length === 2) return { first: w[0]!, second: w[1]! }
  const mid = Math.ceil(w.length / 2)
  return { first: w.slice(0, mid).join(' '), second: w.slice(mid).join(' ') }
}

/** Tête joueur : data URL via le processus principal (les <img https://…> échouent souvent dans Electron). */

function SkinHead({
  uuid,
  sizePx,
  className
}: {
  uuid: string | undefined | null
  sizePx: number
  className?: string
}) {
  const [src, setSrc] = useState<string>(LOGO)

  useEffect(() => {
    if (!uuid?.trim()) {
      setSrc(LOGO)
      return
    }
    let cancelled = false
    void window.solea.getSkinHead(uuid.trim(), sizePx).then((dataUrl) => {
      if (cancelled) return
      setSrc(dataUrl ?? LOGO)
    })
    return () => {
      cancelled = true
    }
  }, [uuid, sizePx])

  return <img src={src} alt="" className={className} />
}

type SkinPresetsStateUi = {
  activePresetId: string | null
  presets: {
    id: string
    name: string
    model: 'slim' | 'default'
    thumbDataUrl: string
  }[]
}

function SkinAccountPreview({
  uuid,
  refreshKey,
  playerName,
  viewerBackground,
  skinAnim,
  reduceMotion,
  onRefresh
}: {
  uuid: string
  refreshKey: number
  playerName: string
  viewerBackground: string
  skinAnim: SkinViewerAnimation
  reduceMotion: boolean
  onRefresh: () => void
}) {
  const { t } = useI18n()
  const { pushToast } = useToast()
  const viewerRef = useRef<AccountSkinViewerHandle>(null)
  const importDialogRef = useRef<HTMLDivElement>(null)
  const importPresetNameRef = useRef<HTMLInputElement>(null)
  const [importOpen, setImportOpen] = useState(false)

  useFocusTrap(importOpen, importDialogRef, {
    initialFocusRef: importPresetNameRef,
    onEscape: () => setImportOpen(false)
  })
  const [preview, setPreview] = useState<{
    source: 'local' | 'remote'
    dataUrl: string
    model: 'slim' | 'default' | 'auto-detect'
    capeDataUrl: string | null
  } | null>(null)
  const [presetsState, setPresetsState] = useState<SkinPresetsStateUi | null>(null)
  const [capeModalOpen, setCapeModalOpen] = useState(false)
  const [importModel, setImportModel] = useState<'default' | 'slim'>('default')
  const [importName, setImportName] = useState('')
  const [loadingPreview, setLoadingPreview] = useState(true)

  useEffect(() => {
    if (!uuid.trim()) {
      setPreview(null)
      setPresetsState(null)
      setLoadingPreview(false)
      return
    }
    setLoadingPreview(true)
    let cancelled = false
    void Promise.all([
      window.solea.getSkinPreview(uuid.trim()),
      window.solea.getSkinPresetsState(uuid.trim())
    ]).then(([p, s]) => {
      if (cancelled) return
      setPreview(p)
      setPresetsState(s)
      setLoadingPreview(false)
    })
    return () => {
      cancelled = true
    }
  }, [uuid, refreshKey])

  const activePreset =
    presetsState?.activePresetId &&
    presetsState.presets.find((pr) => pr.id === presetsState.activePresetId)

  const selectMojang = () => {
    void window.solea.setActiveSkinPreset(uuid, null).then((r) => {
      if (!r.ok) return
      onRefresh()
      if (r.skinSyncError) {
        pushToast(t('skins.syncMojang', { error: r.skinSyncError }), 'error')
      }
    })
  }

  const selectPreset = (presetId: string) => {
    void window.solea.setActiveSkinPreset(uuid, presetId).then((r) => {
      if (!r.ok) return
      onRefresh()
      if (r.skinSyncError) {
        pushToast(t('skins.syncUpload', { error: r.skinSyncError }), 'error')
      }
    })
  }

  const deletePresetTile = (presetId: string, label: string) => {
    if (!window.confirm(t('skins.deleteConfirm', { name: label }))) return
    void window.solea.deleteSkinPreset(uuid, presetId).then((r) => {
      if (!r.ok) return
      pushToast(t('skins.feedbackDeleted'), 'success')
      onRefresh()
      if (r.skinSyncError) {
        pushToast(t('skins.syncAfterDelete', { error: r.skinSyncError }), 'error')
      }
    })
  }

  const runImportPreset = () => {
    void window.solea.importSkinPreset(importModel, importName.trim()).then((r) => {
      setImportOpen(false)
      setImportName('')
      if (!r.ok) {
        if (r.error !== 'Annulé.') pushToast(r.error, 'error')
        return
      }
      pushToast(
        r.skinSyncError ? t('skins.syncImportFail', { error: r.skinSyncError }) : t('skins.syncImportOk'),
        r.skinSyncError ? 'error' : 'success',
        8000
      )
      onRefresh()
    })
  }

  const setActivePresetModel = (model: 'slim' | 'default') => {
    if (!presetsState?.activePresetId) return
    void window.solea
      .updateSkinPresetModel(uuid, presetsState.activePresetId, model)
      .then((r) => {
        if (!r.ok) {
          pushToast(r.error, 'error')
          return
        }
        onRefresh()
        if (r.skinSyncError) {
          pushToast(t('skins.syncModel', { error: r.skinSyncError }), 'error')
        }
      })
  }

  const exportSkinPng = () => {
    const dataUrl = viewerRef.current?.exportPng()
    if (!dataUrl) {
      pushToast(t('skins.exportPngFail'), 'error')
      return
    }
    const a = document.createElement('a')
    a.href = dataUrl
    a.download = `skin-${playerName.replace(/\s+/g, '_')}.png`
    a.click()
    pushToast(t('skins.exportPngOk'), 'success')
  }

  const mojangActive = presetsState ? presetsState.activePresetId === null : true

  return (
    <>
      <div className="account-skins-modrinth">
        <div className="account-viewer-column">
          <div className="account-viewer-inner">
            {loadingPreview ? (
              <div
                className={`account-viewer-skeleton ${reduceMotion ? 'account-viewer-skeleton--static' : ''}`}
                aria-hidden
              />
            ) : (
              <AccountSkinViewer
                ref={viewerRef}
                skinDataUrl={preview?.dataUrl ?? null}
                model={preview?.model ?? 'auto-detect'}
                capeDataUrl={preview?.capeDataUrl ?? null}
                playerName={playerName}
                viewerBackground={viewerBackground}
                animation={skinAnim}
                reduceMotion={reduceMotion}
              />
            )}
          </div>
          <p className="account-viewer-hint">{t('skins.hint')}</p>
          {!loadingPreview && (
            <button type="button" className="btn-muted account-export-png" onClick={() => exportSkinPng()}>
              {t('skins.exportPng')}
            </button>
          )}
          <p className="account-skin-caption account-skin-caption-under">
            {preview?.source === 'local'
              ? t('skins.captionLocal')
              : preview
                ? t('skins.captionRemote')
                : null}
          </p>
        </div>
        <div className="account-skins-sidebar">
          <div className="account-skins-sidebar-head">
            <h3 className="account-skins-heading">{t('skins.title')}</h3>
            <span className="account-skins-beta">{t('skins.beta')}</span>
          </div>
          <p className="account-skins-sub">{t('skins.sub')}</p>
          {loadingPreview ? (
            <div
              className={`account-skin-tiles account-skin-tiles-skeleton ${reduceMotion ? 'account-skin-tiles-skeleton--static' : ''}`}
              aria-busy="true"
              aria-label={t('skins.loadingPresets')}
            >
              <div className="account-skin-tile-skeleton" />
              <div className="account-skin-tile-skeleton" />
              <div className="account-skin-tile-skeleton" />
              <div className="account-skin-tile-skeleton" />
            </div>
          ) : (
            <div className="account-skin-tiles">
              <button
                type="button"
                className="account-skin-tile account-skin-tile-add"
                onClick={() => setImportOpen(true)}
              >
                <span className="account-skin-tile-plus" aria-hidden>
                  +
                </span>
                <span className="account-skin-tile-add-label">{t('skins.add')}</span>
              </button>
              <button
                type="button"
                className={`account-skin-tile account-skin-tile-thumb ${mojangActive ? 'account-skin-tile-active' : ''}`}
                onClick={() => selectMojang()}
                title={t('skins.mojangTitle')}
              >
                {preview && mojangActive ? (
                  <img src={preview.dataUrl} alt="" className="account-skin-tile-img" />
                ) : (
                  <span className="account-skin-tile-mojang-ico" aria-hidden>
                    ☁
                  </span>
                )}
                <span className="account-skin-tile-label">{t('skins.mojang')}</span>
              </button>
              {presetsState?.presets.map((pr) => (
                <div
                  key={pr.id}
                  className={`account-skin-tile-wrap ${presetsState.activePresetId === pr.id ? 'is-active' : ''}`}
                >
                  <button
                    type="button"
                    className={`account-skin-tile account-skin-tile-thumb ${
                      presetsState.activePresetId === pr.id ? 'account-skin-tile-active' : ''
                    }`}
                    onClick={() => selectPreset(pr.id)}
                    title={pr.name}
                  >
                    <img src={pr.thumbDataUrl} alt="" className="account-skin-tile-img" />
                    <span className="account-skin-tile-label">{pr.name}</span>
                  </button>
                  <button
                    type="button"
                    className="account-skin-tile-delete"
                    title={t('skins.deletePreset')}
                    aria-label={`${t('skins.deletePreset')} ${pr.name}`}
                    onClick={(e) => {
                      e.stopPropagation()
                      deletePresetTile(pr.id, pr.name)
                    }}
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          )}

          {presetsState && presetsState.activePresetId !== null ? (
            <div className="account-preset-model-toggle">
              <span className="account-preset-model-label">{t('skins.modelArms')}</span>
              <div className="account-segment">
                <button
                  type="button"
                  className={activePreset?.model === 'default' ? 'on' : ''}
                  onClick={() => setActivePresetModel('default')}
                >
                  {t('skins.steve')}
                </button>
                <button
                  type="button"
                  className={activePreset?.model === 'slim' ? 'on' : ''}
                  onClick={() => setActivePresetModel('slim')}
                >
                  {t('skins.alex')}
                </button>
              </div>
            </div>
          ) : null}

          <div className="account-cape-block">
            <h4 className="account-cape-title">{t('skins.cape')}</h4>
            <button type="button" className="btn-save account-cape-open-btn" onClick={() => setCapeModalOpen(true)}>
              {t('skins.changeCape')}
            </button>
          </div>
        </div>
      </div>

      <AccountCapeModal
        open={capeModalOpen}
        onClose={() => setCapeModalOpen(false)}
        playerName={playerName}
        skinDataUrl={preview?.dataUrl ?? null}
        skinModel={preview?.model ?? 'auto-detect'}
        viewerBackground={viewerBackground}
        skinAnim={skinAnim}
        reduceMotion={reduceMotion}
        onApplied={() => {
          pushToast(t('skins.feedbackCape'), 'success')
          onRefresh()
        }}
      />

      {importOpen ? (
        <div
          className="account-modal-backdrop"
          role="presentation"
          onClick={() => setImportOpen(false)}
        >
          <div
            ref={importDialogRef}
            className="account-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="account-import-title"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 id="account-import-title" className="account-modal-title">
              {t('skins.importTitle')}
            </h3>
            <p className="account-modal-lead">{t('skins.importLead')}</p>
            <label className="account-modal-field">
              <span>{t('skins.presetName')}</span>
              <input
                ref={importPresetNameRef}
                type="text"
                value={importName}
                onChange={(e) => setImportName(e.target.value)}
                placeholder={t('skins.presetPlaceholder')}
                maxLength={48}
              />
            </label>
            <div className="account-modal-model">
              <button
                type="button"
                className={importModel === 'default' ? 'active' : ''}
                onClick={() => setImportModel('default')}
              >
                {t('skins.steve')}
              </button>
              <button
                type="button"
                className={importModel === 'slim' ? 'active' : ''}
                onClick={() => setImportModel('slim')}
              >
                {t('skins.alex')}
              </button>
            </div>
            <p className="account-modal-hint">{t('skins.importHint')}</p>
            <div className="account-modal-actions">
              <button type="button" className="btn-muted" onClick={() => setImportOpen(false)}>
                {t('skins.cancel')}
              </button>
              <button type="button" className="btn-save" onClick={() => runImportPreset()}>
                {t('skins.chooseFile')}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  )
}

function emptyPackProfile(): ModpackGameProfileUI {
  return {
    memoryMin: '2G',
    memoryMax: '6G',
    gameArgs: '',
    screenWidth: null,
    screenHeight: null,
    fullscreen: false
  }
}

function emptySettings(): LauncherSettingsUI {
  const packs = emptyPackProfile()
  return {
    memoryMin: '2G',
    memoryMax: '6G',
    jvmArgs: '',
    gameArgs: '',
    downloadThreads: 8,
    networkTimeoutMs: 20000,
    javaPath: '',
    javaVersion: '21',
    screenWidth: null,
    screenHeight: null,
    fullscreen: false,
    azureClientId: '',
    afterLaunch: 'keep',
    activeModpackId: 'palamod-recreated',
    modpackProfiles: {
      'palamod-recreated': { ...packs },
      'wither-storm': { ...packs }
    },
    uiLanguage: 'en',
    uiTheme: 'dark',
    uiAccentHex: '#ff6a1a',
    uiFontScale: 'm',
    uiReduceMotion: false,
    uiCompact: false,
    uiSounds: true,
    uiSoundVolume: 1,
    uiSoundInstall: true,
    uiSoundLaunch: true,
    discordRichPresence: false,
    updateChannel: 'stable',
    skinViewerAnimation: 'none',
    skinViewerBackground: '#141416'
  }
}

function IconHome({ className }: { className?: string } = {}) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M3 9.5L12 3l9 6.5V21a1 1 0 01-1 1h-5v-7H9v7H4a1 1 0 01-1-1V9.5z" />
    </svg>
  )
}

function IconGear() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <circle cx="12" cy="12" r="3" />
      <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
    </svg>
  )
}

/** Icône paramètres compacte pour la barre latérale. */
function IconSettingsNav() {
  return (
    <svg className="icon-settings-nav" viewBox="0 0 24 24" fill="none" aria-hidden>
      <circle cx="12" cy="12" r="3.25" stroke="currentColor" strokeWidth="1.65" />
      <path
        d="M12 2.25v2.2M12 19.55v2.2M4.6 4.6l1.55 1.55M17.85 17.85l1.55 1.55M2.25 12h2.2M19.55 12h2.2M4.6 19.4l1.55-1.55M17.85 6.15l1.55-1.55"
        stroke="currentColor"
        strokeWidth="1.65"
        strokeLinecap="round"
      />
    </svg>
  )
}

function SettingsToggle({
  checked,
  onChange,
  label
}: {
  checked: boolean
  onChange: (next: boolean) => void
  label: string
}) {
  return (
    <label className="settings-toggle-row">
      <span className="settings-toggle-control">
        <input
          type="checkbox"
          className="settings-toggle-input"
          checked={checked}
          onChange={(e) => onChange(e.target.checked)}
        />
        <span className="settings-toggle-track">
          <span className="settings-toggle-thumb" />
        </span>
      </span>
      <span className="settings-toggle-text">{label}</span>
    </label>
  )
}

function IconUser() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
      <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
      <circle cx="12" cy="7" r="4" />
    </svg>
  )
}

function IconDiscord() {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028 14.09 14.09 0 0 0 1.226-1.994.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.956-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.946 2.418-2.157 2.418z" />
    </svg>
  )
}

const DISCORD_INVITE_URL = 'https://discord.gg/jVGq5aZ6Wc'

/** Libellé affiché dans l’UI (affichage marketing ; la version technique = package.json / getVersion). */
const LAUNCHER_VERSION_DISPLAY = '26.1 | Release'

const MIN_BOOT_MS = 1650

function IconPlay() {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor">
      <path d="M8 5v14l11-7z" />
    </svg>
  )
}

function IconStop() {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor">
      <rect x="6" y="6" width="12" height="12" rx="1.5" />
    </svg>
  )
}

function TitleBar() {
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
    <header className="titlebar">
      <div
        className="titlebar-drag"
        role="presentation"
        onDoubleClick={() => void toggleMax()}
      >
        <span className="titlebar-title">SOLEA PIXEL LAUNCHER</span>
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

function LoginGate({ testMode, onLoggedIn }: { testMode: boolean; onLoggedIn: () => void }) {
  const { t } = useI18n()
  const [busy, setBusy] = useState(false)
  const [msg, setMsg] = useState<string | null>(null)

  const connect = async () => {
    setBusy(true)
    setMsg(null)
    const r = await window.solea.addAccount()
    setBusy(false)
    if (r.ok) onLoggedIn()
    else if (r.reason === 'cancelled') setMsg(t('login.cancelled'))
    else setMsg(r.detail ?? t('login.failed'))
  }

  return (
    <div className="login-root" style={{ backgroundImage: `url(${LOGIN_WALLPAPER})` }}>
      {testMode && (
        <div className="test-strip login-test-strip">
          {t('login.testMode')} <code style={{ color: '#ffcc66' }}>test/electron-user-data</code>
        </div>
      )}
      <div className="login-card">
        <img src={SOLEA_WORDMARK} alt="Solea Pixel" className="login-wordmark" />
        <h2 className="login-welcome-title">{t('login.title')}</h2>
        <p className="login-lead">{t('login.lead')}</p>
        <p className="login-welcome-extra">{t('login.extra')}</p>
        <button type="button" className="btn-ms" disabled={busy} onClick={() => void connect()}>
          {busy ? t('login.connectBusy') : t('login.connect')}
        </button>
        {msg && <p className="login-error">{msg}</p>}
        <p className="login-legal">{t('login.legal')}</p>
      </div>
    </div>
  )
}

export function App() {
  const { t, setLocale, formatPercent } = useI18n()
  const { pushToast } = useToast()
  const [screen, setScreen] = useState<'boot' | 'login' | 'app'>('boot')
  const [testMode, setTestMode] = useState(false)
  const [modpackName, setModpackName] = useState('Palamod Recreated')
  const [view, setView] = useState<'home' | 'settings' | 'account'>('home')
  const [settingsTab, setSettingsTab] = useState<'launcher' | ModpackIdUi>('launcher')
  const [accounts, setAccounts] = useState<{ uuid: string; name: string }[]>([])
  const [activeAcc, setActiveAcc] = useState<{ name: string; uuid: string } | null>(null)
  const [menuOpen, setMenuOpen] = useState(false)
  const [phase, setPhase] = useState<'idle' | 'installing' | 'busy'>('idle')
  const [installLine, setInstallLine] = useState('')
  const [installPct, setInstallPct] = useState(0)
  const [settings, setSettings] = useState<LauncherSettingsUI>(emptySettings)
  const [settingsFb, setSettingsFb] = useState<{ text: string; ok: boolean } | null>(null)
  const [launchPhase, setLaunchPhase] = useState<'idle' | 'launching' | 'running'>('idle')
  const [modpackUi, setModpackUi] = useState<{
    loading: boolean
    needsInstall: boolean
    needsUpdate: boolean
    error?: string
  }>({ loading: true, needsInstall: false, needsUpdate: false })
  const [activeModpackId, setActiveModpackId] = useState<ModpackIdUi>('palamod-recreated')
  const [modpacksList, setModpacksList] = useState<{ id: string; displayName: string }[]>([])
  const [packSwitching, setPackSwitching] = useState(false)
  const [verifyResult, setVerifyResult] = useState<
    | null
    | { ok: true }
    | { ok: false; reason: string; detail?: string; paths?: string[] }
  >(null)
  const [packMaintBusy, setPackMaintBusy] = useState(false)
  const [bootProgress, setBootProgress] = useState(0)
  const [accountSkinKey, setAccountSkinKey] = useState(0)
  const [accountFb, setAccountFb] = useState<string | null>(null)
  const [updateDownloaded, setUpdateDownloaded] = useState(false)
  const [prefersRm, setPrefersRm] = useState(
    () => typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
  )
  const settingsRef = useRef(settings)
  settingsRef.current = settings
  const manualUpdateCheckRef = useRef(false)
  const installBusyRef = useRef(false)
  const launchBusyRef = useRef(false)

  const reduceMotionEffective = useMemo(
    () => settings.uiReduceMotion || prefersRm,
    [settings.uiReduceMotion, prefersRm]
  )

  const uiSoundPrefs: UiSoundPrefs = useMemo(
    () => ({
      master: settings.uiSounds,
      reduceMotion: reduceMotionEffective,
      volume:
        typeof settings.uiSoundVolume === 'number' && !Number.isNaN(settings.uiSoundVolume)
          ? settings.uiSoundVolume
          : 1,
      onInstall: settings.uiSoundInstall !== false,
      onLaunch: settings.uiSoundLaunch !== false
    }),
    [
      settings.uiSounds,
      settings.uiSoundVolume,
      settings.uiSoundInstall,
      settings.uiSoundLaunch,
      reduceMotionEffective
    ]
  )

  const refreshModpackUi = useCallback(async () => {
    setModpackUi((s) => ({ ...s, loading: true }))
    const r = await window.solea.getModpackActionInfo()
    setModpackUi({
      loading: false,
      needsInstall: r.needsInstall,
      needsUpdate: r.needsUpdate,
      error: r.error
    })
  }, [])

  const refreshAuth = useCallback(async () => {
    const s = await window.solea.getAuthState()
    setScreen(s.requiresMicrosoftLogin ? 'login' : 'app')
  }, [])

  const loadAccounts = useCallback(async () => {
    const [list, active] = await Promise.all([window.solea.listAccounts(), window.solea.getActiveAccount()])
    setAccounts(list)
    setActiveAcc(active)
  }, [])

  useEffect(() => {
    if (view === 'account') {
      setAccountFb(null)
      void loadAccounts()
    }
  }, [view, loadAccounts])

  useEffect(() => {
    applyAppearanceSettings(settings)
  }, [settings])

  useEffect(() => {
    setLocale(settings.uiLanguage)
  }, [settings.uiLanguage, setLocale])

  useEffect(() => {
    if (settings.uiTheme !== 'system') return
    return subscribeSystemTheme(() => applyAppearanceSettings(settingsRef.current))
  }, [settings.uiTheme])

  useEffect(() => {
    const m = window.matchMedia('(prefers-reduced-motion: reduce)')
    const onChange = () => {
      setPrefersRm(m.matches)
      applyAppearanceSettings(settingsRef.current)
    }
    m.addEventListener('change', onChange)
    return () => m.removeEventListener('change', onChange)
  }, [])

  useEffect(() => {
    let cancelled = false
    const start = performance.now()
    let rafId = 0

    const tickProgress = () => {
      if (cancelled) return
      const elapsed = performance.now() - start
      const p = Math.min(99, (elapsed / MIN_BOOT_MS) * 100)
      setBootProgress(p)
      if (elapsed < MIN_BOOT_MS) rafId = requestAnimationFrame(tickProgress)
    }
    rafId = requestAnimationFrame(tickProgress)

    const pathsP = window.solea.getPaths().then((p) => {
      if (cancelled) return
      setTestMode(p.testMode)
      if (p.modpackDisplayName) setModpackName(p.modpackDisplayName)
      if (p.modpacks?.length) setModpacksList(p.modpacks)
      if (p.activeModpackId && isModpackId(p.activeModpackId)) setActiveModpackId(p.activeModpackId)
    })

    const authP = window.solea.getAuthState()

    void Promise.all([pathsP, authP])
      .then(([, auth]) => {
        if (cancelled) return
        const elapsed = performance.now() - start
        const wait = Math.max(0, MIN_BOOT_MS - elapsed)
        window.setTimeout(() => {
          if (cancelled) return
          setBootProgress(100)
          window.setTimeout(() => {
            if (!cancelled) setScreen(auth.requiresMicrosoftLogin ? 'login' : 'app')
          }, 240)
        }, wait)
      })
      .catch(() => {
        if (cancelled) return
        setBootProgress(100)
        window.setTimeout(() => {
          if (!cancelled) setScreen('login')
        }, 400)
      })

    return () => {
      cancelled = true
      cancelAnimationFrame(rafId)
    }
  }, [])

  useEffect(() => {
    if (screen !== 'app') return
    void loadAccounts()
    void window.solea.getSettings().then(setSettings)
    void refreshModpackUi()
    void window.solea.getAppVersion()
    void window.solea.isGameRunning().then((run) => {
      if (run) setLaunchPhase('running')
    })
    void window.solea.getPaths().then((p) => {
      if (p.modpackDisplayName) setModpackName(p.modpackDisplayName)
      if (p.modpacks?.length) setModpacksList(p.modpacks)
      if (p.activeModpackId && isModpackId(p.activeModpackId)) setActiveModpackId(p.activeModpackId)
    })
  }, [screen, loadAccounts, refreshModpackUi])

  useEffect(() => {
    if (screen !== 'app') return
    const offA = window.solea.onUpdaterAvailable((p) => {
      manualUpdateCheckRef.current = false
      pushToast(t('updater.available', { version: p.version }), 'info', 7000)
      void window.solea.downloadUpdate().then((r) => {
        if (!r.ok) pushToast(r.error, 'error')
      })
    })
    const offN = window.solea.onUpdaterNotAvailable(() => {
      if (!manualUpdateCheckRef.current) return
      manualUpdateCheckRef.current = false
      pushToast(t('updater.none'), 'success')
    })
    const offD = window.solea.onUpdaterDownloaded(() => {
      setUpdateDownloaded(true)
      pushToast(t('updater.downloaded'), 'success', 8500)
    })
    const offE = window.solea.onUpdaterError((msg) => {
      if (manualUpdateCheckRef.current) manualUpdateCheckRef.current = false
      const m = msg?.trim() ?? ''
      const net = /network|net::|econnrefused|timeout|enotfound|econnreset|failed to fetch|getaddrinfo|offline/i.test(
        m
      )
      const text = net
        ? t('updater.errorNetwork', { detail: m || t('updater.error') })
        : m
          ? `${m} ${t('updater.errorCodeHint')}`
          : t('updater.error')
      pushToast(text, 'error', net ? 10000 : 7000)
    })
    return () => {
      offA()
      offN()
      offD()
      offE()
    }
  }, [screen, t, pushToast])

  const selectModpack = async (id: ModpackIdUi) => {
    if (id === activeModpackId) return
    setPackSwitching(true)
    setVerifyResult(null)
    await new Promise((r) => setTimeout(r, 90))
    const r = await window.solea.setActiveModpack(id)
    if (!r.ok) {
      pushToast(r.error, 'error')
      setPackSwitching(false)
      return
    }
    setActiveModpackId(r.activeModpackId as ModpackIdUi)
    const p = await window.solea.getPaths()
    if (p.modpackDisplayName) setModpackName(p.modpackDisplayName)
    await refreshModpackUi()
    void window.solea.isGameRunning().then((run) => setLaunchPhase(run ? 'running' : 'idle'))
    window.setTimeout(() => setPackSwitching(false), 360)
  }

  useEffect(() => {
    return window.solea.onGameExited(() => setLaunchPhase('idle'))
  }, [])

  useEffect(() => {
    if (launchPhase !== 'running') return
    const id = window.setInterval(() => {
      void window.solea.isGameRunning().then((run) => {
        if (!run) setLaunchPhase('idle')
      })
    }, 2500)
    return () => window.clearInterval(id)
  }, [launchPhase])

  useEffect(() => {
    const off = window.solea.onInstallProgress((p) => {
      setInstallLine(p.detail ?? p.phase)
      if (p.total > 0) setInstallPct(Math.round((p.current / p.total) * 100))
    })
    return off
  }, [])

  useEffect(() => {
    if (!menuOpen) return
    const close = () => setMenuOpen(false)
    document.addEventListener('click', close)
    return () => document.removeEventListener('click', close)
  }, [menuOpen])

  useEffect(() => {
    if (screen !== 'app') return
    const onKey = (e: KeyboardEvent) => {
      if (e.repeat) return
      if ((e.ctrlKey || e.metaKey) && e.key === ',') {
        e.preventDefault()
        setView('settings')
        setSettingsTab('launcher')
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [screen])

  useEffect(() => {
    const busy =
      phase === 'installing' || phase === 'busy' || launchPhase === 'launching' || packMaintBusy
    document.body.classList.toggle('solea-global-busy', busy)
    return () => document.body.classList.remove('solea-global-busy')
  }, [phase, launchPhase, packMaintBusy])

  const setNum = (key: keyof LauncherSettingsUI, v: string, allowNull = false) => {
    if (allowNull && v === '') {
      setSettings((s) => ({ ...s, [key]: null }))
      return
    }
    const n = parseInt(v, 10)
    if (!Number.isNaN(n)) setSettings((s) => ({ ...s, [key]: n }))
  }

  const setPackNum = (
    packId: ModpackIdUi,
    key: 'screenWidth' | 'screenHeight',
    v: string,
    allowNull = false
  ) => {
    setSettings((s) => {
      const cur = s.modpackProfiles[packId] ?? emptyPackProfile()
      let nextVal: number | null = cur[key]
      if (allowNull && v === '') nextVal = null
      else {
        const n = parseInt(v, 10)
        if (!Number.isNaN(n)) nextVal = n
      }
      return {
        ...s,
        modpackProfiles: {
          ...s.modpackProfiles,
          [packId]: { ...cur, [key]: nextVal }
        }
      }
    })
  }

  const patchPackProfile = (packId: ModpackIdUi, patch: Partial<ModpackGameProfileUI>) => {
    setSettings((s) => ({
      ...s,
      modpackProfiles: {
        ...s.modpackProfiles,
        [packId]: { ...(s.modpackProfiles[packId] ?? emptyPackProfile()), ...patch }
      }
    }))
  }

  const onSelectAccount = async (uuid: string) => {
    const r = await window.solea.setActiveAccount(uuid)
    if (r.ok) {
      await loadAccounts()
      setMenuOpen(false)
    }
  }

  const onRemoveAccount = async (uuid: string) => {
    await window.solea.removeAccount(uuid)
    setMenuOpen(false)
    await loadAccounts()
    await refreshAuth()
  }

  const onAddAccount = async () => {
    setPhase('busy')
    const r = await window.solea.addAccount()
    setPhase('idle')
    if (r.ok) {
      await loadAccounts()
      setMenuOpen(false)
      pushToast(t('toast.accountAdded', { name: r.name }), 'success')
    } else if (r.reason !== 'cancelled') pushToast(r.detail ?? t('toast.accountFail'), 'error')
  }

  const onInstall = async () => {
    if (installBusyRef.current) return
    installBusyRef.current = true
    setPhase('installing')
    setInstallPct(0)
    setInstallLine(t('install.prepare'))
    try {
      const r = await window.solea.installModpack()
      if (r.ok) {
        pushToast(t('toast.installDone'), 'success')
        playUiSound('install', uiSoundPrefs)
        void refreshModpackUi()
      } else pushToast(r.error, 'error')
    } finally {
      setPhase('idle')
      installBusyRef.current = false
    }
  }

  const onReinstallModpack = async (packId: ModpackIdUi) => {
    if (!window.confirm(t('confirm.reinstall'))) {
      return
    }
    setPackMaintBusy(true)
    setPhase('installing')
    setInstallPct(0)
    setInstallLine(t('install.reinstall'))
    const r = await window.solea.reinstallModpack(packId)
    setPhase('idle')
    setPackMaintBusy(false)
    if (!r.ok) {
      pushToast(r.error, 'error')
      return
    }
    pushToast(t('toast.reinstalled'), 'success')
    playUiSound('install', uiSoundPrefs)
    void refreshModpackUi()
  }

  const onUninstallModpack = async (packId: ModpackIdUi) => {
    if (!window.confirm(t('confirm.uninstall'))) {
      return
    }
    setPackMaintBusy(true)
    const r = await window.solea.uninstallModpack(packId)
    setPackMaintBusy(false)
    if (!r.ok) {
      pushToast(r.error, 'error')
      return
    }
    pushToast(t('toast.uninstalled'), 'success')
    void refreshModpackUi()
  }

  const onLaunchOrClose = async () => {
    if (launchPhase === 'running') {
      await window.solea.stopGame()
      setLaunchPhase('idle')
      return
    }
    if (launchPhase === 'launching') return
    if (launchBusyRef.current) return
    launchBusyRef.current = true
    setLaunchPhase('launching')
    try {
      const r = await window.solea.launch()
      if (!r.ok) {
        setLaunchPhase('idle')
        pushToast(r.error, 'error')
        return
      }
      setLaunchPhase('running')
      playUiSound('launch', uiSoundPrefs)
    } finally {
      launchBusyRef.current = false
    }
  }

  const onVerify = async () => {
    setVerifyResult(null)
    const r = await window.solea.verifyModpack()
    if (r.ok) {
      setVerifyResult({ ok: true })
      return
    }
    setVerifyResult({
      ok: false,
      reason: r.reason,
      detail: r.detail,
      paths: r.paths
    })
    if (r.reason !== 'extra_mod') pushToast(r.detail ?? r.reason, 'error')
  }

  const onCheckUpdates = async () => {
    const r = await window.solea.checkForUpdates()
    if (!r.started) {
      pushToast(t('updater.devSkip'), 'info')
      return
    }
    manualUpdateCheckRef.current = true
    pushToast(t('updater.checking'), 'info', 2800)
  }

  const saveAllSettings = async () => {
    setSettingsFb(null)
    const normProf = (p: ModpackGameProfileUI): ModpackGameProfileUI => ({
      ...p,
      screenWidth: p.screenWidth === null || p.screenWidth === undefined ? null : Number(p.screenWidth),
      screenHeight: p.screenHeight === null || p.screenHeight === undefined ? null : Number(p.screenHeight)
    })
    const modpackProfiles = Object.fromEntries(
      Object.entries(settings.modpackProfiles).map(([id, p]) => [id, normProf(p)])
    ) as LauncherSettingsUI['modpackProfiles']
    const activeId = isModpackId(settings.activeModpackId) ? settings.activeModpackId : 'palamod-recreated'
    const ap = modpackProfiles[activeId] ?? emptyPackProfile()
    const payload: LauncherSettingsUI = {
      ...settings,
      modpackProfiles,
      memoryMin: ap.memoryMin,
      memoryMax: ap.memoryMax,
      gameArgs: ap.gameArgs,
      screenWidth: ap.screenWidth,
      screenHeight: ap.screenHeight,
      fullscreen: ap.fullscreen
    }
    const r = await window.solea.saveSettings(payload)
    setSettingsFb(
      r.ok
        ? { text: t('settings.saved'), ok: true }
        : { text: r.error, ok: false }
    )
  }

  const resetAllSettings = async () => {
    const r = await window.solea.resetSettings()
    if (r.ok) {
      const s = await window.solea.getSettings()
      setSettings(s)
      setSettingsFb({ text: t('settings.resetOk'), ok: true })
    } else setSettingsFb({ text: r.error, ok: false })
  }

  if (screen === 'boot') {
    return (
      <div className="app-chrome">
        <TitleBar />
        <div className="app-fill">
          <div className="boot-screen">
            <div className="boot-screen-inner">
              <img src={bootLogoUrl} alt="Solea Pixel" className="boot-logo" />
              <div
                className="boot-progress-track"
                role="progressbar"
                aria-valuenow={Math.round(bootProgress)}
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuetext={formatPercent(Math.round(bootProgress))}
                aria-label={t('boot.aria')}
              >
                <div className="boot-progress-fill" style={{ width: `${bootProgress}%` }} />
              </div>
              <span className="boot-progress-label">{t('boot.loading')}</span>
            </div>
          </div>
        </div>
      </div>
    )
  }

  if (screen === 'login') {
    return (
      <div className="app-chrome">
        <TitleBar />
        <div className="app-fill">
          <LoginGate testMode={testMode} onLoggedIn={() => setScreen('app')} />
        </div>
      </div>
    )
  }

  return (
    <div className="app-chrome">
      <TitleBar />
      <div className="app-fill">
    <div className="shell">
      <aside className="shell-sidebar">
        <div className="sb-top">
          <button
            type="button"
            className={`sb-btn ${view === 'home' ? 'active' : ''}`}
            title={t('shell.home')}
            aria-label={t('shell.home')}
            onClick={() => setView('home')}
          >
            <IconHome />
          </button>
          <button
            type="button"
            className={`sb-btn ${view === 'settings' ? 'active' : ''}`}
            title={t('shell.settings')}
            aria-label={t('shell.settings')}
            onClick={() => setView('settings')}
          >
            <IconGear />
          </button>
        </div>
        <div className="sb-packs" role="navigation" aria-label={t('shell.navPacks')}>
          {modpacksList.map((m) =>
            isModpackId(m.id) ? (
              <button
                key={m.id}
                type="button"
                className={`sb-btn sb-btn-pack ${activeModpackId === m.id ? 'active' : ''}`}
                title={m.displayName}
                onClick={() => void selectModpack(m.id)}
              >
                <img src={MODPACK_THEME[m.id].sidebarIcon} alt="" className="sb-pack-icon" />
              </button>
            ) : null
          )}
        </div>
        <div className="sb-bottom">
          <button
            type="button"
            className={`sb-btn ${view === 'account' ? 'active' : ''}`}
            title={t('shell.account')}
            aria-label={t('shell.account')}
            onClick={() => setView('account')}
          >
            <IconUser />
          </button>
          <button
            type="button"
            className="sb-btn sb-btn-discord"
            title={t('shell.discord')}
            aria-label={t('shell.discord')}
            onClick={() => void window.solea.openExternalUrl(DISCORD_INVITE_URL)}
          >
            <IconDiscord />
          </button>
        </div>
      </aside>

      <div
        className={`shell-main ${view === 'settings' || view === 'account' ? 'settings-mode' : ''} ${
          view === 'home' && isModpackId(activeModpackId) ? MODPACK_THEME[activeModpackId].themeClass : ''
        } ${packSwitching && view === 'home' ? 'pack-switching' : ''}`}
      >
        {view === 'home' && isModpackId(activeModpackId) && (
          <div
            key={activeModpackId}
            className="shell-main-wallpaper"
            style={{ backgroundImage: `url(${MODPACK_THEME[activeModpackId].wallpaper})` }}
            aria-hidden
          />
        )}
        <div key={view} className="shell-view-shell">
        {view === 'home' && (
          <>
          <div className="shell-content shell-content-home">
            {testMode && <div className="test-strip home-test-strip">{t('home.testStrip')}</div>}

            <div className="home-panel">
              <div className="home-body">
                <span className="tag-pill">{t('home.tag')}</span>
                {(() => {
                  const { first, second } = packTitleLines(modpackName)
                  return (
                    <h2 className="home-title-stacked">
                      <span className="home-title-line">{first}</span>
                      {second ? <span className="home-title-line">{second}</span> : null}
                    </h2>
                  )
                })()}
                <p className="lead">{t('home.lead')}</p>

                <div className="play-row">
                  <button
                    type="button"
                    className={`btn-play${launchPhase === 'running' ? ' btn-play-close' : ''}`}
                    disabled={
                      launchPhase === 'launching' ||
                      phase === 'installing' ||
                      phase === 'busy' ||
                      modpackUi.loading ||
                      modpackUi.needsInstall
                    }
                    onClick={() => void onLaunchOrClose()}
                  >
                    {launchPhase === 'running' ? <IconStop /> : <IconPlay />}
                    {launchPhase === 'launching'
                      ? t('home.playLaunching')
                      : launchPhase === 'running'
                        ? t('home.playClose')
                        : t('home.play')}
                  </button>
                  {!modpackUi.loading && (modpackUi.needsInstall || modpackUi.needsUpdate) && (
                    <button
                      type="button"
                      className="btn-quiet"
                      disabled={phase !== 'idle' || launchPhase !== 'idle'}
                      onClick={() => void onInstall()}
                    >
                      {phase === 'installing'
                        ? t('home.installBusy')
                        : modpackUi.needsInstall
                          ? t('home.installFirst')
                          : t('home.installUpdate')}
                    </button>
                  )}
                  <button
                    type="button"
                    className="btn-quiet"
                    disabled={phase !== 'idle' || launchPhase !== 'idle'}
                    onClick={() => void onVerify()}
                    title={t('home.verifyTooltip')}
                  >
                    {t('home.verify')}
                  </button>
                </div>

                {verifyResult && (
                  <div
                    className={`verify-banner ${verifyResult.ok ? 'ok' : 'fail'}`}
                    role="status"
                    aria-live="polite"
                  >
                    {verifyResult.ok ? (
                      <>{t('home.verifyOk')}</>
                    ) : verifyResult.reason === 'extra_mod' ? (
                      <>
                        <strong>{t('home.verifyExtraTitle')}</strong> — {t('home.verifyExtraBody')}
                        <ul>
                          {(verifyResult.paths ?? []).map((path) => (
                            <li key={path}>{path}</li>
                          ))}
                        </ul>
                      </>
                    ) : verifyResult.reason === 'hash_mismatch' ? (
                      <>{t('home.verifyHash', { detail: verifyResult.detail ?? '—' })}</>
                    ) : verifyResult.reason === 'missing_file' ? (
                      <>{t('home.verifyMissing', { detail: verifyResult.detail ?? '' })}</>
                    ) : verifyResult.reason === 'no_lock' ? (
                      <>{t('home.verifyNoLock')}</>
                    ) : verifyResult.reason === 'read_error' ? (
                      <>{t('home.verifyRead', { detail: verifyResult.detail ?? verifyResult.reason })}</>
                    ) : (
                      <>{verifyResult.detail ?? verifyResult.reason}</>
                    )}
                  </div>
                )}

                <div className="profile-bar-wrap">
                  <button
                    type="button"
                    className="profile-bar"
                    onClick={(e) => {
                      e.stopPropagation()
                      setMenuOpen((o) => !o)
                    }}
                  >
                    <SkinHead uuid={activeAcc?.uuid} sizePx={56} className="profile-bar-head" />
                    <span>{activeAcc?.name ?? t('home.profileFallback')}</span>
                    <span className="chev">▾</span>
                  </button>
                  {menuOpen && (
                    <div className="profile-menu" onClick={(e) => e.stopPropagation()}>
                      {accounts.map((a) => (
                        <button
                          key={a.uuid}
                          type="button"
                          className={activeAcc?.uuid === a.uuid ? 'active-acc' : ''}
                          onClick={() => void onSelectAccount(a.uuid)}
                        >
                          <SkinHead uuid={a.uuid} sizePx={28} className="profile-menu-head" />
                          {a.name}
                        </button>
                      ))}
                      <div className="sep" />
                      <button type="button" onClick={() => void onAddAccount()}>
                        {t('home.addAccount')}
                      </button>
                      {activeAcc && (
                        <button type="button" onClick={() => void onRemoveAccount(activeAcc.uuid)}>
                          {t('home.removeAccount')}
                        </button>
                      )}
                    </div>
                  )}
                </div>

                {phase === 'installing' && (
                  <div className="progress-block">
                    <div
                      className="progress-track"
                      role="progressbar"
                      aria-valuenow={installPct}
                      aria-valuemin={0}
                      aria-valuemax={100}
                      aria-label={t('install.progressAria')}
                      aria-valuetext={`${installLine} — ${formatPercent(installPct)}`}
                    >
                      <div className="progress-fill" style={{ width: `${installPct}%` }} />
                    </div>
                    <span className="progress-label">{installLine}</span>
                  </div>
                )}

                <p className="progress-label" style={{ marginTop: 12 }} title={t('home.ramTooltip')}>
                  {t('home.ram')}{' '}
                  {settings.modpackProfiles[activeModpackId]?.memoryMin ?? settings.memoryMin} —{' '}
                  {settings.modpackProfiles[activeModpackId]?.memoryMax ?? settings.memoryMax}
                </p>

                {(launchPhase === 'launching' || launchPhase === 'running') && (
                  <button
                    type="button"
                    className="btn-quiet home-log-console-btn"
                    onClick={() => void window.solea.openGameLogWindow()}
                  >
                    {t('home.logConsole')}
                  </button>
                )}
              </div>
            </div>
          </div>
          <footer className="shell-footer">{t('home.footer', { name: modpackName })}</footer>
          </>
        )}

        {view === 'settings' && (
          <div className="settings-layout" style={{ flex: 1, minHeight: 0 }}>
            <nav className="settings-nav">
              <div className="settings-nav-brand">
                <span className="settings-nav-brand-icon" aria-hidden>
                  <IconSettingsNav />
                </span>
                <h3 className="settings-nav-brand-title">{t('settings.title')}</h3>
              </div>
              <button
                type="button"
                className={`nav-item ${settingsTab === 'launcher' ? 'on' : ''}`}
                onClick={() => setSettingsTab('launcher')}
              >
                <IconHome className="settings-nav-launcher-icon" /> {t('settings.navLauncher')}
              </button>
              {modpacksList.map((m) =>
                isModpackId(m.id) ? (
                  <button
                    key={m.id}
                    type="button"
                    className={`nav-item nav-item-pack ${settingsTab === m.id ? 'on' : ''}`}
                    onClick={() => setSettingsTab(m.id)}
                  >
                    <img src={MODPACK_THEME[m.id].sidebarIcon} alt="" className="nav-pack-thumb" />
                    <span className="nav-pack-label">{m.displayName}</span>
                  </button>
                ) : null
              )}
              <div className="nav-bottom">
                <button type="button" className="nav-item" onClick={() => void window.solea.openUserDataFolder()}>
                  {t('settings.userData')}
                </button>
                <button type="button" className="nav-item" onClick={() => void window.solea.openInstanceFolder()}>
                  {t('settings.instanceFolder')}
                </button>
              </div>
            </nav>

            <div className="settings-body">
              <header>
                <img
                  src={
                    settingsTab !== 'launcher' && isModpackId(settingsTab)
                      ? MODPACK_THEME[settingsTab].sidebarIcon
                      : LOGO
                  }
                  alt=""
                  className="settings-header-ico"
                />
                <h2>
                  {settingsTab === 'launcher'
                    ? t('settings.headerLauncher')
                    : isModpackId(settingsTab)
                      ? t('settings.headerGame', {
                          name: modpacksList.find((x) => x.id === settingsTab)?.displayName ?? settingsTab
                        })
                      : t('settings.headerLauncher')}
                </h2>
              </header>

              {phase === 'installing' && (
                <div className="settings-install-progress">
                  <div
                    className="progress-track"
                    role="progressbar"
                    aria-valuenow={installPct}
                    aria-valuemin={0}
                    aria-valuemax={100}
                    aria-label={t('install.progressAria')}
                    aria-valuetext={`${installLine} — ${formatPercent(installPct)}`}
                  >
                    <div className="progress-fill" style={{ width: `${installPct}%` }} />
                  </div>
                  <span className="progress-label">{installLine}</span>
                </div>
              )}

              {settingsTab === 'launcher' && (
                <>
                  <details className="set-card" open>
                    <summary>
                      <div>
                        {t('settings.afterLaunch')}
                        <div className="sub">{t('settings.afterLaunchSub')}</div>
                      </div>
                      <span>▾</span>
                    </summary>
                    <div className="inner">
                      <LauncherSelect
                        value={settings.afterLaunch}
                        onChange={(v) =>
                          setSettings((s) => ({ ...s, afterLaunch: v as 'keep' | 'minimize' }))
                        }
                        options={[
                          { value: 'keep', label: t('settings.afterKeep') },
                          { value: 'minimize', label: t('settings.afterMinimize') }
                        ]}
                      />
                    </div>
                  </details>

                  <details className="set-card" open>
                    <summary>
                      <div>
                        {t('settings.network')}
                        <div className="sub">{t('settings.networkSub')}</div>
                      </div>
                      <span>▾</span>
                    </summary>
                    <div className="inner field-grid">
                      <label>
                        {t('settings.downloadThreads')}
                        <input
                          type="number"
                          min={1}
                          max={32}
                          value={settings.downloadThreads}
                          onChange={(e) => setNum('downloadThreads', e.target.value)}
                        />
                      </label>
                      <label>
                        {t('settings.timeout')}
                        <input
                          type="number"
                          min={5000}
                          max={120000}
                          step={1000}
                          value={settings.networkTimeoutMs}
                          onChange={(e) => setNum('networkTimeoutMs', e.target.value)}
                        />
                      </label>
                      <label className="full">
                        {t('settings.javaPath')}
                        <input
                          type="text"
                          value={settings.javaPath}
                          onChange={(e) => setSettings((s) => ({ ...s, javaPath: e.target.value }))}
                          spellCheck={false}
                        />
                      </label>
                      <label>
                        {t('settings.javaVersion')}
                        <input
                          type="text"
                          value={settings.javaVersion}
                          onChange={(e) => setSettings((s) => ({ ...s, javaVersion: e.target.value }))}
                          spellCheck={false}
                        />
                      </label>
                      <label className="full" title={t('settings.jvmArgsTooltip')}>
                        {t('settings.jvmArgs')}
                        <textarea
                          rows={4}
                          value={settings.jvmArgs}
                          onChange={(e) => setSettings((s) => ({ ...s, jvmArgs: e.target.value }))}
                          spellCheck={false}
                          title={t('settings.jvmArgsTooltip')}
                        />
                      </label>
                      <label className="full">
                        {t('settings.azureId')}
                        <input
                          type="text"
                          value={settings.azureClientId}
                          onChange={(e) => setSettings((s) => ({ ...s, azureClientId: e.target.value }))}
                          spellCheck={false}
                        />
                      </label>
                    </div>
                  </details>

                  <details className="set-card" open>
                    <summary>
                      <div>
                        {t('settings.appearance')}
                        <div className="sub">{t('settings.appearanceSub')}</div>
                      </div>
                      <span>▾</span>
                    </summary>
                    <div className="inner field-grid">
                      <label className="full">
                        {t('settings.lang')}
                        <LauncherSelect
                          value={settings.uiLanguage}
                          onChange={(v) =>
                            setSettings((s) => ({ ...s, uiLanguage: v as 'en' | 'fr' }))
                          }
                          options={[
                            { value: 'en', label: t('settings.langEn') },
                            { value: 'fr', label: t('settings.langFr') }
                          ]}
                        />
                      </label>
                      <label className="full">
                        {t('settings.theme')}
                        <LauncherSelect
                          value={settings.uiTheme}
                          onChange={(v) =>
                            setSettings((s) => ({
                              ...s,
                              uiTheme: v as 'light' | 'dark' | 'system'
                            }))
                          }
                          options={[
                            { value: 'light', label: t('settings.themeLight') },
                            { value: 'dark', label: t('settings.themeDark') },
                            { value: 'system', label: t('settings.themeSystem') }
                          ]}
                        />
                      </label>
                      <label className="full">
                        {t('settings.fontScale')}
                        <LauncherSelect
                          value={settings.uiFontScale}
                          onChange={(v) =>
                            setSettings((s) => ({ ...s, uiFontScale: v as 's' | 'm' | 'l' }))
                          }
                          options={[
                            { value: 's', label: t('settings.fontS') },
                            { value: 'm', label: t('settings.fontM') },
                            { value: 'l', label: t('settings.fontL') }
                          ]}
                        />
                      </label>
                      <div className="full settings-toggle-stack">
                        <SettingsToggle
                          checked={settings.uiReduceMotion}
                          onChange={(next) => setSettings((s) => ({ ...s, uiReduceMotion: next }))}
                          label={t('settings.reduceMotion')}
                        />
                        <SettingsToggle
                          checked={settings.uiCompact}
                          onChange={(next) => setSettings((s) => ({ ...s, uiCompact: next }))}
                          label={t('settings.uiCompact')}
                        />
                        <SettingsToggle
                          checked={settings.uiSounds}
                          onChange={(next) => setSettings((s) => ({ ...s, uiSounds: next }))}
                          label={t('settings.uiSounds')}
                        />
                        {settings.uiSounds ? (
                          <>
                            <label className="full settings-sound-volume">
                              <span className="settings-sound-volume-label">
                                {t('settings.uiSoundVolume')}{' '}
                                <span className="settings-sound-volume-val">
                                  {Math.round((settings.uiSoundVolume ?? 1) * 100)}%
                                </span>
                              </span>
                              <input
                                type="range"
                                min={0}
                                max={100}
                                value={Math.round((settings.uiSoundVolume ?? 1) * 100)}
                                onChange={(e) =>
                                  setSettings((s) => ({
                                    ...s,
                                    uiSoundVolume: Number(e.target.value) / 100
                                  }))
                                }
                              />
                            </label>
                            <SettingsToggle
                              checked={settings.uiSoundInstall !== false}
                              onChange={(next) => setSettings((s) => ({ ...s, uiSoundInstall: next }))}
                              label={t('settings.uiSoundInstall')}
                            />
                            <SettingsToggle
                              checked={settings.uiSoundLaunch !== false}
                              onChange={(next) => setSettings((s) => ({ ...s, uiSoundLaunch: next }))}
                              label={t('settings.uiSoundLaunch')}
                            />
                          </>
                        ) : null}
                        <SettingsToggle
                          checked={settings.discordRichPresence}
                          onChange={(next) => setSettings((s) => ({ ...s, discordRichPresence: next }))}
                          label={t('settings.discordRp')}
                        />
                        <p className="settings-discord-hint">{t('settings.discordRpHint')}</p>
                      </div>
                      <label className="full">
                        {t('settings.updateChannel')}
                        <LauncherSelect
                          value={settings.updateChannel}
                          onChange={(v) =>
                            setSettings((s) => ({
                              ...s,
                              updateChannel: v as 'stable' | 'beta'
                            }))
                          }
                          options={[
                            { value: 'stable', label: t('settings.channelStable') },
                            { value: 'beta', label: t('settings.channelBeta') }
                          ]}
                        />
                      </label>
                      <div
                        className="full settings-updater-row"
                        style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', alignItems: 'center' }}
                      >
                        <button type="button" className="btn-muted" onClick={() => void onCheckUpdates()}>
                          {t('settings.checkUpdates')}
                        </button>
                        {updateDownloaded ? (
                          <button type="button" className="btn-save" onClick={() => void window.solea.quitAndInstall()}>
                            {t('updater.restartNow')}
                          </button>
                        ) : null}
                      </div>
                      <label className="full">
                        {t('settings.skinAnim')}
                        <LauncherSelect
                          value={settings.skinViewerAnimation}
                          onChange={(v) =>
                            setSettings((s) => ({
                              ...s,
                              skinViewerAnimation: v as SkinViewerAnimation
                            }))
                          }
                          options={[
                            { value: 'none', label: t('settings.skinAnimNone') },
                            { value: 'idle', label: t('settings.skinAnimIdle') },
                            { value: 'walk', label: t('settings.skinAnimWalk') }
                          ]}
                        />
                      </label>
                      <label className="full">
                        {t('settings.skinBg')}
                        <input
                          type="text"
                          value={settings.skinViewerBackground}
                          onChange={(e) => setSettings((s) => ({ ...s, skinViewerBackground: e.target.value }))}
                          placeholder="#141416"
                          spellCheck={false}
                        />
                      </label>
                    </div>
                  </details>

                  <details className="set-card">
                    <summary>
                      <div>
                        {t('settings.shortcuts')}
                        <div className="sub">{t('settings.shortcutsSub')}</div>
                      </div>
                      <span>▾</span>
                    </summary>
                    <div className="inner settings-shortcuts-panel">
                      <dl className="settings-shortcuts-dl">
                        <div>
                          <dt>{t('settings.shortcutOpenSettings')}</dt>
                          <dd>Ctrl + ,</dd>
                        </div>
                      </dl>
                    </div>
                  </details>
                </>
              )}

              {settingsTab !== 'launcher' && isModpackId(settingsTab) && (
                <>
                  <details className="set-card" open title={t('settings.ramAllocTooltip')}>
                    <summary>
                      <div>
                        {t('settings.ram')}
                        <div className="sub">{t('settings.ramSub')}</div>
                      </div>
                      <span>▾</span>
                    </summary>
                    <div className="inner field-grid">
                      <label title={t('settings.ramAllocTooltip')}>
                        {t('settings.ramMin')}
                        <input
                          type="text"
                          value={settings.modpackProfiles[settingsTab]?.memoryMin ?? ''}
                          onChange={(e) => patchPackProfile(settingsTab, { memoryMin: e.target.value })}
                          spellCheck={false}
                        />
                      </label>
                      <label title={t('settings.ramAllocTooltip')}>
                        {t('settings.ramMax')}
                        <input
                          type="text"
                          value={settings.modpackProfiles[settingsTab]?.memoryMax ?? ''}
                          onChange={(e) => patchPackProfile(settingsTab, { memoryMax: e.target.value })}
                          spellCheck={false}
                        />
                      </label>
                    </div>
                  </details>

                  <details className="set-card" open>
                    <summary>
                      <div>
                        {t('settings.resolution')}
                        <div className="sub">{t('settings.resolutionSub')}</div>
                      </div>
                      <span>▾</span>
                    </summary>
                    <div className="inner field-grid">
                      <label>
                        {t('settings.width')}
                        <input
                          type="number"
                          value={settings.modpackProfiles[settingsTab]?.screenWidth ?? ''}
                          onChange={(e) => setPackNum(settingsTab, 'screenWidth', e.target.value, true)}
                          placeholder="1920"
                        />
                      </label>
                      <label>
                        {t('settings.height')}
                        <input
                          type="number"
                          value={settings.modpackProfiles[settingsTab]?.screenHeight ?? ''}
                          onChange={(e) => setPackNum(settingsTab, 'screenHeight', e.target.value, true)}
                          placeholder="1080"
                        />
                      </label>
                      <div className="full settings-toggle-stack">
                        <SettingsToggle
                          checked={settings.modpackProfiles[settingsTab]?.fullscreen ?? false}
                          onChange={(next) => patchPackProfile(settingsTab, { fullscreen: next })}
                          label={t('settings.fullscreen')}
                        />
                      </div>
                    </div>
                  </details>

                  <details className="set-card">
                    <summary>
                      <div>
                        {t('settings.gameArgs')}
                        <div className="sub">{t('settings.gameArgsSub')}</div>
                      </div>
                      <span>▾</span>
                    </summary>
                    <div className="inner">
                      <textarea
                        rows={3}
                        value={settings.modpackProfiles[settingsTab]?.gameArgs ?? ''}
                        onChange={(e) => patchPackProfile(settingsTab, { gameArgs: e.target.value })}
                        spellCheck={false}
                      />
                    </div>
                  </details>

                  <details className="set-card">
                    <summary>
                      <div>
                        {t('settings.install')}
                        <div className="sub">{t('settings.installSub')}</div>
                      </div>
                      <span>▾</span>
                    </summary>
                    <div className="inner modpack-maint-actions">
                      <button
                        type="button"
                        className="btn-muted"
                        disabled={packMaintBusy || phase === 'installing' || launchPhase !== 'idle'}
                        onClick={() => void onReinstallModpack(settingsTab)}
                      >
                        {t('settings.reinstall')}
                      </button>
                      <button
                        type="button"
                        className="btn-danger-outline"
                        disabled={packMaintBusy || phase === 'installing' || launchPhase !== 'idle'}
                        onClick={() => void onUninstallModpack(settingsTab)}
                      >
                        {t('settings.uninstall')}
                      </button>
                    </div>
                  </details>
                </>
              )}

              <div className="actions-bar">
                <button type="button" className="btn-save" onClick={() => void saveAllSettings()}>
                  {t('settings.save')}
                </button>
                <button type="button" className="btn-muted" onClick={() => void resetAllSettings()}>
                  {t('settings.reset')}
                </button>
                <button type="button" className="btn-muted" onClick={() => setView('home')}>
                  {t('settings.back')}
                </button>
              </div>
              {settingsFb && (
                <div className={`feedback ${settingsFb.ok ? 'ok' : 'err'}`}>{settingsFb.text}</div>
              )}
            </div>
          </div>
        )}

        {view === 'account' && (
          <div className="account-layout">
            <div className="account-panel">
              <header className="account-header">
                <h2 className="account-title">
                  <IconUser /> {t('account.title')}
                </h2>
                <p className="account-lead">{t('account.lead')}</p>
              </header>

              {activeAcc ? (
                <>
                  <SkinAccountPreview
                    uuid={activeAcc.uuid}
                    refreshKey={accountSkinKey}
                    playerName={activeAcc.name}
                    viewerBackground={settings.skinViewerBackground}
                    skinAnim={settings.skinViewerAnimation}
                    reduceMotion={reduceMotionEffective}
                    onRefresh={() => setAccountSkinKey((k) => k + 1)}
                  />

                  <div className="account-actions">
                    <button
                      type="button"
                      className="btn-muted"
                      onClick={() => {
                        void navigator.clipboard.writeText(activeAcc.uuid)
                        setAccountFb(t('account.uuidCopied'))
                      }}
                    >
                      {t('account.copyUuid')}
                    </button>
                    <button
                      type="button"
                      className="btn-muted"
                      onClick={() => {
                        void window.solea
                          .refreshActiveAccount()
                          .then((r) => {
                            if (r.ok) {
                              void loadAccounts()
                              setAccountFb(t('account.sessionRefreshed'))
                            } else setAccountFb(r.error)
                          })
                      }}
                    >
                      {t('account.refreshSession')}
                    </button>
                  </div>

                  {accounts.length > 1 ? (
                    <details className="set-card account-more">
                      <summary>
                        <div>
                          {t('account.otherAccounts')}
                          <div className="sub">{t('account.otherAccountsSub')}</div>
                        </div>
                        <span>▾</span>
                      </summary>
                      <div className="inner account-account-list">
                        {accounts.map((a) => (
                          <div key={a.uuid} className="account-row">
                            <span className="account-row-name">{a.name}</span>
                            <button
                              type="button"
                              className="btn-muted account-activate-btn"
                              disabled={a.uuid === activeAcc.uuid}
                              onClick={() => {
                                void window.solea.setActiveAccount(a.uuid).then((r) => {
                                  if (r.ok) {
                                    void loadAccounts()
                                    setAccountSkinKey((k) => k + 1)
                                  }
                                })
                              }}
                            >
                              {a.uuid === activeAcc.uuid ? t('account.active') : t('account.activate')}
                            </button>
                          </div>
                        ))}
                      </div>
                    </details>
                  ) : null}

                  {accountFb && <div className="account-feedback">{accountFb}</div>}

                  <div className="actions-bar account-footer-actions">
                    <button type="button" className="btn-muted" onClick={() => setView('home')}>
                      {t('account.back')}
                    </button>
                  </div>
                </>
              ) : (
                <p className="account-empty">{t('account.empty')}</p>
              )}
            </div>
          </div>
        )}
        </div>
        <div className="launcher-version-badge" role="status">
          {LAUNCHER_VERSION_DISPLAY}
        </div>
      </div>
    </div>
      </div>
    </div>
  )
}
