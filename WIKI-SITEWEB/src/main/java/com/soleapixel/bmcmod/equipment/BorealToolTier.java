package com.soleapixel.bmcmod.equipment;

import com.soleapixel.bmcmod.BmcMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public enum BorealToolTier implements Tier {
    INSTANCE;

    public static final TagKey<Item> BOREAL_INGOTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "boreal_ingots"));

    @Override
    public int getUses() {
        return 2_950;
    }

    @Override
    public float getSpeed() {
        return 11.5F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 5.8F;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return 22;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(BOREAL_INGOTS);
    }
}

