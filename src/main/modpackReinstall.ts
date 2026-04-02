import { cpSync, existsSync, mkdirSync, rmSync, statSync } from 'fs'
import { join, dirname } from 'path'
import { randomUUID } from 'crypto'

/** Fichiers d’options Minecraft courants à la racine de l’instance. */
const OPTION_FILES = ['options.txt', 'optionsof.txt', 'optionsshaders.txt'] as const

export type ReinstallPreserveOptions = {
  keepSaves: boolean
  keepScreenshots: boolean
  keepOptions: boolean
}

export function normalizeReinstallPreserve(raw: unknown): ReinstallPreserveOptions {
  if (!raw || typeof raw !== 'object') {
    return { keepSaves: false, keepScreenshots: false, keepOptions: false }
  }
  const o = raw as Record<string, unknown>
  return {
    keepSaves: o.keepSaves === true,
    keepScreenshots: o.keepScreenshots === true,
    keepOptions: o.keepOptions === true
  }
}

/**
 * Supprime l’instance, réinstalle le pack, puis restaure les dossiers/fichiers cochés.
 * Comportement historique si aucune option n’est true : équivalent à tout supprimer puis installer.
 */
export async function reinstallModpackPreserveFlow(
  root: string,
  projectSlug: string,
  preserve: ReinstallPreserveOptions,
  runInstall: () => Promise<void>
): Promise<void> {
  const anyPreserve = preserve.keepSaves || preserve.keepScreenshots || preserve.keepOptions
  if (!anyPreserve) {
    if (existsSync(root)) rmSync(root, { recursive: true, force: true })
    mkdirSync(root, { recursive: true })
    await runInstall()
    return
  }

  const stash = join(dirname(root), `.solea-reinstall-${projectSlug}-${randomUUID().slice(0, 10)}`)
  mkdirSync(stash, { recursive: true })

  try {
    if (existsSync(root)) {
      if (preserve.keepSaves) {
        const saves = join(root, 'saves')
        if (existsSync(saves)) cpSync(saves, join(stash, 'saves'), { recursive: true })
      }
      if (preserve.keepScreenshots) {
        const shots = join(root, 'screenshots')
        if (existsSync(shots)) cpSync(shots, join(stash, 'screenshots'), { recursive: true })
      }
      if (preserve.keepOptions) {
        const optDir = join(stash, 'options-files')
        mkdirSync(optDir, { recursive: true })
        for (const f of OPTION_FILES) {
          const p = join(root, f)
          try {
            if (existsSync(p) && statSync(p).isFile()) cpSync(p, join(optDir, f))
          } catch {
            /* ignore fichiers inaccessibles */
          }
        }
      }
    }

    if (existsSync(root)) rmSync(root, { recursive: true, force: true })
    mkdirSync(root, { recursive: true })
    await runInstall()

    if (preserve.keepSaves) {
      const s = join(stash, 'saves')
      if (existsSync(s)) cpSync(s, join(root, 'saves'), { recursive: true })
    }
    if (preserve.keepScreenshots) {
      const s = join(stash, 'screenshots')
      if (existsSync(s)) cpSync(s, join(root, 'screenshots'), { recursive: true })
    }
    if (preserve.keepOptions) {
      const dir = join(stash, 'options-files')
      if (existsSync(dir)) {
        for (const f of OPTION_FILES) {
          const p = join(dir, f)
          if (existsSync(p)) cpSync(p, join(root, f))
        }
      }
    }
  } finally {
    if (existsSync(stash)) rmSync(stash, { recursive: true, force: true })
  }
}
