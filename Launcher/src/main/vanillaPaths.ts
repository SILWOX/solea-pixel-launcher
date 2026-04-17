/**
 * Vanilla Solea : jeu dans le **.minecraft** officiel ; métadonnées dans `userData/vanilla-profiles-meta/`.
 * Install détectée seulement si `.minecraft/versions/soleapixel-<release>-IrisLoader|OptiFine/` existe avec le JSON marqueur
 * (`inheritsFrom` → id Mojang réel). Les dossiers `1.12.2` seuls (launcher officiel) ne comptent pas comme install Solea.
 */
import { join, dirname } from 'node:path'
import os from 'node:os'
import { mkdirSync, existsSync, readFileSync, writeFileSync, readdirSync, statSync, rmSync } from 'node:fs'
import AdmZip from 'adm-zip'
import { sanitizeVanillaFolderSegment } from '../vanillaFolderNames.js'

export {
  sanitizeVanillaFolderSegment,
  vanillaInstallFolderName,
  defaultShaderStackForReleaseId,
  vanillaJavaMajorHint
} from '../vanillaFolderNames.js'

const VANILLA_META_SIDE_DIR = 'vanilla-profiles-meta'
const VANILLA_BACKUPS_DIR = 'vanilla-saves-backups'
const LEGACY_MINECRAFT_VERSION_DIR = 'minecraft-version'

/** Dossier client réservé au vanilla Solea sous `.minecraft/versions/` (pas les ids Mojang nus comme `1.12.2`). */
const SOLEA_VERSION_DIR_RE = /^soleapixel-.+-(IrisLoader|OptiFine)$/i

function vanillaMetaSidecarPath(userData: string, profileId: string): string {
  return join(userData, VANILLA_META_SIDE_DIR, `${sanitizeVanillaFolderSegment(profileId)}.json`)
}

/** Emplacement ZIP des sauvegardes vanilla (hors .minecraft). */
function vanillaBackupsDir(userData: string, profileId: string): string {
  return join(userData, VANILLA_BACKUPS_DIR, sanitizeVanillaFolderSegment(profileId))
}

export type VanillaMeta = {
  javaPath?: string | null
  javaVersion?: string | null
  shaderStack?: 'optifine' | 'iris'
  lastSelectedVersion?: string | null
}

/** @deprecated Utiliser `sanitizeVanillaFolderSegment` depuis `vanillaFolderNames.ts`. */
export function sanitizeVanillaProfileId(raw: string): string {
  return sanitizeVanillaFolderSegment(raw)
}

/** Répertoire Minecraft Java par défaut (celui du launcher Mojang). */
export function getDefaultDotMinecraftPath(): string {
  if (process.platform === 'darwin') {
    return join(os.homedir(), 'Library', 'Application Support', 'minecraft')
  }
  if (process.platform === 'win32') {
    const ad = process.env.APPDATA
    if (ad?.trim()) return join(ad.trim(), '.minecraft')
    return join(os.homedir(), 'AppData', 'Roaming', '.minecraft')
  }
  return join(os.homedir(), '.minecraft')
}

/** @deprecated Ancien parent `userData/minecraft-version` — conservé pour lecture meta héritée uniquement. */
export function getMinecraftVersionRoot(userData: string): string {
  return join(userData, 'minecraft-version')
}

/** @deprecated Ne plus utiliser pour les chemins de jeu ; seulement migration légacy meta sur disque. */
export function getVanillaProfileRoot(userData: string, profileId: string): string {
  return join(getMinecraftVersionRoot(userData), sanitizeVanillaFolderSegment(profileId))
}

/** Dossier de jeu partagé : racine `.minecraft` (saves, mods, options, screenshots…). */
export function getVanillaProfileGameDir(_userData: string, _profileId?: string): string {
  return getDefaultDotMinecraftPath()
}

/** Ancien chemin `…/profile/vanilla-profile.json` sous minecraft-version (migration). */
export function vanillaMetaPath(userData: string, profileId: string): string {
  return join(getVanillaProfileRoot(userData, profileId), 'vanilla-profile.json')
}

