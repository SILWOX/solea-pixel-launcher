package com.soleapixel.bmcmod.gameplay;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.CloneEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * Aggressif : les clones suivent la cible frappée par le propriétaire.
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class CloneGameplayEvents {

    private CloneGameplayEvents() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        if (!(attacker.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity victim) || !victim.isAlive()) {
            return;
        }
        if (victim instanceof CloneEntity hitClone && attacker.getUUID().equals(hitClone.getOwnerUuid())) {
            return;
        }
        for (CloneEntity clone : UndeadTotemGameplay.findOwnedClones(level, attacker)) {
            if (!clone.isAlive()) {
                continue;
            }
            if (CloneEntity.sameOwner(clone, victim)) {
                continue;
            }
            if (!clone.canAttack(victim)) {
                continue;
            }
            clone.setTarget(victim);
        }
    }
}
