import { z } from 'zod'

/**
 * Valide et nettoie le JSON disque avant fusion avec les défauts.
 * Les clés inconnues sont retirées ; les types invalides font échouer le parse du champ parent si strict.
 */
export const launcherSettingsStoredSchema = z
  .object({
    memoryMin: z.string().optional(),
    memoryMax: z.string().optional(),
    jvmArgs: z.string().optional(),
    gameArgs: z.string().optional(),
    downloadThreads: z.coerce.number().int().min(1).max(32).optional(),
    networkTimeoutMs: z.coerce.number().int().min(5000).max(120000).optional(),
    javaPath: z.string().optional(),
    javaVersion: z.string().optional(),
    screenWidth: z.number().nullable().optional(),
    screenHeight: z.number().nullable().optional(),
    fullscreen: z.boolean().optional(),
    azureClientId: z.string().optional(),
    afterLaunch: z.enum(['keep', 'minimize']).optional(),
    activeModpackId: z.string().optional(),
    modpackProfiles: z.record(z.string(), z.any()).optional(),
    uiLanguage: z.enum(['en', 'fr']).optional(),
    uiTheme: z.enum(['light', 'dark', 'system']).optional(),
    uiAccentHex: z.string().optional(),
    uiFontScale: z.enum(['s', 'm', 'l']).optional(),
    uiReduceMotion: z.boolean().optional(),
    uiCompact: z.boolean().optional(),
    uiSounds: z.boolean().optional(),
    uiSoundVolume: z.coerce.number().min(0).max(1).optional(),
    uiSoundInstall: z.boolean().optional(),
    uiSoundLaunch: z.boolean().optional(),
    discordRichPresence: z.boolean().optional(),
    updateChannel: z.enum(['stable', 'beta']).optional(),
    skinViewerAnimation: z
      .enum([
        'none',
        'idle',
        'walk',
        'run',
        'fly',
        'wave',
        'wave_left',
        'crouch',
        'hit'
      ])
      .optional(),
    uiShortcutOpenSettings: z.string().optional(),
    uiShortcutGoNews: z.string().optional(),
    uiShortcutGoAccount: z.string().optional(),
    nativeNotifications: z.boolean().optional(),
    diagnosticLaunch: z.boolean().optional(),
    networkSlowDownloads: z.boolean().optional(),
    uiChromeGlass: z.boolean().optional()
  })
  .strip()

export type ParsedStoredSettings = z.infer<typeof launcherSettingsStoredSchema>

export function parseLauncherSettingsFromDisk(raw: unknown): ParsedStoredSettings {
  if (typeof raw !== 'object' || raw === null) return {}
  const r = launcherSettingsStoredSchema.safeParse(raw)
  if (!r.success) return {}
  return r.data
}
