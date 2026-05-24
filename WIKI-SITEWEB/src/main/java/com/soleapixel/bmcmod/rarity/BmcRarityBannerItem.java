package com.soleapixel.bmcmod.rarity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Bannière dont le nom d’affichage suit une {@link BmcModRarity} (ex. Mythic). */
public final class BmcRarityBannerItem extends BannerItem {
    private final BmcModRarity rarity;

    public BmcRarityBannerItem(Block bannerBlock, Block wallBannerBlock, Properties properties, BmcModRarity rarity) {
        super(bannerBlock, wallBannerBlock, properties);
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
