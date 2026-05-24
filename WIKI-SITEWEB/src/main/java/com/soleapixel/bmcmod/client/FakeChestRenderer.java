package com.soleapixel.bmcmod.client;

import com.soleapixel.bmcmod.block.chest.FakeChestBlockEntity;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Coffre factice : même atlas que le coffre vanilla simple (chêne). */
public class FakeChestRenderer extends ChestRenderer<FakeChestBlockEntity> {

    public FakeChestRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    protected Material getMaterial(FakeChestBlockEntity blockEntity, ChestType type) {
        return switch (type) {
            case LEFT -> Sheets.CHEST_LOCATION_LEFT;
            case RIGHT -> Sheets.CHEST_LOCATION_RIGHT;
            case SINGLE -> Sheets.CHEST_LOCATION;
        };
    }
}
