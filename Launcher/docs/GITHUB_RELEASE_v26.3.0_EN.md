## Solea Pixel Launcher — v26.3.0 (Release **26.3 | Release**)

### Highlights

- **My Server — local hosting:** spin up **dedicated Minecraft servers** from the same **Solea Pixel modpacks** you play in the launcher. Each server lives in its own folder under `userData/solea-server` so client instances stay separate from host files.
- **Install pipeline:** pulls the **Modrinth `.mrpack`**, runs the **NeoForge / Forge** server installer when required, writes **EULA**, and reports **installing / ready / error** states with surfaced messages in the UI.
- **Runtime console:** **Start** and **stop** the JVM, stream a bounded log, send **server commands** over stdin; uses the launcher’s configured **Java** path (automatically prefers `java.exe` when `javaw.exe` is set for the game client).
- **World & `server.properties`:** edit validated fields (port, slots, view distance, spawn protection, simulation distance, MOTD, etc.), optional **world folder delete**, and optional extra JVM flags via **`solea-jvm-extra.txt`** merged at launch.
- **Profiles & safety:** per-server **RAM**, **port**, name, description, optional **cover image** (size-capped), and a **confirmation modal** before switching the bound modpack on an existing server.
- **Version display:** footer and Home & news **Release notes** badge show **26.3 | Release** (technical package **26.3.0**); tooltip / “What’s new” still expose semver where relevant.

### Built-in changelog

- New **26.3** entry at the top of the in-app changelog; **26.2** and earlier remain under **Release history** in the same panel.

### Build artifacts

- **Installer (NSIS):** `Solea-Pixel-Setup-26.3.0.exe`
- **Portable:** `Solea Pixel 26.3.0.exe`
- **Auto-update:** attach `latest.yml` (and `.blockmap` if you ship differential updates) next to the installer on the GitHub release, per your existing workflow.

### Upgrade notes

- Install over an older build or use **in-app update** once this release is published and `latest.yml` points to **26.3.0**.
- **My Server** needs a working **Java** install and enough disk space for server files; first-time server install may take several minutes depending on the modpack.
