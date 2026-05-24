package com.soleapixel.bmcmod.client;

import com.soleapixel.bmcmod.entity.projectile.ScytheTornadoProjectile;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Entité entièrement portée par les particules côté client ; pas de modèle.
 */
public class ScytheTornadoRenderer extends EntityRenderer<ScytheTornadoProjectile> {
    public ScytheTornadoRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            ScytheTornadoProjectile entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
    }

    @Override
    public ResourceLocation getTextureLocation(ScytheTornadoProjectile entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
