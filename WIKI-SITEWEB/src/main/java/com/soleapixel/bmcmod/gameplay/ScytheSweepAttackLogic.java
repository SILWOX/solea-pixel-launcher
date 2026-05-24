package com.soleapixel.bmcmod.gameplay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.entity.projectile.ScytheTornadoProjectile;
import com.soleapixel.bmcmod.item.ScytheItem;
import com.soleapixel.bmcmod.network.ScythePackets;
import com.soleapixel.bmcmod.registry.ModEntities;
import com.soleapixel.bmcmod.registry.ModEnchantmentKeys;

import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Coup spécial « tourbillon » : avec enchantement Tourbillon, tornades rayonnantes ;
 * sinon cône devant le joueur, bonus dégâts sur la cible primaire, étourdissement court.
 */
public final class ScytheSweepAttackLogic {
    private ScytheSweepAttackLogic() {
    }

    public static void performSweep(ServerPlayer player, ScythePackets.ScytheSweepAttackPayload payload) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        long lockUntil = player.getPersistentData().getLong(ScytheItem.SWEEP_COOLDOWN_GAME_TIME_TAG);
        if (now < lockUntil) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ScytheItem scythe)) {
            return;
        }
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            return;
        }

        var reg = level.registryAccess();
        int primaryId = payload.primaryEntityId();

        if (primaryId < 0) {
            if (ModEnchantmentKeys.enchantmentLevel(stack, reg, ModEnchantmentKeys.WHIRLWIND) <= 0) {
                return;
            }
            performWhirlwindTornadoSweep(player, level, stack, scythe, reg, now);
            return;
        }

        performClassicConeSweep(player, level, stack, scythe, reg, now, payload);
    }

    /**
     * Un « coup » de tornade : même traitement qu'une touche de zone du balayage (hors primaire).
     */
    public static void applyTornadoHit(ServerPlayer player, ServerLevel level, LivingEntity victim, float damage, int briarLevel) {
        if (!victim.isAlive() || victim == player) {
            return;
        }
        DamageSource src = player.damageSources().playerAttack(player);
        victim.invulnerableTime = 0;
        victim.hurt(src, damage);
        applySweepStun(victim, false);
        applyBriarRingOnHit(level, victim, briarLevel);
        playHitStyleFx(level, victim, false);
    }

    private static void performWhirlwindTornadoSweep(
            ServerPlayer player,
            ServerLevel level,
            ItemStack stack,
            ScytheItem scythe,
            RegistryAccess reg,
            long now) {
        int whirlMult = ScytheItem.whirlwindDurabilityMultiplier(stack, reg);
        int briarLevel = ModEnchantmentKeys.enchantmentLevel(stack, reg, ModEnchantmentKeys.BRIAR_RING);
        float effRange = ScytheItem.sweepEffectiveRange(stack, reg);

        float damagePerPulse = ScytheItem.sweepAoeDamage(scythe.scytheTier()) * 0.35F * whirlMult;

        Vec3 base = player.position().add(0.0, 1.05, 0.0);
        for (int i = 0; i < ScytheItem.TORNADO_COUNT; i++) {
            double angle = (i / (double) ScytheItem.TORNADO_COUNT) * Math.PI * 2.0;
            Vec3 dir = new Vec3(Math.cos(angle), 0.06, Math.sin(angle)).normalize();
            ScytheTornadoProjectile tornado = ScytheTornadoProjectile.release(
                    ModEntities.SCYTHE_TORNADO.get(),
                    level,
                    player,
                    base,
                    dir,
                    ScytheItem.TORNADO_SHOOT_SPEED,
                    damagePerPulse,
                    briarLevel);
            level.addFreshEntity(tornado);
        }

        if (briarLevel > 0) {
            playBriarGroundRing(level, player, effRange, briarLevel);
        }

        player.resetAttackStrengthTicker();
        player.swing(InteractionHand.MAIN_HAND, true);

        playSweepSoundsAndRingParticles(level, player);

        int cost = (ScytheItem.SWEEP_DURABILITY_BASE + ScytheItem.TORNADO_COUNT / 2) * whirlMult;
        if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(cost, player, EquipmentSlot.MAINHAND);
        }

        player.getPersistentData().putLong(ScytheItem.SWEEP_COOLDOWN_GAME_TIME_TAG, now + ScytheItem.SWEEP_COOLDOWN_TICKS);

        int totalSpinTicks = ScytheItem.sweepVisualTotalSpinTicks(stack, reg);
        int fullRotations = ScytheItem.sweepVisualFullRotations(stack, reg);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new ScythePackets.ScytheSpinVisualPayload(player.getId(), totalSpinTicks, fullRotations));
    }

    private static void performClassicConeSweep(
            ServerPlayer player,
            ServerLevel level,
            ItemStack stack,
            ScytheItem scythe,
            RegistryAccess reg,
            long now,
            ScythePackets.ScytheSweepAttackPayload payload) {
        int whirlDamageMult = ScytheItem.whirlwindDurabilityMultiplier(stack, reg);
        int briarLevel = ModEnchantmentKeys.enchantmentLevel(stack, reg, ModEnchantmentKeys.BRIAR_RING);
        float effRange = ScytheItem.sweepEffectiveRange(stack, reg);
        double effRangeSq = (double) effRange * effRange;
        double maxPrimaryDist = Math.max(player.entityInteractionRange() + 2.0, effRange);

        Entity primaryEntity = level.getEntity(payload.primaryEntityId());
        if (!(primaryEntity instanceof LivingEntity primaryLiving) || !primaryLiving.isAlive()) {
            return;
        }
        if (player.distanceToSqr(primaryLiving) > maxPrimaryDist * maxPrimaryDist) {
            return;
        }
        if (!player.hasLineOfSight(primaryLiving)) {
            return;
        }

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle().normalize();

        Vec3 toPrimary = primaryLiving.getBoundingBox().getCenter().subtract(eye);
        if (toPrimary.lengthSqr() < 1.0E-4) {
            return;
        }
        toPrimary = toPrimary.normalize();
        if (look.dot(toPrimary) < ScytheItem.SWEEP_MIN_DOT) {
            return;
        }
        if (eye.distanceToSqr(primaryLiving.getBoundingBox().getCenter()) > effRangeSq) {
            return;
        }

        AABB search = player.getBoundingBox().inflate(effRange + 2.0);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, search, e -> e != player && e.isAlive());

        List<LivingEntity> inCone = new ArrayList<>();
        for (LivingEntity e : candidates) {
            Vec3 mid = e.getBoundingBox().getCenter();
            double dSq = eye.distanceToSqr(mid);
            if (dSq > effRangeSq) {
                continue;
            }
            Vec3 to = mid.subtract(eye);
            if (to.lengthSqr() < 1.0E-6) {
                continue;
            }
            to = to.normalize();
            if (look.dot(to) < ScytheItem.SWEEP_MIN_DOT) {
                continue;
            }
            if (!player.hasLineOfSight(e)) {
                continue;
            }
            inCone.add(e);
        }

        if (!inCone.contains(primaryLiving)) {
            return;
        }

        inCone.sort(Comparator.comparingDouble(a -> a.distanceToSqr(player)));

        float baseAoe = ScytheItem.sweepAoeDamage(scythe.scytheTier()) * whirlDamageMult;
        float primaryDamage = baseAoe * ScytheItem.sweepPrimaryBonusMultiplier();

        DamageSource src = player.damageSources().playerAttack(player);

        int hits = 0;
        for (LivingEntity target : inCone) {
            if (hits >= 1 + ScytheItem.SWEEP_MAX_EXTRA_TARGETS) {
                break;
            }
            float amount = target == primaryLiving ? primaryDamage : baseAoe;
            target.invulnerableTime = 0;
            target.hurt(src, amount);
            boolean primary = target == primaryLiving;
            applySweepStun(target, primary);
            applyBriarRingOnHit(level, target, briarLevel);
            playHitStyleFx(level, target, primary);
            hits++;
        }

        if (briarLevel > 0) {
            playBriarGroundRing(level, player, effRange, briarLevel);
        }

        player.resetAttackStrengthTicker();
        player.swing(InteractionHand.MAIN_HAND, true);

        playSweepSoundsAndRingParticles(level, player);

        int cost = ScytheItem.SWEEP_DURABILITY_BASE + Math.min(ScytheItem.SWEEP_MAX_EXTRA_TARGETS, Math.max(0, hits - 1)) * ScytheItem.SWEEP_DURABILITY_PER_TARGET;
        cost *= ScytheItem.whirlwindDurabilityMultiplier(stack, reg);
        if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(cost, player, EquipmentSlot.MAINHAND);
        }

        player.getPersistentData().putLong(ScytheItem.SWEEP_COOLDOWN_GAME_TIME_TAG, now + ScytheItem.SWEEP_COOLDOWN_TICKS);

        int totalSpinTicks = ScytheItem.sweepVisualTotalSpinTicks(stack, reg);
        int fullRotations = ScytheItem.sweepVisualFullRotations(stack, reg);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new ScythePackets.ScytheSpinVisualPayload(player.getId(), totalSpinTicks, fullRotations));
    }

    private static void playSweepSoundsAndRingParticles(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.82F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.4F, 1.25F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.5F, 1.2F);

        double px = player.getX();
        double py = player.getY() + 0.1;
        double pz = player.getZ();
        for (int i = 0; i < 18; i++) {
            double a = (i / 18.0) * Math.PI * 2.0;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, px + Math.cos(a) * 1.35, py + 0.05, pz + Math.sin(a) * 1.35, 1, 0.08, 0.06, 0.08, 0.0);
        }
        for (int i = 0; i < 12; i++) {
            double a = (i / 12.0) * Math.PI * 2.0;
            level.sendParticles(ParticleTypes.ENCHANT, px + Math.cos(a) * 0.9, player.getY() + 1.0, pz + Math.sin(a) * 0.9, 2, 0.12, 0.2, 0.12, 0.25);
        }
        level.sendParticles(ParticleTypes.CLOUD, px, player.getY() + 0.15, pz, 8, 0.35, 0.05, 0.35, 0.02);
    }

    private static void applyBriarRingOnHit(ServerLevel level, LivingEntity target, int briarLevel) {
        if (briarLevel <= 0) {
            return;
        }
        int ticks = briarLevel == 1 ? 25 : 40;
        var weak = new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0, false, true, true);
        if (target.canBeAffected(weak)) {
            target.addEffect(weak);
        }
        double x = target.getX();
        double y = target.getY() + 0.12;
        double z = target.getZ();
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, x, y, z, 6 + briarLevel * 4, 0.28, 0.18, 0.28, 0.012);
    }

    private static void playBriarGroundRing(ServerLevel level, ServerPlayer player, float sweepRadius, int briarLevel) {
        double px = player.getX();
        double py = player.getY() + 0.1;
        double pz = player.getZ();
        int n = briarLevel == 1 ? 20 : 32;
        double r = Math.min(Math.max(1.5, sweepRadius * 0.45), 3.8);
        for (int i = 0; i < n; i++) {
            double a = (i / (double) n) * Math.PI * 2.0;
            level.sendParticles(
                    ParticleTypes.SPORE_BLOSSOM_AIR,
                    px + Math.cos(a) * r,
                    py,
                    pz + Math.sin(a) * r,
                    1,
                    0.05,
                    0.01,
                    0.05,
                    0.0);
        }
    }

    private static void applySweepStun(LivingEntity target, boolean primary) {
        int ticks = primary ? ScytheItem.SWEEP_STUN_PRIMARY_TICKS : ScytheItem.SWEEP_STUN_AOE_TICKS;
        var slow = new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                ticks,
                ScytheItem.SWEEP_STUN_SLOWNESS_AMPLIFIER,
                false,
                true,
                true);
        if (target.canBeAffected(slow)) {
            target.addEffect(slow);
        }
        var dig = new MobEffectInstance(
                MobEffects.DIG_SLOWDOWN,
                ticks,
                ScytheItem.SWEEP_STUN_FATIGUE_AMPLIFIER,
                false,
                true,
                true);
        if (target.canBeAffected(dig)) {
            target.addEffect(dig);
        }
    }

    private static void playHitStyleFx(ServerLevel level, LivingEntity target, boolean primary) {
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();
        int nCrit = primary ? 14 : 8;
        level.sendParticles(ParticleTypes.CRIT, x, y, z, nCrit, 0.35, 0.35, 0.35, 0.12);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y, z, primary ? 10 : 5, 0.25, 0.2, 0.25, 0.2);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y - 0.2, z, 3, 0.2, 0.1, 0.2, 0.0);
        float vol = primary ? 0.95F : 0.65F;
        float pitch = level.random.nextFloat() * 0.15F + 0.95F;
        level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, vol, pitch);
        if (primary) {
            level.playSound(null, x, y, z, SoundEvents.MACE_SMASH_AIR, SoundSource.PLAYERS, 0.45F, 1.1F);
        }
    }
}
