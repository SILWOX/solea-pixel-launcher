package com.soleapixel.bmcmod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.entity.projectile.VoidShardProjectile;
import com.soleapixel.bmcmod.registry.ModEntities;
import com.soleapixel.bmcmod.registry.ModItems;

import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

import java.util.function.Predicate;

public class Blink extends Monster implements RangedAttackMob {

    private static final double SHARD_SPEED = 1.95;
    /** Avance la cible pour compenser son déplacement (facteur sur le temps de vol). */
    private static final double AIM_LEAD_FACTOR = 0.42;

    public Blink(EntityType<? extends Blink> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.FLYING_SPEED, 0.42D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanPassDoors(true);
        nav.setCanFloat(true);
        return nav;
    }

    private static final Predicate<LivingEntity> AVOID_SURVIVAL_PLAYERS =
            living -> living instanceof Player p && !p.getAbilities().instabuild && !p.isSpectator();

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 14.0F, 1.2D, 1.7D, AVOID_SURVIVAL_PLAYERS));
        this.goalSelector.addGoal(3, new RangedAttackGoal(this, 1.0D, 26, 34, 22.0F));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.tickCount % 10 != 0) {
            return;
        }
        Player nearest = this.level().getNearestPlayer(this, 11.0);
        if (nearest == null || nearest.getAbilities().instabuild || nearest.isSpectator()) {
            return;
        }
        if (this.distanceToSqr(nearest) < 121.0 && this.random.nextFloat() < 0.4F) {
            Vec3 v = this.getDeltaMovement();
            this.setDeltaMovement(v.x, Math.min(v.y + 0.12, 0.5), v.z);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.tickClientEndAura();
        }
    }

    /** Particules type Blaze, palette End (portail / inverse / end rod). */
    private void tickClientEndAura() {
        if (this.isInvisible()) {
            return;
        }
        Level level = this.level();
        RandomSource rnd = this.random;
        double w = this.getBbWidth() * 0.55;
        double h = this.getBbHeight();
        double x = this.getX();
        double y = this.getY() + 0.25 + rnd.nextDouble() * h;
        double z = this.getZ();
        int n = 2 + rnd.nextInt(3);
        for (int i = 0; i < n; i++) {
            double px = x + (rnd.nextDouble() - 0.5) * w;
            double py = y + rnd.nextGaussian() * 0.12;
            double pz = z + (rnd.nextDouble() - 0.5) * w;
            double vx = (rnd.nextDouble() - 0.5) * 0.015;
            double vy = rnd.nextDouble() * 0.02;
            double vz = (rnd.nextDouble() - 0.5) * 0.015;
            level.addParticle(ParticleTypes.PORTAL, px, py, pz, vx, vy, vz);
            if (rnd.nextBoolean()) {
                level.addParticle(ParticleTypes.REVERSE_PORTAL, px, py, pz, vx * 0.5, vy * 0.5, vz * 0.5);
            }
        }
        if (rnd.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.END_ROD,
                    x + (rnd.nextDouble() - 0.5) * w,
                    this.getY() + 0.2 + rnd.nextDouble() * h,
                    z + (rnd.nextDouble() - 0.5) * w,
                    0.0, 0.015, 0.0);
        }
        if (rnd.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.DRAGON_BREATH,
                    x + (rnd.nextDouble() - 0.5) * w * 0.8,
                    this.getY() + 0.15,
                    z + (rnd.nextDouble() - 0.5) * w * 0.8,
                    0.0, -0.02, 0.0);
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        VoidShardProjectile shard = new VoidShardProjectile(ModEntities.VOID_SHARD_PROJECTILE.get(), server);
        shard.setOwner(this);
        Vec3 start = this.getEyePosition();
        shard.setPos(start.x, start.y, start.z);

        Vec3 tgtEyes = target.getEyePosition(1.0F);
        Vec3 rough = tgtEyes.subtract(start);
        double roughLen = rough.length();
        if (roughLen < 1.0E-4) {
            return;
        }
        double flightTime = Mth.clamp(roughLen / SHARD_SPEED, 0.35, 14.0);
        Vec3 vel = target.getDeltaMovement();
        Vec3 predicted = tgtEyes.add(vel.scale(flightTime * AIM_LEAD_FACTOR));
        Vec3 to = predicted.subtract(start);
        double len = to.length();
        if (len < 1.0E-4) {
            return;
        }
        Vec3 dir = to.scale(1.0 / len);
        shard.setDeltaMovement(dir.scale(SHARD_SPEED));

        float yRot = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0F;
        float xRot = (float) (-Mth.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * Mth.RAD_TO_DEG);
        this.setYRot(yRot % 360.0F);
        this.yRotO = this.getYRot();
        this.setYHeadRot(yRot);
        this.setXRot(xRot);
        this.xRotO = this.getXRot();

        server.addFreshEntity(shard);
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, start.x, start.y, start.z, 12, 0.25, 0.2, 0.25, 0.04);
        server.sendParticles(ParticleTypes.PORTAL, start.x, start.y, start.z, 8, 0.2, 0.15, 0.2, 0.03);
        this.playSound(SoundEvents.BREEZE_SHOOT, 0.5F, 1.2F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BREEZE_IDLE_GROUND;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.BREEZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BREEZE_DEATH;
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide()) {
            int shards = 1 + this.random.nextInt(4);
            this.spawnAtLocation(new ItemStack(ModItems.VOID_SHARD.get(), shards));
            int rods = 1 + this.random.nextInt(3);
            this.spawnAtLocation(new ItemStack(ModItems.BLINK_ROD.get(), rods));
        }
        super.die(source);
    }
}
