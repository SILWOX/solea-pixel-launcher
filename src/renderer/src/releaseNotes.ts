/** Notes affichées dans « Nouveautés » — clé = version semver du package. */
export const RELEASE_NOTES: Record<string, { en: string; fr: string }> = {
  '26.1.3': {
    en: 'UI & settings: dedicated Audio section, cleaner volume and RAM sliders, single-row play bar, larger boot logo/bar with animated loading dots, social buttons refined. Discord Rich Presence: Download (GitHub releases) and Watch (Modrinth pack page) buttons. Debug window overhaul. Frosted chrome and settings sidebar readability. Custom keyboard shortcuts. Skin preview fixed background. GitHub allowed for external links.',
    fr:
      'UI & paramètres : section Audio dédiée, curseurs volume et RAM repensés, rangée Jouer / Vérifier / compte sur une ligne, écran de démarrage (logo + barre + points animés), boutons sociaux ajustés. Discord Rich Presence : boutons Télécharger (releases GitHub) et Regarder (Modrinth). Refonte fenêtre debug. Chrome givré et lisibilité barre latérale paramètres. Raccourcis clavier personnalisables. Fond d’aperçu skin fixe. github.com autorisé pour les liens externes.'
  },
  '26.1.2': {
    en: 'Packaging: NSIS installer filename matches latest.yml (Solea-Pixel-Setup-…) so GitHub auto-updates download correctly. Includes .connector integrity exemption from 26.1.1.',
    fr:
      'Paquet : nom de l’installateur NSIS aligné sur latest.yml (Solea-Pixel-Setup-…) pour que les mises à jour GitHub se téléchargent. Inclut l’exception mods/.connector de la 26.1.1.'
  },
  '26.1.1': {
    en: 'Integrity: ignore Sinytra Connector cache JARs under mods/.connector/ (e.g. Continuity remapped jars) so the pack can launch.',
    fr:
      'Intégrité : les JAR générés par Sinytra Connector dans mods/.connector/ (ex. Continuity) sont ignorés pour permettre le lancement du pack.'
  },
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
