package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soleapixel.bmcmod.block.chest.FakeChestBlockEntity;
import com.soleapixel.bmcmod.registry.ModBlocks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Rendu 3D de l’item faux coffre (inventaire / main / créatif) : coffre chêne vanilla via le {@link FakeChestRenderer}.
 */
public final class FakeChestBewlr extends BlockEntityWithoutLevelRenderer {
    private final FakeChestBlockEntity dummy;

    public FakeChestBewlr() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.dummy = new FakeChestBlockEntity(
                BlockPos.ZERO,
                ModBlocks.FAKE_CHEST.get()
                        .defaultBlockState()
                        .setValue(ChestBlock.FACING, net.minecraft.core.Direction.SOUTH)
                        .setValue(ChestBlock.TYPE, ChestType.SINGLE)
                        .setValue(ChestBlock.WATERLOGGED, false));
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        var mc = Minecraft.getInstance();
        if (mc.level != null) {
            this.dummy.setLevel(mc.level);
        }
        @SuppressWarnings("unchecked")
        BlockEntityRenderer<FakeChestBlockEntity> renderer =
                (BlockEntityRenderer<FakeChestBlockEntity>) mc.getBlockEntityRenderDispatcher().getRenderer(this.dummy);
        if (renderer != null) {
            renderer.render(this.dummy, 0.0F, poseStack, buffer, packedLight, packedOverlay);
        }
    }
}
