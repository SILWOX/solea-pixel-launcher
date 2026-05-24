package com.soleapixel.bmcmod.block.foundry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.soleapixel.bmcmod.registry.ModItems;

/**
 * Code-side melt rules for the Foundry.
 * One input item is consumed per process tick completion.
 */
public final class FoundryMeltRules {
    public record MeltRule(Item output, int minYield, int maxYield, int cookTime) {
    }

    public record MeltResult(ItemStack output, int cookTime) {
    }

    private static final Map<Item, MeltRule> RULES = new HashMap<>();
    private static final int DEFAULT_COOK_TIME = 205; // Very close to furnace (200) but still slightly slower
    public record RuleEntry(Item input, MeltRule rule) {
    }

    static {
        // Vanilla diamond gear
        gear(Items.DIAMOND_HELMET, Items.DIAMOND, 1, 5);
        gear(Items.DIAMOND_CHESTPLATE, Items.DIAMOND, 1, 7);
        gear(Items.DIAMOND_LEGGINGS, Items.DIAMOND, 1, 6);
        gear(Items.DIAMOND_BOOTS, Items.DIAMOND, 1, 4);
        gear(Items.DIAMOND_SWORD, Items.DIAMOND, 1, 3);
        gear(Items.DIAMOND_PICKAXE, Items.DIAMOND, 1, 4);
        gear(Items.DIAMOND_AXE, Items.DIAMOND, 1, 4);
        gear(Items.DIAMOND_SHOVEL, Items.DIAMOND, 1, 2);
        gear(Items.DIAMOND_HOE, Items.DIAMOND, 1, 2);
        gear(Items.DIAMOND_HORSE_ARMOR, Items.DIAMOND, 2, 5);

        // Vanilla iron gear
        gear(Items.IRON_HELMET, Items.IRON_INGOT, 1, 4);
        gear(Items.IRON_CHESTPLATE, Items.IRON_INGOT, 1, 6);
        gear(Items.IRON_LEGGINGS, Items.IRON_INGOT, 1, 5);
        gear(Items.IRON_BOOTS, Items.IRON_INGOT, 1, 3);
        gear(Items.IRON_SWORD, Items.IRON_INGOT, 1, 2);
        gear(Items.IRON_PICKAXE, Items.IRON_INGOT, 1, 3);
        gear(Items.IRON_AXE, Items.IRON_INGOT, 1, 3);
        gear(Items.IRON_SHOVEL, Items.IRON_INGOT, 1, 1);
        gear(Items.IRON_HOE, Items.IRON_INGOT, 1, 1);
        gear(Items.IRON_HORSE_ARMOR, Items.IRON_INGOT, 2, 4);
        gear(Items.CHAINMAIL_HELMET, Items.IRON_NUGGET, 2, 15);
        gear(Items.CHAINMAIL_CHESTPLATE, Items.IRON_NUGGET, 2, 24);
        gear(Items.CHAINMAIL_LEGGINGS, Items.IRON_NUGGET, 2, 21);
        gear(Items.CHAINMAIL_BOOTS, Items.IRON_NUGGET, 2, 12);

        // Vanilla gold gear
        gear(Items.GOLDEN_HELMET, Items.GOLD_INGOT, 1, 4);
        gear(Items.GOLDEN_CHESTPLATE, Items.GOLD_INGOT, 1, 6);
        gear(Items.GOLDEN_LEGGINGS, Items.GOLD_INGOT, 1, 5);
        gear(Items.GOLDEN_BOOTS, Items.GOLD_INGOT, 1, 3);
        gear(Items.GOLDEN_SWORD, Items.GOLD_INGOT, 1, 2);
        gear(Items.GOLDEN_PICKAXE, Items.GOLD_INGOT, 1, 3);
        gear(Items.GOLDEN_AXE, Items.GOLD_INGOT, 1, 3);
        gear(Items.GOLDEN_SHOVEL, Items.GOLD_INGOT, 1, 1);
        gear(Items.GOLDEN_HOE, Items.GOLD_INGOT, 1, 1);
        gear(Items.GOLDEN_HORSE_ARMOR, Items.GOLD_INGOT, 2, 4);

        // Netherite
        gear(Items.NETHERITE_HELMET, Items.NETHERITE_SCRAP, 1, 2);
        gear(Items.NETHERITE_CHESTPLATE, Items.NETHERITE_SCRAP, 1, 3);
        gear(Items.NETHERITE_LEGGINGS, Items.NETHERITE_SCRAP, 1, 3);
        gear(Items.NETHERITE_BOOTS, Items.NETHERITE_SCRAP, 1, 2);
        gear(Items.NETHERITE_SWORD, Items.NETHERITE_SCRAP, 1, 2);
        gear(Items.NETHERITE_PICKAXE, Items.NETHERITE_SCRAP, 1, 2);
        gear(Items.NETHERITE_AXE, Items.NETHERITE_SCRAP, 1, 2);
        gear(Items.NETHERITE_SHOVEL, Items.NETHERITE_SCRAP, 1, 1);
        gear(Items.NETHERITE_HOE, Items.NETHERITE_SCRAP, 1, 1);

        // Mod copper / emerald / obsidian / enderite / boreal
        gear(ModItems.COPPER_HELMET.get(), Items.COPPER_INGOT, 1, 4);
        gear(ModItems.COPPER_CHESTPLATE.get(), Items.COPPER_INGOT, 1, 6);
        gear(ModItems.COPPER_LEGGINGS.get(), Items.COPPER_INGOT, 1, 5);
        gear(ModItems.COPPER_BOOTS.get(), Items.COPPER_INGOT, 1, 3);
        gear(ModItems.COPPER_SWORD.get(), Items.COPPER_INGOT, 1, 2);
        gear(ModItems.COPPER_PICKAXE.get(), Items.COPPER_INGOT, 1, 3);
        gear(ModItems.COPPER_AXE.get(), Items.COPPER_INGOT, 1, 3);
        gear(ModItems.COPPER_SHOVEL.get(), Items.COPPER_INGOT, 1, 1);
        gear(ModItems.COPPER_HOE.get(), Items.COPPER_INGOT, 1, 1);

        gear(ModItems.EMERALD_HELMET.get(), Items.EMERALD, 1, 5);
        gear(ModItems.EMERALD_CHESTPLATE.get(), Items.EMERALD, 1, 7);
        gear(ModItems.EMERALD_LEGGINGS.get(), Items.EMERALD, 1, 6);
        gear(ModItems.EMERALD_BOOTS.get(), Items.EMERALD, 1, 4);
        gear(ModItems.EMERALD_SWORD.get(), Items.EMERALD, 1, 3);
        gear(ModItems.EMERALD_PICKAXE.get(), Items.EMERALD, 1, 4);
        gear(ModItems.EMERALD_AXE.get(), Items.EMERALD, 1, 4);
        gear(ModItems.EMERALD_SHOVEL.get(), Items.EMERALD, 1, 2);
        gear(ModItems.EMERALD_HOE.get(), Items.EMERALD, 1, 2);

        gear(ModItems.OBSIDIAN_HELMET.get(), Items.OBSIDIAN, 1, 4);
        gear(ModItems.OBSIDIAN_CHESTPLATE.get(), Items.OBSIDIAN, 1, 6);
        gear(ModItems.OBSIDIAN_LEGGINGS.get(), Items.OBSIDIAN, 1, 5);
        gear(ModItems.OBSIDIAN_BOOTS.get(), Items.OBSIDIAN, 1, 3);

        gear(ModItems.ENDERITE_HELMET.get(), ModItems.ENDERITE_SCRAP.get(), 1, 3);
        gear(ModItems.ENDERITE_CHESTPLATE.get(), ModItems.ENDERITE_SCRAP.get(), 1, 4);
        gear(ModItems.ENDERITE_LEGGINGS.get(), ModItems.ENDERITE_SCRAP.get(), 1, 3);
        gear(ModItems.ENDERITE_BOOTS.get(), ModItems.ENDERITE_SCRAP.get(), 1, 2);
        gear(ModItems.ENDERITE_SWORD.get(), ModItems.ENDERITE_SCRAP.get(), 1, 2);
        gear(ModItems.ENDERITE_PICKAXE.get(), ModItems.ENDERITE_SCRAP.get(), 1, 2);
        gear(ModItems.ENDERITE_AXE.get(), ModItems.ENDERITE_SCRAP.get(), 1, 2);
        gear(ModItems.ENDERITE_SHOVEL.get(), ModItems.ENDERITE_SCRAP.get(), 1, 1);
        gear(ModItems.ENDERITE_HOE.get(), ModItems.ENDERITE_SCRAP.get(), 1, 1);
        gear(ModItems.ENDERITE_SHIELD.get(), ModItems.ENDERITE_SCRAP.get(), 1, 2);

        gear(ModItems.BOREAL_HELMET.get(), ModItems.BOREAL_FRAGMENT.get(), 1, 3);
        gear(ModItems.BOREAL_CHESTPLATE.get(), ModItems.BOREAL_FRAGMENT.get(), 1, 4);
        gear(ModItems.BOREAL_LEGGINGS.get(), ModItems.BOREAL_FRAGMENT.get(), 1, 3);
        gear(ModItems.BOREAL_BOOTS.get(), ModItems.BOREAL_FRAGMENT.get(), 1, 2);
        gear(ModItems.BOREAL_SWORD.get(), ModItems.BOREAL_FRAGMENT.get(), 1, 2);
        gear(ModItems.BOREAL_PICKAXE.get(), ModItems.BOREAL_FRAGMENT.get(), 1, 2);
        gear(ModItems.BOREAL_AXE.get(), ModItems.BOREAL_FRAGMENT.get(), 1, 2);
        gear(ModItems.BOREAL_SHOVEL.get(), ModItems.BOREAL_FRAGMENT.get(), 1, 1);

        // Cool extra meltables
        fixed(Items.NETHER_STAR, ModItems.BOREAL_FRAGMENT.get(), 3);
        fixed(Items.BEACON, Items.DIAMOND, 10);
        fixed(ModItems.ENDERITE_BLOCK_ITEM.get(), ModItems.ENDERITE_INGOT.get(), 9);
        fixed(ModItems.BOREAL_BLOCK_ITEM.get(), ModItems.BOREAL_INGOT.get(), 9);
        fixed(ModItems.RUBY_BLOCK_ITEM.get(), ModItems.RUBY.get(), 9);
    }

