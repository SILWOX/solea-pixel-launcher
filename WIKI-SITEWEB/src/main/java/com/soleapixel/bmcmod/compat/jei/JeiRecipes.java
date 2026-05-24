package com.soleapixel.bmcmod.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.soleapixel.bmcmod.recipe.infusion.InfusionRecipe;
import com.soleapixel.bmcmod.recipe.infusion.InfusionRecipes;
import com.soleapixel.bmcmod.block.foundry.FoundryMeltRules;
import com.soleapixel.bmcmod.item.upgrade.ChestplateUpgradeData;
import com.soleapixel.bmcmod.registry.ModItems;

/**
 * Enregistrement des recettes côté JEI : adaptateur autour de {@link InfusionRecipe}.
 */
public final class JeiRecipes {
    private JeiRecipes() {
    }

    public static List<JeiInfusionRecipe> allInfusion() {
        InfusionRecipes.registerDefaultRecipes();
        List<JeiInfusionRecipe> out = new ArrayList<>();
        for (InfusionRecipe r : InfusionRecipes.ENTRIES) {
            out.add(new JeiInfusionRecipe(r));
        }
        return out;
    }

    public static List<JeiFoundryRecipe> allFoundry() {
        List<JeiFoundryRecipe> out = new ArrayList<>();
        for (FoundryMeltRules.RuleEntry e : FoundryMeltRules.allRules()) {
            out.add(new JeiFoundryRecipe(
                    new ItemStack(e.input()),
                    new ItemStack(e.rule().output(), e.rule().maxYield()),
                    e.rule().minYield(),
                    e.rule().maxYield(),
                    e.rule().cookTime()));
        }
        return out;
    }

    public static List<JeiUpgradeRecipe> allUpgradeTable() {
        List<ItemStack> upgrades = List.of(
                new ItemStack(ModItems.ARMOR_UPGRADE.get()),
                new ItemStack(ModItems.DISCRETION_UPGRADE.get()),
                new ItemStack(ModItems.FROST_WALK_UPGRADE.get()),
                new ItemStack(ModItems.HEALTH_UPGRADE.get()),
                new ItemStack(ModItems.LUCK_UPGRADE.get()),
                new ItemStack(ModItems.RANGE_UPGRADE.get()),
                new ItemStack(ModItems.SPEED_UPGRADE.get()),
                new ItemStack(ModItems.STEP_UPGRADE.get()),
                new ItemStack(ModItems.STRENGHT_UPGRADE.get()),
                new ItemStack(ModItems.DASH_UPGRADE.get()),
                new ItemStack(ModItems.RAGE_UPGRADE.get()),
                new ItemStack(ModItems.HEAL_UPGRADE.get()),
                new ItemStack(ModItems.CRITICAL_UPGRADE.get()),
                new ItemStack(ModItems.SWIM_UPGRADE.get()),
                new ItemStack(ModItems.CAMOUFLAGE_UPGRADE.get()));

        List<JeiUpgradeRecipe> out = new ArrayList<>();
        for (ItemStack upgrade : upgrades) {
            ItemStack chestplate = new ItemStack(Items.IRON_CHESTPLATE);
            net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(upgrade.getItem());
            ItemStack result = ChestplateUpgradeData.withAppendedUpgrade(
                    chestplate,
                    id == null ? ChestplateUpgradeData.upgradePlateId() : id);
            out.add(new JeiUpgradeRecipe(upgrade, chestplate, new ItemStack(ModItems.NEBRITH_SHARD.get()), result));
        }
        return out;
    }

    public record JeiInfusionRecipe(InfusionRecipe backing) {
        public int soulCost() {
            return backing.soulCost;
        }

        public ItemStack result() {
            return backing.result.copy();
        }

        public List<ItemStack> ingredientStacks() {
            List<ItemStack> list = new ArrayList<>();
            for (Map.Entry<Item, Integer> e : backing.required.entrySet()) {
                list.add(new ItemStack(e.getKey(), e.getValue()));
            }
            return list;
        }
    }

    public record JeiFoundryRecipe(ItemStack input, ItemStack displayOutput, int minYield, int maxYield, int cookTime) {
        public List<ItemStack> fuelStacks() {
            return List.of(
                    new ItemStack(ModItems.NEBRITH_SHARD.get()),
                    new ItemStack(ModItems.TOPAZ_SHARD.get()),
                    new ItemStack(ModItems.BERYL_SHARD.get()),
                    new ItemStack(ModItems.OPAL_SHARD.get()));
        }
    }

    public record JeiUpgradeRecipe(ItemStack upgradeItem, ItemStack chestplate, ItemStack shard, ItemStack output) {
    }
}
