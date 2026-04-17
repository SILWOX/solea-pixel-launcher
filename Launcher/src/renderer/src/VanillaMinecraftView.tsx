/** Minecraft vanilla — hub inspiré LabyMod (2 colonnes + onglet sélection de version). */
import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties } from 'react'
import type { LauncherSettingsUI } from './launcherTypes'
import { useI18n, type TFunction } from './i18n/I18nContext'
import {
  consumeWarmupPending,
  peekWarmupPending,
  setWarmupPending,
  vanillaWarmupSessionKey
} from './launchWarmupSession'
import { useToast } from './ui/ToastContext'
import hubHeroBgUrl from './assets/branding/home-minecraft-wallpaper.png?url'
import vanillaGrassIconUrl from './assets/branding/vanilla-grass-icon.png?url'
import './vanilla-minecraft.css'
import {
  MOJANG_LAUNCHER_NEWS_URL,
  MOJANG_LAUNCHER_NEWS_URL_LEGACY,
  allLauncherNewsSorted,
  formatNewsDateForUi,
  newsImageUrl,
  parseLauncherNewsJson,
  type McLauncherNewsEntry
} from './minecraftLauncherNews'
import {
  defaultShaderStackForReleaseId,
  vanillaInstallFolderName
} from '../../vanillaFolderNames'

const VERSION_MANIFEST_URL = 'https://launchermeta.mojang.com/mc/game/version_manifest_v2.json'

/**
 * Illustrations `assets/vanilla-major/major-*.png` pour les tuiles (1.8…1.21, etc.).
 * Nouvelle ligne majeure : ajouter `major-X.Y.png` + i18n `vanillaHub.versionPitch.X_Y` seulement quand
 * Fabric, Iris et Sodium ont chacun une build Modrinth pour cette version MC (voir JSDoc `releaseLineAtLeast18`).
 */
const majorTileImageMap = (() => {
  const raw = import.meta.glob('./assets/vanilla-major/major-*.png', {
    eager: true,
    query: '?url',
    import: 'default'
  }) as Record<string, string>
  const map = new Map<string, string>()
  for (const [p, url] of Object.entries(raw)) {
    const norm = p.replace(/\\/g, '/')
    const m = /major-([\d.]+)\.png$/i.exec(norm)
    if (m?.[1]) map.set(m[1], url)
  }
  return map
})()

function majorTileArtUrl(major: string): string | undefined {
  return majorTileImageMap.get(major)
}

type McManifest = {
  latest: { release: string; snapshot: string }
  versions: { id: string; type: string; time: string; releaseTime: string }[]
}

type VanillaTab = 'hub' | 'versions'

export type VanillaMinecraftViewProps = {
  /** Pendant install / lancement vanilla : permet au shell de bloquer un changement d’instance accidentel. */
  onVanillaBusyChange?: (busy: boolean) => void
}