export function readVanillaMeta(userData: string, profileId: string): VanillaMeta {
  let merged: VanillaMeta = {}
  const legacy = vanillaMetaPath(userData, profileId)
  if (existsSync(legacy)) {
    try {
      merged = { ...merged, ...(JSON.parse(readFileSync(legacy, 'utf8')) as VanillaMeta) }
    } catch {
      /* skip */
    }
  }
  const side = vanillaMetaSidecarPath(userData, profileId)
  if (existsSync(side)) {
    try {
      merged = { ...merged, ...(JSON.parse(readFileSync(side, 'utf8')) as VanillaMeta) }
    } catch {
      /* skip */
    }
  }
  return merged
}

export function writeVanillaMeta(userData: string, profileId: string, meta: VanillaMeta): void {
  const side = vanillaMetaSidecarPath(userData, profileId)
  mkdirSync(dirname(side), { recursive: true })
  writeFileSync(side, JSON.stringify(meta, null, 2), 'utf8')
}

/** Supprime l’ancienne arborescence `userData/minecraft-version/` (profils isolés) — le vanilla utilise uniquement `.minecraft`. */
export function removeLegacyVanillaMinecraftVersionTree(userData: string): void {
  const legacy = join(userData, LEGACY_MINECRAFT_VERSION_DIR)
  if (!existsSync(legacy)) return
  try {
    rmSync(legacy, { recursive: true, force: true })
  } catch {
    /* ignore : dossier verrouillé ou autre */
  }
}

export function ensureVanillaProfileLayout(userData: string, _profileId: string): { gameDir: string } {
  removeLegacyVanillaMinecraftVersionTree(userData)
  const gameDir = getVanillaProfileGameDir(userData)
  const subs = ['saves', 'screenshots', 'resourcepacks', 'shaderpacks', 'logs', 'mods', 'config']
  for (const s of subs) {
    mkdirSync(join(gameDir, s), { recursive: true })
  }
  return { gameDir }
}

/** Dossier `versions` du .minecraft officiel. */
export function getVanillaClientVersionsDir(_userData: string, _profileId?: string): string {
  return join(getDefaultDotMinecraftPath(), 'versions')
}

type MojangClientVersionJson = {
  inheritsFrom?: string
  javaVersion?: { majorVersion?: number }
}

/**
 * Lit `javaVersion.majorVersion` dans le JSON client déjà téléchargé (`.minecraft/versions/<id>/<id>.json`),
 * en suivant `inheritsFrom` si besoin — même source que le launcher Mojang pour les lignes 26.x (ex. JVM 25).
 */
export function readInstalledClientVersionJavaMajor(mojangVersionId: string): number | null {
  let cur = String(mojangVersionId ?? '').trim()
  if (!cur || cur.includes('..') || cur !== cur.replace(/[^\w.-]/g, '')) return null
  const versionsRoot = getVanillaClientVersionsDir('')
  const seen = new Set<string>()
  for (let depth = 0; depth < 16 && cur; depth++) {
    if (seen.has(cur)) return null
    seen.add(cur)
    const jsonPath = join(versionsRoot, cur, `${cur}.json`)
    if (!existsSync(jsonPath)) return null
    let raw: MojangClientVersionJson
    try {
      raw = JSON.parse(readFileSync(jsonPath, 'utf8')) as MojangClientVersionJson
    } catch {
      return null
    }
    const mj = raw.javaVersion?.majorVersion
    if (typeof mj === 'number' && mj > 0) return mj
    const inh = typeof raw.inheritsFrom === 'string' ? raw.inheritsFrom.trim() : ''
    if (!inh || inh.includes('..') || inh !== inh.replace(/[^\w.-]/g, '')) return null
    cur = inh
  }
  return null
}

/** True si un pack client Mojang pour cette release est présent (dossier exact ou variante type `1.12.2-OptiFine_…`). */
function mojangVersionPackPresent(versionsRoot: string, base: string): boolean {
  if (!base || base.includes('..') || base !== base.replace(/[^\w.-]/g, '')) return false
  if (!existsSync(versionsRoot)) return false
  if (existsSync(join(versionsRoot, base, `${base}.json`))) return true
  if (existsSync(join(versionsRoot, base, `${base}.jar`))) return true
  for (const name of readdirSync(versionsRoot)) {
    if (!name.startsWith(`${base}-`)) continue
    const full = join(versionsRoot, name)
    try {
      if (!statSync(full).isDirectory()) continue
    } catch {
      continue
    }
    if (readdirSync(full).some((f) => f.endsWith('.json'))) return true
  }
  return false
}

