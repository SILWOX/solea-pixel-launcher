package com.soleapixel.bmcmod.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.gameplay.ScytheSweepAttackLogic;

/**
 * Onde de tornade voyageant tout droit ; dégâts en zone toutes les 3 ticks (pas de collision entité).
 */
public final class ScytheTornadoProjectile extends ThrowableItemProjectile implements ItemSupplier {
    public static final int LIFETIME_TICKS = 40;
    private static final int DAMAGE_INTERVAL_TICKS = 3;

    private int ticksAlive;
    private float damageAmount;
    private int briarRingLevel;

    public ScytheTornadoProjectile(EntityType<? extends ScytheTornadoProjectile> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public void configure(float damagePerPulse, int briarRingLvl) {
        this.damageAmount = damagePerPulse;
        this.briarRingLevel = briarRingLvl;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SNOWBALL;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity target) {
        return false;
    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.BLOCK) {
            super.onHit(result);
            if (!level().isClientSide()) {
                discard();
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            double x = getX();
            double y = getY() + 0.35;
            double z = getZ();
            for (int i = 0; i < 5; i++) {
                double ox = (random.nextDouble() - 0.5) * 1.1;
                double oy = random.nextGaussian() * 0.08;
                double oz = (random.nextDouble() - 0.5) * 1.1;
                level().addParticle(ParticleTypes.CLOUD, x + ox, y + oy, z + oz, 0.04, 0.12, 0.04);
            }
            if (random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.SWEEP_ATTACK, x, y, z, (random.nextDouble() - 0.5) * 0.2, 0.05, (random.nextDouble() - 0.5) * 0.2);
            }
            return;
        }

        ticksAlive++;
        if (ticksAlive > LIFETIME_TICKS || damageAmount <= 0) {
            discard();
            return;
        }

        if (ticksAlive % DAMAGE_INTERVAL_TICKS == 0) {
            pulseDamageServer();
        }
    }

    private void pulseDamageServer() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        Entity ownerEntity = getOwner();
        if (!(ownerEntity instanceof ServerPlayer player)) {
            return;
        }
        var entities = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(1.05, 0.9, 1.05), this::isTornadoDamageTarget);
        for (LivingEntity victim : entities) {
            ScytheSweepAttackLogic.applyTornadoHit(player, sl, victim, damageAmount, briarRingLevel);
        }
    }

    private boolean isTornadoDamageTarget(LivingEntity e) {
        return e.isAlive() && !e.isSpectator() && e != getOwner();
    }

    public static ScytheTornadoProjectile release(
            EntityType<? extends ScytheTornadoProjectile> type,
            ServerLevel level,
            ServerPlayer owner,
            Vec3 spawnAt,
            Vec3 direction,
            float speed,
            float damagePerPulse,
            int briarRingLevel) {
        ScytheTornadoProjectile p = new ScytheTornadoProjectile(type, level);
        p.setOwner(owner);
        p.configure(damagePerPulse, briarRingLevel);
        p.setPos(spawnAt.x, spawnAt.y, spawnAt.z);
        Vec3 dir = direction;
        double len = dir.length();
        if (len < 1.0e-6) {
            dir = Vec3.directionFromRotation(0.0F, owner.getYRot());
        } else {
            dir = dir.scale(1.0 / len);
        }
        p.shoot(dir.x, dir.y, dir.z, speed, 0.0F);
        return p;
    }
}
