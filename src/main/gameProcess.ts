import { execFile as execFileCb } from 'node:child_process'
import { promisify } from 'node:util'

const execFile = promisify(execFileCb)

export async function findMinecraftJavaPids(instanceRoot: string): Promise<number[]> {
  const root = instanceRoot.trim()
  if (!root) return []

  if (process.platform === 'win32') {
    const cmd = `
      $needle = $env:SOLEA_INSTANCE_ROOT.ToLower()
      if (-not $needle) { exit 0 }
      Get-CimInstance Win32_Process -Filter "Name = 'java.exe' OR Name = 'javaw.exe'" | ForEach-Object {
        $cl = if ($_.CommandLine) { $_.CommandLine.ToLower() } else { '' }
        if ($cl -and $cl.Contains($needle)) { $_.ProcessId }
      }
    `
    try {
      const { stdout } = await execFile(
        'powershell.exe',
        ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', cmd],
        {
          encoding: 'utf8',
          windowsHide: true,
          timeout: 20000,
          maxBuffer: 2 * 1024 * 1024,
          env: { ...process.env, SOLEA_INSTANCE_ROOT: root }
        }
      )
      return String(stdout)
        .split(/\r?\n/)
        .map((l) => parseInt(l.trim(), 10))
        .filter((n) => !Number.isNaN(n) && n > 0)
    } catch {
      return []
    }
  }

  try {
    const { stdout } = await execFile('pgrep', ['-f', root], { encoding: 'utf8' })
    return String(stdout)
      .split(/\r?\n/)
      .map((l) => parseInt(l.trim(), 10))
      .filter((n) => !Number.isNaN(n) && n > 0)
  } catch {
    return []
  }
}

export async function isMinecraftRunning(instanceRoot: string): Promise<boolean> {
  const pids = await findMinecraftJavaPids(instanceRoot)
  return pids.length > 0
}

export async function killMinecraftForInstance(
  instanceRoot: string
): Promise<{ ok: true } | { ok: false; error: string }> {
  const pids = await findMinecraftJavaPids(instanceRoot)
  if (pids.length === 0) return { ok: true }
  if (process.platform === 'win32') {
    for (const pid of pids) {
      try {
        await execFile('taskkill', ['/PID', String(pid), '/T', '/F'], {
          windowsHide: true,
          timeout: 15000
        })
      } catch {
        /* continue */
      }
    }
  } else {
    for (const pid of pids) {
      try {
        process.kill(pid, 'SIGTERM')
      } catch {
        /* ignore */
      }
    }
  }
  return { ok: true }
}
