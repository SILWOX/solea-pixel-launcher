package com.soleapixel.bmcmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Witch;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import com.soleapixel.bmcmod.BmcMod;

/**
 * Remplace le rendu du joueur par celui d’une sorcière lorsque la métamorphose est active (vue tierce / autres joueurs).
 */
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class WitchMetamorphRenderHandler {
    private WitchMetamorphRenderHandler() {
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (MorphVisualClient.isMorphed(player.getUUID())) {
            return;
        }
        if (!WitchMetamorphClient.isDisguised(player.getUUID())) {
            return;
        }
        if (!(player.level() instanceof net.minecraft.client.multiplayer.ClientLevel level)) {
            return;
        }
        event.setCanceled(true);
        Witch witch = WitchMetamorphClient.getOrCreateProxyWitch(level, player);
        if (witch == null) {
            return;
        }
        float partialTick = event.getPartialTick();
        float yRot = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        @SuppressWarnings("unchecked")
        LivingEntityRenderer<Witch, ?> renderer = (LivingEntityRenderer<Witch, ?>) Minecraft.getInstance()
                .getEntityRenderDispatcher()
                .getRenderer(witch);
        renderer.render(witch, yRot, partialTick, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
    }
}
