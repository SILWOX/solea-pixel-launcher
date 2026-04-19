/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import {
  lazy,
  Suspense,
  useCallback,
  useEffect,
  useLayoutEffect,
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
  ReinstallPreserveOptions,
  SkinViewerAnimation,
  UiHomeCardVariant,
  UiTheme
} from './launcherTypes'
import logoUrl from './assets/branding/logo.png?url'
import homeMinecraftWallpaperUrl from './assets/branding/home-minecraft-wallpaper.png?url'
import soleaLoginLogoUrl from './assets/branding/solea-pixel-login-logo.png?url'
import bootLogoUrl from './assets/branding/boot-logo.png?url'
import './App.css'
import './settingsAether2.css'
import './homeCardStudio.css'
import './actus/actus.css'
import { fetchAndCacheActuSolea, ACTU_SOLEA_UPDATED_EVENT } from './actus/actusoleaFetch'
import { ActuSoleaFeed } from './actus/ActuSoleaFeed'
import { FeedActuNewsTabs } from './actus/FeedActuNewsTabs'
import { HomeTagPill } from './HomeTagPill'
import { homeTagsForModpack } from './homeTagPresets'
import { MODPACK_THEME, MODPACK_HOME_LEAD_KEY, isModpackId, type ModpackIdUi } from './modpackTheme'
import { AccountSkinViewer, type AccountSkinViewerHandle } from './AccountSkinViewer'
import { AccountCapeModal } from './AccountCapeModal'
import { PackMaintConfirmModal } from './PackMaintConfirmModal'
import { InstancePathChangeModal } from './InstancePathChangeModal'
import { SettingsUnsavedModal } from './SettingsUnsavedModal'
import {
  buildUnsavedSettingsSections,
  cloneLauncherSettings,
  isLauncherSettingsDirty
} from './settingsUnsavedDiff'
import { ModpackUpdatesModal } from './ModpackUpdatesModal'
import { LoginInfoModal } from './LoginInfoModal'
import { CacheClearConfirmModal, type CacheClearKind } from './CacheClearConfirmModal'
import { MemoryClearConfirmModal } from './MemoryClearConfirmModal'
import { CreditsModal } from './CreditsModal'
import { MyServerView } from './MyServerView'
import { applyAppearanceSettings, subscribeSystemTheme } from './appearance'
import { useI18n, type TFunction } from './i18n/I18nContext'
import { LauncherSelect, type LauncherSelectEntry } from './ui/LauncherSelect'
import { MemoryRamSlider } from './MemoryRamSlider'
import { allocGbToMinMaxStrings, ramStringToGb } from './memoryRam'
import { useToast } from './ui/ToastContext'
import { playUiSound, type UiSoundPrefs } from './ui/playUiSound'
import { useFocusTrap } from './a11y/useFocusTrap'
import { LAUNCHER_CHANGELOG, type LauncherChangelogEntry } from './launcherChangelog'
import { debounce } from './debounce'
import {
  SettingsGlossaryTrigger,
  type SettingsGlossaryKey
} from './settingsGlossary'
import {
  acceleratorMatches,
  formatAcceleratorForDisplay,
  keyboardEventToAcceleratorString
} from './keyboardAccelerator'
import { LAUNCHER_VERSION_LABEL as LAUNCHER_VERSION_DISPLAY } from './launcherVersionLabel'

const WARMUP_FIRST_PLAY_SESSION = 'solea.pendingWarmupFirstPlay'

function readWarmupPending(): Record<string, boolean> {
  try {
    const raw = sessionStorage.getItem(WARMUP_FIRST_PLAY_SESSION)
    if (!raw) return {}
    const o = JSON.parse(raw) as unknown
    return o && typeof o === 'object' && !Array.isArray(o) ? (o as Record<string, boolean>) : {}
  } catch {
    return {}
  }
}

function setWarmupPending(packId: string): void {
  const p = readWarmupPending()
  p[packId] = true
  sessionStorage.setItem(WARMUP_FIRST_PLAY_SESSION, JSON.stringify(p))
}

function peekWarmupPending(packId: string): boolean {
  return !!readWarmupPending()[packId]
}

function consumeWarmupPending(packId: string): void {
  const p = readWarmupPending()
  if (!p[packId]) return
  delete p[packId]
  sessionStorage.setItem(WARMUP_FIRST_PLAY_SESSION, JSON.stringify(p))
}

const ScreenshotsViewLazy = lazy(() =>
  import('./ScreenshotsView').then((m) => ({ default: m.ScreenshotsView }))
)

type ThemeRow = { kind: 'group'; labelKey: string } | { kind: 'opt'; value: UiTheme; labelKey: string }

const THEME_ROWS: ThemeRow[] = [
  { kind: 'group', labelKey: 'settings.themeGroupGeneral' },
  { kind: 'opt', value: 'system', labelKey: 'settings.themeSystem' },
  { kind: 'opt', value: 'light', labelKey: 'settings.themeLight' },
  { kind: 'opt', value: 'dark', labelKey: 'settings.themeDark' },
  { kind: 'group', labelKey: 'settings.themeGroupStudio' },
  { kind: 'opt', value: 'amber', labelKey: 'settings.themeAmber' },
  { kind: 'opt', value: 'midnight', labelKey: 'settings.themeMidnight' },
  { kind: 'opt', value: 'high_contrast', labelKey: 'settings.themeHighContrast' },
  { kind: 'group', labelKey: 'settings.themeGroupWorlds' },
  { kind: 'opt', value: 'forest', labelKey: 'settings.themeForest' },
  { kind: 'opt', value: 'nether', labelKey: 'settings.themeNether' },
  { kind: 'opt', value: 'end', labelKey: 'settings.themeEnd' },
  { kind: 'opt', value: 'ocean', labelKey: 'settings.themeOcean' },
  { kind: 'group', labelKey: 'settings.themeGroupStyle' },
  { kind: 'opt', value: 'paper', labelKey: 'settings.themePaper' },
  { kind: 'opt', value: 'monochrome', labelKey: 'settings.themeMonochrome' },
  { kind: 'opt', value: 'solarized', labelKey: 'settings.themeSolarized' }
]

const KNOWN_UI_THEMES = new Set<string>(
  THEME_ROWS.filter((r): r is Extract<ThemeRow, { kind: 'opt' }> => r.kind === 'opt').map((r) => r.value)
)

function themeSelectEntries(t: (key: string) => string): LauncherSelectEntry[] {
  return THEME_ROWS.map((row) =>
    row.kind === 'group'
      ? { type: 'group' as const, label: t(row.labelKey) }
      : { value: row.value, label: t(row.labelKey) }
  )
}

function coerceUiThemeForIpc(v: unknown): UiTheme {
  if (typeof v !== 'string') return 'dark'
  const t = v.trim().normalize('NFKC')
  if (t === 'high-contrast') return 'high_contrast'
  return KNOWN_UI_THEMES.has(t) ? (t as UiTheme) : 'dark'
}

const LOGO = logoUrl
/** Fond écran Microsoft + hub Accueil & actus (même artwork, centré / cover). */
const LOGIN_WALLPAPER = homeMinecraftWallpaperUrl
const SOLEA_LOGIN_LOGO = soleaLoginLogoUrl
const NEWS_WALLPAPER = homeMinecraftWallpaperUrl

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

/** Fichiers factices pour la barre de progression (mode test — aucun téléchargement réel). */
const FAKE_INSTALL_DEBUG_FILES = [
  'mods/ferritecore-6.0.1-forge.jar',
  'mods/fabric-api-0.92.0+1.20.1.jar',
  'mods/geckolib-forge-1.20.1.jar',
  'resourcepacks/enhanced_packs.zip',
  'config/defaultoptions-common.toml'
] as const

/** Titre sur deux lignes (maquette) : coupe équilibrée pour les noms longs. */
function packTitleLines(displayName: string): { first: string; second: string | null } {
  const w = displayName.trim().split(/\s+/).filter(Boolean)
  if (w.length === 0) return { first: displayName, second: null }
  if (w.length === 1) return { first: w[0]!, second: null }
  const last = w[w.length - 1]!
  const lastIsNumericSuffix = /^\d{1,4}$/.test(last)
  /* "MYTHIC TRIALS 1" : ne pas isoler le suffixe sur une 2ᵉ ligne */
  if (lastIsNumericSuffix) {
    if (w.length === 2) return { first: w.join(' '), second: null }
    if (w.length >= 3) return { first: w.join(' '), second: null }
  }
  if (w.length === 2) return { first: w[0]!, second: w[1]! }
  const mid = Math.ceil(w.length / 2)
  return { first: w.slice(0, mid).join(' '), second: w.slice(mid).join(' ') }
}

/** Tags + titre + accroche — partagés entre carte Classique et refonte Studio (structure HTML différente). */
function HomeHeroTagsPack({
  activeModpackId,
  studio,
  t
}: {
  activeModpackId: string
  studio: boolean
  t: TFunction
}) {
  return (
    <div
      className={studio ? 'home-hero-tags home-studio-tags' : 'home-hero-tags'}
      role="list"
    >
      {homeTagsForModpack(activeModpackId).map((presetId) => (
        <HomeTagPill
          key={presetId}
          presetId={presetId}
          label={t(`home.tagPreset.${presetId}.label`)}
          description={t(`home.tagPreset.${presetId}.desc`)}
        />
      ))}
    </div>
  )
}

function HomePackTitlePack({ modpackName, studio }: { modpackName: string; studio: boolean }) {
  const { first, second } = packTitleLines(modpackName)
  return (
    <h2
      className={`home-title-stacked${!second ? ' home-title-stacked--single' : ''}${
        studio ? ' home-studio-title' : ''
      }`}
      id={studio ? 'home-studio-title' : undefined}
    >
      <span className="home-title-line">{first}</span>
      {second ? <span className="home-title-line">{second}</span> : null}
    </h2>
  )
}

function HomePackLeadPack({
  activeModpackId,
  studio,
  t
}: {
  activeModpackId: string
  studio: boolean
  t: TFunction
}) {
  return (
    <p className={`lead${studio ? ' home-studio-lead' : ''}`}>
      {isModpackId(activeModpackId)
        ? t(MODPACK_HOME_LEAD_KEY[activeModpackId])
        : t('home.lead')}
    </p>
  )
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
  className,
  offline
}: {
  uuid: string | undefined | null
  /** Conservé pour l’API ; la résolution réseau est fixe (évite cache basse déf). */
  sizePx: number
  className?: string
  /** Profil hors ligne : pas d’aperçu skin (placeholder neutre). */
  offline?: boolean
}) {
  void _displaySizeHint
  const [src, setSrc] = useState<string>(LOGO)

  useEffect(() => {
    if (offline) {
      setSrc(LOGO)
      return
    }
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
  }, [uuid, offline])

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
      setPresetsState(s ?? { activePresetId: null, presets: [] })
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
      if (!r.ok) {
        pushToast(r.error, 'error')
        return
      }
      onRefresh()
      if (r.skinSyncError) {
        pushToast(t('skins.syncMojang', { error: r.skinSyncError }), 'error')
      }
    })
  }

  const selectPreset = (presetId: string) => {
    void window.solea.setActiveSkinPreset(uuid, presetId).then((r) => {
      if (!r.ok) {
        pushToast(r.error, 'error')
        return
      }
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
        <section className="account-aether-card account-aether-card--preview" aria-labelledby="account-preview-title">
          <header className="account-aether-card-head">
            <p className="account-aether-eyebrow">{t('skins.sectionPreviewEyebrow')}</p>
            <h3 id="account-preview-title" className="account-aether-card-title">
              {t('skins.sectionPreviewTitle')}
            </h3>
            <p className="account-aether-card-desc">{t('skins.sectionPreviewDesc')}</p>
          </header>
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
        </section>

        <aside className="account-aether-card account-aether-card--library" aria-labelledby="account-library-title">
          <header className="account-aether-card-head account-aether-card-head--compact">
            <div className="account-skins-sidebar-head">
              <p className="account-aether-eyebrow">{t('skins.sectionLibraryEyebrow')}</p>
              <div className="account-library-title-row">
                <h3 id="account-library-title" className="account-aether-card-title account-aether-card-title--sm">
                  {t('skins.title')}
                </h3>
                <span className="account-skins-beta">{t('skins.beta')}</span>
              </div>
            </div>
            <p className="account-aether-card-desc">{t('skins.sub')}</p>
          </header>
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
            <p className="account-aether-eyebrow account-cape-eyebrow">{t('skins.sectionCapeEyebrow')}</p>
            <h4 className="account-cape-title">{t('skins.cape')}</h4>
            <p className="account-cape-lead">{t('skins.capeLead')}</p>
            <button type="button" className="btn-save account-cape-open-btn" onClick={() => setCapeModalOpen(true)}>
              {t('skins.changeCape')}
            </button>
          </div>
        </aside>
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
    screenWidth: 800,
    screenHeight: 600,
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
    uiTheme: coerceUiThemeForIpc(settings.uiTheme),
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
    downloadThreads: 16,
    networkTimeoutMs: 20000,
    javaPath: '',
    javaVersion: '21',
    screenWidth: 800,
    screenHeight: 600,
    fullscreen: false,
    azureClientId: '',
    afterLaunch: 'keep',
    openGameLogOnInstanceLaunch: false,
    activeModpackId: 'palamod-recreated',
    modpackProfiles: {
      'palamod-recreated': { ...packs },
      'wither-storm': { ...packs },
      'mythic-trials-1': { ...packs },
      'mythic-trials-2': { ...packs },
      aeloria: { ...packs }
    },
    uiLanguage: 'en',
    uiTheme: 'dark',
    uiAccentHex: '#ff6a1a',
    uiFontScale: 'm',
    uiReduceMotion: false,
    uiCompact: false,
    uiHomeCardVariant: 'studio',
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
    experimentalServerSystemEnabled: true,
    diagnosticLaunch: false,
    networkSlowDownloads: false,
    uiChromeGlass: false,
    uiSettingsShell: 'aether2',
    modpackInstanceParentPath: {}
  }
}

/** Accueil + écran Paramètres partagent le même mode v2 / Legacy (champs disque toujours alignés). */
function normalizeLauncherSettingsUi(s: LauncherSettingsUI): LauncherSettingsUI {
  const legacy = s.uiHomeCardVariant === 'classic' || s.uiSettingsShell === 'legacy'
  return {
    ...s,
    uiHomeCardVariant: legacy ? 'classic' : 'studio',
    uiSettingsShell: legacy ? 'legacy' : 'aether2',
    experimentalServerSystemEnabled:
      typeof s.experimentalServerSystemEnabled === 'boolean'
        ? s.experimentalServerSystemEnabled
        : true
  }
}

/** Valeurs par défaut de l’onglet « Launcher » uniquement (sans profils modpacks ni chemins d’instance). */
function pickLauncherTabDefaultPatch(): Partial<LauncherSettingsUI> {
  const d = emptySettings()
  return {
    afterLaunch: d.afterLaunch,
    openGameLogOnInstanceLaunch: d.openGameLogOnInstanceLaunch,
    downloadThreads: d.downloadThreads,
    networkTimeoutMs: d.networkTimeoutMs,
    azureClientId: d.azureClientId,
    networkSlowDownloads: d.networkSlowDownloads,
    uiLanguage: d.uiLanguage,
    uiTheme: d.uiTheme,
    uiAccentHex: d.uiAccentHex,
    uiFontScale: d.uiFontScale,
    uiReduceMotion: d.uiReduceMotion,
    uiCompact: d.uiCompact,
    uiHomeCardVariant: d.uiHomeCardVariant,
    uiChromeGlass: d.uiChromeGlass,
    uiSettingsShell: d.uiSettingsShell,
    uiSounds: d.uiSounds,
    uiSoundVolume: d.uiSoundVolume,
    uiSoundInstall: d.uiSoundInstall,
    uiSoundLaunch: d.uiSoundLaunch,
    discordRichPresence: d.discordRichPresence,
    updateChannel: d.updateChannel,
    skinViewerAnimation: d.skinViewerAnimation,
    uiShortcutOpenSettings: d.uiShortcutOpenSettings,
    uiShortcutGoNews: d.uiShortcutGoNews,
    uiShortcutGoAccount: d.uiShortcutGoAccount,
    nativeNotifications: d.nativeNotifications,
    experimentalServerSystemEnabled: d.experimentalServerSystemEnabled,
    diagnosticLaunch: d.diagnosticLaunch
  }
}

function applyLauncherTabDefaults(s: LauncherSettingsUI): LauncherSettingsUI {
  return normalizeLauncherSettingsUi({ ...s, ...pickLauncherTabDefaultPatch() })
}

function applyModpackTabDefaults(s: LauncherSettingsUI, packId: ModpackIdUi): LauncherSettingsUI {
  const nextPath = { ...s.modpackInstanceParentPath }
  delete nextPath[packId]
  return {
    ...s,
    modpackProfiles: {
      ...s.modpackProfiles,
      [packId]: emptyPackProfile()
    },
    modpackInstanceParentPath: nextPath
  }
}

type SettingsResetConfirmKind = 'launcher-tab' | 'all' | ModpackIdUi

function IconHome({ className }: { className?: string } = {}) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M3 9.5L12 3l9 6.5V21a1 1 0 01-1 1h-5v-7H9v7H4a1 1 0 01-1-1V9.5z" />
    </svg>
  )
}

