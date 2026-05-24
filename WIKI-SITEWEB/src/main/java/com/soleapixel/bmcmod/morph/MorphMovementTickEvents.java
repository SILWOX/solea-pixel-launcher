package com.soleapixel.bmcmod.morph;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.soleapixel.bmcmod.BmcMod;

/**
 * Avant le tick joueur : verrou au sol pour slime (le saut est renforcé dans {@link MorphSlimeJumpEvents}).
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class MorphMovementTickEvents {
    private MorphMovementTickEvents() {
    }

    @SubscribeEvent
    public static void onPlayerPreTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || sp.level().isClientSide()) {
            return;
        }
        MorphMovementRules.tick(sp);
    }
}
