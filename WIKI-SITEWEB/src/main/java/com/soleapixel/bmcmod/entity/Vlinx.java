package com.soleapixel.bmcmod.entity;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class Vlinx extends Vex {
    public Vlinx(EntityType<? extends Vlinx> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Vex.createAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            if (this.getBoundOrigin() == null) {
                this.setBoundOrigin(this.blockPosition());
            }
            if (this.getTarget() == null && this.getOwner() != null && this.getOwner().getTarget() != null) {
                this.setTarget(this.getOwner().getTarget());
            }
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.DIAMOND_SWORD));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.01F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        this.setBoundOrigin(this.blockPosition());
        return data;
    }
}
