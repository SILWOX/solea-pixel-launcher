import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type ReactNode
} from 'react'
import type {
  LauncherSettingsUI,
  ModpackActionInfoRow,
  ModpackGameProfileUI,
  SkinViewerAnimation
} from './launcherTypes'
import logoUrl from './assets/branding/logo.png?url'
import loginWallpaperUrl from './assets/branding/login-wallpaper.png?url'
import soleaLoginLogoUrl from './assets/branding/solea-pixel-login-logo.png?url'
import bootLogoUrl from './assets/branding/boot-logo.png?url'
import newsWallpaperUrl from './assets/branding/news-wallpaper.png?url'
import './App.css'
import { MODPACK_THEME, isModpackId, type ModpackIdUi } from './modpackTheme'
import { AccountSkinViewer, type AccountSkinViewerHandle } from './AccountSkinViewer'
import { AccountCapeModal } from './AccountCapeModal'
import { PackMaintConfirmModal } from './PackMaintConfirmModal'
import { ModpackUpdatesModal } from './ModpackUpdatesModal'
import { LoginInfoModal } from './LoginInfoModal'
import { CacheClearConfirmModal, type CacheClearKind } from './CacheClearConfirmModal'
import { applyAppearanceSettings, subscribeSystemTheme } from './appearance'
import { useI18n } from './i18n/I18nContext'
import { LauncherSelect } from './ui/LauncherSelect'
import { MemoryRamSlider } from './MemoryRamSlider'
import { allocGbToMinMaxStrings, ramStringToGb } from './memoryRam'
import { useToast } from './ui/ToastContext'
import { playUiSound, type UiSoundPrefs } from './ui/playUiSound'
import { useFocusTrap } from './a11y/useFocusTrap'
import { LAUNCHER_CHANGELOG } from './launcherChangelog'
import { ScreenshotsView } from './ScreenshotsView'
import {
  SettingsGlossaryTrigger,
  type SettingsGlossaryKey
} from './settingsGlossary'
import {
  acceleratorMatches,
  formatAcceleratorForDisplay,
  keyboardEventToAcceleratorString
} from './keyboardAccelerator'

const LOGO = logoUrl
/** Fond dédié à l’écran Microsoft (distinct du fond Palamod sur l’accueil). */
const LOGIN_WALLPAPER = loginWallpaperUrl
const SOLEA_LOGIN_LOGO = soleaLoginLogoUrl
/** Fond de l’onglet Accueil & actus. */
const NEWS_WALLPAPER = newsWallpaperUrl

/** 5 clics rapides sur l’icône Paramètres ouvrent la fenêtre debug (développeur). */
const SETTINGS_DEBUG_TAPS = 5
/** Fenêtre de temps pour enchaîner les clics (sidebar + logo orange Paramètres). */
const SETTINGS_DEBUG_WINDOW_MS = 3200

/** Options du sélecteur d’animation (aperçu skin / cape) — libellés via i18n `labelKey`. */
const SKIN_ANIM_UI_OPTIONS: { value: SkinViewerAnimation; labelKey: string }[] = [
  { value: 'none', labelKey: 'settings.skinAnimNone' },
  { value: 'idle', labelKey: 'settings.skinAnimIdle' },
  { value: 'walk', labelKey: 'settings.skinAnimWalk' },
  { value: 'run', labelKey: 'settings.skinAnimRun' },
  { value: 'fly', labelKey: 'settings.skinAnimFly' },
  { value: 'wave', labelKey: 'settings.skinAnimWaveRight' },
  { value: 'wave_left', labelKey: 'settings.skinAnimWaveLeft' },
  { value: 'crouch', labelKey: 'settings.skinAnimCrouch' },
  { value: 'hit', labelKey: 'settings.skinAnimHit' }
]

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

/**
 * Une texture haute résolution par joueur : si on mettait en cache la première requête (ex. 28px),
 * l’agrandissement en 112px floutait tout. On demande toujours au moins 160px au service.
 */
const SKIN_HEAD_FETCH_PX = 256
const skinHeadCache = new Map<string, string>()
const SKIN_HEAD_CACHE_CAP = 48

function skinHeadCacheSet(uuid: string, dataUrl: string) {
  if (skinHeadCache.size >= SKIN_HEAD_CACHE_CAP) {
    const oldest = skinHeadCache.keys().next().value
    if (oldest !== undefined) skinHeadCache.delete(oldest)
  }
  skinHeadCache.set(uuid, dataUrl)
}

function SkinHead({
  uuid,
  sizePx: _displaySizeHint,
  className
}: {
  uuid: string | undefined | null
  /** Conservé pour l’API ; la résolution réseau est fixe (évite cache basse déf). */
  sizePx: number
  className?: string
}) {
  void _displaySizeHint
  const [src, setSrc] = useState<string>(LOGO)

  useEffect(() => {
    if (!uuid?.trim()) {
      setSrc(LOGO)
      return
    }
    const id = uuid.trim()
    const hit = skinHeadCache.get(id)
    if (hit) {
      setSrc(hit)
      return
    }
    let cancelled = false
    void window.solea.getSkinHead(id, SKIN_HEAD_FETCH_PX).then((dataUrl) => {
      if (cancelled) return
      const next = dataUrl ?? LOGO
      skinHeadCacheSet(id, next)
      setSrc(next)
    })
    return () => {
      cancelled = true
    }
  }, [uuid])

  const cls = className ? `mc-face-img ${className}` : 'mc-face-img'
  return <img src={src} alt="" className={cls} />
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
  onRefresh,
  onSkinAnimationChange
}: {
  uuid: string
  refreshKey: number
  playerName: string
  viewerBackground: string
  skinAnim: SkinViewerAnimation
  reduceMotion: boolean
  onRefresh: () => void
  onSkinAnimationChange: (v: SkinViewerAnimation) => void
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
          <label className="account-skin-anim-field">
            <span className="account-skin-anim-label">{t('settings.skinAnim')}</span>
            <LauncherSelect
              value={skinAnim}
              onChange={(v) => onSkinAnimationChange(v as SkinViewerAnimation)}
              options={SKIN_ANIM_UI_OPTIONS.map((o) => ({ value: o.value, label: t(o.labelKey) }))}
            />
          </label>
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

function serializeLauncherSettingsForIpc(settings: LauncherSettingsUI): LauncherSettingsUI {
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
  return {
    ...settings,
    modpackProfiles,
    memoryMin: ap.memoryMin,
    memoryMax: ap.memoryMax,
    gameArgs: ap.gameArgs,
    screenWidth: ap.screenWidth,
    screenHeight: ap.screenHeight,
    fullscreen: ap.fullscreen
  }
}

function emptySettings(): LauncherSettingsUI {
  const packs = emptyPackProfile()
  return {
    memoryMin: '2G',
    memoryMax: '6G',
    jvmArgs: '',
    gameArgs: '',
    downloadThreads: 12,
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
    discordRichPresence: true,
    updateChannel: 'stable',
    skinViewerAnimation: 'none',
    uiShortcutOpenSettings: 'CommandOrControl+Comma',
    uiShortcutGoNews: 'CommandOrControl+Shift+KeyH',
    uiShortcutGoAccount: 'CommandOrControl+Shift+KeyU',
    nativeNotifications: true,
    diagnosticLaunch: false,
    networkSlowDownloads: false,
    uiChromeGlass: false
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
  label,
  description
}: {
  checked: boolean
  onChange: (next: boolean) => void
  label: string
  description?: string
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
      <span className="settings-toggle-text">
        {label}
        {description ? <span className="settings-toggle-desc">{description}</span> : null}
      </span>
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

function IconScreenshots() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
      <rect x="3" y="5" width="18" height="14" rx="2" />
      <path d="M7 15l3-3 3 3 4-5" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx="8.5" cy="9.5" r="1" fill="currentColor" stroke="none" />
    </svg>
  )
}

function IconDiscord({ className }: { className?: string } = {}) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028 14.09 14.09 0 0 0 1.226-1.994.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.956-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.946 2.418-2.157 2.418z" />
    </svg>
  )
}

const DISCORD_INVITE_URL = 'https://discord.gg/jVGq5aZ6Wc'

/** Catégories rapport instance (jeu / modpack). */
const REPORT_INSTANCE_CATEGORIES = ['launch', 'install', 'account', 'verify', 'mods', 'other'] as const
/** Catégories rapport launcher (app). */
const REPORT_LAUNCHER_CATEGORIES = ['ui', 'login', 'updates', 'downloads', 'performance', 'other'] as const

type NewsHubSocialId = 'modrinth' | 'youtube' | 'x' | 'discord' | 'bmc'

/** Liens onglet Accueil (ordre d’affichage). Chaîne vide = bouton masqué. */
const NEWS_HUB_SOCIAL_DEF: { id: NewsHubSocialId; url: string }[] = [
  { id: 'modrinth', url: 'https://modrinth.com/organization/soleapixel' },
  { id: 'youtube', url: 'https://www.youtube.com/@SILWOX' },
  { id: 'x', url: 'https://x.com/Silwox_OFF' },
  { id: 'discord', url: DISCORD_INVITE_URL },
  { id: 'bmc', url: 'https://buymeacoffee.com/silwox' }
]

function newsHubSocialRows(): { id: NewsHubSocialId; url: string }[] {
  return NEWS_HUB_SOCIAL_DEF.filter((r) => r.url.trim().length > 0)
}

function IconYouTube({ className }: { className?: string } = {}) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M23.5 6.2a3 3 0 00-2.1-2.1C19.5 3.6 12 3.6 12 3.6s-7.5 0-9.4.5A3 3 0 00.5 6.2 30 30 0 000 12a30 30 0 00.5 5.8 3 3 0 002.1 2.1c1.9.5 9.4.5 9.4.5s7.5 0 9.4-.5a3 3 0 002.1-2.1 30 30 0 00.5-5.8 30 30 0 00-.5-5.8zM9.6 15.5V8.5L15.8 12l-6.2 3.5z" />
    </svg>
  )
}

function IconXLogo({ className }: { className?: string } = {}) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
    </svg>
  )
}

function IconModrinth({ className }: { className?: string } = {}) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M12 2l8 4.6v10.8L12 22l-8-4.6V6.6L12 2zm0 2.2L6.2 7.35v9.3L12 19.8l5.8-4.15v-9.3L12 4.2zm0 3.1l4.65 2.65v5.1L12 17.7l-4.65-2.65v-5.1L12 7.3z" />
    </svg>
  )
}

