/** Notes affichées dans « Nouveautés » — clé = version semver du package. */
export const RELEASE_NOTES: Record<string, { en: string; fr: string }> = {
  '26.1.0': {
    en: 'First official Release (26.1): GitHub auto-updates, stable channel, integrity checks enabled for launch.',
    fr:
      'Première version officielle Release (26.1) : mises à jour via GitHub, canal stable, vérification d’intégrité des mods à lancement.'
  },
  '26.0.3': {
    en: 'Build 26.0.3: version bump and packaging refresh.',
    fr: 'Build 26.0.3 : incrément de version et regénération du paquet.'
  },
  '26.0.2': {
    en: 'Beta update: new app icon, version badge, Whats new popup disabled for update testing.',
    fr: 'Mise à jour bêta : nouvelle icône, badge de version, popup Nouveautés désactivée pour tester les mises à jour.'
  },
  '1.0.0': {
    en: 'Initial Solea Pixel Launcher release: Modrinth modpacks, Microsoft login, skin presets, cape picker, NeoForge support.',
    fr: 'Première version du Solea Pixel Launcher : modpacks Modrinth, connexion Microsoft, presets de skin, choix de cape, NeoForge.'
  }
}
