export type ModpackId = 'palamod-recreated' | 'wither-storm'

export type ModpackLoader = 'neoforge' | 'forge'

export interface ModpackSpec {
  id: ModpackId
  displayName: string
  /** Slug Modrinth (URL /api/project/{slug}) */
  projectSlug: string
  gameVersion: string
  /** Loader Modrinth + minecraft-java-core */
  loader: ModpackLoader
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
  }
]

/** Page Modrinth du modpack (slug projet). */
export function modrinthModpackPageUrl(spec: ModpackSpec): string {
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
