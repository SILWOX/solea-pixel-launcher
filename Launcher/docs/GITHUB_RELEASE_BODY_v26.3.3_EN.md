## Solea Pixel Launcher **26.3.3** — stable release

### Summary

Launcher **settings / experimental** layout refresh, **native macOS DMGs** (Apple Silicon + Intel) on GitHub releases, and a **Windows + macOS** CI pipeline on a single version tag.

### What’s new

- **Settings → Launcher:** the **Experimental (BETA)** block now lives **under Keyboard shortcuts** on the same tab. It contains **My servers (experimental)** and **Launcher interface** (AETHER **v2.0** vs **Legacy**).
- **Navigation:** **Notifications**, **Discord Rich Presence**, **update channel**, and **Audio** are back in the main settings flow (no separate “Experimental” sidebar page).
- **macOS:** native **arm64** and **x64** DMGs ship with the GitHub release. The [soleapixel.com](https://soleapixel.com) download section resolves the **arm64** DMG from `latest` when it is published.
- **CI:** pushing a `v*` tag runs **Windows** (NSIS installer, portable `.exe`, `latest.yml`) and **macOS** (both DMGs) in parallel on GitHub Actions.

### Attach these assets to the release

| Platform | Filename |
|----------|----------|
| Windows (installer) | `Solea-Pixel-Setup-26.3.3.exe` |
| Windows (portable) | `Solea Pixel 26.3.3.exe` |
| macOS (Apple Silicon) | `Solea-Pixel-26.3.3-mac-arm64.dmg` |
| macOS (Intel) | `Solea-Pixel-26.3.3-mac-x64.dmg` |
| Auto-update (Windows) | `latest.yml` and `Solea-Pixel-Setup-26.3.3.exe.blockmap` |

### Install & upgrade

- **Windows:** run the installer over an existing install, or use **in-app update** once `latest.yml` references **26.3.3**.
- **macOS:** open the DMG, drag **Solea Pixel** into **Applications**. CI builds are **not notarized**; if Gatekeeper blocks launch, use **System Settings → Privacy & Security** or **right-click → Open** the first time.

### In the launcher

**Home & news** → release notes: **26.3.3** is the newest changelog entry.

---

**Repo:** [github.com/SILWOX/solea-pixel-launcher](https://github.com/SILWOX/solea-pixel-launcher) · **Site:** [soleapixel.com](https://soleapixel.com)
