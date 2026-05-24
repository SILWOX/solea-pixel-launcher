package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import com.soleapixel.bmcmod.block.infusion.InfusionTableBlockEntity;

@OnlyIn(Dist.CLIENT)
public class InfusionTableRenderer implements BlockEntityRenderer<InfusionTableBlockEntity> {
    /** Même livre que la table d’enchantement vanilla (évite une texture custom manquante sur l’atlas). */
    private static final Material BOOK_LOCATION = EnchantTableRenderer.BOOK_LOCATION;
    private final BookModel bookModel;

    public InfusionTableRenderer(BlockEntityRendererProvider.Context context) {
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void render(
            InfusionTableBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.75F, 0.5F);
        float f = (float) blockEntity.time + partialTick;
        poseStack.translate(0.0F, 0.1F + Mth.sin(f * 0.1F) * 0.01F, 0.0F);
        float f1 = blockEntity.rot - blockEntity.oRot;
        while (f1 >= (float) Math.PI) {
            f1 -= (float) (Math.PI * 2);
        }
        while (f1 < (float) -Math.PI) {
            f1 += (float) (Math.PI * 2);
        }
        float f2 = blockEntity.oRot + f1 * partialTick;
        poseStack.mulPose(Axis.YP.rotation(-f2));
        poseStack.mulPose(Axis.ZP.rotationDegrees(80.0F));
        float f3 = Mth.lerp(partialTick, blockEntity.oFlip, blockEntity.flip);
        float f4 = Mth.frac(f3 + 0.25F) * 1.6F - 0.3F;
        float f5 = Mth.frac(f3 + 0.75F) * 1.6F - 0.3F;
        float f6 = Mth.lerp(partialTick, blockEntity.oOpen, blockEntity.open);
        this.bookModel.setupAnim(f, Mth.clamp(f4, 0.0F, 1.0F), Mth.clamp(f5, 0.0F, 1.0F), f6);
        /* entityCutout : le modèle du livre a de la transparence ; entitySolid donne un silhouette noire. */
        VertexConsumer vertexConsumer = BOOK_LOCATION.buffer(bufferSource, RenderType::entityCutout);
        this.bookModel.render(poseStack, vertexConsumer, packedLight, packedOverlay, -1);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(InfusionTableBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.5, pos.getZ() + 1.0);
    }
}
