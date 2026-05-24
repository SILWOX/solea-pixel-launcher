package com.soleapixel.bmcmod.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.projectile.VoidShardProjectile;

public class VoidShardProjectileRenderer extends EntityRenderer<VoidShardProjectile> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "textures/entity/mob/void_shard_projectile.png");
    private final VoidShardProjectileModel model;

    public VoidShardProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new VoidShardProjectileModel(context.bakeLayer(VoidShardProjectileModel.LAYER_LOCATION));
    }

    @Override
    public void render(VoidShardProjectile entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        float yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.scale(0.35F, 0.35F, 0.35F);
        poseStack.translate(0.0F, 0.15F, 0.0F);
        var vertexConsumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(VoidShardProjectile entity) {
        return TEXTURE;
    }
}