function IconBuyMeACoffee({ className }: { className?: string } = {}) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M6 9h11v1.2c0 2.2-1.5 4-3.4 4.3l-.3 2.5H9.7l-.3-2.5C7.5 14.2 6 12.4 6 10.2V9zm2 .8v.4c0 1.4.9 2.6 2.2 2.9h3.6c1.3-.3 2.2-1.5 2.2-2.9v-.4H8zm12.2-.5h1.1c.6 0 1 .5 1 1.1 0 .6-.4 1.1-1 1.1h-1.1V9.3zM7.5 6h9v1.2h-9V6z" />
    </svg>
  )
}

const NEWS_HUB_LABEL_KEYS: Record<NewsHubSocialId, string> = {
  modrinth: 'newsView.socialModrinth',
  youtube: 'newsView.socialYoutube',
  x: 'newsView.socialX',
  discord: 'newsView.socialDiscord',
  bmc: 'newsView.socialBmc'
}

function newsHubSocialLabelKey(id: NewsHubSocialId): string {
  return NEWS_HUB_LABEL_KEYS[id]
}

/** Rend les segments **gras** du changelog (données statiques). */
function formatChangelogLine(line: string): ReactNode {
  const nodes: ReactNode[] = []
  const re = /\*\*([^*]+)\*\*/g
  let last = 0
  let m: RegExpExecArray | null
  let k = 0
  while ((m = re.exec(line)) !== null) {
    if (m.index > last) nodes.push(line.slice(last, m.index))
    nodes.push(<strong key={`cl${k++}`}>{m[1]}</strong>)
    last = m.index + m[0].length
  }
  if (last < line.length) nodes.push(line.slice(last))
  return nodes.length > 0 ? <>{nodes}</> : line
}

type ChangelogKind = 'added' | 'changed' | 'removed' | 'fixed'

function ChangelogKindIcon({ kind }: { kind: ChangelogKind }) {
  const cls = 'news-hub-kind-icon'
  switch (kind) {
    case 'added':
      return (
        <svg className={cls} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" aria-hidden>
          <path d="M12 5v14M5 12h14" strokeLinecap="round" />
        </svg>
      )
    case 'changed':
      return (
        <svg className={cls} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
          <path d="M4 9h12M9 5l-4 4 4 4" strokeLinecap="round" strokeLinejoin="round" />
          <path d="M20 15H8M15 11l4 4-4 4" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      )
    case 'removed':
      return (
        <svg className={cls} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" aria-hidden>
          <path d="M5 12h14" strokeLinecap="round" />
        </svg>
      )
    case 'fixed':
      return (
        <svg className={cls} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
          <path
            d="M9 11l3 3L22 4M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      )
  }
}

function NewsHubSocialIcon({
  id,
  className
}: {
  id: NewsHubSocialId
  className?: string
}) {
  const icons: Record<NewsHubSocialId, ReactNode> = {
    modrinth: <IconModrinth className={className} />,
    youtube: <IconYouTube className={className} />,
    discord: <IconDiscord className={className} />,
    x: <IconXLogo className={className} />,
    bmc: <IconBuyMeACoffee className={className} />
  }
  return icons[id]
}

function IconNewsHubReport({ className }: { className?: string } = {}) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z" />
    </svg>
  )
}

/** Libellé affiché dans l’UI (détail visuel « | Release » ; semver = package.json / getVersion). */
const LAUNCHER_VERSION_DISPLAY = '26.1.4 | Release'