/** Engrenage (paramètres) — barre latérale + en-tête réglages. */
function IconGearCog({
  className,
  strokeWidth = 2
}: {
  className?: string
  strokeWidth?: number
}) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
    >
      <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.72V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  )
}

function IconGear() {
  return <IconGearCog />
}

/** Icône paramètres compacte pour la colonne PARAMÈTRES. */
function IconSettingsNav() {
  return <IconGearCog className="icon-settings-nav" strokeWidth={1.65} />
}

function SettingsToggle({
  checked,
  onChange,
  label,
  description
}: {
  checked: boolean
  onChange: (next: boolean) => void
  label: ReactNode
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

function IconServer() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
      <rect x="3" y="4" width="18" height="6" rx="1.5" />
      <rect x="3" y="14" width="18" height="6" rx="1.5" />
      <path d="M7 7h2M7 17h2" strokeLinecap="round" />
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

type NewsHubSocialId = 'modrinth' | 'website' | 'youtube' | 'x' | 'discord' | 'bmc'

/** Liens onglet Accueil (ordre d’affichage). Chaîne vide = bouton masqué. */
const NEWS_HUB_SOCIAL_DEF: { id: NewsHubSocialId; url: string }[] = [
  { id: 'modrinth', url: 'https://modrinth.com/organization/soleapixel' },
  { id: 'website', url: 'https://soleapixel.com' },
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
  website: 'newsView.socialWebsite',
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

function ChangelogEntrySections({
  entry,
  t
}: {
  entry: LauncherChangelogEntry
  t: TFunction
}) {
  return (
    <>
      {(['added', 'changed', 'removed', 'fixed'] as const).map((key) => {
        const lines = entry[key]
        if (!lines?.length) return null
        return (
          <div key={key} className={`news-hub-change news-hub-change--${key}`}>
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
    </>
  )
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
    website: (
      <img
        src={logoUrl}
        alt=""
        className={className}
        width={26}
        height={26}
        draggable={false}
      />
    ),
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

const NEWS_CHANGELOG_LATEST = LAUNCHER_CHANGELOG[0]
const NEWS_CHANGELOG_OLDER = LAUNCHER_CHANGELOG.slice(1)

function formatBytes(n: number): string {
  if (!Number.isFinite(n) || n < 0) return '—'
  if (n < 1024) return `${Math.round(n)} o`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} Ko`
  if (n < 1024 ** 3) return `${(n / 1024 / 1024).toFixed(1)} Mo`
  return `${(n / 1024 ** 3).toFixed(2)} Go`
}

const MIN_BOOT_MS = 1650

function IconCheckUpdates({ className }: { className?: string } = {}) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      width={20}
      height={20}
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
    >
      <path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16" />
      <path d="M3 21v-5h5" />
      <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8" />
      <path d="M21 3v5h-5" />
    </svg>
  )
}

function IconPlay() {
  return (
    <svg className="btn-play-ico" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M8 5v14l11-7z" />
    </svg>
  )
}

function IconStop() {
  return (
    <svg className="btn-play-ico" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
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

function TitleBar({ showFloatingTitle = false }: { showFloatingTitle?: boolean }) {
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
    <header className="titlebar titlebar--float" aria-label={t('titlebar.productName')}>
      <div
        className="titlebar-drag-shim"
        role="presentation"
        aria-hidden
        onDoubleClick={() => void toggleMax()}
      />
      {showFloatingTitle ? (
        <span className="titlebar-float-title" aria-hidden>
          {t('titlebar.productName')}
        </span>
      ) : null}
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
  const [offlineOpen, setOfflineOpen] = useState(false)
  const [offlineName, setOfflineName] = useState('')
  const [offlineBusy, setOfflineBusy] = useState(false)
  const [offlineErr, setOfflineErr] = useState<string | null>(null)
  const [offlineWelcomeOpen, setOfflineWelcomeOpen] = useState(false)
  const offlineModalRef = useRef<HTMLDivElement>(null)
  const offlineWelcomeRef = useRef<HTMLDivElement>(null)
  useFocusTrap(offlineOpen, offlineModalRef, { onEscape: () => !offlineBusy && setOfflineOpen(false) })
  useFocusTrap(offlineWelcomeOpen, offlineWelcomeRef, { onEscape: () => setOfflineWelcomeOpen(false) })

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

  const submitOffline = async () => {
    if (offlineBusy) return
    const name = offlineName.trim()
    setOfflineBusy(true)
    setOfflineErr(null)
    try {
      const r = await window.solea.addOfflineAccount(name)
      if (!r.ok) {
        setOfflineErr(r.error)
        return
      }
      setOfflineOpen(false)
      setOfflineWelcomeOpen(true)
    } catch (e) {
      const detail = e instanceof Error ? e.message : String(e)
      setOfflineErr(t('login.offlineSaveFailed', { detail }))
    } finally {
      setOfflineBusy(false)
    }
  }

  const finishOfflineWelcome = () => {
    setOfflineWelcomeOpen(false)
    onLoggedIn()
  }

  return (
    <div
      className="login-root"
      style={{
        backgroundImage: `url(${LOGIN_WALLPAPER})`,
        backgroundSize: 'cover',
        backgroundPosition: 'center center'
      }}
    >
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
        <button type="button" className="btn-ms" disabled={busy || offlineBusy} onClick={() => void connect()}>
          {busy ? t('login.connectBusy') : t('login.connect')}
        </button>
        <button
          type="button"
          className="btn-login-offline"
          disabled={busy || offlineBusy}
          onClick={() => {
            setOfflineErr(null)
            setOfflineName('')
            setOfflineOpen(true)
          }}
        >
          {t('login.playOffline')}
        </button>
        {msg && <p className="login-error">{msg}</p>}
        <p className="login-legal">{t('login.legal')}</p>
      </div>
      {offlineOpen ? (
        <div
          className="pack-confirm-backdrop"
          role="presentation"
          onClick={() => !offlineBusy && setOfflineOpen(false)}
        >
          <div
            ref={offlineModalRef}
            className="pack-confirm-modal login-offline-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="login-offline-title"
            onClick={(e) => e.stopPropagation()}
          >
            <p className="pack-confirm-eyebrow">Solea</p>
            <h2 id="login-offline-title" className="pack-confirm-title">
              {t('login.offlineModalTitle')}
            </h2>
            <p className="pack-confirm-body login-offline-lead">{t('login.offlineModalLead')}</p>
            <div className="login-offline-field">
              <label className="login-offline-label" htmlFor="login-offline-name-input">
                <span className="login-offline-label-text">{t('login.offlinePlaceholder')}</span>
                <input
                  id="login-offline-name-input"
                  type="text"
                  className="login-offline-input"
                  value={offlineName}
                  onChange={(e) => setOfflineName(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') void submitOffline()
                  }}
                  placeholder={t('login.offlinePlaceholder')}
                  maxLength={16}
                  autoComplete="username"
                  autoFocus
                />
              </label>
            </div>
            {offlineErr ? <p className="login-error login-offline-err">{offlineErr}</p> : null}
            <div className="pack-confirm-actions login-offline-modal__actions">
              <button
                type="button"
                className="btn-muted login-offline-btn-cancel"
                disabled={offlineBusy}
                onClick={() => setOfflineOpen(false)}
              >
                {t('login.offlineCancel')}
              </button>
              <button
                type="button"
                className="btn-ms login-offline-btn-submit"
                disabled={offlineBusy}
                onClick={() => void submitOffline()}
              >
                {offlineBusy ? t('login.offlineBusy') : t('login.offlineValidate')}
              </button>
            </div>
          </div>
        </div>
      ) : null}
      {offlineWelcomeOpen ? (
        <div className="pack-confirm-backdrop" role="presentation" onClick={finishOfflineWelcome}>
          <div
            ref={offlineWelcomeRef}
            className="pack-confirm-modal login-offline-welcome"
            role="dialog"
            aria-modal="true"
            aria-labelledby="login-offline-welcome-title"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 id="login-offline-welcome-title" className="pack-confirm-title">
              {t('login.offlineWelcomeTitle')}
            </h2>
            <p className="pack-confirm-body">{t('login.offlineWelcomeBody')}</p>
            <div className="pack-confirm-actions">
              <button type="button" className="btn-ms" onClick={finishOfflineWelcome}>
                {t('login.offlineWelcomeOk')}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}

export function App() {
  const { t, setLocale, formatPercent, formatDate } = useI18n()
  const { pushToast } = useToast()
  const [screen, setScreen] = useState<'boot' | 'login' | 'app'>('boot')
  const FEED_TAB_KEY = 'solea.feedTab'
  const [feedTab, setFeedTab] = useState<'actu' | 'updates'>(() => {
    try {
      const v = localStorage.getItem(FEED_TAB_KEY)
      if (v === 'actu' || v === 'updates') return v
    } catch {
      /* ignore */
    }
    return 'actu'
  })
  const setFeedTabPersist = useCallback((v: 'actu' | 'updates') => {
    setFeedTab(v)
    try {
      localStorage.setItem(FEED_TAB_KEY, v)
    } catch {
      /* ignore */
    }
  }, [])
  const [testMode, setTestMode] = useState(false)
  const [modpackName, setModpackName] = useState('Palamod Recreated')
  /** Par défaut : onglet Accueil (actus). Les modpacks restent sur `home`. */
  const [view, setView] = useState<'home' | 'news' | 'settings' | 'account' | 'screenshots' | 'my-server'>(
    'news'
  )
  const [settingsTab, setSettingsTab] = useState<'launcher' | ModpackIdUi>('launcher')
  const [shortcutCapture, setShortcutCapture] = useState<null | 'open' | 'news' | 'account'>(null)
  /** Onglet modpack dont le panneau lourd (RAM, etc.) est monté — retardé pour éviter le freeze au clic. */
  const [modpackSettingsReadyId, setModpackSettingsReadyId] = useState<ModpackIdUi | null>(null)
  const modpackPanelRaf2Ref = useRef(0)
  const settingsDebugTapRef = useRef({ n: 0, until: 0 })
  const [accounts, setAccounts] = useState<{ uuid: string; name: string; offline: boolean }[]>([])
  const [activeAcc, setActiveAcc] = useState<{ name: string; uuid: string; offline?: boolean } | null>(
    null
  )
  const [menuOpen, setMenuOpen] = useState(false)
  const [addAccountModalOpen, setAddAccountModalOpen] = useState(false)
  const [addAccountStep, setAddAccountStep] = useState<'pick' | 'offline'>('pick')
  const [addAccountOfflineName, setAddAccountOfflineName] = useState('')
  const [addAccountBusy, setAddAccountBusy] = useState(false)
  const [addAccountErr, setAddAccountErr] = useState<string | null>(null)
  const addAccountModalRef = useRef<HTMLDivElement>(null)
  /** Dernière issue du refresh MS sur l’accueil — évite de spammer les toasts si la session reste invalide. */
  const sessionRefreshPrevOkRef = useRef<boolean | null>(null)
  const [phase, setPhase] = useState<'idle' | 'installing' | 'uninstalling' | 'busy'>('idle')
  const [installLine, setInstallLine] = useState('')
  const [installPct, setInstallPct] = useState(0)
  const [installIndeterminate, setInstallIndeterminate] = useState(false)
  const backgroundInstallOrUninstall = phase === 'installing' || phase === 'uninstalling'
  const [settings, setSettings] = useState<LauncherSettingsUI>(emptySettings)
  const [settingsFb, setSettingsFb] = useState<{ text: string; ok: boolean } | null>(null)
  const [launchPhase, setLaunchPhase] = useState<'idle' | 'launching' | 'running'>('idle')
  const [launchDots, setLaunchDots] = useState(1)
  const [primaryActionAck, setPrimaryActionAck] = useState(false)
  const primaryActionAckTimerRef = useRef(0)
  const modpackRefreshFlightRef = useRef<Promise<void> | null>(null)
  const [memoryStats, setMemoryStats] = useState<{ totalGiB: number } | null>(null)
  const [myServerInstallProgress, setMyServerInstallProgress] = useState<
    | null
    | { serverId: string; phase: string; current: number; total: number; detail?: string }
  >(null)
  const [packInstanceDetails, setPackInstanceDetails] = useState<{
    installed: boolean
    sizeBytes: number | null
    instanceRoot: string
  } | null>(null)
  const [cacheStats, setCacheStats] = useState<{
    launcherCachesBytes: number
    launcherLogsBytes: number
  } | null>(null)
  const [allModpacksAction, setAllModpacksAction] = useState<ModpackActionInfoRow[] | null>(null)
  const [showModpackUpdatesModal, setShowModpackUpdatesModal] = useState(false)
  const modpackUpdatesModalShownRef = useRef(false)
  const [activeModpackId, setActiveModpackId] = useState<ModpackIdUi>('palamod-recreated')
  const [modpacksList, setModpacksList] = useState<{ id: string; displayName: string }[]>([])
  const [packSwitching, setPackSwitching] = useState(false)
  const [settingsVerifyResult, setSettingsVerifyResult] = useState<
    | null
    | { ok: true }
    | { ok: false; reason: string; detail?: string; paths?: string[] }
  >(null)
  const [checkUpdateBusy, setCheckUpdateBusy] = useState(false)
  const [packMaintBusy, setPackMaintBusy] = useState(false)
  const [packMaintConfirm, setPackMaintConfirm] = useState<
    null | { kind: 'reinstall' | 'uninstall'; packId: ModpackIdUi }
  >(null)
  const [instancePathConfirm, setInstancePathConfirm] = useState<
    null | { packId: ModpackIdUi; parentPath: string | null; newRootPreview: string }
  >(null)
  const [instancePathBusy, setInstancePathBusy] = useState(false)
  /** Aperçu du chemin résolu avant/après choix (dossier parent ou défaut), jusqu’à annulation ou enregistrement. */
  const [instancePathUiPreview, setInstancePathUiPreview] = useState<{
    parentPath: string | null
    resolvedRoot: string
  } | null>(null)
  const [cacheClearConfirm, setCacheClearConfirm] = useState<null | CacheClearKind>(null)
  const [memoryClearOpen, setMemoryClearOpen] = useState(false)
  const [settingsResetConfirm, setSettingsResetConfirm] = useState<SettingsResetConfirmKind | null>(null)
  const settingsResetModalRef = useRef<HTMLDivElement>(null)
  const [launcherVersion, setLauncherVersion] = useState('')
  const [launcherMeta, setLauncherMeta] = useState<{
    knownVersion?: string
    lastLauncherUpdatedAt?: string | null
  } | null>(null)
  const [modpackActivityById, setModpackActivityById] = useState<
    Record<string, { lastPlayAt?: string; lastInstallAt?: string }>
  >({})
  const [quickLaunchHint, setQuickLaunchHint] = useState<{
    kind: 'world' | 'server' | 'instance'
    label: string
  } | null>(null)
  const [bootProgress, setBootProgress] = useState(0)
  const [bootDots, setBootDots] = useState(1)
  const [accountSkinKey, setAccountSkinKey] = useState(0)
  const [accountFb, setAccountFb] = useState<string | null>(null)
  const [accountSessionWarn, setAccountSessionWarn] = useState(false)
  const [creditsModalOpen, setCreditsModalOpen] = useState(false)
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
  const viewRef = useRef(view)
  viewRef.current = view
  const settingsBaselineRef = useRef<LauncherSettingsUI | null>(null)
  const pendingNavRef = useRef<null | (() => void | Promise<void>)>(null)
  const prevAppViewRef = useRef(view)
  const [settingsUnsavedOpen, setSettingsUnsavedOpen] = useState(false)
  const [settingsUnsavedBusy, setSettingsUnsavedBusy] = useState(false)
  const manualUpdateCheckRef = useRef(false)
  const installBusyRef = useRef(false)
  /** Simulation install/update (mode test) — ignore les événements IPC de progression. */
  const fakeInstallActiveRef = useRef(false)
  const fakeInstallIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null)
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

  useEffect(() => {
    if (modpackUi.needsInstall) setMenuOpen(false)
  }, [modpackUi.needsInstall])

  useEffect(() => {
    if (!settings.discordRichPresence) return
    const label =
      screen === 'boot'
        ? t('discordPresence.boot')
        : screen === 'login'
          ? t('discordPresence.login')
          : (
              {
                home: t('shell.home'),
                news: t('shell.news'),
                settings: t('shell.settings'),
                account: t('shell.account'),
                screenshots: t('shell.screenshots')
              } as const
            )[view]
    void window.solea.setDiscordLauncherScreenLabel(label)
  }, [screen, view, settings.discordRichPresence, t])

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

  const triggerPrimaryActionAck = useCallback(() => {
    if (reduceMotionEffective) return
    window.clearTimeout(primaryActionAckTimerRef.current)
    setPrimaryActionAck(true)
    primaryActionAckTimerRef.current = window.setTimeout(() => setPrimaryActionAck(false), 520)
  }, [reduceMotionEffective])

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
    if (view === 'news' || view === 'screenshots' || (view === 'my-server' && settings.uiChromeGlass))
      return NEWS_WALLPAPER
    if (view === 'home' && isModpackId(activeModpackId)) return MODPACK_THEME[activeModpackId].wallpaper
    if (isModpackId(activeModpackId)) return MODPACK_THEME[activeModpackId].wallpaper
    return NEWS_WALLPAPER
  }, [view, activeModpackId, settings.uiChromeGlass])

  const flushSkinAnimationRef = useRef<(v: SkinViewerAnimation) => void>(() => {})

  useEffect(() => {
    flushSkinAnimationRef.current = (v: SkinViewerAnimation) => {
      const merged = { ...settingsRef.current, skinViewerAnimation: v }
      void window.solea.saveSettings(serializeLauncherSettingsForIpc(merged)).then((r) => {
        if (!r.ok) {
          pushToast(r.error, 'error')
          void window.solea.getSettings().then((s) => setSettings(normalizeLauncherSettingsUi(s as LauncherSettingsUI)))
        } else if (settingsBaselineRef.current) {
          settingsBaselineRef.current = { ...settingsBaselineRef.current, skinViewerAnimation: v }
        }
      })
    }
  }, [pushToast])

  const debouncedFlushSkinAnimation = useMemo(
    () => debounce((v: SkinViewerAnimation) => flushSkinAnimationRef.current(v), 420),
    []
  )

  const persistSkinViewerAnimation = useCallback(
    (v: SkinViewerAnimation) => {
      setSettings((s) => ({ ...s, skinViewerAnimation: v }))
      debouncedFlushSkinAnimation(v)
    },
    [debouncedFlushSkinAnimation]
  )

  const refreshAllModpacksAction = useCallback(async (opts?: { force?: boolean }) => {
    const force = !!opts?.force
    if (force) modpackRefreshFlightRef.current = null
    if (modpackRefreshFlightRef.current && !force) {
      await modpackRefreshFlightRef.current
      return
    }
    const p = (async () => {
      try {
        const r = await window.solea.getAllModpacksActionInfo(force)
        setAllModpacksAction(r.packs)
      } catch {
        setAllModpacksAction([])
      }
    })().finally(() => {
      modpackRefreshFlightRef.current = null
    })
    modpackRefreshFlightRef.current = p
    await p
  }, [])

  useEffect(() => {
    if (!allModpacksAction?.length) return
    const hasOutdated = allModpacksAction.some((p) => p.needsUpdate && !p.needsInstall)
    if (!hasOutdated || modpackUpdatesModalShownRef.current) return
    modpackUpdatesModalShownRef.current = true
    setShowModpackUpdatesModal(true)
  }, [allModpacksAction])

  const refreshModpackActivity = useCallback(async () => {
    const [m, meta] = await Promise.all([
      window.solea.getModpackActivity(),
      window.solea.getLauncherMeta()
    ])
    setModpackActivityById(m)
    setLauncherMeta(meta)
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
    if (screen !== 'app') return
    void refreshModpackActivity()
  }, [screen, refreshModpackActivity])

  useEffect(() => {
    sessionRefreshPrevOkRef.current = null
  }, [activeAcc?.uuid])

  useEffect(() => {
    if (screen !== 'app' || view !== 'home' || !activeAcc || activeAcc.offline) {
      setAccountSessionWarn(false)
      return
    }
    let cancelled = false
    void window.solea.refreshActiveAccount().then((r) => {
      if (cancelled) return
      if (r.ok) {
        sessionRefreshPrevOkRef.current = true
        setAccountSessionWarn(false)
        return
      }
      const prev = sessionRefreshPrevOkRef.current
      sessionRefreshPrevOkRef.current = false
      setAccountSessionWarn(true)
      if (prev === null || prev === true) {
        pushToast(t('account.toastMicrosoftSessionFailed', { detail: r.error }), 'error', 10000)
      }
    })
    return () => {
      cancelled = true
    }
  }, [screen, view, activeAcc?.uuid, activeAcc?.offline, pushToast, t])

  useEffect(() => {
    if (activeAcc?.offline && view === 'account') {
      setView('news')
    }
  }, [activeAcc?.offline, activeAcc?.uuid, view])

  useEffect(() => {
    if (activeAcc?.offline && view === 'my-server') {
      setView('news')
    }
  }, [activeAcc?.offline, view])

  useEffect(() => {
    if (!settings.experimentalServerSystemEnabled && view === 'my-server') {
      setView('news')
    }
  }, [settings.experimentalServerSystemEnabled, view])

  useEffect(() => {
    if (screen !== 'app') return
    return window.solea.onSoleaServerEvent((ev) => {
      if (ev.kind !== 'progress') return
      if (ev.phase === 'done' || ev.phase === 'error') {
        setMyServerInstallProgress(null)
        return
      }
      setMyServerInstallProgress({
        serverId: ev.serverId,
        phase: ev.phase,
        current: ev.current,
        total: Math.max(1, ev.total),
        detail: typeof ev.detail === 'string' ? ev.detail : undefined
      })
    })
  }, [screen])

  useEffect(() => {
    if (!settingsGlossaryKey || view !== 'settings') return
    const onDoc = (e: MouseEvent) => {
      const tgt = e.target
      if (
        tgt instanceof Element &&
        (tgt.closest('.settings-glossary-wrap') || tgt.closest('.settings-glossary-pop'))
      ) {
        return
      }
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

  useLayoutEffect(() => {
    if (screen !== 'app') {
      prevAppViewRef.current = view
      return
    }
    if (view === 'settings' && prevAppViewRef.current !== 'settings') {
      settingsBaselineRef.current = cloneLauncherSettings(settingsRef.current)
    }
    prevAppViewRef.current = view
  }, [view, screen])

  const tryLeaveSettings = useCallback((action: () => void | Promise<void>) => {
    if (viewRef.current !== 'settings') {
      void action()
      return
    }
    const baseline = settingsBaselineRef.current
    if (!baseline || !isLauncherSettingsDirty(baseline, settingsRef.current)) {
      void action()
      return
    }
    pendingNavRef.current = action
    setSettingsUnsavedOpen(true)
  }, [])

  useLayoutEffect(() => {
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
        if (!cancelled) setSettings(normalizeLauncherSettingsUi(s))
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
    void window.solea.getSettings().then((s) => setSettings(normalizeLauncherSettingsUi(s)))
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

  const runSelectModpack = async (id: ModpackIdUi) => {
    setView('home')
    if (id === activeModpackId) return
    const prevId = activeModpackId
    const prevName = modpackName
    setPackSwitching(true)
    // Mise à jour optimiste : évite le contour « actif » sur l’ancien pack pendant l’IPC
    // (ex. Actus → autre pack : un frame avec view=home et l’id précédent).
    setActiveModpackId(id)
    const listName = modpacksList.find((m) => m.id === id)?.displayName
    if (listName) setModpackName(listName)
    await new Promise((r) => setTimeout(r, 90))
    const r = await window.solea.setActiveModpack(id)
    if (!r.ok) {
      pushToast(r.error, 'error')
      setActiveModpackId(prevId)
      setModpackName(prevName)
      setPackSwitching(false)
      return
    }
    setActiveModpackId(r.activeModpackId as ModpackIdUi)
    const p = await window.solea.getPaths()
    if (p.modpackDisplayName) setModpackName(p.modpackDisplayName)
    await refreshAllModpacksAction()
    void refreshModpackActivity()
    void window.solea.isGameRunning().then((run) => {
      setLaunchPhase((prev) => {
        if (run) return 'running'
        // Pendant le lancement, isGameRunning est souvent encore false : ne pas repasser à Play.
        if (prev === 'launching') return 'launching'
        return 'idle'
      })
    })
    window.setTimeout(() => setPackSwitching(false), 360)
  }

  const selectModpack = (id: ModpackIdUi) => {
    tryLeaveSettings(() => runSelectModpack(id))
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
    setSettingsVerifyResult(null)
  }, [modpackSettingsReadyId])

  useEffect(() => {
    setInstancePathUiPreview(null)
  }, [modpackSettingsReadyId])

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
      setPackInstanceDetails({
        installed: d.installed,
        sizeBytes: d.sizeBytes,
        instanceRoot: d.instanceRoot
      })
    })
    return () => {
      cancelled = true
    }
  }, [
    view,
    settingsTab,
    packMaintBusy,
    phase,
    isModpackId(settingsTab) ? settings.modpackInstanceParentPath?.[settingsTab] ?? '' : ''
  ])

  const installLocationUi = useMemo(() => {
    if (!modpackSettingsReadyId || !isModpackId(modpackSettingsReadyId)) {
      return {
        displayedRoot: '',
        showCustomBadge: false,
        showLoadingPath: true,
        resetDisabled: true
      }
    }
    const id = modpackSettingsReadyId
    const savedCustom = settings.modpackInstanceParentPath?.[id]?.trim() ?? ''
    const previewParent = instancePathUiPreview?.parentPath
    const hasPreviewCustom = typeof previewParent === 'string' && previewParent.trim() !== ''
    const displayedRoot =
      instancePathUiPreview?.resolvedRoot ?? packInstanceDetails?.instanceRoot ?? ''
    const pendingResetToDefault =
      instancePathUiPreview !== null &&
      instancePathUiPreview.parentPath === null &&
      Boolean(savedCustom)
    const resetDisabled =
      packMaintBusy ||
      instancePathBusy ||
      (!savedCustom && !hasPreviewCustom) ||
      pendingResetToDefault
    const showCustomBadge = instancePathUiPreview !== null ? hasPreviewCustom : Boolean(savedCustom)
    const showLoadingPath = packInstanceDetails === null && instancePathUiPreview === null
    return {
      displayedRoot,
      showCustomBadge,
      showLoadingPath,
      resetDisabled
    }
  }, [
    modpackSettingsReadyId,
    settings.modpackInstanceParentPath,
    instancePathUiPreview,
    packInstanceDetails,
    packMaintBusy,
    instancePathBusy,
    instancePathConfirm
  ])

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
    if (screen !== 'app') return
    void fetchAndCacheActuSolea().then((r) => {
      if (r.ok) window.dispatchEvent(new Event(ACTU_SOLEA_UPDATED_EVENT))
    })
  }, [screen])

  useEffect(() => {
    const off = window.solea.onInstallProgress((p) => {
      if (fakeInstallActiveRef.current) return
      const isUninstall = p.task === 'uninstall' || p.phase === 'uninstall'
      if (p.detail === '__scan__') {
        setInstallLine(isUninstall ? t('uninstall.scanning') : t('install.prepare'))
      } else {
        setInstallLine(p.detail ?? p.phase)
      }
      if (p.total > 0) {
        setInstallIndeterminate(false)
        setInstallPct(Math.round((p.current / p.total) * 100))
      } else {
        setInstallIndeterminate(true)
        setInstallPct(0)
      }
    })
    return off
  }, [t])

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
    if (!shortcutCapture) return
    const onKeyDownCapture = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault()
        e.stopPropagation()
        setShortcutCapture(null)
        return
      }
      const accel = keyboardEventToAcceleratorString(e)
      if (!accel) return
      e.preventDefault()
      e.stopPropagation()
      const which = shortcutCapture
      const s = settingsRef.current
      const next: LauncherSettingsUI =
        which === 'open'
          ? { ...s, uiShortcutOpenSettings: accel }
          : which === 'news'
            ? { ...s, uiShortcutGoNews: accel }
            : { ...s, uiShortcutGoAccount: accel }
      if (
        new Set([next.uiShortcutOpenSettings, next.uiShortcutGoNews, next.uiShortcutGoAccount]).size !== 3
      ) {
        pushToast(t('settings.shortcutDuplicate'), 'error', 5000)
        setShortcutCapture(null)
        return
      }
      setSettings(next)
      void window.solea.saveSettings(serializeLauncherSettingsForIpc(next))
      setShortcutCapture(null)
    }
    window.addEventListener('keydown', onKeyDownCapture, true)
    return () => window.removeEventListener('keydown', onKeyDownCapture, true)
  }, [shortcutCapture, pushToast, t])

  useEffect(() => {
    if (screen !== 'app') return
    const onKey = (e: KeyboardEvent) => {
      if (e.repeat) return
      if (shortcutCapture) return
      const s = settingsRef.current
      if (acceleratorMatches(s.uiShortcutOpenSettings, e)) {
        e.preventDefault()
        setView('settings')
        setSettingsTab('launcher')
        return
      }
      if (acceleratorMatches(s.uiShortcutGoNews, e)) {
        e.preventDefault()
        tryLeaveSettings(() => setView('news'))
        return
      }
      if (acceleratorMatches(s.uiShortcutGoAccount, e)) {
        e.preventDefault()
        const acc = activeAcc
        if (acc?.offline) return
        tryLeaveSettings(() => setView('account'))
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [screen, tryLeaveSettings, activeAcc?.offline, shortcutCapture])

  useEffect(() => {
    // Pas de cursor:wait global pendant install / MAJ / désinstall : la barre en bas indique l’état.
    // packMaintBusy seul ne doit pas figer le curseur (réinstall / désinstall avec progression).
    const busy = phase === 'busy'
    document.body.classList.toggle('solea-global-busy', busy)
    return () => document.body.classList.remove('solea-global-busy')
  }, [phase])

  const homeActivityLabels = useMemo(() => {
    const a = isModpackId(activeModpackId) ? modpackActivityById[activeModpackId] : undefined
    const fmt = (iso?: string | null) => {
      if (iso === null || iso === undefined || !String(iso).trim()) return null as string | null
      const d = new Date(iso)
      return Number.isNaN(d.getTime()) ? null : formatDate(d)
    }
    const lastLauncherUpdate = fmt(launcherMeta?.lastLauncherUpdatedAt ?? undefined)
    return { lastPlay: fmt(a?.lastPlayAt), lastLauncherUpdate }
  }, [activeModpackId, modpackActivityById, launcherMeta, formatDate])

  const quickLaunchPick = useMemo(() => {
    if (!allModpacksAction?.length) return null
    const candidates = allModpacksAction.filter(
      (p) => isModpackId(p.id) && !p.needsInstall && !p.error
    )
    if (candidates.length === 0) return null
    let best: { id: ModpackIdUi; lastPlayAt: string; displayName: string } | null = null
    for (const p of candidates) {
      const lp = modpackActivityById[p.id]?.lastPlayAt
      if (!lp) continue
      if (!best || new Date(lp).getTime() > new Date(best.lastPlayAt).getTime()) {
        best = { id: p.id as ModpackIdUi, lastPlayAt: lp, displayName: p.displayName }
      }
    }
    return best
  }, [allModpacksAction, modpackActivityById])

  const quickLaunchAnyInstalled = useMemo(() => {
    if (!allModpacksAction?.length) return false
    return allModpacksAction.some((p) => isModpackId(p.id) && !p.needsInstall && !p.error)
  }, [allModpacksAction])

  useEffect(() => {
    if (!quickLaunchPick) {
      setQuickLaunchHint(null)
      return
    }
    let cancelled = false
    void window.solea.getQuickLaunchHint(quickLaunchPick.id).then((h) => {
      if (!cancelled) setQuickLaunchHint(h)
    })
    return () => {
      cancelled = true
    }
  }, [quickLaunchPick])

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

  const switchActiveAccount = async (uuid: string, options?: { refreshSkinKey?: boolean }) => {
    if (activeAcc?.uuid === uuid) return
    const acc = accounts.find((a) => a.uuid === uuid)
    if (!acc) return
    const r = await window.solea.setActiveAccount(uuid)
    if (!r.ok) return
    await loadAccounts()
    setMenuOpen(false)
    if (options?.refreshSkinKey) setAccountSkinKey((k) => k + 1)
    pushToast(
      t(acc.offline ? 'account.toastSwitchOffline' : 'account.toastSwitchMicrosoft', { name: acc.name }),
      'success',
      5200
    )
  }

  const onSelectAccount = (uuid: string) => void switchActiveAccount(uuid)

  const onRemoveAccount = async (uuid: string) => {
    await window.solea.removeAccount(uuid)
    setMenuOpen(false)
    await loadAccounts()
    await refreshAuth()
  }

  const openAddAccountModal = () => {
    setAddAccountStep('pick')
    setAddAccountOfflineName('')
    setAddAccountErr(null)
    setAddAccountModalOpen(true)
  }

  const closeAddAccountModal = () => {
    if (addAccountBusy) return
    setAddAccountModalOpen(false)
    setAddAccountStep('pick')
    setAddAccountOfflineName('')
    setAddAccountErr(null)
  }

  const onAddAccount = () => {
    openAddAccountModal()
  }

  const handleAddAccountMicrosoft = async () => {
    if (addAccountBusy) return
    setAddAccountBusy(true)
    setPhase('busy')
    setAddAccountErr(null)
    try {
      const r = await window.solea.addAccount()
      if (r.ok) {
        await loadAccounts()
        setMenuOpen(false)
        setAddAccountModalOpen(false)
        setAddAccountStep('pick')
        pushToast(t('toast.accountAdded', { name: r.name }), 'success')
      } else if (r.reason !== 'cancelled') {
        pushToast(r.detail ?? t('toast.accountFail'), 'error')
      }
    } finally {
      setAddAccountBusy(false)
      setPhase('idle')
    }
  }

  const handleAddAccountOfflineSubmit = async () => {
    if (addAccountBusy) return
    const name = addAccountOfflineName.trim()
    setAddAccountBusy(true)
    setPhase('busy')
    setAddAccountErr(null)
    try {
      const r = await window.solea.addOfflineAccount(name)
      if (!r.ok) {
        setAddAccountErr(r.error)
        return
      }
      await loadAccounts()
      setMenuOpen(false)
      setAddAccountModalOpen(false)
      setAddAccountStep('pick')
      setAddAccountOfflineName('')
      pushToast(t('toast.accountAdded', { name }), 'success')
    } catch (e) {
      const detail = e instanceof Error ? e.message : String(e)
      setAddAccountErr(t('login.offlineSaveFailed', { detail }))
    } finally {
      setAddAccountBusy(false)
      setPhase('idle')
    }
  }

  const installModpackForId = async (packId: ModpackIdUi) => {
    if (installBusyRef.current) return
    installBusyRef.current = true
    setPhase('installing')
    setInstallPct(0)
    setInstallLine(t('install.prepare'))
    try {
      const r = await window.solea.installModpackPack(packId)
      if (r.ok) {
        setWarmupPending(packId)
        pushToast(t('toast.installDone'), 'success')
        void playUiSound('install', uiSoundPrefs)
        void refreshAllModpacksAction()
        void refreshModpackActivity()
      } else pushToast(r.error, 'error')
    } finally {
      setPhase('idle')
      setInstallIndeterminate(false)
      installBusyRef.current = false
    }
  }

  const onInstall = async () => {
    await installModpackForId(activeModpackId)
  }

  const clearFakeInstallSimulation = useCallback(() => {
    if (fakeInstallIntervalRef.current != null) {
      clearInterval(fakeInstallIntervalRef.current)
      fakeInstallIntervalRef.current = null
    }
    fakeInstallActiveRef.current = false
  }, [])

  const runFakeInstallSimulation = useCallback(
    (kind: 'install' | 'update', opts?: { fromDebug?: boolean }) => {
      if (!opts?.fromDebug && !testMode) return
      if (phase !== 'idle' || launchPhase !== 'idle') return
      if (installBusyRef.current) return
      clearFakeInstallSimulation()
      fakeInstallActiveRef.current = true
      setPhase('installing')
      setInstallPct(0)
      const prepareLine =
        kind === 'install' ? t('debug.fakePrepareInstall') : t('debug.fakePrepareUpdate')
      setInstallLine(prepareLine)
      let step = 0
      const maxSteps = 52
      fakeInstallIntervalRef.current = window.setInterval(() => {
        step++
        const pct = Math.min(100, Math.round((step / maxSteps) * 100))
        setInstallPct(pct)
        if (step <= 4) {
          setInstallLine(prepareLine)
        } else {
          const progress = (step - 4) / (maxSteps - 4)
          const idx = Math.min(
            FAKE_INSTALL_DEBUG_FILES.length - 1,
            Math.floor(progress * FAKE_INSTALL_DEBUG_FILES.length)
          )
          setInstallLine(FAKE_INSTALL_DEBUG_FILES[idx]!)
        }
        if (step >= maxSteps) {
          clearFakeInstallSimulation()
          setPhase('idle')
          setInstallPct(0)
          setInstallLine('')
          pushToast(t('debug.fakeInstallDone'), 'info', 3200)
        }
      }, 80)
    },
    [testMode, phase, launchPhase, clearFakeInstallSimulation, t, pushToast]
  )

  useEffect(() => {
    const off = window.solea.onDebugFakeInstall((kind) => {
      setView('home')
      runFakeInstallSimulation(kind, { fromDebug: true })
    })
    return off
  }, [runFakeInstallSimulation])

  useEffect(() => {
    return () => {
      if (fakeInstallIntervalRef.current != null) {
        clearInterval(fakeInstallIntervalRef.current)
        fakeInstallIntervalRef.current = null
      }
      fakeInstallActiveRef.current = false
    }
  }, [])

  useEffect(() => {
    if (view !== 'home' && fakeInstallActiveRef.current) {
      if (fakeInstallIntervalRef.current != null) {
        clearInterval(fakeInstallIntervalRef.current)
        fakeInstallIntervalRef.current = null
      }
      fakeInstallActiveRef.current = false
      setPhase('idle')
      setInstallPct(0)
      setInstallLine('')
      setInstallIndeterminate(false)
    }
  }, [view])

  const onCheckModrinthUpdate = async () => {
    if (checkUpdateBusy || installBusyRef.current || phase !== 'idle' || launchPhase !== 'idle') return
    if (!isModpackId(activeModpackId)) return
    setCheckUpdateBusy(true)
    try {
      await refreshAllModpacksAction({ force: true })
      const info = await window.solea.getModpackActionInfo()
      if (info.error) {
        pushToast(info.error, 'error')
        return
      }
      if (info.needsInstall || info.needsUpdate) {
        pushToast(t('home.checkUpdateApplying'), 'info', 5200)
        await installModpackForId(activeModpackId)
        return
      }
      pushToast(t('home.checkUpdateUpToDate'), 'success', 4200)
    } finally {
      setCheckUpdateBusy(false)
    }
  }

  const executeReinstallModpack = async (packId: ModpackIdUi, preserve: ReinstallPreserveOptions) => {
    setPackMaintBusy(true)
    setPhase('installing')
    setInstallPct(0)
    setInstallLine(t('install.reinstall'))
    try {
      const r = await window.solea.reinstallModpack(packId, preserve)
      if (!r.ok) {
        pushToast(r.error, 'error')
        return
      }
      setWarmupPending(packId)
      pushToast(t('toast.reinstalled'), 'success')
      void playUiSound('install', uiSoundPrefs)
      void refreshAllModpacksAction()
      void refreshModpackActivity()
    } finally {
      setPhase('idle')
      setPackMaintBusy(false)
      setInstallIndeterminate(false)
    }
  }

  const executeUninstallModpack = async (packId: ModpackIdUi) => {
    setPackMaintBusy(true)
    setPhase('uninstalling')
    setInstallPct(0)
    setInstallIndeterminate(true)
    setInstallLine(t('uninstall.scanning'))
    try {
      const r = await window.solea.uninstallModpack(packId)
      if (!r.ok) {
        pushToast(r.error, 'error')
        return
      }
      consumeWarmupPending(packId)
      pushToast(t('toast.uninstalled'), 'success')
      void refreshAllModpacksAction()
    } finally {
      setPackMaintBusy(false)
      setPhase('idle')
      setInstallLine('')
      setInstallPct(0)
      setInstallIndeterminate(false)
    }
  }

  const onReinstallModpack = (packId: ModpackIdUi) => {
    setPackMaintConfirm({ kind: 'reinstall', packId })
  }

  const onUninstallModpack = (packId: ModpackIdUi) => {
    setPackMaintConfirm({ kind: 'uninstall', packId })
  }

  const runInstancePathApply = async (mode: 'move' | 'reinstall') => {
    if (!instancePathConfirm) return
    const { packId, parentPath } = instancePathConfirm
    setInstancePathBusy(true)
    const r = await window.solea.applyInstanceParentPath({ modpackId: packId, parentPath, mode })
    setInstancePathBusy(false)
    if (!r.ok) {
      pushToast(r.error, 'error')
      return
    }
    setInstancePathConfirm(null)
    setInstancePathUiPreview(null)
    void window.solea
      .getSettings()
      .then((s) => setSettings(normalizeLauncherSettingsUi(s as LauncherSettingsUI)))
    void refreshAllModpacksAction()
    pushToast(t('settings.instancePathSaved'), 'success')
  }

  const onPickInstanceInstallFolder = async () => {
    if (!modpackSettingsReadyId || !isModpackId(modpackSettingsReadyId)) return
    const packId = modpackSettingsReadyId
    const pick = await window.solea.pickInstanceParentFolder()
    if (!pick.ok) return
    const preview = await window.solea.getInstancePathPreview(packId, pick.path)
    if (!preview.valid) {
      pushToast(preview.error ?? t('settings.instancePathConflict'), 'error')
      return
    }
    if (preview.oldRoot === preview.newRoot) {
      pushToast(t('settings.instancePathNoChange'), 'info')
      return
    }
    const det = await window.solea.getModpackInstanceDetails(packId)
    const needsInstancePathModal =
      det.folderExists && (det.installed || (det.sizeBytes != null && det.sizeBytes > 0))
    if (needsInstancePathModal) {
      setInstancePathUiPreview({ parentPath: pick.path, resolvedRoot: preview.newRoot })
      setInstancePathConfirm({ packId, parentPath: pick.path, newRootPreview: preview.newRoot })
      return
    }
    setInstancePathUiPreview({ parentPath: pick.path, resolvedRoot: preview.newRoot })
    setInstancePathBusy(true)
    const r = await window.solea.applyInstanceParentPath({
      modpackId: packId,
      parentPath: pick.path,
      mode: 'move'
    })
    setInstancePathBusy(false)
    if (!r.ok) {
      setInstancePathUiPreview(null)
      pushToast(r.error, 'error')
      return
    }
    setInstancePathUiPreview(null)
    void window.solea
      .getSettings()
      .then((s) => setSettings(normalizeLauncherSettingsUi(s as LauncherSettingsUI)))
    void refreshAllModpacksAction()
    pushToast(t('settings.instancePathSaved'), 'success')
  }

  const onResetInstanceInstallFolder = async () => {
    if (!modpackSettingsReadyId || !isModpackId(modpackSettingsReadyId)) return
    const packId = modpackSettingsReadyId
    const preview = await window.solea.getInstancePathPreview(packId, null)
    if (!preview.valid) {
      pushToast(preview.error ?? t('settings.instancePathConflict'), 'error')
      return
    }
    if (preview.oldRoot === preview.newRoot) {
      pushToast(t('settings.instancePathNoChange'), 'info')
      return
    }
    const det = await window.solea.getModpackInstanceDetails(packId)
    const needsInstancePathModal =
      det.folderExists && (det.installed || (det.sizeBytes != null && det.sizeBytes > 0))
    if (needsInstancePathModal) {
      setInstancePathUiPreview({ parentPath: null, resolvedRoot: preview.newRoot })
      setInstancePathConfirm({ packId, parentPath: null, newRootPreview: preview.newRoot })
      return
    }
    setInstancePathUiPreview({ parentPath: null, resolvedRoot: preview.newRoot })
    setInstancePathBusy(true)
    const r = await window.solea.applyInstanceParentPath({ modpackId: packId, parentPath: null, mode: 'move' })
    setInstancePathBusy(false)
    if (!r.ok) {
      setInstancePathUiPreview(null)
      pushToast(r.error, 'error')
      return
    }
    setInstancePathUiPreview(null)
    void window.solea
      .getSettings()
      .then((s) => setSettings(normalizeLauncherSettingsUi(s as LauncherSettingsUI)))
    void refreshAllModpacksAction()
    pushToast(t('settings.instancePathSaved'), 'success')
  }

  const onCacheClearConfirmResolved = () => {
    if (!cacheClearConfirm) return
    const kind = cacheClearConfirm
    setCacheClearConfirm(null)
    const cacheKey = kind === 'launcher' ? 'launcherCaches' : 'launcherLogs'
    void window.solea.clearCache(cacheKey).then((r) => {
      if (r.ok) {
        const msg =
          cacheKey === 'launcherCaches' && r.partialDisk
            ? t('settings.cacheFreedPartial', { n: formatBytes(r.freedBytes) })
            : t('settings.cacheFreed', { n: formatBytes(r.freedBytes) })
        pushToast(msg, cacheKey === 'launcherCaches' && r.partialDisk ? 'info' : 'success')
        void window.solea.getCacheStats().then(setCacheStats)
      } else pushToast(r.error, 'error')
    })
  }

  const onMemoryClearConfirmResolved = () => {
    setMemoryClearOpen(false)
    void window.solea.clearLauncherMemory().then((r) => {
      if (r.ok) {
        pushToast(t('settings.memoryClearedToast'), 'success')
        void refreshModpackActivity()
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
    const packForWarmup = activeModpackId
    const warmupEligible = isModpackId(packForWarmup) && peekWarmupPending(packForWarmup)
    if (warmupEligible) {
      pushToast(t('home.firstLaunchWarmup'), 'info', 14_000)
    }
    setLaunchPhase('launching')
    void playUiSound('launch', uiSoundPrefs)
    try {
      const r = await window.solea.launch()
      if (!r.ok) {
        setLaunchPhase('idle')
        pushToast(r.error, 'error')
        return
      }
      if (warmupEligible) consumeWarmupPending(packForWarmup)
      setLaunchPhase('running')
      void refreshModpackActivity()
    } finally {
      launchBusyRef.current = false
    }
  }

  const onQuickLaunchFromNews = () => {
    if (!quickLaunchPick) return
    tryLeaveSettings(async () => {
      await runSelectModpack(quickLaunchPick.id)
      await onLaunchOrClose()
    })
  }

  const onSettingsVerify = async () => {
    if (!modpackSettingsReadyId || !isModpackId(modpackSettingsReadyId)) return
    setSettingsVerifyResult(null)
    const r = await window.solea.verifyModpackFor(modpackSettingsReadyId)
    if (r.ok) {
      setSettingsVerifyResult({ ok: true })
      return
    }
    setSettingsVerifyResult({
      ok: false,
      reason: r.reason,
      detail: r.detail,
      paths: r.paths
    })
    if (r.reason !== 'extra_mod') pushToast(r.detail ?? r.reason, 'error')
  }

  const onSettingsVerifyRepair = () => {
    if (!modpackSettingsReadyId || !isModpackId(modpackSettingsReadyId)) return
    const row = allModpacksAction?.find((p) => p.id === modpackSettingsReadyId)
    if (row?.needsInstall || row?.needsUpdate) {
      void installModpackForId(modpackSettingsReadyId)
      return
    }
    onReinstallModpack(modpackSettingsReadyId)
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
    if (r.ok) {
      settingsBaselineRef.current = cloneLauncherSettings(settings)
      pushToast(t('settings.saveApplied'), 'success')
    } else {
      pushToast(r.error, 'error')
      setSettingsFb({ text: r.error, ok: false })
    }
  }

  const resetAllSettings = async () => {
    const r = await window.solea.resetSettings()
    if (r.ok) {
      const s = await window.solea.getSettings()
      setSettings(normalizeLauncherSettingsUi(s as LauncherSettingsUI))
      settingsBaselineRef.current = cloneLauncherSettings(s)
      setSettingsFb({ text: t('settings.resetOk'), ok: true })
    } else setSettingsFb({ text: r.error, ok: false })
  }

  const openSettingsResetTabConfirm = () => {
    if (settingsTab === 'launcher') {
      setSettingsResetConfirm('launcher-tab')
    } else if (isModpackId(settingsTab)) {
      setSettingsResetConfirm(settingsTab)
    }
  }

  const handleConfirmSettingsReset = async () => {
    const kind = settingsResetConfirm
    setSettingsResetConfirm(null)
    if (!kind) return
    if (kind === 'all') {
      await resetAllSettings()
      return
    }
    if (kind === 'launcher-tab') {
      setSettings((s) => applyLauncherTabDefaults(s))
      pushToast(t('settings.resetTabToast'), 'info')
      return
    }
    if (isModpackId(kind)) {
      setSettings((s) => applyModpackTabDefaults(s, kind))
      pushToast(t('settings.resetTabToast'), 'info')
    }
  }

  const settingsResetTabDisabled =
    isModpackId(settingsTab) && modpackSettingsReadyId !== settingsTab

  const settingsUnsavedSections = useMemo(() => {
    if (!settingsUnsavedOpen) return []
    const b = settingsBaselineRef.current
    if (!b) return []
    return buildUnsavedSettingsSections(b, settings, modpacksList, t)
  }, [settingsUnsavedOpen, settings, modpacksList, t])

  const handleUnsavedSave = async () => {
    setSettingsUnsavedBusy(true)
    setSettingsFb(null)
    const r = await window.solea.saveSettings(serializeLauncherSettingsForIpc(settingsRef.current))
    setSettingsUnsavedBusy(false)
    if (!r.ok) {
      pushToast(r.error, 'error')
      setSettingsFb({ text: r.error, ok: false })
      return
    }
    settingsBaselineRef.current = cloneLauncherSettings(settingsRef.current)
    pushToast(t('settings.saveApplied'), 'success')
    setSettingsUnsavedOpen(false)
    const fn = pendingNavRef.current
    pendingNavRef.current = null
    await fn?.()
  }

  const handleUnsavedDiscard = () => {
    const b = settingsBaselineRef.current
    if (b) setSettings(cloneLauncherSettings(b))
    setSettingsUnsavedOpen(false)
    const fn = pendingNavRef.current
    pendingNavRef.current = null
    void fn?.()
  }

  useFocusTrap(reportModalOpen, reportModalRef, {
    onEscape: () => setReportModalOpen(false)
  })

  useFocusTrap(addAccountModalOpen, addAccountModalRef, {
    onEscape: () => {
      if (!addAccountBusy) closeAddAccountModal()
    }
  })

  useFocusTrap(settingsResetConfirm !== null, settingsResetModalRef, {
    onEscape: () => setSettingsResetConfirm(null)
  })

  const homePackReadyA11y =
    view === 'home' &&
    !modpackUi.loading &&
    !modpackUi.error &&
    !modpackUi.needsInstall &&
    !modpackUi.needsUpdate &&
    launchPhase === 'idle'

  const shellFooterLegalName = view === 'home' ? modpackName : t('home.footerProductName')

  if (screen === 'boot') {
    return (
      <div className="app-chrome">
        <div className="app-chrome-body">
          <TitleBar />
          <div className="app-fill">
          <div className="boot-screen">
            <div className="boot-screen-inner">
              <div className="boot-logo-wrap">
                <img src={bootLogoUrl} alt="Solea Pixel" className="boot-logo" />
              </div>
              <div className="boot-progress-wrap">
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
    <div className="app-chrome" data-app-view={view} data-home-card={settings.uiHomeCardVariant}>
      {settings.uiChromeGlass ? (
        <div
          key={view === 'home' && isModpackId(activeModpackId) ? activeModpackId : view}
          className="app-chrome-wallpaper"
          style={{ backgroundImage: `url(${chromeWallpaperUrl})` }}
          aria-hidden
        />
      ) : null}
      <div className="app-chrome-body">
        <TitleBar
          showFloatingTitle={view === 'home' && isModpackId(activeModpackId)}
        />
        <div className="app-fill">
    <div className="shell" data-home-card={settings.uiHomeCardVariant}>
      <aside className="shell-sidebar" aria-label={t('shell.sidebarAria')}>
        <div className="sb-rail-section sb-rail-section--top">
          <button
            type="button"
            className={`sb-btn ${view === 'news' ? 'active' : ''}`}
            title={t('shell.home')}
            aria-label={t('shell.home')}
            onClick={() => tryLeaveSettings(() => setView('news'))}
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
          {!activeAcc?.offline && settings.experimentalServerSystemEnabled ? (
            <button
              type="button"
              className={`sb-btn ${view === 'my-server' ? 'active' : ''}`}
              title={t('shell.myServer')}
              aria-label={t('shell.myServer')}
              onClick={() => tryLeaveSettings(() => setView('my-server'))}
            >
              <IconServer />
            </button>
          ) : null}
        </div>
        <div className="sb-rail-section sb-rail-section--middle" role="navigation" aria-label={t('shell.navPacks')}>
          {modpacksList.map((m) =>
            isModpackId(m.id) ? (
              <button
                key={m.id}
                type="button"
                className={`sb-btn sb-btn-pack ${MODPACK_THEME[m.id].themeClass} ${
                  view === 'home' && activeModpackId === m.id ? 'active' : ''
                }`}
                title={m.displayName}
                onClick={() => void selectModpack(m.id)}
              >
                <img src={MODPACK_THEME[m.id].sidebarIcon} alt="" className="sb-pack-icon" />
              </button>
            ) : null
          )}
        </div>
        <div className="sb-rail-section sb-rail-section--bottom">
          <button
            type="button"
            className={`sb-btn ${view === 'screenshots' ? 'active' : ''}`}
            title={t('shell.screenshots')}
            aria-label={t('shell.screenshots')}
            onClick={() => tryLeaveSettings(() => setView('screenshots'))}
          >
            <IconScreenshots />
          </button>
          {!activeAcc?.offline ? (
            <button
              type="button"
              className={`sb-btn ${view === 'account' ? 'active' : ''}`}
              title={t('shell.account')}
              aria-label={t('shell.account')}
              onClick={() => tryLeaveSettings(() => setView('account'))}
            >
              <IconUser />
            </button>
          ) : null}
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
          view === 'settings' || view === 'account' || view === 'my-server' ? 'settings-mode' : ''
        } ${view === 'news' || view === 'screenshots' || view === 'my-server' ? 'shell-main-news' : ''} ${
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
        {(view === 'news' || view === 'screenshots' || (view === 'my-server' && settings.uiChromeGlass)) && (
          <div
            className="shell-main-wallpaper"
            style={{ backgroundImage: `url(${NEWS_WALLPAPER})` }}
            aria-hidden
          />
        )}
        <div
          key={view === 'home' && isModpackId(activeModpackId) ? `home-${activeModpackId}` : view}
          className="shell-view-shell"
        >
        {view === 'home' && (
          <>
          <div className="shell-content shell-content-home">
            {testMode && <div className="test-strip home-test-strip">{t('home.testStrip')}</div>}
            {testMode && (
              <div className="home-debug-fake-install" role="region" aria-label={t('debug.fakeHomeAria')}>
                <button
                  type="button"
                  className="btn-muted home-debug-fake-install__btn"
                  disabled={phase !== 'idle' || launchPhase !== 'idle'}
                  onClick={() => runFakeInstallSimulation('install')}
                >
                  {t('debug.fakeHomeInstall')}
                </button>
                <button
                  type="button"
                  className="btn-muted home-debug-fake-install__btn"
                  disabled={phase !== 'idle' || launchPhase !== 'idle'}
                  onClick={() => runFakeInstallSimulation('update')}
                >
                  {t('debug.fakeHomeUpdate')}
                </button>
              </div>
            )}

            <div
              className={`home-panel${settings.uiHomeCardVariant === 'studio' ? ' home-panel--studio' : ''}`}
            >
              <div className="home-panel-inner">
                <div
                  className={`home-body${settings.uiHomeCardVariant === 'studio' ? ' home-body--studio' : ''}`}
                >
                  {settings.uiHomeCardVariant === 'studio' ? (
                    <section className="home-studio-hero" aria-labelledby="home-studio-title">
                      <div className="home-studio-hero-glow" aria-hidden />
                      <div className="home-studio-accent" aria-hidden />
                      <div className="home-studio-tags-row">
                        <HomeHeroTagsPack activeModpackId={activeModpackId} studio t={t} />
                      </div>
                      <p className="home-studio-eyebrow">{t('home.studioEyebrow')}</p>
                      <div className="home-studio-main">
                        <HomePackTitlePack modpackName={modpackName} studio />
                        <div className="home-studio-lead-wrap">
                          <HomePackLeadPack activeModpackId={activeModpackId} studio t={t} />
                        </div>
                      </div>
                    </section>
                  ) : (
                    <header className="home-hero-head">
                      <HomeHeroTagsPack activeModpackId={activeModpackId} studio={false} t={t} />
                      <HomePackTitlePack modpackName={modpackName} studio={false} />
                      <HomePackLeadPack activeModpackId={activeModpackId} studio={false} t={t} />
                    </header>
                  )}

                  {!modpackUi.loading && modpackUi.error ? (
                    <p className="home-modpack-err" role="alert">
                      {modpackUi.error}
                    </p>
                  ) : null}

                  <div className="home-action-deck-wrap">
                  <div
                    className={`home-action-deck${
                      settings.uiHomeCardVariant === 'studio' ? ' home-action-deck--studio' : ''
                    }`}
                  >
                <div className={`play-row${packNeedsAction ? ' play-row--pack-cta' : ''}`}>
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
                        className={`btn-play btn-play--pack-action${reduceMotionEffective ? '' : ' btn-play--pulse-soft'} btn-play--muted${
                          primaryActionAck ? ' btn-play--action-ack' : ''
                        }`}
                        disabled={phase !== 'idle' || launchPhase !== 'idle'}
                        aria-describedby={homePackReadyA11y ? 'home-pack-ready-sr' : undefined}
                        onPointerDown={() => {
                          if (phase === 'idle' && launchPhase === 'idle') triggerPrimaryActionAck()
                        }}
                        onClick={() => void onInstall()}
                      >
                        <span className="btn-play-label btn-play-label--wrap">
                          {modpackUi.needsInstall ? t('home.ctaInstallAction') : t('home.ctaUpdateAction')}
                        </span>
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
                      }${launchPhase === 'launching' ? ' btn-play--launching' : ''}${
                        primaryActionAck ? ' btn-play--action-ack' : ''
                      }`}
                      disabled={
                        launchPhase === 'launching' ||
                        backgroundInstallOrUninstall ||
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
                      onPointerDown={() => {
                        if (
                          launchPhase !== 'launching' &&
                          !backgroundInstallOrUninstall &&
                          phase !== 'busy' &&
                          !modpackUi.loading &&
                          !modpackUi.needsInstall &&
                          !modpackUi.needsUpdate
                        ) {
                          triggerPrimaryActionAck()
                        }
                      }}
                      onClick={() => void onLaunchOrClose()}
                    >
                      {launchPhase === 'running' ? <IconStop /> : <IconPlay />}
                      <span
                        className={`btn-play-label${
                          launchPhase === 'launching'
                            ? ' btn-play-label--ellipsis'
                            : launchPhase === 'running'
                              ? ' btn-play-label--close'
                              : ''
                        }`}
                      >
                        {launchPhase === 'launching'
                          ? `${t('home.playLaunchingBase')}${settings.uiReduceMotion ? '…' : '.'.repeat(launchDots)}`
                          : launchPhase === 'running'
                            ? t('home.playClose')
                            : t('home.play')}
                      </span>
                    </button>
                  )}
                  {!modpackUi.needsInstall ? (
                    <>
                      <button
                        type="button"
                        className="btn-check-update"
                        disabled={
                          phase !== 'idle' ||
                          launchPhase !== 'idle' ||
                          checkUpdateBusy ||
                          !isModpackId(activeModpackId)
                        }
                        title={t('home.checkUpdateTooltip')}
                        aria-label={t('home.checkUpdateAria')}
                        onClick={() => void onCheckModrinthUpdate()}
                      >
                        <IconCheckUpdates
                          className={
                            checkUpdateBusy && !reduceMotionEffective ? 'btn-check-update-icon--spin' : undefined
                          }
                        />
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
                              <SkinHead
                                uuid={activeAcc?.uuid}
                                offline={activeAcc?.offline}
                                sizePx={64}
                                className="profile-bar-head"
                              />
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
                                            <SkinHead
                                              uuid={a.uuid}
                                              offline={a.offline}
                                              sizePx={40}
                                              className="profile-menu-head"
                                            />
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
                    </>
                  ) : null}
                </div>
                  </div>
                  </div>

                {launchPhase === 'launching' && (
                  <button
                    type="button"
                    className="btn-quiet home-log-console-btn"
                    onClick={() => void window.solea.openGameLogWindow()}
                  >
                    {t('home.logConsole')}
                  </button>
                )}

                {(homeActivityLabels.lastLauncherUpdate || homeActivityLabels.lastPlay) && (
                  <footer className="home-panel-foot">
                    <div className="home-last-activity">
                      <p className="home-last-activity-label">{t('home.lastActivityTitle')}</p>
                      <ul className="home-last-activity-list">
                        {homeActivityLabels.lastLauncherUpdate ? (
                          <li>
                            {t('home.lastLauncherUpdated', {
                              date: homeActivityLabels.lastLauncherUpdate
                            })}
                          </li>
                        ) : null}
                        {homeActivityLabels.lastPlay ? (
                          <li>{t('home.lastPlay', { date: homeActivityLabels.lastPlay })}</li>
                        ) : null}
                      </ul>
                    </div>
                  </footer>
                )}
                </div>
              </div>
            </div>
          </div>
          <footer className="shell-footer">{t('home.footer', { name: shellFooterLegalName })}</footer>
          </>
        )}

        {view === 'news' && (
          <>
            <div
              className={`shell-content shell-content-news news-hub-layout news-hub-layout--${
                settings.uiHomeCardVariant === 'studio' ? 'v2' : 'v1'
              }`}
            >
              {testMode && <div className="test-strip home-test-strip">{t('home.testStrip')}</div>}
              <div className="news-hub-body">
                <div className="news-hub-canvas">
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
                          <SkinHead
                            uuid={activeAcc?.uuid}
                            offline={activeAcc?.offline}
                            sizePx={112}
                            className="news-hub-skin"
                          />
                        </div>
                        <div className="news-hub-username-row">
                          <p className="news-hub-username">{activeAcc?.name ?? t('home.profileFallback')}</p>
                          {activeAcc?.offline ? (
                            <span className="news-hub-offline-badge">{t('newsView.offlineBadge')}</span>
                          ) : null}
                        </div>
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
                            <SkinHead
                              uuid={a.uuid}
                              offline={a.offline}
                              sizePx={28}
                              className="news-hub-acc-tab-head"
                            />
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
                      aria-label={
                        feedTab === 'actu' ? t('actu.panelTitle') : t('changelog.panelTitle')
                      }
                    >
                      <div className="news-hub-changelog-toolbar news-hub-changelog-toolbar--with-tabs">
                        <h3 className="news-hub-changelog-title">
                          {feedTab === 'actu' ? t('actu.panelTitle') : t('changelog.panelTitle')}
                        </h3>
                        <FeedActuNewsTabs value={feedTab} onChange={setFeedTabPersist} />
                        {feedTab === 'updates' ? (
                          <span
                            className="news-hub-version-pill"
                            title={
                              launcherVersion.trim()
                                ? `${LAUNCHER_VERSION_DISPLAY} · ${launcherVersion.trim()}`
                                : LAUNCHER_VERSION_DISPLAY
                            }
                          >
                            {LAUNCHER_VERSION_DISPLAY}
                          </span>
                        ) : null}
                      </div>
                      <div className="news-hub-feed-swap" key={feedTab}>
                      {feedTab === 'actu' ? (
                        <ActuSoleaFeed />
                      ) : (
                        <>
                      <p className="news-hub-changelog-lead">{t('changelog.lead')}</p>
                      {LAUNCHER_CHANGELOG.length === 0 ? (
                        <p className="news-hub-changelog-empty">{t('changelog.empty')}</p>
                      ) : (
                        <div className="news-hub-changelog-scroll">
                          <div className="news-hub-releases">
                            {NEWS_CHANGELOG_LATEST ? (
                              <article
                                className="news-hub-spotlight-embed"
                                aria-labelledby="news-hub-spotlight-heading"
                              >
                                <div className="news-hub-spotlight-embed-head">
                                  <div className="news-hub-spotlight-embed-head-text">
                                    <p className="news-hub-spotlight-eyebrow">
                                      {t('newsView.spotlightEyebrow')}
                                    </p>
                                    <h3 id="news-hub-spotlight-heading" className="news-hub-spotlight-title">
                                      {t('newsView.spotlightTitle', {
                                        version: NEWS_CHANGELOG_LATEST.version
                                      })}
                                    </h3>
                                  </div>
                                  <div className="news-hub-spotlight-meta">
                                    <span className="news-hub-spotlight-ver">
                                      {NEWS_CHANGELOG_LATEST.version}
                                    </span>
                                    {NEWS_CHANGELOG_LATEST.date ? (
                                      <time
                                        className="news-hub-spotlight-date"
                                        dateTime={NEWS_CHANGELOG_LATEST.date}
                                      >
                                        {NEWS_CHANGELOG_LATEST.date}
                                      </time>
                                    ) : null}
                                  </div>
                                </div>
                                <div className="news-hub-spotlight-body">
                                  <ChangelogEntrySections entry={NEWS_CHANGELOG_LATEST} t={t} />
                                </div>
                                {NEWS_CHANGELOG_OLDER.length > 0 ? (
                                  <p className="news-hub-spotlight-footer">
                                    {t('newsView.spotlightFooter')}
                                  </p>
                                ) : null}
                              </article>
                            ) : null}
                            {NEWS_CHANGELOG_OLDER.length > 0 ? (
                              <>
                                <p
                                  className="news-hub-history-label news-hub-history-label--in-scroll"
                                  id="news-hub-history-start"
                                >
                                  {t('newsView.historyLabel')}
                                </p>
                                {NEWS_CHANGELOG_OLDER.map((entry) => (
                                  <article key={entry.version} className="news-hub-release">
                                    <div className="news-hub-release-head">
                                      <span className="news-hub-release-ver">{entry.version}</span>
                                      {entry.date ? (
                                        <time className="news-hub-release-date" dateTime={entry.date}>
                                          {entry.date}
                                        </time>
                                      ) : null}
                                    </div>
                                    <ChangelogEntrySections entry={entry} t={t} />
                                  </article>
                                ))}
                              </>
                            ) : null}
                          </div>
                        </div>
                      )}
                        </>
                      )}
                      </div>
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
                    <div className="news-hub-card news-hub-quick-card">
                      <div className="news-hub-quick-head">
                        <span className="news-hub-quick-eyebrow" aria-hidden>
                          ⚡
                        </span>
                        <div className="news-hub-quick-head-text">
                          <h3 className="news-hub-social-heading news-hub-quick-title">
                            {t('newsView.quickLaunchTitle')}
                          </h3>
                          {quickLaunchPick ? (
                            <p className="news-hub-quick-sub">{t('newsView.quickLaunchSubtitle')}</p>
                          ) : null}
                        </div>
                      </div>
                      {quickLaunchPick ? (
                        <>
                          <div className="news-hub-quick-body">
                            <img
                              src={MODPACK_THEME[quickLaunchPick.id].sidebarIcon}
                              alt=""
                              className="news-hub-quick-pack-ico"
                            />
                            <div className="news-hub-quick-meta">
                              <p className="news-hub-quick-pack-name">{quickLaunchPick.displayName}</p>
                              {quickLaunchHint ? (
                                <p className="news-hub-quick-hint">
                                  {quickLaunchHint.kind === 'world'
                                    ? t('newsView.quickLaunchWorld', { name: quickLaunchHint.label })
                                    : quickLaunchHint.kind === 'server'
                                      ? t('newsView.quickLaunchServer', { name: quickLaunchHint.label })
                                      : t('newsView.quickLaunchInstance', { name: quickLaunchHint.label })}
                                </p>
                              ) : (
                                <p className="news-hub-quick-hint news-hub-quick-hint--skeleton" aria-hidden />
                              )}
                              <p className="news-hub-quick-when">
                                {t('newsView.quickLaunchLastPlayed', {
                                  date: formatDate(new Date(quickLaunchPick.lastPlayAt))
                                })}
                              </p>
                            </div>
                          </div>
                          <button
                            type="button"
                            className="news-hub-quick-cta"
                            disabled={
                              launchPhase !== 'idle' ||
                              backgroundInstallOrUninstall ||
                              phase === 'busy' ||
                              packMaintBusy
                            }
                            onClick={onQuickLaunchFromNews}
                          >
                            <IconPlay className="news-hub-quick-cta-ico" />
                            <span>{t('newsView.quickLaunchPlay')}</span>
                          </button>
                        </>
                      ) : (
                        <div className="news-hub-quick-empty">
                          <div className="news-hub-quick-empty-inner">
                            <span className="news-hub-quick-empty-icon" aria-hidden>
                              ◈
                            </span>
                            <p className="news-hub-quick-empty-text">
                              {quickLaunchAnyInstalled
                                ? t('newsView.quickLaunchEmptyPlayedNever')
                                : t('newsView.quickLaunchEmpty')}
                            </p>
                          </div>
                          <button
                            type="button"
                            className="news-hub-quick-cta news-hub-quick-cta--ghost"
                            onClick={() => tryLeaveSettings(() => setView('home'))}
                          >
                            <span>{t('newsView.quickLaunchOpenHome')}</span>
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                </aside>
                </div>
                </div>
              </div>
            </div>
            <footer className="shell-footer">{t('home.footer', { name: shellFooterLegalName })}</footer>
          </>
        )}

        {view === 'screenshots' && (
          <>
            <Suspense
              fallback={
                <div className="screenshots-view screenshots-lazy-fallback">{t('screenshots.lazyLoading')}</div>
              }
            >
              <ScreenshotsViewLazy modpacksList={modpacksList} initialModpackId={activeModpackId} />
            </Suspense>
            <footer className="shell-footer">{t('home.footer', { name: shellFooterLegalName })}</footer>
          </>
        )}

        {view === 'my-server' && (
          <>
            <MyServerView modpacksList={modpacksList} chromeGlass={settings.uiChromeGlass} />
            <footer className="shell-footer">{t('home.footer', { name: shellFooterLegalName })}</footer>
          </>
        )}

        {view === 'settings' && (
          <div className="settings-layout" style={{ flex: 1, minHeight: 0 }}>
            <nav className="settings-nav" aria-label={t('settings.title')}>
              <div className="settings-nav-top">
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
                <p className="settings-nav-section-label">{t('settings.navSectionGeneral')}</p>
                <button
                  type="button"
                  className={`nav-item ${settingsTab === 'launcher' ? 'on' : ''}`}
                  onClick={() => setSettingsTab('launcher')}
                >
                  <IconHome className="settings-nav-launcher-icon" /> {t('settings.navLauncher')}
                </button>
                {modpacksList.some((m) => isModpackId(m.id)) ? (
                  <>
                    <p className="settings-nav-section-label">{t('settings.navSectionPacks')}</p>
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
                  </>
                ) : null}
              </div>
              <div className="nav-bottom">
                <p className="settings-nav-section-label">{t('settings.navSectionMore')}</p>
                <button type="button" className="nav-item" onClick={() => setCreditsModalOpen(true)}>
                  {t('settings.creditsNav')}
                </button>
                <button
                  type="button"
                  className="nav-item"
                  onClick={() => void window.solea.openExternalUrl('https://soleapixel.com/license')}
                >
                  {t('settings.licenseNav')}
                </button>
                <button type="button" className="nav-item" onClick={openReportModal}>
                  {t('home.help.report')}
                </button>
                <button type="button" className="nav-item" onClick={() => void window.solea.openUserDataFolder()}>
                  {t('settings.userData')}
                </button>
              </div>
            </nav>

            <div className="settings-body">
              <div className="settings-body-inner">
                <div className="settings-body-scroll">
                  <div className="settings-body-scroll-content">
                  <div key={settingsTab} className="settings-panel-swap">
                  <header className="settings-page-header">
                    <div className="settings-page-header-icon-wrap">
                      {settingsTab === 'launcher' ? (
                        <IconHome className="settings-header-ico-svg" aria-hidden />
                      ) : (
                        <img
                          src={
                            isModpackId(settingsTab)
                              ? MODPACK_THEME[settingsTab].sidebarIcon
                              : LOGO
                          }
                          alt=""
                          className="settings-header-ico"
                        />
                      )}
                    </div>
                    <div className="settings-page-header-text">
                      <p className="settings-page-eyebrow">
                        {t('settings.pageEyebrowSection', {
                          section:
                            settingsTab === 'launcher'
                              ? t('settings.navLauncher')
                              : isModpackId(settingsTab)
                                  ? modpacksList.find((x) => x.id === settingsTab)?.displayName ?? settingsTab
                                  : t('settings.navLauncher')
                        })}
                      </p>
                      <h2 className="settings-page-title">
                        {settingsTab === 'launcher'
                          ? t('settings.headerLauncher')
                          : isModpackId(settingsTab)
                              ? t('settings.headerGame', {
                                  name: modpacksList.find((x) => x.id === settingsTab)?.displayName ?? settingsTab
                                })
                              : t('settings.headerLauncher')}
                      </h2>
                    </div>
                  </header>

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
                      <div className="settings-after-launch-extras">
                        <SettingsToggle
                          checked={settings.openGameLogOnInstanceLaunch}
                          onChange={(v) =>
                            setSettings((s) => ({ ...s, openGameLogOnInstanceLaunch: v }))
                          }
                          label={
                            <span className="settings-label-help-row">
                              {t('settings.openLogConsoleOnLaunch')}
                              <SettingsGlossaryTrigger
                                gkey="openLogConsole"
                                openKey={settingsGlossaryKey}
                                setOpenKey={setSettingsGlossaryKey}
                                t={t}
                                discordUrl={DISCORD_INVITE_URL}
                              />
                            </span>
                          }
                          description={t('settings.openLogConsoleOnLaunchSub')}
                        />
                      </div>
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
                    </summary>
                    <div className="inner settings-network-panel">
                      <p className="settings-network-intro">{t('settings.networkIntro')}</p>
                      <p className="settings-network-section-title">{t('settings.networkDownloadsSection')}</p>
                      <div className="field-grid settings-network-grid">
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
                            max={48}
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
                      </div>
                      <div className="settings-network-rule" role="presentation" />
                      <p className="settings-network-section-title">{t('settings.networkMicrosoftSection')}</p>
                      <p className="settings-network-hint">{t('settings.azureSectionHint')}</p>
                      <label className="full settings-network-azure-label">
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
                          placeholder={t('settings.azurePlaceholder')}
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
                      </div>
                      <p className="settings-roadmap-hint">{t('settings.networkRoadmapHint')}</p>
                    </div>
                  </details>

                  <details className="set-card">
                    <summary>
                      <div>
                        {t('settings.cacheMaintenance')}
                        <div className="sub">{t('settings.cacheMaintenanceSub')}</div>
                      </div>
                    </summary>
                    <div className="inner cache-maintenance-panel">
                      {cacheStats ? (
                        <>
                          <p className="cache-line">
                            <strong>{t('settings.cacheLauncherDisk')}</strong>{' '}
                            {formatBytes(cacheStats.launcherCachesBytes)}
                          </p>
                          <p className="cache-line">
                            <strong>{t('settings.cacheLauncherLogs')}</strong>{' '}
                            {formatBytes(cacheStats.launcherLogsBytes)}
                          </p>
                          <p className="cache-line cache-line--sub">
                            <strong>{t('settings.clearMemoryData')}</strong>
                            <span className="cache-memory-hint"> {t('settings.clearMemoryDataSub')}</span>
                          </p>
                          <div className="cache-actions">
                            <button
                              type="button"
                              className="btn-danger-outline"
                              onClick={() => setCacheClearConfirm('launcher')}
                            >
                              {t('settings.cacheClearLauncher')}
                            </button>
                            <button
                              type="button"
                              className="btn-danger-outline"
                              onClick={() => setCacheClearConfirm('logs')}
                            >
                              {t('settings.cacheClearLogs')}
                            </button>
                            <button
                              type="button"
                              className="btn-danger-outline"
                              onClick={() => setMemoryClearOpen(true)}
                            >
                              {t('settings.clearMemoryData')}
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
                        <div className="sub">{t('settings.themeSub')}</div>
                        <LauncherSelect
                          value={settings.uiTheme}
                          onChange={(v) => setSettings((s) => ({ ...s, uiTheme: v as UiTheme }))}
                          options={themeSelectEntries(t)}
                        />
                        <p className="settings-theme-detail">
                          {t(`settings.themeDetail.${settings.uiTheme}`)}
                        </p>
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
                      </div>
                      <div className="full settings-toggle-stack">
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
                    </summary>
                    <div className="inner settings-shortcuts-panel">
                      <p className="settings-shortcut-hint">{t('settings.shortcutHint')}</p>
                      {shortcutCapture ? (
                        <p className="settings-shortcut-listening" role="status" aria-live="polite">
                          {t('settings.shortcutListening')}
                        </p>
                      ) : null}
                      <dl className="settings-shortcuts-dl">
                        <div
                          className={`settings-shortcut-row${
                            shortcutCapture === 'open' ? ' settings-shortcut-row--editing' : ''
                          }`}
                        >
                          <dt>{t('settings.shortcutOpenSettings')}</dt>
                          <dd>
                            <kbd className="settings-shortcut-kbd" title={settings.uiShortcutOpenSettings}>
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
                        <div
                          className={`settings-shortcut-row${
                            shortcutCapture === 'news' ? ' settings-shortcut-row--editing' : ''
                          }`}
                        >
                          <dt>{t('settings.shortcutGoNews')}</dt>
                          <dd>
                            <kbd className="settings-shortcut-kbd" title={settings.uiShortcutGoNews}>
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
                        <div
                          className={`settings-shortcut-row${
                            shortcutCapture === 'account' ? ' settings-shortcut-row--editing' : ''
                          }`}
                        >
                          <dt>{t('settings.shortcutGoAccount')}</dt>
                          <dd>
                            {activeAcc?.offline ? (
                              <p className="settings-shortcut-offline-hint">{t('settings.shortcutGoAccountOffline')}</p>
                            ) : (
                              <>
                                <kbd className="settings-shortcut-kbd" title={settings.uiShortcutGoAccount}>
                                  {formatAcceleratorForDisplay(settings.uiShortcutGoAccount)}
                                </kbd>
                                <button
                                  type="button"
                                  className="btn-muted settings-shortcut-change"
                                  onClick={() => setShortcutCapture('account')}
                                >
                                  {t('settings.shortcutChange')}
                                </button>
                              </>
                            )}
                          </dd>
                        </div>
                      </dl>
                    </div>
                  </details>

                  <details className="set-card" open>
                    <summary>
                      <div>
                        {t('settings.navSectionExperimental')}
                        <div className="sub">{t('settings.experimentalIntro')}</div>
                      </div>
                    </summary>
                    <div className="inner field-grid">
                      <div className="full settings-toggle-stack">
                        <SettingsToggle
                          checked={settings.experimentalServerSystemEnabled}
                          onChange={(next) =>
                            setSettings((s) => ({ ...s, experimentalServerSystemEnabled: next }))
                          }
                          label={t('settings.experimentalServerToggle')}
                          description={t('settings.experimentalServerToggleSub')}
                        />
                      </div>
                      <label className="full">
                        {t('settings.uiLauncherExperience')}
                        <div className="sub">{t('settings.uiLauncherExperienceSub')}</div>
                        <LauncherSelect
                          value={settings.uiHomeCardVariant === 'classic' ? 'classic' : 'studio'}
                          onChange={(v) => {
                            const legacy = v === 'classic'
                            setSettings((s) => ({
                              ...s,
                              uiHomeCardVariant: legacy ? 'classic' : 'studio',
                              uiSettingsShell: legacy ? 'legacy' : 'aether2'
                            }))
                          }}
                          options={[
                            { value: 'studio', label: t('settings.uiHomeCardStudio') },
                            { value: 'classic', label: t('settings.uiHomeCardClassic') }
                          ]}
                        />
                      </label>
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

                  <details className="set-card set-card--resolution" open>
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
                    </summary>
                    <div className="inner modpack-resolution-panel">
                      <p className="modpack-resolution-lead">{t('settings.resolutionGameLead')}</p>
                      <div className="resolution-preset-block">
                        <span className="resolution-preset-label">{t('settings.resolutionPresetsLabel')}</span>
                        <div className="resolution-preset-buttons">
                          <button
                            type="button"
                            className="btn-muted resolution-preset-btn"
                            onClick={() =>
                              patchPackProfile(modpackSettingsReadyId, {
                                screenWidth: 800,
                                screenHeight: 600
                              })
                            }
                          >
                            {t('settings.resolutionPreset800')}
                          </button>
                          <button
                            type="button"
                            className="btn-muted resolution-preset-btn"
                            onClick={() =>
                              patchPackProfile(modpackSettingsReadyId, {
                                screenWidth: 1280,
                                screenHeight: 720
                              })
                            }
                          >
                            {t('settings.resolutionPreset720')}
                          </button>
                          <button
                            type="button"
                            className="btn-muted resolution-preset-btn"
                            onClick={() =>
                              patchPackProfile(modpackSettingsReadyId, {
                                screenWidth: 1920,
                                screenHeight: 1080
                              })
                            }
                          >
                            {t('settings.resolutionPreset1080')}
                          </button>
                        </div>
                      </div>
                      <div className="field-grid modpack-resolution-inputs">
                        <label>
                          {t('settings.width')}
                          <input
                            type="number"
                            value={settings.modpackProfiles[modpackSettingsReadyId]?.screenWidth ?? ''}
                            onChange={(e) =>
                              setPackNum(modpackSettingsReadyId, 'screenWidth', e.target.value, true)
                            }
                            placeholder="800"
                            min={640}
                            max={7680}
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
                            placeholder="600"
                            min={480}
                            max={4320}
                          />
                        </label>
                        <div className="full settings-toggle-stack modpack-resolution-fullscreen">
                          <SettingsToggle
                            checked={settings.modpackProfiles[modpackSettingsReadyId]?.fullscreen ?? false}
                            onChange={(next) =>
                              patchPackProfile(modpackSettingsReadyId, { fullscreen: next })
                            }
                            label={t('settings.fullscreen')}
                          />
                        </div>
                      </div>
                    </div>
                  </details>

                  <details className="set-card">
                    <summary>
                      <div>
                        {t('settings.gameArgs')}
                        <div className="sub">{t('settings.gameArgsSub')}</div>
                      </div>
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

                  <details className="set-card set-card--modpack-install" open>
                    <summary>
                      <div>
                        {t('settings.install')}
                        <div className="sub">{t('settings.installSub')}</div>
                      </div>
                    </summary>
                    <div className="inner modpack-maint-panel modpack-install-section">
                      <div className="modpack-install-stat">
                        <span className="modpack-install-stat-label">{t('settings.packStorage')}</span>
                        <span className="modpack-install-stat-value">
                          {packInstanceDetails === null
                            ? t('settings.packStorageLoading')
                            : packInstanceDetails.installed && packInstanceDetails.sizeBytes != null
                              ? formatBytes(packInstanceDetails.sizeBytes)
                              : t('settings.packNotInstalled')}
                        </span>
                      </div>

                      <div className="modpack-install-location-card">
                        <div className="modpack-install-location-header">
                          <span className="modpack-install-location-title">{t('settings.instancePath')}</span>
                          <div className="modpack-install-location-badges">
                            {instancePathConfirm && installLocationUi.showPendingStrip ? (
                              <span className="modpack-install-location-badge modpack-install-location-badge--preview">
                                {t('settings.instancePathPreviewBadge')}
                              </span>
                            ) : null}
                            {instancePathBusy && instancePathUiPreview ? (
                              <span className="modpack-install-location-badge modpack-install-location-badge--preview">
                                {t('settings.instancePathApplying')}
                              </span>
                            ) : null}
                            {!(instancePathBusy && instancePathUiPreview) ? (
                              installLocationUi.showCustomBadge ? (
                                <span className="modpack-install-location-badge modpack-install-location-badge--custom">
                                  {t('settings.instancePathBadgeCustom')}
                                </span>
                              ) : (
                                <span className="modpack-install-location-badge modpack-install-location-badge--default">
                                  {t('settings.instancePathBadgeDefault')}
                                </span>
                              )
                            ) : null}
                          </div>
                        </div>
                        {installLocationUi.showPendingStrip && instancePathConfirm ? (
                          <p className="modpack-install-pending-hint" role="status">
                            {t('settings.instancePathPendingHint')}
                          </p>
                        ) : null}
                        <div className="modpack-install-location-path-wrap">
                          <div className="modpack-install-location-path-label">
                            {t('settings.instancePathFullResolved')}
                          </div>
                          <div
                            className="modpack-install-location-path-box"
                            title={installLocationUi.displayedRoot || undefined}
                          >
                            {installLocationUi.showLoadingPath ? (
                              <span className="modpack-install-location-loading">
                                {t('settings.packStorageLoading')}
                              </span>
                            ) : (
                              <code>{installLocationUi.displayedRoot}</code>
                            )}
                          </div>
                          {!installLocationUi.showLoadingPath && installLocationUi.displayedRoot ? (
                            <button
                              type="button"
                              className="btn-quiet modpack-install-path-copy"
                              onClick={() => {
                                const p = installLocationUi.displayedRoot
                                if (!navigator.clipboard?.writeText) {
                                  pushToast(t('settings.instancePathCopyFailed'), 'error')
                                  return
                                }
                                void navigator.clipboard.writeText(p).then(
                                  () => pushToast(t('settings.instancePathCopied'), 'success'),
                                  () => pushToast(t('settings.instancePathCopyFailed'), 'error')
                                )
                              }}
                            >
                              {t('settings.instancePathCopy')}
                            </button>
                          ) : null}
                        </div>
                        <p className="modpack-maint-hint modpack-install-location-hint">
                          {t('settings.instancePathHint')}
                        </p>
                        <div className="modpack-install-location-actions">
                          <button
                            type="button"
                            className="btn-muted"
                            disabled={
                              packMaintBusy ||
                              instancePathBusy ||
                              backgroundInstallOrUninstall ||
                              launchPhase !== 'idle'
                            }
                            onClick={() => void onPickInstanceInstallFolder()}
                          >
                            {t('settings.instancePathChooseFolder')}
                          </button>
                          <button
                            type="button"
                            className="btn-muted"
                            disabled={
                              packMaintBusy || instancePathBusy || installLocationUi.resetDisabled
                            }
                            onClick={() => void onResetInstanceInstallFolder()}
                          >
                            {t('settings.instancePathResetDefault')}
                          </button>
                        </div>
                      </div>

                      {packInstanceDetails && !packInstanceDetails.installed ? (
                        <p className="modpack-maint-hint modpack-install-secondary-lead">
                          {t('settings.packMaintDisabledHint')}
                        </p>
                      ) : null}

                      <div className="modpack-install-secondary">
                        <p className="modpack-install-secondary-title">{t('settings.installAfterTitle')}</p>
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
                      <div className="settings-verify-files-row">
                        <button
                          type="button"
                          className="btn-muted"
                          disabled={
                            !packInstanceDetails?.installed ||
                            packMaintBusy ||
                            backgroundInstallOrUninstall ||
                            launchPhase !== 'idle'
                          }
                          title={
                            !packInstanceDetails?.installed
                              ? t('settings.packMaintDisabledHintShort')
                              : t('settings.verifyFilesTooltip')
                          }
                          onClick={() => void onSettingsVerify()}
                        >
                          {t('settings.verifyFilesBtn')}
                        </button>
                      </div>
                      {settingsVerifyResult ? (
                        <div
                          className={`verify-banner settings-verify-banner ${
                            settingsVerifyResult.ok ? 'ok' : 'fail'
                          } verify-banner--with-actions`}
                          role="status"
                          aria-live="polite"
                        >
                          <div className="verify-banner-body">
                            {settingsVerifyResult.ok ? (
                              <>{t('home.verifyOk')}</>
                            ) : settingsVerifyResult.reason === 'extra_mod' ? (
                              <>
                                <strong>{t('home.verifyExtraTitle')}</strong> — {t('home.verifyExtraBody')}
                                <ul>
                                  {(settingsVerifyResult.paths ?? []).map((path) => (
                                    <li key={path}>{path}</li>
                                  ))}
                                </ul>
                              </>
                            ) : settingsVerifyResult.reason === 'hash_mismatch' ? (
                              <>
                                {t('home.verifyHash', { detail: settingsVerifyResult.detail ?? '—' })}
                              </>
                            ) : settingsVerifyResult.reason === 'missing_file' ? (
                              <>
                                {t('home.verifyMissing', { detail: settingsVerifyResult.detail ?? '' })}
                              </>
                            ) : settingsVerifyResult.reason === 'no_lock' ? (
                              <>{t('home.verifyNoLock')}</>
                            ) : settingsVerifyResult.reason === 'read_error' ? (
                              <>
                                {t('home.verifyRead', {
                                  detail: settingsVerifyResult.detail ?? settingsVerifyResult.reason
                                })}
                              </>
                            ) : (
                              <>{settingsVerifyResult.detail ?? settingsVerifyResult.reason}</>
                            )}
                          </div>
                          <div className="verify-banner-actions">
                            {!settingsVerifyResult.ok ? (
                              <>
                                <button
                                  type="button"
                                  className="btn-quiet verify-banner-btn"
                                  disabled={phase !== 'idle' || launchPhase !== 'idle'}
                                  onClick={() => void onSettingsVerify()}
                                >
                                  {t('home.verify.retry')}
                                </button>
                                <button
                                  type="button"
                                  className="btn-quiet verify-banner-btn"
                                  disabled={phase !== 'idle' || launchPhase !== 'idle' || packMaintBusy}
                                  onClick={onSettingsVerifyRepair}
                                >
                                  {t('home.verify.repair')}
                                </button>
                              </>
                            ) : null}
                            <button
                              type="button"
                              className="btn-quiet verify-banner-btn"
                              onClick={() => setSettingsVerifyResult(null)}
                            >
                              {t('home.verify.dismiss')}
                            </button>
                          </div>
                        </div>
                      ) : null}
                        <div className="modpack-maint-actions modpack-install-maint-actions">
                          <button
                            type="button"
                            className="btn-muted"
                            disabled={
                              !packInstanceDetails?.installed ||
                              packMaintBusy ||
                              backgroundInstallOrUninstall ||
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
                              backgroundInstallOrUninstall ||
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
                    </div>
                  </details>
                </>
              ) : null}
                  </div>

                  {settingsFb ? (
                    <div className={`settings-feedback feedback ${settingsFb.ok ? 'ok' : 'err'}`}>
                      {settingsFb.text}
                    </div>
                  ) : null}
                  </div>
                </div>
                <div className="settings-sticky-footer">
                  <div className="settings-footer-inner">
                  <div className="actions-bar settings-footer-actions">
                    <button type="button" className="btn-save" onClick={() => void saveAllSettings()}>
                      {t('settings.save')}
                    </button>
                    <button
                      type="button"
                      className="btn-muted"
                      disabled={settingsResetTabDisabled}
                      title={
                        settingsResetTabDisabled ? t('settings.resetTabDisabledHint') : undefined
                      }
                      onClick={openSettingsResetTabConfirm}
                    >
                      {t('settings.reset')}
                    </button>
                    <button
                      type="button"
                      className="btn-muted"
                      onClick={() => setSettingsResetConfirm('all')}
                    >
                      {t('settings.resetAll')}
                    </button>
                    <button
                      type="button"
                      className="btn-muted"
                      onClick={() => tryLeaveSettings(() => setView('news'))}
                    >
                      {t('settings.back')}
                    </button>
                  </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {view === 'account' && (
          <div className="account-layout">
            <div className="account-scroll">
              <div className="account-panel">
              <header className="account-header">
                <div className="account-header-icon-wrap" aria-hidden>
                  <IconUser />
                </div>
                <div className="account-header-text">
                  <p className="account-page-eyebrow">{t('account.pageEyebrow')}</p>
                  <h2 className="account-title">{t('account.title')}</h2>
                  <p className="account-lead">{t('account.lead')}</p>
                </div>
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
                            } else {
                              setAccountFb(r.error)
                              pushToast(
                                t('account.toastMicrosoftSessionFailed', { detail: r.error }),
                                'error',
                                10000
                              )
                            }
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
                      </summary>
                      <div className="inner account-account-list">
                        {accounts.map((a) => (
                          <div key={a.uuid} className="account-row">
                            <span className="account-row-name">{a.name}</span>
                            <button
                              type="button"
                              className="btn-muted account-activate-btn"
                              disabled={a.uuid === activeAcc.uuid}
                              onClick={() => void switchActiveAccount(a.uuid, { refreshSkinKey: true })}
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
                    <button
                      type="button"
                      className="btn-muted"
                      onClick={() => tryLeaveSettings(() => setView('news'))}
                    >
                      {t('account.back')}
                    </button>
                  </div>
                </>
              ) : (
                <p className="account-empty">{t('account.empty')}</p>
              )}
              </div>
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
        <SettingsUnsavedModal
          open={settingsUnsavedOpen}
          sections={settingsUnsavedSections}
          busy={settingsUnsavedBusy}
          onSave={handleUnsavedSave}
          onDiscard={handleUnsavedDiscard}
        />
        <CreditsModal open={creditsModalOpen} onClose={() => setCreditsModalOpen(false)} />
        {addAccountModalOpen ? (
          <div
            className="pack-confirm-backdrop add-account-modal-backdrop"
            role="presentation"
            onClick={() => {
              if (!addAccountBusy) closeAddAccountModal()
            }}
          >
            <div
              ref={addAccountModalRef}
              className="add-account-aether-modal"
              role="dialog"
              aria-modal="true"
              aria-labelledby="add-account-modal-title"
              onClick={(e) => e.stopPropagation()}
            >
              <button
                type="button"
                className="add-account-aether-dismiss"
                aria-label={t('account.addModalClose')}
                disabled={addAccountBusy}
                onClick={() => closeAddAccountModal()}
              >
                ×
              </button>
              <p className="add-account-aether-eyebrow">{t('account.addModalEyebrow')}</p>
              <h2 id="add-account-modal-title" className="add-account-aether-title">
                {t('account.addModalTitle')}
              </h2>
              <p className="add-account-aether-lead">{t('account.addModalLead')}</p>

              {addAccountStep === 'pick' ? (
                <div className="add-account-aether-pick">
                  <div className="add-account-aether-choice">
                    <button
                      type="button"
                      className="add-account-aether-btn-ms"
                      disabled={addAccountBusy}
                      onClick={() => void handleAddAccountMicrosoft()}
                    >
                      <span className="add-account-aether-btn-label">
                        {addAccountBusy ? t('login.connectBusy') : t('login.connect')}
                      </span>
                      {!addAccountBusy ? (
                        <span className="add-account-aether-btn-sub">{t('account.addModalMicrosoftHint')}</span>
                      ) : null}
                    </button>
                  </div>
                  <div className="add-account-aether-or" aria-hidden>
                    <span className="add-account-aether-or-line" />
                    <span className="add-account-aether-or-text">{t('account.addModalOr')}</span>
                    <span className="add-account-aether-or-line" />
                  </div>
                  <div className="add-account-aether-choice">
                    <button
                      type="button"
                      className="add-account-aether-btn-offline"
                      disabled={addAccountBusy}
                      onClick={() => {
                        setAddAccountStep('offline')
                        setAddAccountErr(null)
                      }}
                    >
                      <span className="add-account-aether-btn-label">{t('login.playOffline')}</span>
                      <span className="add-account-aether-btn-sub">{t('account.addModalOfflineHint')}</span>
                    </button>
                  </div>
                </div>
              ) : (
                <div className="add-account-aether-offline">
                  <button
                    type="button"
                    className="add-account-aether-back"
                    disabled={addAccountBusy}
                    onClick={() => {
                      setAddAccountStep('pick')
                      setAddAccountErr(null)
                    }}
                  >
                    {t('account.addModalBack')}
                  </button>
                  <p className="add-account-aether-offline-hint">{t('login.offlineModalLead')}</p>
                  <label className="add-account-aether-field" htmlFor="add-account-offline-input">
                    <span className="add-account-aether-field-label">{t('login.offlinePlaceholder')}</span>
                    <input
                      id="add-account-offline-input"
                      type="text"
                      className="add-account-aether-input"
                      value={addAccountOfflineName}
                      onChange={(e) => setAddAccountOfflineName(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') void handleAddAccountOfflineSubmit()
                      }}
                      placeholder={t('login.offlinePlaceholder')}
                      maxLength={16}
                      autoComplete="username"
                      autoFocus
                    />
                  </label>
                  {addAccountErr ? <p className="login-error add-account-aether-err">{addAccountErr}</p> : null}
                  <div className="add-account-aether-actions">
                    <button
                      type="button"
                      className="btn-muted add-account-aether-sec"
                      disabled={addAccountBusy}
                      onClick={() => {
                        setAddAccountStep('pick')
                        setAddAccountErr(null)
                      }}
                    >
                      {t('login.offlineCancel')}
                    </button>
                    <button
                      type="button"
                      className="add-account-aether-btn-ms add-account-aether-btn-ms--compact"
                      disabled={addAccountBusy}
                      onClick={() => void handleAddAccountOfflineSubmit()}
                    >
                      {addAccountBusy ? t('login.offlineBusy') : t('login.offlineValidate')}
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        ) : null}
        <ModpackUpdatesModal
          open={showModpackUpdatesModal}
          packs={allModpacksAction ?? []}
          onClose={() => setShowModpackUpdatesModal(false)}
        />
        {instancePathConfirm ? (
          <InstancePathChangeModal
            open
            busy={instancePathBusy}
            newRootPreview={instancePathConfirm.newRootPreview}
            onCancel={() => {
              if (!instancePathBusy) {
                setInstancePathConfirm(null)
                setInstancePathUiPreview(null)
              }
            }}
            onMove={() => void runInstancePathApply('move')}
            onReinstall={() => void runInstancePathApply('reinstall')}
          />
        ) : null}
        {packMaintConfirm?.kind === 'reinstall' ? (
          <PackMaintConfirmModal
            open
            variant="reinstall"
            onCancel={() => setPackMaintConfirm(null)}
            onConfirm={(preserve) => {
              const id = packMaintConfirm.packId
              setPackMaintConfirm(null)
              void executeReinstallModpack(id, preserve)
            }}
          />
        ) : null}
        {packMaintConfirm?.kind === 'uninstall' ? (
          <PackMaintConfirmModal
            open
            variant="uninstall"
            onCancel={() => setPackMaintConfirm(null)}
            onConfirm={() => {
              const id = packMaintConfirm.packId
              setPackMaintConfirm(null)
              void executeUninstallModpack(id)
            }}
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
        {memoryClearOpen ? (
          <MemoryClearConfirmModal
            open
            onConfirm={onMemoryClearConfirmResolved}
            onCancel={() => setMemoryClearOpen(false)}
          />
        ) : null}
        {settingsResetConfirm ? (
          <div
            className="pack-confirm-backdrop"
            role="presentation"
            onClick={() => setSettingsResetConfirm(null)}
          >
            <div
              ref={settingsResetModalRef}
              className="pack-confirm-modal pack-confirm-modal--cache solea-modal-surface"
              role="alertdialog"
              aria-modal="true"
              aria-labelledby="settings-reset-confirm-title"
              aria-describedby="settings-reset-confirm-desc"
              onClick={(e) => e.stopPropagation()}
            >
              <p className="pack-confirm-eyebrow">{t('settings.resetConfirmEyebrow')}</p>
              <h2 id="settings-reset-confirm-title" className="pack-confirm-title">
                {t('confirm.packSureQuestion')}
              </h2>
              <p id="settings-reset-confirm-desc" className="pack-confirm-body">
                {settingsResetConfirm === 'all'
                  ? t('settings.resetAllBody')
                  : settingsResetConfirm === 'launcher-tab'
                    ? t('settings.resetTabBodyLauncher')
                    : t('settings.resetTabBodyModpack', {
                        name:
                          modpacksList.find((x) => x.id === settingsResetConfirm)?.displayName ??
                          settingsResetConfirm
                      })}
              </p>
              <div className="pack-confirm-actions">
                <button
                  type="button"
                  className="btn-muted pack-confirm-btn-cancel"
                  onClick={() => setSettingsResetConfirm(null)}
                >
                  {t('confirm.packCancel')}
                </button>
                <button
                  type="button"
                  className="btn-save pack-confirm-btn-primary"
                  onClick={() => void handleConfirmSettingsReset()}
                >
                  {settingsResetConfirm === 'all'
                    ? t('settings.resetAllConfirm')
                    : t('settings.resetTabConfirm')}
                </button>
              </div>
            </div>
          </div>
        ) : null}
        {backgroundInstallOrUninstall ? (
          <div className="solea-global-progress" aria-live="polite">
            <div className="solea-global-progress-inner">
              <p className="solea-global-progress-title font-mc">
                {phase === 'uninstalling' ? t('globalProgress.uninstalling') : t('globalProgress.installing')}
              </p>
              <div
                className={`solea-global-progress-track${
                  installIndeterminate ? ' solea-global-progress-track--indeterminate' : ''
                }`}
                role="progressbar"
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={installIndeterminate ? undefined : installPct}
                aria-label={t('install.progressAria')}
                aria-valuetext={
                  installIndeterminate
                    ? installLine
                    : `${installLine} — ${formatPercent(installPct)}`
                }
              >
                <div
                  className="solea-global-progress-fill"
                  style={
                    installIndeterminate
                      ? undefined
                      : { width: `${Math.max(0, Math.min(100, installPct))}%` }
                  }
                />
              </div>
              <div className="solea-global-progress-meta">
                <span className="solea-global-progress-label">{installLine}</span>
                {!installIndeterminate ? (
                  <span className="solea-global-progress-pct" aria-hidden>
                    {formatPercent(installPct)}
                  </span>
                ) : null}
              </div>
            </div>
          </div>
        ) : null}
        {myServerInstallProgress ? (
          <div className="solea-myserver-progress" role="status" aria-live="polite">
            <div className="solea-myserver-progress-inner solea-modal-surface">
              <p
                className={`solea-myserver-progress-title${settings.uiChromeGlass ? ' font-mc' : ''}`}
              >
                {t('myServer.installBarTitle')}
              </p>
              <div
                className="solea-global-progress-track"
                role="progressbar"
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={Math.round(
                  (myServerInstallProgress.current / myServerInstallProgress.total) * 100
                )}
              >
                <div
                  className="solea-global-progress-fill"
                  style={{
                    width: `${Math.max(
                      0,
                      Math.min(100, (myServerInstallProgress.current / myServerInstallProgress.total) * 100)
                    )}%`
                  }}
                />
              </div>
              <p className="solea-myserver-progress-detail">
                {myServerInstallProgress.phase}
                {myServerInstallProgress.detail ? ` — ${myServerInstallProgress.detail}` : ''}
              </p>
            </div>
          </div>
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
