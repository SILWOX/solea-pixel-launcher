package com.soleapixel.bmcmod.equipment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import com.soleapixel.bmcmod.BmcMod;

/**
 * Au-dessus du Netherite : durabilité, vitesse, dégâts et enchantabilité légèrement supérieurs.
 */
public enum EnderiteToolTier implements Tier {
    INSTANCE;

    public static final TagKey<Item> ENDERITE_INGOTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "enderite_ingots"));

    @Override
    public int getUses() {
        return 2_450;
    }

    @Override
    public float getSpeed() {
        return 10.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 5.0F;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return 18;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ENDERITE_INGOTS);
    }
}
