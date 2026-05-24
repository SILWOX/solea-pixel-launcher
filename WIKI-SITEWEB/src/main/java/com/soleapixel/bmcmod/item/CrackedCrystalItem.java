package com.soleapixel.bmcmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.soleapixel.bmcmod.rarity.BmcModRarity;

/** Cristal épuisé (morph ou capture) — sans fonction. */
public final class CrackedCrystalItem extends Item {
    public CrackedCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return BmcModRarity.COMMON.styleItemName(Component.translatable(getDescriptionId(stack)));
    }
}
