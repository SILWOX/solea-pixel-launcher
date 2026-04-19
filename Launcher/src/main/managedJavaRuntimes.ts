/**
 * Runtimes Eclipse Temurin (JRE) pour serveur — téléchargés sous userData/runtime/java.
 */
import { app } from 'electron'
import AdmZip from 'adm-zip'
import { existsSync, mkdirSync, readFileSync, readdirSync, rmSync, statSync, writeFileSync } from 'fs'
import { join } from 'path'

export const MANAGED_JAVA_MAJORS = [8, 17, 21, 25] as const
export type ManagedJavaMajor = (typeof MANAGED_JAVA_MAJORS)[number]

const MARKER = '.solea-java-exe.txt'

function adoptiumOsArch(): { os: string; arch: string } {
  const platform = process.platform
  const arch = process.arch
  if (platform === 'win32') return { os: 'windows', arch: arch === 'arm64' ? 'aarch64' : 'x64' }
  if (platform === 'darwin') return { os: 'mac', arch: arch === 'arm64' ? 'aarch64' : 'x64' }
  return { os: 'linux', arch: arch === 'arm64' ? 'aarch64' : 'x64' }
}

export function managedJavaRoot(): string {
  return join(app.getPath('userData'), 'runtime', 'java')
}

export function managedJavaVersionDir(major: number): string {
  return join(managedJavaRoot(), `temurin-${major}`)
}

function javaBinName(): string {
  return process.platform === 'win32' ? 'java.exe' : 'java'
}

/** Repère JAVA_HOME après extraction du zip Temurin (un dossier racine type jdk-21…-jre). */
function findJavaHomeAfterExtract(destDir: string): string | null {
  const binName = javaBinName()
  try {
    for (const name of readdirSync(destDir)) {
      const p = join(destDir, name)
      let st
      try {
        st = statSync(p)
      } catch {
        continue
      }
      if (!st.isDirectory()) continue
      const exe = join(p, 'bin', binName)
      if (existsSync(exe)) return p
    }
  } catch {
    return null
  }
  const flat = join(destDir, 'bin', binName)
  if (existsSync(flat)) return destDir
  return null
}

export function getInstalledManagedJavaExecutable(major: number): string | null {
  const root = managedJavaVersionDir(major)
  const marker = join(root, MARKER)
  if (existsSync(marker)) {
    try {
      const exe = readFileSync(marker, 'utf8').trim()
      if (exe && existsSync(exe)) return exe
    } catch {
      /* fall through */
    }
  }
  const home = findJavaHomeAfterExtract(root)
  if (!home) return null
  const exe = join(home, 'bin', javaBinName())
  return existsSync(exe) ? exe : null
}

export function isManagedJavaMajor(v: number): v is ManagedJavaMajor {
  return (MANAGED_JAVA_MAJORS as readonly number[]).includes(v)
}

export async function ensureManagedJavaDownloaded(
  major: number,
  onLog?: (line: string) => void
): Promise<{ ok: true; javaExecutable: string } | { ok: false; error: string }> {
  if (!isManagedJavaMajor(major)) return { ok: false, error: 'Version Java non prise en charge.' }
  const existing = getInstalledManagedJavaExecutable(major)
  if (existing) return { ok: true, javaExecutable: existing }

  const { os, arch } = adoptiumOsArch()
  const url = `https://api.adoptium.net/v3/binary/latest/${major}/ga/${os}/${arch}/jre/hotspot/normal/eclipse`
  const destRoot = managedJavaVersionDir(major)
  try {
    rmSync(destRoot, { recursive: true, force: true })
    mkdirSync(managedJavaRoot(), { recursive: true })
    mkdirSync(destRoot, { recursive: true })
  } catch (e) {
    return { ok: false, error: e instanceof Error ? e.message : String(e) }
  }

  onLog?.(`[Solea] Téléchargement Java ${major} (Eclipse Temurin JRE)…`)
  let res: Response
  try {
    res = await fetch(url, { redirect: 'follow' })
  } catch (e) {
    return { ok: false, error: e instanceof Error ? e.message : 'Réseau indisponible.' }
  }
  if (!res.ok) return { ok: false, error: `Adoptium HTTP ${String(res.status)}` }

  const tmpZip = join(destRoot, '_download.zip')
  try {
    const buf = Buffer.from(await res.arrayBuffer())
    writeFileSync(tmpZip, buf)
  } catch (e) {
    return { ok: false, error: e instanceof Error ? e.message : 'Échec écriture du téléchargement.' }
  }

  try {
    const zip = new AdmZip(tmpZip)
    zip.extractAllTo(destRoot, true)
  } catch (e) {
    return { ok: false, error: e instanceof Error ? e.message : 'Extraction ZIP impossible.' }
  }

  try {
    rmSync(tmpZip, { force: true })
  } catch {
    /* ignore */
  }

  const home = findJavaHomeAfterExtract(destRoot)
  if (!home) return { ok: false, error: 'Java extrait mais binaire introuvable.' }
  const exe = join(home, 'bin', javaBinName())
  if (!existsSync(exe)) return { ok: false, error: 'java introuvable après extraction.' }
  try {
    writeFileSync(join(destRoot, MARKER), exe, 'utf8')
  } catch {
    /* ignore */
  }
  onLog?.(`[Solea] Java ${major} prêt : ${exe}`)
  return { ok: true, javaExecutable: exe }
}

export function managedJavaInstallStatus(): { major: ManagedJavaMajor; path: string | null }[] {
  return MANAGED_JAVA_MAJORS.map((major) => ({
    major,
    path: getInstalledManagedJavaExecutable(major)
  }))
}
