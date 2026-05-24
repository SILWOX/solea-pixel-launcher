package com.soleapixel.bmcmod.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.registry.ModEntities;
import com.soleapixel.bmcmod.registry.ModItems;

public class EndGolem extends IronGolem {
    private static final float MAX_HEALTH = 510.0F;
    /** Tir de secours quand un Endling éloigne le joueur (distance min depuis le golem). */
    private static final double ASSIST_MIN_DIST = 9.0;
    private static final double ASSIST_MAX_DIST = 64.0;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.bmcmod.end_golem"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);
    private boolean summoned75;
    private boolean summoned50;
    private int assistFireCooldown;
    private int voidWaveCooldown;
    private int slamCooldown;
    private int blinkCooldown;
    /** Lasers / phases (séparé du blink — ne pas réutiliser ces cooldowns pour la téléportation). */
    private int directedLaserCooldown;
    private int radialLaserCooldown;
    private int ringPulseCooldown;

    public EndGolem(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
        this.xpReward = 200;
    }

    /**
     * Boss : ne pas hériter de l’IA du golem de fer (ciblage des monstres près des villages).
     * Cible uniquement les joueurs.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9D, 32.0F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (target != null && !(target instanceof Player)) {
            return;
        }
        super.setTarget(target);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return IronGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.82D)
                .add(Attributes.ATTACK_DAMAGE, 26.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.5D);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (this.assistFireCooldown > 0) {
            this.assistFireCooldown--;
        }
        if (this.voidWaveCooldown > 0) {
            this.voidWaveCooldown--;
        }
        if (this.slamCooldown > 0) {
            this.slamCooldown--;
        }
        if (this.blinkCooldown > 0) {
            this.blinkCooldown--;
        }
        if (this.directedLaserCooldown > 0) {
            this.directedLaserCooldown--;
        }
        if (this.radialLaserCooldown > 0) {
            this.radialLaserCooldown--;
        }
        if (this.ringPulseCooldown > 0) {
            this.ringPulseCooldown--;
        }
        float ratio = this.getHealth() / this.getMaxHealth();
        if (ratio < 0.25F) {
            this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
            this.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
        }
        if (!this.summoned75 && ratio <= 0.75F) {
            this.summoned75 = true;
            this.spawnEndlings(server, 4 + this.random.nextInt(2));
        }
        if (!this.summoned50 && ratio <= 0.50F) {
            this.summoned50 = true;
            this.spawnEndlings(server, 4 + this.random.nextInt(2));
        }
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            runPhaseAttacks(server, target, ratio);
        }
    }

    /**
     * Tranches de ~15 % de vie restante (0 = &gt;85 % … 6 = ≤10 %). Sert aux attaques laser / effets de phase.
     */
    private static int healthPhaseBand(float ratio) {
        if (ratio > 0.85F) {
            return 0;
        }
        if (ratio > 0.70F) {
            return 1;
        }
        if (ratio > 0.55F) {
            return 2;
        }
        if (ratio > 0.40F) {
            return 3;
        }
        if (ratio > 0.25F) {
            return 4;
        }
        if (ratio > 0.10F) {
            return 5;
        }
        return 6;
    }

    private void runPhaseAttacks(ServerLevel server, LivingEntity target, float ratio) {
        double dist = this.distanceTo(target);
        int phase = healthPhaseBand(ratio);

        // Sous ~33 % : comportement d’origine (boules de feu, onde, slam, téléport près du joueur) — inchangé.
        if (ratio <= 0.33F) {
            if (this.tickCount % 20 == 0) {
                this.shootLargeFireballAt(server, target);
            }
            if (this.voidWaveCooldown <= 0) {
                castVoidWave(server, 6.5D, 1.4F, 180, 1);
                this.voidWaveCooldown = 70;
            }
            if (this.slamCooldown <= 0 && dist < 7.5D) {
                castShockSlam(server, target, 1.8F);
                this.slamCooldown = 40;
            }
            if (this.blinkCooldown <= 0 && dist > 10.0D) {
                blinkNearTarget(server, target);
                this.blinkCooldown = 75;
            }
            tryPhaseLaserExtras(server, target, dist, phase);
            return;
        }

        if (ratio > 0.66F) {
            if (this.tickCount % 42 == 0 && dist > 9.0D) {
                this.shootLargeFireballAt(server, target);
            }
            if (this.slamCooldown <= 0 && dist < 5.0D) {
                castShockSlam(server, target, 1.25F);
                this.slamCooldown = 65;
            }
        } else {
            if (this.tickCount % 30 == 0 && dist > 6.0D) {
                this.shootLargeFireballAt(server, target);
            }
            if (this.voidWaveCooldown <= 0) {
                castVoidWave(server, 5.0D, 1.0F, 140, 0);
                this.voidWaveCooldown = 90;
            }
            if (this.slamCooldown <= 0 && dist < 6.5D) {
                castShockSlam(server, target, 1.5F);
                this.slamCooldown = 55;
            }
        }
        tryPhaseLaserExtras(server, target, dist, phase);
    }

