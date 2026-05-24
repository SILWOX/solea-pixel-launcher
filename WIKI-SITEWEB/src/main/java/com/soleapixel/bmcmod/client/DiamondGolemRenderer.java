package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.DiamondGolem;

public class DiamondGolemRenderer extends MobRenderer<DiamondGolem, IronGolemModel<DiamondGolem>> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "textures/entity/mob/diamond_golem.png");

    public DiamondGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new IronGolemModel<>(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(DiamondGolem entity) {
        return TEXTURE;
    }

    @Override
    protected void setupRotations(DiamondGolem entity, PoseStack poseStack, float bob, float yRot, float partialTicks, float scale) {
        super.setupRotations(entity, poseStack, bob, yRot, partialTicks, scale);
        if (entity.walkAnimation.speed() >= 0.01F) {
            float g = entity.walkAnimation.position(partialTicks) + 6.0F;
            float h = (Math.abs(g % 13.0F - 6.5F) - 3.25F) / 3.25F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.5F * h));
        }
    }
}
