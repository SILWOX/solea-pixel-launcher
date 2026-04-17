/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import type { LauncherSettingsUI, ModpackGameProfileUI } from './launcherTypes'
import type { TFunction } from './i18n/I18nContext'

export function cloneLauncherSettings(s: LauncherSettingsUI): LauncherSettingsUI {
  return JSON.parse(JSON.stringify(s)) as LauncherSettingsUI
}

function sortedModpackProfiles(profiles: Record<string, ModpackGameProfileUI>) {
  const out: Record<string, ModpackGameProfileUI> = {}
  for (const id of Object.keys(profiles).sort()) {
    out[id] = profiles[id]
  }
  return out
}

function normalizeForCompare(s: LauncherSettingsUI) {
  return { ...s, modpackProfiles: sortedModpackProfiles(s.modpackProfiles) }
}

export function isLauncherSettingsDirty(a: LauncherSettingsUI, b: LauncherSettingsUI): boolean {
  return JSON.stringify(normalizeForCompare(a)) !== JSON.stringify(normalizeForCompare(b))
}

export type UnsavedSettingsSection = {
  id: string
  title: string
  items: string[]
}

const LAUNCHER_KEYS: (keyof LauncherSettingsUI)[] = [
  'memoryMin',
  'memoryMax',
  'jvmArgs',
  'gameArgs',
  'downloadThreads',
  'networkTimeoutMs',
  'javaPath',
  'javaVersion',
  'screenWidth',
  'screenHeight',
  'fullscreen',
  'azureClientId',
  'afterLaunch',
  'openGameLogOnInstanceLaunch',
  'activeModpackId',
  'uiLanguage',
  'uiTheme',
  'uiAccentHex',
  'uiFontScale',
  'uiReduceMotion',
  'uiCompact',
  'uiHomeCardVariant',
  'uiChromeGlass',
  'uiLiquidGlass',
  'uiSettingsShell',
  'uiSounds',
  'uiSoundVolume',
  'uiSoundInstall',
  'uiSoundLaunch',
  'discordRichPresence',
  'updateChannel',
  'skinViewerAnimation',
  'uiShortcutOpenSettings',
  'uiShortcutGoNews',
  'uiShortcutGoAccount',
  'nativeNotifications',
  'diagnosticLaunch',
  'networkSlowDownloads'
]

const LAUNCHER_FIELD_LABEL_KEY: Partial<Record<keyof LauncherSettingsUI, string>> = {
  memoryMin: 'settings.ram',
  memoryMax: 'settings.ram',
  jvmArgs: 'settings.unsaved.label.jvmArgs',
  gameArgs: 'settings.unsaved.label.gameArgsTop',
  downloadThreads: 'settings.downloadThreads',
  networkTimeoutMs: 'settings.timeout',
  javaPath: 'settings.unsaved.label.javaPath',
  javaVersion: 'settings.unsaved.label.javaVersion',
  screenWidth: 'settings.width',
  screenHeight: 'settings.height',
  fullscreen: 'settings.fullscreen',
  azureClientId: 'settings.azureId',
  afterLaunch: 'settings.afterLaunch',
  openGameLogOnInstanceLaunch: 'settings.openLogConsoleOnLaunch',
  activeModpackId: 'settings.unsaved.label.activePack',
  uiLanguage: 'settings.lang',
  uiTheme: 'settings.theme',
  uiAccentHex: 'settings.unsaved.label.accent',
  uiFontScale: 'settings.fontScale',
  uiReduceMotion: 'settings.reduceMotion',
  uiCompact: 'settings.uiCompact',
  uiHomeCardVariant: 'settings.uiLauncherExperience',
  uiChromeGlass: 'settings.chromeGlass',
  uiLiquidGlass: 'settings.liquidGlass',
  uiSettingsShell: 'settings.uiLauncherExperience',
  uiSounds: 'settings.uiSounds',
  uiSoundVolume: 'settings.uiSoundVolume',
  uiSoundInstall: 'settings.uiSoundInstall',
  uiSoundLaunch: 'settings.uiSoundLaunch',
  discordRichPresence: 'settings.discordRp',
  updateChannel: 'settings.updateChannel',
  skinViewerAnimation: 'settings.skinAnim',
  uiShortcutOpenSettings: 'settings.shortcutOpenSettings',
  uiShortcutGoNews: 'settings.shortcutGoNews',
  uiShortcutGoAccount: 'settings.shortcutGoAccount',
  nativeNotifications: 'settings.nativeNotifications',
  diagnosticLaunch: 'settings.unsaved.label.diagnosticLaunch',
  networkSlowDownloads: 'settings.networkSlowDownloads'
}

