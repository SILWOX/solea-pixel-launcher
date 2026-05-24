import { createHash } from 'crypto'
import {
  mkdirSync,
  readFileSync,
  readdirSync,
  renameSync,
  rmSync,
  statSync,
  writeFileSync,
  existsSync,
  createWriteStream
} from 'fs'
import { dirname, join, normalize, sep } from 'path'
import { availableParallelism, cpus } from 'node:os'
import { pipeline } from 'node:stream/promises'
import { Readable } from 'node:stream'
import AdmZip from 'adm-zip'
import { createReadStream } from 'fs'
import {
  compareLoaderBuild,
  readNeoForgeMinFromModJar,
  resolveLatestNeoForgeBuild,
  resolveNeoForgeBuildForInstance
} from './neoForgeResolve.js'

const API = 'https://api.modrinth.com/v2'

const VERSION_LIST_TTL_MS = 5 * 60 * 1000
const versionListCache = new Map<string, { at: number; data: ModrinthVersion[] }>()

export interface ModrinthVersionFile {
  url: string
  filename: string
  primary?: boolean
  hashes?: { sha512?: string; sha1?: string }
}

export interface ModrinthVersionDependency {
  version_id: string | null
  project_id: string
  file_name: string | null
  dependency_type: string
}

export interface ModrinthVersion {
  id: string
  version_number: string
  date_published: string
  game_versions: string[]
  loaders: string[]
  files: ModrinthVersionFile[]
  dependencies?: ModrinthVersionDependency[]
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
  dependencies?: Record<string, string>
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

/** Fichiers dédiés serveur dans le .mrpack (exclut ceux marqués server: unsupported). */
function shouldInstallForServer(entry: MrpackIndexFile): boolean {
  const s = entry.env?.server
  if (s === 'unsupported') return false
  return true
}

/** Parallélisme SHA-512 sur les fichiers du lock (SSD / plusieurs cœurs). */
function integrityVerifyConcurrency(): number {
  const n =
    typeof availableParallelism === 'function' ? availableParallelism() : Math.max(1, cpus().length)
  return Math.min(48, Math.max(4, n * 2))
}

async function downloadToFile(url: string, dest: string, expectedSha512?: string): Promise<void> {
  mkdirSync(dirname(dest), { recursive: true })
  if (expectedSha512 && existsSync(dest)) {
    const ok = await verifySha512(dest, expectedSha512)
    if (ok) return
  }
  const res = await fetch(url)
  if (!res.ok) throw new Error(`Téléchargement échoué ${res.status}: ${url}`)

  const cl = res.headers.get('content-length')
  const approx = cl ? parseInt(cl, 10) : NaN
  const smallFile = Number.isFinite(approx) && approx >= 0 && approx < 768 * 1024

  if (smallFile) {
    const buf = Buffer.from(await res.arrayBuffer())
    writeFileSync(dest, buf)
  } else {
    if (!res.body) throw new Error(`Réponse vide: ${url}`)
    const tmp = `${dest}.solea-dl`
    try {
      await pipeline(Readable.fromWeb(res.body as import('stream/web').ReadableStream), createWriteStream(tmp))
      renameSync(tmp, dest)
    } catch (e) {
      try {
        rmSync(tmp, { force: true })
      } catch {
        /* ignore */
      }
      throw e
    }
  }

  if (expectedSha512) {
    const ok = await verifySha512(dest, expectedSha512)
    if (!ok) {
      rmSync(dest, { force: true })
      throw new Error(`Hash SHA-512 incorrect pour le fichier téléchargé.`)
    }
  }
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

/** Filtres optionnels : l’API renvoie alors les versions compatibles (évite la pagination tronquée sans filtre). */
export type FetchProjectVersionsFilters = {
  gameVersion?: string
  loaders?: string[]
}

function fetchProjectVersionsCacheKey(projectSlug: string, filters?: FetchProjectVersionsFilters): string {
  const gv = filters?.gameVersion?.trim()
  const ld = filters?.loaders?.length
    ? [...new Set(filters.loaders.map((x) => String(x).toLowerCase()))].sort().join(',')
    : ''
  if (!gv && !ld) return projectSlug
  return `${projectSlug}::gv:${gv ?? ''}::ld:${ld}`
}

export async function fetchProjectVersions(
  projectSlug: string,
  filters?: FetchProjectVersionsFilters
): Promise<ModrinthVersion[]> {
  const now = Date.now()
  const cacheKey = fetchProjectVersionsCacheKey(projectSlug, filters)
  const hit = versionListCache.get(cacheKey)
  if (hit && now - hit.at < VERSION_LIST_TTL_MS) return hit.data

  const url = new URL(`${API}/project/${encodeURIComponent(projectSlug)}/version`)
  url.searchParams.set('limit', '100')
  const gv = filters?.gameVersion?.trim()
  if (gv) url.searchParams.set('game_versions', JSON.stringify([gv]))
  if (filters?.loaders?.length) {
    url.searchParams.set('loaders', JSON.stringify(filters.loaders.map((x) => String(x).toLowerCase())))
  }

  const res = await fetch(url.toString())
  if (!res.ok) throw new Error(`Modrinth API: ${res.status} pour le projet « ${projectSlug} »`)
  const data = (await res.json()) as ModrinthVersion[]
  versionListCache.set(cacheKey, { at: now, data })
  return data
}

/** Détail d’une version (ex. Sodium requis par Iris via `version_id`). */
export async function fetchModrinthVersionById(versionId: string): Promise<ModrinthVersion | null> {
  const res = await fetch(`${API}/version/${encodeURIComponent(versionId)}`)
  if (res.status === 404) return null
  if (!res.ok) throw new Error(`Modrinth API version: ${res.status}`)
  return (await res.json()) as ModrinthVersion
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
  const mrpack = v.files.find((f) => f.filename.toLowerCase().endsWith('.mrpack'))
  if (mrpack) return mrpack
  const primary = v.files.find((f) => f.primary)
  if (primary?.filename.toLowerCase().endsWith('.mrpack')) return primary
  return undefined
}

function getPrimaryModJarFile(v: ModrinthVersion): ModrinthVersionFile | undefined {
  const primaryJar = v.files.find((f) => f.primary && f.filename.toLowerCase().endsWith('.jar'))
  if (primaryJar) return primaryJar
  return v.files.find((f) => f.filename.toLowerCase().endsWith('.jar'))
}

function maxLoaderBuild(a: string, b: string): string {
  return compareLoaderBuild(a, b) >= 0 ? a : b
}

function resolveMrpackLoaderFromIndex(
  index: MrpackIndex,
  gameVersion: string,
  specLoader: string,
  specLoaderBuild?: string
): { loaderType: 'neoforge' | 'forge' | 'fabric'; loaderBuild: string } {
  const deps = index.dependencies ?? {}
  const fallbackBuild = specLoaderBuild?.trim()
  const loaderNorm = (specLoader || 'neoforge').toLowerCase()

  /** Modrinth Pack v1 : `fabric-loader`, parfois `fabric`. */
  const fabricVersion = deps['fabric-loader'] ?? deps.fabric
  const neoForgeVersion = deps.neoforge
  const forgeVersion = deps.forge

  const loaderType: 'neoforge' | 'forge' | 'fabric' =
    loaderNorm === 'fabric' ? 'fabric' : loaderNorm === 'forge' ? 'forge' : 'neoforge'

  if (loaderType === 'fabric') {
    return {
      loaderType: 'fabric',
      loaderBuild: fabricVersion ?? fallbackBuild ?? 'latest'
    }
  }

  if (loaderType === 'forge') {
    let loaderBuild = fallbackBuild
    if (forgeVersion) {
      loaderBuild =
        forgeVersion.includes('-') && forgeVersion.split('-').length >= 2
          ? forgeVersion
          : `${gameVersion}-${forgeVersion}`
    }
    if (!loaderBuild) {
      throw new Error(
        'Le pack ne déclare pas Forge dans modrinth.index.json et aucune version de repli n’est configurée.'
      )
    }
    return {
      loaderType: 'forge',
      loaderBuild: normalizeForgeLoaderBuild(gameVersion, 'forge', loaderBuild)
    }
  }

  const loaderBuild = neoForgeVersion ?? fallbackBuild
  if (!loaderBuild) {
    throw new Error(
      'Le pack ne déclare pas NeoForge dans modrinth.index.json et aucune version de repli n’est configurée.'
    )
  }
  return { loaderType: 'neoforge', loaderBuild }
}

function normalizeForgeLoaderBuild(
  gameVersion: string,
  loaderType: string,
  loaderBuild: string
): string {
  if (loaderType !== 'forge' || !loaderBuild) return loaderBuild
  if (loaderBuild.includes('-')) return loaderBuild
  return `${gameVersion}-${loaderBuild}`
}

function writeInstalledMeta(
  instanceRoot: string,
  meta: {
    projectSlug: string
    versionId: string
    versionNumber: string
    gameVersion: string
    loader: string
    loaderType: 'neoforge' | 'forge' | 'fabric'
    loaderBuild: string
  }
): void {
  writeFileSync(
    join(instanceRoot, '.solea-installed.json'),
    JSON.stringify(
      {
        projectSlug: meta.projectSlug,
        versionId: meta.versionId,
        versionNumber: meta.versionNumber,
        gameVersion: meta.gameVersion,
        loader: meta.loader,
        loaderType: meta.loaderType,
        loaderBuild: meta.loaderBuild,
        neoForgeVersion: meta.loaderType === 'neoforge' ? meta.loaderBuild : undefined
      },
      null,
      2
    ),
    'utf8'
  )
}

export type InstallProgress = {
  phase: string
  current: number
  total: number
  detail?: string
  task?: 'install' | 'uninstall' | 'launch'
  /** Progression téléchargement client vanilla (barre globale). */
  source?: 'vanilla'
  /** Fin du flux vanilla : libère la barre globale. */
  vanillaDone?: boolean
}

export async function installMrpackFromModrinth(options: {
  projectSlug: string
  gameVersion: string
  loader: string
  instanceRoot: string
  /** Version loader de repli si absente de modrinth.index.json (ex. mod .jar). */
  loaderBuild?: string
  /** Fichiers du pack en parallèle (défaut 6). */
  downloadConcurrency?: number
  /** Client (défaut) ou profil serveur (.mrpack env.server). Si aucun fichier serveur, repli sur le client. */
  installProfile?: 'client' | 'server'
  onProgress?: (p: InstallProgress) => void
}): Promise<{ version: ModrinthVersion; index: MrpackIndex }> {
  const { projectSlug, gameVersion, loader, instanceRoot, onProgress } = options
  const installProfile = options.installProfile ?? 'client'
  const downloadConcurrency = Math.min(48, Math.max(1, options.downloadConcurrency ?? 6))
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

  let toInstall =
    installProfile === 'server'
      ? index.files.filter(shouldInstallForServer)
      : index.files.filter(shouldInstallForClient)
  if (installProfile === 'server' && toInstall.length === 0) {
    toInstall = index.files.filter(shouldInstallForClient)
  }

  // During updates, remove files managed by the previous lock that are no longer present
  // in the new pack version. This avoids duplicate old mod jars after version bumps.
  const previousLock = loadIntegrityLock(instanceRoot)
  if (previousLock?.files?.length) {
    const nextPaths = new Set(toInstall.map((f) => f.path.replace(/\\/g, '/').toLowerCase()))
    for (const old of previousLock.files) {
      const rel = old.path.replace(/\\/g, '/')
      if (nextPaths.has(rel.toLowerCase())) continue
      const abs = join(instanceRoot, rel.split('/').join(sep))
      try {
        rmSync(abs, { force: true })
      } catch {
        /* ignore stale file cleanup errors */
      }
    }
  }
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
  let { loaderType, loaderBuild } = resolveMrpackLoaderFromIndex(
    index,
    gameVersion,
    loader,
    options.loaderBuild
  )
  if (loaderType === 'neoforge') {
    loaderBuild = await resolveNeoForgeBuildForInstance(instanceRoot, loaderBuild)
  }

  writeInstalledMeta(instanceRoot, {
    projectSlug,
    versionId: version.id,
    versionNumber: version.version_number,
    gameVersion,
    loader,
    loaderType,
    loaderBuild
  })

  return { version, index }
}

/** Installe un mod Modrinth (.jar) + loader NeoForge/Forge déclaré dans la spec. */
export async function installModJarFromModrinth(options: {
  projectSlug: string
  gameVersion: string
  loader: string
  loaderBuild?: string
  instanceRoot: string
  onProgress?: (p: InstallProgress) => void
}): Promise<{ version: ModrinthVersion }> {
  const { projectSlug, gameVersion, loader, instanceRoot, onProgress } = options
  const loaderBuild = options.loaderBuild?.trim() ?? ''
  onProgress?.({ phase: 'versions', current: 0, total: 1, detail: 'Récupération des versions…' })
  const versions = await fetchProjectVersions(projectSlug)
  const version = pickLatestVersion(versions, gameVersion, loader)
  if (!version) {
    throw new Error(
      `Aucune version Modrinth pour ${gameVersion} + ${loader}. Vérifiez le projet « ${projectSlug} ».`
    )
  }
  const modFile = getPrimaryModJarFile(version)
  if (!modFile?.url) throw new Error('Aucun fichier .jar principal sur cette version Modrinth.')

  const relPath = `mods/${modFile.filename}`
  const dest = join(instanceRoot, 'mods', modFile.filename)
  mkdirSync(dirname(dest), { recursive: true })

  const previousLock = loadIntegrityLock(instanceRoot)
  if (previousLock?.files?.length) {
    for (const old of previousLock.files) {
      const abs = join(instanceRoot, old.path.replace(/\//g, sep))
      try {
        rmSync(abs, { force: true })
      } catch {
        /* ignore */
      }
    }
  }

  onProgress?.({ phase: 'mod', current: 0, total: 1, detail: modFile.filename })
  let sha512 = modFile.hashes?.sha512
  await downloadToFile(modFile.url, dest, sha512)
  if (!sha512) {
    sha512 = createHash('sha512')
      .update(readFileSync(dest))
      .digest('hex')
  }

  const lock: IntegrityLock = {
    versionId: version.id,
    versionNumber: version.version_number,
    gameVersion,
    loader,
    files: [{ path: relPath.replace(/\\/g, '/'), sha512 }],
    modJarPaths: [relPath.replace(/\\/g, '/')],
    generatedAt: new Date().toISOString()
  }
  writeFileSync(join(instanceRoot, '.solea-integrity.json'), JSON.stringify(lock, null, 2), 'utf8')

  const loaderType = loader === 'forge' ? 'forge' : loader === 'fabric' ? 'fabric' : 'neoforge'
  let resolvedLoaderBuild = loaderBuild
  if (loaderType === 'neoforge') {
    let minBuild = loaderBuild || '21.1.0'
    const fromJar = readNeoForgeMinFromModJar(dest)
    if (fromJar) minBuild = maxLoaderBuild(minBuild, fromJar)
    resolvedLoaderBuild = await resolveNeoForgeBuildForInstance(instanceRoot, minBuild)
  } else if (!resolvedLoaderBuild) {
    throw new Error('Configuration loader manquante pour ce mod (loaderBuild).')
  }
  writeInstalledMeta(instanceRoot, {
    projectSlug,
    versionId: version.id,
    versionNumber: version.version_number,
    gameVersion,
    loader,
    loaderType,
    loaderBuild: resolvedLoaderBuild
  })

  return { version }
}

export async function installFromModrinth(options: {
  projectSlug: string
  gameVersion: string
  loader: string
  instanceRoot: string
  modrinthKind?: 'modpack' | 'mod'
  loaderBuild?: string
  downloadConcurrency?: number
  installProfile?: 'client' | 'server'
  onProgress?: (p: InstallProgress) => void
}): Promise<{ version: ModrinthVersion; index?: MrpackIndex }> {
  if (options.modrinthKind === 'mod') {
    const loaderNorm = options.loader.toLowerCase()
    const build = options.loaderBuild?.trim()
    if (!build && loaderNorm !== 'neoforge') {
      throw new Error('Configuration loader manquante pour ce mod (loaderBuild).')
    }
    const { version } = await installModJarFromModrinth({
      projectSlug: options.projectSlug,
      gameVersion: options.gameVersion,
      loader: options.loader,
      loaderBuild: build,
      instanceRoot: options.instanceRoot,
      onProgress: options.onProgress
    })
    return { version }
  }
  return installMrpackFromModrinth(options)
}

export { resolveNeoForgeBuildForInstance, resolveLatestNeoForgeBuild } from './neoForgeResolve.js'

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

  type Fail = Extract<IntegrityResult, { ok: false }>
  let failed: Fail | null = null
  const concurrency = integrityVerifyConcurrency()
  await runWithConcurrency(lock.files, concurrency, async ({ path: rel, sha512 }) => {
    if (failed) return
    const filePath = join(instanceRoot, rel.split('/').join(sep))
    if (!existsSync(filePath)) {
      failed = { ok: false, reason: 'missing_file', detail: rel }
      return
    }
    try {
      const ok = await verifySha512(filePath, sha512)
      if (!ok) failed = { ok: false, reason: 'hash_mismatch', detail: rel }
    } catch (e) {
      failed = { ok: false, reason: 'read_error', detail: String(e) }
    }
  })
  if (failed) return failed

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
      const versions = await fetchProjectVersions(projectSlug, {
        gameVersion,
        loaders: [loader.toLowerCase()]
      })
      let latest = pickLatestVersion(versions, gameVersion, loader)
      if (!latest && versions.length === 0) {
        const wide = await fetchProjectVersions(projectSlug)
        latest = pickLatestVersion(wide, gameVersion, loader)
      }
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

    let versions = await fetchProjectVersions(projectSlug, {
      gameVersion,
      loaders: [loader.toLowerCase()]
    })
    let latest = pickLatestVersion(versions, gameVersion, loader)
    if (!latest && versions.length === 0) {
      versions = await fetchProjectVersions(projectSlug)
      latest = pickLatestVersion(versions, gameVersion, loader)
    }
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
