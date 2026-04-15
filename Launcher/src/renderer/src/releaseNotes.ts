/** Notes affichées dans « Nouveautés » — clé = version semver du package. */
export const RELEASE_NOTES: Record<string, { en: string; fr: string }> = {
  '26.3.1': {
    en:
      'Patch 26.3.1: Home & news UI polish (cleaner glass edges, better use of space on wide windows), Settings → More adds a License link to soleapixel.com/license, storefront hero image refreshed. Display **26.3.1 | Release** (package **26.3.1**).',
    fr:
      'Correctif 26.3.1 : finitions Accueil & actus (bords verre, mise en page ultra-large), Paramètres → Plus : lien **Licence** vers soleapixel.com/license, capture du site vitrine mise à jour. Affichage **26.3.1 | Release** (paquet **26.3.1**).'
  },
  '26.3.0': {
    en:
      'My Server: host local dedicated servers from Solea Pixel modpacks (Modrinth mrpack) under userData/solea-server. Install with NeoForge/Forge server loader, EULA, console start/stop and commands, server.properties editor, world reset, JVM extras file, RAM/port, cover image, modpack change confirmation. Version 26.3 | Release (package 26.3.0).',
    fr:
      'Mon serveur : héberger des serveurs dédiés locaux à partir des modpacks Solea Pixel (mrpack Modrinth) dans userData/solea-server. Installation avec chargeur serveur NeoForge/Forge, EULA, console démarrage/arrêt et commandes, édition server.properties, réinitialisation monde, fichier JVM supplémentaire, RAM/port, image de couverture, confirmation de changement de modpack. Version 26.3 | Release (paquet 26.3.0).'
  },
  '26.2.0': {
    en:
      'AETHER UI on Home & news: full-page scroll, themed scrollbar, hero eyebrow, spotlight card for the latest release, release history column for older versions only. Screenshots library: matching AETHER cards, top accent, page scroll. Launcher version 26.2 | Release (package 26.2.0).',
    fr:
      'AETHER UI — Accueil & actus : défilement pleine page, barre de défilement thémée, eyebrow du hero, carte spotlight pour la dernière version, colonne historique pour les versions précédentes uniquement. Bibliothèque captures : cartes AETHER assorties, bandeau supérieur, défilement page. Launcher 26.2 | Release (paquet 26.2.0).'
  },
  '26.1.4': {
    en:
      'Reporting: REPORT card under Follow us (orange CTA). Modal with Launcher vs modpack instance, full category lists (instance: launch, install, account, verify files, mods; launcher: UI, Microsoft login, updates, downloads/cache, performance), themed LauncherSelects, collapsible help (?), Discord button, orange tech-details checkbox, copy/send/close. Discord: SOLEA_REPORT_WEBHOOK_URL or solea-report-webhook.url (first line) in userData; docs/REPORTING.md; .gitignore for the secret file. Main process validates Discord webhook URLs only, truncates payload (~1900 chars), surfaces HTTP/network errors; focus trap + Escape; clipboard errors to toast. Hero recentred; Follow us tightened.',
    fr:
      'Signalement : carte REPORT sous Follow us (CTA orange). Modal Launcher / instance modpack, listes de catégories complètes (instance : lancement, install, compte, vérif fichiers, mods ; launcher : UI, login Microsoft, mises à jour, téléchargements/cache, perfs), LauncherSelect thémés, aide repliable (?), Discord, case orange contexte technique, copier/envoyer/fermer. Discord : SOLEA_REPORT_WEBHOOK_URL ou solea-report-webhook.url (1re ligne) dans userData ; docs/REPORTING.md ; .gitignore pour le fichier secret. Process principal : validation URL webhooks Discord uniquement, troncature ~1900 car., erreurs HTTP/réseau remontées ; piège à focus + Échap ; erreur presse-papiers en toast. Hero recentré ; Follow us resserré.'
  },
  '26.1.3': {
    en: 'UI & settings: dedicated Audio section, cleaner volume and RAM sliders, single-row play bar, larger boot logo/bar with animated loading dots, social buttons refined. Discord Rich Presence: Install launcher (GitHub releases) and Join Discord buttons. Debug window overhaul. Frosted chrome and settings sidebar readability. Custom keyboard shortcuts. Skin preview fixed background. GitHub allowed for external links.',
    fr:
      'UI & paramètres : section Audio dédiée, curseurs volume et RAM repensés, rangée Jouer / Vérifier / compte sur une ligne, écran de démarrage (logo + barre + points animés), boutons sociaux ajustés. Discord Rich Presence : boutons Installer le launcher (releases GitHub) et Rejoindre Discord. Refonte fenêtre debug. Chrome givré et lisibilité barre latérale paramètres. Raccourcis clavier personnalisables. Fond d’aperçu skin fixe. github.com autorisé pour les liens externes.'
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
