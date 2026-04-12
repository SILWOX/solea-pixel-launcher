/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import palWall from './assets/modpacks/palamod/wallpaper.png?url'
import palIcon from './assets/modpacks/palamod/icon.png?url'
import wsWall from './assets/modpacks/wither-storm/wallpaper.png?url'
import wsIcon from './assets/modpacks/wither-storm/icon.png?url'
import mtWall from './assets/modpacks/mythic-trials-1/wallpaper.png?url'
import mtIcon from './assets/modpacks/mythic-trials-1/icon.png?url'
import mt2Wall from './assets/modpacks/mythic-trials-2/wallpaper.png?url'
import mt2Icon from './assets/modpacks/mythic-trials-2/icon.png?url'
import aeWall from './assets/modpacks/aeloria/wallpaper.png?url'
import aeIcon from './assets/modpacks/aeloria/icon.png?url'

export type ModpackIdUi =
  | 'palamod-recreated'
  | 'wither-storm'
  | 'mythic-trials-1'
  | 'mythic-trials-2'
  | 'aeloria'

export const MODPACK_THEME: Record<
  ModpackIdUi,
  {
    wallpaper: string
    sidebarIcon: string
    themeClass: string
  }
> = {
  'palamod-recreated': {
    wallpaper: palWall,
    sidebarIcon: palIcon,
    themeClass: 'theme-palamod'
  },
  'wither-storm': {
    wallpaper: wsWall,
    sidebarIcon: wsIcon,
    themeClass: 'theme-wither-storm'
  },
  'mythic-trials-1': {
    wallpaper: mtWall,
    sidebarIcon: mtIcon,
    themeClass: 'theme-mythic-trials-1'
  },
  'mythic-trials-2': {
    wallpaper: mt2Wall,
    sidebarIcon: mt2Icon,
    themeClass: 'theme-mythic-trials-2'
  },
  aeloria: {
    wallpaper: aeWall,
    sidebarIcon: aeIcon,
    themeClass: 'theme-aeloria'
  }
}

export function isModpackId(s: string): s is ModpackIdUi {
  return (
    s === 'palamod-recreated' ||
    s === 'wither-storm' ||
    s === 'mythic-trials-1' ||
    s === 'mythic-trials-2' ||
    s === 'aeloria'
  )
}

/** Clé i18n `home.lead.<id>` — description d’accueil propre à chaque modpack. */
export const MODPACK_HOME_LEAD_KEY: Record<ModpackIdUi, string> = {
  'palamod-recreated': 'home.lead.palamod-recreated',
  'wither-storm': 'home.lead.wither-storm',
  'mythic-trials-1': 'home.lead.mythic-trials-1',
  'mythic-trials-2': 'home.lead.mythic-trials-2',
  aeloria: 'home.lead.aeloria'
}

