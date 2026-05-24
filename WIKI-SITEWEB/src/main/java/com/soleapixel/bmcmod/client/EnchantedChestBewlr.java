package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soleapixel.bmcmod.block.chest.EnchantedChestBlockEntity;
import com.soleapixel.bmcmod.registry.ModBlocks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.ChestBlock;

/**
 * Rendu 3D de l’item (inventaire, main, onglet créatif) : même forme qu’un coffre avec la texture enchantée.
 */
public final class EnchantedChestBewlr extends BlockEntityWithoutLevelRenderer {
    private final EnchantedChestBlockEntity dummy;

    public EnchantedChestBewlr() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.dummy = new EnchantedChestBlockEntity(
                BlockPos.ZERO,
                ModBlocks.ENCHANTED_CHEST.get()
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
        BlockEntityRenderer<EnchantedChestBlockEntity> renderer = (BlockEntityRenderer<EnchantedChestBlockEntity>)
                mc.getBlockEntityRenderDispatcher().getRenderer(this.dummy);
        if (renderer != null) {
            renderer.render(this.dummy, 0.0F, poseStack, buffer, packedLight, packedOverlay);
        }
    }
}
