# WIKI COMPLET — BETTER MINECRAFT (BMCMOD)

> Version cible: **26.0.3-Beta**  
> Minecraft: **1.21.1**  
> Loader: **NeoForge**  
> Mod ID: `bmcmod`

---

## 1) Vision du mod

Better Minecraft ajoute une progression complète qui part du **survival vanilla** et va vers des systèmes avancés:
- nouvelles lignes d'équipements (cuivre -> émeraude -> enderite -> boreal),
- nouveaux outils/armes (faux, arcs évolutifs, staffs, shields),
- machines (infusion, foundry, coffre XP, table d'upgrade, feeder),
- events PVE (Undead Invasion, End Storm, boss events),
- contenu exploration/biomes/ressources,
- progression guidée (quests, cristaux d'âme, métamorphose, advancements, commandes admin/joueur).

L'objectif gameplay est de proposer une **courbe de puissance longue**, avec du contenu orienté combat, farm, utilitaire et roleplay serveur.

---

## 2) Progression recommandée (ordre logique)

### Phase A — Début de partie
1. Récupère du **cuivre** et craft ton premier set cuivre.
2. Lance les premiers crafts utilitaires (backpack de base, quiver, outils).
3. Commence les premiers objectifs de quête (`Quest Log`).
4. Pose les premiers blocs de base (bois Hollow/Sunwood, déco, stockage).

### Phase B — Milieu de partie
1. Monte vers les sets **Émeraude** puis **Obsidian/Shulker** selon ton style.
2. Débloque les systèmes:
   - `Infusion Table`
   - `Upgrade Table`
   - `Foundry`
   - `Enchanted Chest`
   - `Feeder`
3. Commence les armes spéciales (faux avancées, arcs upgrades, staffs).
4. Rentre dans les events (`Undead Invasion`) pour le loot progression.

### Phase C — Fin de partie
1. Farm ressources End/anciennes (enderite, fossil, boreal fragments).
2. Upgrade complet vers **Enderite** puis **Boreal**.
3. Maîtrise `End Storm`, mini-boss et loot de haut niveau.
4. Finalise les builds méta: builder wand, upgrades chestplate, combo enchantements.

---

## 3) Contenu principal par catégorie

### 3.1 Armes, outils et combat

#### Faux (ligne complète)
- `Iron Scythe`
- `Golden Scythe`
- `Diamond Scythe`
- `Emerald Scythe`
- `Netherite Scythe`
- `Enderite Scythe`
- `Boreal Scythe`

Mécaniques clefs:
- mode zone configurable (agri/labour/récolte),
- attaque mêlée volontairement plus lente,
- charge Whirlwind puis frappe de zone.

#### Arcs
- `Lightning Bow`
- progression `Iron -> Gold -> Diamond -> Emerald` via smithing.

#### Staffs
- `End Staff`
- `Dragon Staff`
- `Wither Staff`
- `Flame Staff`
- `Ice Staff`
- `Echo Staff`

Les staffs utilisent des modes (cycle) + coût/charges + effets contextuels (téléport, beam, invocation, etc.).

#### Boucliers
- `Diamond Shield`
- `Netherite Shield`
- `Enderite Shield`

### 3.2 Armures et équipements

#### Lignes d'armure
- Cuivre
- Émeraude
- Shulker
- Obsidian
- Enderite
- Boreal

#### Équipements spéciaux
- Chapeaux métiers: butcher, librarian, weaponsmith, shepherd, fisherman, cartographer, armorer, farmer
- `Witch Hat`
- `Undead Crown`
- `Sky Boots`

#### Utilitaires inventaire
- `Backpack` + versions `Iron/Gold/Diamond/Emerald`
- `Quiver`
- `Pocket Ender Chest`

### 3.3 Blocs, machines et automatisation

Blocs de machine:
- `Infusion Table`
- `Upgrade Table`
- `Foundry`
- `Enchanted Chest`
- `Endstone Furnace`
- `Experience Liquid`
- `Feeder`

Ressources/minerais/blocs de progression:
- `Ruby Ore`, `Deepslate Ruby Ore`, `Block of Ruby`
- familles géodes: `Nebrith`, `Opal`, `Topaz`, `Beryl`
- debris: `Forgotten Debris`, `Fossil Debris`

Bois/biomes/building:
- famille `Hollow`
- famille `Sunwood` (incluant boat/chest boat, panneaux, etc.)

### 3.4 Mobs, PNJ et events

#### Entités notables
- `Diamond Golem`
- `Blink`
- `End Golem`
- `Endling`
- `Mimic Chest`
- `Skeleton Villager`
- `Bounty Hunter`
- `Undead Illager`
- `Vlinx`
- `Radiant Slime`
- `Quest Trader`

#### Events
- **Undead Invasion**: vagues, capitaines, progression niveau, coffres de récompense.
- **End Storm**: évènement The End avec vagues/loot dédiés.
- **Boss Event**: spawn mini-boss random ou forcé via commandes.

### 3.5 Progression systems

- `Quest Log`: quêtes randomisées (kill, gather, mine, trade, fish, craft, explore, treasure, bounty).
- `Crystal`: capture d'âmes pour infusion.
- `Morph Crystal` + `Capture Crystal`: métamorphose/capture d'entités (hors boss/joueurs selon règles).
- progression flacons (`Undead Bottle I -> VI`, `End Storm Bottle I -> IV`).
- énorme arbre d'`advancement.bmcmod.*` (100+ entre progression et challenges).

### 3.6 Enchantements custom

Liste principale:
- `Bleeding`
- `Life Steal`
- `Timber`
- `Crushing Blow`
- `Fire Thorn`
- `Explosive Shot`
- `Auto-Smelt`
- `Curse of Empathic Strike`
- `Curse of Launchstrike`
- `Shield Charge`
- `Excavator`
- `Whirlwind`
- `Wide Sweep`
- `Reaping`
- `Briar Ring`

### 3.7 Commandes (serveur + debug + admin)

Commandes exposées via aide in-game:
- `/bmc` (et sous-commandes)
- `/givehead`
- `/metamorph`
- `/nether`, `/end`, `/overworld`
- `/bmc quest give ...`
- `/bmc crystal ...`
- `/bmc undeadinvasion ...`
- `/bmc endstorm ...`
- `/bmc bossevent spawn ...`

---

## 4) Biomes / monde

Biomes détectés:
- `Hollow Garden`
- `Solea Grove`

---

## 5) Chiffres utiles (snapshot wiki)

D'après `src/main/resources/assets/bmcmod/lang/en_us.json`:
- entrées `item.bmcmod.*`: **320**
- entrées `block.bmcmod.*`: **100**
- entrées `entity.bmcmod.*`: **13**
- entrées `enchantment.bmcmod.*`: **15**
- entrées `advancement.bmcmod.*`: **100**

---

## 6) Focus crafting/upgrade (exemples)

Exemples vérifiés dans les recipes JSON:
- `enderite_ingot.json`: 4 `enderite_scrap` + 4 `minecraft:diamond` -> 1 `enderite_ingot`
- `boreal_ingot.json`: 4 `boreal_fragment` + 4 `fossil_scrap` -> 1 `boreal_ingot`
- nombreuses recipes `minecraft:smithing_transform` (arcs, staffs, scythes, enderite, boreal, shields, builder wands).

---

## 7) Architecture assets/data (pour le site wiki)

Namespace principal: `bmcmod`

Dossiers source de référence:
- `src/main/resources/assets/bmcmod/`
- `src/main/resources/data/bmcmod/`
- `src/main/resources/data/c/` (tags interop)
- `assets/minecraft/lang/en_us.json` (fichier additionnel volumineux présent à la racine projet)

Fichiers clefs pour la documentation web:
- `src/main/resources/assets/bmcmod/lang/en_us.json` (noms, textes, UX, HUD, commandes)
- `src/main/resources/data/bmcmod/recipe/*.json`
- `src/main/resources/data/bmcmod/enchantment/*.json`
- `src/main/resources/data/bmcmod/advancement/**/*.json`
- `src/main/resources/data/bmcmod/worldgen/**/*.json`
- `src/main/resources/data/bmcmod/loot_table/**/*.json`

---

## 8) Copie ASSETS intégrée dans ce document (pour transfert Cursor)

> Important: le fichier `assets/minecraft/lang/en_us.json` dépasse **400k caractères**.
> Pour éviter de casser un prompt, le mieux est d'envoyer au second projet:
> 1) ce markdown,
> 2) les fichiers source listés ci-dessous en pièce jointe.

