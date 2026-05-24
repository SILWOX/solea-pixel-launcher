package com.soleapixel.bmcmod.entity;

import com.soleapixel.bmcmod.registry.ModEntities;
import com.soleapixel.bmcmod.registry.ModItems;
import com.soleapixel.bmcmod.registry.ModParticles;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class UndeadIllager extends Evoker {
    private static final String TAG_VLINX_OWNER = "bmcmod:vlinx_owner";
    /** Limite globale de Vlinx vivants par Undead Illager (summons + conversions). */
    private static final int MAX_VLINX_PER_ILLAGER = 5;
    /** Distance d’un pas de repli vers la cible (évite de traverser toute la carte). */
    private static final double RETREAT_NAVIGATION_DISTANCE = 3.5D;
    private int specialCooldown = 100;
    private int fangRainTicks = 0;
    private int beamTicks = 0;
    private int panicRetreatTicks = 0;
    private float recentDamageWindow = 0.0F;
    private int lastDamageTick = -200;
    private int antiBurstCooldown = 0;
    private int beamAttackCooldown = 0;
    private int fangRainAttackCooldown = 0;

    public UndeadIllager(EntityType<? extends UndeadIllager> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Evoker.createAttributes()
                .add(Attributes.MAX_HEALTH, 48.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 3.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 42.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.EVOKER_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.EVOKER_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.EVOKER_HURT;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        LivingEntity target = this.getTarget();
        removeOrConvertNearbyVex(level, target);
        purgeSurplusVanillaVex(level);
        if (target == null || !target.isAlive()) {
            return;
        }

        if (this.specialCooldown > 0) {
            this.specialCooldown--;
        }
        if (this.antiBurstCooldown > 0) {
            this.antiBurstCooldown--;
        }
        if (this.beamAttackCooldown > 0) {
            this.beamAttackCooldown--;
        }
        if (this.fangRainAttackCooldown > 0) {
            this.fangRainAttackCooldown--;
        }
        this.recentDamageWindow = Math.max(0.0F, this.recentDamageWindow - 0.06F);

        if (this.fangRainTicks > 0) {
            this.fangRainTicks--;
            if (this.tickCount % 6 == 0) {
                castFangRain(level, target);
            }
        }

        if (this.beamTicks > 0) {
            this.beamTicks--;
            castHexBeam(level, target);
        }
        if (this.panicRetreatTicks > 0) {
            this.panicRetreatTicks--;
            runRetreatBehavior(target);
            if (this.tickCount % 10 == 0) {
                castHexBeam(level, target);
            }
        }
        if (this.tickCount % 40 == 0) {
            // Gives the boss-like feel requested and prevents burst deletion in later waves.
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 70, 0, false, false));
        }

        if (this.isCastingSpell() || this.specialCooldown > 0 || this.random.nextFloat() > 0.18F) {
            return;
        }
        chooseAndCastSpecial(level, target);
    }

    private void castLongFangs(ServerLevel level, LivingEntity target) {
        for (int burst = 0; burst < 3; burst++) {
            float yaw = (float) Math.atan2(target.getZ() - this.getZ(), target.getX() - this.getX());
            for (int i = 1; i <= 12; i++) {
                double d = i * 1.25D;
                double x = this.getX() + Mth.cos(yaw) * d;
                double z = this.getZ() + Mth.sin(yaw) * d;
                double y = findGroundY(level, x, z, this.getY());
                int warmup = i + burst * 5;
                level.addFreshEntity(new EvokerFangs(level, x, y, z, yaw, warmup, this));
            }
        }
        emitUndeadParticles(level, this.position(), 42, 0.9D);
        level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1.1F, 0.85F);
    }

    private void castFangShield(ServerLevel level) {
        for (int ring = 0; ring < 2; ring++) {
            double radius = 2.3D + ring * 1.6D;
            int points = 12 + ring * 6;
            for (int i = 0; i < points; i++) {
                float angle = (float) (i * (Math.PI * 2.0D / points));
                double x = this.getX() + Math.cos(angle) * radius;
                double z = this.getZ() + Math.sin(angle) * radius;
                double y = findGroundY(level, x, z, this.getY());
                level.addFreshEntity(new EvokerFangs(level, x, y, z, angle, 8 + ring * 2, this));
            }
        }
        emitUndeadParticles(level, this.position(), 35, 0.7D);
        level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 0.95F, 1.0F);
    }

    private void startFangRain(ServerLevel level) {
        this.fangRainTicks = 200 + this.random.nextInt(101);
        emitUndeadParticles(level, this.position(), 48, 1.0D);
        level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.0F, 0.7F);
    }

    private void castFangRain(ServerLevel level, LivingEntity target) {
        for (int i = 0; i < 4; i++) {
            double x = target.getX() + (this.random.nextDouble() - 0.5D) * 8.0D;
            double z = target.getZ() + (this.random.nextDouble() - 0.5D) * 8.0D;
            double y = findGroundY(level, x, z, target.getY());
            float rot = this.random.nextFloat() * ((float) Math.PI * 2F);
            level.addFreshEntity(new EvokerFangs(level, x, y, z, rot, this.random.nextInt(4), this));
        }
        emitUndeadParticles(level, target.position(), 8, 0.3D);
    }

    private void summonVlinxPack(ServerLevel level, LivingEntity target) {
        int room = MAX_VLINX_PER_ILLAGER - countOwnedVlinx(level);
        if (room <= 0) {
            return;
        }
        int toSpawn = Math.min(3, room);
        for (int i = 0; i < toSpawn; i++) {
            Vlinx vlinx = ModEntities.VLINX.get().create(level);
            if (vlinx == null) {
                continue;
            }
            double x = this.getX() + (this.random.nextDouble() - 0.5D) * 5.0D;
            double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 5.0D;
            double y = findGroundY(level, x, z, this.getY());
            vlinx.moveTo(x, y + 0.8D, z, this.random.nextFloat() * 360.0F, 0.0F);
            vlinx.finalizeSpawn(level, level.getCurrentDifficultyAt(vlinx.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            vlinx.setOwner(this);
            vlinx.setTarget(target);
            vlinx.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
            vlinx.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.DIAMOND_SWORD));
            vlinx.getPersistentData().putUUID(TAG_VLINX_OWNER, this.getUUID());
            vlinx.setBoundOrigin(this.blockPosition());
            vlinx.setLimitedLife(20 * (35 + this.random.nextInt(35)));
            vlinx.setPersistenceRequired();
            level.addFreshEntity(vlinx);
        }
        emitUndeadParticles(level, this.position(), 52, 1.2D);
        level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.1F, 0.9F);
    }

    private int countOwnedVlinx(ServerLevel level) {
        int n = 0;
        for (Vlinx v : level.getEntitiesOfClass(Vlinx.class, this.getBoundingBox().inflate(48.0D))) {
            if (!v.isAlive()) {
                continue;
            }
            if (v.getOwner() == this) {
                n++;
            } else if (v.getPersistentData().hasUUID(TAG_VLINX_OWNER)
                    && this.getUUID().equals(v.getPersistentData().getUUID(TAG_VLINX_OWNER))) {
                n++;
            }
        }
        return n;
    }

    private void castDebuffCircle(ServerLevel level) {
        AABB area = this.getBoundingBox().inflate(7.5D, 2.5D, 7.5D);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!canHitWithMagic(entity)) {
                continue;
            }
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
        }
        emitUndeadParticles(level, this.position(), 64, 1.5D);
        level.playSound(null, this.blockPosition(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.HOSTILE, 0.9F, 0.8F);
    }

    private void summonSkeletonVillagers(ServerLevel level, LivingEntity target) {
        for (int i = 0; i < 3; i++) {
            SkeletonVillager sv = ModEntities.SKELETON_VILLAGER.get().create(level);
            if (sv == null) {
                continue;
            }
            double x = target.getX() + (this.random.nextDouble() - 0.5D) * 6.0D;
            double z = target.getZ() + (this.random.nextDouble() - 0.5D) * 6.0D;
            double y = findGroundY(level, x, z, target.getY());
            sv.moveTo(x, y, z, this.random.nextFloat() * 360.0F, 0.0F);
            sv.setTarget(target);
            if (this.random.nextFloat() < 0.35F) {
                sv.setSoulVariant(true);
            }
            level.addFreshEntity(sv);
            emitUndeadParticles(level, new Vec3(x, y + 0.6D, z), 14, 0.35D);
        }
        level.playSound(null, this.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 0.75F, 0.7F);
    }

    private void startHexBeam(ServerLevel level) {
        this.beamTicks = 50;
        emitUndeadParticles(level, this.position(), 24, 0.6D);
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 0.85F, 1.15F);
    }

    private void castHexBeam(ServerLevel level, LivingEntity target) {
        Vec3 from = this.getEyePosition();
        Vec3 to = target.getEyePosition();
        Vec3 dir = to.subtract(from);
        double len = Math.max(1.0D, dir.length());
        Vec3 step = dir.scale(1.0D / len);
        for (double d = 0; d < len; d += 0.6D) {
            Vec3 p = from.add(step.scale(d));
            level.sendParticles(ModParticles.UNDEAD_INVASION.get(), p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        if (this.beamTicks % 10 == 0) {
            target.hurt(level.damageSources().magic(), 4.0F);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
        }
    }

    private void castSoulBombard(ServerLevel level, LivingEntity target) {
        Vec3 center = target.position();
        for (int i = 0; i < 5; i++) {
            double x = center.x + (this.random.nextDouble() - 0.5D) * 8.0D;
            double z = center.z + (this.random.nextDouble() - 0.5D) * 8.0D;
            double y = findGroundY(level, x, z, center.y);
            level.sendParticles(ModParticles.UNDEAD_INVASION.get(), x, y + 1.0D, z, 28, 0.25D, 0.55D, 0.25D, 0.03D);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(x - 1.8D, y - 0.4D, z - 1.8D, x + 1.8D, y + 2.6D, z + 1.8D))) {
                if (!canHitWithMagic(entity)) {
                    continue;
                }
                entity.hurt(level.damageSources().magic(), 6.0F);
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
            }
        }
        level.playSound(null, this.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 0.8F, 0.75F);
    }

    private void castNecroticWave(ServerLevel level, LivingEntity target) {
        AABB area = this.getBoundingBox().inflate(9.0D, 3.0D, 9.0D);
        level.sendParticles(ModParticles.UNDEAD_INVASION.get(), this.getX(), this.getY() + 0.8D, this.getZ(), 78, 1.35D, 0.7D, 1.35D, 0.01D);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!canHitWithMagic(entity)) {
                continue;
            }
            Vec3 push = entity.position().subtract(this.position()).normalize().scale(0.75D);
            entity.push(push.x, 0.16D, push.z);
            entity.hurt(level.damageSources().magic(), 5.5F);
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, 1));
        }
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.55F, 1.15F);
    }

    private void castMirrorEscape(ServerLevel level) {
        this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0, false, true));
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, false, false));
        this.panicRetreatTicks = Math.max(this.panicRetreatTicks, 45);
        emitUndeadParticles(level, this.position(), 36, 0.85D);
        for (int i = 0; i < 3; i++) {
            double x = this.getX() + (this.random.nextDouble() - 0.5D) * 4.0D;
            double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 4.0D;
            level.sendParticles(ModParticles.UNDEAD_INVASION.get(), x, this.getY() + 1.0D, z, 10, 0.15D, 0.4D, 0.15D, 0.01D);
        }
        level.playSound(null, this.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 1.0F, 0.75F);
    }

    private static double findGroundY(ServerLevel level, double x, double z, double hintY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(hintY), Mth.floor(z));
        int min = level.getMinBuildHeight();
        int max = level.getMaxBuildHeight() - 1;
        pos.setY(Mth.clamp(pos.getY(), min + 1, max));
        while (pos.getY() > min + 1 && level.getBlockState(pos.below()).isAir()) {
            pos.move(0, -1, 0);
        }
        while (pos.getY() < max && !level.getBlockState(pos).isAir()) {
            pos.move(0, 1, 0);
        }
        return pos.getY();
    }

    private static void emitUndeadParticles(ServerLevel level, Vec3 origin, int count, double spread) {
        level.sendParticles(ModParticles.UNDEAD_INVASION.get(), origin.x, origin.y + 0.9D, origin.z, count, spread, 0.8D, spread, 0.02D);
    }

    private void chooseAndCastSpecial(ServerLevel level, LivingEntity target) {
        double d2 = this.distanceToSqr(target);
        if (d2 < 16.0D) {
            // Close range pressure and punish melee.
            int pick = this.random.nextInt(100);
            if (pick < 28) {
                castFangShield(level);
            } else if (pick < 52) {
                castNecroticWave(level, target);
            } else if (pick < 72) {
                castLongFangs(level, target);
            } else if (pick < 86) {
                castDebuffCircle(level);
            } else {
                castMirrorEscape(level);
            }
            this.specialCooldown = 52 + this.random.nextInt(55);
            return;
        }
        if (d2 > 110.0D || this.panicRetreatTicks > 0) {
            // Long range pressure while repositioning.
            int pick = this.random.nextInt(100);
            if (pick < 18 && this.beamAttackCooldown <= 0) {
                startHexBeam(level);
                this.beamAttackCooldown = 140 + this.random.nextInt(70);
            } else if (pick < 60) {
                castSoulBombard(level, target);
            } else if (pick < 70 && this.fangRainAttackCooldown <= 0) {
                startFangRain(level);
                this.fangRainAttackCooldown = 220 + this.random.nextInt(100);
            } else if (pick < 86) {
                summonVlinxPack(level, target);
            } else {
                summonSkeletonVillagers(level, target);
            }
            this.specialCooldown = 46 + this.random.nextInt(50);
            return;
        }
        // Mid range: diverse full kit.
        int pick = this.random.nextInt(10);
        switch (pick) {
            case 0 -> castLongFangs(level, target);
            case 1 -> castFangShield(level);
            case 2 -> {
                if (this.fangRainAttackCooldown <= 0) {
                    startFangRain(level);
                    this.fangRainAttackCooldown = 230 + this.random.nextInt(120);
                } else {
                    castSoulBombard(level, target);
                }
            }
            case 3 -> summonVlinxPack(level, target);
            case 4 -> castDebuffCircle(level);
            case 5 -> summonSkeletonVillagers(level, target);
            case 6 -> {
                if (this.beamAttackCooldown <= 0) {
                    startHexBeam(level);
                    this.beamAttackCooldown = 150 + this.random.nextInt(90);
                } else {
                    castLongFangs(level, target);
                }
            }
            case 7 -> castSoulBombard(level, target);
            case 8 -> castNecroticWave(level, target);
            default -> castMirrorEscape(level);
        }
        this.specialCooldown = 58 + this.random.nextInt(75);
    }

    private void runRetreatBehavior(LivingEntity target) {
        Vec3 away = this.position().subtract(target.position());
        if (away.lengthSqr() < 1.0E-4D) {
            away = new Vec3(this.random.nextDouble() - 0.5D, 0.0D, this.random.nextDouble() - 0.5D);
        }
        away = away.normalize().scale(RETREAT_NAVIGATION_DISTANCE);
        Vec3 dest = this.position().add(away.x, 0.0D, away.z);
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 2, false, false));
        this.getNavigation().moveTo(dest.x, this.getY(), dest.z, 1.35D);
    }

    private void castAntiBurstRepulse(ServerLevel level) {
        AABB area = this.getBoundingBox().inflate(6.0D, 2.5D, 6.0D);
        level.sendParticles(ModParticles.UNDEAD_INVASION.get(), this.getX(), this.getY() + 1.0D, this.getZ(), 84, 1.15D, 0.7D, 1.15D, 0.02D);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!canHitWithMagic(entity)) {
                continue;
            }
            Vec3 push = entity.position().subtract(this.position()).normalize().scale(1.1D);
            entity.push(push.x, 0.34D, push.z);
            entity.hurt(level.damageSources().magic(), 4.0F);
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 55, 1));
        }
        this.panicRetreatTicks = Math.max(this.panicRetreatTicks, 50);
        this.specialCooldown = Math.max(this.specialCooldown, 35);
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.75F, 1.25F);
    }

    private void removeOrConvertNearbyVex(ServerLevel level, @Nullable LivingEntity target) {
        int owned = countOwnedVlinx(level);
        LivingEntity chaseTarget = target != null && target.isAlive() ? target : this.getTarget();
        for (Vex vex : level.getEntitiesOfClass(Vex.class, this.getBoundingBox().inflate(10.0D))) {
            if (vex instanceof Vlinx) {
                continue;
            }
            if (vex.getOwner() != this) {
                continue;
            }
            if (owned >= MAX_VLINX_PER_ILLAGER) {
                vex.discard();
                continue;
            }
            Vlinx converted = ModEntities.VLINX.get().create(level);
            if (converted == null) {
                vex.discard();
                continue;
            }
            converted.moveTo(vex.getX(), vex.getY(), vex.getZ(), vex.getYRot(), vex.getXRot());
            converted.finalizeSpawn(level, level.getCurrentDifficultyAt(converted.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            converted.setOwner(this);
            converted.setTarget(chaseTarget != null && chaseTarget.isAlive() ? chaseTarget : null);
            converted.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
            converted.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.DIAMOND_SWORD));
            converted.getPersistentData().putUUID(TAG_VLINX_OWNER, this.getUUID());
            level.addFreshEntity(converted);
            emitUndeadParticles(level, vex.position(), 10, 0.25D);
            vex.discard();
            owned++;
        }
    }

    /**
     * Le sort vanilla « Vex » continue de s’exécuter : on supprime l’excédent au lieu d’empiler des conversions.
     */
    private void purgeSurplusVanillaVex(ServerLevel level) {
        List<Vex> mine = new ArrayList<>();
        for (Vex vex : level.getEntitiesOfClass(Vex.class, this.getBoundingBox().inflate(14.0D))) {
            if (vex instanceof Vlinx) {
                continue;
            }
            if (vex.getOwner() == this) {
                mine.add(vex);
            }
        }
        int total = countOwnedVlinx(level) + mine.size();
        while (total > MAX_VLINX_PER_ILLAGER && !mine.isEmpty()) {
            mine.remove(mine.size() - 1).discard();
            total--;
        }
    }

    private boolean canHitWithMagic(LivingEntity entity) {
        if (entity == this || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof UndeadIllager || entity instanceof Vlinx || entity instanceof SkeletonVillager) {
            return false;
        }
        if (entity instanceof net.minecraft.server.level.ServerPlayer || entity instanceof net.minecraft.world.entity.player.Player) {
            return true;
        }
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            return mob.getTarget() == this
                    || mob.getLastHurtByMob() == this
                    || this.getLastHurtByMob() == mob;
        }
        // Passive/non-mob entities are ignored to avoid accidental collateral damage.
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (this.random.nextFloat() < 0.55F) {
            this.spawnAtLocation(new ItemStack(this.random.nextBoolean() ? Items.TOTEM_OF_UNDYING : ModItems.UNDEAD_TOTEM.get()));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean ok = super.hurt(source, amount);
        if (!ok || this.level().isClientSide) {
            return ok;
        }
        this.recentDamageWindow += Math.max(0.0F, amount);
        this.lastDamageTick = this.tickCount;
        if (this.antiBurstCooldown <= 0 && this.recentDamageWindow >= 16.0F && this.tickCount - this.lastDamageTick <= 35) {
            castAntiBurstRepulse((ServerLevel) this.level());
            this.antiBurstCooldown = 85;
            this.recentDamageWindow = 0.0F;
        }
        return true;
    }

}
