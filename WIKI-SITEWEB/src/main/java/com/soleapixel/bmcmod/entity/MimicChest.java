package com.soleapixel.bmcmod.entity;

import javax.annotation.Nullable;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.soleapixel.bmcmod.registry.ModSounds;

/**
 * Coffre-mimic : poursuite et mêlée ; sprint côté serveur quand la cible est un peu loin pour animer la « course ».
 */
public class MimicChest extends Monster {

    public MimicChest(EntityType<? extends MimicChest> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 28.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.28D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 14.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.getTarget() != null && this.getTarget().isAlive()) {
            double d = this.distanceToSqr(this.getTarget());
            this.setSprinting(d > 3.5 * 3.5 && d < 20 * 20);
        } else {
            this.setSprinting(false);
        }
    }

    private boolean isChasingAlivePlayer() {
        return this.getTarget() instanceof Player player && player.isAlive();
    }

    @Override
    public int getAmbientSoundInterval() {
        return this.isChasingAlivePlayer() ? 48 : 400;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.isChasingAlivePlayer() ? ModSounds.ENTITY_MIMIC_CHEST_CHASE.get() : null;
    }

    @Override
    public void playAmbientSound() {
        if (!this.isChasingAlivePlayer()) {
            return;
        }
        super.playAmbientSound();
        if (this.random.nextFloat() < 0.5F) {
            boolean open = this.random.nextBoolean();
            this.playSound(
                    open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE,
                    0.26F + this.random.nextFloat() * 0.1F,
                    0.78F + this.random.nextFloat() * 0.28F);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected void playHurtSound(DamageSource damageSource) {
        super.playHurtSound(damageSource);
        if (this.random.nextFloat() < 0.4F) {
            this.playSound(
                    SoundEvents.WOOD_HIT,
                    0.38F + this.random.nextFloat() * 0.12F,
                    0.82F + this.random.nextFloat() * 0.22F);
        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!this.level().isClientSide()) {
            this.playSound(SoundEvents.CHEST_CLOSE, 0.52F, 0.74F + this.random.nextFloat() * 0.22F);
        }
        super.die(damageSource);
    }
}
