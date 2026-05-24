package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.SkeletonVillager;

public class SkeletonVillagerRenderer extends HumanoidMobRenderer<SkeletonVillager, HumanoidModel<SkeletonVillager>> {
    private static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "textures/entity/mob/skeleton_villager.png");
    private static final ResourceLocation SOUL = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "textures/entity/mob/soul_skeleton_villager.png");
    private final SkeletonVillagerModel baseModel;
    private final PlayerModel<SkeletonVillager> mimicModel;

    public SkeletonVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new SkeletonVillagerModel(context.bakeLayer(SkeletonVillagerModel.LAYER_LOCATION)), 0.5F);
        this.baseModel = (SkeletonVillagerModel) this.model;
        this.mimicModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON_INNER_ARMOR)),
                new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(SkeletonVillager entity, float entityYaw, float partialTicks, PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight) {
        HumanoidModel<SkeletonVillager> previous = this.model;
        this.model = entity.hasMimicked() ? this.mimicModel : this.baseModel;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        this.model = previous;
    }

    @Override
    protected void scale(SkeletonVillager entity, PoseStack poseStack, float partialTickTime) {
        if (entity.isSoulVariant()) {
            poseStack.scale(1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(SkeletonVillager entity) {
        if (entity.hasMimicked()) {
            ResourceLocation skin = resolveMimicSkin(entity);
            if (skin != null) {
                return skin;
            }
        }
        return entity.isSoulVariant() ? SOUL : BASE;
    }

    @Override
    protected RenderType getRenderType(SkeletonVillager entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        if (entity.isSoulVariant()) {
            return RenderType.entityTranslucent(this.getTextureLocation(entity));
        }
        return super.getRenderType(entity, bodyVisible, translucent, glowing);
    }

    private static ResourceLocation resolveMimicSkin(SkeletonVillager entity) {
        var uuid = entity.getMimicPlayerUuid();
        if (uuid == null) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            var player = mc.level.getPlayerByUUID(uuid);
            if (player instanceof net.minecraft.client.player.AbstractClientPlayer acp) {
                return acp.getSkin().texture();
            }
        }
        if (mc.getConnection() != null) {
            var info = mc.getConnection().getPlayerInfo(uuid);
            if (info != null && info.getSkin() != null) {
                return info.getSkin().texture();
            }
        }
        return net.minecraft.client.resources.DefaultPlayerSkin.get(uuid).texture();
    }
}
