# Obtenir les fichiers `.dmg` macOS (étape par étape)

Les DMG **ne peuvent pas** être créés sur Windows. Ils sont produits sur **macOS** (GitHub Actions ou Mac local).

---

## Prérequis

- Le dépôt est sur GitHub : `SILWOX/solea-pixel-launcher` (ou ton fork avec les mêmes workflows).
- Le fichier **`.github/workflows/release.yml`** est bien présent sur la branche **par défaut** (`main` ou `master`), avec le job **release-macos**.

---

## Méthode A — Tu crées une **nouvelle** release avec un tag

1. **Commit** toutes les modifications (version 26.3.3, site, etc.) et **pousse** sur GitHub.
2. Ouvre un terminal à la racine du dépôt (là où se trouve le dossier `.git`).
3. Crée le tag (remplace par ta version si besoin) :
   ```bash
   git tag v26.3.3
   ```
4. Envoie le tag sur GitHub :
   ```bash
   git push origin v26.3.3
   ```
5. Sur GitHub : **Actions** → workflow **Release** → vérifie que **release-windows** et **release-macos** tournent (verts à la fin).
6. Sur GitHub : **Releases** → ouvre la release **v26.3.3** → en bas, tu dois voir entre autres :
   - `Solea-Pixel-26.3.3-mac-arm64.dmg`
   - `Solea-Pixel-26.3.3-mac-x64.dmg`
7. Télécharge les DMG depuis cette page.

---

## Méthode B — La release **existe déjà** (ex. tu as déjà les `.exe`) mais **pas les DMG**

1. Assure-toi que le workflow à jour (avec **workflow_dispatch** et **macos-only**) est **poussé** sur la branche par défaut.
2. GitHub → onglet **Actions**.
3. Dans la liste à gauche, clique sur **Release**.
4. À droite : bouton **Run workflow** (lancer le workflow).
5. **Tag** : entre exactement le tag de la release, par exemple `v26.3.3`.
6. **Platform** : choisis **`macos-only`** (pour ne lancer que le build macOS).
7. Clique sur **Run workflow**.
8. Attends la fin du job **release-macos** (pastille verte).
9. Retourne sur **Releases** → ta release : les **DMG** sont ajoutés aux fichiers existants.

Si tu ne vois pas **Run workflow**, le `workflow_dispatch` n’est pas encore sur la branche par défaut : fais un **merge / push** du fichier `.github/workflows/release.yml` puis réessaie.

---

## Méthode C — Tu as un **Mac**

```bash
cd Launcher
npm ci
npm run dist:mac
```

Les DMG sont dans le dossier **`Launcher/release/`**.

---

## En cas de problème

- **Le job macOS est rouge** : ouvre le job → lis les logs (souvent dépendances, Node, ou quota GitHub).
- **Pas de release créée au push du tag** : vérifie que **GitHub Actions** est activé pour le dépôt et que le tag commence bien par **`v`** (ex. `v26.3.3`, pas `26.3.3` seul si le workflow filtre sur `v*`).
