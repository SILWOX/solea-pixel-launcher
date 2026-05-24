package com.soleapixel.bmcmod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import com.soleapixel.bmcmod.BmcMod;

@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class MorphClientLifecycle {
    private MorphClientLifecycle() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        MorphVisualClient.clearAllProxies();
    }
}
