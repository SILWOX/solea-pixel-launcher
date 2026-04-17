/**
 * Identifiants de profil vanilla Solea (`soleapixel-…`) — métadonnées dans `vanilla-profiles-meta/`.
 * Le jeu utilise le `.minecraft` officiel ; `minecraft-java-core` continue à télécharger sous `versions/<release>/`.
 */

/** 1.8–1.15 : profil « OptiFine » = Forge installé par le launcher + OptiFine en jar dans mods/ ; 1.16+ : Iris (+ Sodium) Fabric. */
export function defaultShaderStackForReleaseId(versionId: string): 'optifine' | 'iris' {
  const m = /^(\d+)\.(\d+)/.exec(versionId.trim())
  if (!m) return 'iris'
  const major = parseInt(m[1]!, 10)
  const minor = parseInt(m[2]!, 10)
  if (Number.isNaN(major) || Number.isNaN(minor)) return 'iris'
  if (major !== 1) return 'iris'
  if (minor < 8) return 'iris'
  if (minor <= 15) return 'optifine'
  return 'iris'
}

/**
 * Indice `java.version` pour minecraft-java-core : basé sur la **release Minecraft**,
 * pas sur le modpack actif du launcher (évite Java 21 sur 1.14.4).
 */
export function vanillaJavaMajorHint(versionId: string): string {
  const m = /^(\d+)\.(\d+)(?:\.(\d+))?/.exec(String(versionId ?? '').trim())
  if (!m) return '21'
  const maj = parseInt(m[1]!, 10)
  const min = parseInt(m[2]!, 10)
  const pat = m[3] != null ? parseInt(m[3]!, 10) : 0
  if (Number.isNaN(maj) || Number.isNaN(min)) return '21'
  /** Lignes « calendrier » Mojang (26.x, …) : JVM ≈ année MC − 1 (ex. 26.1 → 25) jusqu’à lecture du JSON officiel. */
  if (maj > 1) {
    if (maj >= 24) return String(Math.max(21, maj - 1))
    return '21'
  }
  if (min < 17) return '8'
  if (min === 17) return '16'
  if (min < 20) return '17'
  if (min === 20 && pat < 5) return '17'
  return '21'
}

/**
 * Segment de dossier sûr (pas de séparateurs, caractères réservés retirés).
 */
export function sanitizeVanillaFolderSegment(raw: string): string {
  let s = String(raw ?? '')
    .replace(/[<>:"/\\|?*\x00-\x1f]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
  s = s.replace(/[. ]+$/g, '').trim()
  if (!s) s = 'vanilla'
  if (s.includes('..')) s = 'vanilla'
  if (s.length > 120) s = s.slice(0, 120).replace(/[. ]+$/g, '').trim() || 'vanilla'
  return s
}

/**
 * ID profil Solea (fichier meta + affichage) : `soleapixel-<release>-IrisLoader` ou `soleapixel-<release>-OptiFine`.
 * Le client Mojang reste sous `.minecraft/versions/<release>/`.
 */
export function vanillaInstallFolderName(versionId: string, shaderStack: 'optifine' | 'iris'): string {
  const v = String(versionId ?? '')
    .trim()
    .replace(/\s+/g, '')
  const suffix = shaderStack === 'optifine' ? 'OptiFine' : 'IrisLoader'
  return sanitizeVanillaFolderSegment(v ? `soleapixel-${v}-${suffix}` : `soleapixel-${suffix}`)
}
