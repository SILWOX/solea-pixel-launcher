package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.ScytheItem;

@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class ScytheSweepRenderHandler {
    private static final ThreadLocal<Boolean> SPIN_ROLLBACK = new ThreadLocal<>();

    private ScytheSweepRenderHandler() {
    }

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        SPIN_ROLLBACK.remove();
        LivingEntity ent = event.getEntity();
        if (!(ent instanceof Player)) {
            return;
        }
        int left = ScytheSpinVisualTracker.getRemainingTicks(ent.getId());
        if (left <= 0) {
            return;
        }
        SPIN_ROLLBACK.set(Boolean.TRUE);
        float total = Math.max(1, ScytheSpinVisualTracker.getTotalTicks(ent.getId()));
        int rotations = ScytheSpinVisualTracker.getFullRotations(ent.getId());
        float phase = 1.0F - left / total;
        float spin = phase * 360.0F * rotations;
        float halfH = ent.getBbHeight() * 0.5F;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.0, halfH, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.translate(0.0, -halfH, 0.0);
        float bob = Mth.sin(phase * Mth.PI * 2.0F) * 0.05F;
        poseStack.translate(bob, 0.0, -bob * 0.5F);
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (!Boolean.TRUE.equals(SPIN_ROLLBACK.get())) {
            return;
        }
        SPIN_ROLLBACK.remove();
        event.getPoseStack().popPose();
    }
}
