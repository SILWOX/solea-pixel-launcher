package com.soleapixel.bmcmod.entity.projectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.registry.ModItems;

public class VoidShardProjectile extends ThrowableItemProjectile implements ItemSupplier {
    private static final float ARROW_LIKE_DAMAGE = 6.0F;

    public VoidShardProjectile(EntityType<? extends VoidShardProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.VOID_SHARD.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide()) {
            return;
        }
        Entity victim = result.getEntity();
        Entity shooter = this.getOwner();
        DamageSource source = this.damageSources().mobProjectile(this, shooter instanceof LivingEntity le ? le : null);
        if (victim instanceof LivingEntity living) {
            living.hurt(source, ARROW_LIKE_DAMAGE);
        }
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 1.0F, 1.0F);
        if (shooter != null && victim != shooter && victim.isAlive() && shooter.isAlive()) {
            swapPositions(shooter, victim);
        }
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 0.6F, 1.1F);
            this.discard();
        }
    }

    private static void swapPositions(Entity a, Entity b) {
        double ax = a.getX();
        double ay = a.getY();
        double az = a.getZ();
        float ayaw = a.getYRot();
        float apitch = a.getXRot();
        double bx = b.getX();
        double by = b.getY();
        double bz = b.getZ();
        float byaw = b.getYRot();
        float bpitch = b.getXRot();

        teleportEntity(a, bx, by, bz, byaw, bpitch);
        teleportEntity(b, ax, ay, az, ayaw, apitch);

        a.resetFallDistance();
        b.resetFallDistance();
        if (a instanceof LivingEntity la) {
            la.hurtMarked = true;
        }
        if (b instanceof LivingEntity lb) {
            lb.hurtMarked = true;
        }
    }

    private static void teleportEntity(Entity entity, double x, double y, double z, float yRot, float xRot) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(level, x, y, z, yRot, xRot);
        } else {
            entity.moveTo(x, y, z, yRot, xRot);
        }
    }

    public void shootTowards(LivingEntity target) {
        Vec3 aim = target.getBoundingBox().getCenter().subtract(this.position()).normalize();
        double speed = 1.85;
        this.setDeltaMovement(aim.scale(speed));
    }

    public void shootWithSpread(Entity shooter, float xRot, float yRot, float pitch, float velocity, float inaccuracy) {
        super.shootFromRotation(shooter, xRot, yRot, pitch, velocity, inaccuracy);
    }
}
