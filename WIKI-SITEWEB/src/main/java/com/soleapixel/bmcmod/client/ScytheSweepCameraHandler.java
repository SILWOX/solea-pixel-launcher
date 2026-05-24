package com.soleapixel.bmcmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.ScytheItem;

/** Léger balancement caméra 1ʳᵉ personne pendant le tourbillon (complète le spin 3ᵉ personne). */
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class ScytheSweepCameraHandler {
    private ScytheSweepCameraHandler() {
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        int fp = ScytheSweepClientHandler.getFirstPersonSweepAnimTicks();
        int spin = ScytheSpinVisualTracker.getRemainingTicks(mc.player.getId());
        int left = Math.max(fp, spin);
        if (left <= 0) {
            return;
        }
        int phaseTotal = ScytheSweepClientHandler.getSweepCameraPhaseTotalTicks();
        if (phaseTotal <= 0) {
            phaseTotal = Math.max(
                    ScytheItem.SWEEP_VISUAL_FP_TICKS,
                    ScytheSpinVisualTracker.getTotalTicks(mc.player.getId()));
        }
        float total = Math.max(1, phaseTotal);
        float phase = 1.0F - left / total;
        double pt = event.getPartialTick();
        float wobble = Mth.sin((float) (phase * Math.PI * 2.0 + pt * 0.15)) * 4.0F;
        event.setYaw(event.getYaw() + wobble);
        event.setRoll(event.getRoll() + Mth.sin((float) (phase * Math.PI)) * 2.5F);
    }
}
