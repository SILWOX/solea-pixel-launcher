/**
 * Télécharge Sodium + Iris (Fabric) depuis Modrinth dans le dossier **mods du profil Solea**
 * (`.minecraft/versions/<soleapixel-…>/mods/`), pas dans `.minecraft/mods/`.
 *
 * Utilise l’API Modrinth **filtrée** (`game_versions` + `loaders`) : sans filtre,
 * la liste est paginée (~20 entrées) et les builds récentes pour une vraie release MC
 * (ex. 1.21.4) peuvent être absentes → aucun jar téléchargé.
 *
 * **Alignement produit :** le hub ne doit être enrichi (tuile `major-*.png`, pitch i18n) pour une nouvelle
 * ligne MC qu’une fois **Fabric**, **Iris** et **Sodium** publiés pour cette version sur Modrinth — voir
 * JSDoc `releaseLineAtLeast18` dans `VanillaMinecraftView.tsx`.
 *
 * Lignes 1.8–1.15 (stack OptiFine) : **Forge** est installé via `minecraft-java-core` dans `index.ts`, pas ce module.
 */
import { mkdirSync, existsSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import {
  fetchProjectVersions,
  fetchModrinthVersionById,
  pickLatestVersion,
  type ModrinthVersion
} from './modrinth.js'

const SODIUM_SLUG = 'sodium'
const IRIS_SLUG = 'iris'
/** Project id Modrinth de Sodium — Iris déclare souvent une dépendance `required` sur une build précise. */
const SODIUM_PROJECT_ID = 'AANobbMI'

function pickPrimaryJar(v: ModrinthVersion): { url: string; filename: string } | null {
  const f = v.files.find((x) => x.primary) ?? v.files.find((x) => x.filename.toLowerCase().endsWith('.jar'))
  if (!f?.url || !f.filename) return null
  return { url: f.url, filename: f.filename }
}

async function downloadJarTo(url: string, dest: string): Promise<void> {
  const res = await fetch(url)
  if (!res.ok) throw new Error(`Download ${res.status}: ${url}`)
  const buf = Buffer.from(await res.arrayBuffer())
  writeFileSync(dest, buf)
}

/** Versions Fabric pour une release Minecraft ; repli liste large si la requête filtrée est vide. */
async function listFabricVersionsForGame(slug: string, gameVersion: string): Promise<ModrinthVersion[]> {
  let rows = await fetchProjectVersions(slug, { gameVersion, loaders: ['fabric'] })
  if (rows.length === 0) {
    const wide = await fetchProjectVersions(slug)
    rows = wide.filter((v) => v.game_versions.includes(gameVersion) && v.loaders.includes('fabric'))
  }
  return rows
}

/**
 * Best-effort : n’écrase pas un fichier déjà présent avec le même nom (évite re-téléchargements).
 */
export async function ensureVanillaIrisSodiumMods(options: {
  gameVersion: string
  /** Dossier mods isolé pour ce profil (ex. …/versions/soleapixel-1.21.1-IrisLoader/mods). */
  soleaProfileModsDir: string
}): Promise<{ ok: true } | { ok: false; error: string }> {
  const { gameVersion, soleaProfileModsDir } = options
  const gv = gameVersion.trim()
  if (!gv) return { ok: false, error: 'Missing game version.' }
  const modsDir = soleaProfileModsDir.trim()
  if (!modsDir) return { ok: false, error: 'Missing Solea profile mods directory.' }
  mkdirSync(modsDir, { recursive: true })
  const errors: string[] = []
  try {
    const irisList = await listFabricVersionsForGame(IRIS_SLUG, gv)
    const vIris = pickLatestVersion(irisList, gv, 'fabric')

    let vSodium: ModrinthVersion | undefined
    const sodiumDepId = vIris?.dependencies?.find(
      (d) => d.project_id === SODIUM_PROJECT_ID && d.dependency_type === 'required' && d.version_id
    )?.version_id
    if (sodiumDepId) {
      const pinned = await fetchModrinthVersionById(sodiumDepId)
      if (pinned?.loaders.includes('fabric') && pickPrimaryJar(pinned)) vSodium = pinned
    }
    if (!vSodium) {
      const sodiumList = await listFabricVersionsForGame(SODIUM_SLUG, gv)
      vSodium = pickLatestVersion(sodiumList, gv, 'fabric')
    }

    if (!vSodium) errors.push(`${SODIUM_SLUG}: aucune version Fabric pour ${gv}`)
    if (!vIris) errors.push(`${IRIS_SLUG}: aucune version Fabric pour ${gv}`)

    const ordered: ModrinthVersion[] = []
    if (vSodium) ordered.push(vSodium)
    if (vIris) ordered.push(vIris)

    for (const v of ordered) {
      try {
        const jar = pickPrimaryJar(v)
        if (!jar) {
          errors.push(`${v.version_number}: pas de jar principal`)
          continue
        }
        const dest = join(modsDir, jar.filename)
        if (existsSync(dest)) continue
        await downloadJarTo(jar.url, dest)
      } catch (e) {
        errors.push(`${v.version_number}: ${e instanceof Error ? e.message : String(e)}`)
      }
    }
  } catch (e) {
    return { ok: false, error: e instanceof Error ? e.message : String(e) }
  }
  if (errors.length) return { ok: false, error: errors.join(' · ') }
  return { ok: true }
}
