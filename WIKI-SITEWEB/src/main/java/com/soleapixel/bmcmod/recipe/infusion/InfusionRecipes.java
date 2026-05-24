package com.soleapixel.bmcmod.recipe.infusion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.soleapixel.bmcmod.registry.ModItems;

/**
 * Registre en code des recettes d'infusion (même multiset que les 4 emplacements 2x2, ordre libre).
 */
public final class InfusionRecipes {
    public static final List<InfusionRecipe> ENTRIES = new ArrayList<>();
    private static boolean registered;

    public static void registerDefaultRecipes() {
        if (registered) {
            return;
        }
        registered = true;
        // Lightning Bow — 15 Breeze Rod, 15 Blink Rod, 1 Bow, 4 Netherite Ingot
        ENTRIES.add(
                new InfusionRecipe(
                        72,
                        new ItemStack(ModItems.LIGHTNING_BOW.get()),
                        Map.of(
                                Items.BREEZE_ROD,
                                15,
                                ModItems.BLINK_ROD.get(),
                                15,
                                Items.BOW,
                                1,
                                Items.NETHERITE_INGOT,
                                4)));
        // Morph Crystal — 32 Diamond, 1 Crystal, 10 Ruby, 1 Skeleton Skull
        ENTRIES.add(
                new InfusionRecipe(
                        56,
                        new ItemStack(ModItems.MORPH_CRYSTAL.get()),
                        Map.of(
                                Items.DIAMOND,
                                32,
                                ModItems.CRYSTAL.get(),
                                1,
                                ModItems.RUBY.get(),
                                10,
                                Items.SKELETON_SKULL,
                                1)));
        // Capture Crystal — 32 Diamond, 1 Crystal, 32 Emerald, 1 Creeper Head
        ENTRIES.add(
                new InfusionRecipe(
                        56,
                        new ItemStack(ModItems.CAPTURE_CRYSTAL.get()),
                        Map.of(
                                Items.DIAMOND,
                                32,
                                ModItems.CRYSTAL.get(),
                                1,
                                Items.EMERALD,
                                32,
                                Items.CREEPER_HEAD,
                                1)));
        // Sky Boots — 120 âmes, 32 plumes, 1 bottes émeraude (mod), 2 pattes de lapin
        ENTRIES.add(
                new InfusionRecipe(
                        120,
                        new ItemStack(ModItems.SKY_BOOTS.get()),
                        Map.of(
                                Items.FEATHER,
                                32,
                                ModItems.EMERALD_BOOTS.get(),
                                1,
                                Items.RABBIT_FOOT,
                                2)));
        // Echo Staff — 120 âmes, 12 éclats d’écho, 1 crâne squelette, 2 tendrils warden, 8 lingots de fer
        ENTRIES.add(
                new InfusionRecipe(
                        120,
                        new ItemStack(ModItems.ECHO_STAFF.get()),
                        Map.of(
                                Items.ECHO_SHARD,
                                12,
                                Items.SKELETON_SKULL,
                                1,
                                ModItems.WARDEN_TENDRIL.get(),
                                2,
                                Items.IRON_INGOT,
                                8)));
        // Flame Staff — 100 âmes, bâton de blaze, charges de feu, poudre / crème magma
        ENTRIES.add(
                new InfusionRecipe(
                        100,
                        new ItemStack(ModItems.FLAME_STAFF.get()),
                        Map.of(
                                Items.BLAZE_ROD,
                                2,
                                Items.FIRE_CHARGE,
                                8,
                                Items.BLAZE_POWDER,
                                12,
                                Items.MAGMA_CREAM,
                                4)));
        // Ice Staff — 115 âmes, glace compacte/bleue, neige compacte et seau de neige poudreuse
        ENTRIES.add(
                new InfusionRecipe(
                        115,
                        new ItemStack(ModItems.ICE_STAFF.get()),
                        Map.of(
                                Items.PACKED_ICE,
                                24,
                                Items.BLUE_ICE,
                                8,
                                Items.SNOW_BLOCK,
                                32,
                                Items.POWDER_SNOW_BUCKET,
                                1)));
        // End Staff — 110 âmes, perles, bâtons de l’End, pierre de l’End, fruit de chorus
        ENTRIES.add(
                new InfusionRecipe(
                        110,
                        new ItemStack(ModItems.END_STAFF.get()),
                        Map.of(
                                Items.ENDER_PEARL,
                                16,
                                Items.END_ROD,
                                4,
                                Items.END_STONE,
                                32,
                                Items.CHORUS_FRUIT,
                                8)));
        // Dragon Staff — 115 âmes, souffle, tête dragon, 4 cristaux de l’End
        ENTRIES.add(
                new InfusionRecipe(
                        115,
                        new ItemStack(ModItems.DRAGON_STAFF.get()),
                        Map.of(
                                Items.DRAGON_BREATH,
                                6,
                                Items.DRAGON_HEAD,
                                1,
                                Items.END_CRYSTAL,
                                4)));
        // Wither Staff — 115 âmes, 3 têtes de wither squelette, 3 nether stars, 32 os, 20 blaze rods
        ENTRIES.add(
                new InfusionRecipe(
                        115,
                        new ItemStack(ModItems.WITHER_STAFF.get()),
                        Map.of(
                                Items.WITHER_SKELETON_SKULL,
                                3,
                                Items.NETHER_STAR,
                                3,
                                Items.BONE,
                                32,
                                Items.BLAZE_ROD,
                                20)));
        // Pocket Ender Chest — 5 âmes, 1 coffre de l’End, 6 perles de l’End, 1 sac à dos (base)
        ENTRIES.add(
                new InfusionRecipe(
                        5,
                        new ItemStack(ModItems.POCKET_ENDER_CHEST.get()),
                        Map.of(
                                Items.ENDER_CHEST,
                                1,
                                Items.ENDER_PEARL,
                                6,
                                ModItems.BACKPACK.get(),
                                1)));
        // Perle instable — 150 âmes, 1 perle de l’End, pierre de l’End, chorus, obsidienne (matériaux de l’End instable)
        ENTRIES.add(
                new InfusionRecipe(
                        150,
                        new ItemStack(ModItems.UNSTABLE_PEARL.get()),
                        Map.of(
                                Items.ENDER_PEARL,
                                1,
                                Items.END_STONE,
                                48,
                                Items.CHORUS_FRUIT,
                                16,
                                Items.OBSIDIAN,
                                8)));
    }

    public static Optional<InfusionRecipe> findMatch(ItemStack in0, ItemStack in1, ItemStack in2, ItemStack in3) {
        Map<Item, Integer> grid = InfusionRecipe.multisetFromInputs(in0, in1, in2, in3);
        for (InfusionRecipe r : ENTRIES) {
            if (r.matchesGrid(grid)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /** Résultat d’une recette enregistrée (ex. affichage de l’âme dans l’inventaire). */
    public static Optional<InfusionRecipe> findRecipeProducingItem(Item resultItem) {
        for (InfusionRecipe r : ENTRIES) {
            if (r.result.getItem() == resultItem) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    private InfusionRecipes() {
    }
}
