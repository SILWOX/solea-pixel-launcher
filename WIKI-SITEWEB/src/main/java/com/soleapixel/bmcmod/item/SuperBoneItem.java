package com.soleapixel.bmcmod.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.registry.ModItems;
import com.soleapixel.bmcmod.registry.ModMobEffects;

/**
 * Nourriture de combat pour loup : buffs 10 min (effets + modificateurs d’attribut) — uniquement loup apprivoisé par le joueur.
 */
public final class SuperBoneItem extends Item {
    /** 10 minutes */
    public static final int BUFF_DURATION_TICKS = 20 * 60 * 10;

    /**
     * Jusqu’à ce tick de jeu inclus : buff actif (pas de second os) ; à l’expiration les modificateurs sont retirés
     * ({@link com.soleapixel.bmcmod.gameplay.SuperBoneWolfEvents}).
     */
    public static final String BUFF_UNTIL_GAME_TIME_TAG = "bmcmod:super_bone_next_tick";

    private static final ResourceLocation ATTR_HEALTH = BmcMod.loc("super_bone_max_health");
    private static final ResourceLocation ATTR_ATTACK = BmcMod.loc("super_bone_attack_damage");
    private static final ResourceLocation ATTR_ARMOR = BmcMod.loc("super_bone_armor");
    private static final ResourceLocation ATTR_ARMOR_TOUGH = BmcMod.loc("super_bone_armor_toughness");
    private static final ResourceLocation ATTR_KB_RES = BmcMod.loc("super_bone_knockback_resistance");
    private static final ResourceLocation ATTR_FOLLOW = BmcMod.loc("super_bone_follow_range");
    private static final ResourceLocation ATTR_STEP = BmcMod.loc("super_bone_step_height");

    public SuperBoneItem(Properties properties) {
        super(properties);
    }

    private static boolean isSuperBoneCooldownActive(Wolf wolf, Level level) {
        long until = wolf.getPersistentData().getLong(BUFF_UNTIL_GAME_TIME_TAG);
        return level.getGameTime() < until;
    }

    private static void applyOneModifier(
            AttributeInstance inst, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        if (inst == null) {
            return;
        }
        inst.removeModifier(id);
        if (Math.abs(amount) > 1.0E-9) {
            inst.addTransientModifier(new AttributeModifier(id, amount, op));
        }
    }

    /** Réapplique les bonus (cohérence après chargement / chunk). */
    public static void refreshSuperBoneAttributeModifiers(Wolf wolf) {
        applyOneModifier(wolf.getAttribute(Attributes.MAX_HEALTH), ATTR_HEALTH, 10.0, AttributeModifier.Operation.ADD_VALUE);
        applyOneModifier(wolf.getAttribute(Attributes.ATTACK_DAMAGE), ATTR_ATTACK, 4.0, AttributeModifier.Operation.ADD_VALUE);
        applyOneModifier(wolf.getAttribute(Attributes.ARMOR), ATTR_ARMOR, 8.0, AttributeModifier.Operation.ADD_VALUE);
        applyOneModifier(wolf.getAttribute(Attributes.ARMOR_TOUGHNESS), ATTR_ARMOR_TOUGH, 3.0, AttributeModifier.Operation.ADD_VALUE);
        applyOneModifier(wolf.getAttribute(Attributes.KNOCKBACK_RESISTANCE), ATTR_KB_RES, 0.35, AttributeModifier.Operation.ADD_VALUE);
        applyOneModifier(wolf.getAttribute(Attributes.FOLLOW_RANGE), ATTR_FOLLOW, 20.0, AttributeModifier.Operation.ADD_VALUE);
        applyOneModifier(wolf.getAttribute(Attributes.STEP_HEIGHT), ATTR_STEP, 0.35, AttributeModifier.Operation.ADD_VALUE);
    }

    public static void clearSuperBoneAttributeModifiers(Wolf wolf) {
        removeIfPresent(wolf.getAttribute(Attributes.MAX_HEALTH), ATTR_HEALTH);
        removeIfPresent(wolf.getAttribute(Attributes.ATTACK_DAMAGE), ATTR_ATTACK);
        removeIfPresent(wolf.getAttribute(Attributes.ARMOR), ATTR_ARMOR);
        removeIfPresent(wolf.getAttribute(Attributes.ARMOR_TOUGHNESS), ATTR_ARMOR_TOUGH);
        removeIfPresent(wolf.getAttribute(Attributes.KNOCKBACK_RESISTANCE), ATTR_KB_RES);
        removeIfPresent(wolf.getAttribute(Attributes.FOLLOW_RANGE), ATTR_FOLLOW);
        removeIfPresent(wolf.getAttribute(Attributes.STEP_HEIGHT), ATTR_STEP);
    }

    private static void removeIfPresent(AttributeInstance inst, ResourceLocation id) {
        if (inst != null) {
            inst.removeModifier(id);
        }
    }

    /**
     * Appelé depuis {@link com.soleapixel.bmcmod.gameplay.SuperBoneEntityInteractEvents} (prioritaire sur {@code mobInteract} vanilla).
     */
    public static InteractionResult tryApplyToOwnedWolf(
            ItemStack stack, Player player, Entity target, InteractionHand usedHand) {
        if (!stack.is(ModItems.SUPER_BONE.get())) {
            return InteractionResult.PASS;
        }
        if (!(target instanceof Wolf wolf)) {
            return InteractionResult.PASS;
        }
        Level level = player.level();
        if (!wolf.isTame() || !wolf.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        if (isSuperBoneCooldownActive(wolf, level)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.bmcmod.super_bone.cooldown"), true);
            }
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Effets de statut (10 min)
        wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BUFF_DURATION_TICKS, 2, false, true, true));
        wolf.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BUFF_DURATION_TICKS, 1, false, true, true));
        wolf.addEffect(new MobEffectInstance(MobEffects.JUMP, BUFF_DURATION_TICKS, 1, false, true, true));
        wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BUFF_DURATION_TICKS, 1, false, true, true));
        wolf.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, BUFF_DURATION_TICKS, 1, false, true, true));
        wolf.addEffect(new MobEffectInstance(MobEffects.REGENERATION, BUFF_DURATION_TICKS, 0, false, true, true));
        wolf.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, BUFF_DURATION_TICKS, 0, false, true, true));
        wolf.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, BUFF_DURATION_TICKS, 2, false, true, true));
        wolf.addEffect(new MobEffectInstance(ModMobEffects.GROW, BUFF_DURATION_TICKS, 0, false, true, true));

        wolf.getPersistentData().putLong(BUFF_UNTIL_GAME_TIME_TAG, level.getGameTime() + BUFF_DURATION_TICKS);
        refreshSuperBoneAttributeModifiers(wolf);
        wolf.heal(Math.min(20.0F, Math.max(0.0F, wolf.getMaxHealth() - wolf.getHealth())));

        stack.consume(1, player);

        level.playSound(null, wolf.blockPosition(), SoundEvents.WOLF_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.15F);
        level.playSound(null, wolf.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.35F, 1.7F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        return tryApplyToOwnedWolf(stack, player, interactionTarget, usedHand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.bmcmod.super_bone.tooltip").withStyle(ChatFormatting.GRAY));
    }

    public static Item.Properties defaultProperties() {
        return new Item.Properties().stacksTo(16).rarity(Rarity.RARE);
    }
}
