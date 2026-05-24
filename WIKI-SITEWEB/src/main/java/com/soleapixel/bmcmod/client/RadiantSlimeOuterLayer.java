package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.RadiantSlime;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RadiantSlimeOuterLayer extends RenderLayer<RadiantSlime, SlimeModel<RadiantSlime>> {
    private static final ResourceLocation OUTER = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "textures/entity/slime/radiant_slime_outer.png");
    private final EntityModel<RadiantSlime> model;

    public RadiantSlimeOuterLayer(RenderLayerParent<RadiantSlime, SlimeModel<RadiantSlime>> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new SlimeModel<>(modelSet.bakeLayer(ModelLayers.SLIME_OUTER));
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            RadiantSlime livingEntity,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean outlineWhenInvisible = minecraft.shouldEntityAppearGlowing(livingEntity) && livingEntity.isInvisible();
        if (!livingEntity.isInvisible() || outlineWhenInvisible) {
            VertexConsumer vertexconsumer;
            if (outlineWhenInvisible) {
                vertexconsumer = buffer.getBuffer(RenderType.outline(OUTER));
            } else {
                vertexconsumer = buffer.getBuffer(RenderType.entityTranslucent(OUTER));
            }

            this.getParentModel().copyPropertiesTo(this.model);
            this.model.prepareMobModel(livingEntity, limbSwing, limbSwingAmount, partialTicks);
            this.model.setupAnim(livingEntity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            this.model.renderToBuffer(
                    poseStack,
                    vertexconsumer,
                    packedLight,
                    LivingEntityRenderer.getOverlayCoords(livingEntity, 0.0F));
        }
    }
}
