package com.soleapixel.bmcmod.gameplay;

import net.minecraft.world.entity.animal.Wolf;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.SuperBoneItem;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Maintient les modificateurs d’attribut du super os pendant la durée du buff et les retire à l’expiration.
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class SuperBoneWolfEvents {
    private SuperBoneWolfEvents() {
    }

    @SubscribeEvent
    public static void onWolfTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Wolf wolf)) {
            return;
        }
        long until = wolf.getPersistentData().getLong(SuperBoneItem.BUFF_UNTIL_GAME_TIME_TAG);
        if (until <= 0L) {
            return;
        }
        long now = wolf.level().getGameTime();
        if (now >= until) {
            SuperBoneItem.clearSuperBoneAttributeModifiers(wolf);
            wolf.getPersistentData().remove(SuperBoneItem.BUFF_UNTIL_GAME_TIME_TAG);
            return;
        }
        SuperBoneItem.refreshSuperBoneAttributeModifiers(wolf);
    }
}
