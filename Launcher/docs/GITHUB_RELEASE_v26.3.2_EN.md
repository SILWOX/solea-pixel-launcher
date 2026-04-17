## Solea Pixel Launcher — **26.3.2 | Release** (package **26.3.2**)

### Highlights

- **Vanilla Minecraft — correct Java for 26.x / calendar-era versions:** the launcher now resolves the JVM major version from Mojang’s **version JSON** (`javaVersion.majorVersion`, including **`inheritsFrom`** chains on disk), then from the **version manifest** online, then a conservative **fallback heuristic** — instead of always sending **Java 21** to `minecraft-java-core` for non-`1.x` release ids. Fixes installs/launches for lines like **26.1.x** when the game requires a newer JDK (e.g. **25**).
- **Legacy UI (AETHER v1.0):** the main **left shell sidebar** stays visible on **Settings** (it was incorrectly hidden in v1 mode).
- **Legacy v1 rail styling:** new **`sidebarClassicV1.css`** — single dark column, thin **section dividers**, **46×46** tiles with a stronger **orange active border**, aligned with the pre–AETHER-v2 sidebar look.

### Built-in changelog

- New **26.3.2** entry at the top of **Home & news → Release notes**; earlier versions remain in **Release history**.

### Build artifacts

- **Installer (NSIS):** `Solea-Pixel-Setup-26.3.2.exe`
- **Portable:** `Solea Pixel 26.3.2.exe`
- **Auto-update:** attach **`latest.yml`** (and **`.blockmap`** if you ship differential updates) next to the installer on the GitHub release.

### Upgrade notes

- Install over an older build or use **in-app update** once this release is published and `latest.yml` points to **26.3.2**.
- For **Vanilla 26.x**, ensure a matching **JDK/JRE** is installed and selected in launcher settings if auto-detection does not find it.

---

## Texte FR (copier-coller description GitHub / annonce)

**Solea Pixel Launcher — 26.3.2 | Release** (paquet **26.3.2**)

- **Vanilla — Java pour Minecraft 26.x / ids « calendrier » :** la version de JVM suit le JSON Mojang (`javaVersion.majorVersion`, chaîne `inheritsFrom` sur le disque), puis le manifeste, puis une heuristique — plus de blocage sur Java 21 seul pour les releases qui ne sont pas en `1.x`.
- **Interface Legacy (AETHER v1) :** la **barre latérale gauche** reste affichée dans **Paramètres** (correctif).
- **Rail Legacy v1 :** fichier **`sidebarClassicV1.css`** — fond plus plat, séparateurs fins, pastilles carrées, bord actif orange plus visible (style proche **26.1**).

**Fichiers :** `Solea-Pixel-Setup-26.3.2.exe`, `Solea Pixel 26.3.2.exe`, `latest.yml`.
