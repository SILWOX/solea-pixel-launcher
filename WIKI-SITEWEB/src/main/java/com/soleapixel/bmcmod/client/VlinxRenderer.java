package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soleapixel.bmcmod.BmcMod;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VexRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Vex;

public class VlinxRenderer extends VexRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "textures/entity/mob/vlinx.png");

    public VlinxRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Vex entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(Vex entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(1.23F, 1.23F, 1.23F);
        super.scale(entity, poseStack, partialTickTime);
    }
}
