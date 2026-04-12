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
    version: '26.2',
    date: '2026-03-30',
    added: [
      '**Home & news — AETHER UI:** full-page scroll shell with the themed AETHER scrollbar, a centered content rail, hero eyebrow, and a **spotlight** card for the latest release so patch notes read clearly before history.',
      '**Screenshots — AETHER UI:** page-level scroll (same scrollbar treatment), sharper glass cards with a top accent strip, and spacing/typography aligned with Settings and Account.',
      '**Release history column** now lists earlier versions only; the newest build is always shown in the spotlight above the three-column hub.'
    ],
    changed: [
      'Tighter visual hierarchy on Home & news: spotlight uses full-width emphasis; profile, follow/report, and history panels stay consistent with the rest of the launcher chrome.',
      'Changelog panel copy and layout tuned so “Release notes” + semver pill still anchor the middle column while older entries scroll independently.'
    ]
  },
  {
    version: '26.1.4',
    date: '2026-03-30',
    added: [
      'Signalement depuis l’accueil : carte **REPORT** (titre + court texte d’intro) sous « Follow us », bouton d’action orange pour ouvrir le formulaire.',
      'Modal de signalement : segment **Launcher** / **Instance (modpack)** avec rappel contextuel ; au changement de portée, la catégorie repasse sur la première entrée de la liste.',
      'Catégories **instance** : lancement / crash, installation / mise à jour, compte en jeu, vérification des fichiers / intégrité, mods / crash en jeu, autre.',
      'Catégories **launcher** : interface / affichage, connexion Microsoft / compte launcher, mises à jour du launcher, téléchargements / cache, gels / lenteur, autre.',
      'Sélecteur d’instance et liste des catégories via le même composant que le reste du launcher (**LauncherSelect**), styles dédiés dans la modal (clair / sombre, focus visible).',
      'Panneau d’aide repliable (bouton **?**) expliquant copie / envoi ; bouton **Rejoindre Discord** ; zone de détails libre ; case à cocher mise en avant (orange) pour joindre version launcher, modpack (si instance) et OS.',
      'Actions **Copier le rapport** (Markdown dans le presse-papiers), **Envoyer à l’équipe** (si configuré), **Fermer** ; piège à focus dans la modal et **Échap** pour fermer.',
      'Envoi vers Discord : **SOLEA_REPORT_WEBHOOK_URL** (environnement) ou fichier **solea-report-webhook.url** dans userData (première ligne = URL) ; documentation **docs/REPORTING.md** ; entrée **.gitignore** pour ne pas versionner le fichier secret.'
    ],
    changed: [
      'Accueil & actus : bloc « Follow us » un peu plus compact ; en-tête hero recentré (le signalement n’est plus dans le bandeau du hero).',
      'Interface du signalement : libellés et aide rédigés sans jargon « webhook » ; le toast si l’envoi n’est pas configuré renvoie vers la doc (variable ou fichier local).'
    ],
    fixed: [
      'Côté process principal : seules les URL de webhooks **discord.com** / **canary.discord.com** sont acceptées ; fichier ou variable ignorés si vide ou invalide (évite les requêtes vers une mauvaise cible).',
      'Corps du message tronqué (~1900 caractères) avant envoi pour rester dans les limites Discord.',
      'Erreurs réseau ou HTTP renvoyées au renderer avec extrait de réponse pour le toast d’échec ; contenu vide refusé proprement.',
      'Échec de copie presse-papiers : toast d’erreur dédié au lieu d’un silence.'
    ]
  },
  {
    version: '26.1.3',
    date: '2026-03-31',
    added: [
      'Discord Rich Presence : boutons « Installer le launcher » (releases GitHub) et « Rejoindre Discord » (invitation Solea Pixel).',
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