function formatBytes(n: number): string {
  if (!Number.isFinite(n) || n < 0) return '—'
  if (n < 1024) return `${Math.round(n)} o`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} Ko`
  if (n < 1024 ** 3) return `${(n / 1024 / 1024).toFixed(1)} Mo`
  return `${(n / 1024 ** 3).toFixed(2)} Go`
}

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

function IconChevronDown({ className }: { className?: string } = {}) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M6 9l6 6 6-6"
        stroke="currentColor"
        strokeWidth="2.25"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function IconCheckMenu({ className }: { className?: string } = {}) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M20 6L9 17l-5-5"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function IconPlusMenu({ className }: { className?: string } = {}) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M12 5v14M5 12h14"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
    </svg>
  )
}

function IconTrashMenu({ className }: { className?: string } = {}) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M3 6h18M9 6V4h6v2m-7 5v9a2 2 0 002 2h4a2 2 0 002-2v-9M10 11v6M14 11v6"
        stroke="currentColor"
        strokeWidth="1.85"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
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

function FlagUs({ className }: { className?: string } = {}) {
  return (
    <svg className={className} viewBox="0 0 20 14" aria-hidden>
      <rect width="20" height="14" rx="1.5" fill="#b22234" />
      <path fill="#fff" d="M0 2h20v1.5H0V2zm0 3h20v1.5H0V5zm0 3h20v1.5H0V8zm0 3h20v1.5H0V11zm0 3h14v1.5H0z" />
      <rect width="8.5" height="7.5" fill="#3c3b6e" />
      <g fill="#fff">
        <circle cx="1.4" cy="1.25" r="0.45" />
        <circle cx="3.4" cy="1.25" r="0.45" />
        <circle cx="5.4" cy="1.25" r="0.45" />
        <circle cx="7.4" cy="1.25" r="0.45" />
        <circle cx="2.4" cy="2.5" r="0.45" />
        <circle cx="4.4" cy="2.5" r="0.45" />
        <circle cx="6.4" cy="2.5" r="0.45" />
        <circle cx="1.4" cy="3.75" r="0.45" />
        <circle cx="3.4" cy="3.75" r="0.45" />
        <circle cx="5.4" cy="3.75" r="0.45" />
        <circle cx="7.4" cy="3.75" r="0.45" />
        <circle cx="2.4" cy="5" r="0.45" />
        <circle cx="4.4" cy="5" r="0.45" />
        <circle cx="6.4" cy="5" r="0.45" />
        <circle cx="1.4" cy="6.25" r="0.45" />
        <circle cx="3.4" cy="6.25" r="0.45" />
        <circle cx="5.4" cy="6.25" r="0.45" />
        <circle cx="7.4" cy="6.25" r="0.45" />
      </g>
    </svg>
  )
}

function FlagFr({ className }: { className?: string } = {}) {
  return (
    <svg className={className} viewBox="0 0 20 14" aria-hidden>
      <rect width="20" height="14" rx="1.5" fill="#202020" />
      <rect x="0" y="0" width="6.67" height="14" fill="#002395" />
      <rect x="6.67" y="0" width="6.66" height="14" fill="#fff" />
      <rect x="13.33" y="0" width="6.67" height="14" fill="#e1000f" />
    </svg>
  )
}

function LoginGate({
  testMode,
  onLoggedIn,
  onPersistLocale
}: {
  testMode: boolean
  onLoggedIn: () => void
  onPersistLocale: (l: 'en' | 'fr') => void
}) {
  const { t, locale, setLocale } = useI18n()
  const [busy, setBusy] = useState(false)
  const [msg, setMsg] = useState<string | null>(null)
  const [langOpen, setLangOpen] = useState(false)
  const [infoOpen, setInfoOpen] = useState(false)

  useEffect(() => {
    if (!langOpen) return
    const close = () => setLangOpen(false)
    document.addEventListener('click', close)
    return () => document.removeEventListener('click', close)
  }, [langOpen])

  const pickLang = (l: 'en' | 'fr') => {
    setLocale(l)
    setLangOpen(false)
    onPersistLocale(l)
    void window.solea.saveSettings({ uiLanguage: l })
  }

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
      <div className="login-lang-switch" onClick={(e) => e.stopPropagation()}>
        <button
          type="button"
          className="login-lang-trigger"
          aria-expanded={langOpen}
          aria-haspopup="listbox"
          aria-label={t('login.langAria')}
          onClick={(e) => {
            e.stopPropagation()
            setLangOpen((o) => !o)
          }}
        >
          <span className="login-lang-flag" aria-hidden>
            {locale === 'fr' ? <FlagFr /> : <FlagUs />}
          </span>
          <span className="login-lang-code">{locale === 'fr' ? 'FR' : 'US'}</span>
          <svg className="login-lang-chev" viewBox="0 0 24 24" fill="none" aria-hidden>
            <path
              d="M6 9l6 6 6-6"
              stroke="currentColor"
              strokeWidth="2.25"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>
        {langOpen && (
          <ul className="login-lang-menu" role="listbox" aria-label={t('login.langAria')}>
            <li role="none">
              <button
                type="button"
                role="option"
                aria-selected={locale === 'en'}
                className={`login-lang-option${locale === 'en' ? ' is-active' : ''}`}
                onClick={() => pickLang('en')}
              >
                <span className="login-lang-flag" aria-hidden>
                  <FlagUs />
                </span>
                <span className="login-lang-option-label">
                  <span className="login-lang-option-code">US</span>
                  <span className="login-lang-option-name">{t('login.langEnglish')}</span>
                </span>
              </button>
            </li>
            <li role="none">
              <button
                type="button"
                role="option"
                aria-selected={locale === 'fr'}
                className={`login-lang-option${locale === 'fr' ? ' is-active' : ''}`}
                onClick={() => pickLang('fr')}
              >
                <span className="login-lang-flag" aria-hidden>
                  <FlagFr />
                </span>
                <span className="login-lang-option-label">
                  <span className="login-lang-option-code">FR</span>
                  <span className="login-lang-option-name">{t('login.langFrench')}</span>
                </span>
              </button>
            </li>
          </ul>
        )}
      </div>
      <button
        type="button"
        className="login-info-trigger"
        aria-label={t('login.infoAria')}
        aria-haspopup="dialog"
        aria-expanded={infoOpen}
        aria-controls="login-info-dialog"
        onClick={(e) => {
          e.stopPropagation()
          setInfoOpen(true)
        }}
      >
        <span className="login-info-trigger-icon" aria-hidden>
          i
        </span>
        <span>{t('login.infoTrigger')}</span>
      </button>
      <LoginInfoModal open={infoOpen} onClose={() => setInfoOpen(false)} />
      <div className="login-card">
        <img src={SOLEA_LOGIN_LOGO} alt="" className="login-wordmark" />
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
  const { t, setLocale, formatPercent, formatDate } = useI18n()
  const { pushToast } = useToast()
  const [screen, setScreen] = useState<'boot' | 'login' | 'app'>('boot')
  const [testMode, setTestMode] = useState(false)
  const [modpackName, setModpackName] = useState('Palamod Recreated')
  /** Par défaut : onglet Accueil (actus). Les modpacks restent sur `home`. */
  const [view, setView] = useState<'home' | 'news' | 'settings' | 'account' | 'screenshots'>('news')
  const [settingsTab, setSettingsTab] = useState<'launcher' | ModpackIdUi>('launcher')
  const [shortcutCapture, setShortcutCapture] = useState<null | 'open' | 'news' | 'account'>(null)
  /** Onglet modpack dont le panneau lourd (RAM, etc.) est monté — retardé pour éviter le freeze au clic. */
  const [modpackSettingsReadyId, setModpackSettingsReadyId] = useState<ModpackIdUi | null>(null)
  const modpackPanelRaf2Ref = useRef(0)
  const settingsDebugTapRef = useRef({ n: 0, until: 0 })
  const [accounts, setAccounts] = useState<{ uuid: string; name: string }[]>([])
  const [activeAcc, setActiveAcc] = useState<{ name: string; uuid: string } | null>(null)
  const [menuOpen, setMenuOpen] = useState(false)
  const [phase, setPhase] = useState<'idle' | 'installing' | 'busy'>('idle')
  const [installLine, setInstallLine] = useState('')
  const [installPct, setInstallPct] = useState(0)
  const [settings, setSettings] = useState<LauncherSettingsUI>(emptySettings)
  const [settingsFb, setSettingsFb] = useState<{ text: string; ok: boolean } | null>(null)
  const [launchPhase, setLaunchPhase] = useState<'idle' | 'launching' | 'running'>('idle')
  const [launchDots, setLaunchDots] = useState(1)
  const [memoryStats, setMemoryStats] = useState<{ totalGiB: number } | null>(null)
  const [packInstanceDetails, setPackInstanceDetails] = useState<{
    installed: boolean
    sizeBytes: number | null
  } | null>(null)
  const [cacheStats, setCacheStats] = useState<{
    gradleCachesBytes: number
    launcherLogsBytes: number
  } | null>(null)
  const [allModpacksAction, setAllModpacksAction] = useState<ModpackActionInfoRow[] | null>(null)
  const [showModpackUpdatesModal, setShowModpackUpdatesModal] = useState(false)
  const modpackUpdatesModalShownRef = useRef(false)
  const [activeModpackId, setActiveModpackId] = useState<ModpackIdUi>('palamod-recreated')
  const [modpacksList, setModpacksList] = useState<{ id: string; displayName: string }[]>([])
  const [packSwitching, setPackSwitching] = useState(false)
  const [verifyResult, setVerifyResult] = useState<
    | null
    | { ok: true }
    | { ok: false; reason: string; detail?: string; paths?: string[] }
  >(null)
  const [packMaintBusy, setPackMaintBusy] = useState(false)
  const [packMaintConfirm, setPackMaintConfirm] = useState<
    null | { kind: 'reinstall' | 'uninstall'; packId: ModpackIdUi }
  >(null)
  const [cacheClearConfirm, setCacheClearConfirm] = useState<null | CacheClearKind>(null)
  const [launcherVersion, setLauncherVersion] = useState('')
  const [modpackActivityById, setModpackActivityById] = useState<
    Record<string, { lastPlayAt?: string; lastInstallAt?: string }>
  >({})
  const [bootProgress, setBootProgress] = useState(0)
  const [bootDots, setBootDots] = useState(1)
  const [accountSkinKey, setAccountSkinKey] = useState(0)
  const [accountFb, setAccountFb] = useState<string | null>(null)
  const [accountSessionWarn, setAccountSessionWarn] = useState(false)
  const [reportModalOpen, setReportModalOpen] = useState(false)
  const [reportScope, setReportScope] = useState<'launcher' | 'instance'>('instance')
  const [reportHelpOpen, setReportHelpOpen] = useState(false)
  const [reportModpackId, setReportModpackId] = useState<string>('palamod-recreated')
  const [reportCategory, setReportCategory] = useState<string>('launch')
  const [reportDetails, setReportDetails] = useState('')
  const [reportIncludeTech, setReportIncludeTech] = useState(true)
  const [reportSending, setReportSending] = useState(false)
  const [settingsGlossaryKey, setSettingsGlossaryKey] = useState<SettingsGlossaryKey | null>(null)
  const [updateDownloaded, setUpdateDownloaded] = useState(false)
  const [prefersRm, setPrefersRm] = useState(
    () => typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
  )
  const settingsRef = useRef(settings)
  settingsRef.current = settings
  const manualUpdateCheckRef = useRef(false)
  const installBusyRef = useRef(false)
  const launchBusyRef = useRef(false)
  const reportModalRef = useRef<HTMLDivElement>(null)
  const modpackUi = useMemo(() => {
    if (allModpacksAction === null) {
      return {
        loading: true,
        needsInstall: false,
        needsUpdate: false,
        error: undefined as string | undefined
      }
    }
    const cur = allModpacksAction.find((p) => p.id === activeModpackId)
    if (!cur) {
      return {
        loading: false,
        needsInstall: true,
        needsUpdate: false,
        error: undefined as string | undefined
      }
    }
    return {
      loading: false,
      needsInstall: cur.needsInstall,
      needsUpdate: cur.needsUpdate,
      error: cur.error
    }
  }, [allModpacksAction, activeModpackId])

  const packNeedsAction = modpackUi.needsInstall || modpackUi.needsUpdate

  const reportInstanceSelectOptions = useMemo(
    () =>
      modpacksList
        .filter((m) => isModpackId(m.id))
        .map((m) => ({ value: m.id, label: m.displayName })),
    [modpacksList]
  )

  const reportCategorySelectOptions = useMemo(() => {
    if (reportScope === 'launcher') {
      return REPORT_LAUNCHER_CATEGORIES.map((value) => ({
        value,
        label: t(`home.report.catLauncher.${value}`)
      }))
    }
    return REPORT_INSTANCE_CATEGORIES.map((value) => ({
      value,
      label: t(`home.report.cat.${value}`)
    }))
  }, [reportScope, t])

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

  const chromeWallpaperUrl = useMemo(() => {
    if (view === 'settings') return LOGIN_WALLPAPER
    if (view === 'news' || view === 'screenshots') return NEWS_WALLPAPER
    if (view === 'home' && isModpackId(activeModpackId)) return MODPACK_THEME[activeModpackId].wallpaper
    if (isModpackId(activeModpackId)) return MODPACK_THEME[activeModpackId].wallpaper
    return NEWS_WALLPAPER
  }, [view, activeModpackId])

  const persistSkinViewerAnimation = useCallback(
    async (v: SkinViewerAnimation) => {
      const merged = { ...settingsRef.current, skinViewerAnimation: v }
      setSettings(merged)
      const r = await window.solea.saveSettings(serializeLauncherSettingsForIpc(merged))
      if (!r.ok) {
        pushToast(r.error, 'error')
        void window.solea.getSettings().then(setSettings)
      }
    },
    [pushToast]
  )

  const refreshAllModpacksAction = useCallback(async () => {
    try {
      const r = await window.solea.getAllModpacksActionInfo()
      setAllModpacksAction(r.packs)
    } catch {
      setAllModpacksAction([])
    }
  }, [])

  useEffect(() => {
    if (!allModpacksAction?.length) return
    const hasOutdated = allModpacksAction.some((p) => p.needsUpdate && !p.needsInstall)
    if (!hasOutdated || modpackUpdatesModalShownRef.current) return
    modpackUpdatesModalShownRef.current = true
    setShowModpackUpdatesModal(true)
  }, [allModpacksAction])

  const refreshModpackActivity = useCallback(async () => {
    const m = await window.solea.getModpackActivity()
    setModpackActivityById(m)
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
    if (screen !== 'app' || view !== 'home') return
    void refreshModpackActivity()
  }, [screen, view, refreshModpackActivity])

  useEffect(() => {
    if (screen !== 'app' || view !== 'home' || !activeAcc) {
      setAccountSessionWarn(false)
      return
    }
    let cancelled = false
    void window.solea.refreshActiveAccount().then((r) => {
      if (cancelled) return
      setAccountSessionWarn(!r.ok)
    })
    return () => {
      cancelled = true
    }
  }, [screen, view, activeAcc?.uuid])

  useEffect(() => {
    if (!settingsGlossaryKey || view !== 'settings') return
    const onDoc = (e: MouseEvent) => {
      const tgt = e.target
      if (tgt instanceof Element && tgt.closest('.settings-glossary-wrap')) return
      setSettingsGlossaryKey(null)
    }
    document.addEventListener('mousedown', onDoc)
    return () => document.removeEventListener('mousedown', onDoc)
  }, [settingsGlossaryKey, view])

  useEffect(() => {
    if (!settingsGlossaryKey) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setSettingsGlossaryKey(null)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [settingsGlossaryKey])

  useEffect(() => {
    if (view !== 'settings') setSettingsGlossaryKey(null)
  }, [view])

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
    const settingsP = window.solea
      .getSettings()
      .then((s) => {
        if (!cancelled) setSettings(s)
      })
      .catch(() => {})

    void Promise.all([pathsP, authP, settingsP])
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
    void refreshAllModpacksAction()
    void window.solea.getAppVersion().then(setLauncherVersion)
    void window.solea.isGameRunning().then((run) => {
      if (run) setLaunchPhase('running')
    })
    void window.solea.getPaths().then((p) => {
      if (p.modpackDisplayName) setModpackName(p.modpackDisplayName)
      if (p.modpacks?.length) setModpacksList(p.modpacks)
      if (p.activeModpackId && isModpackId(p.activeModpackId)) setActiveModpackId(p.activeModpackId)
    })
  }, [screen, loadAccounts, refreshAllModpacksAction])

  useEffect(() => {
    if (screen !== 'app') return
    const offA = window.solea.onUpdaterAvailable((p) => {
      manualUpdateCheckRef.current = false
      pushToast(t('updater.available', { version: p.version }), 'info', 18_000, {
        label: t('updater.toastDownloadNow'),
        onClick: () => {
          void window.solea.downloadUpdate().then((r) => {
            if (!r.ok) pushToast(r.error, 'error')
          })
        }
      })
    })
    const offN = window.solea.onUpdaterNotAvailable(() => {
      if (!manualUpdateCheckRef.current) return
      manualUpdateCheckRef.current = false
      pushToast(t('updater.none'), 'success')
    })
    const offD = window.solea.onUpdaterDownloaded(() => {
      setUpdateDownloaded(true)
      pushToast(t('updater.downloaded'), 'success', 14_000, {
        label: t('updater.restartNow'),
        onClick: () => {
          void window.solea.quitAndInstall()
        }
      })
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
    setView('home')
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
    await refreshAllModpacksAction()
    void refreshModpackActivity()
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
    void window.solea.getMemoryStats().then(
      (s) => setMemoryStats({ totalGiB: s.totalGiB }),
      () => setMemoryStats({ totalGiB: 16 })
    )
  }, [])

  useEffect(() => {
    cancelAnimationFrame(modpackPanelRaf2Ref.current)
    modpackPanelRaf2Ref.current = 0
    if (view !== 'settings' || settingsTab === 'launcher' || !isModpackId(settingsTab)) {
      setModpackSettingsReadyId(null)
      return
    }
    setModpackSettingsReadyId(null)
    const target = settingsTab
    let cancelled = false
    const id1 = requestAnimationFrame(() => {
      if (cancelled) return
      modpackPanelRaf2Ref.current = requestAnimationFrame(() => {
        modpackPanelRaf2Ref.current = 0
        if (!cancelled) setModpackSettingsReadyId(target)
      })
    })
    return () => {
      cancelled = true
      cancelAnimationFrame(id1)
      cancelAnimationFrame(modpackPanelRaf2Ref.current)
      modpackPanelRaf2Ref.current = 0
    }
  }, [view, settingsTab])

  useEffect(() => {
    if (view !== 'settings' || !isModpackId(settingsTab)) {
      setPackInstanceDetails(null)
      return
    }
    const packId = settingsTab
    setPackInstanceDetails(null)
    let cancelled = false
    void window.solea.getModpackInstanceDetails(packId).then((d) => {
      if (cancelled) return
      setPackInstanceDetails({ installed: d.installed, sizeBytes: d.sizeBytes })
    })
    return () => {
      cancelled = true
    }
  }, [view, settingsTab, packMaintBusy, phase])

  useEffect(() => {
    if (view !== 'settings' || settingsTab !== 'launcher') {
      setCacheStats(null)
      return
    }
    let cancelled = false
    void window.solea.getCacheStats().then((s) => {
      if (!cancelled) setCacheStats(s)
    })
    return () => {
      cancelled = true
    }
  }, [view, settingsTab])

  useEffect(() => {
    if (launchPhase !== 'launching') {
      setLaunchDots(1)
      return
    }
    if (settings.uiReduceMotion) return
    const id = window.setInterval(() => {
      setLaunchDots((d) => (d % 3) + 1)
    }, 450)
    return () => window.clearInterval(id)
  }, [launchPhase, settings.uiReduceMotion])

  useEffect(() => {
    if (screen !== 'boot') {
      setBootDots(1)
      return
    }
    if (reduceMotionEffective) return
    const id = window.setInterval(() => {
      setBootDots((d) => (d % 3) + 1)
    }, 450)
    return () => window.clearInterval(id)
  }, [screen, reduceMotionEffective])

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
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') close()
    }
    document.addEventListener('click', close)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('click', close)
      document.removeEventListener('keydown', onKey)
    }
  }, [menuOpen])

  useEffect(() => {
    if (screen !== 'app') return
    const onKey = (e: KeyboardEvent) => {
      if (e.repeat) return
      const s = settingsRef.current
      if (acceleratorMatches(s.uiShortcutOpenSettings, e)) {
        e.preventDefault()
        setView('settings')
        setSettingsTab('launcher')
        return
      }
      if (acceleratorMatches(s.uiShortcutGoNews, e)) {
        e.preventDefault()
        setView('news')
        return
      }
      if (acceleratorMatches(s.uiShortcutGoAccount, e)) {
        e.preventDefault()
        setView('account')
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

  const homeActivityLabels = useMemo(() => {
    const a = isModpackId(activeModpackId) ? modpackActivityById[activeModpackId] : undefined
    const fmt = (iso?: string) => {
      if (!iso?.trim()) return null as string | null
      const d = new Date(iso)
      return Number.isNaN(d.getTime()) ? null : formatDate(d)
    }
    return { lastPlay: fmt(a?.lastPlayAt), lastInstall: fmt(a?.lastInstallAt) }
  }, [activeModpackId, modpackActivityById, formatDate])

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

  const patchPackRamFromSliderGb = (packId: ModpackIdUi, gb: number) => {
    patchPackProfile(packId, allocGbToMinMaxStrings(gb))
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
        void playUiSound('install', uiSoundPrefs)
        void refreshAllModpacksAction()
        void refreshModpackActivity()
      } else pushToast(r.error, 'error')
    } finally {
      setPhase('idle')
      installBusyRef.current = false
    }
  }

  const executeReinstallModpack = async (packId: ModpackIdUi) => {
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
    void playUiSound('install', uiSoundPrefs)
    void refreshAllModpacksAction()
    void refreshModpackActivity()
  }

  const executeUninstallModpack = async (packId: ModpackIdUi) => {
    setPackMaintBusy(true)
    const r = await window.solea.uninstallModpack(packId)
    setPackMaintBusy(false)
    if (!r.ok) {
      pushToast(r.error, 'error')
      return
    }
    pushToast(t('toast.uninstalled'), 'success')
    void refreshAllModpacksAction()
  }

  const onReinstallModpack = (packId: ModpackIdUi) => {
    setPackMaintConfirm({ kind: 'reinstall', packId })
  }

  const onUninstallModpack = (packId: ModpackIdUi) => {
    setPackMaintConfirm({ kind: 'uninstall', packId })
  }

  const onPackMaintConfirmResolved = () => {
    if (!packMaintConfirm) return
    const { kind, packId } = packMaintConfirm
    setPackMaintConfirm(null)
    void (kind === 'reinstall' ? executeReinstallModpack(packId) : executeUninstallModpack(packId))
  }

  const onCacheClearConfirmResolved = () => {
    if (!cacheClearConfirm) return
    const kind = cacheClearConfirm
    setCacheClearConfirm(null)
    const cacheKey = kind === 'gradle' ? 'gradleCaches' : 'launcherLogs'
    void window.solea.clearCache(cacheKey).then((r) => {
      if (r.ok) {
        pushToast(t('settings.cacheFreed', { n: formatBytes(r.freedBytes) }), 'success')
        void window.solea.getCacheStats().then(setCacheStats)
      } else pushToast(r.error, 'error')
    })
  }

  const bumpSettingsDebugTap = useCallback(() => {
    const r = settingsDebugTapRef.current
    const now = Date.now()
    if (now > r.until) r.n = 0
    r.n += 1
    r.until = now + SETTINGS_DEBUG_WINDOW_MS
    if (r.n < SETTINGS_DEBUG_TAPS) return
    r.n = 0
    void window.solea.openDebugWindow().catch((err) => {
      pushToast(err instanceof Error ? err.message : String(err), 'error')
    })
  }, [pushToast])

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
      void playUiSound('launch', uiSoundPrefs)
      void refreshModpackActivity()
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

  const onVerifyRepair = () => {
    if (modpackUi.needsInstall || modpackUi.needsUpdate) {
      void onInstall()
      return
    }
    if (isModpackId(activeModpackId)) onReinstallModpack(activeModpackId)
  }

  const buildReportBody = useCallback(async () => {
    const paths = await window.solea.getPaths()
    const catLabel =
      reportScope === 'launcher'
        ? t(`home.report.catLauncher.${reportCategory}`)
        : t(`home.report.cat.${reportCategory}`)
    const instLabel =
      modpacksList.find((m) => m.id === reportModpackId)?.displayName ?? reportModpackId
    const scopeLine =
      reportScope === 'launcher'
        ? `**${t('home.report.md.scope')}:** ${t('home.report.scopeLauncher')}`
        : `**${t('home.report.md.scope')}:** ${t('home.report.scopeInstance')}`
    const instanceLine =
      reportScope === 'launcher'
        ? `**${t('home.report.md.instance')}:** ${t('home.report.md.instanceNA')}`
        : `**${t('home.report.md.instance')}:** ${instLabel} (\`${reportModpackId}\`)`
    const lines = [
      '## Solea Pixel — Report',
      scopeLine,
      instanceLine,
      `**${t('home.report.md.category')}:** ${catLabel}`,
      '',
      reportDetails.trim() || '(no details)',
      ''
    ]
    if (reportIncludeTech) {
      lines.push(
        '---',
        `**Launcher:** ${launcherVersion.trim() || LAUNCHER_VERSION_DISPLAY}`,
        `**UI pack (context):** ${modpackName} (${activeModpackId})`,
        `**OS:** ${navigator.userAgent}`
      )
      if (reportScope === 'instance') {
        lines.push(`**Modrinth:** ${paths.homeLinks?.modrinthUrl ?? '—'}`)
      }
    }
    return lines.join('\n')
  }, [
    reportScope,
    reportCategory,
    reportDetails,
    reportIncludeTech,
    launcherVersion,
    modpackName,
    activeModpackId,
    reportModpackId,
    modpacksList,
    t
  ])

  const openReportModal = () => {
    setReportScope('instance')
    setReportHelpOpen(false)
    setReportModpackId(activeModpackId)
    setReportCategory('launch')
    setReportDetails('')
    setReportIncludeTech(true)
    setReportModalOpen(true)
  }

  const copyReportToClipboard = async () => {
    try {
      const body = await buildReportBody()
      await navigator.clipboard.writeText(body)
      pushToast(t('home.report.copied'), 'success')
    } catch {
      pushToast(t('home.report.clipboardFail'), 'error')
    }
  }

  const sendReportDiscord = async () => {
    setReportSending(true)
    try {
      const body = await buildReportBody()
      const r = await window.solea.submitReportDiscordWebhook(body)
      if (r.ok) {
        pushToast(t('home.report.sent'), 'success')
        setReportModalOpen(false)
      } else if (r.error === 'no_webhook_env') {
        pushToast(t('home.report.sendDisabled'), 'info')
      } else {
        pushToast(t('home.report.sendFail', { detail: r.error }), 'error')
      }
    } finally {
      setReportSending(false)
    }
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
    const r = await window.solea.saveSettings(serializeLauncherSettingsForIpc(settings))
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

  useFocusTrap(reportModalOpen, reportModalRef, {
    onEscape: () => setReportModalOpen(false)
  })

  const homePackReadyA11y =
    view === 'home' &&
    !modpackUi.loading &&
    !modpackUi.error &&
    !modpackUi.needsInstall &&
    !modpackUi.needsUpdate &&
    launchPhase === 'idle'

  if (screen === 'boot') {
    return (
      <div className="app-chrome">
        <div className="app-chrome-body">
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
              <span className="boot-progress-label" aria-live="polite">
                {t('boot.loadingBase')}
                {reduceMotionEffective ? '…' : '.'.repeat(bootDots)}
              </span>
            </div>
          </div>
        </div>
        </div>
      </div>
    )
  }

  if (screen === 'login') {
    return (
      <div className="app-chrome">
        <div className="app-chrome-body">
          <TitleBar />
          <div className="app-fill">
          <LoginGate
            testMode={testMode}
            onLoggedIn={() => {
              setScreen('app')
              setView('news')
            }}
            onPersistLocale={(l) => setSettings((s) => ({ ...s, uiLanguage: l }))}
          />
        </div>
        </div>
      </div>
    )
  }

  return (
    <div className="app-chrome">
      {settings.uiChromeGlass ? (
        <div
          key={view === 'home' && isModpackId(activeModpackId) ? activeModpackId : view}
          className="app-chrome-wallpaper"
          style={{ backgroundImage: `url(${chromeWallpaperUrl})` }}
          aria-hidden
        />
      ) : null}
      <div className="app-chrome-body">
        <TitleBar />
        <div className="app-fill">
    <div className="shell">
      <aside className="shell-sidebar">
        <div className="sb-top">
          <button
            type="button"
            className={`sb-btn ${view === 'news' ? 'active' : ''}`}
            title={t('shell.home')}
            aria-label={t('shell.home')}
            onClick={() => setView('news')}
          >
            <IconHome />
          </button>
          <button
            type="button"
            className={`sb-btn ${view === 'settings' ? 'active' : ''}`}
            title={t('shell.settings')}
            aria-label={t('shell.settings')}
            onClick={() => {
              setView('settings')
              bumpSettingsDebugTap()
            }}
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
            className={`sb-btn ${view === 'screenshots' ? 'active' : ''}`}
            title={t('shell.screenshots')}
            aria-label={t('shell.screenshots')}
            onClick={() => setView('screenshots')}
          >
            <IconScreenshots />
          </button>
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
        className={`shell-main ${
          view === 'settings' || view === 'account' ? 'settings-mode' : ''
        } ${view === 'news' || view === 'screenshots' ? 'shell-main-news' : ''} ${
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
        {(view === 'news' || view === 'screenshots') && (
          <div
            className="shell-main-wallpaper"
            style={{ backgroundImage: `url(${NEWS_WALLPAPER})` }}
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

                {!modpackUi.loading && modpackUi.error ? (
                  <p className="home-modpack-err" role="alert">
                    {modpackUi.error}
                  </p>
                ) : null}

                <div className="play-row">
                  {homePackReadyA11y ? (
                    <span id="home-pack-ready-sr" className="sr-only">
                      {t('home.packReady')}
                    </span>
                  ) : null}
                  {packNeedsAction ? (
                    <div className="play-row-primary">
                      <p className="home-pack-action-title">
                        {modpackUi.needsInstall ? t('home.ctaInstallTitle') : t('home.ctaUpdateTitle')}
                      </p>
                      <button
                        type="button"
                        className={`btn-play btn-play--pack-action${reduceMotionEffective ? '' : ' btn-play--pulse-soft'} btn-play--muted`}
                        disabled={phase !== 'idle' || launchPhase !== 'idle'}
                        aria-describedby={homePackReadyA11y ? 'home-pack-ready-sr' : undefined}
                        onClick={() => void onInstall()}
                      >
                        {modpackUi.needsInstall ? t('home.ctaInstallAction') : t('home.ctaUpdateAction')}
                      </button>
                    </div>
                  ) : (
                    <button
                      type="button"
                      className={`btn-play${launchPhase === 'running' ? ' btn-play-close' : ''}${
                        launchPhase === 'idle' && homePackReadyA11y && !reduceMotionEffective
                          ? ' btn-play--ready'
                          : ''
                      }${
                        launchPhase === 'idle' &&
                        !packNeedsAction &&
                        (modpackUi.loading || Boolean(modpackUi.error))
                          ? ' btn-play--muted'
                          : ''
                      }`}
                      disabled={
                        launchPhase === 'launching' ||
                        phase === 'installing' ||
                        phase === 'busy' ||
                        modpackUi.loading ||
                        modpackUi.needsInstall ||
                        modpackUi.needsUpdate
                      }
                      aria-describedby={homePackReadyA11y ? 'home-pack-ready-sr' : undefined}
                      aria-label={
                        launchPhase === 'launching'
                          ? t('home.playLaunchingAria')
                          : launchPhase === 'running'
                            ? t('home.playClose')
                            : t('home.play')
                      }
                      onClick={() => void onLaunchOrClose()}
                    >
                      {launchPhase === 'running' ? <IconStop /> : <IconPlay />}
                      {launchPhase === 'launching'
                        ? `${t('home.playLaunchingBase')}${settings.uiReduceMotion ? '…' : '.'.repeat(launchDots)}`
                        : launchPhase === 'running'
                          ? t('home.playClose')
                          : t('home.play')}
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
                  <div className="play-row-account">
                    <div className="profile-bar-wrap">
                        <button
                          type="button"
                          className={`profile-bar${menuOpen ? ' profile-bar--open' : ''}${
                            accountSessionWarn ? ' profile-bar--session-warn' : ''
                          }`}
                          aria-expanded={menuOpen}
                          aria-haspopup="menu"
                          aria-controls={menuOpen ? 'home-account-menu' : undefined}
                          title={accountSessionWarn ? t('home.account.sessionWarn') : undefined}
                          onClick={(e) => {
                            e.stopPropagation()
                            setMenuOpen((o) => !o)
                          }}
                        >
                          <span className="profile-bar-avatar-ring">
                            <SkinHead uuid={activeAcc?.uuid} sizePx={64} className="profile-bar-head" />
                          </span>
                          <span className="profile-bar-name">
                            {activeAcc?.name ?? t('home.profileFallback')}
                          </span>
                          <span className="profile-bar-chev" aria-hidden>
                            <IconChevronDown className="profile-bar-chev-svg" />
                          </span>
                        </button>
                        {menuOpen && (
                          <div
                            className="profile-menu"
                            id="home-account-menu"
                            role="menu"
                            aria-labelledby="home-account-menu-label"
                            onClick={(e) => e.stopPropagation()}
                          >
                            <div className="profile-menu-inner">
                              <div className="profile-menu-accent" aria-hidden />
                              <p className="profile-menu-kicker" id="home-account-menu-label">
                                {t('home.accountMenuHeading')}
                              </p>
                              <ul className="profile-menu-accounts" role="none">
                                {accounts.map((a) => {
                                  const sel = activeAcc?.uuid === a.uuid
                                  return (
                                    <li key={a.uuid} role="none">
                                      <button
                                        type="button"
                                        role="menuitemradio"
                                        aria-checked={sel}
                                        className={`profile-menu-account${sel ? ' profile-menu-account--active' : ''}`}
                                        onClick={() => void onSelectAccount(a.uuid)}
                                      >
                                        <span className="profile-menu-account-avatar">
                                          <SkinHead uuid={a.uuid} sizePx={40} className="profile-menu-head" />
                                        </span>
                                        <span className="profile-menu-account-name">{a.name}</span>
                                        <span
                                          className={`profile-menu-account-check${sel ? '' : ' profile-menu-account-check--empty'}`}
                                          aria-hidden
                                        >
                                          {sel ? <IconCheckMenu /> : null}
                                        </span>
                                      </button>
                                    </li>
                                  )
                                })}
                              </ul>
                              <div className="profile-menu-actions" role="none">
                                <button
                                  type="button"
                                  role="menuitem"
                                  className="profile-menu-action"
                                  onClick={() => void onAddAccount()}
                                >
                                  <span
                                    className="profile-menu-action-icon profile-menu-action-icon--accent"
                                    aria-hidden
                                  >
                                    <IconPlusMenu />
                                  </span>
                                  {t('home.addAccount')}
                                </button>
                                {activeAcc ? (
                                  <button
                                    type="button"
                                    role="menuitem"
                                    className="profile-menu-action profile-menu-action--danger"
                                    onClick={() => void onRemoveAccount(activeAcc.uuid)}
                                  >
                                    <span className="profile-menu-action-icon" aria-hidden>
                                      <IconTrashMenu />
                                    </span>
                                    {t('home.removeAccount')}
                                  </button>
                                ) : null}
                              </div>
                            </div>
                          </div>
                        )}
                      </div>
                    </div>
                </div>

                {verifyResult && (
                  <div
                    className={`verify-banner ${verifyResult.ok ? 'ok' : 'fail'} verify-banner--with-actions`}
                    role="status"
                    aria-live="polite"
                  >
                    <div className="verify-banner-body">
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
                    <div className="verify-banner-actions">
                      {!verifyResult.ok ? (
                        <>
                          <button
                            type="button"
                            className="btn-quiet verify-banner-btn"
                            disabled={phase !== 'idle' || launchPhase !== 'idle'}
                            onClick={() => void onVerify()}
                          >
                            {t('home.verify.retry')}
                          </button>
                          <button
                            type="button"
                            className="btn-quiet verify-banner-btn"
                            disabled={phase !== 'idle' || launchPhase !== 'idle' || packMaintBusy}
                            onClick={onVerifyRepair}
                          >
                            {t('home.verify.repair')}
                          </button>
                        </>
                      ) : null}
                      <button
                        type="button"
                        className="btn-quiet verify-banner-btn"
                        onClick={() => setVerifyResult(null)}
                      >
                        {t('home.verify.dismiss')}
                      </button>
                    </div>
                  </div>
                )}

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

                {launchPhase === 'launching' && (
                  <button
                    type="button"
                    className="btn-quiet home-log-console-btn"
                    onClick={() => void window.solea.openGameLogWindow()}
                  >
                    {t('home.logConsole')}
                  </button>
                )}

                {(homeActivityLabels.lastInstall || homeActivityLabels.lastPlay) && (
                  <div className="home-last-activity">
                    <p className="home-last-activity-label">{t('home.lastActivityTitle')}</p>
                    <ul className="home-last-activity-list">
                      {homeActivityLabels.lastInstall ? (
                        <li>
                          {t('home.lastInstall', { date: homeActivityLabels.lastInstall })}
                        </li>
                      ) : null}
                      {homeActivityLabels.lastPlay ? (
                        <li>{t('home.lastPlay', { date: homeActivityLabels.lastPlay })}</li>
                      ) : null}
                    </ul>
                  </div>
                )}
              </div>
            </div>
          </div>
          <footer className="shell-footer">{t('home.footer', { name: modpackName })}</footer>
          </>
        )}

        {view === 'news' && (
          <>
            <div className="shell-content shell-content-news news-hub-layout">
              {testMode && <div className="test-strip home-test-strip">{t('home.testStrip')}</div>}
              <header className="news-hub-page-hero">
                <h2 className="news-hub-page-title">{t('shell.news')}</h2>
                <p className="news-hub-page-tagline">{t('newsView.heroSubtitle')}</p>
              </header>
              <div className="news-hub-grid">
                <aside className="news-hub-col news-hub-col--profile">
                  <div className="news-hub-card news-hub-profile-card">
                    <header className="news-hub-profile-top">
                      <h3 className="news-hub-profile-heading">{t('account.title')}</h3>
                      <div className="news-hub-profile-user">
                        <div className="news-hub-skin-wrap">
                          <SkinHead uuid={activeAcc?.uuid} sizePx={112} className="news-hub-skin" />
                        </div>
                        <p className="news-hub-username">{activeAcc?.name ?? t('home.profileFallback')}</p>
                      </div>
                    </header>
                    <div className="news-hub-profile-mid">
                      <p className="news-hub-acc-nav-label">{t('newsView.accountsNav')}</p>
                      <nav className="news-hub-acc-list" aria-label={t('newsView.accountsNav')}>
                        {accounts.map((a) => (
                          <button
                            key={a.uuid}
                            type="button"
                            className={`news-hub-acc-tab ${activeAcc?.uuid === a.uuid ? 'on' : ''}`}
                            onClick={() => void onSelectAccount(a.uuid)}
                          >
                            <SkinHead uuid={a.uuid} sizePx={28} className="news-hub-acc-tab-head" />
                            <span className="news-hub-acc-tab-name">{a.name}</span>
                          </button>
                        ))}
                      </nav>
                    </div>
                    <div className="news-hub-acc-actions">
                      <button
                        type="button"
                        className="btn-quiet news-hub-acc-btn"
                        onClick={() => void onAddAccount()}
                      >
                        {t('home.addAccount')}
                      </button>
                      {activeAcc ? (
                        <button
                          type="button"
                          className="btn-quiet news-hub-acc-btn news-hub-acc-btn--danger"
                          onClick={() => void onRemoveAccount(activeAcc.uuid)}
                        >
                          {t('home.removeAccount')}
                        </button>
                      ) : null}
                    </div>
                  </div>
                </aside>

                <main className="news-hub-col news-hub-col--feed">
                  <div className="news-hub-card news-hub-feed-card">
                    <section
                      className="news-hub-changelog"
                      aria-label={t('changelog.panelTitle')}
                    >
                      <div className="news-hub-changelog-toolbar">
                        <h3 className="news-hub-changelog-title">{t('changelog.panelTitle')}</h3>
                        {launcherVersion.trim() ? (
                          <span className="news-hub-version-pill" title={launcherVersion.trim()}>
                            {launcherVersion.trim()}
                          </span>
                        ) : null}
                      </div>
                      <p className="news-hub-changelog-lead">{t('changelog.lead')}</p>
                      {LAUNCHER_CHANGELOG.length === 0 ? (
                        <p className="news-hub-changelog-empty">{t('changelog.empty')}</p>
                      ) : (
                        <div className="news-hub-changelog-scroll">
                          <div className="news-hub-releases">
                            {LAUNCHER_CHANGELOG.map((entry, releaseIdx) => (
                              <article
                                key={entry.version}
                                className={`news-hub-release${releaseIdx === 0 ? ' news-hub-release--latest' : ''}`}
                              >
                                <div className="news-hub-release-head">
                                  <span className="news-hub-release-ver">{entry.version}</span>
                                  {entry.date ? (
                                    <time className="news-hub-release-date" dateTime={entry.date}>
                                      {entry.date}
                                    </time>
                                  ) : null}
                                </div>
                                {(['added', 'changed', 'removed', 'fixed'] as const).map((key) => {
                                  const lines = entry[key]
                                  if (!lines?.length) return null
                                  return (
                                    <div
                                      key={key}
                                      className={`news-hub-change news-hub-change--${key}`}
                                    >
                                      <div className="news-hub-change-head">
                                        <span className="news-hub-change-icon" aria-hidden>
                                          <ChangelogKindIcon kind={key} />
                                        </span>
                                        <h4 className="news-hub-change-title">{t(`changelog.${key}`)}</h4>
                                      </div>
                                      <ul className="news-hub-change-list">
                                        {lines.map((line, i) => (
                                          <li key={i}>{formatChangelogLine(line)}</li>
                                        ))}
                                      </ul>
                                    </div>
                                  )
                                })}
                              </article>
                            ))}
                          </div>
                        </div>
                      )}
                    </section>
                  </div>
                </main>

                <aside className="news-hub-col news-hub-col--social">
                  <div className="news-hub-col-social-stack">
                    <div className="news-hub-card news-hub-social-card news-hub-social-card--compact">
                      <h3 className="news-hub-social-heading">{t('newsView.followUs')}</h3>
                      <div className="news-hub-social-body">
                        <div className="news-hub-social-list">
                          {newsHubSocialRows().map((row) => (
                            <button
                              key={row.id}
                              type="button"
                              className={`news-hub-social-btn news-hub-social-btn--${row.id}`}
                              onClick={() => void window.solea.openExternalUrl(row.url)}
                            >
                              <NewsHubSocialIcon id={row.id} className="news-hub-social-icon" />
                              <span>{t(newsHubSocialLabelKey(row.id))}</span>
                            </button>
                          ))}
                        </div>
                      </div>
                    </div>
                    <div className="news-hub-card news-hub-report-card">
                      <h3 className="news-hub-social-heading news-hub-report-heading">
                        {t('newsView.reportSection')}
                      </h3>
                      <p className="news-hub-report-hint">{t('newsView.reportHint')}</p>
                      <button
                        type="button"
                        className="news-hub-report-cta"
                        onClick={openReportModal}
                      >
                        <IconNewsHubReport className="news-hub-report-cta-icon" />
                        <span>{t('home.help.report')}</span>
                      </button>
                    </div>
                  </div>
                </aside>
              </div>
            </div>
            <footer className="shell-footer">{t('home.footer', { name: modpackName })}</footer>
          </>
        )}

        {view === 'screenshots' && (
          <>
            <ScreenshotsView modpacksList={modpacksList} initialModpackId={activeModpackId} />
            <footer className="shell-footer">{t('home.footer', { name: modpackName })}</footer>
          </>
        )}

        {view === 'settings' && (
          <div className="settings-layout" style={{ flex: 1, minHeight: 0 }}>
            <nav className="settings-nav">
              <div className="settings-nav-brand">
                <button
                  type="button"
                  className="settings-nav-brand-icon"
                  aria-label={t('settings.title')}
                  onClick={() => bumpSettingsDebugTap()}
                >
                  <IconSettingsNav />
                </button>
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
                <button type="button" className="nav-item" onClick={openReportModal}>
                  {t('home.help.report')}
                </button>
                <button type="button" className="nav-item" onClick={() => void window.solea.openUserDataFolder()}>
                  {t('settings.userData')}
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
                      <div className="settings-summary-with-help">
                        <div>
                          {t('settings.afterLaunch')}
                          <div className="sub">{t('settings.afterLaunchSub')}</div>
                        </div>
                        <SettingsGlossaryTrigger
                          gkey="afterLaunch"
                          openKey={settingsGlossaryKey}
                          setOpenKey={setSettingsGlossaryKey}
                          t={t}
                          discordUrl={DISCORD_INVITE_URL}
                        />
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
                      <div className="settings-summary-with-help">
                        <div>
                          {t('settings.network')}
                          <div className="sub">{t('settings.networkSub')}</div>
                        </div>
                        <SettingsGlossaryTrigger
                          gkey="networkCard"
                          openKey={settingsGlossaryKey}
                          setOpenKey={setSettingsGlossaryKey}
                          t={t}
                          discordUrl={DISCORD_INVITE_URL}
                        />
                      </div>
                      <span>▾</span>
                    </summary>
                    <div className="inner field-grid">
                      <label>
                        <span className="settings-label-help-row">
                          {t('settings.downloadThreads')}
                          <SettingsGlossaryTrigger
                            gkey="downloadThreads"
                            openKey={settingsGlossaryKey}
                            setOpenKey={setSettingsGlossaryKey}
                            t={t}
                            discordUrl={DISCORD_INVITE_URL}
                          />
                        </span>
                        <input
                          type="number"
                          min={1}
                          max={32}
                          value={settings.downloadThreads}
                          onChange={(e) => setNum('downloadThreads', e.target.value)}
                        />
                      </label>
                      <label>
                        <span className="settings-label-help-row">
                          {t('settings.timeout')}
                          <SettingsGlossaryTrigger
                            gkey="networkTimeout"
                            openKey={settingsGlossaryKey}
                            setOpenKey={setSettingsGlossaryKey}
                            t={t}
                            discordUrl={DISCORD_INVITE_URL}
                          />
                        </span>
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
                        <span className="settings-label-help-row">
                          {t('settings.javaPath')}
                          <SettingsGlossaryTrigger
                            gkey="javaPath"
                            openKey={settingsGlossaryKey}
                            setOpenKey={setSettingsGlossaryKey}
                            t={t}
                            discordUrl={DISCORD_INVITE_URL}
                          />
                        </span>
                        <input
                          type="text"
                          value={settings.javaPath}
                          onChange={(e) => setSettings((s) => ({ ...s, javaPath: e.target.value }))}
                          spellCheck={false}
                        />
                      </label>
                      <label>
                        <span className="settings-label-help-row">
                          {t('settings.javaVersion')}
                          <SettingsGlossaryTrigger
                            gkey="javaVersion"
                            openKey={settingsGlossaryKey}
                            setOpenKey={setSettingsGlossaryKey}
                            t={t}
                            discordUrl={DISCORD_INVITE_URL}
                          />
                        </span>
                        <input
                          type="text"
                          value={settings.javaVersion}
                          onChange={(e) => setSettings((s) => ({ ...s, javaVersion: e.target.value }))}
                          spellCheck={false}
                        />
                      </label>
                      <label className="full" title={t('settings.jvmArgsTooltip')}>
                        <span className="settings-label-help-row">
                          {t('settings.jvmArgs')}
                          <SettingsGlossaryTrigger
                            gkey="jvmArgs"
                            openKey={settingsGlossaryKey}
                            setOpenKey={setSettingsGlossaryKey}
                            t={t}
                            discordUrl={DISCORD_INVITE_URL}
                          />
                        </span>
                        <textarea
                          rows={4}
                          value={settings.jvmArgs}
                          onChange={(e) => setSettings((s) => ({ ...s, jvmArgs: e.target.value }))}
                          spellCheck={false}
                          title={t('settings.jvmArgsTooltip')}
                        />
                      </label>
                      <label className="full">
                        <span className="settings-label-help-row">
                          {t('settings.azureId')}
                          <SettingsGlossaryTrigger
                            gkey="azureId"
                            openKey={settingsGlossaryKey}
                            setOpenKey={setSettingsGlossaryKey}
                            t={t}
                            discordUrl={DISCORD_INVITE_URL}
                          />
                        </span>
                        <input
                          type="text"
                          value={settings.azureClientId}
                          onChange={(e) => setSettings((s) => ({ ...s, azureClientId: e.target.value }))}
                          spellCheck={false}
                        />
                      </label>
                    </div>
                    <div className="inner settings-net-extras">
                      <div className="settings-toggle-stack">
                        <div className="settings-toggle-with-help">
                          <SettingsToggle
                            checked={settings.networkSlowDownloads === true}
                            onChange={(next) => setSettings((s) => ({ ...s, networkSlowDownloads: next }))}
                            label={t('settings.networkSlowDownloads')}
                          />
                          <SettingsGlossaryTrigger
                            gkey="networkSlow"
                            openKey={settingsGlossaryKey}
                            setOpenKey={setSettingsGlossaryKey}
                            t={t}
                            discordUrl={DISCORD_INVITE_URL}
                          />
                        </div>
                        <div className="settings-toggle-with-help">
                          <SettingsToggle
                            checked={settings.diagnosticLaunch === true}
                            onChange={(next) => setSettings((s) => ({ ...s, diagnosticLaunch: next }))}
                            label={t('settings.diagnosticLaunch')}
                          />
                          <SettingsGlossaryTrigger
                            gkey="diagLaunch"
                            openKey={settingsGlossaryKey}
                            setOpenKey={setSettingsGlossaryKey}
                            t={t}
                            discordUrl={DISCORD_INVITE_URL}
                          />
                        </div>
                      </div>
                      <p className="settings-roadmap-hint">{t('settings.networkRoadmapHint')}</p>
                      <button
                        type="button"
                        className="btn-muted settings-java-dl-btn"
                        onClick={() => void window.solea.openJavaDownloadPage()}
                      >
                        {t('settings.javaDownloadTemurin')}
                      </button>
                    </div>
                  </details>

                  <details className="set-card">
                    <summary>
                      <div>
                        {t('settings.cacheMaintenance')}
                        <div className="sub">{t('settings.cacheMaintenanceSub')}</div>
                      </div>
                      <span>▾</span>
                    </summary>
                    <div className="inner cache-maintenance-panel">
                      {cacheStats ? (
                        <>
                          <p className="cache-line">
                            <strong>{t('settings.cacheGradle')}</strong> {formatBytes(cacheStats.gradleCachesBytes)}
                          </p>
                          <p className="cache-line">
                            <strong>{t('settings.cacheLauncherLogs')}</strong>{' '}
                            {formatBytes(cacheStats.launcherLogsBytes)}
                          </p>
                          <div className="cache-actions">
                            <button
                              type="button"
                              className="btn-danger-outline"
                              onClick={() => setCacheClearConfirm('gradle')}
                            >
                              {t('settings.cacheClearGradle')}
                            </button>
                            <button
                              type="button"
                              className="btn-danger-outline"
                              onClick={() => setCacheClearConfirm('logs')}
                            >
                              {t('settings.cacheClearLogs')}
                            </button>
                          </div>
                        </>
                      ) : (
                        <p className="cache-loading">{t('settings.cacheLoading')}</p>
                      )}
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
                      <div className="full settings-theme-glass-block">
                        <SettingsToggle
                          checked={settings.uiChromeGlass}
                          onChange={(next) => setSettings((s) => ({ ...s, uiChromeGlass: next }))}
                          label={t('settings.chromeGlass')}
                          description={t('settings.chromeGlassSub')}
                        />
                      </div>
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
                          checked={settings.nativeNotifications !== false}
                          onChange={(next) => setSettings((s) => ({ ...s, nativeNotifications: next }))}
                          label={t('settings.nativeNotifications')}
                        />
                        <SettingsToggle
                          checked={settings.discordRichPresence}
                          onChange={(next) => setSettings((s) => ({ ...s, discordRichPresence: next }))}
                          label={t('settings.discordRp')}
                        />
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
                    </div>
                  </details>

                  <details className="set-card" open>
                    <summary>
                      <div>
                        {t('settings.audio')}
                        <div className="sub">{t('settings.audioSub')}</div>
                      </div>
                      <span>▾</span>
                    </summary>
                    <div className="inner field-grid">
                      <div className="full settings-toggle-stack">
                        <SettingsToggle
                          checked={settings.uiSounds}
                          onChange={(next) => setSettings((s) => ({ ...s, uiSounds: next }))}
                          label={t('settings.uiSounds')}
                        />
                        {settings.uiSounds ? (
                          <>
                            <div
                              className="full settings-volume-control"
                              style={
                                {
                                  ['--settings-vol-pct' as string]: `${Math.round((settings.uiSoundVolume ?? 1) * 100)}%`
                                } as CSSProperties
                              }
                            >
                              <div className="settings-volume-header">
                                <span className="settings-volume-title">{t('settings.uiSoundVolume')}</span>
                                <span className="settings-volume-pct" aria-live="polite">
                                  {Math.round((settings.uiSoundVolume ?? 1) * 100)}%
                                </span>
                              </div>
                              <div className="settings-volume-row">
                                <span className="settings-volume-cap">0</span>
                                <input
                                  type="range"
                                  className="settings-volume-range"
                                  min={0}
                                  max={100}
                                  value={Math.round((settings.uiSoundVolume ?? 1) * 100)}
                                  onChange={(e) =>
                                    setSettings((s) => ({
                                      ...s,
                                      uiSoundVolume: Number(e.target.value) / 100
                                    }))
                                  }
                                  aria-label={t('settings.uiSoundVolume')}
                                />
                                <span className="settings-volume-cap">100</span>
                              </div>
                            </div>
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
                      </div>
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
                      {shortcutCapture ? (
                        <p className="settings-shortcut-listening" role="status">
                          {t('settings.shortcutListening')}
                        </p>
                      ) : null}
                      <dl className="settings-shortcuts-dl">
                        <div className="settings-shortcut-row">
                          <dt>{t('settings.shortcutOpenSettings')}</dt>
                          <dd>
                            <kbd className="settings-shortcut-kbd">
                              {formatAcceleratorForDisplay(settings.uiShortcutOpenSettings)}
                            </kbd>
                            <button
                              type="button"
                              className="btn-muted settings-shortcut-change"
                              onClick={() => setShortcutCapture('open')}
                            >
                              {t('settings.shortcutChange')}
                            </button>
                          </dd>
                        </div>
                        <div className="settings-shortcut-row">
                          <dt>{t('settings.shortcutGoNews')}</dt>
                          <dd>
                            <kbd className="settings-shortcut-kbd">
                              {formatAcceleratorForDisplay(settings.uiShortcutGoNews)}
                            </kbd>
                            <button
                              type="button"
                              className="btn-muted settings-shortcut-change"
                              onClick={() => setShortcutCapture('news')}
                            >
                              {t('settings.shortcutChange')}
                            </button>
                          </dd>
                        </div>
                        <div className="settings-shortcut-row">
                          <dt>{t('settings.shortcutGoAccount')}</dt>
                          <dd>
                            <kbd className="settings-shortcut-kbd">
                              {formatAcceleratorForDisplay(settings.uiShortcutGoAccount)}
                            </kbd>
                            <button
                              type="button"
                              className="btn-muted settings-shortcut-change"
                              onClick={() => setShortcutCapture('account')}
                            >
                              {t('settings.shortcutChange')}
                            </button>
                          </dd>
                        </div>
                      </dl>
                    </div>
                  </details>
                </>
              )}

              {isModpackId(settingsTab) && modpackSettingsReadyId !== settingsTab ? (
                <div
                  className="settings-modpack-deferred-skeleton"
                  role="status"
                  aria-busy="true"
                  aria-label={t('settings.modpackPanelLoading')}
                />
              ) : null}

              {modpackSettingsReadyId !== null && isModpackId(modpackSettingsReadyId) ? (
                <>
                  <details className="set-card" open title={t('settings.ramAllocTooltip')}>
                    <summary>
                      <div className="settings-summary-with-help">
                        <div>
                          {t('settings.ram')}
                          <div className="sub">{t('settings.ramSub')}</div>
                        </div>
                        <SettingsGlossaryTrigger
                          gkey="ram"
                          openKey={settingsGlossaryKey}
                          setOpenKey={setSettingsGlossaryKey}
                          t={t}
                          discordUrl={DISCORD_INVITE_URL}
                        />
                      </div>
                      <span>▾</span>
                    </summary>
                    <div className="inner memory-ram-settings-inner" title={t('settings.ramAllocTooltip')}>
                      <MemoryRamSlider
                        allocGb={ramStringToGb(
                          settings.modpackProfiles[modpackSettingsReadyId]?.memoryMax ?? settings.memoryMax
                        )}
                        totalGiB={memoryStats?.totalGiB ?? 16}
                        onChangeAllocGb={(gb) => patchPackRamFromSliderGb(modpackSettingsReadyId, gb)}
                      />
                    </div>
                  </details>

                  <details className="set-card" open>
                    <summary>
                      <div className="settings-summary-with-help">
                        <div>
                          {t('settings.resolution')}
                          <div className="sub">{t('settings.resolutionSub')}</div>
                        </div>
                        <SettingsGlossaryTrigger
                          gkey="resolution"
                          openKey={settingsGlossaryKey}
                          setOpenKey={setSettingsGlossaryKey}
                          t={t}
                          discordUrl={DISCORD_INVITE_URL}
                        />
                      </div>
                      <span>▾</span>
                    </summary>
                    <div className="inner field-grid">
                      <label>
                        {t('settings.width')}
                        <input
                          type="number"
                          value={settings.modpackProfiles[modpackSettingsReadyId]?.screenWidth ?? ''}
                          onChange={(e) =>
                            setPackNum(modpackSettingsReadyId, 'screenWidth', e.target.value, true)
                          }
                          placeholder="1920"
                        />
                      </label>
                      <label>
                        {t('settings.height')}
                        <input
                          type="number"
                          value={settings.modpackProfiles[modpackSettingsReadyId]?.screenHeight ?? ''}
                          onChange={(e) =>
                            setPackNum(modpackSettingsReadyId, 'screenHeight', e.target.value, true)
                          }
                          placeholder="1080"
                        />
                      </label>
                      <div className="full settings-toggle-stack">
                        <SettingsToggle
                          checked={settings.modpackProfiles[modpackSettingsReadyId]?.fullscreen ?? false}
                          onChange={(next) =>
                            patchPackProfile(modpackSettingsReadyId, { fullscreen: next })
                          }
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
                        value={settings.modpackProfiles[modpackSettingsReadyId]?.gameArgs ?? ''}
                        onChange={(e) =>
                          patchPackProfile(modpackSettingsReadyId, { gameArgs: e.target.value })
                        }
                        spellCheck={false}
                      />
                    </div>
                  </details>

                  <details className="set-card" open>
                    <summary>
                      <div>
                        {t('settings.install')}
                        <div className="sub">{t('settings.installSub')}</div>
                      </div>
                      <span>▾</span>
                    </summary>
                    <div className="inner modpack-maint-panel">
                      <div className="modpack-storage-row">
                        <span className="modpack-storage-label">{t('settings.packStorage')}</span>
                        <span className="modpack-storage-value">
                          {packInstanceDetails === null
                            ? t('settings.packStorageLoading')
                            : packInstanceDetails.installed && packInstanceDetails.sizeBytes != null
                              ? formatBytes(packInstanceDetails.sizeBytes)
                              : t('settings.packNotInstalled')}
                        </span>
                      </div>
                      {packInstanceDetails && !packInstanceDetails.installed ? (
                        <p className="modpack-maint-hint">{t('settings.packMaintDisabledHint')}</p>
                      ) : null}
                      <div className="modpack-folder-help-row">
                        <button
                          type="button"
                          className="btn-muted modpack-open-folder-btn"
                          disabled={!packInstanceDetails?.installed || packMaintBusy}
                          title={
                            !packInstanceDetails?.installed ? t('settings.packFolderDisabledHint') : undefined
                          }
                          onClick={() =>
                            void window.solea.openModpackInstanceFolder(modpackSettingsReadyId).then((r) => {
                              if (!r.ok) pushToast(r.error, 'error')
                            })
                          }
                        >
                          {t('settings.packOpenFolder')}
                        </button>
                        <SettingsGlossaryTrigger
                          gkey="instanceFolder"
                          openKey={settingsGlossaryKey}
                          setOpenKey={setSettingsGlossaryKey}
                          t={t}
                          discordUrl={DISCORD_INVITE_URL}
                        />
                      </div>
                      <p className="settings-label-help-row modpack-verify-glossary">
                        {t('settings.verifyFilesGlossaryLabel')}
                        <SettingsGlossaryTrigger
                          gkey="verifyFiles"
                          openKey={settingsGlossaryKey}
                          setOpenKey={setSettingsGlossaryKey}
                          t={t}
                          discordUrl={DISCORD_INVITE_URL}
                        />
                      </p>
                      <div className="modpack-maint-actions">
                        <button
                          type="button"
                          className="btn-muted"
                          disabled={
                            !packInstanceDetails?.installed ||
                            packMaintBusy ||
                            phase === 'installing' ||
                            launchPhase !== 'idle'
                          }
                          title={
                            !packInstanceDetails?.installed ? t('settings.packMaintDisabledHintShort') : undefined
                          }
                          onClick={() => void onReinstallModpack(modpackSettingsReadyId)}
                        >
                          {t('settings.reinstall')}
                        </button>
                        <button
                          type="button"
                          className="btn-danger-outline"
                          disabled={
                            !packInstanceDetails?.installed ||
                            packMaintBusy ||
                            phase === 'installing' ||
                            launchPhase !== 'idle'
                          }
                          title={
                            !packInstanceDetails?.installed ? t('settings.packMaintDisabledHintShort') : undefined
                          }
                          onClick={() => void onUninstallModpack(modpackSettingsReadyId)}
                        >
                          {t('settings.uninstall')}
                        </button>
                      </div>
                    </div>
                  </details>
                </>
              ) : null}

              <div className="actions-bar">
                <button type="button" className="btn-save" onClick={() => void saveAllSettings()}>
                  {t('settings.save')}
                </button>
                <button type="button" className="btn-muted" onClick={() => void resetAllSettings()}>
                  {t('settings.reset')}
                </button>
                <button type="button" className="btn-muted" onClick={() => setView('news')}>
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
                    viewerBackground="#141416"
                    skinAnim={settings.skinViewerAnimation}
                    reduceMotion={reduceMotionEffective}
                    onRefresh={() => setAccountSkinKey((k) => k + 1)}
                    onSkinAnimationChange={(v) => void persistSkinViewerAnimation(v)}
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
                    <button type="button" className="btn-muted" onClick={() => setView('news')}>
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
        {reportModalOpen ? (
          <div
            className="pack-confirm-backdrop"
            role="presentation"
            onClick={() => {
              setReportHelpOpen(false)
              setReportModalOpen(false)
            }}
          >
            <div
              ref={reportModalRef}
              className="pack-confirm-modal pack-confirm-modal--support home-report-modal"
              role="dialog"
              aria-modal="true"
              aria-labelledby="home-report-title"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="home-report-heading">
                <div className="home-report-heading-main">
                  <p className="pack-confirm-eyebrow">{t('home.help.report')}</p>
                  <h2 id="home-report-title" className="pack-confirm-title home-report-title">
                    {t('home.report.title')}
                  </h2>
                </div>
                <div className="home-report-heading-actions">
                  <button
                    type="button"
                    className={`home-report-icon-btn${reportHelpOpen ? ' is-active' : ''}`}
                    aria-expanded={reportHelpOpen}
                    aria-controls="home-report-help"
                    title={t('home.report.helpAria')}
                    onClick={() => setReportHelpOpen((o) => !o)}
                  >
                    <svg className="home-report-icon-btn-svg" viewBox="0 0 24 24" fill="none" aria-hidden>
                      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" />
                      <path
                        d="M9.5 9.5a2.5 2.5 0 015 0c0 2-2.5 1.5-2.5 4M12 17h.01"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                      />
                    </svg>
                  </button>
                  <button
                    type="button"
                    className="home-report-discord-btn"
                    onClick={() => void window.solea.openExternalUrl(DISCORD_INVITE_URL)}
                  >
                    <IconDiscord className="home-report-discord-ico" />
                    <span>{t('home.report.joinDiscord')}</span>
                  </button>
                </div>
              </div>
              {reportHelpOpen ? (
                <div
                  id="home-report-help"
                  className="home-report-help"
                  role="region"
                  aria-label={t('home.report.helpTitle')}
                >
                  <p className="home-report-help-title">{t('home.report.helpTitle')}</p>
                  <p className="home-report-help-body">{t('home.report.helpBody')}</p>
                </div>
              ) : null}
              <p className="pack-confirm-body home-report-lead">{t('home.report.lead')}</p>
              <div className="home-report-panel">
                <div className="home-report-section">
                  <span className="home-report-section-label">{t('home.report.scopeLabel')}</span>
                  <div
                    className="home-report-segment"
                    role="group"
                    aria-label={t('home.report.scopeLabel')}
                  >
                    <button
                      type="button"
                      className={reportScope === 'launcher' ? 'is-active' : ''}
                      onClick={() => {
                        setReportScope('launcher')
                        setReportCategory(REPORT_LAUNCHER_CATEGORIES[0])
                      }}
                    >
                      {t('home.report.scopeLauncher')}
                    </button>
                    <button
                      type="button"
                      className={reportScope === 'instance' ? 'is-active' : ''}
                      onClick={() => {
                        setReportScope('instance')
                        setReportCategory(REPORT_INSTANCE_CATEGORIES[0])
                      }}
                    >
                      {t('home.report.scopeInstance')}
                    </button>
                  </div>
                  <p className="home-report-section-hint">{t('home.report.scopeHint')}</p>
                </div>
                <div className="home-report-fields">
                  {reportScope === 'instance' ? (
                    <>
                      <span className="home-report-label">{t('home.report.instance')}</span>
                      <LauncherSelect
                        className="home-report-launcher-select"
                        aria-label={t('home.report.instance')}
                        value={reportModpackId}
                        onChange={setReportModpackId}
                        options={reportInstanceSelectOptions}
                        disabled={reportInstanceSelectOptions.length === 0}
                      />
                    </>
                  ) : null}
                  <span className="home-report-label">{t('home.report.category')}</span>
                  <LauncherSelect
                    key={reportScope}
                    className="home-report-launcher-select"
                    aria-label={t('home.report.category')}
                    value={reportCategory}
                    onChange={setReportCategory}
                    options={reportCategorySelectOptions}
                  />
                  <label className="home-report-label" htmlFor="home-report-details">
                    {t('home.report.details')}
                  </label>
                  <textarea
                    id="home-report-details"
                    className="home-report-textarea"
                    rows={5}
                    value={reportDetails}
                    onChange={(e) => setReportDetails(e.target.value)}
                  />
                  <label className="home-report-check home-report-check--themed">
                    <input
                      type="checkbox"
                      className="home-report-check-input"
                      checked={reportIncludeTech}
                      onChange={(e) => setReportIncludeTech(e.target.checked)}
                    />
                    <span className="home-report-check-box" aria-hidden />
                    <span className="home-report-check-text">
                      {t(
                        reportScope === 'launcher'
                          ? 'home.report.includeTechLauncher'
                          : 'home.report.includeTech'
                      )}
                    </span>
                  </label>
                </div>
              </div>
              <div className="pack-confirm-actions home-report-actions">
                <button
                  type="button"
                  className="btn-muted pack-confirm-btn-cancel"
                  onClick={() => setReportModalOpen(false)}
                >
                  {t('home.report.close')}
                </button>
                <button
                  type="button"
                  className="btn-muted"
                  onClick={() => void copyReportToClipboard()}
                >
                  {t('home.report.copy')}
                </button>
                <button
                  type="button"
                  className="btn-save pack-confirm-btn-primary"
                  disabled={reportSending}
                  onClick={() => void sendReportDiscord()}
                >
                  {t('home.report.send')}
                </button>
              </div>
            </div>
          </div>
        ) : null}
        <ModpackUpdatesModal
          open={showModpackUpdatesModal}
          packs={allModpacksAction ?? []}
          onClose={() => setShowModpackUpdatesModal(false)}
        />
        {packMaintConfirm ? (
          <PackMaintConfirmModal
            open
            variant={packMaintConfirm.kind}
            onConfirm={onPackMaintConfirmResolved}
            onCancel={() => setPackMaintConfirm(null)}
          />
        ) : null}
        {cacheClearConfirm ? (
          <CacheClearConfirmModal
            open
            kind={cacheClearConfirm}
            onConfirm={onCacheClearConfirmResolved}
            onCancel={() => setCacheClearConfirm(null)}
          />
        ) : null}
        <div className="launcher-version-badge" role="status">
          {LAUNCHER_VERSION_DISPLAY}
        </div>
      </div>
    </div>
      </div>
    </div>
    </div>
  )
}
