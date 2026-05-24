package com.soleapixel.bmcmod.gameplay;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.registry.ModItems;

/**
 * Bottes célestes : vitesse + hauteur de pas (modificateurs transitoires), immunité aux dégâts de chute,
 * double saut (paquet + {@link SkyBootsGameplay#applyDoubleJump(ServerPlayer)}).
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class SkyBootsGameplay {
    private static final ResourceLocation SKY_SPEED_ID = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "sky_boots_movement_speed");
    private static final ResourceLocation SKY_STEP_ID = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "sky_boots_step_height");
    private static final double SKY_SPEED_BONUS = 0.18;
    /** Joueur vanilla ~0,6 ; +0,4 → marche sur bloc entier sans sauter. */
    private static final double SKY_STEP_BONUS = 0.4;
    /** Poussée horizontale dans la direction du regard (second saut, si le client demande un dash). */
    private static final double DOUBLE_JUMP_FORWARD_BOOST = 0.68;
    private static final String PDC_DOUBLE_JUMP_COOLDOWN = "bmcmod_sky_boots_dj_until";

    private SkyBootsGameplay() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        applyOrRemoveBootAttributes(player);
    }

    /** Tôt dans la séquence de dégâts (NeoForge) : met les chutes à 0 avant armure / absorption. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (!hasSkyBoots(player) || !isFallDamage(event.getSource())) {
            return;
        }
        event.setAmount(0.0F);
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (!hasSkyBoots(player)) {
            return;
        }
        event.setDistance(0.0F);
        event.setDamageMultiplier(0.0F);
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (!hasSkyBoots(player) || !isFallDamage(event.getSource())) {
            return;
        }
        event.setNewDamage(0.0F);
    }

    private static boolean hasSkyBoots(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.SKY_BOOTS.get());
    }

    private static boolean isFallDamage(DamageSource s) {
        if (s.is(DamageTypeTags.IS_FALL) || s.is(DamageTypes.FALL)) {
            return true;
        }
        String id = s.getMsgId();
        return id != null && id.contains("fall");
    }

    private static void applyOrRemoveBootAttributes(ServerPlayer player) {
        AttributeInstance move = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
        if (move == null || step == null) {
            return;
        }
        boolean sky = player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.SKY_BOOTS.get());
        if (sky) {
            if (!move.hasModifier(SKY_SPEED_ID)) {
                move.addTransientModifier(
                        new AttributeModifier(SKY_SPEED_ID, SKY_SPEED_BONUS, AttributeModifier.Operation.ADD_VALUE));
            }
            if (!step.hasModifier(SKY_STEP_ID)) {
                step.addTransientModifier(
                        new AttributeModifier(SKY_STEP_ID, SKY_STEP_BONUS, AttributeModifier.Operation.ADD_VALUE));
            }
        } else {
            move.removeModifier(SKY_SPEED_ID);
            step.removeModifier(SKY_STEP_ID);
        }
    }

    public static void handleDoubleJumpPacket(ServerPlayer player, boolean forwardDash) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
        if (!feet.is(ModItems.SKY_BOOTS.get())) {
            return;
        }
        if (player.isInWater() || player.isInLava()) {
            return;
        }
        if (player.isFallFlying()) {
            return;
        }
        // Rejette seulement un paquet reçu alors que le joueur est encore en phase « décollage au sol » (désync).
        if (player.onGround() && player.fallDistance <= 0.02F && player.getDeltaMovement().y > -0.08) {
            return;
        }
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong(PDC_DOUBLE_JUMP_COOLDOWN) > now) {
            return;
        }
        applyDoubleJump(player, forwardDash);
        player.getPersistentData().putLong(PDC_DOUBLE_JUMP_COOLDOWN, now + 6L);
    }

    private static void applyDoubleJump(ServerPlayer player, boolean forwardDash) {
        double base = Math.max(player.getAttributeValue(Attributes.JUMP_STRENGTH), 0.47);
        // Second saut plus haut qu’un saut vanilla ; cumule un peu avec l’élan vertical actuel.
        double vy = Math.max(player.getDeltaMovement().y + base * 0.52, base * 1.72);
        Vec3 look = player.getLookAngle();
        Vec3 m = player.getDeltaMovement();
        double vx = m.x;
        double vz = m.z;
        if (forwardDash) {
            vx += look.x * DOUBLE_JUMP_FORWARD_BOOST;
            vz += look.z * DOUBLE_JUMP_FORWARD_BOOST;
        }
        player.setDeltaMovement(vx, vy, vz);
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.resetFallDistance();
        spawnDoubleJumpParticles(player, forwardDash ? look : new Vec3(0.0, 1.0, 0.0));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.35F, 1.45F);
    }

    /** Particules visibles par les joueurs autour (propulsion « céleste »). */
    private static void spawnDoubleJumpParticles(ServerPlayer player, Vec3 look) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        double px = player.getX();
        double py = player.getY() + 0.08;
        double pz = player.getZ();
        // Légèrement derrière les pieds, dans le sens opposé à la poussée.
        Vec3 wake = new Vec3(-look.x, 0.12, -look.z);
        if (wake.lengthSqr() > 1.0E-6) {
            wake = wake.normalize().scale(0.22);
        }
        double wx = px + wake.x;
        double wz = pz + wake.z;

        level.sendParticles(ParticleTypes.END_ROD, wx, py, wz, 22, 0.14, 0.1, 0.14, 0.02);
        level.sendParticles(ParticleTypes.FIREWORK, wx, py + 0.05, wz, 6, 0.18, 0.12, 0.18, 0.04);
        level.sendParticles(ParticleTypes.ENCHANT, px, py + 0.55, pz, 28, 0.4, 0.35, 0.4, 0.55);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, wx, py - 0.02, wz, 10, 0.16, 0.06, 0.16, 0.018);
    }
}
