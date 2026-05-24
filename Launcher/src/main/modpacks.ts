export type ModpackId =
  | 'palamod-recreated'
  | 'wither-storm'
  | 'mythic-trials-1'
  | 'mythic-trials-2'
  | 'aeloria'
  | 'solea-optimised'

export type ModpackLoader = 'neoforge' | 'forge' | 'fabric'

export type ModrinthInstallKind = 'modpack' | 'mod'

export interface ModpackSpec {
  id: ModpackId
  displayName: string
  /** Slug Modrinth (URL /api/project/{slug}) */
  projectSlug: string
  gameVersion: string
  /** Loader Modrinth + minecraft-java-core */
  loader: ModpackLoader
  /** modpack = .mrpack ; mod = .jar unique (Better MC sur Modrinth). */
  modrinthKind?: ModrinthInstallKind
  /** Build loader de repli si absent du pack / du .jar (Fabric, Forge). NeoForge : détecté auto. */
  loaderBuild?: string
  /** Version Java conseillée (affichage / défaut réglages) */
  recommendedJava: string
  /**
   * Clé « Art Assets » Discord (même nom que dans le portail développeur).
   * Si absente, Rich Presence utilise l’image « logo ».
   */
  discordLargeImageKey?: string
  /** Invitation Discord affichée sur le panneau d’accueil. */
  discordUrl?: string
}

export const MODPACKS: ModpackSpec[] = [
  {
    id: 'palamod-recreated',
    displayName: 'Palamod Recreated',
    projectSlug: 'paladium-mc',
    gameVersion: '1.21.1',
    loader: 'neoforge',
    recommendedJava: '21',
    discordLargeImageKey: 'solea_pack_palamod',
    discordUrl: 'https://discord.gg/jVGq5aZ6Wc'
  },
  {
    id: 'wither-storm',
    displayName: 'The End Of Wither Storm',
    projectSlug: 'the-end-of-wither-storm',
    gameVersion: '1.20.1',
    loader: 'forge',
    recommendedJava: '17',
    discordLargeImageKey: 'solea_pack_wither',
    discordUrl: 'https://discord.gg/jVGq5aZ6Wc'
  },
  {
    id: 'mythic-trials-1',
    displayName: 'MYTHIC TRIALS 1',
    projectSlug: 'mythic-trials',
    gameVersion: '1.20.1',
    loader: 'forge',
    recommendedJava: '17',
    discordLargeImageKey: 'solea_pack_mythic_trials',
    discordUrl: 'https://discord.gg/jVGq5aZ6Wc'
  },
  {
    id: 'mythic-trials-2',
    displayName: 'MYTHIC TRIALS 2',
    projectSlug: 'mythic-trials-mt2',
    gameVersion: '1.20.1',
    loader: 'forge',
    recommendedJava: '17',
    discordLargeImageKey: 'solea_pack_mythic_trials_2',
    discordUrl: 'https://discord.gg/jVGq5aZ6Wc'
  },
  {
    id: 'aeloria',
    displayName: 'Better MC',
    projectSlug: 'bmcmod',
    gameVersion: '1.21.1',
    loader: 'neoforge',
    modrinthKind: 'mod',
    recommendedJava: '21',
    discordLargeImageKey: 'solea_pack_aeloria',
    discordUrl: 'https://discord.gg/jVGq5aZ6Wc'
  },
  {
    id: 'solea-optimised',
    displayName: 'SOLEA OPTIMISED',
    projectSlug: 'solea-optimised',
    gameVersion: '1.21.11',
    loader: 'fabric',
    loaderBuild: '0.18.4',
    recommendedJava: '21',
    discordLargeImageKey: 'solea_pack_optimised',
    discordUrl: 'https://discord.gg/jVGq5aZ6Wc'
  }
]

/** Page Modrinth du modpack (slug projet). */
export function modrinthModpackPageUrl(spec: ModpackSpec): string {
  if (spec.id === 'aeloria') return `https://modrinth.com/mod/${spec.projectSlug}`
  return `https://modrinth.com/modpack/${spec.projectSlug}`
}

export const DEFAULT_MODPACK_ID: ModpackId = 'palamod-recreated'

const BY_ID = Object.fromEntries(MODPACKS.map((m) => [m.id, m])) as Record<ModpackId, ModpackSpec>

export function resolveModpackId(raw: string | undefined | null): ModpackId {
  if (raw && raw in BY_ID) return raw as ModpackId
  return DEFAULT_MODPACK_ID
}

export function getModpackSpec(id: ModpackId): ModpackSpec {
  return BY_ID[id]
}

export function listModpackSummaries(): { id: ModpackId; displayName: string }[] {
  return MODPACKS.map((m) => ({ id: m.id, displayName: m.displayName }))
}
