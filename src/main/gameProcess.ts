import { execFileSync } from 'child_process'

export function findMinecraftJavaPids(instanceRoot: string): number[] {
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
      const out = execFileSync('powershell.exe', ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', cmd], {
        encoding: 'utf8',
        windowsHide: true,
        timeout: 20000,
        maxBuffer: 2 * 1024 * 1024,
        env: { ...process.env, SOLEA_INSTANCE_ROOT: root }
      })
      return out
        .split(/\r?\n/)
        .map((l) => parseInt(l.trim(), 10))
        .filter((n) => !Number.isNaN(n) && n > 0)
    } catch {
      return []
    }
  }

  try {
    const out = execFileSync('pgrep', ['-f', root], { encoding: 'utf8' })
    return out
      .split(/\r?\n/)
      .map((l) => parseInt(l.trim(), 10))
      .filter((n) => !Number.isNaN(n) && n > 0)
  } catch {
    return []
  }
}

export function isMinecraftRunning(instanceRoot: string): boolean {
  return findMinecraftJavaPids(instanceRoot).length > 0
}

export function killMinecraftForInstance(instanceRoot: string): { ok: true } | { ok: false; error: string } {
  const pids = findMinecraftJavaPids(instanceRoot)
  if (pids.length === 0) return { ok: true }
  if (process.platform === 'win32') {
    for (const pid of pids) {
      try {
        execFileSync('taskkill', ['/PID', String(pid), '/T', '/F'], { windowsHide: true, timeout: 15000 })
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
