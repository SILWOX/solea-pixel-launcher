import { existsSync, readFileSync, writeFileSync, mkdirSync } from 'fs'
import { join } from 'path'
import { app } from 'electron'

export type LauncherMeta = {
  /** Dernière version connue (détection install / mise à jour). */
  knownVersion?: string
  /**
   * Dernière fois où le launcher a été considéré comme installé ou mis à jour (changement de version).
   * `null` si l’utilisateur a effacé la mémoire.
   */
  lastLauncherUpdatedAt?: string | null
}

function metaPath(): string {
  return join(app.getPath('userData'), 'launcher-meta.json')
}

function readMeta(): LauncherMeta {
  const p = metaPath()
  if (!existsSync(p)) return {}
  try {
    const raw = JSON.parse(readFileSync(p, 'utf8')) as unknown
    return raw && typeof raw === 'object' ? (raw as LauncherMeta) : {}
  } catch {
    return {}
  }
}

function writeMeta(data: LauncherMeta): void {
  mkdirSync(app.getPath('userData'), { recursive: true })
  writeFileSync(metaPath(), JSON.stringify(data, null, 2), 'utf8')
}

export function getLauncherMeta(): LauncherMeta {
  return readMeta()
}

/**
 * Au démarrage : première exécution ou nouvelle version → enregistre la date de « dernière mise à jour » launcher.
 */
export function syncLauncherMetaOnStartup(): void {
  const cur = app.getVersion()
  const m = readMeta()
  if (!m.knownVersion) {
    writeMeta({ knownVersion: cur, lastLauncherUpdatedAt: new Date().toISOString() })
    return
  }
  if (m.knownVersion !== cur) {
    writeMeta({
      ...m,
      knownVersion: cur,
      lastLauncherUpdatedAt: new Date().toISOString()
    })
  }
}

export function clearLauncherUpdatedMemory(): void {
  const m = readMeta()
  writeMeta({
    ...m,
    lastLauncherUpdatedAt: null
  })
}
