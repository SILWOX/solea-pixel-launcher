package com.soleapixel.bmcmod.entity;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.registry.ModItems;

public class SkeletonVillager extends Monster implements net.minecraft.world.entity.monster.RangedAttackMob {
    public static final String TAG_SOUL = "bmcmod:soul_variant";
    public static final String TAG_MIMICKED = "bmcmod:soul_mimicked";
    public static final String TAG_MIMIC_UUID = "bmcmod:mimic_player_uuid";
    private static final EntityDataAccessor<Boolean> DATA_SOUL = SynchedEntityData.defineId(SkeletonVillager.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_MIMICKED = SynchedEntityData.defineId(SkeletonVillager.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> DATA_MIMIC_UUID = SynchedEntityData.defineId(SkeletonVillager.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final int WEAPON_CROSSBOW = 0;
    private static final int WEAPON_SWORD = 1;
    private static final int WEAPON_TRIDENT = 2;
    private int crossbowChargeTicks;
    private int crossbowCooldownTicks;
    private boolean crossbowReadyToShoot;

    public SkeletonVillager(EntityType<? extends SkeletonVillager> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SOUL, false);
        builder.define(DATA_MIMICKED, false);
        builder.define(DATA_MIMIC_UUID, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        RangedAttackGoal rangedGoal = new RangedAttackGoal(this, 1.05D, 24, 16.0F) {
            @Override
            public boolean canUse() {
                ItemStack main = SkeletonVillager.this.getMainHandItem();
                return (main.is(Items.CROSSBOW) || main.is(Items.TRIDENT)) && super.canUse();
            }
        };
        MeleeAttackGoal meleeGoal = new MeleeAttackGoal(this, 1.15D, true) {
            @Override
            public boolean canUse() {
                ItemStack main = SkeletonVillager.this.getMainHandItem();
                return !main.is(Items.CROSSBOW) && !main.is(Items.TRIDENT) && super.canUse();
            }
        };
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RestrictSunGoal(this));
        this.goalSelector.addGoal(3, new FleeSunGoal(this, 1.12D));
        this.goalSelector.addGoal(4, rangedGoal);
        this.goalSelector.addGoal(5, meleeGoal);
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByPlayerTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * Vengeance uniquement contre les joueurs — {@link HurtByTargetGoal} vanilla aggro aussi les autres mobs
     * qui infligent des dégâts (collisions, projectiles d’allier, etc.).
     */
    private static final class HurtByPlayerTargetGoal extends HurtByTargetGoal {
        HurtByPlayerTargetGoal(SkeletonVillager mob) {
            super(mob);
        }

        @Override
        public boolean canUse() {
            LivingEntity attacker = this.mob.getLastHurtByMob();
            if (!(attacker instanceof Player)) {
                return false;
            }
            return super.canUse();
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity entity) {
        boolean hit = super.doHurtTarget(entity);
        if (hit && entity instanceof Player player && this.isSoulVariant() && !this.hasMimicked()) {
            copyFromPlayer(player);
            this.level().broadcastEntityEvent(this, (byte) 60);
        }
        return hit;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 60 && this.isSoulVariant()) {
            for (int i = 0; i < 36; i++) {
                double ox = (this.random.nextDouble() - 0.5D) * this.getBbWidth();
                double oy = this.random.nextDouble() * this.getBbHeight();
                double oz = (this.random.nextDouble() - 0.5D) * this.getBbWidth();
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0.0D, 0.01D, 0.0D);
            }
            this.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.1F, 0.85F);
            return;
        }
        super.handleEntityEvent(id);
    }

    private void copyFromPlayer(Player player) {
        this.setMimicked(true);
        this.setMimicPlayerUuid(player.getUUID());
        this.setCustomName(player.getName());
        this.setCustomNameVisible(true);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor() && slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
                continue;
            }
            ItemStack copied = player.getItemBySlot(slot).copy();
            this.setItemSlot(slot, copied);
            this.setDropChance(slot, 0.0F);
        }

