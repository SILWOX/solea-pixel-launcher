package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.UseAnim;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;

import com.soleapixel.bmcmod.BmcMod;

/**
 * Remplace le bras joueur en 1ʳᵉ personne par celui du proxy si le modèle est un {@link HumanoidModel}
 * (zombie, squelette, piglin…). Sinon le rendu vanilla reste.
 */
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class MorphFirstPersonArmHandler {
    private MorphFirstPersonArmHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player || mc.level == null) {
            return;
        }
        if (!MorphVisualClient.isMorphed(player.getUUID())) {
            return;
        }
        if (player.isUsingItem() && player.getUseItem().getUseAnimation() == UseAnim.TOOT_HORN) {
            return;
        }
        if (!(mc.level instanceof net.minecraft.client.multiplayer.ClientLevel level)) {
            return;
        }
        LivingEntity proxy =
                MorphVisualClient.getOrCreateMorphProxy(level, player, MorphVisualClient.getSoul(player.getUUID()));
        if (proxy == null) {
            return;
        }
        var dispatcher = mc.getEntityRenderDispatcher();
        @SuppressWarnings("unchecked")
        LivingEntityRenderer<LivingEntity, ?> renderer =
                (LivingEntityRenderer<LivingEntity, ?>) dispatcher.getRenderer(proxy);
        if (!(renderer.getModel() instanceof HumanoidModel<?> model)) {
            return;
        }
        event.setCanceled(true);
        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(true);
        HumanoidArm arm = event.getArm();
        ModelPart armPart = arm == HumanoidArm.LEFT ? model.leftArm : model.rightArm;
        float yRotBody = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float yRotHead = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        @SuppressWarnings("unchecked")
        HumanoidModel<LivingEntity> humanoid = (HumanoidModel<LivingEntity>) model;
        humanoid.setupAnim(
                proxy,
                0.0F,
                0.0F,
                player.tickCount + partialTick,
                yRotHead - yRotBody,
                proxy.getXRot());
        float attack = Mth.lerp(partialTick, player.oAttackAnim, player.attackAnim);
        HumanoidArm swingSide =
                player.swingingArm == InteractionHand.MAIN_HAND ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        if (attack > 0.0F && player.swinging && swingSide == arm) {
            float shake = Mth.sin(attack * (float) Math.PI);
            float twist = Mth.sin(attack * attack * (float) Math.PI) * -0.5F;
            if (arm == HumanoidArm.RIGHT) {
                model.rightArm.xRot = -0.1F - shake * 0.7F;
                model.rightArm.yRot = -twist;
            } else {
                model.leftArm.xRot = -0.1F - shake * 0.7F;
                model.leftArm.yRot = twist;
            }
        }
        ResourceLocation tex = renderer.getTextureLocation(proxy);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        boolean left = arm == HumanoidArm.LEFT;
        if (left) {
            pose.translate(0.35, -0.85 + 0.12, -0.85);
        } else {
            pose.translate(-0.35, -0.85 + 0.12, -0.85);
        }
        pose.mulPose(Axis.XP.rotationDegrees(60.0F));
        pose.mulPose(Axis.ZP.rotationDegrees((left ? -1.0F : 1.0F) * 45.0F));
        pose.scale(0.55F, 0.55F, 0.55F);
        MultiBufferSource buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        armPart.render(
                pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(tex)),
                light,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
