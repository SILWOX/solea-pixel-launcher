package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.Blink;

public class BlinkRenderer extends MobRenderer<Blink, BlinkModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "textures/entity/mob/blink.png");

    /** Abaisse le modèle (échelle 1/16) pour coller au corps de collision élargi (tête + anneau de bâtons). */
    private static final float MODEL_VERTICAL_OFFSET = -0.34F;

    public BlinkRenderer(EntityRendererProvider.Context context) {
        super(context, new BlinkModel(context.bakeLayer(BlinkModel.LAYER_LOCATION)), 0.45F);
    }

    @Override
    protected void setupRotations(Blink entity, PoseStack poseStack, float bob, float yRot, float partialTick, float scale) {
        super.setupRotations(entity, poseStack, bob, yRot, partialTick, scale);
        poseStack.translate(0.0F, MODEL_VERTICAL_OFFSET, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(Blink entity) {
        return TEXTURE;
    }
}
