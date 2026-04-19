import { execFile } from 'child_process'
import { promisify } from 'util'

const execFileAsync = promisify(execFile)

/** Ex. sortie `java -version` → major 21 ou 8 (legacy 1.8.x). */
export function parseJavaVersionOutput(combined: string): number | null {
  const m = /version "?(\d+)(?:\.(\d+))?/i.exec(combined)
  if (!m) return null
  const major = parseInt(m[1]!, 10)
  if (major === 1) {
    const minor = m[2] != null ? parseInt(m[2]!, 10) : NaN
    return Number.isFinite(minor) ? minor : null
  }
  return Number.isFinite(major) ? major : null
}

export async function getJavaRuntimeMajor(javaBin: string): Promise<number | null> {
  try {
    const { stdout, stderr } = await execFileAsync(javaBin, ['-version'], {
      encoding: 'utf8',
      maxBuffer: 256 * 1024,
      timeout: 20000,
      windowsHide: true
    })
    return parseJavaVersionOutput(`${stdout}\n${stderr}`)
  } catch (e: unknown) {
    const err = e as { stdout?: string; stderr?: string }
    const t = `${err.stdout ?? ''}\n${err.stderr ?? ''}`
    if (t.trim()) return parseJavaVersionOutput(t)
    return null
  }
}
