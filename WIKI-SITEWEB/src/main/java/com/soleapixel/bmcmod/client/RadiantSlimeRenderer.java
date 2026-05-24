package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.RadiantSlime;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RadiantSlimeRenderer extends MobRenderer<RadiantSlime, SlimeModel<RadiantSlime>> {
    private static final ResourceLocation INNER = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "textures/entity/slime/radiant_slime.png");

    public RadiantSlimeRenderer(EntityRendererProvider.Context context) {
        super(context, new SlimeModel<>(context.bakeLayer(ModelLayers.SLIME)), 0.25F);
        this.addLayer(new RadiantSlimeOuterLayer(this, context.getModelSet()));
    }

    /** Lumière renforcée (slime « radieux ») sans fullbright total pour garder un peu de lecture du monde. */
    private static int radiantPackedLight(int worldPacked) {
        int block = Math.min(15, LightTexture.block(worldPacked) + 5);
        int sky = Math.min(15, LightTexture.sky(worldPacked) + 3);
        return LightTexture.pack(block, sky);
    }

    @Override
    public void render(RadiantSlime entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.shadowRadius = 0.25F * (float) entity.getSize();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, radiantPackedLight(packedLight));
    }

    @Override
    protected void scale(RadiantSlime livingEntity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(0.999F, 0.999F, 0.999F);
        poseStack.translate(0.0F, 0.001F, 0.0F);
        float f1 = (float) livingEntity.getSize();
        float f2 = Mth.lerp(partialTickTime, livingEntity.oSquish, livingEntity.squish) / (f1 * 0.5F + 1.0F);
        float f3 = 1.0F / (f2 + 1.0F);
        poseStack.scale(f3 * f1, 1.0F / f3 * f1, f3 * f1);
    }

    @Override
    public ResourceLocation getTextureLocation(RadiantSlime entity) {
        return INNER;
    }
}
