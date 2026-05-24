# Notes de publication — v26.3.3

Ce fichier contient **deux blocs prêts à coller** dans GitHub : un **corps de release en anglais** (recommandé pour l’audience large), puis un **équivalent français**. Tu peux aussi n’en coller qu’un seul.

---

## Bloc 1 — Release description (English, copy below the line)

```markdown
## Solea Pixel Launcher **26.3.3** — stable release

### What’s new

- **Launcher settings:** the **Experimental (BETA)** section now sits **under Keyboard shortcuts** on the same **Settings → Launcher** tab. It only covers **My servers (experimental)** and **Launcher interface** (AETHER **v2.0** vs **Legacy**).
- **Clearer layout:** **Notifications**, **Discord Rich Presence**, **update channel**, and **Audio** are back in the main launcher settings flow instead of a separate “Experimental” sidebar page.
- **macOS:** this release ships **native DMGs** for **Apple Silicon (arm64)** and **Intel (x64)**. The [download page](https://soleapixel.com) picks the **arm64** DMG from the latest GitHub release when it’s attached.
- **Release pipeline:** tagging `v*` triggers **Windows** (NSIS installer, portable `.exe`, `latest.yml`) and **macOS** DMG builds in parallel on GitHub Actions.

### Downloads

| Platform | File | Notes |
|----------|------|--------|
| **Windows** | `Solea-Pixel-Setup-26.3.3.exe` | NSIS installer (x64) |
| **Windows** | `Solea Pixel 26.3.3.exe` | Portable (x64) |
| **macOS** | `Solea-Pixel-26.3.3-mac-arm64.dmg` | Apple Silicon (M1/M2/M3…) |
| **macOS** | `Solea-Pixel-26.3.3-mac-x64.dmg` | Intel Mac |
| **Updates** | `latest.yml` (+ `.blockmap`) | Used by the in-app updater on Windows |

### Upgrade

- **Windows:** run the new installer over the old install, or wait for the **in-app update** once `latest.yml` on this release points to **26.3.3**.
- **macOS:** open the DMG, drag **Solea Pixel** into **Applications**. CI builds are **not notarized**; if Gatekeeper blocks the app, use **System Settings → Privacy & Security** or **right-click → Open** the first time.

### In-app notes

Home & news → **Release notes** / **Release history** includes a **26.3.3** entry at the top.

---

**Repository:** [SILWOX/solea-pixel-launcher](https://github.com/SILWOX/solea-pixel-launcher) · **Site:** [soleapixel.com](https://soleapixel.com)
```

---

## Bloc 2 — Description GitHub (français, copier sous la ligne)

```markdown
## Solea Pixel Launcher **26.3.3** — version stable

### Nouveautés

- **Paramètres du launcher :** la zone **Expérimental (BÉTA)** est maintenant **sous Raccourcis clavier**, toujours dans **Paramètres → Launcher**. Elle regroupe **Mes serveurs (expérimental)** et **Interface du launcher** (AETHER **v2.0** ou **Legacy**).
- **Mise en page :** **Notifications**, **Discord Rich Presence**, **canal de mise à jour** et **Audio** sont de nouveau dans le flux principal des paramètres (plus de page « Expérimental » isolée dans la barre latérale).
- **macOS :** cette version propose des **DMG natifs** **arm64** (Apple Silicon) et **x64** (Intel). La [page de téléchargement](https://soleapixel.com) pointe le DMG **arm64** du `latest` GitHub lorsqu’il est publié.
- **CI :** un tag `v*` lance en parallèle le build **Windows** (installateur NSIS, portable, `latest.yml`) et les **DMG** macOS sur GitHub Actions.

### Téléchargements

| Plateforme | Fichier | Remarque |
|------------|---------|----------|
| **Windows** | `Solea-Pixel-Setup-26.3.3.exe` | Installateur NSIS (x64) |
| **Windows** | `Solea Pixel 26.3.3.exe` | Portable (x64) |
| **macOS** | `Solea-Pixel-26.3.3-mac-arm64.dmg` | Apple Silicon (M1/M2/M3…) |
| **macOS** | `Solea-Pixel-26.3.3-mac-x64.dmg` | Mac Intel |
| **Mises à jour** | `latest.yml` (+ `.blockmap`) | Mise à jour intégrée (Windows) |

### Mise à jour

- **Windows :** réinstalle par-dessus ou laisse le **launcher se mettre à jour** une fois le `latest.yml` de cette release en **26.3.3**.
- **macOS :** ouvre le DMG, glisse **Solea Pixel** dans **Applications**. Les builds CI **ne sont pas notarisés** ; si Gatekeeper bloque, va dans **Réglages système → Confidentialité et sécurité** ou **clic droit → Ouvrir** la première fois.

### Dans l’application

Accueil & actus → **Notes de version** / **Historique** : entrée **26.3.3** en tête.

---

**Dépôt :** [SILWOX/solea-pixel-launcher](https://github.com/SILWOX/solea-pixel-launcher) · **Site :** [soleapixel.com](https://soleapixel.com)
```

---

## macOS : pourquoi il n’y a pas de DMG sur ton PC Windows

`electron-builder` **refuse** de construire pour macOS hors macOS (`Build for macOS is supported only on macOS`). Les DMG « de la meilleure qualité possible » sont donc produits sur **`macos-latest`** (GitHub Actions) ou sur un Mac local avec `npm run dist:mac`.

**Obtenir les DMG sans refaire Windows :** dans GitHub → **Actions** → workflow **Release** → **Run workflow** : choisir le tag (ex. `v26.3.3`) et **macOS only** pour n’attacher que les DMG à la release existante (voir `release.yml`).
