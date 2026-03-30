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

## Mises à jour

- `publish.url` dans `package.json` doit pointer vers un hébergeur qui sert `latest.yml` et les artefacts générés par `electron-builder`. Les clients vérifient les empreintes via **electron-updater** (SHA-512 dans le manifeste).