    /** Rayons laser, rafale radiale et anneau : s’ajoutent selon le palier de vie (sans modifier la téléportation). */
    private void tryPhaseLaserExtras(ServerLevel server, LivingEntity target, double dist, int phase) {
        if (phase < 1) {
            return;
        }
        if (this.directedLaserCooldown <= 0 && dist > 3.5D) {
            float dmg = 3.0F + Mth.clamp(phase, 1, 4) * 0.55F;
            castDirectedLaser(server, target, dmg);
            this.directedLaserCooldown = Mth.clamp(80 - phase * 9, 22, 80);
        }
        if (phase >= 2 && this.radialLaserCooldown <= 0) {
            float rdmg = 2.5F + (phase >= 5 ? 1.8F : 0.0F);
            castRadialLaserBurst(server, target, rdmg);
            this.radialLaserCooldown = phase >= 6 ? 38 : Mth.clamp(95 - phase * 6, 45, 95);
        }
        if (phase >= 3 && this.ringPulseCooldown <= 0) {
            double ringR = 6.8D + phase * 0.35D;
            float ringDmg = 1.75F + Mth.clamp(phase - 2, 0, 4) * 0.22F;
            castGroundRingPulse(server, ringR, ringDmg);
            this.ringPulseCooldown = Mth.clamp(105 - phase * 5, 48, 105);
        }
    }

