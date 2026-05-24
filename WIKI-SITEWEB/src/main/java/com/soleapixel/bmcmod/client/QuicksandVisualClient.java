package com.soleapixel.bmcmod.client;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.block.QuicksandBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Comme la neige poudreuse, le moteur ne dessine pas de « parois » solides quand la caméra est dans un bloc sans
 * collision pleine — d’où l’effet x-ray. Ici : voile plein écran couleur sable sous le HUD
 * ({@link RenderGuiEvent.Pre}), plutôt que de piloter le brouillard comme avant.
 */
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class QuicksandVisualClient {
    /** ARGB — beige sable opaque (~comme la lecture dans la poudreuse). */
    private static final int OVERLAY_ARGB = 0xE8D9C9A4;

    private QuicksandVisualClient() {
    }

    private static boolean eyesInQuicksand(Minecraft mc, float partialTick) {
        if (mc.player == null || mc.level == null) {
            return false;
        }
        Vec3 eye = mc.player.getEyePosition(partialTick);
        BlockPos pos = BlockPos.containing(eye);
        return QuicksandBlock.isQuicksand(mc.level.getBlockState(pos));
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.isPaused()) {
            return;
        }
        float pt = mc.getTimer().getGameTimeDeltaPartialTick(false);
        if (!eyesInQuicksand(mc, pt)) {
            return;
        }
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        event.getGuiGraphics().fill(0, 0, w, h, OVERLAY_ARGB);
    }
}
