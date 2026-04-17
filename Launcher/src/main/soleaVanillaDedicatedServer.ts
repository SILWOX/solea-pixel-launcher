/**
 * Installation serveur Minecraft vanilla officiel (server.jar) pour « My Server ».
 */
import { chmodSync, writeFileSync } from 'fs'
import { join } from 'path'

const MANIFEST_URL = 'https://launchermeta.mojang.com/mc/game/version_manifest_v2.json'

type VersionManifestEntry = { id: string; url: string; type?: string; releaseTime?: string }
type VersionManifest = { versions: VersionManifestEntry[] }
type VersionJson = {
  id?: string
  downloads?: { server?: { url: string; sha1?: string; size?: number } }
  /** Présent sur les versions récentes — ex. 21, 25 pour Minecraft 26.x */
  javaVersion?: { component?: string; majorVersion?: number }
}

/** Versions du type 1.8 / 1.12.2 : exige Minecraft ≥ 1.8.0 (les snapshots passent si présentes dans le manifeste). */
export function isVanillaServerVersionAtLeast18(versionId: string): boolean {
  const v = versionId.trim()
  const m = /^(\d+)\.(\d+)(?:\.(\d+))?$/.exec(v)
  if (!m) return true
  const major = parseInt(m[1], 10)
  const minor = parseInt(m[2], 10)
  if (Number.isNaN(major) || Number.isNaN(minor)) return true
  if (major > 1) return true
  if (major < 1) return false
  return minor >= 8
}

/** Releases officielles ≥ 1.8, plus récentes en premier (pour menus). */
export async function listMojangVanillaReleaseIdsFrom18(): Promise<string[]> {
  const manRes = await fetch(MANIFEST_URL, { signal: AbortSignal.timeout(25000) })
  if (!manRes.ok) throw new Error(`Manifeste HTTP ${String(manRes.status)}`)
  const manifest = (await manRes.json()) as VersionManifest
  const versions = manifest.versions ?? []
  const releases = versions.filter(
    (v) => v.type === 'release' && isVanillaServerVersionAtLeast18(v.id)
  )
  releases.sort((a, b) => {
    const ta = a.releaseTime ? Date.parse(a.releaseTime) : 0
    const tb = b.releaseTime ? Date.parse(b.releaseTime) : 0
    return tb - ta
  })
  return releases.map((v) => v.id)
}

/** Lit `javaVersion.majorVersion` dans le JSON de version Mojang (pour contrôle JVM au démarrage). */
export async function fetchVanillaVersionJavaMajor(versionId: string): Promise<number | null> {
  const want = versionId.trim()
  if (!want) return null
  try {
    const manRes = await fetch(MANIFEST_URL, { signal: AbortSignal.timeout(25000) })
    if (!manRes.ok) return null
    const manifest = (await manRes.json()) as VersionManifest
    const entry = manifest.versions?.find((e) => e.id === want)
    if (!entry?.url) return null
    const verRes = await fetch(entry.url, { signal: AbortSignal.timeout(25000) })
    if (!verRes.ok) return null
    const verJson = (await verRes.json()) as VersionJson
    const jm = verJson.javaVersion?.majorVersion
    return typeof jm === 'number' && jm > 0 ? jm : null
  } catch {
    return null
  }
}

export async function installVanillaDedicatedServerFiles(opts: {
  instanceRoot: string
  /** Id de version Mojang (ex. 1.21.4, 1.8.9) */
  versionId: string
  onLog?: (line: string) => void
}): Promise<{ mcVersionId: string; javaMajor?: number }> {
  const { instanceRoot, versionId, onLog } = opts
  const want = versionId.trim()
  if (!want) throw new Error('Version Minecraft vide.')

  onLog?.(`[Solea] Manifeste Mojang…`)
  const manRes = await fetch(MANIFEST_URL, { signal: AbortSignal.timeout(25000) })
  if (!manRes.ok) throw new Error(`Manifeste HTTP ${String(manRes.status)}`)
  const manifest = (await manRes.json()) as VersionManifest
  const entry = manifest.versions?.find((e) => e.id === want)
  if (!entry?.url) throw new Error(`Version « ${want} » introuvable (manifeste Mojang).`)

  onLog?.(`[Solea] Métadonnées ${entry.id}…`)
  const verRes = await fetch(entry.url, { signal: AbortSignal.timeout(25000) })
  if (!verRes.ok) throw new Error(`Version JSON HTTP ${String(verRes.status)}`)
  const verJson = (await verRes.json()) as VersionJson
  const javaMajorRaw = verJson.javaVersion?.majorVersion
  const javaMajor =
    typeof javaMajorRaw === 'number' && javaMajorRaw > 0 ? javaMajorRaw : undefined
  const serverUrl = verJson.downloads?.server?.url
  if (!serverUrl) throw new Error(`Pas de server.jar officiel pour « ${want} ».`)

  onLog?.(`[Solea] Téléchargement server.jar…`)
  const jarRes = await fetch(serverUrl, { signal: AbortSignal.timeout(180000) })
  if (!jarRes.ok) throw new Error(`server.jar HTTP ${String(jarRes.status)}`)
  const buf = Buffer.from(await jarRes.arrayBuffer())
  if (buf.length < 1024) throw new Error('server.jar trop petit (téléchargement invalide).')

  const jarPath = join(instanceRoot, 'server.jar')
  writeFileSync(jarPath, buf)
  writeFileSync(join(instanceRoot, 'eula.txt'), 'eula=true\n', 'utf8')

  const runBat = join(instanceRoot, 'run.bat')
  writeFileSync(
    runBat,
    ['@echo off', 'cd /d "%~dp0"', 'java @user_jvm_args.txt -jar server.jar nogui', ''].join('\r\n'),
    'utf8'
  )
  const runSh = join(instanceRoot, 'run.sh')
  writeFileSync(
    runSh,
    ['#!/bin/sh', 'cd "$(dirname "$0")"', 'exec java @user_jvm_args.txt -jar server.jar nogui', ''].join('\n'),
    'utf8'
  )
  try {
    chmodSync(runSh, 0o755)
  } catch {
    /* Windows ou FS sans chmod */
  }

  const mcId = verJson.id ?? entry.id
  writeFileSync(
    join(instanceRoot, '.solea-installed.json'),
    JSON.stringify(
      {
        versionId: mcId,
        versionNumber: want,
        ...(javaMajor != null ? { javaMajor } : {})
      },
      null,
      2
    ),
    'utf8'
  )

  onLog?.(`[Solea] Serveur vanilla ${want} installé (server.jar).`)
  if (javaMajor != null) {
    onLog?.(`[Solea] JVM minimale indiquée par Mojang : Java ${String(javaMajor)}.`)
  }
  return { mcVersionId: mcId, javaMajor }
}
