package com.soleapixel.bmcmod.gameplay;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.upgrade.ChestplateUpgradeData;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Camouflage : accroupi, les créatures hostiles ne peuvent pas te prendre pour nouvelle cible (boss exclus).
 * Révélé quelques secondes si tu frappes une entité ou si tu subis des dégâts debout (pas accroupi).
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class CamouflageUpgradeGameplay {
    private static final String REVEAL_UNTIL_KEY = "bmcmod_camouflage_reveal_until";
    private static final int REVEAL_DURATION_TICKS = 100;
    private static final int CLEAR_AGGRO_INTERVAL = 10;
    private static final double AGGRO_CLEAR_RANGE = 40.0;

    private CamouflageUpgradeGameplay() {
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof Mob mob)) {
            return;
        }
        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (newTarget == null || !(newTarget instanceof ServerPlayer player)) {
            return;
        }
        if (!isStealthHidden(player)) {
            return;
        }
        if (isBossMob(mob)) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        if (!hasCamouflage(player)) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity victim) || victim == player) {
            return;
        }
        markRevealed(player);
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        if (!hasCamouflage(player)) {
            return;
        }
        if (player.isShiftKeyDown()) {
            return;
        }
        if (event.getNewDamage() <= 0.0F) {
            return;
        }
        markRevealed(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        if (player.tickCount % CLEAR_AGGRO_INTERVAL != 0) {
            return;
        }
        if (!isStealthHidden(player)) {
            return;
        }
        AABB box = player.getBoundingBox().inflate(AGGRO_CLEAR_RANGE);
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, box)) {
            if (mob.getTarget() == player && !isBossMob(mob)) {
                mob.setTarget(null);
            }
        }
    }

    private static boolean hasCamouflage(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return ChestplateUpgradeData.count(chest, ChestplateUpgradeData.CAMOUFLAGE_UPGRADE_ID) > 0;
    }

    private static long getRevealUntil(ServerPlayer player) {
        return player.getPersistentData().getLong(REVEAL_UNTIL_KEY);
    }

    private static void markRevealed(ServerPlayer player) {
        long now = player.level().getGameTime();
        long until = now + REVEAL_DURATION_TICKS;
        long current = getRevealUntil(player);
        if (until > current) {
            player.getPersistentData().putLong(REVEAL_UNTIL_KEY, until);
        }
    }

    private static boolean isStealthHidden(ServerPlayer player) {
        if (!hasCamouflage(player) || !player.isShiftKeyDown()) {
            return false;
        }
        long now = player.level().getGameTime();
        return now >= getRevealUntil(player);
    }

    private static boolean isBossMob(Mob mob) {
        return mob instanceof EnderDragon || mob instanceof WitherBoss || mob instanceof Warden;
    }
}
