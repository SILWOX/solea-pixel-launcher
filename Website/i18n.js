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
      'nav.bmc': 'Better MC',
      'nav.bmcAria': 'Better Minecraft mod — teaser page',
      'nav.news': 'News',
      'nav.about': 'About',
      'nav.downloads': 'Downloads',
      'nav.faq': 'FAQ',
      'nav.content': 'Content',
      'nav.skip': 'Skip to content',
      'nav.top': 'Back to top',
      'nav.socialToolbar': 'Social links',
      'nav.ariaModrinth': 'Solea Pixel on Modrinth',
      'nav.ariaYoutube': 'SILWOX on YouTube',
      'nav.ariaX': 'SILWOX on X',
      'nav.ariaDiscord': 'Solea Pixel on Discord',
      'nav.ariaBmc': 'Support on Buy Me a Coffee',
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
      'page404.easterGoldenAria': 'Golden secret — try to catch it',
      'page404.easterTitle': 'Nice one!',
      'page404.easterLead': 'You won a reward. Tap below to claim it.',
      'page404.easterClaim': 'Claim',
      'page404.easterDismiss': 'Close',
      'page404.easterVoidReset': 'Back to the start',
      'page404.voidSequelRebuildHint': 'Drag the pieces like the home page — rebuild your layout.',
      'page404.voidSequelEndGame': 'End the game',
      'page404.voidSequelDialogTitle': 'End of the trial',
      'page404.voidSequelContinue': 'Continue',
      'page404.voidP2GoldBtn': 'Continue',
      'page404.voidP2HackTitle': 'Intercepted signal',
      'page404.voidP2LicBrand': 'SPL',
      'page404.voidP2LicGlitch': 'ERROR 404',
      'page404.voidP2WinTitle': 'Well done',
      'page404.voidP2WinBody': 'You caught the rogue 404.',
      'page404.voidP2WinOk': 'OK',
      'page404.voidP2PacTag': 'Phase 2',
      'page404.voidP2PacTitle': 'Maze run',
      'page404.voidP2PacHint':
        'Random maze each run (larger grid). Reach the green exit. Two hostile orbs at first — a third joins after 67s. If they see you in a straight line, they chase (slow steps). You have 3 lives. After 60s a few internal walls shift (exit stays open). Arrow keys or WASD.',
      'page404.voidP2PacLaunch': 'Start',
      'page404.voidP2PacRetry': 'Try again',
      'page404.voidSequelGoHome': 'Return to the site',
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
        'Discover our new launcher, optimized for all Minecraft use. You will find SOLEA PIXEL modpacks, access to vanilla Minecraft, and various other options available.',
      'hub.btnDl': 'Download',
      'hub.btnDiscover': 'Discover',
      'hub.versionLoading': 'Loading latest installer…',
      'hub.versionSlow': 'GitHub is slow — still fetching the latest installer…',
      'hub.versionOk': 'Latest Windows installer: **{v}**.',
      'hub.versionFallback': 'Latest build on the download page.',
      'tilt.pool.0.title': 'Modrinth pack',
      'tilt.pool.0.sub': 'Powered by Modrinth — install in a few clicks',
      'tilt.pool.1.title': 'Easy sign-in',
      'tilt.pool.1.sub': 'Connect your Minecraft account from the launcher',
      'tilt.pool.2.title': 'UI glow-up',
      'tilt.pool.2.sub': 'Sharper panels and calmer layouts while you play',
      'tilt.pool.3.title': 'Modpack hub',
      'tilt.pool.3.sub': 'All SOLEA packs in one place, synced from Modrinth',
      'tilt.pool.4.title': 'Offline & cracked',
      'tilt.pool.4.sub': 'Play without a Microsoft account — respect server rules and limits',
      'tilt.pool.5.title': 'Home & news',
      'tilt.pool.5.sub': 'Patch notes, links, and banners right where you launch',
      'tilt.pool.6.title': 'NeoForge-ready',
      'tilt.pool.6.sub': 'Modern pack installs without hand-editing folders',
      'tilt.pool.7.title': 'Local server slot',
      'tilt.pool.7.sub': 'A tidy space to host a world on your own PC',
      'tilt.pool.8.title': 'Quiet updates',
      'tilt.pool.8.sub': 'The launcher stays current from the same channel as this site',
      'tilt.pool.9.title': 'Discord & help',
      'tilt.pool.9.sub': 'Jump to the community when you need a hand or a hint',
      'meta.build': 'Build',
      'meta.platform': 'Platform',
      'downloads.tag': 'Get',
      'downloads.title': 'Downloads',
      'downloads.lead':
        'Stable channel: **Windows** (NSIS + portable) and **macOS** DMG (arm64 preferred, x64 when published) from the same GitHub `latest` release.',
      'downloads.stable': 'Stable',
      'downloads.beta': 'Beta',
      'downloads.betaSoon': 'No separate beta channel yet — stay tuned on Discord.',
      'downloads.win': 'Windows (x64)',
      'downloads.winHint': 'x64 · NSIS installer',
      'downloads.winBtn': 'Download installer',
      'downloads.mac': 'macOS',
      'downloads.macHint': 'Apple Silicon (arm64) DMG when attached to the release; Intel (x64) DMG also on GitHub if present.',
      'downloads.macBtn': 'Download DMG',
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
      'downloads.platformsAria': 'Download options by platform',
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
      'news.lead': '',
      'news.body1': '',
      'news.body2': '',
      'news.liveIntro': '',
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
      'content.pageTitle': 'SOLEA PIXEL — Content hub',
      'content.metaDescription':
        'Every Modrinth project from SOLEA PIXEL in one place: team members, modpacks, mods, resource packs, and quick links to each project page.',
      'content.h1': 'Content hub',
      'content.eyebrow': 'Modrinth · catalogue',
      'content.membersSub': 'People behind the organisation — profiles open on Modrinth.',
      'content.projectsSub':
        'Browse by category — modpacks, mods, resource packs, then shaders & plugins. Each card links to the official Modrinth project page.',
      'content.tocNav': 'Browse project categories',
      'content.tocLabel': 'Jump to',
      'content.railTitle': 'On this page',
      'content.railHero': 'Top',
      'content.railTeam': 'Team',
      'content.railCatalog': 'Catalogue',
      'content.mainMore': 'Shaders, plugins & more',
      'content.catDescModpack':
        'Installable packs (.mrpack) — ideal with the Solea Pixel launcher or any Modrinth-compatible client.',
      'content.catDescMod':
        'Gameplay, content, and utility mods: new mechanics, mobs, gear, and quality-of-life.',
      'content.catDescResourcepack':
        'Textures, models, and ambience — change how the game looks without new code on the server.',
      'content.catDescMore':
        'Shaders, plugins, data packs, and other formats grouped here so the three main lanes stay easy to scan.',
      'content.lead':
        'Live catalogue of the <a href="https://modrinth.com/organization/soleapixel">SOLEA PIXEL</a> organisation on Modrinth — members and every public project, grouped by type.',
      'content.orgCta': 'Open organisation on Modrinth',
      'content.members': 'Team',
      'content.projects': 'Projects',
      'content.loading': 'Loading Modrinth data…',
      'content.error': 'Could not load organisation data. Try again in a moment.',
      'content.empty': 'No projects in this view.',
      'content.statsDl': '{n} downloads',
      'content.statsFollow': '{n} followers',
      'content.view': 'View',
      'content.download': 'Download',
      'content.type.modpack': 'Modpacks',
      'content.type.mod': 'Mods',
      'content.type.resourcepack': 'Resource packs',
      'content.type.shader': 'Shaders',
      'content.type.plugin': 'Plugins',
      'content.type.datapack': 'Data packs',
      'content.type.other': 'Other',
      'content.backHome': '← Home',
      'content.sitemap': 'Site map & links',
      'content.footerLicense': 'SPL license',
      'betterminecraft.pageTitle': 'Better Minecraft (BMC) — SOLEA PIXEL',
      'betterminecraft.metaDescription':
        'Better Minecraft (BMC) for NeoForge 1.21.1: vanilla+ survival expansion — gear, infusion, biomes, goals. Long-form teaser by SOLEA PIXEL; downloads on Modrinth.',
      'betterminecraft.heroBadge': 'NeoForge · Minecraft Java 1.21.1 · Modrinth',
      'betterminecraft.heroTitle': 'Better Minecraft',
      'betterminecraft.heroSubtitle': '[ BMC MOD ]',
      'betterminecraft.heroLead':
        'Vanilla+ survival you can read at a glance: new goals, crafted pacing, and atmosphere that still feels like Minecraft.',
      'betterminecraft.introTitle': 'What is Better Minecraft?',
      'betterminecraft.introLead':
        'Better Minecraft (**BMC**) is a **NeoForge** mod for **Java 1.21.1** that stretches survival with handcrafted content — not random clutter, not a tech-tree simulator.',
      'betterminecraft.introP1':
        'We design around one question: **would this feel at home in a Mojang changelog?** That means readable progression, readable UI clutter (we play with **JEI** / **Jade** in mind), and features that reward planning — new tiers, new reasons to explore, and small loops that sit beside vanilla instead of replacing it.',
      'betterminecraft.introP2':
        'You will find **gear lines** (including enderite & emerald direction), **infusion-style crafting hooks**, **enchantments & systems** that extend the mid/late game, and **world flavour** that pushes the End & Overworld forward over time. Exact recipes and numbers evolve with the overhaul — the Modrinth changelog stays the source of truth.',
      'betterminecraft.introP3':
        'BMC is meant for **modpack authors** who want “more Minecraft” without teaching players a second game, and for **solo worlds** where you want fresh milestones after the dragon without installing twenty unrelated mods.',
      'betterminecraft.pillarsTitle': 'Three big selling points',
      'betterminecraft.pillar1Title': 'Modpack-friendly',
      'betterminecraft.pillar1Body':
        'Built to **slot into packs** you already play: plays nice with staples like **JEI** and **Jade**, and does not fight the rest of your mod list for attention.',
      'betterminecraft.pillar2Title': 'Vanilla-like',
      'betterminecraft.pillar2Body':
        'Adds goals, gear, and world flavour that still **feel like Minecraft** — closer to a chunky vanilla update than a whole new game.',
      'betterminecraft.pillar3Title': 'Not too complex',
      'betterminecraft.pillar3Body':
        'No homework wall: **readable progression**, systems you can explain in one breath, and fewer “learn my entire tech tree first” moments.',
      'betterminecraft.detailsTitle': 'Concrete directions (overview)',
      'betterminecraft.detailsHtml':
        '<ul class="bmc-detail-list"><li><strong>Gear &amp; materials</strong> — new lines to plan around (enderite &amp; emerald are headline examples), with room for polish as the 1.21.1 rewrite lands.</li><li><strong>Infusion &amp; crafting goals</strong> — extra reasons to farm, explore, and revisit the world between two bases.</li><li><strong>Enchantments &amp; small mechanics</strong> — hooks that stretch combat and preparation without replacing core balance wholesale.</li><li><strong>Biomes &amp; ambience</strong> — End &amp; Overworld treated as first-class spaces; specifics shift version to version — follow the changelog.</li><li><strong>Documentation</strong> — release notes on Modrinth; deeper wiki pages appear as the overhaul stabilises.</li></ul>',
      'betterminecraft.fitTitle': 'Who should install it?',
      'betterminecraft.fitP1':
        'Perfect if you want **more checklist after diamond** — new stations to build, new routes to optimise, new excuses to wander the map — while keeping the “Minecraft brain” you already have.',
      'betterminecraft.fitP2':
        'Less ideal if you are looking for **factory automation**, **magic tech trees**, or **dimension packs** that ignore the base game. BMC hugs vanilla; it does not replace it.',
      'betterminecraft.featuresTitle': 'Feature spotlight',
      'betterminecraft.feat1Title': 'Gear that paces you',
      'betterminecraft.feat1Body':
        'Enderite & emerald style lines give you **long-term projects** without invalidating iron or diamond overnight. Stats and recipes are tuned during the NeoForge migration — check Modrinth for the latest numbers.',
      'betterminecraft.feat2Title': 'Infusion & crafting identity',
      'betterminecraft.feat2Body':
        'Infusion-style workflows tie late-game materials to **meaningful choices** — what to charge first, what to farm next — instead of a single OP craft.',
      'betterminecraft.feat3Title': 'Worlds that grow with you',
      'betterminecraft.feat3Body':
        'End & Overworld content is built to **reward return trips**: new sights, new reasons to bring shulker boxes, and ambience that reads “expansion pack” rather than “random biome mod”.',
      'betterminecraft.feat4Title': 'Systems, not noise',
      'betterminecraft.feat4Body':
        'Enchantments and light-touch mechanics extend survival loops — **sidegrades, trade-offs, preparation** — instead of stacking unrelated gimmicks.',
      'betterminecraft.feat5Title': 'Vanilla+ promise',
      'betterminecraft.feat5Body':
        'If a feature cannot be explained to a returning player in one sentence, it probably does not belong here. BMC is the “**wider sandbox**” lane: more goals, same vocabulary.',
      'betterminecraft.compatTitle': 'Compatibility',
      'betterminecraft.compatBody': 'Minecraft **Java 1.21.1** · **NeoForge** · runs on **client & dedicated server**.',
      'betterminecraft.compatNote':
        'Mod loaders change fast — always match the **NeoForge** version range listed on the Modrinth file you download. When in doubt, ask on Discord with your latest log.',
      'betterminecraft.linksTitle': 'Official links',
      'betterminecraft.linkProject': 'Modrinth — project page',
      'betterminecraft.linkChangelog': 'Changelog & files',
      'betterminecraft.linkOrgShort': 'SOLEA PIXEL org',
      'betterminecraft.linkDiscord': 'Community Discord',
      'betterminecraft.ctaModrinth': 'Open on Modrinth',
      'betterminecraft.ctaOrg': 'SOLEA PIXEL on Modrinth',
      'betterminecraft.footerNote': 'Art & branding belong to their respective owners. Not an official Minecraft service.',
      'betterminecraft.backHome': '← Back to home',
      'betterminecraft.showcaseTitle': 'Textures & gear (preview)',
      'betterminecraft.showcaseHint': 'Hover a card (or focus it) to read the blurb — on touch devices the text stays visible.',
      'betterminecraft.showcaseArmorFloatNote':
        '**Emerald** (left edge) and **enderite** (right edge) spin in **3D** like in-game — soft background preview (handy for a wiki layout too).',
      'betterminecraft.showcase1Name': 'Nebrith block',
      'betterminecraft.showcase1Desc':
        'Found in **End geodes**: a distinctive material and a great-looking decorative block.',
      'betterminecraft.showcase2Name': 'Ruby ore',
      'betterminecraft.showcase2Desc':
        'Rubies are rare gems mainly used for **special trades** with the **Cured Witch**.',
      'betterminecraft.showcase3Name': 'Enderite block',
      'betterminecraft.showcase3Desc':
        'Even rarer than Netherite: crafted from **nine enderite ingots** and genuinely hard to obtain.',
      'betterminecraft.showcase4Name': 'Dragon staff',
      'betterminecraft.showcase4Desc':
        'A magic staff that channels the **Ender Dragon’s** power as its energy source.',
      'betterminecraft.showcase5Name': 'Echo staff',
      'betterminecraft.showcase5Desc':
        'The **Warden** is no longer the only keeper of that power — the Echo staff wields it too.',
      'betterminecraft.showcase6Name': 'Enderite sword',
      'betterminecraft.showcase6Desc':
        'Stronger and tougher than Netherite — a new emblem for your adventure.',
      'betterminecraft.showcase7Name': 'Lightning bow',
      'betterminecraft.showcase7Desc':
        'True to its name: **tighter shots**, **longer range**, and a **powerful lightning strike** on impact. Do not stand in the way.',
      'betterminecraft.showcase8Name': 'Liquid experience',
      'betterminecraft.showcase8Desc':
        'Liquid XP found in the End — **not quite** what you might expect.',
      'betterminecraft.showcase9Name': 'Emerald armor',
      'betterminecraft.showcase9Desc':
        'No stronger than diamond, but the **full set** has an edge — equip it all and **visit the villagers**.',
      'betterminecraft.showcase10Name': 'Enderite armor',
      'betterminecraft.showcase10Desc':
        'Dark **purple** and true to its name — tougher than older armor that is starting to feel dated.',
      'betterminecraft.showcase11Name': 'Soul crystal',
      'betterminecraft.showcase11Desc':
        'Whatever you think, you are wrong! **Trap souls** in the crystal and use them at the **Infusion Table** — pure sorcery.',
      'bmcwiki.pageTitle': 'Better Minecraft Wiki (draft) — SOLEA PIXEL',
      'bmcwiki.metaDescription':
        'Draft BMC wiki: category tree only — **not indexed** and **not intended** for the next public site update until you enable it.',
      'bmcwiki.draftBadge': 'Draft · not for next release',
      'bmcwiki.draftBody':
        'This URL is **noindex** and listed as **Disallow** in `robots.txt` so search engines skip it. Remove those guards and add real content when you are ready to publish.',
      'bmcwiki.mainTitle': 'Better Minecraft wiki',
      'bmcwiki.mainLead':
        'Articles synced from the **Better Minecraft** mod sources in this repo: armor tiers, wood sets, storage, infusion, mobs, and utility items. **Correct anything** that drifts from your build.',
      'bmcwiki.quickNavAria': 'Quick jumps within this wiki page',
      'bmcwiki.quickNavLabel': 'Jump',
      'bmcwiki.quickNavInfusion': 'Infusion',
      'bmcwiki.quickNavEnd': 'End & ruby',
      'bmcwiki.quickNavArmor': 'Enderite gear',
      'bmcwiki.quickNavItems': 'Backpacks',
      'bmcwiki.quickNavStorage': 'Storage',
      'bmcwiki.navLabel': 'Contents',
      'bmcwiki.sidebarAria': 'Wiki categories',
      'bmcwiki.navTreeAria': 'Category tree',
      'bmcwiki.sidebarToggle': 'Show / hide categories',
      'bmcwiki.sidebarToggleAria': 'Toggle the wiki category panel',
      'bmcwiki.catArmor': 'Armor & tools',
      'bmcwiki.linkArmorEnderite': 'Enderite',
      'bmcwiki.linkArmorEmerald': 'Emerald',
      'bmcwiki.linkArmorCopper': 'Copper',
      'bmcwiki.linkArmorObsidian': 'Obsidian',
      'bmcwiki.linkArmorShulker': 'Shulker',
      'bmcwiki.linkArmorHats': 'Villager hats & witch',
      'bmcwiki.catBlocks': 'Blocks',
      'bmcwiki.linkNebrith': 'Nebrith',
      'bmcwiki.linkInfusionTable': 'Infusion table',
      'bmcwiki.linkBlocksStone': 'Light & dark stone',
      'bmcwiki.linkBlocksEnd': 'End ores & ruby',
      'bmcwiki.linkBlocksStorage': 'Enchanted chest & furnace',
      'bmcwiki.linkBlocksHollow': 'Hollow wood set',
      'bmcwiki.linkBlocksSunwood': 'Sunwood & Solea Grove',
      'bmcwiki.catItems': 'Items',
      'bmcwiki.linkItemEnderite': 'Enderite (materials)',
      'bmcwiki.linkItemCrystals': 'Soul crystals',
      'bmcwiki.linkItemLightningBow': 'Lightning bow',
      'bmcwiki.linkAllStaff': 'All staves',
      'bmcwiki.linkItemBackpacks': 'Backpacks',
      'bmcwiki.linkItemFood': 'Berries & apples',
      'bmcwiki.linkItemUtility': 'Utility & drops',
      'bmcwiki.catMobs': 'Mobs',
      'bmcwiki.linkMobsOverview': 'Other mod mobs',
      'bmcwiki.linkRadiantSlime': 'Radiant slime',
      'bmcwiki.linkMetaRarity': 'Rarity display',
      'bmcwiki.linkProgressFossil': 'Fossil debris & XP',
      'bmcwiki.catEnchants': 'Enchantments & effects',
      'bmcwiki.catWorld': 'Biomes & structures',
      'bmcwiki.linkWorldBiomes': 'Hollow Garden & Solea Grove',
      'bmcwiki.catProgress': 'Progression & goals',
      'bmcwiki.catSoon': 'Sub-pages to add later.',
      'bmcwiki.titleItemEnderite': 'Items: Enderite (materials)',
      'bmcwiki.itemEnderiteIntro':
        '**Enderite** is an End-tier material: scrap from **Forgotten Debris** (smelt or blast), combine with diamonds into ingots, then compress into a block or duplicate the **smithing template**.',
      'bmcwiki.enderiteScrapSmelt':
        'Smelt or blast **Forgotten Debris** in a furnace — same timing as common ore smelts — to get **Enderite Scrap**.',
      'bmcwiki.armorEnderiteIntro':
        '**Enderite armor and tools** are forged from **Netherite** at the smithing table using the upgrade template and **Enderite Ingots** (see **Items: Enderite** for how to make ingots).',
      'bmcwiki.xrefToArmorEnderite': '→ Enderite armor & tools',
      'bmcwiki.xrefToItemEnderite': '→ Enderite materials',
      'bmcwiki.xrefToInfusion': '→ Infusion table',
      'bmcwiki.xrefToArmorEmerald': '→ Emerald armor',
      'bmcwiki.xrefToCrystals': '→ Soul crystals',
      'bmcwiki.xrefEmeraldBootsInfusion': '→ Sky boots (infusion)',
      'bmcwiki.xrefEmeraldBootsCraft': '→ Emerald armor (boots)',
      'bmcwiki.xrefRubyOres': '→ End & ores (ruby)',
      'bmcwiki.xrefToInfusionRecipe': '→ Infusion recipe',
      'bmcwiki.xrefNetheriteForBow': '→ Netherite (smithing)',
      'bmcwiki.xrefStaffInfusion': '→ Infusion recipes for staves',
      'bmcwiki.xrefNebrithInfusion': '→ Infusion (uses Nebrith shards in other crafts)',
      'bmcwiki.xrefPocketEnderInfusion': '→ Pocket ender chest (infusion)',
      'bmcwiki.xrefToBackpacks': '→ Backpacks',
      'bmcwiki.xrefToFossil': '→ Fossil debris & XP',
      'bmcwiki.xrefToWorldBiomes': '→ Biomes (worldgen)',
      'bmcwiki.xrefToUtility': '→ Utility & drops',
      'bmcwiki.xrefToRadiant': '→ Radiant slime',
      'bmcwiki.xrefWardenTendril': '→ Warden tendril (drops)',
      'bmcwiki.titleArmorObsidian': 'Armor: Obsidian',
      'bmcwiki.bodyArmorObsidian':
        'Full **obsidian** armor set (helmet through boots) — **Epic** tier gear registered in `ModItems` with its own material stats; craft like other armor tiers (see **JEI** / in-game recipes).',
      'bmcwiki.titleArmorShulker': 'Armor: Shulker',
      'bmcwiki.bodyArmorShulker':
        '**Shulker** armor is a four-piece **Epic** set themed on shulker shells — separate progression path from emerald or enderite.',
      'bmcwiki.titleArmorHats': 'Armor: Villager hats & witch hat',
      'bmcwiki.bodyArmorHats':
        'Cosmetic-style **helmet** slots for villager jobs (butcher, librarian, weaponsmith, shepherd, fisherman, cartographer, armorer, farmer) plus a **Witch Hat** with extra behaviour (soul storage line in tooltips — see `WitchHatItem`).',
      'bmcwiki.titleItemBackpacks': 'Items: Backpacks',
      'bmcwiki.bodyItemBackpacks':
        'Tiered **backpack** line: leather → iron → gold → diamond → emerald (rising rows and **rarity**). Datapack-friendly tag **`bmcmod:backpack`**; **Shift** shows a contents preview in tooltips. Used as an ingredient on the **Pocket Ender Chest** infusion card.',
      'bmcwiki.titleItemFood': 'Items: Berries, apples & XP bottle',
      'bmcwiki.bodyItemFood':
        '**Purple berries** drop from the **Purple Berry Bush** in Hollow Garden biomes (food + short regen). **Gold berries** are a high-tier reward food. **Diamond apple** and **Enchanted diamond apple** are custom gapple-style items with long timed effects. **Sealed experience bottle** stores XP in bottle form (see item class in the mod).',
      'bmcwiki.titleItemUtility': 'Items: Utility & rare drops',
      'bmcwiki.bodyItemUtility':
        '**Void shard** — End-related consumable/stackable utility (`VoidShardItem`). **Melody Horn** — hold to play looping tunes. **Unknown Book** — rare single-stack book. **Music Disc** — *Beyond The Enderman* (`beyond_the_enderman` jukebox song). **Experience liquid bucket** — fluid bucket from mod fluids. **Warden tendril** — Warden loot (loot modifier in code). **End Golem Heart** — mythic drop tied to the End Golem. **Fossil scrap** — exotic material from **Fossil Debris**.',
      'bmcwiki.titleBlocksStorage': 'Blocks: Enchanted chest & Endstone furnace',
      'bmcwiki.bodyBlocksStorage':
        '**Enchanted chest** stores **XP** with HUD lines for upgrades (max **3** upgrades) and rolling gain; **Enchanted Chest Upgrade** is consumed with a cap. **Endstone furnace** is a themed furnace block (`EndstoneFurnaceBlock`). Recipes live under `data/bmcmod/recipe/`.',
      'bmcwiki.titleBlocksHollow': 'Blocks: Hollow wood (Hollow Garden)',
      'bmcwiki.bodyBlocksHollow':
        'Full **hollow** wood family: logs, stripped variants, **hollow planks**, stairs, slabs, fence, gate, door, trapdoor, pressure plate, button, **barrel**, **bookshelf**, **leaves**, **sapling**. Grows in **Hollow Garden**–style biomes alongside **hollow grass** and **purple berry** bushes.',
      'bmcwiki.titleBlocksSunwood': 'Blocks: Sunwood (Solea Grove)',
      'bmcwiki.bodyBlocksSunwood':
        '**Sunwood** log / wood / planks and building set (stairs, slabs, fence, gate, door, trapdoor, plates, buttons), **signs** and **hanging signs**, **leaves**, **sapling**, decorative **sunbloom** and **surface moss**, plus **Sunwood boat** and **chest boat**.',
      'bmcwiki.titleMetaRarity': 'UI: Rarity tiers & tooltips',
      'bmcwiki.bodyMetaRarity':
        'The mod defines extra **BmcMod** rarity tiers (Uncommon through **Fragmented**) used for borders and tooltips (`BmcModRarity`, client `RarityTooltipHandler`). **Rarity sticks** in `ModItems` are **debug / test** items for previewing those tiers — not progression gear.',
      'bmcwiki.titleProgressFossil': 'Progression: Fossil debris & liquid XP',
      'bmcwiki.bodyProgressFossil':
        '**Fossil Debris** is a very tough underground ore (`ModBlocks.FOSSIL_DEBRIS`) with deep-world generation; it drops toward **fossil scrap**. **Liquid experience** generates in the End and feeds the **Radiant slime** ecosystem — see worldgen under `data/bmcmod/worldgen` and fluid registration in code.',
      'bmcwiki.copperFullBody':
        'The **full copper armor** and **tool set** (sword through hoe) exists alongside the helmet; stats sit between stone and iron as in `CopperEquipment`.',
      'bmcwiki.itemCrystalsMorph':
        '**Morph crystal** — kill a mob with the crystal in your **off-hand** to store its form; **hold use** to morph (soul consumed). **Capture crystal** — similar flow for capture gameplay with higher durability pool. Both are **infusion** crafts once you have a primed **soul crystal**.',
      'bmcwiki.enderiteScrapTitle': 'Enderite scrap',
      'bmcwiki.enderiteScrapBody':
        'Mine **Forgotten Debris** in the End (similar vein feel to Ancient Debris). Smelt or blast the ore block to get **Enderite Scrap**.',
      'bmcwiki.enderiteIngotTitle': 'Enderite ingot (shapeless)',
      'bmcwiki.craftShapelessEnderiteIngotAria':
        'Shapeless recipe (shown in a 3×3 grid): four Enderite Scrap and four Diamonds → one Enderite Ingot — ingredients can be placed in any arrangement.',
      'bmcwiki.legendEnderiteIngot':
        'Vanilla **diamond** textures from Minecraft **1.21.1** assets (community mirror). Mod textures copied from your `bmcmod` pack.',
      'bmcwiki.enderiteBlockTitle': 'Enderite block',
      'bmcwiki.craftEnderiteBlockAria': 'Shaped 3×3: nine Enderite Ingots to one Enderite Block',
      'bmcwiki.legendEnderiteBlock': 'Nine **Enderite Ingots** → one **Enderite Block** (storage).',
      'bmcwiki.enderiteTemplateTitle': 'Enderite upgrade template (duplicate)',
      'bmcwiki.enderiteTemplateBody':
        'You need the **Enderite Upgrade Smithing Template** for smithing. Duplicate it like other templates: one template plus End Stone and diamonds yields **two** templates.',
      'bmcwiki.craftTemplateDupAria': 'Shaped recipe to duplicate the Enderite upgrade smithing template',
      'bmcwiki.legendTemplateDup':
        'One **Enderite Upgrade Template**, **End Stone** centre, **diamonds** around — output **two** templates.',
      'bmcwiki.enderiteSmithingTitle': 'Smithing (Netherite → Enderite)',
      'bmcwiki.enderiteSmithingBody':
        'At a **Smithing Table**: place the template, a **Netherite** tool or armor piece, and an **Enderite Ingot** in the addition slot.',
      'bmcwiki.smithingEnderiteAria': 'Smithing transform layout: template plus Netherite base plus Enderite ingot',
      'bmcwiki.smithSlotTemplate': 'Template',
      'bmcwiki.smithSlotBase': 'Base',
      'bmcwiki.smithSlotAdd': 'Add',
      'bmcwiki.legendSmithingLetters':
        '**Smithing table**: template + any **Netherite** piece + **Enderite Ingot** → matching **Enderite** item (example shown: **Netherite Sword** → **Enderite Sword**).',
      'bmcwiki.enderiteSmithingList1':
        '**Tools & weapons**: same pattern for sword, pickaxe, axe, shovel, and hoe smithing recipes.',
      'bmcwiki.enderiteSmithingList2':
        '**Armor & shields**: helmet, chestplate, leggings, boots, and the **Enderite Shield** use the same transform; shields follow the same smithing flow as other gear.',
      'bmcwiki.emeraldIntro':
        '**Emerald** tools and armor use emeralds as material. Gear is tuned as a **diamond-sidegrade** with slightly higher protection totals and repair/emerald theme.',
      'bmcwiki.emeraldSetTitle': 'Full set bonus',
      'bmcwiki.emeraldSetBody':
        'With **all four** emerald armor pieces equipped, you keep **Hero of the Village I** with infinite duration — ideal for **villager trading** runs.',
      'bmcwiki.emeraldToolsBody':
        'Tools use the mod’s emerald tier (crafted like vanilla tier gear, replacing the base material with emeralds — see in-game recipes / JEI).',
      'bmcwiki.copperIntro':
        '**Copper** gear fills an early slot: tools sit **between stone and iron**; armor totals are **slightly below iron** but cheap to craft with copper ingots and nuggets.',
      'bmcwiki.copperHelmetTitle': 'Copper helmet (storms)',
      'bmcwiki.copperHelmetBody':
        'During **thunderstorms**, while it is **raining**, you can see the **sky**, and you wear the **copper helmet**, the mod can spawn **lightning** at random nearby surface positions (rare tick).',
      'bmcwiki.nebrithIntro':
        '**Nebrith** is a purple crystal system: **buds** grow from **Budding Nebrith** into clusters. Break clusters for **shards**; shards craft storage blocks and several devices.',
      'bmcwiki.nebrithShardsTitle': 'Shards & growth',
      'bmcwiki.nebrithShardsBody':
        'Clusters behave like **amethyst**: correct tool, Silk Touch optional depending on drops. Shards are the currency for nebrith-themed crafts (block, spyglass, tinted glass, etc.).',
      'bmcwiki.nebrithBlockTitle': 'Nebrith block',
      'bmcwiki.craftNebrithBlockAria': 'Four Nebrith Shards in a 2×2 pattern (shown inside a 3×3 crafting grid) → one Nebrith Block',
      'bmcwiki.legendNebrithBlock': 'Four **Nebrith Shards** (2×2) → **Nebrith Block**.',
      'bmcwiki.relicTableTitle': 'Relic table',
      'bmcwiki.craftRelicTableAria': 'Shaped recipe: Purpur, End Stone, central Nebrith Shard to Relic Table',
      'bmcwiki.legendRelicTable':
        '**Purpur**, **End Stone**, one **Nebrith Shard** in the centre → **Relic Table** (top texture shown as result icon).',
      'bmcwiki.infusionIntroFull':
        'The **Infusion Table** has a GUI: place a **Soul Crystal** in its dedicated slot, stack the **four ingredient types** in the 2×2 grid (counts can be split across slots; only items that belong to the recipe may appear — see `InfusionRecipe.java`). When the recipe matches, **souls stored in the crystal** pay the **soul cost** and the result appears in the output slot. Recipes are registered in **`InfusionRecipes.java`** (multiset / shapeless logic).',
      'bmcwiki.infusionRecipesTitle': 'Infusion recipes (in-game)',
      'bmcwiki.infusionRecipesLead':
        'Each card lists **total counts** across the four slots. **Order does not matter** as long as no extra item types are present.',
      'bmcwiki.infuseLightningBowTitle': 'Lightning Bow',
      'bmcwiki.infuseLightningBowSoul': 'Soul cost: **72**',
      'bmcwiki.infuseMorphTitle': 'Morph Crystal',
      'bmcwiki.infuseMorphSoul': 'Soul cost: **56**',
      'bmcwiki.infuseCaptureTitle': 'Capture Crystal',
      'bmcwiki.infuseCaptureSoul': 'Soul cost: **56**',
      'bmcwiki.infuseSkyBootsTitle': 'Sky Boots',
      'bmcwiki.infuseSkyBootsSoul': 'Soul cost: **120**',
      'bmcwiki.infuseEchoStaffTitle': 'Echo Staff',
      'bmcwiki.infuseEchoStaffSoul': 'Soul cost: **120**',
      'bmcwiki.infuseFlameStaffTitle': 'Flame Staff',
      'bmcwiki.infuseFlameStaffSoul': 'Soul cost: **100**',
      'bmcwiki.infuseEndStaffTitle': 'End Staff',
      'bmcwiki.infuseEndStaffSoul': 'Soul cost: **110**',
      'bmcwiki.infuseDragonStaffTitle': 'Dragon Staff',
      'bmcwiki.infuseDragonStaffSoul': 'Soul cost: **115**',
      'bmcwiki.infusePocketEnderTitle': 'Pocket Ender Chest',
      'bmcwiki.infusePocketEnderSoul': 'Soul cost: **5**',
      'bmcwiki.titleItemCrystals': 'Items: Soul crystals',
      'bmcwiki.itemCrystalsIntro':
        'The **Soul Crystal** captures souls (off-hand flow in the mod). **Morph** and **Capture** crystals are high-tier crafts at the infusion table.',
      'bmcwiki.itemCrystalsBody':
        '**Cracked crystal** appears when durability is exhausted. Uses tie into the **Infusion Table** and metamorphosis systems.',
      'bmcwiki.titleItemLightningBow': 'Items: Lightning Bow',
      'bmcwiki.itemLightningBowIntro':
        'The **Lightning Bow** is an **Exotic** ranged weapon: tighter shots, long range, and lightning on impact (see in-game tooltip).',
      'bmcwiki.itemLightningBowBody':
        'Crafted only through **infusion** — see the recipe card below and the **Netherite** link for ingots used in the recipe.',
      'bmcwiki.titleBlocksStone': 'Blocks: Light & dark stone',
      'bmcwiki.blocksStoneBody':
        '**Light stone** and variants spawn in the **Overworld**; **dark stone** and polished variants align with **Nether blackstone** rules. Stairs, slabs, walls, buttons, and pressure plates follow vanilla stonecutting / crafting patterns (see JEI).',
      'bmcwiki.titleBlocksEnd': 'Blocks: End & ruby',
      'bmcwiki.blocksEndBody':
        '**Forgotten Debris** (End, high toughness) → **Enderite Scrap**. **Ruby ore** + **deepslate ruby ore** in the Overworld → **ruby** gem and **block of ruby**. **End sand**, **End anchor** (respawn-anchor rules, End-only validity), **void torch** / **void lantern**, **Jaerys** plant, and **hollow grass** (Hollow Garden ground) complete the surface kit shown above.',
      'bmcwiki.titleMobsOverview': 'Mobs: overview',
      'bmcwiki.mobsOverviewIntro': 'Spawn eggs and custom entities registered by the mod (names from `en_us.json`):',
      'bmcwiki.mobBlink': '**Blink** — End-themed mob (spawn egg available).',
      'bmcwiki.mobEndGolem': '**End Golem** — heavy End guardian (spawn egg).',
      'bmcwiki.mobEndling': '**Endling** — End creature (spawn egg).',
      'bmcwiki.mobDiamondGolem': '**Diamond Golem** — golem variant (spawn egg).',
      'bmcwiki.mobIllusioner': '**Illusioner** — vanilla mob type exposed with a spawn egg in the mod.',
      'bmcwiki.mobChargedCreeper': '**Charged Creeper** — spawn egg for testing or creative.',
      'bmcwiki.titleWorldBiomes': 'World: biomes',
      'bmcwiki.worldBiomesBody':
        'Datapack biomes include **Hollow Garden** (hollow wood, hollow grass, purple berries, End-adjacent feel) and **Solea Grove** (sunwood, sunbloom, surface moss). See `data/bmcmod/worldgen/biome` and `placed_feature` / `configured_feature` trees plus lang keys `biome.bmcmod.*`.',
      'bmcwiki.craftInfusionAria': 'Shaped crafting recipe for the Infusion Table',
      'bmcwiki.legendInfusion':
        '**Book**, **Emeralds**, **End Stone Bricks** as in the shaped recipe → **Infusion Table** (top texture as icon).',
      'bmcwiki.infusionNote':
        'Soul totals and ingredients follow **`InfusionRecipes.java`** in your mod copy — rebalance there if you change costs.',
      'bmcwiki.allStaffIntro':
        'Magic **staves** use durability / charges; several support **Ctrl + use** to **cycle modes** (see item tooltips in-game).',
      'bmcwiki.staffDragon':
        '**Dragon Staff** — Mode 1: charge and release a **dragon fireball** toward your aim. Mode 2: hold for a **breath beam** along your line of sight. **Ctrl + use** switches mode.',
      'bmcwiki.staffEcho':
        '**Echo Staff** — **Six** sonic charges. Hold use about **five seconds**, then **release** to fire a **sonic**-style blast (Warden-inspired).',
      'bmcwiki.staffFlame':
        '**Flame Staff** — Hold use: **flamethrower** in view direction; burning enemies take **bonus damage**. Durability **drains while firing**.',
      'bmcwiki.staffEnd':
        '**End Staff** — Three modes: **charged ender pearl**, **target warp** (creature in line of sight), **random escape** (enderman-style). **Ctrl + use** cycles modes; pearl mode uses hold → release.',
      'bmcwiki.radiantIntro':
        '**Radiant Slime** is a custom slime linked to **liquid experience** in the End ecosystem.',
      'bmcwiki.radiantBullet1': 'Tougher than ordinary slimes; interacts with **player experience** (steals XP in combat context).',
      'bmcwiki.radiantBullet2':
        'Spawn logic ties to **night / lake** style surface spawns near XP fluid; some spawns **return to the lake at dawn** (see mod spawn logic).',
      'bmcwiki.radiantBullet3': 'Spawn egg exists for creative / testing; world placement follows the mod’s configured rules.',
      'bmcwiki.backBmc': '← Back to BMC teaser',
      'bmcwiki.backHome': '← Home',
      'footer.legal':
        'Not affiliated with Mojang AB or Microsoft · <a href="#" class="js-legal-modal-open">Legal & privacy</a> · <a href="/content/">Site map</a> · <a href="/license/" class="footer-license-pill" target="_blank" rel="noopener noreferrer">SPL license</a> · <a href="https://discord.gg/jVGq5aZ6Wc" target="_blank" rel="noopener noreferrer">Discord</a>',
    },
    fr: {
      'page.title': 'SOLEA PIXEL',
      'meta.description':
        'BIENVENUE : installez ici notre launcher et découvrez les fonctionnalités proposées par le launcher.',
      'nav.home': 'Accueil',
      'nav.bmc': 'Better MC',
      'nav.bmcAria': 'Mod Better Minecraft — page teaser',
      'nav.news': 'Actu',
      'nav.about': 'À propos',
      'nav.downloads': 'Téléchargements',
      'nav.faq': 'FAQ',
      'nav.content': 'Contenu',
      'nav.skip': 'Aller au contenu',
      'nav.top': 'Haut de page',
      'nav.socialToolbar': 'Réseaux sociaux',
      'nav.ariaModrinth': 'Solea Pixel sur Modrinth',
      'nav.ariaYoutube': 'SILWOX sur YouTube',
      'nav.ariaX': 'SILWOX sur X',
      'nav.ariaDiscord': 'Solea Pixel sur Discord',
      'nav.ariaBmc': 'Soutien sur Buy Me a Coffee',
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
      'page404.easterGoldenAria': 'Secret doré — essaie de l’attraper',
      'page404.easterTitle': 'Bravo !',
      'page404.easterLead': 'Tu as gagné une récompense.',
      'page404.easterClaim': 'Obtenir',
      'page404.easterDismiss': 'Fermer',
      'page404.easterVoidReset': 'Retour au point de départ',
      'page404.voidSequelRebuildHint': "Replace les morceaux comme sur l'accueil — glisse-les avec la souris.",
      'page404.voidSequelEndGame': 'Mettre fin au jeu',
      'page404.voidSequelDialogTitle': 'Fin de l’épreuve',
      'page404.voidSequelContinue': 'Continuer',
      'page404.voidP2GoldBtn': 'Continuer',
      'page404.voidP2HackTitle': 'Signal intercepté',
      'page404.voidP2LicBrand': 'SPL',
      'page404.voidP2LicGlitch': 'ERROR 404',
      'page404.voidP2WinTitle': 'Bien joué',
      'page404.voidP2WinBody': 'Tu as isolé le 404 fuyard.',
      'page404.voidP2WinOk': 'OK',
      'page404.voidP2PacTag': 'Épreuve',
      'page404.voidP2PacTitle': 'Labyrinthe',
      'page404.voidP2PacHint':
        'Labyrinthe aléatoire à chaque partie (grille plus grande). Atteins la sortie verte. Deux orbes hostiles au début — une troisième arrive après 67 s. Si elles te voient en ligne droite, elles te poursuivent, avec des déplacements espacés. 3 vies. Après 60 s quelques murs internes bougent (la sortie reste). Flèches ou ZQSD / WASD.',
      'page404.voidP2PacLaunch': 'Lancer',
      'page404.voidP2PacRetry': 'Recommencer',
      'page404.voidSequelGoHome': 'Retourner au site',
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
        'Découvrez notre nouveau launcher optimisé pour toute utilisation Minecraft. Vous retrouverez les modpack SOLEA PIXEL, un accès à Minecraft vanilla et diverses autres options disponibles.',
      'hub.btnDl': 'Télécharger',
      'hub.btnDiscover': 'Découvrir',
      'hub.versionLoading': 'Chargement de la dernière version…',
      'hub.versionSlow': 'GitHub est lent — on récupère encore la dernière version…',
      'hub.versionOk': 'Installateur Windows détecté : **{v}**.',
      'hub.versionFallback': 'Dernière version sur la page Téléchargements.',
      'tilt.pool.0.title': 'Pack Modrinth',
      'tilt.pool.0.sub': 'Propulsé par Modrinth — installation en quelques clics',
      'tilt.pool.1.title': 'Connexion facile',
      'tilt.pool.1.sub': 'Relie ton compte Minecraft depuis le launcher',
      'tilt.pool.2.title': 'Interface soignée',
      'tilt.pool.2.sub': 'Panneaux plus nets et lecture plus confortable',
      'tilt.pool.3.title': 'Tous les modpacks',
      'tilt.pool.3.sub': 'Les packs SOLEA au même endroit, synchronisés via Modrinth',
      'tilt.pool.4.title': 'Crack & hors-ligne',
      'tilt.pool.4.sub': 'Jouer sans compte Microsoft — respecte les règles et limites des serveurs',
      'tilt.pool.5.title': 'Accueil & actus',
      'tilt.pool.5.sub': 'Patch notes, liens et annonces là où tu lances le jeu',
      'tilt.pool.6.title': 'NeoForge',
      'tilt.pool.6.sub': 'Installs de packs modernes sans bricoler les dossiers',
      'tilt.pool.7.title': 'Mon serveur local',
      'tilt.pool.7.sub': 'Un espace rangé pour héberger un monde sur ton PC',
      'tilt.pool.8.title': 'Mises à jour discrètes',
      'tilt.pool.8.sub': 'Le launcher reste à jour via le même canal que ce site',
      'tilt.pool.9.title': 'Discord & support',
      'tilt.pool.9.sub': 'La communauté quand tu bloques ou cherches une info',
      'meta.build': 'Build',
      'meta.platform': 'Plateforme',
      'downloads.tag': 'Téléchargements',
      'downloads.title': 'Téléchargements',
      'downloads.lead':
        'Chaîne stable : **Windows** (NSIS + portable) et **macOS** (DMG arm64 en priorité, x64 sur la release GitHub si publié), depuis le même `latest`.',
      'downloads.stable': 'Stable',
      'downloads.beta': 'Bêta',
      'downloads.betaSoon': 'Pas encore de canal bêta séparé — suivez les annonces sur Discord.',
      'downloads.win': 'Windows (x64)',
      'downloads.winHint': 'x64 · installateur NSIS',
      'downloads.winBtn': "Télécharger l'installateur",
      'downloads.mac': 'macOS',
      'downloads.macHint':
        'DMG **arm64** (Apple Silicon) quand il est joint à la release ; DMG **x64** (Intel) aussi sur GitHub si présent.',
      'downloads.macBtn': 'Télécharger le DMG',
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
      'downloads.platformsAria': 'Options de téléchargement par plateforme',
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
      'news.lead': '',
      'news.body1': '',
      'news.body2': '',
      'news.liveIntro': '',
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
      'content.pageTitle': 'SOLEA PIXEL — Hub contenu',
      'content.metaDescription':
        'Tous les projets Modrinth de SOLEA PIXEL au même endroit : membres, modpacks, mods, resource packs et liens vers chaque page projet.',
      'content.h1': 'Hub contenu',
      'content.eyebrow': 'Modrinth · catalogue',
      'content.membersSub': 'Membres de l’organisation — les profils s’ouvrent sur Modrinth.',
      'content.projectsSub':
        'Parcours par catégorie — modpacks, mods, resource packs, puis shaders & plugins. Chaque carte renvoie vers la page Modrinth officielle du projet.',
      'content.tocNav': 'Parcourir les catégories de projets',
      'content.tocLabel': 'Aller à',
      'content.railTitle': 'Sur cette page',
      'content.railHero': 'Haut de page',
      'content.railTeam': 'Équipe',
      'content.railCatalog': 'Catalogue',
      'content.mainMore': 'Shaders, plugins et autres',
      'content.catDescModpack':
        'Packs installables (.mrpack) — idéal avec le launcher Solea Pixel ou tout client compatible Modrinth.',
      'content.catDescMod':
        'Mods de gameplay, contenu ou confort : nouvelles mécaniques, créatures, équipements, qualité de vie.',
      'content.catDescResourcepack':
        'Textures, modèles et ambiance — change le rendu visuel sans ajouter de code côté serveur.',
      'content.catDescMore':
        'Shaders, plugins, data packs et autres formats regroupés ici pour garder les trois grands blocs lisibles.',
      'content.lead':
        'Catalogue en direct de l’organisation <a href="https://modrinth.com/organization/soleapixel">SOLEA PIXEL</a> sur Modrinth — membres et chaque projet public, triés par type.',
      'content.orgCta': 'Ouvrir l’organisation sur Modrinth',
      'content.members': 'Équipe',
      'content.projects': 'Projets',
      'content.loading': 'Chargement des données Modrinth…',
      'content.error': 'Impossible de charger l’organisation. Réessaie dans un instant.',
      'content.empty': 'Aucun projet dans cette section.',
      'content.statsDl': '{n} téléchargements',
      'content.statsFollow': '{n} abonnés',
      'content.view': 'Voir',
      'content.download': 'Télécharger',
      'content.type.modpack': 'Modpacks',
      'content.type.mod': 'Mods',
      'content.type.resourcepack': 'Resource packs',
      'content.type.shader': 'Shaders',
      'content.type.plugin': 'Plugins',
      'content.type.datapack': 'Data packs',
      'content.type.other': 'Autre',
      'content.backHome': '← Accueil',
      'content.sitemap': 'Plan du site & liens',
      'content.footerLicense': 'Licence SPL',
      'betterminecraft.pageTitle': 'Better Minecraft (BMC) — SOLEA PIXEL',
      'betterminecraft.metaDescription':
        'Better Minecraft (BMC) pour NeoForge 1.21.1 : extension survie vanilla+, équipements, infusion, biomes, objectifs. Teaser détaillé SOLEA PIXEL ; téléchargements sur Modrinth.',
      'betterminecraft.heroBadge': 'NeoForge · Minecraft Java 1.21.1 · Modrinth',
      'betterminecraft.heroTitle': 'Better Minecraft',
      'betterminecraft.heroSubtitle': '[ BMC MOD ]',
      'betterminecraft.heroLead':
        'Survie **vanilla+** lisible d’un coup d’œil : nouveaux objectifs, rythme maîtrisé, ambiance toujours Minecraft.',
      'betterminecraft.introTitle': "C'est quoi Better Minecraft ?",
      'betterminecraft.introLead':
        'Better Minecraft (**BMC**) est un mod **NeoForge** pour **Java 1.21.1** qui allonge le survie avec du contenu pensé — pas du bruit aléatoire, ni une usine à machines.',
      'betterminecraft.introP1':
        'On part d’une question : **est-ce que ça pourrait tenir dans un changelog Mojang ?** Progression lisible, interface familière (on pense **JEI** / **Jade**), et des mécaniques qui récompensent l’anticipation — nouveaux paliers, nouvelles raisons d’explorer, petites boucles **à côté** du vanilla plutôt qu’à la place.',
      'betterminecraft.introP2':
        'Tu y trouveras des **lignes d’équipement** (dont enderite & émeraude comme fils conducteurs), des **boucles d’infusion / d’artisanat**, des **enchantements & systèmes** pour le milieu / fin de jeu, et du **relief monde** (End & Overworld) qui avance au fil des versions. Chiffres et recettes bougent avec la refonte : le **changelog Modrinth** reste la référence.',
      'betterminecraft.introP3':
        'C’est pensé pour les **auteurs de modpacks** qui veulent « plus de Minecraft » sans réécrire la courbe d’apprentissage, et pour les **mondes solo** où tu veux de nouveaux jalons après le dragon sans empiler vingt mods sans lien.',
      'betterminecraft.pillarsTitle': 'Trois gros points du mod',
      'betterminecraft.pillar1Title': 'Compatible modpack',
      'betterminecraft.pillar1Body':
        'Pensé pour **s’insérer dans tes packs** : bon voisin avec **JEI**, **Jade** et le reste de la liste, sans voler la vedette aux autres mods.',
      'betterminecraft.pillar2Title': 'Proche du vanilla',
      'betterminecraft.pillar2Body':
        'Du contenu qui reste dans l’**esprit Minecraft** — objectifs, équipements, monde : plutôt une **grosse couche vanilla** qu’un jeu à part.',
      'betterminecraft.pillar3Title': 'Pas trop complexe',
      'betterminecraft.pillar3Body':
        'Pas de mur de devoirs : **progression lisible**, des systèmes qu’on peut résumer vite, sans « apprends tout mon arbre tech avant de t’amuser ».',
      'betterminecraft.detailsTitle': 'Axes concrets (vue d’ensemble)',
      'betterminecraft.detailsHtml':
        '<ul class="bmc-detail-list"><li><strong>Équipement &amp; matériaux</strong> — nouvelles lignes à planifier (enderite &amp; émeraude en tête d’affiche), affinées pendant la migration 1.21.1.</li><li><strong>Infusion &amp; objectifs d’artisanat</strong> — plus de raisons de farmer, d’explorer et de relier le monde entre deux bases.</li><li><strong>Enchantements &amp; petites mécaniques</strong> — des leviers qui allongent combat &amp; préparation sans remplacer l’équilibre de base.</li><li><strong>Biomes &amp; ambiance</strong> — End &amp; Overworld comme espaces de premier plan ; le détail évolue — suis le changelog.</li><li><strong>Documentation</strong> — notes de version sur Modrinth ; pages wiki plus poussées au fur et à mesure que la refonte se stabilise.</li></ul>',
      'betterminecraft.fitTitle': 'Pour qui ?',
      'betterminecraft.fitP1':
        'Idéal si tu veux **plus de cases à cocher après le diamant** — nouvelles stations, nouvelles routes d’optimisation, nouvelles excuses pour arpenter la carte — en gardant le « cerveau Minecraft » que tu as déjà.',
      'betterminecraft.fitP2':
        'Moins adapté si tu cherches **l’automatisation usine**, les **arbres de magie tech** ou les **dimensions** qui ignorent le jeu de base. BMC reste collé au vanilla ; il ne le remplace pas.',
      'betterminecraft.featuresTitle': 'Zoom sur les blocs',
      'betterminecraft.feat1Title': 'Équipement qui te cadence',
      'betterminecraft.feat1Body':
        'Les lignes type enderite &amp; émeraude donnent des **projets long terme** sans rendre le fer ou le diamant ridicules du jour au lendemain. Stats et recettes suivent la migration NeoForge — voir Modrinth pour les valeurs à jour.',
      'betterminecraft.feat2Title': 'Infusion & identité craft',
      'betterminecraft.feat2Body':
        'Les workflows type infusion lient les matériaux de fin de partie à des **choix** — quoi charger en premier, quoi farmer ensuite — plutôt qu’à une recette OP unique.',
      'betterminecraft.feat3Title': 'Des mondes qui te rappellent',
      'betterminecraft.feat3Body':
        'Le contenu End & Overworld vise des **retours sur zone** : nouvelles raisons de remplir des shulkers, une ambiance « extension » plutôt que « biome random ».',
      'betterminecraft.feat4Title': 'Des systèmes, pas du bruit',
      'betterminecraft.feat4Body':
        'Enchantements et mécaniques légères allongent les boucles — **compromis**, **sidegrades** et **préparation** — au lieu d’empiler des gadgets sans lien.',
      'betterminecraft.feat5Title': 'Promesse vanilla+',
      'betterminecraft.feat5Body':
        'Si une feature ne s’explique pas en une phrase à un joueur qui revient, elle n’a probablement pas sa place ici. BMC, c’est le couloir **bac à sable plus large** : plus d’objectifs, le même vocabulaire.',
      'betterminecraft.compatTitle': 'Compatibilité',
      'betterminecraft.compatBody': 'Minecraft **Java 1.21.1** · **NeoForge** · **client & serveur dédié**.',
      'betterminecraft.compatNote':
        'Les loaders bougent vite — aligne toujours la **version de NeoForge** avec la plage indiquée sur le fichier Modrinth que tu télécharges. En cas de doute, demande sur Discord avec ton dernier log.',
      'betterminecraft.linksTitle': 'Liens officiels',
      'betterminecraft.linkProject': 'Modrinth — page projet',
      'betterminecraft.linkChangelog': 'Changelog & fichiers',
      'betterminecraft.linkOrgShort': 'Org SOLEA PIXEL',
      'betterminecraft.linkDiscord': 'Discord communauté',
      'betterminecraft.ctaModrinth': 'Voir sur Modrinth',
      'betterminecraft.ctaOrg': 'SOLEA PIXEL sur Modrinth',
      'betterminecraft.footerNote':
        'Visuels & marques appartiennent à leurs détenteurs. Service Minecraft non officiel.',
      'betterminecraft.backHome': '← Retour à l’accueil',
      'betterminecraft.showcaseTitle': 'Textures & équipements (aperçu)',
      'betterminecraft.showcaseHint':
        'Survole une carte (ou mets-y le focus) pour lire le texte — sur écran tactile, la description reste affichée.',
      'betterminecraft.showcaseArmorFloatNote':
        '**Émeraude** (bord gauche) et **enderite** (bord droit) tournent en **3D** comme en jeu — aperçu discret sur le fond (réutilisable pour ton wiki).',
      'betterminecraft.showcase1Name': 'Bloc de Nebrith',
      'betterminecraft.showcase1Desc':
        'Trouvable dans les **géodes de l’End** : une matière distinctive et une **belle décoration**.',
      'betterminecraft.showcase2Name': 'Minerai de rubis',
      'betterminecraft.showcase2Desc':
        'Le rubis est une gemme rare, pensée surtout pour des **échanges spéciaux** avec le **villageois sorcier guéri**.',
      'betterminecraft.showcase3Name': 'Bloc d’enderite',
      'betterminecraft.showcase3Desc':
        'Plus rare que la Netherite : se craft avec **neuf lingots d’enderite** et reste **très difficile** à obtenir.',
      'betterminecraft.showcase4Name': 'Bâton du dragon',
      'betterminecraft.showcase4Desc':
        'Bâton de magie qui canalise la **puissance du dragon** comme source d’énergie.',
      'betterminecraft.showcase5Name': 'Bâton d’écho',
      'betterminecraft.showcase5Desc':
        'Le **Warden** n’est plus le seul détenteur de ce pouvoir — le **bâton d’écho** s’en sert aussi.',
      'betterminecraft.showcase6Name': 'Épée en enderite',
      'betterminecraft.showcase6Desc':
        'Plus tranchante et plus résistante que la Netherite — le **nouvel emblème** de ton aventure.',
      'betterminecraft.showcase7Name': 'Arc foudre',
      'betterminecraft.showcase7Desc':
        'Comme son nom l’indique : **tir plus précis**, **portée accrue**, et un **éclair puissant** à l’impact. Ne reste pas sur la trajectoire !',
      'betterminecraft.showcase8Name': 'Expérience liquide',
      'betterminecraft.showcase8Desc':
        'De l’XP sous forme liquide dans l’End — **ce n’est pas tout à fait** ce que tu imagines.',
      'betterminecraft.showcase9Name': 'Armure en émeraude',
      'betterminecraft.showcase9Desc':
        'Pas plus forte que le diamant, mais le **set complet** a son atout : équipe-toi entièrement et **va voir les villageois**.',
      'betterminecraft.showcase10Name': 'Armure en enderite',
      'betterminecraft.showcase10Desc':
        '**Violet sombre** et fidèle à son nom — plus résistante que l’ancienne armure qui commence à faire vieux jeu.',
      'betterminecraft.showcase11Name': 'Cristal',
      'betterminecraft.showcase11Desc':
        'Quoi que tu croies, tu te trompes ! **Capture des âmes** dans le cristal et utilise-les à la **table d’infusion** — de la sorcellerie.',
      'bmcwiki.pageTitle': 'Wiki Better Minecraft (brouillon) — SOLEA PIXEL',
      'bmcwiki.metaDescription':
        'Wiki BMC brouillon : **enderite**, **nebrith**, **table d’infusion**, bâtons, etc. — **non indexé** tant que tu ne publies pas.',
      'bmcwiki.draftBadge': 'Brouillon · pas pour la prochaine mise en ligne',
      'bmcwiki.draftBody':
        'Cette URL est en **noindex** et **interdite** aux robots dans `robots.txt`. Retire ces garde-fous et complète le contenu quand tu voudras publier.',
      'bmcwiki.mainTitle': 'Wiki Better Minecraft',
      'bmcwiki.mainLead':
        'Articles alignés sur les **sources du mod Better Minecraft** dans ce dépôt : paliers d’armure, bois, stockage, infusion, créatures et objets utilitaires. **Corrige** ce qui ne colle plus à ta version.',
      'bmcwiki.quickNavAria': 'Raccourcis vers les grandes sections de cette page',
      'bmcwiki.quickNavLabel': 'Aller à',
      'bmcwiki.quickNavInfusion': 'Infusion',
      'bmcwiki.quickNavEnd': 'End & rubis',
      'bmcwiki.quickNavArmor': 'Équipement enderite',
      'bmcwiki.quickNavItems': 'Sacs à dos',
      'bmcwiki.quickNavStorage': 'Stockage',
      'bmcwiki.navLabel': 'Sommaire',
      'bmcwiki.sidebarAria': 'Catégories du wiki',
      'bmcwiki.navTreeAria': 'Arborescence des catégories',
      'bmcwiki.sidebarToggle': 'Afficher / masquer les catégories',
      'bmcwiki.sidebarToggleAria': 'Ouvrir ou fermer le panneau des catégories',
      'bmcwiki.catArmor': 'Armures & outils',
      'bmcwiki.linkArmorEnderite': 'Enderite',
      'bmcwiki.linkArmorEmerald': 'Émeraude',
      'bmcwiki.linkArmorCopper': 'Cuivre',
      'bmcwiki.linkArmorObsidian': 'Obsidienne',
      'bmcwiki.linkArmorShulker': 'Shulker',
      'bmcwiki.linkArmorHats': 'Coiffes village & sorcière',
      'bmcwiki.catBlocks': 'Blocs',
      'bmcwiki.linkNebrith': 'Nebrith',
      'bmcwiki.linkInfusionTable': 'Table d’infusion',
      'bmcwiki.linkBlocksStone': 'Pierres claire & sombre',
      'bmcwiki.linkBlocksEnd': 'Minerai de l’End & rubis',
      'bmcwiki.linkBlocksStorage': 'Coffre enchanté & four',
      'bmcwiki.linkBlocksHollow': 'Bois creux (Hollow)',
      'bmcwiki.linkBlocksSunwood': 'Sunwood & Solea Grove',
      'bmcwiki.catItems': 'Objets',
      'bmcwiki.linkItemEnderite': 'Enderite (matériaux)',
      'bmcwiki.linkItemCrystals': 'Cristaux d’âmes',
      'bmcwiki.linkItemLightningBow': 'Arc foudre',
      'bmcwiki.linkAllStaff': 'Tous les bâtons',
      'bmcwiki.linkItemBackpacks': 'Sacs à dos',
      'bmcwiki.linkItemFood': 'Baies & pommes',
      'bmcwiki.linkItemUtility': 'Utilitaires & butins',
      'bmcwiki.catMobs': 'Créatures',
      'bmcwiki.linkMobsOverview': 'Autres créatures du mod',
      'bmcwiki.linkRadiantSlime': 'Radiant slime',
      'bmcwiki.linkMetaRarity': 'Affichage des raretés',
      'bmcwiki.linkProgressFossil': 'Débris fossiles & XP',
      'bmcwiki.catEnchants': 'Enchantements & effets',
      'bmcwiki.catWorld': 'Biomes & structures',
      'bmcwiki.linkWorldBiomes': 'Hollow Garden & Solea Grove',
      'bmcwiki.catProgress': 'Progression & objectifs',
      'bmcwiki.catSoon': 'Sous-pages à ajouter plus tard.',
      'bmcwiki.titleItemEnderite': 'Objets : Enderite (matériaux)',
      'bmcwiki.itemEnderiteIntro':
        'L’**enderite** : fragments (**Forgotten Debris** → four / haut fourneau), combinaison avec des **diamants** en lingots, compression en **bloc**, duplication du **gabarit**.',
      'bmcwiki.enderiteScrapSmelt':
        'Fonds ou haut fourneau le minerai **Forgotten Debris** comme une roche classique pour obtenir des **fragments d’enderite**.',
      'bmcwiki.armorEnderiteIntro':
        'L’**armure et les outils en enderite** viennent de la **netherite** au **tableau de forgeron** avec le gabarit et des **lingots d’enderite** (voir **Objets : Enderite** pour les lingots).',
      'bmcwiki.xrefToArmorEnderite': '→ Armures & outils enderite',
      'bmcwiki.xrefToItemEnderite': '→ Matériaux enderite',
      'bmcwiki.xrefToInfusion': '→ Table d’infusion',
      'bmcwiki.xrefToArmorEmerald': '→ Armure émeraude',
      'bmcwiki.xrefToCrystals': '→ Cristaux d’âmes',
      'bmcwiki.xrefEmeraldBootsInfusion': '→ Bottes ciel (infusion)',
      'bmcwiki.xrefEmeraldBootsCraft': '→ Armure émeraude (bottes)',
      'bmcwiki.xrefRubyOres': '→ End & rubis',
      'bmcwiki.xrefToInfusionRecipe': '→ Recette d’infusion',
      'bmcwiki.xrefNetheriteForBow': '→ Netherite (forge)',
      'bmcwiki.xrefStaffInfusion': '→ Recettes d’infusion des bâtons',
      'bmcwiki.xrefNebrithInfusion': '→ Infusion (éclats nebrith dans d’autres crafts)',
      'bmcwiki.xrefPocketEnderInfusion': '→ Coffre de l’End de poche (infusion)',
      'bmcwiki.xrefToBackpacks': '→ Sacs à dos',
      'bmcwiki.xrefToFossil': '→ Débris fossiles & XP',
      'bmcwiki.xrefToWorldBiomes': '→ Biomes (worldgen)',
      'bmcwiki.xrefToUtility': '→ Utilitaires & butins',
      'bmcwiki.xrefToRadiant': '→ Radiant slime',
      'bmcwiki.xrefWardenTendril': '→ Tendril de warden (butin)',
      'bmcwiki.titleArmorObsidian': 'Armure : obsidienne',
      'bmcwiki.bodyArmorObsidian':
        'Set complet **obsidienne** (casque → bottes), palier **épique** enregistré dans `ModItems` ; recettes type armure (voir **JEI** / jeu).',
      'bmcwiki.titleArmorShulker': 'Armure : shulker',
      'bmcwiki.bodyArmorShulker':
        'Quatre pièces **shulker**, thème coque de shulker, palier **épique**, fil de progression distinct de l’émeraude ou de l’enderite.',
      'bmcwiki.titleArmorHats': 'Armure : coiffes de village & chapeau de sorcière',
      'bmcwiki.bodyArmorHats':
        'Casques « **métier** » villageois (boucher, bibliothécaire, armurier, etc.) + **chapeau de sorcière** avec mécanique d’**âmes** stockées (voir `WitchHatItem` et infobulles).',
      'bmcwiki.titleItemBackpacks': 'Objets : sacs à dos',
      'bmcwiki.bodyItemBackpacks':
        'Ligne de **sacs** : cuir → fer → or → diamant → émeraude (rangées et **rareté**). Tag **`bmcmod:backpack`** pour les datapacks ; **Maj** pour un aperçu du contenu. Ingrédient de la recette **coffre de l’End de poche** à l’infusion.',
      'bmcwiki.titleItemFood': 'Objets : baies, pommes & fiole d’XP',
      'bmcwiki.bodyItemFood':
        '**Baies violettes** sur **buisson** dans le Hollow Garden (nourriture + régén courte). **Baie dorée** récompense haut palier. **Pomme diamant** / **pomme diamant enchantée** façon gapple avec effets longs. **Fiole d’expérience scellée** pour stocker de l’XP (voir classe dédiée).',
      'bmcwiki.titleItemUtility': 'Objets : utilitaires & butins rares',
      'bmcwiki.bodyItemUtility':
        '**Éclat du vide**, **corne mélodie**, **livre inconnu**, **disque** (*Beyond The Enderman*), **seau de liquide d’expérience**, **tendril de warden** (butin Warden), **cœur de golem de l’End**, **fragment fossile** (débris fossiles).',
      'bmcwiki.titleBlocksStorage': 'Blocs : coffre enchanté & four en pierre de l’End',
      'bmcwiki.bodyBlocksStorage':
        'Le **coffre enchanté** accumule de l’**XP** avec affichage HUD, **améliorations** (max **3**) et bonus sur ticks ; l’**amélioration** se consomme jusqu’au plafond. Le **four en pierre de l’End** est un four thématique. Recettes dans `data/bmcmod/recipe/`.',
      'bmcwiki.titleBlocksHollow': 'Blocs : bois creux (Hollow Garden)',
      'bmcwiki.bodyBlocksHollow':
        'Famille **hollow** : bûches, écorcées, **planches**, escaliers, dalles, barrières, portillon, porte, trappe, plaque, bouton, **tonneau**, **bibliothèque**, **feuilles**, **pousse**. Biomes **Hollow Garden** avec **herbe creuse** et **buisson** de baies.',
      'bmcwiki.titleBlocksSunwood': 'Blocs : sunwood (Solea Grove)',
      'bmcwiki.bodyBlocksSunwood':
        '**Sunwood** : bûches, bois, planches, set de construction, **panneaux** et **enseignes suspendues**, feuilles, pousse, **sunbloom**, **mousse de surface**, **bateau** et **bateau coffre**.',
      'bmcwiki.titleMetaRarity': 'Interface : raretés & infobulles',
      'bmcwiki.bodyMetaRarity':
        'Le mod ajoute des paliers **BmcMod** (Commun → **Fragmenté**) pour bordures et infobulles (`BmcModRarity`, `RarityTooltipHandler`). Les **bâtons de test** servent d’aperçu — pas une progression joueur.',
      'bmcwiki.titleProgressFossil': 'Progression : débris fossiles & XP liquide',
      'bmcwiki.bodyProgressFossil':
        'Les **débris fossiles** sont un minerai très résistant en profondeur (`ModBlocks.FOSSIL_DEBRIS`) ; ils mènent aux **fragments fossiles**. Le **liquide d’expérience** en End alimente le **Radiant slime** — voir `data/bmcmod/worldgen` et les fluides dans le code.',
      'bmcwiki.copperFullBody':
        'L’**armure cuivre complète** et les **outils** (épée à houe) existent en plus du casque ; stats entre pierre et fer (`CopperEquipment`).',
      'bmcwiki.itemCrystalsMorph':
        '**Cristal de métamorphose** : tuer une créature avec le cristal en **main secondaire** pour stocker la forme ; **maintenir utiliser** pour se transformer (âme consommée). **Cristal de capture** : flux voisin avec plus de durabilité. Les deux se **craftent à l’infusion** avec un cristal d’âmes approvisionné.',
      'bmcwiki.enderiteScrapTitle': 'Fragment d’enderite',
      'bmcwiki.enderiteScrapBody':
        'Casse le minerai **Forgotten Debris** dans l’End (esprit proche des débris antiques). Fonds ou haut fourneau → **fragment d’enderite**.',
      'bmcwiki.enderiteIngotTitle': 'Lingot d’enderite (sans forme)',
      'bmcwiki.craftShapelessEnderiteIngotAria':
        'Recette sans forme (affichée en grille 3×3) : quatre fragments d’enderite et quatre diamants → un lingot — placement libre.',
      'bmcwiki.legendEnderiteIngot':
        'Textures **diamant** vanilla Minecraft **1.21.1** (miroir communautaire). Textures du mod copiées depuis ton pack **bmcmod**.',
      'bmcwiki.enderiteBlockTitle': 'Bloc d’enderite',
      'bmcwiki.craftEnderiteBlockAria': 'Forme 3×3 : neuf lingots d’enderite pour un bloc d’enderite',
      'bmcwiki.legendEnderiteBlock': 'Neuf **lingots d’enderite** → un **bloc** (stockage).',
      'bmcwiki.enderiteTemplateTitle': 'Gabarit de forge d’enderite (duplication)',
      'bmcwiki.enderiteTemplateBody':
        'Le **gabarit de forge** « upgrade enderite » est requis au **tableau de forgeron**. Comme les autres gabarits : un gabarit + pierre de l’End + diamants → **deux** gabarits.',
      'bmcwiki.craftTemplateDupAria': 'Recette en forme pour dupliquer le gabarit d’upgrade enderite',
      'bmcwiki.legendTemplateDup':
        'Un **gabarit d’upgrade enderite**, **pierre de l’End** au centre, **diamants** autour → **deux** gabarits.',
      'bmcwiki.enderiteSmithingTitle': 'Forge (netherite → enderite)',
      'bmcwiki.enderiteSmithingBody':
        'Au **tableau de forgeron** : gabarit, pièce en **netherite**, **lingot d’enderite** dans la case addition.',
      'bmcwiki.smithingEnderiteAria': 'Schéma de transformation : gabarit + base netherite + lingot d’enderite',
      'bmcwiki.smithSlotTemplate': 'Gabarit',
      'bmcwiki.smithSlotBase': 'Base',
      'bmcwiki.smithSlotAdd': 'Ajout',
      'bmcwiki.legendSmithingLetters':
        '**Tableau de forgeron** : gabarit + pièce **netherite** + **lingot d’enderite** → version **enderite** (exemple : **épée netherite** → **épée enderite**).',
      'bmcwiki.enderiteSmithingList1':
        '**Outils & armes** : même logique pour épée, pioche, hache, pelle et houe.',
      'bmcwiki.enderiteSmithingList2':
        '**Armure & boucliers** : casque, plastron, jambières, bottes et **bouclier d’enderite** suivent la même transformation.',
      'bmcwiki.emeraldIntro':
        'L’**émeraude** sert de matériau pour outils et armure, calés comme **alternative au diamant** avec un peu plus de protection totale.',
      'bmcwiki.emeraldSetTitle': 'Bonus armure complète',
      'bmcwiki.emeraldSetBody':
        'Avec les **quatre** pièces d’armure en émeraude, tu gardes **Héros du village I** en durée **infinie** — pratique pour le **commerce** avec les villageois.',
      'bmcwiki.emeraldToolsBody':
        'Les outils utilisent le palier émeraude du mod (recettes type vanilla avec émeraudes — voir en jeu / JEI).',
      'bmcwiki.copperIntro':
        'Le **cuivre** : outils **entre pierre et fer** ; armure un peu **sous le fer** en points totaux, économique en lingots et pépites.',
      'bmcwiki.copperHelmetTitle': 'Casque en cuivre (orages)',
      'bmcwiki.copperHelmetBody':
        'Pendant un **orage**, s’il **pleut**, que tu vois le **ciel** et que tu portes le **casque en cuivre**, le mod peut faire tomber la **foudre** près de toi en surface (tick rare).',
      'bmcwiki.nebrithIntro':
        'Le **nebrith** est un cristal violet : **bourgeons** sur **nebrith en bourgeon** jusqu’aux grappes. Casser les grappes donne des **éclats** ; les éclats craftent blocs et machines.',
      'bmcwiki.nebrithShardsTitle': 'Éclats & croissance',
      'bmcwiki.nebrithShardsBody':
        'Comportement proche de l’**améthyste** : bon outil, Toucher de soie selon tables de butin. Les éclats servent aux crafts « nebrith » (bloc, longue-vue, vitre teintée, etc.).',
      'bmcwiki.nebrithBlockTitle': 'Bloc de nebrith',
      'bmcwiki.craftNebrithBlockAria':
        'Quatre éclats en carré 2×2 (affiché dans une grille 3×3) → bloc de nebrith',
      'bmcwiki.legendNebrithBlock': 'Quatre **éclats de nebrith** (2×2) → **bloc de nebrith**.',
      'bmcwiki.relicTableTitle': 'Table de reliques',
      'bmcwiki.craftRelicTableAria': 'Forme : purpur, pierre de l’End, éclat central → table de reliques',
      'bmcwiki.legendRelicTable':
        '**Purpur**, **pierre de l’End**, un **éclat de nebrith** au centre → **table de reliques** (texture du dessus comme icône).',
      'bmcwiki.infusionIntroFull':
        'La **table d’infusion** a une interface : un slot pour le **cristal d’âmes**, une grille **2×2** pour les ingrédients (les quantités peuvent être réparties sur plusieurs cases ; aucun autre type d’objet — voir `InfusionRecipe.java`). Quand la recette correspond, les **âmes du cristal** paient le **coût en âmes** et le résultat apparaît. Les recettes sont listées dans **`InfusionRecipes.java`**.',
      'bmcwiki.infusionRecipesTitle': 'Recettes d’infusion (en jeu)',
      'bmcwiki.infusionRecipesLead':
        'Chaque fiche donne les **totaux** à réunir sur la grille 2×2. **L’ordre des cases importe peu** tant qu’il n’y a pas d’objet en trop.',
      'bmcwiki.infuseLightningBowTitle': 'Arc foudre',
      'bmcwiki.infuseLightningBowSoul': 'Coût en âmes : **72**',
      'bmcwiki.infuseMorphTitle': 'Cristal de métamorphose',
      'bmcwiki.infuseMorphSoul': 'Coût en âmes : **56**',
      'bmcwiki.infuseCaptureTitle': 'Cristal de capture',
      'bmcwiki.infuseCaptureSoul': 'Coût en âmes : **56**',
      'bmcwiki.infuseSkyBootsTitle': 'Bottes du ciel',
      'bmcwiki.infuseSkyBootsSoul': 'Coût en âmes : **120**',
      'bmcwiki.infuseEchoStaffTitle': 'Bâton d’écho',
      'bmcwiki.infuseEchoStaffSoul': 'Coût en âmes : **120**',
      'bmcwiki.infuseFlameStaffTitle': 'Bâton de flammes',
      'bmcwiki.infuseFlameStaffSoul': 'Coût en âmes : **100**',
      'bmcwiki.infuseEndStaffTitle': 'Bâton de l’End',
      'bmcwiki.infuseEndStaffSoul': 'Coût en âmes : **110**',
      'bmcwiki.infuseDragonStaffTitle': 'Bâton du dragon',
      'bmcwiki.infuseDragonStaffSoul': 'Coût en âmes : **115**',
      'bmcwiki.infusePocketEnderTitle': 'Coffre de l’End de poche',
      'bmcwiki.infusePocketEnderSoul': 'Coût en âmes : **5**',
      'bmcwiki.titleItemCrystals': 'Objets : Cristaux d’âmes',
      'bmcwiki.itemCrystalsIntro':
        'Le **cristal d’âmes** sert à capturer des âmes (flux main secondaire du mod). Les cristaux **métamorphose** et **capture** sont des crafts d’infusion de haut niveau.',
      'bmcwiki.itemCrystalsBody':
        'Le **cristal fissuré** apparaît quand la durabilité est épuisée. Tout se relie à la **table d’infusion** et aux systèmes de métamorphose.',
      'bmcwiki.titleItemLightningBow': 'Objets : Arc foudre',
      'bmcwiki.itemLightningBowIntro':
        'L’**arc foudre** est une arme à distance **Exotique** : tir plus serré, longue portée, éclair à l’impact (voir l’infobulle en jeu).',
      'bmcwiki.itemLightningBowBody':
        'Se fabrique uniquement par **infusion** — voir la fiche ci-dessous et le lien **netherite** pour les lingots.',
      'bmcwiki.titleBlocksStone': 'Blocs : pierres claire & sombre',
      'bmcwiki.blocksStoneBody':
        'La **pierre claire** et ses variantes apparaissent en **Overworld** ; la **pierre sombre** et le poli suivent la logique **blackstone** du Nether. Escaliers, dalles, murs, boutons et plaques reprennent les schémas vanilla / tailleur (voir JEI).',
      'bmcwiki.titleBlocksEnd': 'Blocs : End & rubis',
      'bmcwiki.blocksEndBody':
        '**Forgotten Debris** (End, très résistant) → **fragments d’enderite**. **Minerai de rubis** + **ardoise** en Overworld → gemme et **bloc de rubis**. **Sable de l’End**, **ancre de l’End** (règles d’ancre, valide dans l’End), **torche du vide** / **lanterne du vide**, plante **Jaerys**, **herbe creuse** (sol du Hollow Garden) : le kit surface de l’aperçu ci-dessus.',
      'bmcwiki.titleMobsOverview': 'Créatures : aperçu',
      'bmcwiki.mobsOverviewIntro': 'Œufs et entités enregistrés par le mod (noms issus de `fr_fr.json`) :',
      'bmcwiki.mobBlink': '**Blink** — créature de l’End (œuf disponible).',
      'bmcwiki.mobEndGolem': '**Golem de l’End** — gardien (œuf).',
      'bmcwiki.mobEndling': '**Endling** — créature de l’End (œuf).',
      'bmcwiki.mobDiamondGolem': '**Golem de diamant** — variante (œuf).',
      'bmcwiki.mobIllusioner': '**Illusionniste** — type vanilla exposé avec un œuf dans le mod.',
      'bmcwiki.mobChargedCreeper': '**Creeper chargé** — œuf pour tests / créatif.',
      'bmcwiki.titleWorldBiomes': 'Monde : biomes',
      'bmcwiki.worldBiomesBody':
        '**Hollow Garden** : bois creux, herbe creuse, baies violettes, ambiance proche de l’End. **Solea Grove** : sunwood, sunbloom, mousse de surface. Voir `data/bmcmod/worldgen/biome` et les arbres `placed_feature` / `configured_feature`, plus les clés `biome.bmcmod.*`.',
      'bmcwiki.craftInfusionAria': 'Recette en forme pour la table d’infusion',
      'bmcwiki.legendInfusion':
        '**Livre**, **émeraudes**, **briques de pierre de l’End** comme dans la recette en forme → **table d’infusion** (texture du dessus).',
      'bmcwiki.infusionNote':
        'Les coûts et ingrédients suivent **`InfusionRecipes.java`** dans ta copie du mod — ajuste là si tu changes la balance.',
      'bmcwiki.allStaffIntro':
        'Les **bâtons** magiques consomment durabilité / charges ; plusieurs utilisent **Ctrl + clic droit** pour **changer de mode** (voir les infobulles en jeu).',
      'bmcwiki.staffDragon':
        '**Bâton du dragon** — Mode 1 : charge puis lâche une **boule de feu** de dragon. Mode 2 : maintien, **souffle** sur la ligne de mire. **Ctrl + utiliser** change le mode.',
      'bmcwiki.staffEcho':
        '**Bâton d’écho** — **Six** charges soniques. Maintien ~**5 s** puis **relâcher** pour l’onde (esprit **Warden**).',
      'bmcwiki.staffFlame':
        '**Bâton de flammes** — Maintien : **lance-flammes** ; ennemis en feu : **dégâts bonus**. La durabilité **baisse pendant le tir**.',
      'bmcwiki.staffEnd':
        '**Bâton de l’End** — Trois modes : **perle chargée**, **téléport cible** (créature visée), **fuite aléatoire**. **Ctrl + utiliser** pour cycler ; perle en maintien → relâcher.',
      'bmcwiki.radiantIntro':
        'Le **Radiant slime** est un slime custom lié au **liquide d’expérience** dans l’écosystème de l’End.',
      'bmcwiki.radiantBullet1': 'Plus résistant qu’un slime classique ; peut **pomper de l’XP** au joueur en combat.',
      'bmcwiki.radiantBullet2':
        'Spawns liés à la **nuit / lacs** près du fluide ; certains retournent au **lac au lever du jour** (logique du mod).',
      'bmcwiki.radiantBullet3': 'Œuf d’apparition pour le créatif / tests ; le monde suit les règles configurées dans le mod.',
      'bmcwiki.backBmc': '← Retour au teaser BMC',
      'bmcwiki.backHome': '← Accueil',
      'footer.legal':
        'Non affilié Mojang AB ni Microsoft · <a href="#" class="js-legal-modal-open">Mentions & confidentialité</a> · <a href="/content/">Plan du site</a> · <a href="/license/" class="footer-license-pill" target="_blank" rel="noopener noreferrer">Licence SPL</a> · <a href="https://discord.gg/jVGq5aZ6Wc" target="_blank" rel="noopener noreferrer">Discord</a>',
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
          : profile === 'contentHub'
            ? t('content.metaDescription')
            : profile === 'betterminecraft'
              ? t('betterminecraft.metaDescription')
              : profile === 'betterminecraft-wiki'
                ? t('bmcwiki.metaDescription')
                : t('meta.description')
    const shareTitle =
      profile === 'license'
        ? t('license.pageTitle')
        : profile === 'error404'
          ? t('page404.docTitle')
          : profile === 'contentHub'
            ? t('content.pageTitle')
            : profile === 'betterminecraft'
              ? t('betterminecraft.pageTitle')
              : profile === 'betterminecraft-wiki'
                ? t('bmcwiki.pageTitle')
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
