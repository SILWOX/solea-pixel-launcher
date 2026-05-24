package com.soleapixel.bmcmod.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** Branche le {@link FakeChestBewlr} sur l’item faux coffre (icône inventaire). */
public final class FakeChestItemExtensions implements IClientItemExtensions {
    public static final FakeChestItemExtensions INSTANCE = new FakeChestItemExtensions();
    private static FakeChestBewlr bewlr;

    private FakeChestItemExtensions() {
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (bewlr == null) {
            bewlr = new FakeChestBewlr();
        }
        return bewlr;
    }
}
