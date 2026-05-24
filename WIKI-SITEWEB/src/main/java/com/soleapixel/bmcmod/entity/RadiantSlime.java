package com.soleapixel.bmcmod.entity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.registry.ModFluids;

import net.neoforged.neoforge.event.EventHooks;

/**
 * Slime lié au liquide d’expérience : plus résistant, vole de l’XP au joueur, et les spawns
 * “nuit / lac” retournent au lac au lever du jour (sauf spawns par surcharge d’objets).
 */
public class RadiantSlime extends Slime {
    /**
     * {@link Slime#finalizeSpawn} impose une taille {@code 1 << n} puis appelle {@link Mob#finalizeSpawn}.
     * On saute Slime et on invoque la {@code finalizeSpawn} d’une superclasse via {@code invokespecial}
     * ({@link MethodHandles.Lookup#findSpecial}) : un simple {@link java.lang.reflect.Method#invoke} sur {@link Mob}
     * ferait encore le dispatch vers {@link RadiantSlime#finalizeSpawn} → récursion infinie.
     * <p>
     * Le {@link MethodType} passé à {@code findSpecial} ne doit <strong>pas</strong> inclure le receveur ; seuls les
     * paramètres réels de la méthode (comme dans l’exemple officiel {@code ArrayList.toString} / {@code Listie}).
     */
    @Nullable
    private static final MethodHandle MOB_FINALIZE_SPAWN = resolveMobFinalizeSpawnHandle();

