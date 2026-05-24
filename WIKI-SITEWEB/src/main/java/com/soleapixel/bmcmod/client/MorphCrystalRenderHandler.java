package com.soleapixel.bmcmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import com.soleapixel.bmcmod.BmcMod;

@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class MorphCrystalRenderHandler {
    private MorphCrystalRenderHandler() {
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (!MorphVisualClient.isMorphed(player.getUUID())) {
            return;
        }
        if (!(player.level() instanceof net.minecraft.client.multiplayer.ClientLevel level)) {
            return;
        }
        LivingEntity proxy = MorphVisualClient.getOrCreateMorphProxy(level, player, MorphVisualClient.getSoul(player.getUUID()));
        if (proxy == null) {
            return;
        }
        event.setCanceled(true);
        float partialTick = event.getPartialTick();
        float yRot = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        @SuppressWarnings("unchecked")
        LivingEntityRenderer<LivingEntity, ?> renderer =
                (LivingEntityRenderer<LivingEntity, ?>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(proxy);
        renderer.render(proxy, yRot, partialTick, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
    }
}
