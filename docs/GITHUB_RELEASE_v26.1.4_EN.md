## Solea Pixel Launcher — v26.1.4

### What's new
- **Reporting**: **REPORT** card on Home under Follow us, with a short intro and an orange button to open the form.
- **Report modal**: choose **Launcher** or **Instance (modpack)**; switching scope resets the category to the first item in the list.
- **Instance categories**: launch / crash, install / update, in-game account, verify files / integrity, mods / in-game crash, other.
- **Launcher categories**: UI / display, Microsoft sign-in / launcher account, launcher updates, downloads / cache, freezes / slow launcher, other.
- **Selectors**: same component as elsewhere in the launcher (consistent light / dark styling, visible focus in the modal).
- **Help & Discord**: collapsible help panel (? button), **Join Discord** button, free-text details field, highlighted (orange) checkbox to attach launcher version, modpack (when instance), and OS.
- **Actions**: copy report (Markdown) to clipboard, send to team (when configured), close; focus trap in the modal and **Escape** to dismiss.
- **Discord delivery**: set environment variable `SOLEA_REPORT_WEBHOOK_URL` **or** put the URL in `solea-report-webhook.url` in the launcher userData folder (first line = URL). See `docs/REPORTING.md`. The repo ignores that filename in `.gitignore` so secrets are not committed.

### Changes
- Home & news: Follow us block is a bit tighter; hero header is re-centered (report entry is no longer in the hero bar).
- Reporting copy avoids “webhook” jargon in the UI; if sending is not set up, a toast explains how to enable it (env var or local file + doc).

### Fixes
- Main process only accepts Discord webhook URLs on `discord.com` / `canary.discord.com`; invalid or empty config is ignored.
- Message body truncated (~1900 characters) before send to stay within Discord limits.
- Network / HTTP errors surfaced to the UI with detail for the error toast; empty content rejected cleanly.
- Clipboard copy failure shows a dedicated error toast instead of failing silently.

### Build artifacts
- Installer: `Solea-Pixel-Setup-26.1.4.exe`
- Portable: `Solea Pixel 26.1.4.exe`
