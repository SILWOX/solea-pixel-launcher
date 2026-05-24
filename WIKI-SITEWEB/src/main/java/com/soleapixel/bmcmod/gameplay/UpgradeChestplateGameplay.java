package com.soleapixel.bmcmod.gameplay;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.upgrade.ChestplateUpgradeData;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class UpgradeChestplateGameplay {
    private static final ResourceLocation ARMOR_ID = BmcMod.loc("upgrade_armor_bonus");
    private static final ResourceLocation HEALTH_ID = BmcMod.loc("upgrade_health_bonus");
    private static final ResourceLocation LUCK_ID = BmcMod.loc("upgrade_luck_bonus");
    private static final ResourceLocation RANGE_ID = BmcMod.loc("upgrade_range_bonus");
    private static final ResourceLocation ENTITY_RANGE_ID = BmcMod.loc("upgrade_entity_range_bonus");
    private static final ResourceLocation SPEED_ID = BmcMod.loc("upgrade_speed_bonus");
    private static final ResourceLocation STEP_ID = BmcMod.loc("upgrade_step_bonus");
    private static final ResourceLocation STRENGHT_ID = BmcMod.loc("upgrade_strenght_bonus");
    private static final ResourceLocation FROST_SPEED_ID = BmcMod.loc("upgrade_frost_speed_bonus");
    private static final ResourceLocation SWIM_SPEED_ID = BmcMod.loc("upgrade_swim_speed_bonus");
    private static final String FORCED_SILENT_KEY = "bmcmod_upgrade_forced_silent";
    private static final String DASH_UNLOCK_GAME_TIME_KEY = "bmcmod_dash_upgrade_until";
    /** Pas de nouveau dash avant d’être repassé hors sol après le dernier dash (anti double-dash en cascade). */
    private static final String DASH_AIR_LOCK_KEY = "bmcmod_dash_upgrade_air_lock";
    private static final String DASH_LEFT_GROUND_AFTER_DASH_KEY = "bmcmod_dash_upgrade_was_airborne";
    private static final String DASH_LAST_DASH_GAME_TICK_KEY = "bmcmod_dash_upgrade_last_tick";
    /** Très court délai (ticks) après le retour au sol avant d’autoriser un nouveau dash. */
    private static final int DASH_POST_LAND_DELAY_TICKS = 8;
    /**
     * Si le joueur reste coincé avec un verrou (ex. téléport/exploit), permet de sortir après un délai,
     * sans permettre deux dash consécutifs en plein saut (le verrou saute seulement si déjà au sol).
     */
    private static final int DASH_AIR_LOCK_GROUND_FALLBACK_TICKS = 35;

    private UpgradeChestplateGameplay() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);

        int armorCount = ChestplateUpgradeData.count(chest, ChestplateUpgradeData.ARMOR_UPGRADE_ID);
        int healthCount = ChestplateUpgradeData.count(chest, ChestplateUpgradeData.HEALTH_UPGRADE_ID);
        int luckCount = ChestplateUpgradeData.count(chest, ChestplateUpgradeData.LUCK_UPGRADE_ID);
        int rangeCount = ChestplateUpgradeData.count(chest, ChestplateUpgradeData.RANGE_UPGRADE_ID);
        int speedCount = ChestplateUpgradeData.count(chest, ChestplateUpgradeData.SPEED_UPGRADE_ID);
        int stepCount = ChestplateUpgradeData.count(chest, ChestplateUpgradeData.STEP_UPGRADE_ID);
        int strenghtCount = ChestplateUpgradeData.count(chest, ChestplateUpgradeData.STRENGHT_UPGRADE_ID);
        int swimCount = ChestplateUpgradeData.count(chest, ChestplateUpgradeData.SWIM_UPGRADE_ID);
        boolean hasDiscretion = ChestplateUpgradeData.count(chest, ChestplateUpgradeData.DISCRETION_UPGRADE_ID) > 0;
        boolean hasFrostWalk = ChestplateUpgradeData.count(chest, ChestplateUpgradeData.FROST_WALK_UPGRADE_ID) > 0;

        applyModifier(player.getAttribute(Attributes.ARMOR), ARMOR_ID, armorCount, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, healthCount * 4.0, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player.getAttribute(Attributes.LUCK), LUCK_ID, luckCount * 0.1, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE), RANGE_ID, rangeCount * 0.5, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_RANGE_ID, rangeCount * 0.5, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, speedCount * 0.03, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player.getAttribute(Attributes.STEP_HEIGHT), STEP_ID, stepCount * 0.5, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), STRENGHT_ID, strenghtCount * 1.0, AttributeModifier.Operation.ADD_VALUE);

        boolean onIce = isOnIce(player);
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), FROST_SPEED_ID, hasFrostWalk && onIce ? 0.12 : 0.0, AttributeModifier.Operation.ADD_VALUE);
        boolean inWater = player.isInWaterOrBubble();
        applyModifier(player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY), SWIM_SPEED_ID, inWater ? swimCount * 0.55 : 0.0, AttributeModifier.Operation.ADD_VALUE);
        if (hasFrostWalk) {
            player.setTicksFrozen(0);
            if (player.level().getBlockState(player.blockPosition()).is(Blocks.POWDER_SNOW) && player.getDeltaMovement().y < 0.0) {
                player.setDeltaMovement(player.getDeltaMovement().x, Math.max(0.08, player.getDeltaMovement().y * -0.1), player.getDeltaMovement().z);
            }
        }
        if (swimCount > 0 && inWater) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, true, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20 + 10 * swimCount, 0, true, false, false));
        }

        if (!(player instanceof ServerPlayer)) {
            player.setSilent(hasDiscretion);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (hasDiscretion) {
                if (!serverPlayer.isSilent()) {
                    serverPlayer.setSilent(true);
                    serverPlayer.getPersistentData().putBoolean(FORCED_SILENT_KEY, true);
                }
            } else if (serverPlayer.getPersistentData().getBoolean(FORCED_SILENT_KEY)) {
                serverPlayer.setSilent(false);
                serverPlayer.getPersistentData().remove(FORCED_SILENT_KEY);
            }
            tickDashUpgradeLandLock(serverPlayer, chest);
        }
    }

    private static void tickDashUpgradeLandLock(ServerPlayer player, ItemStack chest) {
        if (player.level().isClientSide()) {
            return;
        }
        if (ChestplateUpgradeData.count(chest, ChestplateUpgradeData.DASH_UPGRADE_ID) <= 0) {
            var pClear = player.getPersistentData();
            pClear.remove(DASH_AIR_LOCK_KEY);
            pClear.remove(DASH_LEFT_GROUND_AFTER_DASH_KEY);
            pClear.remove(DASH_LAST_DASH_GAME_TICK_KEY);
            pClear.remove(DASH_UNLOCK_GAME_TIME_KEY);
            return;
        }
        var pdc = player.getPersistentData();
        if (!pdc.getBoolean(DASH_AIR_LOCK_KEY)) {
            return;
        }
        boolean onGround = player.onGround();
        if (!onGround) {
            pdc.putBoolean(DASH_LEFT_GROUND_AFTER_DASH_KEY, true);
            return;
        }
        boolean wasAirborne = pdc.getBoolean(DASH_LEFT_GROUND_AFTER_DASH_KEY);
        long now = player.level().getGameTime();
        long lastDash = pdc.getLong(DASH_LAST_DASH_GAME_TICK_KEY);
        boolean groundFallbackUnlock = lastDash != 0L && now - lastDash >= DASH_AIR_LOCK_GROUND_FALLBACK_TICKS;
        // Retour au sol après avoir été en l’air depuis le dash, OU timeout si le joueur n’a pas quitté le sol depuis trop longtemps (anomalie)
        if (wasAirborne || groundFallbackUnlock) {
            pdc.putLong(DASH_UNLOCK_GAME_TIME_KEY, now + DASH_POST_LAND_DELAY_TICKS);
            pdc.remove(DASH_AIR_LOCK_KEY);
            pdc.remove(DASH_LEFT_GROUND_AFTER_DASH_KEY);
            pdc.remove(DASH_LAST_DASH_GAME_TICK_KEY);
        }
    }

    private static void applyModifier(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(id);
        if (Math.abs(amount) > 1.0E-9) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static boolean isOnIce(Player player) {
        BlockPos feet = player.blockPosition().below();
        var state = player.level().getBlockState(feet);
        return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE) || state.is(Blocks.FROSTED_ICE);
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        int rageCount = ChestplateUpgradeData.count(chest, ChestplateUpgradeData.RAGE_UPGRADE_ID);
        int criticalCount = ChestplateUpgradeData.count(chest, ChestplateUpgradeData.CRITICAL_UPGRADE_ID);
        if (rageCount <= 0 && criticalCount <= 0) {
            return;
        }
        float damage = event.getNewDamage();
        if (rageCount > 0) {
            float missingFrac = Math.max(0.0F, 1.0F - (player.getHealth() / Math.max(1.0F, player.getMaxHealth())));
            float rageMult = 1.0F + Math.min(0.80F, missingFrac * (0.40F + (rageCount - 1) * 0.20F));
            damage *= rageMult;
        }
        if (criticalCount > 0) {
            // Base 30% instant crit chance (no jump needed), then scales with stacked critical upgrades.
            float critChance = Math.min(0.80F, 0.30F + Math.max(0, criticalCount - 1) * 0.10F);
            if (player.getRandom().nextFloat() < critChance) {
                damage *= 1.5F;
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.7F, 1.0F);
                if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 9, 0.25, 0.2, 0.25, 0.1);
                }
            }
        }
        event.setNewDamage(damage);
    }

    @SubscribeEvent
    public static void onMobKilled(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LivingEntity dead = event.getEntity();
        if (dead == player) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (ChestplateUpgradeData.count(chest, ChestplateUpgradeData.HEAL_UPGRADE_ID) <= 0) {
            return;
        }
        player.heal(3.0F);
        player.getFoodData().eat(2, 0.6F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, true, true));
    }

    public static void handleDashUpgradePacket(ServerPlayer player, boolean forwardDash) {
        if (player == null || player.level().isClientSide() || !forwardDash) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (ChestplateUpgradeData.count(chest, ChestplateUpgradeData.DASH_UPGRADE_ID) <= 0) {
            return;
        }
        long now = player.level().getGameTime();
        var pdc = player.getPersistentData();
        if (pdc.getLong(DASH_UNLOCK_GAME_TIME_KEY) > now) {
            return;
        }
        if (pdc.getBoolean(DASH_AIR_LOCK_KEY)) {
            return;
        }
        if (player.isInWaterOrBubble() || player.isInLava() || player.isFallFlying()) {
            return;
        }
        var look = player.getLookAngle();
        double dashStrength = 1.7D;
        double upBoost = 0.12D;
        player.setDeltaMovement(
                player.getDeltaMovement().x + look.x * dashStrength,
                Math.max(player.getDeltaMovement().y, upBoost),
                player.getDeltaMovement().z + look.z * dashStrength);
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.resetFallDistance();
        pdc.putBoolean(DASH_AIR_LOCK_KEY, true);
        pdc.putBoolean(DASH_LEFT_GROUND_AFTER_DASH_KEY, false);
        pdc.putLong(DASH_LAST_DASH_GAME_TICK_KEY, now);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.45F, 1.6F);
    }
}
