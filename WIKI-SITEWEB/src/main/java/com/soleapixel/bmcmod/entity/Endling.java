package com.soleapixel.bmcmod.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.Vec3;

public class Endling extends Monster {
    private int circleTimer;

    public Endling(EntityType<? extends Endling> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.85D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new EndlingDashGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel)) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (!(target instanceof Player player)) {
            return;
        }
        this.circleTimer++;
        if (this.circleTimer < 100 + this.random.nextInt(100)) {
            return;
        }
        this.circleTimer = 0;
        Vec3 t = player.position();
        Vec3 self = this.position();
        Vec3 to = t.subtract(self);
        double dist = to.horizontalDistance();
        if (dist > 6.0D || dist < 0.5D) {
            return;
        }
        Vec3 push = to.normalize().scale(1.12);
        player.push(push.x, 0.38, push.z);
        player.hurtMarked = true;
        EndGolem.onPlayerShovedByEndling((ServerLevel) this.level(), player, this, push);
    }
}
