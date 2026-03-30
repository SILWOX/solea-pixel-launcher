import { createHash } from 'crypto'
import {
  mkdirSync,
  readFileSync,
  readdirSync,
  rmSync,
  statSync,
  writeFileSync,
  existsSync
} from 'fs'
import { dirname, join, normalize, sep } from 'path'
import AdmZip from 'adm-zip'
import { createReadStream } from 'fs'

const API = 'https://api.modrinth.com/v2'

const VERSION_LIST_TTL_MS = 5 * 60 * 1000
const versionListCache = new Map<string, { at: number; data: ModrinthVersion[] }>()

export interface ModrinthVersionFile {
  url: string
  filename: string
  primary?: boolean
}

export interface ModrinthVersion {
  id: string
  version_number: string
  date_published: string
  game_versions: string[]
  loaders: string[]
  files: ModrinthVersionFile[]
}

export interface MrpackIndexFile {
  path: string
  hashes: { sha512?: string; sha1?: string }
  downloads: string[]
  env?: { client?: 'required' | 'optional' | 'unsupported'; server?: string }
  fileSize?: number
}

export interface MrpackIndex {
  formatVersion: number
  name?: string
  versionId?: string
  files: MrpackIndexFile[]
  dependencies: Record<string, string>
}

export interface IntegrityLock {
  versionId: string
  versionNumber: string
  gameVersion: string
  loader: string
  files: { path: string; sha512: string }[]
  modJarPaths: string[]
  generatedAt: string
}

function shouldInstallForClient(entry: MrpackIndexFile): boolean {
  const c = entry.env?.client
  if (c === 'unsupported') return false
  return true
}

async function downloadToFile(url: string, dest: string, expectedSha512?: string): Promise<void> {
  mkdirSync(dirname(dest), { recursive: true })
  if (expectedSha512 && existsSync(dest)) {
    const ok = await verifySha512(dest, expectedSha512)
    if (ok) return
  }
  const res = await fetch(url)
  if (!res.ok) throw new Error(`Téléchargement échoué ${res.status}: ${url}`)
  const buf = Buffer.from(await res.arrayBuffer())
  writeFileSync(dest, buf)
}

async function runWithConcurrency<T>(items: T[], limit: number, fn: (item: T) => Promise<void>): Promise<void> {
  const n = items.length
  if (n === 0) return
  let idx = 0
  async function worker(): Promise<void> {
    while (true) {
      const i = idx++
      if (i >= n) return
      await fn(items[i]!)
    }
  }
  const workers = Math.min(Math.max(1, limit), n)
  await Promise.all(Array.from({ length: workers }, () => worker()))
}

async function verifySha512(filePath: string, expected: string): Promise<boolean> {
  const hash = createHash('sha512')
  const stream = createReadStream(filePath)
  for await (const chunk of stream) hash.update(chunk)
  const digest = hash.digest('hex')
  return digest.toLowerCase() === expected.toLowerCase()
}

export async function fetchProjectVersions(projectSlug: string): Promise<ModrinthVersion[]> {
  const now = Date.now()
  const hit = versionListCache.get(projectSlug)
  if (hit && now - hit.at < VERSION_LIST_TTL_MS) return hit.data

  const res = await fetch(`${API}/project/${projectSlug}/version`)
  if (!res.ok) throw new Error(`Modrinth API: ${res.status} pour le projet « ${projectSlug} »`)
  const data = (await res.json()) as ModrinthVersion[]
  versionListCache.set(projectSlug, { at: now, data })
  return data
}

export function pickLatestVersion(
  versions: ModrinthVersion[],
  gameVersion: string,
  loader: string
): ModrinthVersion | undefined {
  const filtered = versions.filter(
    (v) => v.game_versions.includes(gameVersion) && v.loaders.includes(loader)
  )
  filtered.sort((a, b) => new Date(b.date_published).getTime() - new Date(a.date_published).getTime())
  return filtered[0]
}

function getPrimaryMrpackFile(v: ModrinthVersion): ModrinthVersionFile | undefined {
  const primary = v.files.find((f) => f.primary)
  return primary ?? v.files.find((f) => f.filename.endsWith('.mrpack'))
}

export type InstallProgress = { phase: string; current: number; total: number; detail?: string }