export function buildUnsavedSettingsSections(
  baseline: LauncherSettingsUI,
  current: LauncherSettingsUI,
  modpacksList: { id: string; displayName: string }[],
  t: TFunction
): UnsavedSettingsSection[] {
  const sections: UnsavedSettingsSection[] = []
  const launcherItems: string[] = []
  const seen = new Set<string>()

  for (const key of LAUNCHER_KEYS) {
    if (baseline[key] !== current[key]) {
      const lk = LAUNCHER_FIELD_LABEL_KEY[key] ?? 'settings.unsaved.label.generic'
      const label = lk === 'settings.unsaved.label.generic' ? t(lk, { key: String(key) }) : t(lk)
      if (!seen.has(label)) {
        seen.add(label)
        launcherItems.push(label)
      }
    }
  }

  if (launcherItems.length) {
    sections.push({
      id: 'launcher',
      title: t('settings.unsaved.sectionLauncher'),
      items: launcherItems.sort((a, b) => a.localeCompare(b))
    })
  }

  const vb = baseline.vanillaGameProfile
  const vc = current.vanillaGameProfile
  if (vb && vc) {
    const vItems: string[] = []
    if (vb.memoryMin !== vc.memoryMin || vb.memoryMax !== vc.memoryMax) vItems.push(t('settings.ram'))
    if (
      vb.screenWidth !== vc.screenWidth ||
      vb.screenHeight !== vc.screenHeight ||
      vb.fullscreen !== vc.fullscreen
    ) {
      vItems.push(t('settings.resolution'))
    }
    if (vb.gameArgs !== vc.gameArgs) vItems.push(t('settings.gameArgs'))
    if (vItems.length) {
      sections.push({
        id: 'vanilla',
        title: t('settings.unsaved.sectionVanilla'),
        items: vItems.sort((a, b) => a.localeCompare(b))
      })
    }
  }

  const packIds = new Set([
    ...Object.keys(baseline.modpackProfiles),
    ...Object.keys(current.modpackProfiles)
  ])

  for (const id of [...packIds].sort()) {
    const pb = baseline.modpackProfiles[id]
    const pc = current.modpackProfiles[id]
    if (!pb || !pc) continue
    const items: string[] = []
    if (pb.memoryMin !== pc.memoryMin || pb.memoryMax !== pc.memoryMax) items.push(t('settings.ram'))
    if (
      pb.screenWidth !== pc.screenWidth ||
      pb.screenHeight !== pc.screenHeight ||
      pb.fullscreen !== pc.fullscreen
    ) {
      items.push(t('settings.resolution'))
    }
    if (pb.gameArgs !== pc.gameArgs) items.push(t('settings.gameArgs'))
    const pbPath = baseline.modpackInstanceParentPath?.[id]?.trim() ?? ''
    const pcPath = current.modpackInstanceParentPath?.[id]?.trim() ?? ''
    if (pbPath !== pcPath) items.push(t('settings.instancePath'))
    if (items.length) {
      const name = modpacksList.find((m) => m.id === id)?.displayName ?? id
      sections.push({
        id: `pack-${id}`,
        title: t('settings.unsaved.sectionModpack', { name }),
        items
      })
    }
  }

  if (sections.length === 0 && isLauncherSettingsDirty(baseline, current)) {
    sections.push({
      id: 'generic',
      title: t('settings.unsaved.sectionOther'),
      items: [t('settings.unsaved.genericChanges')]
    })
  }

  return sections
}