function readSoleaMarkerInheritsFrom(versionsRoot: string, soleaFolderName: string): string | null {
  const id = sanitizeVanillaFolderSegment(soleaFolderName)
  if (!id || !SOLEA_VERSION_DIR_RE.test(id)) return null
  const markerPath = join(versionsRoot, id, `${id}.json`)
  if (!existsSync(markerPath)) return null
  try {
    const raw = JSON.parse(readFileSync(markerPath, 'utf8')) as { id?: unknown; inheritsFrom?: unknown }
    if (raw?.id !== id || typeof raw.inheritsFrom !== 'string') return null
    const mc = raw.inheritsFrom.trim()
    if (!mc || mc.includes('..') || mc !== mc.replace(/[^\w.-]/g, '')) return null
    if (!mojangVersionPackPresent(versionsRoot, mc)) return null
    return mc
  } catch {
    return null
  }
}

/**
 * Marqueur Solea : petit JSON `inheritsFrom` l’id Mojang réel (dossier `versions/1.12.2/` créé par minecraft-java-core).
 * Le hub ne considère comme install Solea que ce dossier `versions/soleapixel-…/`, pas un `1.12.2` seul du launcher officiel.
 */
export function writeVanillaSoleaVersionMarker(
  profileId: string,
  inheritsFromMojangId: string
): { ok: true } | { ok: false; error: string } {
  const id = sanitizeVanillaFolderSegment(profileId)
  const base = inheritsFromMojangId.trim()
  if (!SOLEA_VERSION_DIR_RE.test(id)) {
    return { ok: false, error: 'Invalid Solea version folder name (expected soleapixel-<release>-IrisLoader|OptiFine).' }
  }
  if (!base || base.includes('..') || base !== base.replace(/[^\w.-]/g, '')) {
    return { ok: false, error: 'Invalid base Minecraft version id.' }
  }
  const verDir = getVanillaClientVersionsDir('')
  if (!mojangVersionPackPresent(verDir, base)) {
    return { ok: false, error: 'Base Minecraft version is not installed under .minecraft/versions yet.' }
  }
  mkdirSync(join(verDir, id), { recursive: true })
  const body = {
    id,
    inheritsFrom: base,
    type: 'release',
    releaseTime: new Date().toISOString(),
    time: new Date().toISOString()
  }
  writeFileSync(join(verDir, id, `${id}.json`), JSON.stringify(body, null, 2), 'utf8')
  return { ok: true }
}

export function ensureVanillaSoleaVersionMarker(
  profileId: string,
  mojangVersionId: string
): { ok: true } | { ok: false; error: string } {
  const id = sanitizeVanillaFolderSegment(profileId)
  const verDir = getVanillaClientVersionsDir('')
  if (existsSync(join(verDir, id, `${id}.json`))) return { ok: true }
  return writeVanillaSoleaVersionMarker(profileId, mojangVersionId)
}

/** Indique si le client vanilla **Solea** est installé (dossier marqueur `soleapixel-…` + client Mojang de base). */
export function listVanillaClientVersionIds(userData: string, profileId: string): string[] {
  void userData
  const verDir = getVanillaClientVersionsDir(userData)
  if (!existsSync(verDir)) return []
  const id = sanitizeVanillaFolderSegment(profileId)
  const mc = readSoleaMarkerInheritsFrom(verDir, id)
  if (!mc) return []
  return [mc]
}

function mergeVanillaInstallRow(
  rows: { folder: string; versions: string[] }[],
  folder: string,
  vid: string
): void {
  const f = sanitizeVanillaFolderSegment(folder)
  const v = String(vid ?? '').trim()
  if (!f || !v) return
  const row = rows.find((r) => r.folder === f)
  if (row) {
    if (!row.versions.includes(v)) row.versions.push(v)
  } else {
    rows.push({ folder: f, versions: [v] })
  }
}

