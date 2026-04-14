import { existsSync, readdirSync, statSync, rmSync, rmdirSync, unlinkSync } from 'fs'
import { basename, join } from 'path'

export function directorySizeSync(root: string): number {
  if (!existsSync(root)) return 0
  let total = 0
  const walk = (p: string): void => {
    let st: ReturnType<typeof statSync>
    try {
      st = statSync(p, { throwIfNoEntry: false })
    } catch {
      return
    }
    if (!st) return
    if (st.isFile()) total += st.size
    else if (st.isDirectory()) {
      let names: string[]
      try {
        names = readdirSync(p)
      } catch {
        return
      }
      for (const n of names) walk(join(p, n))
    }
  }
  walk(root)
  return total
}

/**
 * Même résultat que {@link directorySizeSync}, mais cède la boucle Node régulièrement
 * pour ne pas figer l’UI Electron pendant les gros dossiers (Gradle, instance modpack).
 */
export async function directorySizeAsync(root: string): Promise<number> {
  if (!existsSync(root)) return 0
  let total = 0
  let filesSinceYield = 0
  const YIELD_EVERY = 200

  async function walk(p: string): Promise<void> {
    let st: ReturnType<typeof statSync>
    try {
      st = statSync(p, { throwIfNoEntry: false })
    } catch {
      return
    }
    if (!st) return
    if (st.isFile()) {
      total += st.size
      filesSinceYield++
      if (filesSinceYield >= YIELD_EVERY) {
        filesSinceYield = 0
        await new Promise<void>((resolve) => setImmediate(resolve))
      }
      return
    }
    if (!st.isDirectory()) return
    let names: string[]
    try {
      names = readdirSync(p)
    } catch {
      return
    }
    for (const n of names) {
      await walk(join(p, n))
    }
  }

  await walk(root)
  return total
}

function rmPathBestEffort(p: string): { freedBytes: number; hadFailures: boolean } {
  let st: ReturnType<typeof statSync>
  try {
    st = statSync(p, { throwIfNoEntry: false })
  } catch {
    return { freedBytes: 0, hadFailures: true }
  }
  if (!st) return { freedBytes: 0, hadFailures: false }

  if (st.isDirectory()) {
    let freedBytes = 0
    let hadFailures = false
    let names: string[]
    try {
      names = readdirSync(p)
    } catch {
      return { freedBytes: 0, hadFailures: true }
    }
    for (const n of names) {
      const sub = rmPathBestEffort(join(p, n))
      freedBytes += sub.freedBytes
      if (sub.hadFailures) hadFailures = true
    }
    try {
      rmdirSync(p)
    } catch {
      hadFailures = true
    }
    return { freedBytes, hadFailures }
  }

  try {
    const size = st.size
    rmSync(p, { force: true })
    return { freedBytes: size, hadFailures: false }
  } catch {
    return { freedBytes: 0, hadFailures: true }
  }
}

/**
 * Supprime le contenu d’un dossier sans faire échouer toute l’opération si certains fichiers
 * sont verrouillés (ex. cache Chromium pendant que l’app Electron tourne — EPERM sur Windows).
 */
export function rmDirContentsBestEffort(dir: string): { freedBytes: number; hadFailures: boolean } {
  if (!existsSync(dir)) return { freedBytes: 0, hadFailures: false }
  let freedBytes = 0
  let hadFailures = false
  let names: string[]
  try {
    names = readdirSync(dir)
  } catch {
    return { freedBytes: 0, hadFailures: true }
  }
  for (const name of names) {
    const sub = rmPathBestEffort(join(dir, name))
    freedBytes += sub.freedBytes
    if (sub.hadFailures) hadFailures = true
  }
  return { freedBytes, hadFailures }
}

export function rmDirContentsIfExists(dir: string): { ok: true; freedBytes: number } | { ok: false; error: string } {
  if (!existsSync(dir)) return { ok: true, freedBytes: 0 }
  const before = directorySizeSync(dir)
  try {
    for (const name of readdirSync(dir)) {
      const p = join(dir, name)
      rmSync(p, { recursive: true, force: true })
    }
    return { ok: true, freedBytes: before }
  } catch (e) {
    return { ok: false, error: e instanceof Error ? e.message : String(e) }
  }
}

const RM_YIELD_SCAN = 200
const RM_YIELD_DELETE = 80

export type RmTreeProgressPhase = 'scan' | 'delete'

/**
 * Supprime un fichier ou un dossier (récursif) en cédant la boucle Node régulièrement
 * pour ne pas figer le processus principal Electron (désinstallation d’instance).
 */
export async function rmTreeRecursiveWithProgress(
  root: string,
  onProgress: (current: number, total: number, detail: string, phase: RmTreeProgressPhase) => void
): Promise<void> {
  if (!existsSync(root)) return
  const toDelete: string[] = []
  let scanCount = 0

  async function walk(p: string): Promise<void> {
    let st: ReturnType<typeof statSync>
    try {
      st = statSync(p, { throwIfNoEntry: false })
    } catch {
      return
    }
    if (!st) return
    if (st.isFile()) {
      toDelete.push(p)
      scanCount++
      if (scanCount % RM_YIELD_SCAN === 0) {
        onProgress(scanCount, 0, '__scan__', 'scan')
        await new Promise<void>((r) => setImmediate(r))
      }
      return
    }
    if (st.isDirectory()) {
      let names: string[]
      try {
        names = readdirSync(p)
      } catch {
        return
      }
      for (const n of names) {
        await walk(join(p, n))
      }
      toDelete.push(p)
      scanCount++
      if (scanCount % RM_YIELD_SCAN === 0) {
        onProgress(scanCount, 0, '__scan__', 'scan')
        await new Promise<void>((r) => setImmediate(r))
      }
    }
  }

  await walk(root)
  const total = toDelete.length
  for (let i = 0; i < toDelete.length; i++) {
    const p = toDelete[i]!
    onProgress(i + 1, total, basename(p), 'delete')
    try {
      const st = statSync(p, { throwIfNoEntry: false })
      if (!st) continue
      if (st.isFile()) unlinkSync(p)
      else {
        try {
          rmdirSync(p)
        } catch {
          rmSync(p, { recursive: true, force: true })
        }
      }
    } catch {
      try {
        rmSync(p, { recursive: true, force: true })
      } catch {
        /* ignore */
      }
    }
    if (i % RM_YIELD_DELETE === 0 && i > 0) {
      await new Promise<void>((r) => setImmediate(r))
    }
  }
}
