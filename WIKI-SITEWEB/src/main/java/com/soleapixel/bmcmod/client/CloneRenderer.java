package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soleapixel.bmcmod.entity.CloneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

public class CloneRenderer extends HumanoidMobRenderer<CloneEntity, HumanoidModel<CloneEntity>> {
    private final PlayerModel<CloneEntity> steveModel;
    private final PlayerModel<CloneEntity> alexModel;

    public CloneRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.steveModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        this.alexModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(CloneEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        HumanoidModel<CloneEntity> previous = this.model;
        this.model = entity.isSlimModel() ? this.alexModel : this.steveModel;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        this.model = previous;
        if (entity.level() != null && entity.tickCount % 12 == Math.floorMod(entity.getId(), 12)) {
            entity.level()
                    .addParticle(
                            ParticleTypes.SOUL_FIRE_FLAME,
                            entity.getRandomX(0.65D),
                            entity.getY() + entity.getBbHeight() * 0.92D,
                            entity.getRandomZ(0.65D),
                            0.02D,
                            0.06D,
                            0.02D);
        }
    }

    /** Same proportional scale vanilla applies to {@link net.minecraft.client.player.AbstractClientPlayer} models. */
    private static final float PLAYER_MODEL_BODY_SCALE = 0.9375F;

    @Override
    protected void scale(CloneEntity entity, PoseStack poseStack, float partialTick) {
        super.scale(entity, poseStack, partialTick);
        float s = PLAYER_MODEL_BODY_SCALE;
        poseStack.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(CloneEntity entity) {
        var ownerId = entity.getOwnerUuid();
        if (ownerId == null) {
            return DefaultPlayerSkin.getDefaultTexture();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            var player = mc.level.getPlayerByUUID(ownerId);
            if (player instanceof net.minecraft.client.player.AbstractClientPlayer acp) {
                PlayerSkin skin = acp.getSkin();
                return skin != null ? skin.texture() : DefaultPlayerSkin.get(ownerId).texture();
            }
        }
        if (mc.getConnection() != null) {
            var info = mc.getConnection().getPlayerInfo(ownerId);
            if (info != null && info.getSkin() != null) {
                return info.getSkin().texture();
            }
        }
        return DefaultPlayerSkin.get(ownerId).texture();
    }
}
