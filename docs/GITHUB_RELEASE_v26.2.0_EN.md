## Solea Pixel Launcher — v26.2.0 (Release **26.2 | Release**)

### Highlights
- **AETHER UI — Home & news**: cleaner hub with hero eyebrow, latest release highlighted inside the **Release notes** column, and **one scrollbar** for patch content only (hero + side columns stay fixed; account actions stay visible without scrolling the whole page).
- **AETHER UI — Screenshots**: page-level scroll with the same AETHER scrollbar treatment; cards use the shared glass tokens and an orange **top accent** strip aligned with Settings / Account.
- **AETHER UI — Left rail**: refined sidebar sections, clearer default / hover / active states, optional thin scroll for many modpacks, improved frosted-glass styling when **Extend wallpaper behind title bar & sidebar** is enabled.
- **Home (modpack)**: when a pack is **not installed**, the **Check update** control and **account selector** are hidden until after install (install CTA only).
- **Navigation**: modpack icons in the left rail show the active outline **only on the modpack home screen** — switching to Settings, Home & news, etc. no longer leaves the previous pack looking selected.
- **Version display**: Home & news **Release notes** badge matches the footer (**26.2 | Release**); tooltip can still show the technical semver from the app when available.

### Built-in changelog
- New **26.2** entry (English) at the top of the in-app changelog; older versions remain under **Release history** in the same scroll area.

### Build artifacts
- **Installer (NSIS):** `Solea-Pixel-Setup-26.2.0.exe`
- **Portable:** `Solea Pixel 26.2.0.exe`
- **Auto-update:** attach `latest.yml` (and `.blockmap` if you use differential updates) next to the installer on your release, per your existing GitHub workflow.

### Upgrade notes
- Install over an older build or use in-app update once this release is published and `latest.yml` points to **26.2.0**.
