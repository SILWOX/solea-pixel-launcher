package com.soleapixel.bmcmod.entity;

import java.util.Locale;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import com.soleapixel.bmcmod.registry.ModItems;

/**
 * Training dummy: infinite target that displays incoming damage.
 */
public class Dummy extends ArmorStand {
    private static final byte HIT_ANIMATION_EVENT = 61;
    private static final int HIT_ANIMATION_DURATION_TICKS = 8;
    private static final String TAG_DAMAGE_LABEL_OWNER = "bmcmod_dummy_label_owner";
    private static final String TAG_DAMAGE_LABEL_AGE = "bmcmod_dummy_label_age";
    private static final int DAMAGE_LABEL_LIFETIME_TICKS = 14;
    private int hitAnimationTicks;

    public Dummy(EntityType<? extends Dummy> type, Level level) {
        super(type, level);
        this.setInvisible(false);
        this.setNoBasePlate(false);
        this.setNoGravity(true);
        this.setShowArms(true);
        this.setCustomNameVisible(false);
        this.setCustomName(null);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.DUMMY_ITEM.get());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isCustomNameVisible()) {
            this.setCustomNameVisible(false);
        }
        if (this.hitAnimationTicks > 0) {
            this.hitAnimationTicks--;
        }
        if (!this.level().isClientSide && this.tickCount % 2 == 0) {
            tickDamageLabels();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) {
            return true;
        }
        if (source.getEntity() instanceof Player player && player.isShiftKeyDown()) {
            return false;
        }
        float displayedDamage = amount;
        if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            float armor = this.getArmorValue();
            float toughness = (float) this.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            displayedDamage = CombatRules.getDamageAfterAbsorb(this, amount, source, armor, toughness);
        }
        float shown = Math.max(0.0F, displayedDamage);
        spawnDamageParticles(shown);
        spawnDamageLabel(shown);
        if (source.getEntity() instanceof ServerPlayer attacker) {
            ChatFormatting color = shown >= 20.0F ? ChatFormatting.RED : shown >= 10.0F ? ChatFormatting.GOLD : ChatFormatting.YELLOW;
            attacker.displayClientMessage(Component.literal(formatDamage(shown)).withStyle(color, ChatFormatting.BOLD), true);
        }
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ARMOR_STAND_HIT, SoundSource.PLAYERS, 0.95F, 0.9F + this.random.nextFloat() * 0.2F);
        this.hitAnimationTicks = HIT_ANIMATION_DURATION_TICKS;
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, HIT_ANIMATION_EVENT);
        }
        return false;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == HIT_ANIMATION_EVENT) {
            this.hitAnimationTicks = HIT_ANIMATION_DURATION_TICKS;
            return;
        }
        super.handleEntityEvent(id);
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (!this.level().isClientSide) {
                ItemStack drop = new ItemStack(ModItems.DUMMY_ITEM.get());
                if (!player.getInventory().add(drop)) {
                    player.drop(drop, false);
                }
                this.discard();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        // Keep vanilla armor-stand interactions (equip/unequip armor, etc.).
        return super.interactAt(player, vec, hand);
    }

    private static String formatDamage(float amount) {
        float rounded = Math.round(amount * 10.0F) / 10.0F;
        if (Math.abs(rounded - Math.round(rounded)) < 0.0001F) {
            return Integer.toString(Math.round(rounded));
        }
        return String.format(Locale.ROOT, "%.1f", rounded);
    }

    public float hitAnimationProgress(float partialTick) {
        if (this.hitAnimationTicks <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, (this.hitAnimationTicks - partialTick) / (float) HIT_ANIMATION_DURATION_TICKS);
    }

    private void spawnDamageParticles(float damage) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int burstCount = damage >= 20.0F ? 22 : damage >= 10.0F ? 16 : 12;
        double bodyCenterY = this.getY() + this.getBbHeight() * 0.52D;
        for (int i = 0; i < burstCount; i++) {
            double theta = this.random.nextDouble() * Math.PI * 2.0D;
            double phi = Math.acos((this.random.nextDouble() * 2.0D) - 1.0D);
            double vx = Math.sin(phi) * Math.cos(theta);
            double vy = Math.cos(phi) * 0.6D;
            double vz = Math.sin(phi) * Math.sin(theta);

            double px = this.getX() + vx * (0.12D + this.random.nextDouble() * 0.20D);
            double py = bodyCenterY + vy * (0.10D + this.random.nextDouble() * 0.24D);
            double pz = this.getZ() + vz * (0.12D + this.random.nextDouble() * 0.20D);

            // 360-degree critical-hit burst from the dummy body.
            serverLevel.sendParticles(ParticleTypes.CRIT, px, py, pz, 1, vx * 0.16D, 0.03D + vy * 0.10D, vz * 0.16D, 0.0D);
            if (this.random.nextFloat() < 0.28F) {
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, px, py, pz, 1, vx * 0.08D, vy * 0.06D, vz * 0.08D, 0.0D);
            }
        }
    }

    private static void drawChar(ServerLevel level, char c, float x, float y, float z, float s, DamageParticleStyle style, float side) {
        switch (c) {
            case '0' -> drawSegments(level, x, y, z, s, style, side, true, true, true, true, true, true, false);
            case '1' -> drawSegments(level, x, y, z, s, style, side, false, true, true, false, false, false, false);
            case '2' -> drawSegments(level, x, y, z, s, style, side, true, true, false, true, true, false, true);
            case '3' -> drawSegments(level, x, y, z, s, style, side, true, true, true, true, false, false, true);
            case '4' -> drawSegments(level, x, y, z, s, style, side, false, true, true, false, false, true, true);
            case '5' -> drawSegments(level, x, y, z, s, style, side, true, false, true, true, false, true, true);
            case '6' -> drawSegments(level, x, y, z, s, style, side, true, false, true, true, true, true, true);
            case '7' -> drawSegments(level, x, y, z, s, style, side, true, true, true, false, false, false, false);
            case '8' -> drawSegments(level, x, y, z, s, style, side, true, true, true, true, true, true, true);
            case '9' -> drawSegments(level, x, y, z, s, style, side, true, true, true, true, false, true, true);
            case '.' -> spawnPoint(level, x + 2.8F * s, y + 0.4F * s, z, style, side);
            default -> {
            }
        }
    }

    private static void drawSegments(ServerLevel level, float x, float y, float z, float s, DamageParticleStyle style, float side,
            boolean a, boolean b, boolean c, boolean d, boolean e, boolean f, boolean g) {
        if (a) line(level, x + 0.5F * s, y + 5.8F * s, z, x + 2.9F * s, y + 5.8F * s, z, 5, style, side);
        if (b) line(level, x + 3.2F * s, y + 3.4F * s, z, x + 3.2F * s, y + 5.6F * s, z, 4, style, side);
        if (c) line(level, x + 3.2F * s, y + 0.9F * s, z, x + 3.2F * s, y + 3.1F * s, z, 4, style, side);
        if (d) line(level, x + 0.5F * s, y + 0.6F * s, z, x + 2.9F * s, y + 0.6F * s, z, 5, style, side);
        if (e) line(level, x + 0.2F * s, y + 0.9F * s, z, x + 0.2F * s, y + 3.1F * s, z, 4, style, side);
        if (f) line(level, x + 0.2F * s, y + 3.4F * s, z, x + 0.2F * s, y + 5.6F * s, z, 4, style, side);
        if (g) line(level, x + 0.5F * s, y + 3.2F * s, z, x + 2.9F * s, y + 3.2F * s, z, 5, style, side);
    }

    private static void line(ServerLevel level, float x1, float y1, float z1, float x2, float y2, float z2, int points, DamageParticleStyle style, float side) {
        if (points <= 1) {
            spawnPoint(level, x1, y1, z1, style, side);
            return;
        }
        for (int i = 0; i < points; i++) {
            float t = i / (float) (points - 1);
            spawnPoint(level, x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, z1 + (z2 - z1) * t, style, side);
        }
    }

    private static void spawnPoint(ServerLevel level, float x, float y, float z, DamageParticleStyle style, float side) {
        double driftX = side * style.sideDrift + (level.random.nextDouble() - 0.5D) * 0.016D;
        double driftY = style.upwardDrift + (level.random.nextDouble() - 0.5D) * 0.012D;
        double driftZ = (level.random.nextDouble() - 0.5D) * 0.014D;
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 1, driftX, driftY, driftZ, 0.0D);
        if (style.magicSparkChance > 0.0F && level.random.nextFloat() < style.magicSparkChance) {
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y, z, 1, driftX * 0.45D, driftY * 0.45D, driftZ * 0.45D, 0.0D);
        }
    }

    private static DamageParticleStyle styleForDamage(float damage) {
        if (damage >= 40.0F) {
            return new DamageParticleStyle(0.078F, 16, 0.036F, 0.042F, 0.45F);
        }
        if (damage >= 22.0F) {
            return new DamageParticleStyle(0.070F, 12, 0.031F, 0.036F, 0.32F);
        }
        if (damage >= 10.0F) {
            return new DamageParticleStyle(0.064F, 10, 0.027F, 0.030F, 0.20F);
        }
        if (damage >= 4.0F) {
            return new DamageParticleStyle(0.060F, 8, 0.022F, 0.026F, 0.10F);
        }
        return new DamageParticleStyle(0.056F, 6, 0.018F, 0.021F, 0.05F);
    }

    private record DamageParticleStyle(float scale, int burstCount, float sideDrift,
            float upwardDrift, float magicSparkChance) {
    }

    private void spawnDamageLabel(float damage) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        TextDisplay label = EntityType.TEXT_DISPLAY.create(serverLevel);
        if (label == null) {
            return;
        }
        float side = this.random.nextBoolean() ? 1.0F : -1.0F;
        double x = this.getX() + side * (0.20D + this.random.nextDouble() * 0.22D);
        double y = this.getY() + 1.95D + this.random.nextDouble() * 0.18D;
        double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 0.2D;
        label.moveTo(x, y, z, 0.0F, 0.0F);
        label.setNoGravity(true);
        ChatFormatting color = damage >= 20.0F ? ChatFormatting.RED : damage >= 10.0F ? ChatFormatting.GOLD : ChatFormatting.YELLOW;
        CompoundTag tag = new CompoundTag();
        tag.putString("billboard", "center");
        tag.putString("text", Component.Serializer.toJson(Component.literal(formatDamage(damage)).withStyle(color, ChatFormatting.BOLD), serverLevel.registryAccess()));
        tag.putInt("background", 0x70000000);
        tag.putByte("text_opacity", (byte) 255);
        tag.putBoolean("shadow", true);
        tag.putBoolean("see_through", true);
        label.load(tag);
        label.getPersistentData().putUUID(TAG_DAMAGE_LABEL_OWNER, this.getUUID());
        label.getPersistentData().putInt(TAG_DAMAGE_LABEL_AGE, 0);
        serverLevel.addFreshEntity(label);
    }

    private void tickDamageLabels() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (TextDisplay label : serverLevel.getEntitiesOfClass(TextDisplay.class, this.getBoundingBox().inflate(14.0D), e ->
                e.getPersistentData().hasUUID(TAG_DAMAGE_LABEL_OWNER)
                        && this.getUUID().equals(e.getPersistentData().getUUID(TAG_DAMAGE_LABEL_OWNER)))) {
            int age = label.getPersistentData().getInt(TAG_DAMAGE_LABEL_AGE) + 1;
            label.getPersistentData().putInt(TAG_DAMAGE_LABEL_AGE, age);
            label.setPos(label.getX(), label.getY() + 0.038D, label.getZ());
            if (age >= DAMAGE_LABEL_LIFETIME_TICKS) {
                label.discard();
            }
        }
    }

    private void clearDamageLabels() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (TextDisplay label : serverLevel.getEntitiesOfClass(TextDisplay.class, this.getBoundingBox().inflate(20.0D), e ->
                e.getPersistentData().hasUUID(TAG_DAMAGE_LABEL_OWNER)
                        && this.getUUID().equals(e.getPersistentData().getUUID(TAG_DAMAGE_LABEL_OWNER)))) {
            label.discard();
        }
    }

    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        clearDamageLabels();
        super.remove(reason);
    }

}
