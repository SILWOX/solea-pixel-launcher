package com.soleapixel.bmcmod.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Branche le {@link EnchantedChestBewlr} sur l’item coffre (inventaire / main / créatif).
 */
public final class EnchantedChestItemExtensions implements IClientItemExtensions {
    public static final EnchantedChestItemExtensions INSTANCE = new EnchantedChestItemExtensions();
    private static EnchantedChestBewlr bewlr;

    private EnchantedChestItemExtensions() {
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (bewlr == null) {
            bewlr = new EnchantedChestBewlr();
        }
        return bewlr;
    }
}
