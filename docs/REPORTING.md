# Signalements (rapports utilisateur)

Le launcher peut **envoyer** le texte du formulaire « Signaler un problème » vers un **webhook Discord** entrant, sans serveur dédié.

## Configuration

1. Dans Discord : **Paramètres du salon** → **Intégrations** → **Webhooks** → **Nouveau webhook**.
2. Copiez l’**URL du webhook** (elle contient un secret — ne la partagez pas et ne la commitez pas).
3. Choisissez **une** des options suivantes :

   - **Variable d’environnement** **`SOLEA_REPORT_WEBHOOK_URL`** avant de lancer l’app (script, raccourci Windows, terminal).

     Exemple (PowerShell, session courante) :

     ```powershell
     $env:SOLEA_REPORT_WEBHOOK_URL = "https://discord.com/api/webhooks/..."
     ```

   - **Fichier local (pratique pour les tests)** : créez un fichier nommé exactement **`solea-report-webhook.url`** dans le dossier **données utilisateur** du launcher (même endroit que les logs / instances). Mettez **une seule ligne** : l’URL du webhook, puis enregistrez. Au prochain lancement, **Envoyer à l’équipe** utilisera cette URL (priorité à la variable d’environnement si elle est aussi définie).

     Emplacement type sous Windows : `%APPDATA%\solea-pixel-launcher\solea-report-webhook.url` (le nom du dossier peut suivre le `productName` / `name` Electron — vérifiez avec **Ouvrir le dossier données** dans les paramètres du launcher si besoin).

     Ne versionnez pas ce fichier ; traitez-le comme un secret.

## Comportement

- Le corps du message est envoyé en JSON : `{ "content": "..." }`.
- La longueur est limitée côté launcher (troncature ~1900 caractères) pour rester sous la limite Discord (~2000).
- Si la variable n’est pas définie, le bouton **Envoyer** affiche une explication : l’utilisateur peut toujours **copier** le rapport et le coller sur Discord ou par e-mail.

## Sécurité

- Traitez l’URL du webhook comme un **mot de passe** : toute personne qui la possède peut poster dans le salon.
- Préférez un salon dédié « support » avec permissions restrictives.
- Ne versionnez pas l’URL dans le dépôt ; utilisez l’environnement ou un gestionnaire de secrets sur vos builds internes.

## Évolutions possibles

- Webhook avec **embed** ou pièces jointes (fichier log) via multipart.
- Backend persistant (base de données, ticketing) si vous dépassez le confort du webhook.
