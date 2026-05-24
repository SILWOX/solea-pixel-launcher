package com.soleapixel.bmcmod.gameplay;

import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.soleapixel.bmcmod.BmcMod;

/** IA légère des alliés invoqués par le Wither Staff. */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class WitherStaffAllyEvents {
    public static final String TAG_OWNER = "bmcmod:wither_ally_owner";
    public static final String TAG_EXPIRES_AT = "bmcmod:wither_ally_expires_at";

    private WitherStaffAllyEvents() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof WitherSkeleton ally) || !(ally.level() instanceof ServerLevel level)) {
            return;
        }
        var tag = ally.getPersistentData();
        if (!tag.hasUUID(TAG_OWNER)) {
            return;
        }
        long expires = tag.getLong(TAG_EXPIRES_AT);
        if (expires > 0 && level.getGameTime() >= expires) {
            ally.discard();
            return;
        }
        UUID ownerId = tag.getUUID(TAG_OWNER);
        if (!(level.getEntity(ownerId) instanceof ServerPlayer owner) || !owner.isAlive()) {
            ally.discard();
            return;
        }
        // Hard safety: summoned allies must never target their owner.
        if (ally.getTarget() == owner) {
            ally.setTarget(null);
        }
        if (ally.tickCount % 8 != 0) {
            return;
        }
        if (ally.distanceToSqr(owner) > 32.0 * 32.0) {
            ally.teleportTo(owner.getX(), owner.getY(), owner.getZ());
        }
        LivingEntity target = chooseTarget(owner, ally);
        if (target != null && target != owner) {
            ally.setTarget(target);
        }
    }

    private static LivingEntity chooseTarget(ServerPlayer owner, WitherSkeleton ally) {
        LivingEntity revenge = owner.getLastHurtByMob();
        if (isValidHostile(owner, ally, revenge)) {
            return revenge;
        }
        LivingEntity attack = owner.getLastHurtMob();
        if (isValidHostile(owner, ally, attack)) {
            return attack;
        }
        AABB area = owner.getBoundingBox().inflate(16.0, 8.0, 16.0);
        Monster nearest = null;
        double best = Double.MAX_VALUE;
        for (Monster m : owner.level().getEntitiesOfClass(Monster.class, area)) {
            if (!isValidHostile(owner, ally, m)) {
                continue;
            }
            double d = m.distanceToSqr(owner);
            if (d < best) {
                best = d;
                nearest = m;
            }
        }
        return nearest;
    }

    private static boolean isValidHostile(ServerPlayer owner, WitherSkeleton ally, LivingEntity target) {
        if (target == null || !target.isAlive() || target == owner || target == ally) {
            return false;
        }
        if (!(target instanceof Monster)) {
            return false;
        }
        if (target instanceof Mob mob && mob.getPersistentData().hasUUID(TAG_OWNER)) {
            return false;
        }
        return true;
    }
}