/**
 * Lignes Paramètres : uniquement les installs Solea (`versions/soleapixel-…-IrisLoader|OptiFine/` + JSON marqueur).
 * Les dossiers Mojang seuls (`1.12.2`, etc.) ne sont pas listés.
 */
export function listAllVanillaInstallFolders(userData: string): { folder: string; versions: string[] }[] {
  const rows: { folder: string; versions: string[] }[] = []
  const seenFolder = new Set<string>()

  const metaDir = join(userData, VANILLA_META_SIDE_DIR)
  if (existsSync(metaDir)) {
    for (const name of readdirSync(metaDir)) {
      if (!name.endsWith('.json')) continue
      const folder = sanitizeVanillaFolderSegment(name.slice(0, -5))
      if (!folder) continue
      const versions = listVanillaClientVersionIds(userData, folder)
      for (const vid of versions) mergeVanillaInstallRow(rows, folder, vid)
      if (versions.length > 0) seenFolder.add(folder)
    }
  }

  const verDir = getVanillaClientVersionsDir(userData)
  if (existsSync(verDir)) {
    for (const name of readdirSync(verDir)) {
      const id = sanitizeVanillaFolderSegment(name)
      if (!SOLEA_VERSION_DIR_RE.test(id)) continue
      if (seenFolder.has(id)) continue
      const full = join(verDir, id)
      try {
        if (!statSync(full).isDirectory()) continue
      } catch {
        continue
      }
      const mc = readSoleaMarkerInheritsFrom(verDir, id)
      if (!mc) continue
      mergeVanillaInstallRow(rows, id, mc)
      seenFolder.add(id)
    }
  }

  for (const r of rows) {
    r.versions.sort((a, b) => a.localeCompare(b, undefined, { numeric: true, sensitivity: 'base' }))
  }
  return rows.sort((a, b) => a.folder.localeCompare(b.folder, undefined, { sensitivity: 'base' }))
}

/**
 * Supprime uniquement le dossier marqueur Solea `.minecraft/versions/<soleapixel-…>/` (quelques Ko de JSON).
 * Le cache client Mojang `versions/<release>/` (ex. `1.12.2`) n’est pas supprimé — le launcher officiel peut encore l’utiliser.
 */
export function uninstallVanillaClientVersion(
  userData: string,
  profileId: string,
  versionId: string
): { ok: true } | { ok: false; error: string } {
  const id = String(versionId ?? '').trim()
  if (!id || id !== id.replace(/[^\w.-]/g, '') || id.includes('..')) {
    return { ok: false, error: 'Invalid version id.' }
  }
  const pid = sanitizeVanillaFolderSegment(profileId)
  const allowed = new Set(listVanillaClientVersionIds(userData, pid))
  if (!allowed.has(id)) {
    return { ok: false, error: 'Version folder not found.' }
  }
  const verDir = getVanillaClientVersionsDir(userData)
  const target = join(verDir, pid)
  if (!existsSync(target) || !statSync(target).isDirectory()) {
    return { ok: false, error: 'Version folder not found.' }
  }
  try {
    rmSync(target, { recursive: true, force: true })
    return { ok: true }
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    return { ok: false, error: msg }
  }
}

export function backupVanillaSaves(userData: string, profileId: string): { ok: true; zipPath: string } | { ok: false; error: string } {
  const gameDir = getVanillaProfileGameDir(userData)
  const saves = join(gameDir, 'saves')
  if (!existsSync(saves)) {
    mkdirSync(saves, { recursive: true })
  }
  const backups = vanillaBackupsDir(userData, profileId)
  mkdirSync(backups, { recursive: true })
  const stamp = new Date().toISOString().replace(/[:.]/g, '-')
  const zipPath = join(backups, `saves-${stamp}.zip`)
  try {
    const zip = new AdmZip()
    const hasWorlds = existsSync(saves) && readdirSync(saves).length > 0
    if (!hasWorlds) {
      zip.addFile(
        'readme.txt',
        Buffer.from('Aucun monde dans saves/ au moment de la sauvegarde.\n', 'utf8')
      )
    } else {
      zip.addLocalFolder(saves, 'saves')
    }
    zip.writeZip(zipPath)
    return { ok: true, zipPath }
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    return { ok: false, error: msg }
  }
}
