package com.soleapixel.bmcmod.entity;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import com.soleapixel.bmcmod.entity.ai.clone.CloneDangerEscapeGoal;
import com.soleapixel.bmcmod.menu.CloneInventoryMenu;
import com.soleapixel.bmcmod.registry.ModMenus;

/**
 * Clone invoqué par le totem mort-vivant : copie apparence et équipement du joueur (main gauche : totem d’immortalité),
 * combat les créatures hostiles et aide en mêlée.
 */
public class CloneEntity extends SkeletonVillager {
    public static final int CLONE_INVENTORY_SIZE = 27;
    private static final String TAG_OWNER = "bmcmod_clone_owner";
    private static final String TAG_SLIM = "bmcmod_clone_slim";
    private static final String TAG_ITEMS = "bmcmod_clone_items_v1";
    private static final String TAG_ITEM_PAYLOAD = "Item";

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID = SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_SLIM = SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.BOOLEAN);

    private final SimpleContainer cloneInventory = new SimpleContainer(CLONE_INVENTORY_SIZE);

    public CloneEntity(EntityType<? extends CloneEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_UUID, Optional.empty());
        builder.define(DATA_SLIM, false);
    }

    public SimpleContainer getCloneInventory() {
        return cloneInventory;
    }

    @Override
    protected void registerGoals() {
        RangedAttackGoal rangedGoal = new RangedAttackGoal(this, 1.05D, 20, 18.0F) {
            @Override
            public boolean canUse() {
                ItemStack main = CloneEntity.this.getMainHandItem();
                return (main.is(Items.CROSSBOW) || main.is(Items.TRIDENT) || main.is(Items.BOW)) && super.canUse();
            }
        };
        MeleeAttackGoal meleeGoal = new MeleeAttackGoal(this, 1.2D, true) {
            @Override
            public boolean canUse() {
                ItemStack main = CloneEntity.this.getMainHandItem();
                return !main.is(Items.CROSSBOW) && !main.is(Items.TRIDENT) && !main.is(Items.BOW) && super.canUse();
            }
        };

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new CloneDangerEscapeGoal(this));
        this.goalSelector.addGoal(3, meleeGoal);
        this.goalSelector.addGoal(4, rangedGoal);
        this.goalSelector.addGoal(5, new CloneFollowOwnerGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new CloneFriendlyHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new CloneOwnerAttackerTargetGoal(this));
        this.targetSelector.addGoal(
                3,
                new NearestAttackableTargetGoal<>(this, Monster.class, 14, true, false, liv -> canAttackMonsterBaseline(CloneEntity.this, liv)));
    }

    /** Même joueur propriétaire : clones alliés. */
    public static boolean sameOwner(CloneEntity self, LivingEntity entity) {
        if (!(entity instanceof CloneEntity other)) {
            return false;
        }
        UUID a = self.getOwnerUuid();
        UUID b = other.getOwnerUuid();
        return a != null && b != null && a.equals(b);
    }

    private static boolean canAttackMonsterBaseline(CloneEntity clone, LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (sameOwner(clone, target)) {
            return false;
        }
        Player owner = clone.getOwnerPlayer();
        if (owner != null && target.getUUID().equals(owner.getUUID())) {
            return false;
        }
        return clone.canAttack(target);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (sameOwner(this, target)) {
            return false;
        }
        Player owner = getOwnerPlayer();
        if (owner != null && target.getUUID().equals(owner.getUUID())) {
            return false;
        }
        if (target instanceof CloneEntity vc && owner != null && vc.getOwnerUuid() != null && vc.getOwnerUuid().equals(owner.getUUID())) {
            return false;
        }
        return super.canAttack(target);
    }

    @Override
    public boolean isSunBurnTick() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    public void initializeFromOwner(ServerPlayer owner, boolean slim) {
        setOwnerUuid(owner.getUUID());
        setSlimModel(slim);
        setMimicked(true);
        setMimicPlayerUuid(owner.getUUID());
        setCustomName(owner.getName());
        setCustomNameVisible(false);
        setSoulVariant(true);
        this.setPersistenceRequired();

        if (owner.getAttribute(Attributes.SCALE) != null && this.getAttribute(Attributes.SCALE) != null) {
            this.getAttribute(Attributes.SCALE).setBaseValue(owner.getAttribute(Attributes.SCALE).getBaseValue());
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor() && slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
                continue;
            }
            ItemStack copied = owner.getItemBySlot(slot).copy();
            this.setItemSlot(slot, copied);
            this.setDropChance(slot, 0.0F);
        }
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);

        this.setHealth(this.getMaxHealth());
        this.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 0.8F, 1.15F);
    }

    @Nullable
    public UUID getOwnerUuid() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    public void setOwnerUuid(@Nullable UUID uuid) {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(uuid));
    }

    public boolean isSlimModel() {
        return this.entityData.get(DATA_SLIM);
    }

    public void setSlimModel(boolean slim) {
        this.entityData.set(DATA_SLIM, slim);
    }

    @Nullable
    public Player getOwnerPlayer() {
        UUID ownerId = getOwnerUuid();
        if (ownerId == null || !(this.level() instanceof ServerLevel sl)) {
            return null;
        }
        return sl.getPlayerByUUID(ownerId);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        UUID oid = getOwnerUuid();
        if (oid == null || !player.getUUID().equals(oid)) {
            return InteractionResult.PASS;
        }
        sp.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.bmcmod.clone");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player p) {
                return new CloneInventoryMenu(ModMenus.CLONE_INVENTORY.get(), containerId, inv, CloneEntity.this);
            }
        }, buf -> buf.writeVarInt(getId()));
        return InteractionResult.CONSUME;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID ownerId = getOwnerUuid();
        if (ownerId != null) {
            tag.putUUID(TAG_OWNER, ownerId);
        }
        tag.putBoolean(TAG_SLIM, isSlimModel());
        saveCloneInventory(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setOwnerUuid(tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null);
        setSlimModel(tag.getBoolean(TAG_SLIM));
        loadCloneInventory(tag);
    }

    private void saveCloneInventory(CompoundTag root) {
        net.minecraft.core.HolderLookup.Provider registries = registryAccess();
        ListTag list = new ListTag();
        for (int i = 0; i < cloneInventory.getContainerSize(); i++) {
            ItemStack stack = cloneInventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag wrap = new CompoundTag();
            wrap.putByte("Slot", (byte) i);
            CompoundTag itemPart = new CompoundTag();
            stack.save(registries, itemPart);
            wrap.put(TAG_ITEM_PAYLOAD, itemPart);
            list.add(wrap);
        }
        root.put(TAG_ITEMS, list);
    }

    private void loadCloneInventory(CompoundTag root) {
        net.minecraft.core.HolderLookup.Provider registries = registryAccess();
        for (int i = 0; i < cloneInventory.getContainerSize(); i++) {
            cloneInventory.setItem(i, ItemStack.EMPTY);
        }
        ListTag list = root.getList(TAG_ITEMS, CompoundTag.TAG_COMPOUND);
        for (int j = 0; j < list.size(); ++j) {
            CompoundTag st = list.getCompound(j);
            byte slot = st.getByte("Slot");
            if (slot < 0 || slot >= cloneInventory.getContainerSize()) {
                continue;
            }
            CompoundTag itemPart = st.getCompound(TAG_ITEM_PAYLOAD);
            ItemStack stack = ItemStack.parse(registries, itemPart).orElse(ItemStack.EMPTY);
            cloneInventory.setItem(slot, stack);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PLAYER_BREATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    private static final class CloneFollowOwnerGoal extends Goal {
        private final CloneEntity clone;

        private CloneFollowOwnerGoal(CloneEntity clone) {
            this.clone = clone;
        }

        @Override
        public boolean canUse() {
            Player owner = clone.getOwnerPlayer();
            if (owner == null || !owner.isAlive()) {
                return false;
            }
            boolean far = clone.distanceToSqr(owner) > 25.0D * 25.0D;
            return far && clone.getTarget() == null;
        }

        @Override
        public void tick() {
            Player owner = clone.getOwnerPlayer();
            if (owner == null) {
                return;
            }
            if (clone.distanceToSqr(owner) > 30.0D * 30.0D) {
                clone.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                return;
            }
            clone.getNavigation().moveTo(owner, 1.15D);
        }
    }

    private static final class CloneOwnerAttackerTargetGoal extends Goal {
        private final CloneEntity clone;

        private CloneOwnerAttackerTargetGoal(CloneEntity clone) {
            this.clone = clone;
        }

        @Override
        public boolean canUse() {
            Player owner = clone.getOwnerPlayer();
            if (owner == null) {
                return false;
            }
            LivingEntity attacker = owner.getLastHurtByMob();
            return attacker != null && attacker.isAlive();
        }

        @Override
        public void start() {
            Player owner = clone.getOwnerPlayer();
            if (owner != null) {
                LivingEntity attacker = owner.getLastHurtByMob();
                if (attacker != null && attacker.isAlive() && clone.canAttack(attacker)) {
                    clone.setTarget(attacker);
                }
            }
        }
    }

    private static final class CloneFriendlyHurtByTargetGoal extends HurtByTargetGoal {
        private final CloneEntity clone;

        CloneFriendlyHurtByTargetGoal(CloneEntity clone) {
            super(clone);
            this.clone = clone;
        }

        @Override
        public boolean canUse() {
            if (!super.canUse()) {
                return false;
            }
            LivingEntity attacker = clone.getLastHurtByMob();
            return attacker != null && !sameOwner(clone, attacker);
        }
    }
}
