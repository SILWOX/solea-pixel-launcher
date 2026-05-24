package com.soleapixel.bmcmod.morph;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;

public final class MorphBossBlacklist {
    private MorphBossBlacklist() {
    }

    public static boolean isBoss(LivingEntity victim) {
        return victim instanceof WitherBoss || victim instanceof EnderDragon || victim instanceof Warden;
    }
}