export async function installMrpackFromModrinth(options: {
  projectSlug: string
  gameVersion: string
  loader: string
  instanceRoot: string
  /** Fichiers du pack en parallèle (défaut 6). */
  downloadConcurrency?: number
  onProgress?: (p: InstallProgress) => void
}): Promise<{ version: ModrinthVersion; index: MrpackIndex }> {
  const { projectSlug, gameVersion, loader, instanceRoot, onProgress } = options
  const downloadConcurrency = Math.min(32, Math.max(1, options.downloadConcurrency ?? 6))
  onProgress?.({ phase: 'versions', current: 0, total: 1, detail: 'Récupération des versions…' })
  const versions = await fetchProjectVersions(projectSlug)
  const version = pickLatestVersion(versions, gameVersion, loader)
  if (!version) {
    throw new Error(
      `Aucune version Modrinth pour ${gameVersion} + ${loader}. Vérifiez le projet « ${projectSlug} ».`
    )
  }
  const packFile = getPrimaryMrpackFile(version)
  if (!packFile?.url) throw new Error('Aucun fichier .mrpack principal sur cette version.')

  onProgress?.({ phase: 'mrpack', current: 0, total: 1, detail: 'Téléchargement du pack…' })
  const mrpackBuf = Buffer.from(await (await fetch(packFile.url)).arrayBuffer())
  const zip = new AdmZip(mrpackBuf)
  const indexEntry = zip.getEntry('modrinth.index.json')
  if (!indexEntry) throw new Error('modrinth.index.json manquant dans le .mrpack')
  const index = JSON.parse(indexEntry.getData().toString('utf8')) as MrpackIndex
  if (!index.files?.length) throw new Error('Index du pack vide.')

  mkdirSync(instanceRoot, { recursive: true })

  const toInstall = index.files.filter(shouldInstallForClient)
  const total = toInstall.length
  let completed = 0
  await runWithConcurrency(toInstall, downloadConcurrency, async (f) => {
    const sha512 = f.hashes?.sha512
    if (!sha512 || !f.downloads?.[0]) {
      throw new Error(`Entrée invalide dans le pack: ${f.path}`)
    }
    const rel = f.path.replace(/\//g, sep)
    const dest = join(instanceRoot, rel)
    await downloadToFile(f.downloads[0], dest, sha512)
    const ok = await verifySha512(dest, sha512)
    if (!ok) {
      rmSync(dest, { force: true })
      throw new Error(`Hash SHA-512 incorrect pour ${f.path}`)
    }
    completed++
    onProgress?.({
      phase: 'files',
      current: completed,
      total,
      detail: f.path
    })
  })

  const overridesPrefix = 'overrides/'
  for (const e of zip.getEntries()) {
    if (e.isDirectory) continue
    const name = e.entryName.replace(/\\/g, '/')
    if (!name.startsWith(overridesPrefix)) continue
    const rel = name.slice(overridesPrefix.length)
    const out = join(instanceRoot, rel.split('/').join(sep))
    mkdirSync(dirname(out), { recursive: true })
    writeFileSync(out, e.getData())
  }

  const lockFiles: { path: string; sha512: string }[] = []
  const modJarPaths: string[] = []
  for (const f of toInstall) {
    const sha512 = f.hashes.sha512!
    const norm = f.path.replace(/\\/g, '/')
    lockFiles.push({ path: norm, sha512 })
    if (norm.startsWith('mods/') && norm.endsWith('.jar')) modJarPaths.push(norm)
  }

  const lock: IntegrityLock = {
    versionId: version.id,
    versionNumber: version.version_number,
    gameVersion,
    loader,
    files: lockFiles,
    modJarPaths,
    generatedAt: new Date().toISOString()
  }
  writeFileSync(join(instanceRoot, '.solea-integrity.json'), JSON.stringify(lock, null, 2), 'utf8')
  const neoForgeVersion = index.dependencies?.neoforge
  const forgeVersion = index.dependencies?.forge
  let loaderType: 'neoforge' | 'forge'
  let loaderBuild: string
  if (neoForgeVersion) {
    loaderType = 'neoforge'
    loaderBuild = neoForgeVersion
  } else if (forgeVersion) {
    loaderType = 'forge'
    /** Modrinth donne souvent « 47.4.10 » ; minecraft-java-core attend « 1.20.1-47.4.10 ». */
    loaderBuild =
      forgeVersion.includes('-') && forgeVersion.split('-').length >= 2
        ? forgeVersion
        : `${gameVersion}-${forgeVersion}`
  } else {
    throw new Error(
      'Le pack ne déclare ni NeoForge (dependencies.neoforge) ni Forge (dependencies.forge). Impossible de lancer.'
    )
  }

  writeFileSync(
    join(instanceRoot, '.solea-installed.json'),
    JSON.stringify(
      {
        projectSlug,
        versionId: version.id,
        versionNumber: version.version_number,
        gameVersion,
        loader,
        loaderType,
        loaderBuild,
        neoForgeVersion: loaderType === 'neoforge' ? loaderBuild : undefined
      },
      null,
      2
    ),
    'utf8'
  )

  return { version, index }
}

export function loadIntegrityLock(instanceRoot: string): IntegrityLock | null {
  const p = join(instanceRoot, '.solea-integrity.json')
  if (!existsSync(p)) return null
  return JSON.parse(readFileSync(p, 'utf8')) as IntegrityLock
}

export type IntegrityResult =
  | { ok: true }
  | {
      ok: false
      reason: 'no_lock' | 'hash_mismatch' | 'extra_mod' | 'missing_file' | 'read_error'
      detail?: string
      paths?: string[]
    }

/** Chemins relatifs (normalisés /) générés au runtime, absents du mrpack — ne pas compter comme mods « en trop ». */
const IGNORED_EXTRA_MOD_REL_PREFIXES = ['mods/.connector/']

function isIgnoredRuntimeModJar(rel: string): boolean {
  const n = rel.replace(/\\/g, '/').toLowerCase()
  return IGNORED_EXTRA_MOD_REL_PREFIXES.some((p) => n.startsWith(p))
}

export async function verifyInstanceIntegrity(instanceRoot: string): Promise<IntegrityResult> {
  const lock = loadIntegrityLock(instanceRoot)
  if (!lock) return { ok: false, reason: 'no_lock', detail: 'Installez le modpack avant de lancer.' }

  for (const { path: rel, sha512 } of lock.files) {
    const filePath = join(instanceRoot, rel.split('/').join(sep))
    if (!existsSync(filePath)) {
      return { ok: false, reason: 'missing_file', detail: rel }
    }
    const ok = await verifySha512(filePath, sha512)
    if (!ok) return { ok: false, reason: 'hash_mismatch', detail: rel }
  }

  const allowedModJars = new Set(lock.modJarPaths.map((p) => normalize(p).replace(/\\/g, '/').toLowerCase()))
  const modsDir = join(instanceRoot, 'mods')
  if (existsSync(modsDir)) {
    const extras: string[] = []
    const walkMods = (dir: string, sub: string) => {
      for (const name of readdirSync(dir)) {
        const full = join(dir, name)
        if (statSync(full).isDirectory()) walkMods(full, sub ? `${sub}/${name}` : name)
        else if (name.endsWith('.jar')) {
          const rel = `mods/${sub ? `${sub}/` : ''}${name}`.replace(/\\/g, '/')
          if (!allowedModJars.has(rel.toLowerCase()) && !isIgnoredRuntimeModJar(rel)) extras.push(rel)
        }
      }
    }
    try {
      walkMods(modsDir, '')
    } catch (e) {
      return { ok: false, reason: 'read_error', detail: String(e) }
    }
    if (extras.length) return { ok: false, reason: 'extra_mod', paths: extras }
  }

  return { ok: true }
}

export type ModpackActionInfo = {
  needsInstall: boolean
  needsUpdate: boolean
  installedVersionNumber?: string
  latestVersionNumber?: string
  error?: string
}

export async function getModpackActionInfo(options: {
  instanceRoot: string
  projectSlug: string
  gameVersion: string
  loader: string
}): Promise<ModpackActionInfo> {
  const { instanceRoot, projectSlug, gameVersion, loader } = options
  const installedPath = join(instanceRoot, '.solea-installed.json')

  const readLatestNumber = async (): Promise<string | undefined> => {
    try {
      const versions = await fetchProjectVersions(projectSlug)
      const latest = pickLatestVersion(versions, gameVersion, loader)
      return latest?.version_number
    } catch {
      return undefined
    }
  }

  if (!existsSync(installedPath)) {
    return { needsInstall: true, needsUpdate: false, latestVersionNumber: await readLatestNumber() }
  }

  try {
    const installed = JSON.parse(readFileSync(installedPath, 'utf8')) as {
      versionId?: string
      versionNumber?: string
    }
    if (!installed.versionId) {
      return { needsInstall: true, needsUpdate: false, latestVersionNumber: await readLatestNumber() }
    }

    const versions = await fetchProjectVersions(projectSlug)
    const latest = pickLatestVersion(versions, gameVersion, loader)
    if (!latest) {
      return {
        needsInstall: false,
        needsUpdate: false,
        installedVersionNumber: installed.versionNumber,
        error: 'Aucune version compatible sur Modrinth.'
      }
    }

    return {
      needsInstall: false,
      needsUpdate: latest.id !== installed.versionId,
      installedVersionNumber: installed.versionNumber,
      latestVersionNumber: latest.version_number
    }
  } catch (e) {
    return {
      needsInstall: true,
      needsUpdate: false,
      error: e instanceof Error ? e.message : String(e)
    }
  }
}