### 8.1 Manifest de copie (chemins exacts)

```text
src/main/resources/assets/bmcmod/lang/en_us.json
src/main/resources/assets/bmcmod/README.txt
src/main/resources/assets/minecraft/atlases/shield_patterns.json
src/main/resources/data/bmcmod/**
assets/minecraft/lang/en_us.json
```

### 8.2 Extrait assets — `src/main/resources/assets/bmcmod/README.txt`

```text
Texture layout for modders / resource pack authors: see repository root: docs/bmcmod_resource_layout.md
Namespace: bmcmod. Override path pattern: <pack.zip>/assets/bmcmod/...
```

### 8.3 Extrait assets — `src/main/resources/assets/minecraft/atlases/shield_patterns.json`

```json
{
  "sources": [
    { "type": "single", "resource": "entity/shield_base" },
    { "type": "single", "resource": "entity/shield_base_nopattern" },
    { "type": "directory", "source": "entity/shield", "prefix": "entity/shield/" },
    { "type": "single", "resource": "bmcmod:entity/shield/diamond_shield" },
    { "type": "single", "resource": "bmcmod:entity/shield/netherite_shield" },
    { "type": "single", "resource": "bmcmod:entity/shield/enderite_shield" }
  ]
}
```

### 8.4 Extrait assets — localisation `en_us` (sample)

```json
{
  "itemGroup.bmcmod": "Better Minecraft",
  "biome.bmcmod.hollow_garden": "Hollow Garden",
  "biome.bmcmod.solea_grove": "Solea Grove",
  "item.bmcmod.enderite_sword": "Enderite Sword",
  "item.bmcmod.boreal_sword": "Boreal Sword",
  "item.bmcmod.end_staff": "End Staff",
  "item.bmcmod.dragon_staff": "Dragon Staff",
  "item.bmcmod.wither_staff": "Wither Staff",
  "item.bmcmod.quest_log": "Quest Log",
  "entity.bmcmod.bounty_hunter": "Bounty Hunter",
  "entity.bmcmod.radiant_slime": "Radiant Slime",
  "enchantment.bmcmod.whirlwind": "Whirlwind",
  "enchantment.bmcmod.reaping": "Reaping",
  "advancement.bmcmod.root.title": "Better Minecraft"
}
```

---

## 9) Recommandation pour construire ton site wiki

Pour ton autre projet Cursor (site web):
1. Utiliser ce fichier comme **document maître**.
2. Importer en plus:
   - `src/main/resources/assets/bmcmod/lang/en_us.json`
   - `src/main/resources/data/bmcmod/`
3. Générer des pages auto:
   - `/items`
   - `/blocks`
   - `/entities`
   - `/enchantments`
   - `/events`
   - `/commands`
   - `/progression`
4. Ajouter un moteur de recherche par ID (`bmcmod:*`).

---

## 10) Crédits

- Studio: **SOLEA PIXEL**
- Site: https://soleapixel.com