    private void castDirectedLaser(ServerLevel server, LivingEntity target, float damage) {
        Vec3 start = this.getEyePosition(1.0F);
        Vec3 aim = target.getEyePosition(1.0F).subtract(start);
        double len = Math.min(aim.length(), 42.0D);
        if (len < 0.5D) {
            return;
        }
        Vec3 dir = aim.normalize();
        AABB hitBox = target.getBoundingBox().inflate(0.35D, 0.2D, 0.35D);
        boolean hit = false;
        for (double d = 0.4D; d < len && !hit; d += 0.42D) {
            Vec3 p = start.add(dir.scale(d));
            server.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 3, 0.08D, 0.08D, 0.08D, 0.01D);
            if (d % 2.1D < 0.45D) {
                server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0.04D, 0.04D, 0.04D, 0.0D);
            }
            if (hitBox.contains(p)) {
                target.hurt(this.damageSources().magic(), damage);
                hit = true;
            }
        }
        Vec3 end = start.add(dir.scale(len));
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, end.x, end.y, end.z, 8, 0.2D, 0.2D, 0.2D, 0.04D);
        server.playSound(null, start.x, start.y, start.z, SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 0.45F, 1.7F);
    }

    private void castRadialLaserBurst(ServerLevel server, LivingEntity target, float damagePerHit) {
        Vec3 center = new Vec3(this.getX(), this.getY() + this.getEyeHeight() * 0.55D, this.getZ());
        int rays = 8;
        for (int i = 0; i < rays; i++) {
            double ang = (Math.PI * 2.0D * i) / rays;
            Vec3 dir = new Vec3(Math.cos(ang), 0.12D, Math.sin(ang)).normalize();
            for (double d = 0.7D; d < 8.0D; d += 0.45D) {
                Vec3 p = center.add(dir.scale(d));
                server.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 2, 0.07D, 0.07D, 0.07D, 0.01D);
                server.sendParticles(ParticleTypes.PORTAL, p.x, p.y - 0.1D, p.z, 1, 0.1D, 0.05D, 0.1D, 0.02D);
                if (target.getBoundingBox().inflate(0.45D).contains(p)) {
                    target.hurt(this.damageSources().magic(), damagePerHit);
                    break;
                }
            }
        }
        server.playSound(null, center.x, center.y, center.z, SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 0.5F, 0.55F + this.random.nextFloat() * 0.2F);
    }

    private void castGroundRingPulse(ServerLevel server, double outerRadius, float damage) {
        Vec3 c = this.position();
        for (Player nearby : server.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(outerRadius + 1.5D))) {
            double dx = nearby.getX() - c.x;
            double dz = nearby.getZ() - c.z;
            double horiz = Math.sqrt(dx * dx + dz * dz);
            if (horiz < outerRadius * 0.38D || horiz > outerRadius) {
                continue;
            }
            nearby.hurt(this.damageSources().magic(), damage);
            Vec3 push = new Vec3(dx, 0.0D, dz).normalize().scale(0.42D);
            nearby.push(push.x, 0.14D, push.z);
            nearby.hurtMarked = true;
        }
        int steps = 52;
        for (int i = 0; i < steps; i++) {
            double ang = (Math.PI * 2.0D * i) / steps;
            double px = c.x + Math.cos(ang) * outerRadius;
            double pz = c.z + Math.sin(ang) * outerRadius;
            server.sendParticles(ParticleTypes.DRAGON_BREATH, px, c.y + 0.12D, pz, 2, 0.14D, 0.06D, 0.14D, 0.015D);
        }
        server.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.2D, c.z, 24, outerRadius * 0.4D, 0.15D, outerRadius * 0.4D, 0.02D);
        server.playSound(null, c.x, c.y, c.z, SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.HOSTILE, 0.4F, 1.5F);
    }

    private void castVoidWave(ServerLevel server, double radius, float damage, int effectDuration, int effectAmp) {
        Vec3 c = this.position();
        for (LivingEntity nearby : server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius))) {
            if (nearby == this || !(nearby instanceof Player)) {
                continue;
            }
            if (nearby.distanceTo(this) > radius) {
                continue;
            }
            nearby.hurt(this.damageSources().magic(), damage);
            nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, effectDuration, effectAmp, false, true, true));
            Vec3 push = nearby.position().subtract(c);
            if (push.lengthSqr() > 1.0E-5) {
                Vec3 p = push.normalize().scale(0.55D + (radius * 0.02D));
                nearby.push(p.x, 0.25D, p.z);
                nearby.hurtMarked = true;
            }
        }
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH, c.x, c.y + 1.0D, c.z, 50, 1.1D, 0.5D, 1.1D, 0.02D);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, c.x, c.y + 1.0D, c.z, 34, 1.0D, 0.35D, 1.0D, 0.06D);
        server.playSound(null, c.x, c.y, c.z, SoundEvents.ENDERMAN_SCREAM, net.minecraft.sounds.SoundSource.HOSTILE, 0.6F, 0.7F);
    }

    private void castShockSlam(ServerLevel server, LivingEntity target, float damage) {
        Vec3 c = this.position();
        target.hurt(this.damageSources().mobAttack(this), damage);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true, true));
        Vec3 dir = target.position().subtract(c);
        if (dir.lengthSqr() > 1.0E-5) {
            Vec3 push = dir.normalize().scale(1.0D);
            target.push(push.x, 0.48D, push.z);
            target.hurtMarked = true;
        }
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, c.x, c.y + 0.6D, c.z, 26, 0.9D, 0.3D, 0.9D, 0.03D);
        server.playSound(null, c.x, c.y, c.z, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, net.minecraft.sounds.SoundSource.HOSTILE, 0.6F, 0.9F);
    }

    private void blinkNearTarget(ServerLevel server, LivingEntity target) {
        Vec3 t = target.position();
        double ox = (this.random.nextDouble() - 0.5D) * 3.0D;
        double oz = (this.random.nextDouble() - 0.5D) * 3.0D;
        this.teleportTo(t.x + ox, t.y, t.z + oz);
        this.setTarget(target);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0D, this.getZ(), 55, 0.9D, 0.9D, 0.9D, 0.08D);
        server.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.HOSTILE, 0.7F, 0.8F);
    }

    /**
     * Appelé quand un Endling pousse le joueur : tir immédiat si le joueur est assez loin et la poussée l’éloigne du golem.
     */
    public static void onPlayerShovedByEndling(ServerLevel level, Player player, Endling endling, Vec3 horizontalPush) {
        Vec3 flat = new Vec3(horizontalPush.x, 0.0, horizontalPush.z);
        if (flat.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 pushDir = flat.normalize();
        for (EndGolem golem : level.getEntitiesOfClass(EndGolem.class, player.getBoundingBox().inflate(56.0))) {
            if (golem.getTarget() != player) {
                continue;
            }
            double dist = golem.distanceTo(player);
            if (dist < ASSIST_MIN_DIST || dist > ASSIST_MAX_DIST) {
                continue;
            }
            Vec3 golemToPlayer = player.position().subtract(golem.position());
            Vec3 gtpFlat = new Vec3(golemToPlayer.x, 0.0, golemToPlayer.z);
            if (gtpFlat.lengthSqr() < 1.0E-6) {
                continue;
            }
            gtpFlat = gtpFlat.normalize();
            if (gtpFlat.dot(pushDir) < 0.18) {
                continue;
            }
            golem.tryAssistFireball(level, player);
        }
    }

    private void tryAssistFireball(ServerLevel server, Player player) {
        if (this.assistFireCooldown > 0) {
            return;
        }
        if (this.getHealth() / this.getMaxHealth() <= 0.10F) {
            return;
        }
        this.shootLargeFireballAt(server, player);
        this.assistFireCooldown = 32;
    }

    private void shootLargeFireballAt(ServerLevel server, LivingEntity target) {
        Vec3 eye = this.getEyePosition(1.0F);
        Vec3 aim = target.getEyePosition(1.0F).subtract(eye);
        double len = aim.length();
        if (len < 1.0E-4) {
            return;
        }
        Vec3 move = aim.scale(1.0 / len).scale(0.18);
        LargeFireball ball = new LargeFireball(server, this, move, 2);
        ball.setPos(eye);
        server.addFreshEntity(ball);
    }

    private void spawnEndlings(ServerLevel server, int count) {
        for (int i = 0; i < count; i++) {
            Endling e = ModEntities.ENDLING.get().create(server);
            if (e == null) {
                continue;
            }
            double ox = (this.random.nextDouble() - 0.5) * 4.0;
            double oz = (this.random.nextDouble() - 0.5) * 4.0;
            e.moveTo(this.getX() + ox, this.getY(), this.getZ() + oz, this.getYRot(), 0.0F);
            LivingEntity t = this.getTarget();
            if (t instanceof Player p) {
                e.setTarget(p);
            }
            server.addFreshEntity(e);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel server) {
            Vec3 c = this.position();
            // Dragon-like death burst: bright rays and spiral particles around the boss.
            server.playSound(null, c.x, c.y, c.z, SoundEvents.ENDER_DRAGON_DEATH, net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 1.0F);
            server.playSound(null, c.x, c.y, c.z, SoundEvents.END_PORTAL_SPAWN, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.65F);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1.2D, c.z, 8, 1.2D, 1.0D, 1.2D, 0.0D);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, c.x, c.y + 1.4D, c.z, 170, 2.3D, 1.6D, 2.3D, 0.08D);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH, c.x, c.y + 1.0D, c.z, 140, 2.0D, 1.2D, 2.0D, 0.03D);
            for (int i = 0; i < 28; i++) {
                double ang = (Math.PI * 2.0D * i) / 28.0D;
                double dx = Math.cos(ang) * 2.8D;
                double dz = Math.sin(ang) * 2.8D;
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, c.x + dx, c.y + 1.0D + (i % 4) * 0.25D, c.z + dz, 6, 0.18D, 0.2D, 0.18D, 0.02D);
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, c.x + dx * 0.7D, c.y + 0.8D, c.z + dz * 0.7D, 5, 0.16D, 0.14D, 0.16D, 0.06D);
            }
            server.explode(this, c.x, c.y, c.z, 4.0F, false, Level.ExplosionInteraction.TNT);
            this.spawnAtLocation(new ItemStack(ModItems.END_GOLEM_HEART.get()));
        }
        super.die(source);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        this.bossEvent.removeAllPlayers();
    }
}
