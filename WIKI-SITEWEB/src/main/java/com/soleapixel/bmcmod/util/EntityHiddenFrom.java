package com.soleapixel.bmcmod.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * "Caché" = invisibilité (effet) ou la ligne de vue n’est pas claire.
 */
public final class EntityHiddenFrom {
    private EntityHiddenFrom() {
    }

    public static boolean isHiddenTo(ServerPlayer viewer, LivingEntity target) {
        if (target == viewer) {
            return false;
        }
        if (target.isInvisible() || target.hasEffect(MobEffects.INVISIBILITY)) {
            return true;
        }
        return !viewer.hasLineOfSight(target);
    }
}
