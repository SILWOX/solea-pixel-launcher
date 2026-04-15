/**
 * i18n Solea Pixel — EN par défaut, FR si navigateur français ou choix utilisateur.
 */
;(function () {
  const STORAGE_KEY = 'solea-site-lang'

  const STR = {
    en: {
      'page.title': 'SOLEA PIXEL',
      'meta.description':
        'WELCOME: install our launcher here and discover the features offered by the launcher.',
      'nav.home': 'Home',
      'nav.news': 'News',
      'nav.about': 'About',
      'nav.downloads': 'Downloads',
      'nav.faq': 'FAQ',
      'nav.skip': 'Skip to content',
      'nav.top': 'Back to top',
      'nav.socialToolbar': 'Social links',
      'nav.ariaModrinth': 'Solea Pixel on Modrinth',
      'nav.ariaYoutube': 'SILWOX on YouTube',
      'nav.ariaX': 'SILWOX on X',
      'nav.ariaDiscord': 'Solea Pixel on Discord',
      'nav.ariaBmc': 'Support on Buy Me a Coffee',
      'nav.socialShort': 'Social',
      'nav.langShort': 'Lang',
      'page404.docTitle': 'SOLEA PIXEL — Page not found',
      'page404.metaDescription':
        'The page you requested was not found on Solea Pixel. Return to the home page or open the SPL license.',
      'page404.skip': 'Skip to content',
      'page404.code': '404',
      'page404.title': 'Page not found',
      'page404.lead':
        'This address does not exist on this site. Check the spelling of the URL or use the links below.',
      'page404.pathLabel': 'Requested path',
      'page404.homeBtn': 'Back to home',
      'page404.licenseBtn': 'SPL license',
      'lang.fr': 'FR',
      'lang.en': 'EN',
      'hub.tag1': '100% free',
      'hub.tag2': 'Premium & cracked',
      'hub.tag3': 'Solea Pixel modpacks',
      'hub.tag4': 'Powered by Modrinth',
      'hub.tag5': 'Community driven',
      'hub.h1a': 'Everything to play',
      'hub.h1b': 'SOLEA packs',
      'hub.typePrefix': 'The SOLEA PIXEL universe:',
      'hub.lead':
        'One launcher: news, Microsoft accounts, NeoForge modpacks — and a **Local server** space to run a world on your PC, with files kept tidy.',
      'hub.btnDl': 'Download',
      'hub.btnDiscover': 'Discover',
      'hub.versionLoading': 'Loading latest installer…',
      'hub.versionSlow': 'GitHub is slow — still fetching the latest installer…',
      'hub.versionOk': 'Latest Windows installer: **{v}**.',
      'hub.versionFallback': 'Latest build on the download page.',
      'tilt.1l': 'Home',
      'tilt.1t': 'News & patch notes',
      'tilt.2l': 'Social',
      'tilt.2t': 'Modrinth, Discord…',
      'tilt.3l': 'Account',
      'tilt.3t': 'Microsoft / skin',
      'meta.build': 'Build',
      'meta.platform': 'Platform',
      'downloads.tag': 'Get',
      'downloads.title': 'Downloads',
      'downloads.lead': 'Stable channel for Windows. Other platforms and beta builds will follow.',
      'downloads.stable': 'Stable',
      'downloads.beta': 'Beta',
      'downloads.betaSoon': 'No separate beta channel yet — stay tuned on Discord.',
      'downloads.win': 'Windows (x64)',
      'downloads.winHint': 'x64 · NSIS installer',
      'downloads.winBtn': 'Download installer',
      'downloads.mac': 'macOS',
      'downloads.linux': 'Linux',
      'downloads.na': 'Not available yet',
      'downloads.reqTitle': 'Before you install',
      'downloads.req1': 'Windows 10 or 11 (64-bit)',
      'downloads.req2': 'About 400 MB free disk for the launcher',
      'downloads.req3': 'Internet for first launch and Modrinth installs',
      'downloads.afterTitle': 'Install & updates',
      'downloads.afterBody':
        'The Windows build is code-signed when published. If SmartScreen appears, use “More info” then “Run anyway” only if you trust Solea Pixel. The launcher can update itself from the same release channel as this page.',
      'downloads.helpTitle': 'Need help?',
      'downloads.helpBody':
        'Read the <a href="#faq">FAQ</a> or ask on <a href="https://discord.gg/jVGq5aZ6Wc" target="_blank" rel="noopener noreferrer">Discord</a>.',
      'about.tag': 'About',
      'about.title': 'The project behind the launcher',
      'about.lead': 'Modpacks, community, support — everything we build for players.',
      'about.c1t': 'Modpacks via Modrinth',
      'about.c1b':
        'Packs are wired straight into the launcher from Modrinth: install from the app and jump in — no manual folder juggling.',
      'about.c2t': 'Our Discord',
      'about.c2b':
        'Join our community server: chat with other players, share screenshots, and reach support in real time when you need help.',
      'about.c3t': 'Support that listens',
      'about.c3b':
        'Stuck on install, crash, or modpack? We offer several ways to reach us — Discord is the fastest, and we read every report.',
      'about.c4t': 'NeoForge & updates',
      'about.c4b':
        'Built around NeoForge 1.21.1 with clear update paths: the launcher and this site stay aligned on the same official installers.',
      'about.c5t': 'Designed for everyone',
      'about.c5b':
        'Whether you play premium or offline, solo or with friends, the UI stays readable and the options stay where you expect them.',
      'about.c6t': 'Why Solea Pixel instead of the default launcher?',
      'about.c6b':
        'The official launcher is great for vanilla Minecraft. Solea Pixel focuses on **our NeoForge modpacks**, Modrinth installs, news in one place, optional offline play, and a **local server** workflow — without replacing Mojang tools, just offering a smoother path for Solea players.',
      'about.smartscreenTitle': 'Installation: “Windows protected your PC” (SmartScreen)',
      'about.smartscreenIntro':
        'When you run the installer, Windows may show a blue SmartScreen screen — that is common for a new app that has not yet built enough reputation with Microsoft.',
      'about.smartscreenWhy':
        '<strong>Why?</strong> Solea Pixel is still new to SmartScreen’s reputation system. The warning can appear even for safe, signed installers until enough people have installed them.',
      'about.smartscreenHow': 'How to install anyway:',
      'about.smartscreenStep1Lead': 'Click the underlined ',
      'about.smartscreenMoreBtn': 'More info',
      'about.smartscreenStep1Tail': ' link on that screen — the same wording Windows shows in English.',
      'about.smartscreenStep2':
        'Then click <strong>Run anyway</strong> when the button appears — only if you downloaded Solea Pixel from this site or our official GitHub releases.',
      'about.smartscreenHelpTitle': '“More info” — what SmartScreen is doing',
      'about.smartscreenHelpP1':
        'The blue “Windows protected your PC” screen uses Microsoft Defender SmartScreen. It checks files against reputation signals (how often a file is seen, publisher identity, code signing, and more). A warning often appears for **new or rarely downloaded installers** while reputation is still building.',
      'about.smartscreenHelpP2':
        'Tapping **More info** expands the screen and usually reveals **Run anyway**. That second step is an explicit choice: you confirm you trust this run of the file on your PC.',
      'about.smartscreenHelpP3':
        'A SmartScreen prompt **does not mean** Microsoft has classified Solea Pixel as malware. It can appear even for legitimate, signed software early in its lifecycle. Reputation improves as more people install the same signed build successfully.',
      'about.smartscreenHelpP4':
        'Microsoft documents SmartScreen as part of Windows security to reduce phishing and unsafe downloads. Reading their official pages (below) is the best way to understand what the OS is checking.',
      'about.smartscreenHelpTrust':
        '<strong>Solea Pixel:</strong> download only from <strong>this website</strong> or <strong>our official GitHub releases</strong>. We ship Windows installers publicly; if anything looks off, compare the link with our Discord or open an issue on GitHub before choosing <strong>Run anyway</strong>.',
      'about.smartscreenHelpMsHeading': 'Official Microsoft resources',
      'about.smartscreenHelpLinks':
        '<ul class="help-ms-link-list"><li><a href="https://learn.microsoft.com/en-us/windows/security/threat-protection/microsoft-defender-smartscreen/microsoft-defender-smartscreen-overview" target="_blank" rel="noopener noreferrer">Microsoft Learn — Microsoft Defender SmartScreen overview</a></li><li><a href="https://support.microsoft.com/en-us/windows/stay-protected-with-windows-security-5551497d-dc1e-b22d-9667-26b8d6fa5cc8" target="_blank" rel="noopener noreferrer">Microsoft Support — Stay protected with Windows Security</a></li></ul>',
      'about.smartscreenHelpClosing':
        'If you downloaded Solea Pixel from an official channel, SmartScreen is usually a temporary hurdle while Windows learns the file — not a judgement that the app is unsafe.',
      'news.tag': 'News',
      'news.title': 'News',
      'news.lead': 'Updates for players — published on this site, separate from GitHub installer releases.',
      'news.body1':
        'This hub is where we will post news and announcements. It is <strong>not</strong> the GitHub release changelog: you can share news here without shipping a new launcher build.',
      'news.body2':
        'The Solea Pixel launcher reads this same feed in Home & news, so players see new posts in the app as soon as they are published.',
      'news.liveIntro': 'Latest posts (same JSON feed as the launcher):',
      'news.liveLoading': 'Loading news…',
      'news.liveError': 'Could not load news.',
      'news.liveEmpty': 'No posts to show yet.',
      'news.liveUpdated': 'Feed last updated: {date}',
      'engage.follow': 'Follow releases',
      'engage.followSub': 'Watch on GitHub',
      'engage.discord': 'Join Discord',
      'engage.discordSub': 'Community & support',
      'engage.bug': 'Report a bug',
      'engage.bugSub': 'GitHub Issues',
      'legal.title': 'Legal & privacy',
      'legal.mentionsTitle': 'Publisher',
      'legal.mentionsBody':
        'Solea Pixel is an independent community project. Minecraft is a trademark of Mojang AB. Solea Pixel is not affiliated with Mojang AB or Microsoft.',
      'legal.privacyTitle': 'Privacy',
      'legal.privacyBody':
        'This site does not run advertising trackers. We call GitHub’s API when you load the page <strong>only for the latest installer version</strong> on the download block, and may store your <strong>language choice</strong> locally in the browser (localStorage). No account is required to browse.',
      'legal.cookiesNote':
        'No non-essential cookies: no analytics cookie banner. If we add optional analytics later, we will ask for consent first.',
      'legal.close': 'Close',
      'legal.nav': 'Legal & privacy',
      'faq.tag': 'FAQ',
      'faq.title': 'Frequently asked questions',
      'faq.lead':
        'Launcher, server, downloads, licence, SmartScreen, collaborations — quick answers in one place.',
      'faq.q1': 'What is SOLEA PIXEL?',
      'faq.a1':
        'We are a small team of young creators in the Minecraft space. Follow us on social media or hop into Discord to be part of the journey.',
      'faq.q2': 'The launcher',
      'faq.a2':
        'Solea Pixel is tuned for our modpacks: a clean interface, rich options, and free access. Play multiple packs from one window, connect one or several Microsoft accounts, or use offline mode with a chosen username when you do not own the game.',
      'faq.q3': 'Local server',
      'faq.a3':
        'Host a modpack server on your own PC. Files stay in your Solea folder; we are working on deeper integration (including easier networking). A Playit.gg–style experience is planned so your server can get a stable **.soleapixel.net** address — more news later.',
      'faq.q4': 'Sign-in',
      'faq.a4':
        'Sign in with Microsoft for a licensed profile, or use a free display name for offline play. Microsoft login follows official OAuth practices — you can revoke access anytime from your Microsoft account settings.',
      'faq.q5': 'Downloads & updates',
      'faq.a5':
        'Windows builds are published as they are ready. macOS and Linux packages are planned. When you install, the launcher can update itself from the same release channel as this site.',
      'faq.q6': 'Mojang / Microsoft',
      'faq.a6': 'Solea Pixel is not affiliated with Mojang AB or Microsoft. Minecraft is a trademark of Mojang AB.',
      'faq.q7': 'Can I stream or make videos?',
      'faq.a7': 'Yes — we love seeing gameplay and tutorials. Credit Solea Pixel and link to the site or Discord so people can find the launcher.',
      'faq.q8': 'I’d like to collaborate',
      'faq.a8':
        'Yes — <strong>SOLEA PIXEL STUDIO</strong> keeps partnerships open: we offer a full programme, with details shared on Discord. <a href="https://discord.gg/jVGq5aZ6Wc" target="_blank" rel="noopener noreferrer">Join the server here</a>.',
      'faq.q9': 'Which platforms are supported?',
      'faq.a9':
        'Windows is our priority today. macOS and Linux are on the roadmap — follow announcements on Discord and the Downloads block on this site.',
      'faq.q10': 'Is the launcher free?',
      'faq.a10':
        'Yes — access to the launcher and the ecosystem we ship as free stays that way. Modpacks and mods remain governed by their original licences and the <a href="/license/" target="_blank" rel="noopener noreferrer">SPL</a>.',
      'faq.q11': 'Where do I report a bug or suggest a feature?',
      'faq.a11':
        'Use <a href="https://github.com/SILWOX/solea-pixel-launcher/issues/new" target="_blank" rel="noopener noreferrer">GitHub Issues</a> for reproducible bugs and code-related reports (add screenshots and your launcher version). For quick help and ideas, use <a href="https://discord.gg/jVGq5aZ6Wc" target="_blank" rel="noopener noreferrer">Discord</a>.',
      'faq.q12': 'Can I play without a Microsoft account?',
      'faq.a12':
        'For a licensed session you sign in with Microsoft. If you do not own the game, you can use offline display-name mode instead — see the launcher sign-in options. You can revoke Microsoft access anytime from your Microsoft account settings.',
      'faq.q13': 'How do I update the launcher?',
      'faq.a13':
        'Updates follow the same Windows release channel as this site. After install, the app can check for updates itself — you can also confirm your version in Settings or from the installer line on the home section.',
      'faq.q14': 'Do all modpacks come from Modrinth?',
      'faq.a14':
        'The launcher is built around Modrinth for our packs. Individual mods and assets keep their original authors’ licences — when in doubt, read each project page on Modrinth.',
      'faq.q15': 'Can I use Solea Pixel on a paid server?',
      'faq.a15':
        'Paid or monetised servers depend on how you use SOLEA PIXEL content. Read the <a href="/license/" target="_blank" rel="noopener noreferrer">SPL licence</a> (commercial use & modpack sections) before charging players or selling access.',
      'faq.q16': 'What if SmartScreen blocks the installer?',
      'faq.a16':
        'That is common for newer Windows installers. Open <a href="#about-smartscreen-title">About this project</a> and follow the SmartScreen steps there, or use the in-page help for the full explanation.',
      'license.pageTitle': 'SOLEA PIXEL — SPL License',
      'license.metaDescription':
        'Solea Pixel License (SPL) v1.0 — permissions, attribution, launcher and modpack rules. Official legal text.',
      'license.badge': 'Legal',
      'license.h1': 'Solea Pixel License (SPL)',
      'license.versionLine': 'Version 1.0 · April 2026',
      'license.tocTitle': 'On this page',
      'license.tocNav': 'License sections',
      'license.heroLede':
        'Terms for SOLEA PIXEL content, the launcher, modpacks, distribution, and commercial use — organised in clear sections below.',
      'license.sourceLabel': 'Source file',
      'license.back': 'Back to site',
      'license.source': 'View on GitHub',
      'license.toc1': 'Definitions',
      'license.toc2': 'Permissions',
      'license.toc3': 'Attribution',
      'license.toc4': 'Launcher',
      'license.toc5': 'Modpacks',
      'license.toc6': 'Mods & assets',
      'license.toc7': 'Platforms',
      'license.toc8': 'Commercial',
      'license.toc9': 'Warranty',
      'license.toc10': 'Updates',
      'license.toc11': 'Enforcement',
      'license.toc12': 'Jurisdiction',
      'license.toc13': 'Contact',
      'license.toc14': 'Final',
      'license.sec1.title': 'Definitions',
      'license.sec2.title': 'General permissions',
      'license.sec3.title': 'Attribution requirement',
      'license.sec4.title': 'Launcher restrictions',
      'license.sec5.title': 'Modpack terms',
      'license.sec6.title': 'Mods, resource packs & other content',
      'license.sec7.title': 'Authorized distribution platforms',
      'license.sec8.title': 'Commercial use policy',
      'license.sec9.title': 'No warranty (disclaimer)',
      'license.sec10.title': 'License updates',
      'license.sec11.title': 'Enforcement & violations',
      'license.sec12.title': 'Legal jurisdiction',
      'license.sec13.title': 'Contact & permissions',
      'license.sec14.title': 'Final notes',
      'footer.legal':
        'Not affiliated with Mojang AB or Microsoft · <a href="#" class="js-legal-modal-open">Legal & privacy</a> · <a href="/license/" class="footer-license-pill" target="_blank" rel="noopener noreferrer">SPL license</a> · <a href="https://discord.gg/jVGq5aZ6Wc" target="_blank" rel="noopener noreferrer">Discord</a>',
    },
    fr: {
      'page.title': 'SOLEA PIXEL',
      'meta.description':
        'BIENVENUE : installez ici notre launcher et découvrez les fonctionnalités proposées par le launcher.',
      'nav.home': 'Accueil',
      'nav.news': 'Actu',
      'nav.about': 'À propos',
      'nav.downloads': 'Téléchargements',
      'nav.faq': 'FAQ',
      'nav.skip': 'Aller au contenu',
      'nav.top': 'Haut de page',
      'nav.socialToolbar': 'Réseaux sociaux',
      'nav.ariaModrinth': 'Solea Pixel sur Modrinth',
      'nav.ariaYoutube': 'SILWOX sur YouTube',
      'nav.ariaX': 'SILWOX sur X',
      'nav.ariaDiscord': 'Solea Pixel sur Discord',
      'nav.ariaBmc': 'Soutien sur Buy Me a Coffee',
      'nav.socialShort': 'Réseaux',
      'nav.langShort': 'Langue',
      'page404.docTitle': 'SOLEA PIXEL — Page introuvable',
      'page404.metaDescription':
        'La page demandée n’existe pas sur Solea Pixel. Retour à l’accueil ou ouverture de la licence SPL.',
      'page404.skip': 'Aller au contenu',
      'page404.code': '404',
      'page404.title': 'Page introuvable',
      'page404.lead':
        'Cette adresse n’existe pas sur ce site. Vérifie l’URL ou utilise les liens ci-dessous.',
      'page404.pathLabel': 'Chemin demandé',
      'page404.homeBtn': "Retour à l'accueil",
      'page404.licenseBtn': 'Licence SPL',
      'lang.fr': 'FR',
      'lang.en': 'EN',
      'hub.tag1': '100 % gratuit',
      'hub.tag2': 'Premium & crack',
      'hub.tag3': 'Modpacks SOLEA PIXEL',
      'hub.tag4': 'Propulsé par Modrinth',
      'hub.tag5': 'Communauté',
      'hub.h1a': 'TOUT POUR JOUER AU',
      'hub.h1b': 'PACK SOLEA',
      'hub.typePrefix': "L'univers SOLEA PIXEL :",
      'hub.lead':
        'Un seul outil : actus, comptes Microsoft, modpacks NeoForge — et un espace **Mon serveur** pour faire tourner un monde chez toi, sans mélanger les fichiers.',
      'hub.btnDl': 'Télécharger',
      'hub.btnDiscover': 'Découvrir',
      'hub.versionLoading': 'Chargement de la dernière version…',
      'hub.versionSlow': 'GitHub est lent — on récupère encore la dernière version…',
      'hub.versionOk': 'Installateur Windows détecté : **{v}**.',
      'hub.versionFallback': 'Dernière version sur la page Téléchargements.',
      'tilt.1l': 'Accueil',
      'tilt.1t': 'Actus & patch notes',
      'tilt.2l': 'Réseaux',
      'tilt.2t': 'Modrinth, Discord…',
      'tilt.3l': 'Compte',
      'tilt.3t': 'Microsoft / skin',
      'meta.build': 'Build',
      'meta.platform': 'Plateforme',
      'downloads.tag': 'Téléchargements',
      'downloads.title': 'Téléchargements',
      'downloads.lead':
        'Chaîne stable pour Windows. Les autres plateformes et un canal bêta distinct arriveront quand ils seront prêts.',
      'downloads.stable': 'Stable',
      'downloads.beta': 'Bêta',
      'downloads.betaSoon': 'Pas encore de canal bêta séparé — suivez les annonces sur Discord.',
      'downloads.win': 'Windows (x64)',
      'downloads.winHint': 'x64 · installateur NSIS',
      'downloads.winBtn': "Télécharger l'installateur",
      'downloads.mac': 'macOS',
      'downloads.linux': 'Linux',
      'downloads.na': 'Pas encore disponible',
      'downloads.reqTitle': "Avant d'installer",
      'downloads.req1': 'Windows 10 ou 11 (64 bits)',
      'downloads.req2': 'Environ 400 Mo d’espace disque pour le launcher',
      'downloads.req3': 'Internet pour le premier lancement et les installs Modrinth',
      'downloads.afterTitle': 'Installation & mises à jour',
      'downloads.afterBody':
        'Les builds Windows sont signés quand ils sont publiés. Si SmartScreen s’affiche, utilise « Plus d’infos » puis « Exécuter quand même » seulement si tu fais confiance à Solea Pixel. Le launcher peut se mettre à jour via le même canal que cette page.',
      'downloads.helpTitle': 'Besoin d’aide ?',
      'downloads.helpBody':
        'Consulte la <a href="#faq">FAQ</a> ou demande sur <a href="https://discord.gg/jVGq5aZ6Wc" target="_blank" rel="noopener noreferrer">Discord</a>.',
      'about.tag': 'À propos',
      'about.title': 'Le projet derrière le launcher',
      'about.lead': 'Modpacks, communauté, support — ce que nous construisons pour les joueurs.',
      'about.c1t': 'Modpacks via Modrinth',
      'about.c1b':
        'Les modpacks sont intégrés directement au launcher depuis Modrinth : installe depuis l’appli et joue — sans bricoler les dossiers à la main.',
      'about.c2t': 'Notre Discord',
      'about.c2b':
        'Rejoins notre Discord communautaire : échange avec d’autres joueurs, partage des captures, et contacte l’assistance en direct quand tu bloques.',
      'about.c3t': "Support à l'écoute",
      'about.c3b':
        'Un souci d’installation, de crash ou de modpack ? Nous proposons plusieurs moyens de nous joindre — Discord est le plus rapide, et nous lisons chaque signalement.',
      'about.c4t': 'NeoForge & mises à jour',
      'about.c4b':
        'Pensé autour de NeoForge 1.21.1 avec des mises à jour claires : le launcher et ce site restent alignés sur les mêmes installateurs officiels.',
      'about.c5t': 'Pensé pour tous',
      'about.c5b':
        'Que tu joues en premium ou en crack, solo ou entre amis, l’interface reste lisible et les options là où tu les attends.',
      'about.c6t': 'Pourquoi Solea Pixel plutôt que le launcher par défaut ?',
      'about.c6b':
        'Le launcher officiel est idéal pour du Minecraft vanilla. Solea Pixel est pensé pour **nos modpacks NeoForge**, les installs Modrinth, les actus au même endroit, le jeu hors ligne optionnel et un flux **Mon serveur** — sans remplacer les outils Mojang, juste un parcours plus fluide pour les joueurs Solea.',
      'about.smartscreenTitle': 'Installation : « Windows a protégé votre ordinateur » (SmartScreen)',
      'about.smartscreenIntro':
        'Au lancement de l’installateur, Windows peut afficher l’écran bleu SmartScreen — c’est fréquent pour une application récente qui n’a pas encore assez de « réputation » aux yeux du filtre.',
      'about.smartscreenWhy':
        '<strong>Pourquoi ?</strong> Solea Pixel est une application récente. Microsoft affiche cet avertissement pour beaucoup de nouveaux programmes, même s’ils sont sûrs et signés.',
      'about.smartscreenHow': 'Comment installer quand même :',
      'about.smartscreenStep1Lead': 'Clique sur le lien souligné ',
      'about.smartscreenMoreBtn': 'Informations complémentaires',
      'about.smartscreenStep1Tail': ' — le même libellé que sur une Windows en français.',
      'about.smartscreenStep2':
        'Un bouton <strong>Exécuter quand même</strong> apparaît : clique dessus — seulement si tu as téléchargé Solea Pixel depuis ce site ou les releases GitHub officielles.',
      'about.smartscreenHelpTitle': '« Informations complémentaires » : que fait SmartScreen ?',
      'about.smartscreenHelpP1':
        'L’écran bleu « Windows a protégé votre ordinateur » s’appuie sur Microsoft Defender SmartScreen. Windows compare le fichier à des signaux de réputation (fréquence d’apparition, éditeur, signature, etc.). Un avertissement est fréquent pour **un installateur récent ou peu téléchargé** tant que la réputation n’est pas encore établie.',
      'about.smartscreenHelpP2':
        'En cliquant sur **Informations complémentaires**, l’écran se développe et affiche en principe **Exécuter quand même**. Ce second clic est un choix explicite : tu confirmes que tu acceptes de lancer ce fichier sur ton PC.',
      'about.smartscreenHelpP3':
        'Un message SmartScreen **ne veut pas dire** que Microsoft classe Solea Pixel comme malware. Il peut s’afficher même pour un logiciel légitime et signé au début de son cycle. La réputation s’améliore quand davantage de personnes installent la même build signée sans problème.',
      'about.smartscreenHelpP4':
        'Microsoft décrit SmartScreen dans la documentation officielle Windows comme un moyen de réduire le hameçonnage et les téléchargements dangereux. Les pages ci-dessous sont la meilleure source pour comprendre ce que le système vérifie.',
      'about.smartscreenHelpTrust':
        '<strong>Solea Pixel :</strong> télécharge uniquement depuis <strong>ce site</strong> ou <strong>nos releases GitHub officielles</strong>. Nous publions les installateurs Windows publiquement ; en cas de doute, compare l’URL avec notre Discord ou ouvre une issue GitHub avant de cliquer sur <strong>Exécuter quand même</strong>.',
      'about.smartscreenHelpMsHeading': 'Documentation officielle Microsoft',
      'about.smartscreenHelpLinks':
        '<ul class="help-ms-link-list"><li><a href="https://learn.microsoft.com/fr-fr/windows/security/threat-protection/microsoft-defender-smartscreen/microsoft-defender-smartscreen-overview" target="_blank" rel="noopener noreferrer">Microsoft Learn — Présentation de Microsoft Defender SmartScreen</a></li><li><a href="https://support.microsoft.com/fr-fr/windows/rester-prot%C3%A9-gr%C3%A2ce-%C3%A0-la-s%C3%A9curit%C3%A9-windows-5551497d-dc1e-b22d-9667-26b8d6fa5cc8" target="_blank" rel="noopener noreferrer">Support Microsoft — Rester protégé avec la sécurité Windows</a></li></ul>',
      'about.smartscreenHelpClosing':
        'Si tu as téléchargé Solea Pixel depuis une source officielle, SmartScreen est le plus souvent un passage temporaire pendant que Windows « apprend » le fichier — ce n’est pas un verdict que l’appli est dangereuse.',
      'news.tag': 'Actu',
      'news.title': 'Actu',
      'news.lead': 'Infos joueurs — publiées sur ce site, à part des releases GitHub de l’installateur.',
      'news.body1':
        'Ici on publiera les annonces et actus du projet. Ce n’est <strong>pas</strong> le journal des releases GitHub : tu peux poster une actu sans publier une nouvelle version du launcher.',
      'news.body2':
        'Le launcher Solea Pixel lit le même flux dans Accueil & actus : les joueurs voient les nouveaux messages dès leur publication.',
      'news.liveIntro': 'Dernières actus (même flux JSON que le launcher) :',
      'news.liveLoading': 'Chargement des actus…',
      'news.liveError': 'Impossible de charger les actus.',
      'news.liveEmpty': 'Aucun article pour le moment.',
      'news.liveUpdated': 'Dernière mise à jour du flux : {date}',
      'engage.follow': 'Suivre les releases',
      'engage.followSub': 'Sur GitHub',
      'engage.discord': 'Rejoindre Discord',
      'engage.discordSub': 'Communauté & aide',
      'engage.bug': 'Signaler un bug',
      'engage.bugSub': 'GitHub Issues',
      'legal.title': 'Mentions & confidentialité',
      'legal.mentionsTitle': 'Éditeur / projet',
      'legal.mentionsBody':
        'Solea Pixel est un projet communautaire indépendant. Minecraft est une marque de Mojang AB. Solea Pixel n’est pas affilié à Mojang AB ni Microsoft.',
      'legal.privacyTitle': 'Confidentialité',
      'legal.privacyBody':
        'Ce site n’utilise pas de traceurs publicitaires. Nous appelons l’API GitHub au chargement <strong>uniquement pour la ligne de version</strong> de l’installateur, et pouvons enregistrer ton <strong>choix de langue</strong> en local (localStorage). Aucun compte n’est requis pour consulter les pages.',
      'legal.cookiesNote':
        'Pas de cookies non essentiels : pas de bannière « cookies » pour de l’analyse. Si nous ajoutons un outil d’analytics optionnel plus tard, nous demanderons un consentement clair.',
      'legal.close': 'Fermer',
      'legal.nav': 'Mentions & confidentialité',
      'faq.tag': 'FAQ',
      'faq.title': 'Questions fréquentes',
      'faq.lead':
        'Launcher, serveur, téléchargements, licence, SmartScreen, collaborations — les réponses regroupées ici.',
      'faq.q1': "Qu'est-ce que SOLEA PIXEL ?",
      'faq.a1':
        'Nous sommes une petite organisation de jeunes créateurs autour de Minecraft. Rejoins-nous sur les réseaux ou directement sur Discord.',
      'faq.q2': 'Le launcher',
      'faq.a2':
        'Notre launcher Solea Pixel est optimisé pour nos modpacks : interface soignée, nombreuses options, jeu gratuit. Lance plusieurs packs depuis une même fenêtre, connecte un ou plusieurs comptes Microsoft, ou joue hors ligne avec un pseudo si tu n’as pas le jeu.',
      'faq.q3': 'Serveur local',
      'faq.a3':
        'Notre système permet d’héberger un serveur modpack sur ton PC, avec les fichiers dans ton dossier Solea. Nous préparons une intégration plus poussée (réseau simplifié) ; une expérience type Playit.gg est prévue pour obtenir une adresse **.soleapixel.net** stable — on en reparle quand ce sera prêt.',
      'faq.q4': 'Connexion',
      'faq.a4':
        'Connecte-toi avec un compte Microsoft pour un profil licence, ou utilise un nom de joueur gratuit en mode hors ligne si tu ne possèdes pas Minecraft. La connexion Microsoft suit les pratiques OAuth officielles — tu peux révoquer l’accès à tout moment depuis ton compte Microsoft.',
      'faq.q5': 'Téléchargements & mises à jour',
      'faq.a5':
        'Les builds Windows sont publiés dès qu’ils sont prêts. macOS et Linux suivront. Une fois installé, le launcher peut se mettre à jour via le même canal que ce site.',
      'faq.q6': 'Mojang / Microsoft',
      'faq.a6': 'Solea Pixel n’est pas affilié à Mojang AB ni Microsoft. Minecraft est une marque de Mojang AB.',
      'faq.q7': 'Je peux streamer ou faire des vidéos ?',
      'faq.a7':
        'Oui — on adore voir du gameplay et des tutos. Cite Solea Pixel et renvoie vers le site ou Discord pour que les gens trouvent le launcher.',
      'faq.q8': 'Je souhaite collaborer',
      'faq.a8':
        'Oui — avec <strong>SOLEA PIXEL STUDIO</strong>, nos partenariats sont très ouverts : nous proposons un programme complet, présenté sur Discord. <a href="https://discord.gg/jVGq5aZ6Wc" target="_blank" rel="noopener noreferrer">Clique ici pour rejoindre le serveur</a>.',
      'faq.q9': 'Quels systèmes sont supportés ?',
      'faq.a9':
        'Windows est la priorité aujourd’hui. macOS et Linux figurent sur la feuille de route — suis les annonces sur Discord et le bloc Téléchargements de ce site.',
      'faq.q10': 'Le launcher est-il gratuit ?',
      'faq.a10':
        'Oui — l’accès au launcher et à l’écosystème annoncé comme gratuit le reste. Les modpacks et les mods restent soumis aux licences d’origine et à la <a href="/license/" target="_blank" rel="noopener noreferrer">SPL</a>.',
      'faq.q11': 'Où signaler un bug ou une idée ?',
      'faq.a11':
        'Pour le code et les bugs reproductibles, passe par <a href="https://github.com/SILWOX/solea-pixel-launcher/issues/new" target="_blank" rel="noopener noreferrer">GitHub Issues</a> (captures d’écran + version du launcher). Pour l’aide rapide et les retours communautaires, rejoins-nous sur <a href="https://discord.gg/jVGq5aZ6Wc" target="_blank" rel="noopener noreferrer">Discord</a>.',
      'faq.q12': 'Puis-je jouer sans compte Microsoft ?',
      'faq.a12':
        'Pour une session avec licence, connecte-toi avec Microsoft. Si tu ne possèdes pas le jeu, tu peux utiliser le mode hors ligne avec un nom d’affichage — voir les options de connexion du launcher. Tu peux révoquer l’accès Microsoft à tout moment depuis ton compte Microsoft.',
      'faq.q13': 'Comment mettre à jour le launcher ?',
      'faq.a13':
        'Les mises à jour suivent le même canal Windows que ce site. Après installation, l’app peut vérifier les mises à jour elle-même — tu peux aussi vérifier ta version dans les réglages ou via la ligne d’installateur sur l’accueil.',
      'faq.q14': 'Les modpacks viennent-ils tous de Modrinth ?',
      'faq.a14':
        'Le launcher s’appuie sur Modrinth pour nos packs. Chaque mod ou asset garde la licence de ses auteurs — en cas de doute, lis la page du projet sur Modrinth.',
      'faq.q15': 'Puis-je utiliser Solea Pixel sur un serveur payant ?',
      'faq.a15':
        'Les serveurs payants ou monétisés dépendent de la façon dont tu utilises le contenu SOLEA PIXEL. Lis la <a href="/license/" target="_blank" rel="noopener noreferrer">licence SPL</a> (usage commercial & modpacks) avant de faire payer des joueurs ou de vendre un accès.',
      'faq.q16': 'Que faire si SmartScreen bloque l’installateur ?',
      'faq.a16':
        'C’est fréquent pour les installateurs récents sur Windows. Ouvre <a href="#about-smartscreen-title">À propos du projet</a> et suis la section SmartScreen, ou utilise l’aide détaillée depuis cette même zone.',
      'license.pageTitle': 'SOLEA PIXEL — Licence SPL',
      'license.metaDescription':
        'Licence Solea Pixel (SPL) v1.0 — autorisations, attribution, règles du launcher et des modpacks. Texte juridique officiel.',
      'license.badge': 'Juridique',
      'license.h1': 'Solea Pixel License (SPL)',
      'license.versionLine': 'Version 1.0 · avril 2026',
      'license.tocTitle': 'Sommaire',
      'license.tocNav': 'Sections de la licence',
      'license.heroLede':
        'Conditions pour le contenu SOLEA PIXEL, le launcher, les modpacks, la distribution et l’usage commercial — présentées en sections claires ci-dessous.',
      'license.sourceLabel': 'Fichier source',
      'license.back': 'Retour au site',
      'license.source': 'Voir sur GitHub',
      'footer.legal':
        'Non affilié Mojang AB ni Microsoft · <a href="#" class="js-legal-modal-open">Mentions & confidentialité</a> · <a href="/license/" class="footer-license-pill" target="_blank" rel="noopener noreferrer">Licence SPL</a> · <a href="https://discord.gg/jVGq5aZ6Wc" target="_blank" rel="noopener noreferrer">Discord</a>',
    },
  }

  if (typeof window !== 'undefined' && window.__SOLEA_LICENSE_FR) {
    Object.assign(STR.fr, window.__SOLEA_LICENSE_FR)
  }

  function detectBrowserLang() {
    const n = (navigator.language || navigator.userLanguage || 'en').toLowerCase()
    return n.startsWith('fr') ? 'fr' : 'en'
  }

  function getStoredLang() {
    try {
      const v = localStorage.getItem(STORAGE_KEY)
      if (v === 'en' || v === 'fr') return v
    } catch {
      /* ignore */
    }
    return null
  }

  function readLangFromUrl() {
    try {
      const q = new URLSearchParams(window.location.search).get('lang')
      if (q === 'en' || q === 'fr') return q
    } catch {
      /* ignore */
    }
    return null
  }

  function syncLangQuery(lang) {
    try {
      const u = new URL(window.location.href)
      u.searchParams.set('lang', lang)
      const next = `${u.pathname}${u.search}${u.hash || ''}`
      history.replaceState(null, '', next)
    } catch {
      /* ignore */
    }
  }

  let currentLang = readLangFromUrl() || getStoredLang() || detectBrowserLang()

  function t(key, vars) {
    let s = STR[currentLang]?.[key] ?? STR.en[key] ?? key
    if (vars) {
      for (const [k, v] of Object.entries(vars)) s = s.replaceAll(`{${k}}`, String(v))
    }
    return s
  }

  function applyMarkdownBold(el, text) {
    el.innerHTML = text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  }

  function applyI18n() {
    document.documentElement.lang = currentLang === 'fr' ? 'fr' : 'en'
    const profile = document.documentElement.dataset.i18nProfile
    const desc =
      profile === 'license'
        ? t('license.metaDescription')
        : profile === 'error404'
          ? t('page404.metaDescription')
          : t('meta.description')
    const shareTitle =
      profile === 'license'
        ? t('license.pageTitle')
        : profile === 'error404'
          ? t('page404.docTitle')
          : t('page.title')
    const meta = document.querySelector('meta[name="description"]')
    if (meta) meta.setAttribute('content', desc)
    document.querySelector('meta[property="og:description"]')?.setAttribute('content', desc)
    document.querySelector('meta[name="twitter:description"]')?.setAttribute('content', desc)
    document.querySelector('meta[property="og:title"]')?.setAttribute('content', shareTitle)
    document.querySelector('meta[name="twitter:title"]')?.setAttribute('content', shareTitle)

    document.querySelectorAll('[data-i18n]').forEach((el) => {
      const key = el.getAttribute('data-i18n')
      if (!key) return
      const val = t(key)
      if (el.tagName === 'TITLE') {
        document.title = val
        return
      }
      if (el.hasAttribute('data-i18n-html')) {
        el.innerHTML = val
        return
      }
      if (/\*\*.+\*\*/.test(val)) applyMarkdownBold(el, val)
      else el.textContent = val
    })

    /* Page licence : corps des sections — EN reste le HTML initial, FR injecté depuis STR.fr */
    document.querySelectorAll('[data-i18n-license-fr]').forEach((el) => {
      const key = el.getAttribute('data-i18n-license-fr')
      if (!key) return
      if (el.dataset.defaultBody === undefined) el.dataset.defaultBody = el.innerHTML
      if (currentLang === 'fr') {
        const html = STR.fr[key]
        if (html) el.innerHTML = html
      } else {
        el.innerHTML = el.dataset.defaultBody
      }
    })

    const licenseArticle = document.getElementById('license-doc-start')
    if (licenseArticle) {
      licenseArticle.setAttribute('lang', currentLang === 'fr' ? 'fr' : 'en')
    }

    document.querySelectorAll('[data-i18n-placeholder]').forEach((el) => {
      const key = el.getAttribute('data-i18n-placeholder')
      if (key) el.setAttribute('placeholder', t(key))
    })

    document.querySelectorAll('[data-i18n-aria]').forEach((el) => {
      const key = el.getAttribute('data-i18n-aria')
      if (key) el.setAttribute('aria-label', t(key))
    })

    const frBtn = document.getElementById('lang-fr')
    const enBtn = document.getElementById('lang-en')
    if (frBtn) {
      frBtn.classList.toggle('lang-btn--on', currentLang === 'fr')
      frBtn.setAttribute('aria-pressed', currentLang === 'fr' ? 'true' : 'false')
    }
    if (enBtn) {
      enBtn.classList.toggle('lang-btn--on', currentLang === 'en')
      enBtn.setAttribute('aria-pressed', currentLang === 'en' ? 'true' : 'false')
    }

    window.dispatchEvent(new CustomEvent('solea-lang-change', { detail: { lang: currentLang } }))
  }

  function setLang(lang) {
    if (lang !== 'en' && lang !== 'fr') return
    currentLang = lang
    try {
      localStorage.setItem(STORAGE_KEY, lang)
    } catch {
      /* ignore */
    }
    syncLangQuery(lang)
    applyI18n()
  }

  function initLangControls() {
    document.getElementById('lang-fr')?.addEventListener('click', () => setLang('fr'))
    document.getElementById('lang-en')?.addEventListener('click', () => setLang('en'))
  }

  window.SoleaI18n = {
    t,
    apply: applyI18n,
    setLang,
    getLang: () => currentLang,
    init() {
      initLangControls()
      const fromUrl = readLangFromUrl()
      if (fromUrl) {
        try {
          localStorage.setItem(STORAGE_KEY, fromUrl)
        } catch {
          /* ignore */
        }
      }
      applyI18n()
    },
  }
})()