    private static void gear(Item in, Item out, int min, int max) {
        int nerfedMax = max >= 3 ? max - 1 : max;
        int safeMax = Math.max(min, nerfedMax);
        RULES.put(in, new MeltRule(out, min, safeMax, DEFAULT_COOK_TIME));
    }

    private static void fixed(Item in, Item out, int count) {
        int nerfedCount = count >= 4 ? count - 1 : count;
        int safeCount = Math.max(1, nerfedCount);
        RULES.put(in, new MeltRule(out, safeCount, safeCount, DEFAULT_COOK_TIME));
    }

    public static MeltResult meltResult(ItemStack input) {
        if (input.isEmpty()) {
            return new MeltResult(ItemStack.EMPTY, DEFAULT_COOK_TIME);
        }
        MeltRule rule = RULES.get(input.getItem());
        if (rule == null) {
            return new MeltResult(ItemStack.EMPTY, DEFAULT_COOK_TIME);
        }
        int count = computeYield(input, rule);
        if (count <= 0) {
            return new MeltResult(ItemStack.EMPTY, rule.cookTime);
        }
        return new MeltResult(new ItemStack(rule.output(), count), rule.cookTime);
    }

    public static List<RuleEntry> allRules() {
        return RULES.entrySet().stream()
                .map(e -> new RuleEntry(e.getKey(), e.getValue()))
                .sorted((a, b) -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(a.input())
                        .toString()
                        .compareTo(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(b.input()).toString()))
                .toList();
    }

    public static int defaultCookTime() {
        return DEFAULT_COOK_TIME;
    }

    private static int computeYield(ItemStack input, MeltRule rule) {
        if (!input.isDamageableItem()) {
            return rule.maxYield();
        }
        int maxDamage = input.getMaxDamage();
        if (maxDamage <= 0) {
            return rule.maxYield();
        }
        int remaining = Math.max(0, maxDamage - input.getDamageValue());
        float ratio = Mth.clamp(remaining / (float) maxDamage, 0.0F, 1.0F);
        int spread = rule.maxYield() - rule.minYield();
        return rule.minYield() + Mth.floor(spread * ratio);
    }

    private FoundryMeltRules() {
    }
}
