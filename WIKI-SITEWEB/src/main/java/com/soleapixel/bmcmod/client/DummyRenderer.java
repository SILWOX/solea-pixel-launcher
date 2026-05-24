package com.soleapixel.bmcmod.client;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.Dummy;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class DummyRenderer extends LivingEntityRenderer<Dummy, TargetDummyModel> {
    private static final ResourceLocation TARGET_DUMMY_TEXTURE = BmcMod.loc("textures/entity/mob/target_dummy_model.png");

    public DummyRenderer(EntityRendererProvider.Context context) {
        super(context, new TargetDummyModel(context.bakeLayer(TargetDummyModel.LAYER_LOCATION)), 0.4F);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(Dummy entity) {
        return TARGET_DUMMY_TEXTURE;
    }

    @Override
    protected boolean shouldShowName(Dummy entity) {
        return false;
    }
}
