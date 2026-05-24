package com.soleapixel.bmcmod.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.rarity.BmcModRarity;
import com.soleapixel.bmcmod.rarity.RarityStickItem;

/**
 * Bâton de l’End : 3 modes (perle chargée, rayon + TP aléatoire cible, TP aléatoire soi).
 * Ctrl + clic droit change de mode (paquet réseau, voir {@link com.soleapixel.bmcmod.client.EndStaffClientCycleHandler}).
 */
public final class EndStaffItem extends RarityStickItem {
    public static final String MODE_KEY = "EndStaffMode";
    public static final int MODE_PEARL = 1;
    public static final int MODE_TARGET = 2;
    public static final int MODE_RANDOM = 3;

    public static final int DRAW_DURATION = 72_000;
    /** Temps minimum de charge avant de pouvoir lancer une perle (ticks). */
    public static final int PEARL_MIN_CHARGE_TICKS = 16;
    public static final int MAX_USES = 400;
    public static final int COST_PEARL = 3;
    public static final int COST_TARGET = 5;
    public static final int COST_RANDOM = 8;
    public static final float TARGET_DAMAGE = 3.25F;
    public static final double TARGET_RANGE = 36.0;
    public static final double TARGET_RAY_HALF_WIDTH = 0.95;

    public static final int CD_PEARL = 22;
    public static final int CD_TARGET = 28;
    public static final int CD_RANDOM = 45;

    public EndStaffItem(Properties properties) {
        super(properties, BmcModRarity.EXOTIC);
    }

    public static int getMode(ItemStack stack) {
        int m = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(MODE_KEY);
        return m >= MODE_PEARL && m <= MODE_RANDOM ? m : MODE_PEARL;
    }

