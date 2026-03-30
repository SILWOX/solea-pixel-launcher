/**
 * Release notes shown under Home & news. Newest version first.
 * On each release: prepend an entry and list `added` / `changed` / `removed` / `fixed` as needed.
 */
export type LauncherChangelogEntry = {
  version: string
  date?: string
  added?: string[]
  changed?: string[]
  removed?: string[]
  fixed?: string[]
}

export const LAUNCHER_CHANGELOG: LauncherChangelogEntry[] = [
  {
    version: '26.1.3',
    date: '2026-03-31',
    added: [
      'Discord Rich Presence : boutons « Télécharger » (releases GitHub du launcher) et « Regarder » (page Modrinth du modpack actif).',
      'Paramètres : section **Audio** dédiée (séparée d’Apparence & langue) pour les sons d’interface, le volume et les options fin d’install / lancement.',
      'Réglages modpack : repères RAM rapides en boutons (6G / 8G / 12G) sous le curseur.',
      'Raccourcis clavier personnalisables (Paramètres → Raccourcis) pour ouvrir les paramètres, Accueil & actus et le compte.',
      'Fenêtre debug (5 clics sur Paramètres) : actualisation auto configurable, copie de l’instantané JSON, ouverture des dossiers userData / instance, lien vers la console de lancement, défilement propre et tableau des processus optionnel.'
    ],
    changed: [
      'Accueil : Jouer, Vérifier les fichiers et sélecteur de compte alignés sur une seule ligne ; compte poussé à droite quand il y a de la place.',
      'Accueil & actus : boutons réseaux sociaux moins « pilule », un peu plus grands (coins 12px, texte et icônes).',
      'Écran de démarrage : logo et barre de progression plus visibles ; libellé « Chargement » avec points animés (comme au lancement du jeu), sauf si « Réduire les animations » est actif.',
      'Paramètres : curseur de volume repensé (piste remplie, repères 0–100 %, bloc dédié).',
      'Paramètres modpack : curseur RAM plus lisible (min / RAM PC en haut, valeur allouée au centre, piste plus épaisse et pouce plus visible).',
      'Apparence : chrome givré allégé ; écran Paramètres utilise le même fond type wallpaper que l’accueil ; texte de la barre latérale des paramètres lisible sur le verre.',
      'Discord Rich Presence : activation par défaut, client ID et présence stabilisés (type d’activité, enregistrement du schéma, nouvelles tentatives si Discord s’ouvre après le launcher).',
      'Aperçu skin / cape : fond de prévisualisation fixe (#141416) — le réglage de couleur de fond a été retiré pour simplifier.',
      'Ouverture de liens externes : `github.com` autorisé (cohérent avec les boutons Discord et liens officiels).'
    ],
    fixed: [
      'Debug : boucle FPS sans fuite de `requestAnimationFrame` à l’arrêt.',
      'Rich Presence : repli automatique si Discord refuse les boutons ou les images (nouvelle tentative sans boutons, puis sans image).'
    ],
    removed: [
      'Option « fond de l’aperçu skin » dans les paramètres d’apparence.'
    ]
  },
  {
    version: '26.1.2',
    date: '2026-03-30',
    changed: [
      'Home & news now shows built-in release notes instead of downloading an external JSON news feed.'
    ],
    removed: [
      'Remote news feed URL and network fetch for announcements.'
    ]
  }
]
