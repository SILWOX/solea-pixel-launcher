## Solea Pixel — **26.3 | Release** (launcher **v26.3.0**)

This release ships the **Solea Pixel Launcher 26.3.0** together with updates to the **public website** so downloads, news, and presentation stay aligned with the product.

### Launcher — what’s new

**My Server (local hosting)**  
Host **dedicated Minecraft servers** from the same **Solea Pixel modpacks** you use in the launcher. Each server gets its own directory under **`userData/solea-server`**, separate from your client instances.

**Install & runtime**
- Pulls the **Modrinth `.mrpack`** and runs the **NeoForge / Forge** server installer when needed.
- **EULA** is handled as part of setup; the UI shows **installing / ready / error** with clear feedback.
- **Start / stop** the server JVM, stream a **bounded console log**, and send **server commands** to the running process.
- Uses the **Java path from launcher settings** (if you use `javaw.exe` for the game, the host path switches to **`java.exe`** for the server).

**World & configuration**
- Edit **`server.properties`** through validated fields (port, max players, view distance, spawn protection, simulation distance, MOTD, etc.).
- Optional **world folder reset** when you need a clean world.
- Optional extra JVM arguments via **`solea-jvm-extra.txt`**, merged at launch after the main `-Xmx` line.

**Profiles & safety**
- Per-server **RAM**, **port**, name, description, and optional **cover image** (with a size limit).
- **Changing the bound modpack** on an existing server requires an explicit **confirmation modal** so mistakes are harder.

**Release notes in-app**
- Footer and **Home & news** badge show **26.3 | Release** (package **26.3.0**).
- Built-in changelog: new **26.3** section at the top; **26.2** and older entries remain under **Release history**.
- **What’s new** copy updated for **26.3.0** (EN/FR).

### Website

- **Section headers**: clearer **pill + rail** styling (stronger contrast, borders, glow, line and anchor dot); **FAQ** uses a matching **blue** treatment.
- **Between sections**: softer **atmospheric** transitions instead of harsh cuts; adjusted for **`prefers-reduced-motion`**.
- **Navigation**: removed the small **SOCIAL** / **LANG** labels above the social icons and language switch (cleaner bar; **`aria-label`** kept for accessibility).
- **News**: **live news** block on the site when configured (cards and markup aligned with launcher-style announcements).

### Downloads & auto-update

| Artifact | Filename |
|----------|----------|
| **Installer (NSIS)** | `Solea-Pixel-Setup-26.3.0.exe` |
| **Portable** | `Solea Pixel 26.3.0.exe` |
| **Update metadata** | `latest.yml` (+ `Solea-Pixel-Setup-26.3.0.exe.blockmap` if you use differential updates) |

Attach **`latest.yml`** (and **`.blockmap`** if applicable) next to the NSIS installer on this release so **in-app updates** resolve correctly.

### Upgrade notes

- Install over an older build, or use **in-app update** once **`latest.yml`** points to **26.3.0**.
- **My Server** expects a working **Java** installation and enough **disk space**; first server install can take **several minutes** depending on the modpack.

Full launcher notes: `docs/GITHUB_RELEASE_v26.3.0_EN.md` in the repository.
