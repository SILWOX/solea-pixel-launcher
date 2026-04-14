# Packaging Windows (NSIS, portable, signature)

## Installateur NSIS

- `npm run dist` produit un installateur NSIS et une build **portable** (voir `package.json` → `build.win.target`).
- Raccourcis bureau / menu Démarrer : options `nsis` dans `package.json` (`createDesktopShortcut`, `createStartMenuShortcut`, `shortcutName`).

## Build portable

- L’artefact **portable** est un `.exe` tout-en-un : décompresse l’app dans un dossier temporaire à chaque lancement. Les données utilisateur restent dans le profil Electron habituel (`%APPDATA%` ou équivalent), sauf si vous définissez `SOLEA_TEST_MODE` ou un `userData` personnalisé.
- Pour une installation « dossier unique » avec données à côté de l’exe, il faudrait une variante qui fixe `app.setPath('userData', …)` au démarrage — non activée par défaut.

## Signature de code (SmartScreen)

- Le champ `signAndEditExecutable` est à `false` par défaut. Pour la distribution publique, configurez **electron-builder** avec votre certificat Authenticode (variables d’environnement `CSC_LINK` / `CSC_KEY_PASSWORD` ou équivalent selon votre CI).
- Sans signature, Windows SmartScreen peut afficher un avertissement « application non reconnue ».

## Mises à jour (GitHub Releases)

1. Le dépôt GitHub est référencé dans **`repository.url`** et dans **`build.publish`** (`owner` / `repo`) : ils doivent rester alignés avec le repo réel (ex. `SILWOX/solea-pixel-launcher`).
2. Build local sans upload : `npm run dist` (dossier `release/`).
3. Publication CI : pousse un tag `v*` dont la version correspond à **`version`** dans `package.json` (ex. `26.1.3` → tag `v26.1.3`). Le workflow `.github/workflows/release.yml` lance `npm run dist:publish` avec `GH_TOKEN` pour créer la release et y attacher NSIS, portable et `latest.yml`.
4. **electron-updater** interroge l’API GitHub des releases, compare la version packagée à la dernière release (ou *prerelease* si le canal « beta » est choisi dans les réglages), télécharge l’installateur et vérifie les empreintes SHA-512.
5. Les mises à jour automatiques complètes (téléchargement + `quitAndInstall`) ciblent surtout l’**installateur NSIS**. L’exe **portable** peut être proposé en téléchargement manuel sur la même release ; le flux auto-update standard est pensé pour une app installée via NSIS.
