package com.soleapixel.bmcmod.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.morph.MorphHitboxEvents;
import com.soleapixel.bmcmod.morph.MorphPlayerState;

/**
 * Client : mêmes dimensions que le mob pour les joueurs déguisés (F3+B, collisions prédites côté client).
 */
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class MorphHitboxClientEvents {
    private MorphHitboxClientEvents() {
    }

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!player.level().isClientSide()) {
            return;
        }
        CompoundTag soul = resolveMorphSoulClient(player);
        if (!soul.contains("id", Tag.TAG_STRING)) {
            return;
        }
        MorphHitboxEvents.computeMorphDimensions(player.level(), soul, event.getPose()).ifPresent(event::setNewSize);
    }

    private static CompoundTag resolveMorphSoulClient(Player player) {
        if (MorphVisualClient.isMorphed(player.getUUID())) {
            return MorphVisualClient.getSoul(player.getUUID());
        }
        if (MorphPlayerState.isMorphed(player)) {
            return MorphPlayerState.getMorphSoul(player);
        }
        return new CompoundTag();
    }
}