    public static void setMode(ItemStack stack, int mode) {
        int m = Mth.clamp(mode, MODE_PEARL, MODE_RANDOM);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(MODE_KEY, m);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void cycleMode(ItemStack stack) {
        int next = getMode(stack) == MODE_RANDOM ? MODE_PEARL : getMode(stack) + 1;
        setMode(stack, next);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        int mode = getMode(stack);
        if (!sp.getAbilities().instabuild && sp.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (mode == MODE_PEARL) {
            sp.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (mode == MODE_TARGET) {
            if (tryTargetTeleport(sp, stack, hand)) {
                sp.awardStat(Stats.ITEM_USED.get(this));
            }
            return InteractionResultHolder.success(stack);
        }
        if (mode == MODE_RANDOM) {
            if (tryRandomSelfTeleport(sp, stack, hand)) {
                sp.awardStat(Stats.ITEM_USED.get(this));
            }
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DRAW_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel sl) || !(entity instanceof ServerPlayer sp)) {
            return;
        }
        if (getMode(stack) != MODE_PEARL) {
            return;
        }
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            sp.stopUsingItem();
            return;
        }
        int elapsed = DRAW_DURATION - remainingUseDuration;
        if (elapsed > 0 && elapsed % 8 == 0 && elapsed < PEARL_MIN_CHARGE_TICKS) {
            int pct = Mth.clamp((int) (100L * elapsed / PEARL_MIN_CHARGE_TICKS), 0, 99);
            sp.displayClientMessage(Component.translatable("message.bmcmod.end_staff.charging_pearl", pct).withStyle(ChatFormatting.DARK_PURPLE), true);
        }
        if (elapsed == PEARL_MIN_CHARGE_TICKS) {
            sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 0.35F, 1.2F);
            sp.displayClientMessage(Component.translatable("message.bmcmod.end_staff.pearl_ready").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        if (elapsed > 0 && elapsed % 4 == 0) {
            Vec3 eye = sp.getEyePosition(1f);
            Vec3 look = sp.getLookAngle().normalize();
            Vec3 p = eye.add(look.scale(0.35 + (elapsed % 20) * 0.02));
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, p.x, p.y, p.z, 3, 0.06, 0.06, 0.06, 0.02);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer sp)) {
            return;
        }
        if (getMode(stack) != MODE_PEARL) {
            return;
        }
        int used = DRAW_DURATION - timeLeft;
        if (used < PEARL_MIN_CHARGE_TICKS) {
            if (used > 3) {
                sp.displayClientMessage(Component.translatable("message.bmcmod.end_staff.pearl_cancelled").withStyle(ChatFormatting.GRAY), true);
            }
            return;
        }
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            return;
        }
        if (!sp.getAbilities().instabuild && sp.getCooldowns().isOnCooldown(this)) {
            return;
        }
        throwChargedPearl(sp, stack);
        if (!sp.getAbilities().instabuild) {
            EquipmentSlot slot = sp.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            for (int i = 0; i < COST_PEARL; i++) {
                if (stack.getDamageValue() >= stack.getMaxDamage()) {
                    break;
                }
                stack.hurtAndBreak(1, sp, slot);
            }
            sp.getCooldowns().addCooldown(this, CD_PEARL);
        }
        sp.awardStat(Stats.ITEM_USED.get(this));
    }

    private static void throwChargedPearl(ServerPlayer player, ItemStack staffStack) {
        ServerLevel level = player.serverLevel();
        ThrownEnderpearl pearl = new ThrownEnderpearl(level, player);
        pearl.setItem(new ItemStack(Items.ENDER_PEARL));
        pearl.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.55F, 1.0F);
        level.addFreshEntity(pearl);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_PEARL_THROW, SoundSource.PLAYERS, 0.55F, 0.75F + level.random.nextFloat() * 0.2F);
    }

    private boolean tryTargetTeleport(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        ServerLevel level = player.serverLevel();
        LivingEntity target = traceLivingAlongLook(level, player, TARGET_RANGE);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.bmcmod.end_staff.no_target").withStyle(ChatFormatting.GRAY), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.2F, 1.8F);
            return false;
        }
        target.hurt(level.damageSources().playerAttack(player), TARGET_DAMAGE);
        boolean ok = teleportLivingRandomly(level, target, 16.0);
        if (!ok) {
            player.displayClientMessage(Component.translatable("message.bmcmod.end_staff.tp_failed").withStyle(ChatFormatting.RED), true);
            return false;
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
        if (!player.getAbilities().instabuild) {
            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            for (int i = 0; i < COST_TARGET; i++) {
                if (stack.getDamageValue() >= stack.getMaxDamage()) {
                    break;
                }
                stack.hurtAndBreak(1, player, slot);
            }
            player.getCooldowns().addCooldown(this, CD_TARGET);
        }
        return true;
    }

    private boolean tryRandomSelfTeleport(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        ServerLevel level = player.serverLevel();
        boolean ok = teleportLivingRandomly(level, player, 48.0);
        if (!ok) {
            player.displayClientMessage(Component.translatable("message.bmcmod.end_staff.tp_failed").withStyle(ChatFormatting.RED), true);
            return false;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!player.getAbilities().instabuild) {
            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            for (int i = 0; i < COST_RANDOM; i++) {
                if (stack.getDamageValue() >= stack.getMaxDamage()) {
                    break;
                }
                stack.hurtAndBreak(1, player, slot);
            }
            player.getCooldowns().addCooldown(this, CD_RANDOM);
        }
        return true;
    }

    private static LivingEntity traceLivingAlongLook(ServerLevel level, ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition(1f);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(range));
        HitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }
        AABB sweep = new AABB(start, end).inflate(TARGET_RAY_HALF_WIDTH + 0.25);
        LivingEntity best = null;
        double bestAlong = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, sweep)) {
            if (!e.isAlive() || e == player || !e.isPickable()) {
                continue;
            }
            Vec3 c = e.getBoundingBox().getCenter();
            if (distancePointToSegmentSqr(c, start, end) > TARGET_RAY_HALF_WIDTH * TARGET_RAY_HALF_WIDTH) {
                continue;
            }
            double along = c.subtract(start).dot(look);
            if (along < 0 || along > range + 0.5) {
                continue;
            }
            if (along < bestAlong) {
                bestAlong = along;
                best = e;
            }
        }
        return best;
    }

    private static double distancePointToSegmentSqr(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double lenSq = ab.lengthSqr();
        if (lenSq < 1.0E-7) {
            return p.distanceToSqr(a);
        }
        double t = Mth.clamp(p.subtract(a).dot(ab) / lenSq, 0.0, 1.0);
        Vec3 closest = a.add(ab.scale(t));
        return p.distanceToSqr(closest);
    }

    /**
     * TP « style enderman » : plusieurs essais dans un carré horizontal autour de la position actuelle.
     */
    private static boolean teleportLivingRandomly(ServerLevel level, LivingEntity entity, double spread) {
        var random = level.random;
        double bx = entity.getX();
        double bz = entity.getZ();
        for (int attempt = 0; attempt < 28; attempt++) {
            double tx = bx + (random.nextDouble() - 0.5) * 2.0 * spread;
            double tz = bz + (random.nextDouble() - 0.5) * 2.0 * spread;
            int ix = Mth.floor(tx);
            int iz = Mth.floor(tz);
            if (!level.hasChunk(ix >> 4, iz >> 4)) {
                continue;
            }
            double ty = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz) + (entity instanceof Player ? 0.01 : 0.25);
            if (entity.randomTeleport(tx, ty, tz, true)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!StaffTooltip.showDetailedLines(tooltip, flag)) {
            return;
        }
        int mode = getMode(stack);
        tooltip.add(Component.translatable("item.bmcmod.end_staff.mode_line", Component.translatable("item.bmcmod.end_staff.mode." + mode))
                .withStyle(ChatFormatting.DARK_AQUA));
        int left = stack.getMaxDamage() - stack.getDamageValue();
        tooltip.add(Component.translatable("item.bmcmod.end_staff.fuel", left, stack.getMaxDamage()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bmcmod.end_staff.usage").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.bmcmod.end_staff.mode_switch").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamageableItem();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * (stack.getMaxDamage() - stack.getDamageValue()) / (float) stack.getMaxDamage());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = Math.max(0.0F, ((float) stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage());
        return Mth.hsvToRgb(0.78F - f * 0.12F, 0.85F, 1.0F);
    }

    @Override
    public boolean canFitInsideContainerItems(ItemStack stack) {
        return false;
    }
}
