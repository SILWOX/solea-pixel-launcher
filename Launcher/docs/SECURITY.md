# Sécurité — jetons et données locales

## Comptes Microsoft / Minecraft

- Les jetons OAuth (refresh, access) sont stockés **localement** dans le profil utilisateur du launcher (dossier `userData` Electron), pas sur nos serveurs.
- Sur **Windows**, le chiffrement au repos peut s’appuyer sur les mécanismes système selon la version d’Electron et la configuration du compte utilisateur (DPAPI / coffre-fort). Ne considérez pas le stockage comme inviolable sur une machine compromise.
- Ne partagez jamais le dossier `userData` ou une sauvegarde non chiffrée contenant `launcher-settings.json` et les fichiers de comptes.

## Ouverture de liens externes (`openExternal`)

- Seules les URL **HTTPS** dont l’hôte figure sur la liste blanche du processus principal (`safeOpenExternal.ts`) peuvent être ouvertes depuis le renderer.
- Toute tentative hors liste est **journalisée** (`solea-main.log`) et ignorée.

## Téléchargements Modrinth

- Chaque fichier du `.mrpack` est vérifié par **SHA-512** après téléchargement (voir `modrinth.ts`).
- Les métadonnées de versions Modrinth sont mises en **cache mémoire** avec TTL pour limiter les requêtes API.
