import { cpSync, existsSync, mkdirSync, readdirSync, renameSync, rmSync, statSync } from 'fs'
import { join, resolve, dirname } from 'path'
import { app } from 'electron'
import { getModpackSpec, MODPACKS, resolveModpackId, type ModpackId } from './modpacks.js'
import type { LauncherSettings } from './settings.js'

/** Dossiers d’instances par défaut sous userData (userData = déjà …/Roaming/.soleapixel). */
export function getDefaultInstanceRootForModpack(modpackId: ModpackId): string {
  const spec = getModpackSpec(modpackId)
  return join(app.getPath('userData'), 'instances', spec.projectSlug)
}

/** Instance sous un dossier parent choisi : {parent}/.soleapixel/instances/{slug}. */
export function getCustomInstanceRootForModpack(modpackId: ModpackId, parentPath: string): string {
  const spec = getModpackSpec(modpackId)
  return join(resolve(parentPath.trim()), '.soleapixel', 'instances', spec.projectSlug)
}

export function resolveInstanceRootForModpack(modpackId: ModpackId, settings: LauncherSettings): string {
  const id = resolveModpackId(modpackId)
  const custom = settings.modpackInstanceParentPath?.[id]?.trim()
  if (custom) return getCustomInstanceRootForModpack(id, custom)
  return getDefaultInstanceRootForModpack(id)
}

/**
 * Anciennes installations hors userData : `.soleapixel/instance/` → `.soleapixel/instances/`.
 */
export function migrateExternalSoleapixelInstanceFolderToInstances(settings: LauncherSettings): void {
  const seen = new Set<string>()
  for (const m of MODPACKS) {
    const raw = settings.modpackInstanceParentPath?.[m.id]?.trim()
    if (!raw) continue
    const r = resolve(raw)
    if (seen.has(r)) continue
    seen.add(r)
    const legacyBase = join(r, '.soleapixel', 'instance')
    const nextBase = join(r, '.soleapixel', 'instances')
    if (!existsSync(legacyBase) || !statSync(legacyBase).isDirectory()) continue
    if (!existsSync(nextBase)) {
      try {
        renameSync(legacyBase, nextBase)
      } catch {
        /* best-effort */
      }
      continue
    }
    try {
      for (const name of readdirSync(legacyBase)) {
        const from = join(legacyBase, name)
        const to = join(nextBase, name)
        if (existsSync(to)) continue
        renameSync(from, to)
      }
    } catch {
      /* ignore */
    }
  }
}

/** Renomme l’ancien dossier au singulier (`instance`) vers `instances` si besoin (sous userData). */
export function migrateLegacyInstanceLayout(): void {
  const ud = app.getPath('userData')
  const singular = join(ud, 'instance')
  const plural = join(ud, 'instances')
  if (!existsSync(singular)) return
  if (!existsSync(plural)) {
    try {
      renameSync(singular, plural)
    } catch {
      /* best-effort */
    }
    return
  }
  try {
    for (const name of readdirSync(singular)) {
      const from = join(singular, name)
      const to = join(plural, name)
      if (existsSync(to)) continue
      renameSync(from, to)
    }
  } catch {
    /* ignore */
  }
}

function getDriveRootForPath(p: string): string {
  const r = resolve(p)
  if (process.platform === 'win32') {
    const m = /^([a-zA-Z]:)([\\/]|$)/.exec(r)
    if (m) return m[1]!.toUpperCase() + '\\'
  }
  return '/'
}

