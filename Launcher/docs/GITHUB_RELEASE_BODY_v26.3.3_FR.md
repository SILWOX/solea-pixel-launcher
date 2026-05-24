## Solea Pixel Launcher **26.3.3** — version stable

### Résumé

Mise à jour **paramètres / expérimental**, **DMG macOS** (Apple Silicon + Intel) sur les releases GitHub, et pipeline CI **Windows + macOS** sur un même tag.

### Nouveautés

- **Paramètres → Launcher** : le bloc **Expérimental (BÉTA)** est placé **sous Raccourcis clavier** sur le même onglet. Il regroupe **Mes serveurs (expérimental)** et le choix d’**interface du launcher** (AETHER **v2.0** ou **Legacy**).
- **Navigation** : **Notifications**, **Discord Rich Presence**, **canal de mise à jour** et **Audio** sont de nouveau dans le flux principal des réglages (plus de page « Expérimental » isolée dans la barre latérale).
- **macOS** : DMG natifs **arm64** (M1/M2/M3…) et **x64** (Intel), publiés sur GitHub avec la release. La page [soleapixel.com](https://soleapixel.com) propose le téléchargement du DMG **arm64** lorsqu’il est présent sur `latest`.
- **CI** : en poussant un tag `v*`, GitHub Actions construit **Windows** (installateur NSIS, portable, `latest.yml`) et **macOS** (les deux DMG) en parallèle.

### Fichiers à joindre à la release

| Plateforme | Nom du fichier |
|------------|------------------|
| Windows (installateur) | `Solea-Pixel-Setup-26.3.3.exe` |
| Windows (portable) | `Solea Pixel 26.3.3.exe` |
| macOS (Apple Silicon) | `Solea-Pixel-26.3.3-mac-arm64.dmg` |
| macOS (Intel) | `Solea-Pixel-26.3.3-mac-x64.dmg` |
| Mise à jour auto (Windows) | `latest.yml` et `Solea-Pixel-Setup-26.3.3.exe.blockmap` |

### Installation & mise à jour

- **Windows** : lance l’installateur par-dessus une ancienne version, ou utilise la **mise à jour intégrée** une fois `latest.yml` pointant sur **26.3.3**.
- **macOS** : ouvre le DMG, glisse **Solea Pixel** dans **Applications**. Les builds CI ne sont **pas notarisés** : si macOS bloque l’ouverture, utilise **Réglages système → Confidentialité et sécurité** ou **clic droit sur l’app → Ouvrir** la première fois.

### Dans le launcher

**Accueil & actus** → notes de version : entrée **26.3.3** en tête de l’historique.

---

**Dépôt** : [github.com/SILWOX/solea-pixel-launcher](https://github.com/SILWOX/solea-pixel-launcher) · **Site** : [soleapixel.com](https://soleapixel.com)