    @Nullable
    private static MethodHandle resolveMobFinalizeSpawnHandle() {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType argsOnly = MethodType.methodType(
                SpawnGroupData.class,
                ServerLevelAccessor.class,
                DifficultyInstance.class,
                MobSpawnType.class,
                SpawnGroupData.class);
        for (Class<?> refc = Slime.class.getSuperclass(); refc != null && refc != Object.class; refc = refc.getSuperclass()) {
            try {
                MethodHandles.Lookup priv = MethodHandles.privateLookupIn(refc, lookup);
                return priv.findSpecial(refc, "finalizeSpawn", argsOnly, RadiantSlime.class);
            } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException ignored) {
            }
        }
        BmcMod.LOGGER.warn("RadiantSlime: findSpecial finalizeSpawn introuvable, repli sur Slime.finalizeSpawn");
        return null;
    }

    private static final String TAG_LAKE_RETURN = "BmcLakeReturn";
    private static final String TAG_LAKE_X = "BmcLakeX";
    private static final String TAG_LAKE_Y = "BmcLakeY";
    private static final String TAG_LAKE_Z = "BmcLakeZ";

    private boolean lakeReturnBehavior;
    @Nullable
    private BlockPos lakeAnchor;
    private int lakeSeekTicks;

    public RadiantSlime(EntityType<? extends Slime> type, Level level) {
        super(type, level);
    }

    /**
     * Ne pas appeler {@link Slime#finalizeSpawn} : il remet une taille puissance de 2 incompatible avec nos tailles
     * 1–3 et peut laisser l’entité dans un état incohérent. On fixe la taille puis {@link Mob#finalizeSpawn} uniquement.
     */
    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        RandomSource rng = level.getRandom();
        int sizeBefore = this.getSize();
        switch (spawnType) {
            case SPAWN_EGG, DISPENSER, COMMAND, SPAWNER -> this.setSize(randomRadiantSpawnSize(rng), true);
            case NATURAL -> this.setSize(randomRadiantSpawnSize(rng), true);
            case MOB_SUMMONED -> this.setSize(sizeBefore, true);
            default -> {
                return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
            }
        }
        return invokeMobFinalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    /**
     * Tailles plus petites que le slime vanilla en moyenne ; taille 4 (= max naturel vanilla) rare.
     * (Vanilla surface utilise 1, 2, 4 via puissances de 2 ; ici 1–4 avec pondération sur 1–2.)
     */
    public static int randomRadiantSpawnSize(RandomSource rng) {
        float r = rng.nextFloat();
        if (r < 0.58F) {
            return 1;
        }
        if (r < 0.88F) {
            return 2;
        }
        if (r < 0.97F) {
            return 3;
        }
        return 4;
    }

    @Nullable
    private SpawnGroupData invokeMobFinalizeSpawn(
            ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        MethodHandle handle = MOB_FINALIZE_SPAWN;
        if (handle == null) {
            return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        }
        try {
            return (SpawnGroupData) handle.invoke(this, level, difficulty, spawnType, spawnGroupData);
        } catch (Throwable t) {
            BmcMod.LOGGER.error("RadiantSlime: finalizeSpawn (super via findSpecial) a échoué, repli sur Slime", t);
            return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        }
    }

    /** Appelé au spawn (naturel ou surcharge). */
    public void configureSpawn(boolean returnToLakeAtDaybreak, @Nullable BlockPos lakeSurface) {
        this.lakeReturnBehavior = returnToLakeAtDaybreak;
        this.lakeAnchor = lakeSurface == null ? null : lakeSurface.immutable();
        this.lakeSeekTicks = 0;
    }

    public void inheritLakeBehavior(RadiantSlime parent) {
        this.lakeReturnBehavior = parent.lakeReturnBehavior;
        this.lakeAnchor = parent.lakeAnchor == null ? null : parent.lakeAnchor.immutable();
        this.lakeSeekTicks = 0;
    }

    public boolean hasLakeReturnBehavior() {
        return this.lakeReturnBehavior;
    }

    @Override
    public void setSize(int size, boolean resetHealth) {
        super.setSize(size, resetHealth);
        int s = this.getSize();
        double base = (double) (s * s);
        double boosted = base * (1.72 + 0.22 * (double) Math.min(s, 4));
        if (this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) != null) {
            this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(boosted);
        }
        if (resetHealth) {
            this.setHealth(this.getMaxHealth());
        } else {
            this.setHealth(Math.min(this.getHealth(), this.getMaxHealth()));
        }
    }

    /** Particules au contact du sol (rebond) : effet type XP au lieu du slime vert. */
    @Override
    protected ParticleOptions getParticleType() {
        return ParticleTypes.ENCHANT;
    }

    /**
     * À la mort, le slime radieux se divise toujours en <strong>5</strong> petits (vanilla : 2–4).
     * On neutralise ensuite la taille pour que {@link Slime#remove} ne duplique pas le split.
     */
    @Override
    public void remove(Entity.RemovalReason reason) {
        int i = this.getSize();
        if (!this.level().isClientSide() && i > 1 && this.isDeadOrDying()) {
            Component customName = this.getCustomName();
            boolean noAi = this.isNoAi();
            float w = this.getDimensions(this.getPose()).width();
            float half = w / 2.0F;
            int childSize = i / 2;
            ArrayList<Mob> children = new ArrayList<>(5);
            for (int l = 0; l < 5; l++) {
                float ang = (float) (l * (Math.PI * 2.0) / 5.0);
                float ox = Mth.cos(ang) * half * 0.55F;
                float oz = Mth.sin(ang) * half * 0.55F;
                Slime slime = this.getType().create(this.level());
                if (slime != null) {
                    if (this.isPersistenceRequired()) {
                        slime.setPersistenceRequired();
                    }
                    slime.setCustomName(customName);
                    slime.setNoAi(noAi);
                    slime.setInvulnerable(this.isInvulnerable());
                    slime.setSize(childSize, true);
                    slime.moveTo(this.getX() + ox, this.getY() + 0.5, this.getZ() + oz, this.random.nextFloat() * 360.0F, 0.0F);
                    children.add(slime);
                }
            }
            if (!EventHooks.onMobSplit(this, children).isCanceled()) {
                children.forEach(this.level()::addFreshEntity);
            }
        }
        if (i > 1) {
            this.setSize(1, false);
        }
        super.remove(reason);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (!this.onGround() && this.getDeltaMovement().y > 0.08 && this.random.nextInt(4) == 0) {
                double py = this.getY() + 0.25 * (double) this.getSize();
                this.level().addParticle(ParticleTypes.ENCHANT, this.getX(), py, this.getZ(), 0.0, 0.0, 0.0);
            }
            return;
        }
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (!this.lakeReturnBehavior || this.lakeAnchor == null) {
            return;
        }
        if (!server.isDay()) {
            this.lakeSeekTicks = 0;
            return;
        }
        this.lakeSeekTicks++;
        if (this.lakeSeekTicks > 4800) {
            this.discard();
            return;
        }
        if (this.tickCount % 12 != 0) {
            return;
        }
        Vec3 target = Vec3.atCenterOf(this.lakeAnchor);
        Vec3 self = this.position();
        Vec3 to = target.subtract(self);
        double len = to.horizontalDistance() + Math.abs(to.y) * 0.35;
        if (len < 2.2 && this.feetInExperienceLiquid()) {
            this.discard();
            return;
        }
        Vec3 push = to.normalize().scale(0.22 + this.random.nextDouble() * 0.08);
        this.setDeltaMovement(this.getDeltaMovement().add(push.x, Math.min(0.42, push.y + 0.18), push.z));
        this.hurtMarked = true;
    }

    @Override
    protected void dealDamage(LivingEntity target) {
        if (this.isAlive() && this.isWithinMeleeAttackRange(target) && this.hasLineOfSight(target)) {
            var source = this.damageSources().mobAttack(this);
            if (target.hurt(source, this.getAttackDamage())) {
                this.playSound(
                        net.minecraft.sounds.SoundEvents.SLIME_ATTACK,
                        1.0F,
                        (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                if (this.level() instanceof ServerLevel server) {
                    net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(server, target, source);
                }
                if (target instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator()) {
                    int drain = xpDrainForSize();
                    if (drain > 0) {
                        player.giveExperiencePoints(-drain);
                    }
                }
            }
        }
    }

    /**
     * Points d’XP retirés au joueur par coup réussi : un peu plus qu’avant, et la montée avec {@link #getSize()}
     * est plus marquée (gros slimes = plus de vol).
     */
    private int xpDrainForSize() {
        int s = this.getSize();
        int base = 2 + s * 4;
        int spread = 4 + s * 4;
        return base + this.random.nextInt(spread);
    }

    private boolean feetInExperienceLiquid() {
        var fs = this.level().getFluidState(this.blockPosition());
        return fs.getType() == ModFluids.EXPERIENCE_STILL.get() || fs.getType() == ModFluids.EXPERIENCE_FLOWING.get();
    }

    /**
     * Vanilla : les slimes de taille 1 ne frappent pas ({@code isTiny()}). Les Radiant Slimes, même minuscules,
     * doivent infliger des dégâts / voler de l’XP au contact.
     */
    @Override
    protected boolean isDealsDamage() {
        return this.isEffectiveAi();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_LAKE_RETURN, this.lakeReturnBehavior);
        if (this.lakeAnchor != null) {
            tag.putInt(TAG_LAKE_X, this.lakeAnchor.getX());
            tag.putInt(TAG_LAKE_Y, this.lakeAnchor.getY());
            tag.putInt(TAG_LAKE_Z, this.lakeAnchor.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.lakeReturnBehavior = tag.getBoolean(TAG_LAKE_RETURN);
        if (tag.contains(TAG_LAKE_X)) {
            this.lakeAnchor = new BlockPos(tag.getInt(TAG_LAKE_X), tag.getInt(TAG_LAKE_Y), tag.getInt(TAG_LAKE_Z));
        } else {
            this.lakeAnchor = null;
        }
    }
}
