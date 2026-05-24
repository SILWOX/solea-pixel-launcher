package com.soleapixel.bmcmod.morph;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.MorphCrystalItem;
import com.soleapixel.bmcmod.registry.ModItems;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class MorphCrystalEvents {
    private MorphCrystalEvents() {
    }

    @SubscribeEvent
    public static void onMorphPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && !sp.level().isClientSide()) {
            MorphCrystalServer.stripMorphOnDeath(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || sp.level().isClientSide()) {
            return;
        }
        MorphCrystalServer.tickPlayer(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            MorphCrystalServer.onPlayerLogout(sp);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            MorphCrystalServer.onPlayerLogin(sp);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        MorphCrystalServer.onPlayerClone(event);
    }

    @SubscribeEvent
    public static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (left.getItem() instanceof MorphCrystalItem
                || right.getItem() instanceof MorphCrystalItem
                || left.is(ModItems.CRACKED_CRYSTAL.get())
                || right.is(ModItems.CRACKED_CRYSTAL.get())) {
            event.setCanceled(true);
        }
    }
}
