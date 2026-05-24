package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.UndeadIllager;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IllagerRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class UndeadIllagerRenderer extends IllagerRenderer<UndeadIllager> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "textures/entity/mob/undead_illager.png");

    public UndeadIllagerRenderer(EntityRendererProvider.Context context) {
        super(context, new IllagerModel<>(context.bakeLayer(ModelLayers.EVOKER)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(UndeadIllager entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(UndeadIllager entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(1.05F, 1.05F, 1.05F);
    }
}
