import { existsSync, readFileSync, unlinkSync, writeFileSync } from 'fs'
import { join } from 'path'
import { spawn } from 'child_process'
import { loadSettings } from './settings.js'

export type SoleInstalledMeta = {
  gameVersion: string
  loaderType?: 'neoforge' | 'forge'
  loaderBuild?: string
  neoForgeVersion?: string
}

function readInstalledMeta(instanceRoot: string): SoleInstalledMeta | null {
  const p = join(instanceRoot, '.solea-installed.json')
  if (!existsSync(p)) return null
  try {
    return JSON.parse(readFileSync(p, 'utf8')) as SoleInstalledMeta
  } catch {
    return null
  }
}

function normalizeForgeBuild(gameVersion: string, build: string): string {
  if (!build) return build
  if (build.includes('-')) return build
  return `${gameVersion}-${build}`
}

function resolveJavaExecutable(): string {
  const s = loadSettings().javaPath?.trim()
  if (!s) return 'java'
  if (s.toLowerCase().endsWith('javaw.exe')) return `${s.slice(0, -9)}java.exe`
  return s
}

function forgeInstallerJarUrl(forgeFullVersion: string): string {
  return `https://maven.minecraftforge.net/net/minecraftforge/forge/${forgeFullVersion}/forge-${forgeFullVersion}-installer.jar`
}

function neoForgeInstallerJarUrl(neoVersion: string): string {
  return `https://maven.neoforged.net/releases/net/neoforged/neoforge/${neoVersion}/neoforge-${neoVersion}-installer.jar`
}

async function downloadToFile(url: string, dest: string): Promise<void> {
  const res = await fetch(url, { signal: AbortSignal.timeout(120_000) })
  if (!res.ok) throw new Error(`HTTP ${res.status} — ${url}`)
  const buf = Buffer.from(await res.arrayBuffer())
  writeFileSync(dest, buf)
}

function spawnWithLogs(
  command: string,
  args: string[],
  cwd: string,
  onLine?: (line: string) => void
): Promise<number> {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { cwd, windowsHide: true })
    const pipe = (buf: Buffer) => {
      String(buf)
        .split(/\r?\n/)
        .map((l) => l.trimEnd())
        .filter((l) => l.trim())
        .forEach((l) => onLine?.(l))
    }
    child.stdout?.on('data', pipe)
    child.stderr?.on('data', (b) => pipe(Buffer.from(`[stderr] ${b}`)))
    child.on('error', reject)
    child.on('close', (code) => resolve(code ?? 1))
  })
}

function hasRunScript(instanceRoot: string): boolean {
  return existsSync(join(instanceRoot, 'run.bat')) || existsSync(join(instanceRoot, 'run.sh'))
}

function writeEula(instanceRoot: string) {
  writeFileSync(join(instanceRoot, 'eula.txt'), 'eula=true\n', 'utf8')
}

/**
 * Télécharge l’installateur Forge ou NeoForge et lance `--installServer` dans le dossier du serveur
 * (après extraction du .mrpack). Nécessite Java (paramètre Java du launcher ou `java` sur le PATH).
 */
export async function installForgeNeoForgeServerLoader(options: {
  instanceRoot: string
  onLog?: (line: string) => void
}): Promise<void> {
  const { instanceRoot, onLog } = options
  const meta = readInstalledMeta(instanceRoot)
  if (!meta?.gameVersion) throw new Error('.solea-installed.json manquant ou incomplet.')

  let loaderType: 'neoforge' | 'forge'
  let build: string
  if (meta.loaderType === 'neoforge' || meta.neoForgeVersion) {
    loaderType = 'neoforge'
    build = (meta.loaderType === 'neoforge' ? meta.loaderBuild : meta.neoForgeVersion) ?? ''
  } else if (meta.loaderType === 'forge' || meta.loaderBuild) {
    loaderType = 'forge'
    build = normalizeForgeBuild(meta.gameVersion, meta.loaderBuild ?? '')
  } else {
    throw new Error('Loader Forge/NeoForge non décrit dans .solea-installed.json.')
  }
  if (!build) throw new Error('Version du loader (build) manquante.')

  const java = resolveJavaExecutable()
  const url =
    loaderType === 'neoforge' ? neoForgeInstallerJarUrl(build) : forgeInstallerJarUrl(build)
  const jarName =
    loaderType === 'neoforge' ? `neoforge-${build}-installer.jar` : `forge-${build}-installer.jar`
  const jarPath = join(instanceRoot, jarName)

  onLog?.(`[Solea] Téléchargement de l’installateur ${loaderType} (${build})…`)
  try {
    await downloadToFile(url, jarPath)
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    try {
      unlinkSync(jarPath)
    } catch {
      /* ignore */
    }
    throw new Error(`Échec du téléchargement de l’installateur serveur : ${msg}`)
  }

  onLog?.(`[Solea] Installation du serveur ${loaderType} dans le dossier du pack (Java : ${java})…`)
  const code = await spawnWithLogs(java, ['-jar', jarName, '--installServer', '.'], instanceRoot, onLog)
  try {
    unlinkSync(jarPath)
  } catch {
    /* ignore */
  }

  if (code !== 0) {
    throw new Error(`L’installateur serveur s’est terminé avec le code ${code}.`)
  }

  if (!hasRunScript(instanceRoot)) {
    throw new Error(
      'Installation terminée mais aucun run.bat / run.sh trouvé. Vérifiez la version Java (ex. 17 pour Forge 1.20.x, 21 pour NeoForge 1.21.x).'
    )
  }

  writeEula(instanceRoot)
  onLog?.('[Solea] Loader serveur installé (run.bat / run.sh). eula.txt acceptée pour le premier démarrage.')
}
