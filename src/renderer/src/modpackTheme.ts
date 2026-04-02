/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import palWall from './assets/modpacks/palamod/wallpaper.png?url'
import palIcon from './assets/modpacks/palamod/icon.png?url'
import wsWall from './assets/modpacks/wither-storm/wallpaper.png?url'
import wsIcon from './assets/modpacks/wither-storm/icon.png?url'

export type ModpackIdUi = 'palamod-recreated' | 'wither-storm'

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
  }
}

export function isModpackId(s: string): s is ModpackIdUi {
  return s === 'palamod-recreated' || s === 'wither-storm'
}
