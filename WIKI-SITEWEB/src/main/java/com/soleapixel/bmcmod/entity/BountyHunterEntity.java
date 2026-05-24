package com.soleapixel.bmcmod.entity;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Cible du contrat « Bounty Hunter » : zombie renforcé, équipe rouge (scoreboard), agresse le porteur du contrat.
 */
public class BountyHunterEntity extends Zombie {
    public static final String TEAM_BOUNTY_GLOW = "bmcmod_bounty_hunter_glow";
    private static final EntityDataAccessor<String> DATA_CONTRACT_OWNER = SynchedEntityData.defineId(
            BountyHunterEntity.class,
            EntityDataSerializers.STRING);

    public BountyHunterEntity(EntityType<? extends BountyHunterEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.45D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CONTRACT_OWNER, "");
    }

    public void setContractOwner(UUID owner) {
        this.entityData.set(DATA_CONTRACT_OWNER, owner.toString());
    }

    @Nullable
    public UUID getContractOwner() {
        String s = this.entityData.get(DATA_CONTRACT_OWNER);
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel sl)) {
            return;
        }
        UUID owner = getContractOwner();
        if (owner != null && this.getTarget() == null) {
            Player p = sl.getPlayerByUUID(owner);
            if (p != null && p.isAlive() && !p.isCreative() && !p.isSpectator()) {
                this.setTarget(p);
            }
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.setBaby(false);
        this.setCanPickUpLoot(false);
        this.setCustomName(net.minecraft.network.chat.Component.literal("Bounty Hunter"));
        this.setCustomNameVisible(true);
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, MobEffectInstance.INFINITE_DURATION, 1, false, false));
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        if (level.getLevel() instanceof ServerLevel sl) {
            applyGlowTeam(sl);
        }
        return data;
    }

    private void applyGlowTeam(ServerLevel level) {
        var board = level.getScoreboard();
        PlayerTeam team = board.getPlayerTeam(TEAM_BOUNTY_GLOW);
        if (team == null) {
            team = board.addPlayerTeam(TEAM_BOUNTY_GLOW);
        }
        team.setColor(ChatFormatting.RED);
        board.addPlayerToTeam(this.getStringUUID(), team);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        String s = this.entityData.get(DATA_CONTRACT_OWNER);
        if (s != null && !s.isEmpty()) {
            tag.putString("BountyContractOwner", s);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BountyContractOwner")) {
            this.entityData.set(DATA_CONTRACT_OWNER, tag.getString("BountyContractOwner"));
        }
    }
}
