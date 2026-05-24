package com.soleapixel.bmcmod.client;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.block.chest.EnchantedChestBlockEntity;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.state.properties.ChestType;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Même forme qu'un coffre vanilla ; chemins de texture {@code bmcmod:entity/chest/enchanted_chest*}.
 */
public class EnchantedChestRenderer extends ChestRenderer<EnchantedChestBlockEntity> {
    public EnchantedChestRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    protected Material getMaterial(EnchantedChestBlockEntity be, ChestType type) {
        return switch (type) {
            case LEFT -> new Material(Sheets.CHEST_SHEET, BmcMod.loc("entity/chest/enchanted_chest_left"));
            case RIGHT -> new Material(Sheets.CHEST_SHEET, BmcMod.loc("entity/chest/enchanted_chest_right"));
            case SINGLE -> new Material(Sheets.CHEST_SHEET, BmcMod.loc("entity/chest/enchanted_chest"));
        };
    }
}
