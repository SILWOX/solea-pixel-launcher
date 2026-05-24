package com.soleapixel.bmcmod.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.Endling;

public class EndlingRenderer extends MobRenderer<Endling, EndlingModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "textures/entity/mob/endlings.png");

    public EndlingRenderer(EntityRendererProvider.Context context) {
        super(context, new EndlingModel(context.bakeLayer(EndlingModel.LAYER_LOCATION)), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(Endling entity) {
        return TEXTURE;
    }
}