/** Racines `.soleapixel` trouvées sous un lecteur (profondeur limitée). */
export function discoverSoleapixelRootsOnDrive(driveRoot: string): string[] {
  const roots: string[] = []
  const root = driveRoot.endsWith('\\') || driveRoot === '/' ? driveRoot : driveRoot + '\\'
  const visit = (dir: string, depth: number) => {
    if (depth > 2) return
    let sp: string
    try {
      sp = join(dir, '.soleapixel')
      if (existsSync(sp) && statSync(sp).isDirectory()) {
        roots.push(resolve(sp))
      }
    } catch {
      return
    }
    if (depth >= 2) return
    try {
      const entries = readdirSync(dir, { withFileTypes: true })
      for (const e of entries) {
        if (!e.isDirectory()) continue
        const n = e.name
        if (n === 'System Volume Information' || n === '$Recycle.Bin') continue
        visit(join(dir, n), depth + 1)
      }
    } catch {
      /* ignore */
    }
  }
  try {
    visit(root, 0)
  } catch {
    /* ignore */
  }
  return roots
}

function getPathDeviceId(p: string): number | bigint {
  try {
    return statSync(p).dev
  } catch {
    return -1
  }
}

/**
 * Un seul emplacement parent par « disque » (identifiant de volume) pour les instances hors userData,
 * et au plus une arborescence `.soleapixel` par lecteur (hors dossier du launcher).
 */
export function validateModpackInstanceParentPaths(
  next: LauncherSettings
): { ok: true } | { ok: false; error: string } {
  const map = next.modpackInstanceParentPath ?? {}
  const ud = app.getPath('userData')
  const udResolved = resolve(ud)

  const parents: { id: string; path: string; resolved: string; dev: number | bigint }[] = []
  for (const m of MODPACKS) {
    const raw = map[m.id]?.trim()
    if (!raw) continue
    const r = resolve(raw)
    if (!existsSync(r) || !statSync(r).isDirectory()) {
      return { ok: false, error: `Dossier parent invalide ou inaccessible (${m.displayName}).` }
    }
    parents.push({ id: m.id, path: raw, resolved: r, dev: getPathDeviceId(r) })
  }

  for (let i = 0; i < parents.length; i++) {
    for (let j = i + 1; j < parents.length; j++) {
      const a = parents[i]!
      const b = parents[j]!
      if (a.dev === b.dev && a.dev !== -1 && a.resolved !== b.resolved) {
        return {
          ok: false,
          error:
            'Un seul dossier parent par disque pour les instances personnalisées. Choisissez le même emplacement pour tous les modpacks sur ce volume, ou laissez le défaut.'
        }
      }
    }
  }

  if (process.platform === 'win32') {
    const seenDrives = new Set<string>()
    for (const p of parents) {
      const drive = getDriveRootForPath(p.resolved)
      seenDrives.add(drive)
    }
    for (const drive of seenDrives) {
      const expectedRoots = new Set<string>()
      for (const p of parents) {
        if (getDriveRootForPath(p.resolved) !== drive) continue
        expectedRoots.add(resolve(join(p.resolved, '.soleapixel')))
      }
      const found = discoverSoleapixelRootsOnDrive(drive)
      for (const f of found) {
        const fr = resolve(f)
        if (fr === resolve(ud)) continue
        if (expectedRoots.has(fr)) continue
        return {
          ok: false,
          error:
            `Une autre installation Solea (.soleapixel) existe déjà sur ce lecteur (${drive}). Un seul dossier .soleapixel par disque est autorisé pour éviter les conflits.`
        }
      }
    }
  }

  return { ok: true }
}

export function moveInstanceFolderBestEffort(src: string, dest: string): { ok: true } | { ok: false; error: string } {
  if (!existsSync(src)) return { ok: true }
  if (existsSync(dest)) {
    return { ok: false, error: 'Le dossier de destination existe déjà.' }
  }
  try {
    mkdirSync(dirname(dest), { recursive: true })
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    return { ok: false, error: msg }
  }
  try {
    const devSrc = statSync(src).dev
    const devDest = statSync(dirname(dest)).dev
    if (devSrc === devDest) {
      renameSync(src, dest)
    } else {
      cpSync(src, dest, { recursive: true })
      rmSync(src, { recursive: true, force: true })
    }
    return { ok: true }
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    return { ok: false, error: msg }
  }
}
