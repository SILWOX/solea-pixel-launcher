package com.soleapixel.bmcmod.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class BmcModShieldItemExtensions implements IClientItemExtensions {
    public static final BmcModShieldItemExtensions INSTANCE = new BmcModShieldItemExtensions();
    private static BmcModShieldBewlr bewlr;

    private BmcModShieldItemExtensions() {
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (bewlr == null) {
            bewlr = new BmcModShieldBewlr();
        }
        return bewlr;
    }
}
