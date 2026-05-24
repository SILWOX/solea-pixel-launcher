import { existsSync, readdirSync } from 'fs'
import { join } from 'path'
import AdmZip from 'adm-zip'

const NEOFORGE_VERSIONS_URL =
  'https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge'
const VERSIONS_TTL_MS = 60 * 60 * 1000

let versionsCache: { at: number; versions: string[] } | null = null

export function compareLoaderBuild(a: string, b: string): number {
  const pa = a
    .split(/[.+_-]/)
    .map((x) => parseInt(x, 10))
    .filter((n) => !Number.isNaN(n))
  const pb = b
    .split(/[.+_-]/)
    .map((x) => parseInt(x, 10))
    .filter((n) => !Number.isNaN(n))
  const len = Math.max(pa.length, pb.length)
  for (let i = 0; i < len; i++) {
    const d = (pa[i] ?? 0) - (pb[i] ?? 0)
    if (d !== 0) return d
  }
  return a.localeCompare(b)
}

/** Lit la version NeoForge minimale exigée dans neoforge.mods.toml (ex. `[21.1.227,)`). */
export function readNeoForgeMinFromModJar(jarPath: string): string | null {
  try {
    const zip = new AdmZip(jarPath)
    for (const e of zip.getEntries()) {
      if (!/neoforge\.mods\.toml$/i.test(e.entryName.replace(/\\/g, '/'))) continue
      const toml = e.getData().toString('utf8')
      const blocks = toml.split(/\[\[dependencies\./)
      for (const block of blocks) {
        if (!/modId\s*=\s*"neoforge"/i.test(block)) continue
        const rangeMatch = block.match(/versionRange\s*=\s*"(?:\[|\()([^,\]\)]+)/i)
        if (rangeMatch?.[1]?.trim()) return rangeMatch[1].trim()
      }
    }
  } catch {
    /* ignore */
  }
  return null
}

/** Version minimale exigée par les mods déjà présents dans mods/. */
export function scanNeoForgeMinFromInstanceMods(instanceRoot: string): string | null {
  const modsDir = join(instanceRoot, 'mods')
  if (!existsSync(modsDir)) return null
  let best: string | null = null
  for (const name of readdirSync(modsDir)) {
    if (!name.toLowerCase().endsWith('.jar')) continue
    const min = readNeoForgeMinFromModJar(join(modsDir, name))
    if (!min) continue
    if (!best || compareLoaderBuild(min, best) > 0) best = min
  }
  return best
}

export async function fetchNeoForgeReleaseVersions(): Promise<string[]> {
  if (versionsCache && Date.now() - versionsCache.at < VERSIONS_TTL_MS) {
    return versionsCache.versions
  }
  const res = await fetch(NEOFORGE_VERSIONS_URL, { signal: AbortSignal.timeout(20_000) })
  if (!res.ok) throw new Error(`NeoForge Maven (${res.status})`)
  const data = (await res.json()) as { versions?: string[] }
  const versions = Array.isArray(data.versions) ? data.versions : []
  versionsCache = { at: Date.now(), versions }
  return versions
}

/**
 * Dernière release NeoForge sur la même branche que minBuild (ex. 21.1.219 → 21.1.230),
 * ou minBuild si Maven est injoignable.
 */
export async function resolveLatestNeoForgeBuild(minBuild: string): Promise<string> {
  const min = minBuild.trim()
  if (!min || min === 'latest') {
    try {
      const versions = await fetchNeoForgeReleaseVersions()
      const line = versions.filter((v) => /^21\.1\.\d+$/.test(v)).sort(compareLoaderBuild)
      return line[line.length - 1] ?? min
    } catch {
      return min || 'latest'
    }
  }

  try {
    const versions = await fetchNeoForgeReleaseVersions()
    const parts = min.split('.')
    const prefix = parts.length >= 2 ? `${parts[0]}.${parts[1]}.` : `${min.split('.')[0]}.`
    const candidates = versions.filter(
      (v) => v.startsWith(prefix) && compareLoaderBuild(v, min) >= 0
    )
    if (!candidates.length) return min
    return candidates.sort(compareLoaderBuild)[candidates.length - 1]!
  } catch {
    return min
  }
}

function maxLoaderBuild(a: string, b: string): string {
  return compareLoaderBuild(a, b) >= 0 ? a : b
}

/** Combine index / meta + mods installés, puis résout la dernière release compatible. */
export async function resolveNeoForgeBuildForInstance(
  instanceRoot: string,
  declaredBuild?: string
): Promise<string> {
  let min = declaredBuild?.trim() || '21.1.0'
  const fromMods = scanNeoForgeMinFromInstanceMods(instanceRoot)
  if (fromMods) min = maxLoaderBuild(min, fromMods)
  return resolveLatestNeoForgeBuild(min)
}
