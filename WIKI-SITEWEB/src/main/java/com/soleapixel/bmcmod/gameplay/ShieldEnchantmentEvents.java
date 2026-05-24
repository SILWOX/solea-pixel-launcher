package com.soleapixel.bmcmod.gameplay;

import com.soleapixel.bmcmod.registry.ModEnchantmentKeys;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/**
 * {@link ModEnchantmentKeys#SHIELD_CHARGE} : en bloquant avec succès, chance de repousser fortement l’attaquant
 * direct ; petite chance que le repoussement inflige des dégâts (échelle au niveau I–III).
 */
public final class ShieldEnchantmentEvents {
    /** 75 % : repoussement fort sur l’attaquant en mêlée. */
    private static final float KNOCKBACK_CHANCE = 0.75F;
    /** 10 % : en plus du repoussement, dégâts (tirage indépendant après succès du repoussement). */
    private static final float BONUS_DAMAGE_CHANCE = 0.10F;

    private static final double KNOCKBACK_H_BASE = 0.52;
    private static final double KNOCKBACK_H_PER_LEVEL = 0.28;
    private static final double KNOCKBACK_Y_BASE = 0.10;
    private static final double KNOCKBACK_Y_PER_LEVEL = 0.055;

    private static final float DAMAGE_BASE = 2.0F;
    private static final float DAMAGE_PER_LEVEL = 1.5F;

    private ShieldEnchantmentEvents() {
    }

    @SubscribeEvent
    public static void onShieldBlockRepulse(LivingShieldBlockEvent event) {
        if (!event.getBlocked()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack shield = player.getUseItem();
        if (shield.isEmpty()) {
            return;
        }
        int chargeLevel = ModEnchantmentKeys.enchantmentLevel(shield, level.registryAccess(), ModEnchantmentKeys.SHIELD_CHARGE);
        if (chargeLevel <= 0) {
            return;
        }
        Entity direct = event.getDamageSource().getDirectEntity();
        if (!(direct instanceof LivingEntity attacker) || attacker == player || !attacker.isAlive() || !attacker.isAttackable()) {
            return;
        }
        if (level.random.nextFloat() >= KNOCKBACK_CHANCE) {
            return;
        }

        Vec3 fromPlayer = attacker.position().subtract(player.position());
        double hxz = Math.hypot(fromPlayer.x, fromPlayer.z);
        Vec3 push;
        if (hxz < 1.0E-4) {
            Vec3 look = player.getLookAngle();
            double hl = Math.hypot(look.x, look.z);
            if (hl < 1.0E-4) {
                return;
            }
            push = new Vec3(look.x / hl, 0.0, look.z / hl);
        } else {
            push = new Vec3(fromPlayer.x / hxz, 0.0, fromPlayer.z / hxz);
        }

        double hStrength = KNOCKBACK_H_BASE + KNOCKBACK_H_PER_LEVEL * chargeLevel;
        double yStrength = KNOCKBACK_Y_BASE + KNOCKBACK_Y_PER_LEVEL * chargeLevel;
        Vec3 impulse = new Vec3(push.x * hStrength, yStrength, push.z * hStrength);
        attacker.setDeltaMovement(attacker.getDeltaMovement().add(impulse));
        attacker.hasImpulse = true;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9F, 0.65F + level.random.nextFloat() * 0.15F);

        if (level.random.nextFloat() < BONUS_DAMAGE_CHANCE) {
            float dmg = DAMAGE_BASE + DAMAGE_PER_LEVEL * chargeLevel;
            attacker.hurt(level.damageSources().playerAttack(player), dmg);
            level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.35F, 1.1F);
        }
    }
}
