package com.soleapixel.bmcmod.rarity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class RarityStickItem extends Item {
    private final BmcModRarity rarity;

    public RarityStickItem(Properties properties, BmcModRarity rarity) {
        super(properties);
        this.rarity = rarity;
    }

    public BmcModRarity getBmcModRarity() {
        return rarity;
    }

    @Override
    public Component getName(ItemStack stack) {
        return rarity.styleItemName(Component.translatable(getDescriptionId(stack)));
    }
}
