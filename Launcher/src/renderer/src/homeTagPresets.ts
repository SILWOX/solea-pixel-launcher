/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { isModpackId } from './modpackTheme'

/**
 * Presets d’étiquettes carte d’accueil — prêts pour assignation modpack (futures mises à jour).
 * Les IDs servent aux clés i18n `home.tagPreset.<id>.label` / `.desc` et aux classes CSS
 * `tag-pill--preset-<kebab>`.
 */
export type HomeTagPresetId =
  | 'modded_survival'
  | 'news'
  | 'featured'
  | 'most_popular'
  | 'vanilla'
  | 'huge'
  | 'medium'
  | 'small'

/** Ordre stable pour prévisualisation debug et documentation. */
export const HOME_TAG_PRESET_IDS: HomeTagPresetId[] = [
  'modded_survival',
  'news',
  'featured',
  'most_popular',
  'vanilla',
  'small',
  'medium',
  'huge'
]

/** Étiquettes affichées sur la carte d’accueil pour le modpack sélectionné (logique produit actuelle). */
export function homeTagsForModpack(modpackId: string): HomeTagPresetId[] {
  if (!isModpackId(modpackId)) return ['modded_survival']
  const tags: HomeTagPresetId[] = ['modded_survival']

  if (modpackId === 'palamod-recreated') {
    tags.push('most_popular', 'small')
  } else if (modpackId === 'wither-storm') {
    tags.push('small')
  } else if (modpackId === 'mythic-trials-1' || modpackId === 'mythic-trials-2') {
    tags.push('huge')
  } else if (modpackId === 'aeloria') {
    tags.push('news', 'medium')
  } else if (modpackId === 'solea-optimised') {
    tags.push('featured', 'vanilla', 'small')
  }

  return tags
}
