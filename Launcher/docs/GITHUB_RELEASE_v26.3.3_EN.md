## Solea Pixel Launcher — **26.3.3 | Release** (package **26.3.3**)

### Highlights

- **Settings → Launcher:** the **Experimental (BETA)** block is now **under Keyboard shortcuts** (same tab): **My servers** toggle and **Launcher interface (AETHER v2.0 / Legacy)**. **Notifications**, **Discord Rich Presence**, **update channel**, and **Audio** are back in the main launcher flow (no longer grouped under a separate sidebar “Experimental” page).
- **macOS:** DMG builds for **arm64** (Apple Silicon) and **x64** (Intel) are attached to GitHub releases. The public site resolves the **arm64** DMG from the **latest** release when present.
- **CI:** pushing a tag `v*` runs **Windows** (NSIS + portable + `latest.yml`) and **macOS** (`macos-latest`) builds in parallel so one release ships both platforms.

### Built-in changelog

- New **26.3.3** entry at the top of **Home & news → Release notes** / **Release history**.

### Build artifacts

- **Windows installer (NSIS):** `Solea-Pixel-Setup-26.3.3.exe`
- **Windows portable:** `Solea Pixel 26.3.3.exe`
- **macOS DMG:** `Solea-Pixel-26.3.3-mac-arm64.dmg`, `Solea-Pixel-26.3.3-mac-x64.dmg`
- **Auto-update (Windows):** `latest.yml` (and `.blockmap` if published) next to the installer on the GitHub release.

### Upgrade notes

- Install over an older build or use **in-app update** once this release is published and `latest.yml` points to **26.3.3**.
- **macOS:** unsigned / non-notarized CI builds may trigger **Gatekeeper** warnings — open **System Settings → Privacy & Security** if needed, or right-click → Open the first time.

---

## Texte FR (copier-coller description GitHub / annonce)

**Solea Pixel Launcher — 26.3.3 | Release** (paquet **26.3.3**)

- **Paramètres → Launcher :** bloc **Expérimental (BÉTA)** **sous Raccourcis clavier** : **Mes serveurs** + **Interface du launcher (AETHER v2.0 / Legacy)**. **Notifications**, **Discord Rich Presence**, **canal de mise à jour** et **Audio** reviennent dans le flux principal (plus de page « Expérimental » séparée dans la barre latérale).
- **macOS :** DMG **arm64** et **x64** sur la release GitHub ; le site vitrine pointe le DMG **arm64** du `latest` quand il est présent.
- **CI :** tag `v*` → build **Windows** + **macOS** en parallèle sur la même release.

**Fichiers :** `Solea-Pixel-Setup-26.3.3.exe`, `Solea Pixel 26.3.3.exe`, `Solea-Pixel-26.3.3-mac-arm64.dmg`, `Solea-Pixel-26.3.3-mac-x64.dmg`, `latest.yml`.