        copyAttribute(Attributes.MAX_HEALTH, player);
        copyAttribute(Attributes.MOVEMENT_SPEED, player);
        copyAttribute(Attributes.ATTACK_DAMAGE, player);
        copyAttribute(Attributes.ARMOR, player);
        copyAttribute(Attributes.ARMOR_TOUGHNESS, player);
        this.setHealth(Math.max(1.0F, player.getHealth()));
        this.setTarget(player);
    }

    private void copyAttribute(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, Player player) {
        AttributeInstance selfAttr = this.getAttribute(attribute);
        AttributeInstance playerAttr = player.getAttribute(attribute);
        if (selfAttr == null || playerAttr == null) {
            return;
        }
        double value = playerAttr.getBaseValue();
        if (attribute == Attributes.MOVEMENT_SPEED) {
            // Player base speed is much lower than hostile mobs; keep mimic agile.
            value = Math.max(0.24D, value);
        }
        selfAttr.setBaseValue(value);
    }

    @Override
    public void performRangedAttack(net.minecraft.world.entity.LivingEntity target, float velocity) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack main = this.getMainHandItem();
        if (main.is(Items.TRIDENT)) {
            ThrownTrident trident = new ThrownTrident(level, this, main.copy());
            Vec3 aim = target.getEyePosition().subtract(this.getEyePosition());
            trident.shoot(aim.x, aim.y + aim.horizontalDistance() * 0.08D, aim.z, 1.6F, 6.0F);
            level.addFreshEntity(trident);
            this.playSound(SoundEvents.DROWNED_SHOOT, 1.0F, 0.9F + this.getRandom().nextFloat() * 0.1F);
            return;
        }

        if (main.is(Items.CROSSBOW) && !this.crossbowReadyToShoot) {
            // Keep ranged goal active, but only allow firing after completed charge.
            return;
        }

        // MC 1.21+ : Arrow exige un ItemStack d’arme valide (arc) — ItemStack.EMPTY fait crasher.
        ItemStack bowForArrow = main.is(Items.BOW) ? main : Items.BOW.getDefaultInstance();
        AbstractArrow arrow = main.is(Items.CROSSBOW)
                ? ProjectileUtil.getMobArrow(this, new ItemStack(Items.ARROW), velocity, Items.BOW.getDefaultInstance())
                : new Arrow(level, this, new ItemStack(Items.ARROW), bowForArrow);
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float speed = main.is(Items.CROSSBOW) ? 1.9F : 1.6F;
        // Slightly tighter spread for crossbow users while keeping difficulty scaling.
        float inaccuracy = main.is(Items.CROSSBOW)
                ? (float) (12 - this.level().getDifficulty().getId() * 4)
                : (float) (14 - this.level().getDifficulty().getId() * 4);
        arrow.shoot(dx, dy + horizontal * 0.2D, dz, speed, inaccuracy);
        level.addFreshEntity(arrow);
        this.playSound(main.is(Items.CROSSBOW) ? SoundEvents.CROSSBOW_SHOOT : SoundEvents.SKELETON_SHOOT, 1.0F, 0.95F + this.getRandom().nextFloat() * 0.1F);
        if (main.is(Items.CROSSBOW)) {
            this.crossbowReadyToShoot = false;
            this.crossbowCooldownTicks = 16;
        }
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide) {
            tickDaylightBurnLikeSkeleton();
        }
        if (!this.level().isClientSide) {
            tickCrossbowChargeLikePillager();
        }
        if (!this.level().isClientSide && !this.isSoulVariant() && !this.hasMimicked() && this.tickCount % 40 == 0 && this.getMainHandItem().isEmpty()) {
            equipRandomWeapon(this.getRandom());
        }
        super.aiStep();
    }

    private void tickDaylightBurnLikeSkeleton() {
        if (!this.level().isDay() || !this.isSunBurnTick()) {
            return;
        }
        ItemStack head = this.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.isEmpty() && protectsFromSun(head)) {
            if (head.isDamageableItem()) {
                int added = this.random.nextInt(2);
                head.setDamageValue(head.getDamageValue() + added);
                if (head.getDamageValue() >= head.getMaxDamage()) {
                    this.onEquippedItemBroken(head.getItem(), EquipmentSlot.HEAD);
                    this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                }
            }
            return;
        }
        this.igniteForSeconds(8.0F);
    }

    private static boolean protectsFromSun(ItemStack head) {
        // Avoid invisible cosmetic hats blocking sunlight; only real head protection counts.
        if (head.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == EquipmentSlot.HEAD) {
            return true;
        }
        return head.is(Items.CARVED_PUMPKIN)
                || head.is(Items.JACK_O_LANTERN)
                || head.is(Items.SKELETON_SKULL)
                || head.is(Items.WITHER_SKELETON_SKULL)
                || head.is(Items.ZOMBIE_HEAD)
                || head.is(Items.CREEPER_HEAD)
                || head.is(Items.DRAGON_HEAD)
                || head.is(Items.PIGLIN_HEAD)
                || head.is(Items.PLAYER_HEAD);
    }

    @Override
    protected boolean isSunBurnTick() {
        if (this.isSoulVariant()) {
            return false;
        }
        return super.isSunBurnTick();
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightning) {
        super.thunderHit(level, lightning);
        this.setSoulVariant(true);
        this.setMimicked(false);
        this.setMimicPlayerUuid(null);
        clearSoulDefaultEquipment();
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        if (this.isSoulVariant()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            return;
        }

        int weaponRoll = random.nextInt(100);
        equipRandomWeapon(random, weaponRoll);

        maybeEquipVillagerHat(random);
        if (this.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && random.nextFloat() < 0.22F) {
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        }
        if (random.nextFloat() < 0.16F) {
            this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        }
        if (random.nextFloat() < 0.12F) {
            this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        }
        if (random.nextFloat() < 0.10F) {
            this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));
        }
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        if (spawnType != MobSpawnType.SPAWN_EGG) {
            if (this.getRandom().nextFloat() < 0.1F) {
                this.setSoulVariant(true);
                clearSoulDefaultEquipment();
            }
        }
        // Ensure weapon/armor are visible immediately on first tick after spawn.
        if (!this.isSoulVariant() && this.getMainHandItem().isEmpty()) {
            equipRandomWeapon(this.getRandom());
        }
        if (!this.isSoulVariant()) {
            maybeEquipVillagerHat(this.getRandom());
        }
        if (!this.isSoulVariant() && this.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && this.getRandom().nextFloat() < 0.22F) {
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        }
        if (!this.isSoulVariant() && this.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && this.getRandom().nextFloat() < 0.16F) {
            this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        }
        if (!this.isSoulVariant() && this.getItemBySlot(EquipmentSlot.LEGS).isEmpty() && this.getRandom().nextFloat() < 0.12F) {
            this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        }
        if (!this.isSoulVariant() && this.getItemBySlot(EquipmentSlot.FEET).isEmpty() && this.getRandom().nextFloat() < 0.10F) {
            this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));
        }
        return data;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        ItemStack mainHandBeforeDrop = this.getMainHandItem().copy();
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        if (this.hasMimicked()) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.isArmor() || slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                    this.setItemSlot(slot, ItemStack.EMPTY);
                }
            }
        }
        super.dropCustomDeathLoot(level, source, recentlyHit);

        int bones = this.random.nextInt(3);
        if (bones > 0) {
            this.spawnAtLocation(new ItemStack(Items.BONE, bones));
        }
        int arrows = this.random.nextInt(3);
        if (arrows > 0) {
            this.spawnAtLocation(new ItemStack(Items.ARROW, arrows));
        }
        if ((mainHandBeforeDrop.is(Items.IRON_SWORD) || mainHandBeforeDrop.is(Items.CROSSBOW)) && this.random.nextFloat() < 0.085F) {
            this.spawnAtLocation(mainHandBeforeDrop.copyWithCount(1));
        }

        if (source.getEntity() instanceof Creeper creeper && creeper.canDropMobsSkull()) {
            creeper.increaseDroppedSkulls();
            this.spawnAtLocation(new ItemStack(ModItems.SKELETON_VILLAGER_SKULL_ITEM.get()));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isSoulVariant() ? SoundEvents.SOUL_ESCAPE.value() : SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }

    public boolean canFireProjectileWeapon(net.minecraft.world.item.ProjectileWeaponItem weapon) {
        return weapon instanceof CrossbowItem;
    }

    private void tickCrossbowChargeLikePillager() {
        ItemStack main = this.getMainHandItem();
        if (!main.is(Items.CROSSBOW)) {
            if (this.isUsingItem()) {
                this.stopUsingItem();
            }
            this.crossbowChargeTicks = 0;
            this.crossbowCooldownTicks = 0;
            this.crossbowReadyToShoot = false;
            return;
        }
        if (this.crossbowCooldownTicks > 0) {
            this.crossbowCooldownTicks--;
        }
        var target = this.getTarget();
        if (target == null || !target.isAlive() || !this.hasLineOfSight(target)) {
            if (this.isUsingItem()) {
                this.stopUsingItem();
            }
            this.crossbowChargeTicks = 0;
            return;
        }
        if (this.crossbowReadyToShoot) {
            return;
        }
        if (!this.isUsingItem()) {
            if (this.crossbowCooldownTicks <= 0) {
                this.startUsingItem(InteractionHand.MAIN_HAND);
                this.crossbowChargeTicks = 0;
            }
            return;
        }
        this.crossbowChargeTicks++;
        // Pillager-style visible charge window before firing.
        if (this.crossbowChargeTicks >= 20) {
            this.stopUsingItem();
            this.crossbowChargeTicks = 0;
            this.crossbowReadyToShoot = true;
            this.playSound(SoundEvents.CROSSBOW_LOADING_END.value(), 1.0F, 0.95F + this.getRandom().nextFloat() * 0.1F);
        }
    }

    private void equipRandomWeapon(RandomSource random) {
        equipRandomWeapon(random, random.nextInt(100));
    }

    private void equipRandomWeapon(RandomSource random, int weaponRoll) {
        int weapon = weaponRoll < 72 ? WEAPON_CROSSBOW : (weaponRoll < 95 ? WEAPON_SWORD : WEAPON_TRIDENT);
        switch (weapon) {
            case WEAPON_SWORD -> this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            case WEAPON_TRIDENT -> this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
            default -> this.setItemSlot(EquipmentSlot.MAINHAND, createRandomCrossbow(random));
        }
    }

    private ItemStack createRandomCrossbow(RandomSource random) {
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        if (this.level() == null || this.level().registryAccess() == null) {
            return crossbow;
        }
        ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(crossbow.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        var enchantments = this.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (random.nextFloat() < 0.34F) {
            int level = random.nextFloat() < 0.18F ? 3 : (random.nextBoolean() ? 2 : 1);
            mut.set(enchantments.getOrThrow(Enchantments.QUICK_CHARGE), level);
        }
        if (random.nextFloat() < 0.15F) {
            if (random.nextFloat() < 0.22F) {
                mut.set(enchantments.getOrThrow(Enchantments.MULTISHOT), 1);
            } else {
                int level = random.nextBoolean() ? 2 : 1;
                mut.set(enchantments.getOrThrow(Enchantments.PIERCING), level);
            }
        }
        crossbow.set(DataComponents.ENCHANTMENTS, mut.toImmutable());
        return crossbow;
    }

    private void maybeEquipVillagerHat(RandomSource random) {
        if (!this.getItemBySlot(EquipmentSlot.HEAD).isEmpty() || random.nextFloat() >= 0.06F) {
            return;
        }
        ItemStack hat = switch (random.nextInt(8)) {
            case 0 -> new ItemStack(ModItems.VILLAGER_HAT_BUTCHER.get());
            case 1 -> new ItemStack(ModItems.VILLAGER_HAT_LIBRARIAN.get());
            case 2 -> new ItemStack(ModItems.VILLAGER_HAT_WEAPONSMITH.get());
            case 3 -> new ItemStack(ModItems.VILLAGER_HAT_SHEPHERD.get());
            case 4 -> new ItemStack(ModItems.VILLAGER_HAT_FISHERMAN.get());
            case 5 -> new ItemStack(ModItems.VILLAGER_HAT_CARTOGRAPHER.get());
            case 6 -> new ItemStack(ModItems.VILLAGER_HAT_ARMORER.get());
            default -> new ItemStack(ModItems.VILLAGER_HAT_FARMER.get());
        };
        this.setItemSlot(EquipmentSlot.HEAD, hat);
    }

    private void clearSoulDefaultEquipment() {
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        this.setCanPickUpLoot(false);
    }


    public boolean isSoulVariant() {
        return this.entityData.get(DATA_SOUL);
    }

    public void setSoulVariant(boolean soul) {
        this.entityData.set(DATA_SOUL, soul);
        this.getPersistentData().putBoolean(TAG_SOUL, soul);
    }

    public boolean hasMimicked() {
        return this.entityData.get(DATA_MIMICKED);
    }

    public void setMimicked(boolean mimicked) {
        this.entityData.set(DATA_MIMICKED, mimicked);
        this.getPersistentData().putBoolean(TAG_MIMICKED, mimicked);
    }

    @Nullable
    public UUID getMimicPlayerUuid() {
        return this.entityData.get(DATA_MIMIC_UUID).orElse(null);
    }

    public void setMimicPlayerUuid(@Nullable UUID uuid) {
        this.entityData.set(DATA_MIMIC_UUID, Optional.ofNullable(uuid));
        if (uuid == null) {
            this.getPersistentData().remove(TAG_MIMIC_UUID);
        } else {
            this.getPersistentData().putUUID(TAG_MIMIC_UUID, uuid);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_SOUL, this.isSoulVariant());
        tag.putBoolean(TAG_MIMICKED, this.hasMimicked());
        UUID uuid = this.getMimicPlayerUuid();
        if (uuid != null) {
            tag.putUUID(TAG_MIMIC_UUID, uuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setSoulVariant(tag.getBoolean(TAG_SOUL));
        this.setMimicked(tag.getBoolean(TAG_MIMICKED));
        this.setMimicPlayerUuid(tag.hasUUID(TAG_MIMIC_UUID) ? tag.getUUID(TAG_MIMIC_UUID) : null);
    }
}