export function VanillaMinecraftView({ onVanillaBusyChange }: VanillaMinecraftViewProps = {}) {
  const { t } = useI18n()
  const { pushToast } = useToast()
  const [tab, setTab] = useState<VanillaTab>('hub')
  const [manifest, setManifest] = useState<McManifest | null>(null)
  const [manifestErr, setManifestErr] = useState<string | null>(null)
  const [selectedVersion, setSelectedVersion] = useState('')
  const [selectedMajor, setSelectedMajor] = useState('')
  const [shaderStack, setShaderStack] = useState<'optifine' | 'iris'>('iris')
  const [launchBusy, setLaunchBusy] = useState(false)
  /** Après un lancement réussi, l’IPC revient tout de suite : on garde le libellé « Lancement » animé un court instant (comme l’accueil modpack qui reste en phase launching). */
  const [vanillaLaunchAnimating, setVanillaLaunchAnimating] = useState(false)
  const vanillaLaunchAnimTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  /** Points animés sur le CTA (même rythme que le bouton Play accueil modpack). */
  const [busyDots, setBusyDots] = useState(1)
  /** Garde synchrone : évite deux handlers avant que `setLaunchBusy(true)` soit flush (clics rapides). */
  const launchBusyRef = useRef(false)
  /** Dossiers présents sous `game/versions/<id>/` (client déjà téléchargé). */
  const [installedClientVersions, setInstalledClientVersions] = useState<string[]>([])
  const [skinHeadUrl, setSkinHeadUrl] = useState<string | null>(null)
  const [settingsUi, setSettingsUi] = useState<LauncherSettingsUI | null>(null)
  const [newsItems, setNewsItems] = useState<McLauncherNewsEntry[]>([])
  const [newsErr, setNewsErr] = useState<string | null>(null)
  const [newsLoading, setNewsLoading] = useState(false)
  /** Tout Minecraft détecté par le launcher (vanilla .minecraft ou instance modpack) — même politique « un jeu » que l’accueil. */
  const [soleaMcRunning, setSoleaMcRunning] = useState(false)

  const majorToLatest = useMemo(() => buildMajorToLatestPatch(manifest?.versions ?? []), [manifest])
  const majorKeysSorted = useMemo(() => sortMajorKeysDesc([...majorToLatest.keys()]), [majorToLatest])

  const vanillaProfileFolder = useMemo(
    () =>
      selectedVersion
        ? vanillaInstallFolderName(selectedVersion, defaultShaderStackForReleaseId(selectedVersion))
        : '',
    [selectedVersion]
  )

  const patchList = useMemo(() => {
    if (!manifest?.versions?.length || !selectedMajor) return []
    return listPatchesForMajor(selectedMajor, manifest.versions)
  }, [manifest, selectedMajor])

  useEffect(() => {
    let cancelled = false
    void (async () => {
      try {
        const r = await window.solea.fetchActuText(VERSION_MANIFEST_URL)
        if (!r.ok) {
          if (!cancelled) setManifestErr(r.error)
          return
        }
        const parsed = JSON.parse(r.text) as McManifest
        if (!cancelled) {
          setManifest(parsed)
          setManifestErr(null)
        }
      } catch (e) {
        if (!cancelled) setManifestErr(e instanceof Error ? e.message : String(e))
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    void (async () => {
      const acc = await window.solea.getActiveAccount()
      if (cancelled) return
      if (!acc) {
        setSkinHeadUrl(null)
        return
      }
      const head = await window.solea.getSkinHead(acc.uuid, 144)
      if (!cancelled) setSkinHeadUrl(head)
    })()
    return () => {
      cancelled = true
    }
  }, [])

  const refreshSettings = useCallback(async () => {
    const st = await window.solea.getSettings()
    setSettingsUi(st as LauncherSettingsUI)
  }, [])

  useEffect(() => {
    void refreshSettings()
  }, [refreshSettings])

  useEffect(() => {
    onVanillaBusyChange?.(launchBusy)
    return () => {
      onVanillaBusyChange?.(false)
    }
  }, [launchBusy, onVanillaBusyChange])

  useEffect(() => {
    if (tab !== 'hub') return
    void refreshSettings()
  }, [tab, refreshSettings])

  const loadMinecraftNews = useCallback(async () => {
    setNewsLoading(true)
    setNewsErr(null)
    const cacheBust = (base: string) => `${base}${base.includes('?') ? '&' : '?'}_=${Date.now()}`
    try {
      let lastErr: string | null = null
      for (const baseUrl of [MOJANG_LAUNCHER_NEWS_URL, MOJANG_LAUNCHER_NEWS_URL_LEGACY]) {
        const r = await window.solea.fetchActuText(cacheBust(baseUrl))
        if (!r.ok) {
          lastErr = r.error
          continue
        }
        const all = parseLauncherNewsJson(r.text)
        const items = allLauncherNewsSorted(all).slice(0, 20)
        if (items.length > 0) {
          setNewsItems(items)
          setNewsErr(null)
          return
        }
      }
      setNewsItems([])
      setNewsErr(lastErr)
    } catch (e) {
      setNewsErr(e instanceof Error ? e.message : String(e))
      setNewsItems([])
    } finally {
      setNewsLoading(false)
    }
  }, [])

  useEffect(() => {
    if (tab !== 'hub') return
    void loadMinecraftNews()
  }, [tab, loadMinecraftNews])

  const refreshSoleaMcRunning = useCallback(() => {
    void window.solea.isGameRunning().then(setSoleaMcRunning)
  }, [])

  useEffect(() => {
    void refreshSoleaMcRunning()
    const id = window.setInterval(() => void refreshSoleaMcRunning(), 2500)
    return () => window.clearInterval(id)
  }, [refreshSoleaMcRunning])

  useEffect(() => {
    return window.solea.onGameExited(() => {
      setSoleaMcRunning(false)
    })
  }, [])

  const vanillaHubInitDoneRef = useRef(false)

  /** Reprise : réglages launcher puis manifest (une fois au chargement du manifest). Le client se signale par la présence du dossier Mojang sous .minecraft/versions/. */
  useEffect(() => {
    if (!manifest?.versions?.length || vanillaHubInitDoneRef.current) return
    vanillaHubInitDoneRef.current = true
    let cancelled = false
    void (async () => {
      const st = (await window.solea.getSettings()) as LauncherSettingsUI
      let v = (typeof st.vanillaHubLastSelectedVersion === 'string' && st.vanillaHubLastSelectedVersion.trim()) || ''
      const validInManifest = (id: string) =>
        Boolean(id) && releaseLineAtLeast18(id) && manifest!.versions.some((x) => x.id === id)
      if (!validInManifest(v)) {
        v = ''
      }
      if (!cancelled && v && validInManifest(v)) {
        setSelectedVersion(v)
        const maj = majorFromReleaseId(v)
        if (maj) setSelectedMajor(maj)
        return
      }
      const rel = manifest!.latest.release
      if (!cancelled && releaseLineAtLeast18(rel)) {
        setSelectedVersion(rel)
        const maj = majorFromReleaseId(rel)
        if (maj) setSelectedMajor(maj)
        void window.solea.saveSettings({ vanillaHubLastSelectedVersion: rel })
        return
      }
      const first = firstReleaseAtLeast18(manifest!.versions)
      if (!cancelled && first) {
        setSelectedVersion(first)
        const maj = majorFromReleaseId(first)
        if (maj) setSelectedMajor(maj)
        void window.solea.saveSettings({ vanillaHubLastSelectedVersion: first })
      }
    })()
    return () => {
      cancelled = true
    }
  }, [manifest])

  const refreshInstalledClientVersions = useCallback(async () => {
    if (!vanillaProfileFolder) {
      setInstalledClientVersions([])
      return
    }
    const r = await window.solea.vanillaListClientVersions(vanillaProfileFolder)
    if (r.ok) setInstalledClientVersions(r.versions)
  }, [vanillaProfileFolder])

  useEffect(() => {
    void refreshInstalledClientVersions()
  }, [refreshInstalledClientVersions])

  useEffect(() => {
    if (tab !== 'hub') return
    void refreshInstalledClientVersions()
  }, [tab, selectedVersion, refreshInstalledClientVersions])

  useEffect(() => {
    if (!selectedVersion || !selectedMajor) return
    const maj = majorFromReleaseId(selectedVersion)
    if (maj && maj !== selectedMajor) setSelectedMajor(maj)
  }, [selectedVersion, selectedMajor])

  useEffect(() => {
    if (!selectedVersion) return
    setShaderStack(defaultShaderStackForReleaseId(selectedVersion))
  }, [selectedVersion])

  const onPersistVersion = async (v: string, options?: { returnToHub?: boolean }) => {
    setSelectedVersion(v)
    const maj = majorFromReleaseId(v)
    if (maj) setSelectedMajor(maj)
    void window.solea.saveSettings({ vanillaHubLastSelectedVersion: v })
    if (options?.returnToHub) setTab('hub')
  }

  const onPickMajor = (major: string) => {
    setSelectedMajor(major)
    const latest = majorToLatest.get(major)
    if (latest) void onPersistVersion(latest)
  }

  const selectedClientInstalled =
    Boolean(selectedVersion) && installedClientVersions.includes(selectedVersion)

  const onCloseRunningGame = async () => {
    if (launchBusyRef.current || launchBusy) return
    launchBusyRef.current = true
    setLaunchBusy(true)
    try {
      const r = await window.solea.stopGame()
      if (!r.ok) {
        pushToast(r.error, 'error')
        return
      }
      await new Promise<void>((res) => setTimeout(res, 500))
      const still = await window.solea.isGameRunning()
      setSoleaMcRunning(still)
      pushToast(still ? t('vanillaMc.toastGameStopFailed') : t('vanillaMc.toastGameStopped'), still ? 'info' : 'success')
    } catch (e) {
      pushToast(e instanceof Error ? e.message : String(e), 'error')
    } finally {
      launchBusyRef.current = false
      setLaunchBusy(false)
    }
  }

  const onInstallOrLaunch = async () => {
    if (soleaMcRunning) {
      pushToast(t('vanillaMc.toastRunningUseClose'), 'info')
      return
    }
    if (!selectedVersion || !vanillaProfileFolder) {
      pushToast(t('vanillaMc.toastPickVersion'), 'error')
      return
    }
    if (launchBusyRef.current || launchBusy) {
      pushToast(t('vanillaMc.toastCtaBusy'), 'info')
      return
    }
    const dl = window.solea.vanillaDownloadClient
    const launch = window.solea.vanillaLaunch
    if (typeof dl !== 'function' || typeof launch !== 'function') {
      pushToast(t('vanillaMc.toastApiMissing'), 'error')
      return
    }
    if (!manifest?.versions?.length) {
      pushToast(manifestErr?.trim() || t('vanillaMc.toastManifestRequired'), 'error')
      return
    }
    if (!manifest.versions.some((v) => v.id === selectedVersion)) {
      pushToast(t('vanillaMc.toastVersionNotInManifest', { v: selectedVersion }), 'error')
      return
    }
    launchBusyRef.current = true
    setLaunchBusy(true)
    try {
      const listR = await window.solea.vanillaListClientVersions(vanillaProfileFolder)
      const onDisk = listR.ok ? listR.versions : []
      const uiThoughtReady = selectedClientInstalled

      if (!onDisk.includes(selectedVersion)) {
        if (uiThoughtReady) pushToast(t('vanillaMc.toastClientRepair'), 'info')
        const r = await dl({
          profileId: vanillaProfileFolder,
          version: selectedVersion,
          shaderStack
        })
        if (!r.ok) {
          pushToast(r.error, 'error')
          return
        }
        await refreshInstalledClientVersions()
        void window.solea.saveSettings({ vanillaHubLastSelectedVersion: selectedVersion })
        setWarmupPending(vanillaWarmupSessionKey(vanillaProfileFolder))
        pushToast(t('vanillaMc.toastClientInstalled'), 'success')
      } else if (!uiThoughtReady) {
        await refreshInstalledClientVersions()
      }

      const warmupKey = vanillaWarmupSessionKey(vanillaProfileFolder)
      const warmupEligible = peekWarmupPending(warmupKey)
      if (warmupEligible) {
        pushToast(t('home.firstLaunchWarmup'), 'info', 14_000)
      }
      const r = await launch({
        profileId: vanillaProfileFolder,
        version: selectedVersion,
        shaderStack,
        backupSaves: false
      })
      if (!r.ok) {
        setVanillaLaunchAnimating(false)
        pushToast(r.error, 'error')
        return
      }
      if (warmupEligible) consumeWarmupPending(warmupKey)
      void window.solea.saveSettings({ vanillaHubLastSelectedVersion: selectedVersion })
      pushToast(t('vanillaMc.toastLaunched'), 'success')
      void refreshSoleaMcRunning()
      setVanillaLaunchAnimating(true)
      if (vanillaLaunchAnimTimerRef.current != null) clearTimeout(vanillaLaunchAnimTimerRef.current)
      vanillaLaunchAnimTimerRef.current = setTimeout(() => {
        vanillaLaunchAnimTimerRef.current = null
        setVanillaLaunchAnimating(false)
      }, 2400)
    } catch (e) {
      setVanillaLaunchAnimating(false)
      pushToast(e instanceof Error ? e.message : String(e), 'error')
    } finally {
      launchBusyRef.current = false
      setLaunchBusy(false)
    }
  }

  /** Panneau versions : enregistre le patch choisi et revient à l’accueil (lancement depuis la carte). */
  const onConfirmVersionToHub = async () => {
    if (!selectedVersion) return
    void window.solea.saveSettings({ vanillaHubLastSelectedVersion: selectedVersion })
    setTab('hub')
  }

  const sideTitle = !selectedMajor
    ? t('vanillaHub.sidePick')
    : (() => {
        const tk = `vanillaMc.majorTitle.${dotToKey(selectedMajor)}`
        const s = t(tk)
        return s !== tk ? s : t('vanillaMc.majorTitle.fallback', { maj: selectedMajor })
      })()
  const sideBlurb = !selectedMajor
    ? t('vanillaHub.sideBlurbIdle')
    : (() => {
        const pitch = versionPitchForMajor(selectedMajor, t)
        if (pitch) return pitch
        const bk = `vanillaMc.majorBlurb.${dotToKey(selectedMajor)}`
        const s = t(bk)
        return s !== bk ? s : t('vanillaMc.majorBlurb.default')
      })()

  const reduceMotion = Boolean(settingsUi?.uiReduceMotion)

  /** Libellés / points animés : inclut le court feedback après lancement réussi. */
  const ctaBusy = launchBusy || vanillaLaunchAnimating
  /** Désactivation du clic : uniquement pendant IPC réel (pas pendant l’animation de libellé). */
  const ctaDisabledBusy = launchBusy

  useEffect(() => {
    return () => {
      if (vanillaLaunchAnimTimerRef.current != null) {
        clearTimeout(vanillaLaunchAnimTimerRef.current)
        vanillaLaunchAnimTimerRef.current = null
      }
    }
  }, [])

  useEffect(() => {
    const wantDots = ctaBusy && !reduceMotion
    if (!wantDots) {
      setBusyDots(1)
      return
    }
    const id = window.setInterval(() => {
      setBusyDots((d) => (d % 3) + 1)
    }, 450)
    return () => window.clearInterval(id)
  }, [ctaBusy, reduceMotion])

  const showLaunchingLabel = selectedClientInstalled && (launchBusy || vanillaLaunchAnimating)
  const showInstallingLabel = !selectedClientInstalled && launchBusy
  const showCloseGameCta = soleaMcRunning && !ctaBusy

  const hubVersionPitch = useMemo(() => {
    const maj = selectedVersion ? majorFromReleaseId(selectedVersion) : null
    return maj ? versionPitchForMajor(maj, t) : ''
  }, [selectedVersion, t])

  const playBackgroundUrl = useMemo(() => {
    if (!selectedVersion) return hubHeroBgUrl
    const maj = majorFromReleaseId(selectedVersion)
    if (!maj) return hubHeroBgUrl
    return majorTileArtUrl(maj) ?? hubHeroBgUrl
  }, [selectedVersion])

  const playCardVersionArt = playBackgroundUrl !== hubHeroBgUrl

  const sideHeroArtUrl = useMemo(
    () => (selectedMajor ? majorTileArtUrl(selectedMajor) : undefined),
    [selectedMajor]
  )

  const tileGradientClass = (major: string) => {
    const h = (major.charCodeAt(0) + (major.charCodeAt(2) ?? 0)) | 0
    return `vanilla-hub__maj-tile--tone-${h % 6}`
  }

  return (
    <div className={`vanilla-hub${reduceMotion ? ' vanilla-hub--reduce-motion' : ''}`}>
      <header className="vanilla-hub__topbar vanilla-hub__topbar--minimal vanilla-hub__topbar--branded" aria-label={t('shell.vanillaMinecraft')}>
        <div className="vanilla-hub__brand vanilla-hub__brand--compact">
          <img
            src={vanillaGrassIconUrl}
            alt=""
            className="vanilla-hub__brand-logo vanilla-hub__brand-logo--compact"
            width={30}
            height={30}
            draggable={false}
          />
          <span className="vanilla-hub__brand-headline font-mc">{t('vanillaHub.brandHeadline')}</span>
        </div>
        <div className="vanilla-hub__topbar-spacer" aria-hidden />
      </header>

      <div
        className={`vanilla-hub__body${tab === 'versions' ? ' vanilla-hub__body--versions' : ' vanilla-hub__body--hub'}`}
      >
        {manifestErr ? <p className="vanilla-hub__err">{manifestErr}</p> : null}

        {tab === 'hub' ? (
          <div className="vanilla-hub__cols">
            <section className="vanilla-hub__col vanilla-hub__col--play">
              <div
                className={`vanilla-hub__play-card${playCardVersionArt ? ' vanilla-hub__play-card--version-art' : ''}`}
                style={
                  {
                    ['--vanilla-hub-play-bg' as string]: `url(${playBackgroundUrl})`
                  } as CSSProperties
                }
              >
                <div className="vanilla-hub__play-card__ambient" aria-hidden>
                  <span className="vanilla-hub__glow vanilla-hub__glow--cyan" />
                  <span className="vanilla-hub__glow vanilla-hub__glow--green" />
                  <div className="vanilla-hub__cubes">
                    {['a', 'b', 'c', 'd', 'e', 'f'].map((k, i) => (
                      <span key={k} className="vanilla-hub__cube" style={{ ['--cube-i' as string]: String(i) }} />
                    ))}
                  </div>
                </div>
                <div className="vanilla-hub__play-card__veil" />
                <div className="vanilla-hub__play-card__inner vanilla-hub__play-card__inner--v2">
                  <div className="vanilla-hub__play-hero">
                    <div className="vanilla-hub__skin-ring vanilla-hub__skin-ring--hero">
                      {skinHeadUrl ? (
                        <img src={skinHeadUrl} alt="" className="vanilla-hub__skin-img" width={112} height={112} />
                      ) : (
                        <div className="vanilla-hub__skin-fallback" aria-hidden>
                          ?
                        </div>
                      )}
                    </div>
                    <p className="vanilla-hub__play-version">{selectedVersion || '—'}</p>
                    <p className="vanilla-hub__play-sub vanilla-hub__play-sub--hero">
                      {selectedVersion && hubVersionPitch ? hubVersionPitch : t('vanillaHub.playSub')}
                    </p>
                  </div>
                  <div className="vanilla-hub__play-dock">
                    <div className="vanilla-hub__launch-row vanilla-hub__launch-row--dock">
                      <button
                        type="button"
                        className={`vanilla-hub__launch-cta${showCloseGameCta ? ' vanilla-hub__launch-cta--close' : ''}`}
                        disabled={ctaDisabledBusy || (!showCloseGameCta && !selectedVersion)}
                        aria-busy={ctaBusy}
                        aria-label={
                          showCloseGameCta
                            ? t('vanillaMc.closeGameAria')
                            : ctaBusy
                              ? showLaunchingLabel
                                ? t('vanillaMc.launchingAria')
                                : t('vanillaMc.installing')
                              : undefined
                        }
                        onClick={() => void (showCloseGameCta ? onCloseRunningGame() : onInstallOrLaunch())}
                      >
                        <span className="vanilla-hub__launch-cta__ico" aria-hidden>
                          {showCloseGameCta ? '■' : selectedClientInstalled ? '▶' : '↓'}
                        </span>
                        <span className="vanilla-hub__launch-cta__txt">
                          {showCloseGameCta
                            ? t('vanillaMc.closeGameCta')
                            : showLaunchingLabel
                              ? `${t('vanillaMc.launchingBase')}${reduceMotion ? '…' : '.'.repeat(busyDots)}`
                              : showInstallingLabel
                                ? `${t('vanillaMc.installingBase')}${reduceMotion ? '…' : '.'.repeat(busyDots)}`
                                : selectedClientInstalled
                                  ? t('vanillaHub.launchCta', { v: selectedVersion || '—' })
                                  : t('vanillaHub.installCta', { v: selectedVersion || '—' })}
                        </span>
                      </button>
                      <button
                        type="button"
                        className="vanilla-hub__ver-picker"
                        disabled={ctaDisabledBusy || soleaMcRunning}
                        title={
                          soleaMcRunning
                            ? t('vanillaMc.btnVersionsDisabledRunning')
                            : ctaDisabledBusy
                              ? t('vanillaHub.btnVersionsDisabledHint')
                              : t('vanillaHub.btnVersions')
                        }
                        aria-label={t('vanillaHub.btnVersions')}
                        onClick={() => setTab('versions')}
                      >
                        <span className="vanilla-hub__ver-picker__ico" aria-hidden>
                          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden>
                            <path
                              d="M4 7h16M4 12h16M4 17h10"
                              stroke="currentColor"
                              strokeWidth="2"
                              strokeLinecap="round"
                            />
                          </svg>
                        </span>
                        <span className="vanilla-hub__ver-picker__txt">{t('vanillaHub.btnVersions')}</span>
                      </button>
                    </div>
                    <button
                      type="button"
                      className="vanilla-hub__dock-folder"
                      title={t('vanillaHub.gameFolderHint')}
                      disabled={ctaDisabledBusy && !soleaMcRunning}
                      onClick={() => {
                        void (async () => {
                          const r = await window.solea.vanillaOpenDotMinecraftFolder()
                          if (!r.ok) pushToast(r.error, 'error')
                        })()
                      }}
                    >
                      {t('vanillaHub.gameFolderBtn')}
                    </button>
                  </div>
                </div>
              </div>
            </section>

            <section className="vanilla-hub__col vanilla-hub__col--news" aria-labelledby="vanilla-news-h">
              <div className="vanilla-hub__news-head">
                <div>
                  <h2 id="vanilla-news-h" className="vanilla-hub__news-title">
                    {t('vanillaHub.newsTitle')}
                  </h2>
                  <p className="vanilla-hub__news-sub">{t('vanillaHub.newsSubtitle')}</p>
                </div>
                <button
                  type="button"
                  className="vanilla-hub__news-refresh"
                  disabled={newsLoading}
                  onClick={() => void loadMinecraftNews()}
                  title={t('vanillaHub.newsRefresh')}
                  aria-label={t('vanillaHub.newsRefresh')}
                >
                  ↻
                </button>
              </div>
              <div className="vanilla-hub__news-scroll">
                {newsLoading && !newsItems.length ? (
                  <p className="vanilla-hub__news-loading">{t('vanillaHub.newsLoading')}</p>
                ) : null}
                {newsErr ? <p className="vanilla-hub__err vanilla-hub__err--inline">{newsErr}</p> : null}
                {!newsLoading && !newsErr && newsItems.length === 0 ? (
                  <p className="vanilla-hub__news-empty">{t('vanillaHub.newsEmpty')}</p>
                ) : null}
                {newsItems.map((item) => {
                  const img = newsImageUrl(item)
                  const href = item.readMoreLink?.trim()
                  return (
                    <article key={item.id} className="vanilla-hub__news-card">
                      {img ? (
                        <div className="vanilla-hub__news-card__media">
                          <img src={img} alt="" className="vanilla-hub__news-card__img" loading="lazy" />
                        </div>
                      ) : null}
                      <div className="vanilla-hub__news-card__body">
                        <div className="vanilla-hub__news-card__meta">
                          {item.tag ? <span className="vanilla-hub__news-card__tag">{item.tag}</span> : null}
                          <time className="vanilla-hub__news-card__date" dateTime={item.date}>
                            {formatNewsDateForUi(item.date)}
                          </time>
                        </div>
                        <h3 className="vanilla-hub__news-card__title">{item.title}</h3>
                        <p className="vanilla-hub__news-card__text">{item.text}</p>
                        {href ? (
                          <button
                            type="button"
                            className="vanilla-hub__news-card__more"
                            onClick={() => void window.solea.openExternalUrl(href)}
                          >
                            {t('vanillaHub.newsReadMore')}
                          </button>
                        ) : null}
                      </div>
                    </article>
                  )
                })}
              </div>
              <p className="vanilla-hub__news-foot">{t('vanillaHub.newsFooter')}</p>
            </section>
          </div>
        ) : (
          <div className="vanilla-hub__version-layout">
            <div className="vanilla-hub__version-main">
              <div className="vanilla-hub__version-head">
                <button
                  type="button"
                  className="vanilla-hub__back-pill"
                  onClick={() => setTab('hub')}
                  title={t('vanillaHub.backHub')}
                  aria-label={t('vanillaHub.backHub')}
                >
                  <span className="vanilla-hub__back-pill__ico" aria-hidden>
                    ⌂
                  </span>
                  <span className="vanilla-hub__back-pill__txt">{t('vanillaHub.backCta')}</span>
                </button>
                <div className="vanilla-hub__version-head-text">
                  <h2 className="vanilla-hub__version-head-title font-mc">{t('vanillaHub.versionsTitle')}</h2>
                  <p className="vanilla-hub__version-head-sub">{t('vanillaHub.versionsHeadHint')}</p>
                </div>
              </div>
              {manifest ? (
                <div className="vanilla-hub__maj-scroll">
                  <div className="vanilla-hub__maj-grid">
                    {majorKeysSorted.map((maj) => {
                      const art = majorTileArtUrl(maj)
                      return (
                        <button
                          key={maj}
                          type="button"
                          className={`vanilla-hub__maj-tile ${art ? 'vanilla-hub__maj-tile--photo' : tileGradientClass(maj)} ${
                            selectedMajor === maj ? 'vanilla-hub__maj-tile--selected' : ''
                          }`}
                          style={
                            art
                              ? ({
                                  ['--maj-tile-photo' as string]: `url(${art})`
                                } as CSSProperties)
                              : undefined
                          }
                          disabled={soleaMcRunning}
                          title={soleaMcRunning ? t('vanillaMc.btnVersionsDisabledRunning') : undefined}
                          onClick={() => onPickMajor(maj)}
                        >
                          <span className="vanilla-hub__maj-tile__ver font-mc">{maj}</span>
                        </button>
                      )
                    })}
                  </div>
                </div>
              ) : null}
            </div>
            <aside className="vanilla-hub__version-side vanilla-hub__version-side--integrated">
              <div
                className={`vanilla-hub__side-hero ${
                  sideHeroArtUrl ? 'vanilla-hub__side-hero--photo' : 'vanilla-hub__side-hero--solid'
                }`}
                style={
                  sideHeroArtUrl
                    ? ({ ['--side-hero-img' as string]: `url(${sideHeroArtUrl})` } as CSSProperties)
                    : undefined
                }
              >
                <div className="vanilla-hub__side-hero__media" aria-hidden />
                <div className="vanilla-hub__side-hero__fade" aria-hidden />
                <div className="vanilla-hub__side-hero__content">
                  <h3 className="vanilla-hub__side-title font-mc">{sideTitle}</h3>
                  <p className="vanilla-hub__side-blurb">{sideBlurb}</p>
                </div>
              </div>
              <p className="vanilla-hub__side-label">{t('vanillaHub.patchLine')}</p>
              <div className="vanilla-hub__patch-scroll">
                <div className="vanilla-hub__patch-grid">
                  {patchList.map((id) => (
                    <button
                      key={id}
                      type="button"
                      className={`vanilla-hub__patch-btn ${selectedVersion === id ? 'vanilla-hub__patch-btn--on' : ''}`}
                      disabled={soleaMcRunning}
                      title={soleaMcRunning ? t('vanillaMc.btnVersionsDisabledRunning') : undefined}
                      onClick={() => void onPersistVersion(id)}
                    >
                      {id}
                    </button>
                  ))}
                </div>
              </div>
              <div className="vanilla-hub__version-side-launch">
                <button
                  type="button"
                  className="vanilla-hub__launch-cta vanilla-hub__launch-cta--side vanilla-hub__launch-cta--confirm"
                  disabled={!selectedVersion || soleaMcRunning}
                  title={soleaMcRunning ? t('vanillaMc.btnVersionsDisabledRunning') : undefined}
                  onClick={() => void onConfirmVersionToHub()}
                >
                  <span className="vanilla-hub__launch-cta__ico" aria-hidden>
                    ✓
                  </span>
                  <span className="vanilla-hub__launch-cta__txt">
                    {t('vanillaHub.confirmVersionCta', { v: selectedVersion || '—' })}
                  </span>
                </button>
                <p className="vanilla-hub__launch-hint">{t('vanillaHub.confirmVersionHint')}</p>
              </div>
            </aside>
          </div>
        )}
      </div>
    </div>
  )
}

function dotToKey(major: string): string {
  return major.replace(/\./g, '_')
}

/** Court résumé « nom commercial + thème » pour une ligne majeure (ex. 1.15 → Buzzy Bees). */
function versionPitchForMajor(major: string, t: TFunction): string {
  const pk = `vanillaHub.versionPitch.${dotToKey(major)}`
  const s = t(pk)
  if (s !== pk) return s
  return t('vanillaHub.versionPitch.fallback', { maj: major })
}

function majorFromReleaseId(id: string): string | null {
  const m = /^(\d+\.\d+)/.exec(id.trim())
  return m ? m[1]! : null
}

function buildMajorToLatestPatch(versions: { id: string; type: string }[]): Map<string, string> {
  const map = new Map<string, string>()
  for (const v of versions) {
    if (v.type !== 'release') continue
    if (!releaseLineAtLeast18(v.id)) continue
    const maj = majorFromReleaseId(v.id)
    if (!maj || !majorKeyAtLeast18(maj)) continue
    const cur = map.get(maj)
    if (!cur || compareFullReleaseDesc(v.id, cur) > 0) map.set(maj, v.id)
  }
  return map
}

function sortMajorKeysDesc(keys: string[]): string[] {
  return [...keys].sort((a, b) => compareSemverKeysDesc(a, b))
}

function compareSemverKeysDesc(a: string, b: string): number {
  const pa = a.split('.').map((x) => parseInt(x, 10) || 0)
  const pb = b.split('.').map((x) => parseInt(x, 10) || 0)
  const n = Math.max(pa.length, pb.length)
  for (let i = 0; i < n; i++) {
    const na = pa[i] ?? 0
    const nb = pb[i] ?? 0
    if (na !== nb) return nb - na
  }
  return 0
}

function listPatchesForMajor(major: string, versions: { id: string; type: string }[]): string[] {
  const out: string[] = []
  for (const v of versions) {
    if (v.type !== 'release') continue
    if (!releaseLineAtLeast18(v.id)) continue
    if (v.id === major || v.id.startsWith(`${major}.`)) out.push(v.id)
  }
  return out.sort(compareFullReleaseDesc).slice(0, 28)
}

function compareFullReleaseDesc(a: string, b: string): number {
  const pa = a.split('.').map((x) => parseInt(x, 10) || 0)
  const pb = b.split('.').map((x) => parseInt(x, 10) || 0)
  const n = Math.max(pa.length, pb.length)
  for (let i = 0; i < n; i++) {
    const na = pa[i] ?? 0
    const nb = pb[i] ?? 0
    if (na !== nb) return nb - na
  }
  return 0
}

/**
 * Releases `type: release` du manifeste Mojang : ligne **1.y** (y ≥ 8) ou toute ligne **X.y** avec X > 1
 * (ex. 26.2.x dès que Mojang les publie).
 *
 * **Règle d’ajout côté Solea (tuile, visuel, texte « pitch », vérifs install) :** on ne « prend en charge »
 * officiellement une nouvelle ligne majeure qu’après que **Fabric**, **Iris** et **Sodium** disposent chacun
 * d’une mise à jour Modrinth compatible avec cette version Minecraft. Exemple : la 26.2 apparaît dans le
 * manifeste — on n’ajoute pas tout de suite tuile + assets + i18n ; on attend les trois mods, puis on
 * complète le hub comme pour les lignes 1.x. D’ici là l’id peut déjà être listé ici si le filtre l’inclut ;
 * l’install stack Iris dépend de `vanillaShaderMods.ts` (échec partiel si jars absents sur Modrinth).
 */
function releaseLineAtLeast18(id: string): boolean {
  const m = /^(\d+)\.(\d+)/.exec(id.trim())
  if (!m) return false
  const major = parseInt(m[1]!, 10)
  const minor = parseInt(m[2]!, 10)
  if (Number.isNaN(major) || Number.isNaN(minor)) return false
  if (major > 1) return true
  if (major < 1) return false
  return minor >= 8
}

function majorKeyAtLeast18(maj: string): boolean {
  const parts = maj.split('.').map((x) => parseInt(x, 10) || 0)
  const a = parts[0] ?? 0
  const b = parts[1] ?? 0
  if (a > 1) return true
  if (a < 1) return false
  return b >= 8
}

function firstReleaseAtLeast18(versions: { id: string; type: string }[]): string | null {
  for (const v of versions) {
    if (v.type === 'release' && releaseLineAtLeast18(v.id)) return v.id
  }
  return null
}

