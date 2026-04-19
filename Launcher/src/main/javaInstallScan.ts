/**
 * Détection heuristique des installations Java sur la machine (serveur / lancement).
 */
import { execFile } from 'child_process'
import { existsSync, readdirSync, statSync } from 'fs'
import { join } from 'path'
import { promisify } from 'util'
import { getJavaRuntimeMajor } from './javaRuntimeProbe.js'

const execFileAsync = promisify(execFile)

export type JavaInstallCandidate = { path: string; major: number }

function binJava(platform: string): string {
  return platform === 'win32' ? 'java.exe' : 'java'
}

async function tryAddCandidate(
  javaExe: string,
  out: JavaInstallCandidate[],
  seen: Set<string>
): Promise<void> {
  const key = javaExe.replace(/\\/g, '/').toLowerCase()
  if (seen.has(key)) return
  const major = await getJavaRuntimeMajor(javaExe)
  if (major == null) return
  seen.add(key)
  out.push({ path: javaExe, major })
}

function walkOneLevelForJavaBins(baseDir: string, platform: string): string[] {
  const j = binJava(platform)
  const found: string[] = []
  if (!existsSync(baseDir)) return found
  try {
    for (const name of readdirSync(baseDir)) {
      const p = join(baseDir, name)
      let st
      try {
        st = statSync(p)
      } catch {
        continue
      }
      if (!st.isDirectory()) continue
      const exe = join(p, 'bin', j)
      if (existsSync(exe)) found.push(exe)
    }
  } catch {
    /* ignore */
  }
  return found
}

/** Sous-dossiers type jdk-17*, jdk-21* sous Microsoft. */
function walkMicrosoftJdk(baseDir: string, platform: string): string[] {
  const j = binJava(platform)
  const found: string[] = []
  if (!existsSync(baseDir)) return found
  try {
    for (const name of readdirSync(baseDir)) {
      if (!/^jdk-?\d/i.test(name)) continue
      const p = join(baseDir, name)
      let st
      try {
        st = statSync(p)
      } catch {
        continue
      }
      if (!st.isDirectory()) continue
      const exe = join(p, 'bin', j)
      if (existsSync(exe)) found.push(exe)
    }
  } catch {
    /* ignore */
  }
  return found
}

export async function scanLocalJavaInstallations(): Promise<JavaInstallCandidate[]> {
  const platform = process.platform
  const out: JavaInstallCandidate[] = []
  const seen = new Set<string>()

  if (platform === 'win32') {
    const pf64 = process.env.ProgramW6432 || process.env.ProgramFiles || 'C:\\Program Files'
    const pf32 = process.env['ProgramFiles(x86)'] || 'C:\\Program Files (x86)'
    const bases = [
      join(pf64, 'Eclipse Adoptium'),
      join(pf64, 'Java'),
      join(pf32, 'Java'),
      join(pf64, 'Microsoft'),
      join(pf64, 'Amazon Corretto'),
      join(pf64, 'Zulu')
    ]
    for (const b of bases) {
      const exes = /[/\\]Microsoft$/i.test(b) ? walkMicrosoftJdk(b, platform) : walkOneLevelForJavaBins(b, platform)
      for (const exe of exes) await tryAddCandidate(exe, out, seen)
    }

    try {
      const { stdout } = await execFileAsync('where.exe', ['java'], {
        encoding: 'utf8',
        windowsHide: true,
        maxBuffer: 256 * 1024
      })
      for (const line of stdout.split(/\r?\n/)) {
        const p = line.trim()
        if (!p.toLowerCase().endsWith('java.exe')) continue
        await tryAddCandidate(p, out, seen)
      }
    } catch {
      /* where absent or no java on PATH */
    }
  } else if (platform === 'darwin') {
    const base = '/Library/Java/JavaVirtualMachines'
    if (existsSync(base)) {
      try {
        for (const name of readdirSync(base)) {
          if (!name.endsWith('.jdk')) continue
          const exe = join(base, name, 'Contents', 'Home', 'bin', 'java')
          if (existsSync(exe)) await tryAddCandidate(exe, out, seen)
        }
      } catch {
        /* ignore */
      }
    }
    const home = process.env.JAVA_HOME?.trim()
    if (home) {
      const exe = join(home, 'bin', 'java')
      if (existsSync(exe)) await tryAddCandidate(exe, out, seen)
    }
  } else {
    const jvm = '/usr/lib/jvm'
    if (existsSync(jvm)) {
      try {
        for (const name of readdirSync(jvm)) {
          const exe = join(jvm, name, 'bin', 'java')
          if (existsSync(exe)) await tryAddCandidate(exe, out, seen)
        }
      } catch {
        /* ignore */
      }
    }
    const home = process.env.JAVA_HOME?.trim()
    if (home) {
      const exe = join(home, 'bin', 'java')
      if (existsSync(exe)) await tryAddCandidate(exe, out, seen)
    }
  }

  out.sort((a, b) => b.major - a.major || a.path.localeCompare(b.path))
  return out
}
