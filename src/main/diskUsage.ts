import { existsSync, readdirSync, statSync, rmSync } from 'fs'
import { join } from 'path'

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
