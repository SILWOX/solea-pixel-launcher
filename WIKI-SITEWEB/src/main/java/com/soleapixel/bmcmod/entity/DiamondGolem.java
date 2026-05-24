package com.soleapixel.bmcmod.entity;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class DiamondGolem extends IronGolem {
    /** Golem de fer : 100 PV ; golem de diamant : le double. */
    private static final double MAX_HEALTH = 200.0D;

    private static final String TAG_OWNER = "CompanionOwner";
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(DiamondGolem.class,
            EntityDataSerializers.OPTIONAL_UUID);

    public DiamondGolem(EntityType<? extends IronGolem> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, Optional.empty());
    }

    public boolean isCompanion() {
        return entityData.get(DATA_OWNER).isPresent();
    }

    public Optional<UUID> getOwnerUUID() {
        return entityData.get(DATA_OWNER);
    }

    public Optional<Player> getOwner() {
        return getOwnerUUID().flatMap(uuid -> Optional.ofNullable(level().getPlayerByUUID(uuid)));
    }

    public void setOwner(Player player) {
        entityData.set(DATA_OWNER, Optional.of(player.getUUID()));
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(3, new DiamondGolemFollowOwnerGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level().isClientSide() && stack.is(Items.DIAMOND) && !isCompanion()) {
            setOwner(player);
            stack.consume(1, player);
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        getOwnerUUID().ifPresent(uuid -> tag.putUUID(TAG_OWNER, uuid));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID(TAG_OWNER)) {
            entityData.set(DATA_OWNER, Optional.of(tag.getUUID(TAG_OWNER)));
        } else {
            entityData.set(DATA_OWNER, Optional.empty());
        }
    }
}
