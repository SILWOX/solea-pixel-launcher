package com.soleapixel.bmcmod.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.rarity.BmcModRarity;
import com.soleapixel.bmcmod.rarity.RarityStickItem;

/**
 * Bâton du dragon : mode 1 boule de dragon (charge + relâchement), mode 2 rayon de souffle (maintien).
 * Ctrl + clic droit pour cycler (paquet réseau).
 */
public final class DragonStaffItem extends RarityStickItem {
    public static final String MODE_KEY = "DragonStaffMode";
    public static final int MODE_FIREBALL = 1;
    public static final int MODE_BREATH = 2;

    public static final int DRAW_DURATION = 72_000;
    public static final int FIREBALL_MIN_CHARGE_TICKS = 14;
    public static final int MAX_USES = 400;
    public static final int COST_FIREBALL = 10;
    public static final int CD_FIREBALL = 35;

    public static final double BREATH_RANGE = 20.0;
    public static final double BREATH_RAY_HALF_WIDTH = 1.32;
    public static final float BREATH_DAMAGE_PER_TICK = 0.45F;
    public static final int BREATH_DRAIN_INTERVAL = 2;
    public static final int CD_BREATH_RELEASE = 6;

    public DragonStaffItem(Properties properties) {
        super(properties, BmcModRarity.EXOTIC);
    }

    public static int getMode(ItemStack stack) {
        int m = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(MODE_KEY);
        return m >= MODE_FIREBALL && m <= MODE_BREATH ? m : MODE_FIREBALL;
    }

    public static void setMode(ItemStack stack, int mode) {
        int m = Mth.clamp(mode, MODE_FIREBALL, MODE_BREATH);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(MODE_KEY, m);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void cycleMode(ItemStack stack) {
        setMode(stack, getMode(stack) == MODE_FIREBALL ? MODE_BREATH : MODE_FIREBALL);
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
        if (!sp.getAbilities().instabuild && sp.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        sp.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
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
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            sp.stopUsingItem();
            return;
        }
        int mode = getMode(stack);
        int elapsed = DRAW_DURATION - remainingUseDuration;
        if (mode == MODE_FIREBALL) {
            if (elapsed > 0 && elapsed % 8 == 0 && elapsed < FIREBALL_MIN_CHARGE_TICKS) {
                int pct = Mth.clamp((int) (100L * elapsed / FIREBALL_MIN_CHARGE_TICKS), 0, 99);
                sp.displayClientMessage(Component.translatable("message.bmcmod.dragon_staff.charging_fireball", pct).withStyle(ChatFormatting.DARK_PURPLE), true);
            }
            if (elapsed == FIREBALL_MIN_CHARGE_TICKS) {
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.25F, 1.6F);
                sp.displayClientMessage(Component.translatable("message.bmcmod.dragon_staff.fireball_ready").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            if (elapsed > 0 && elapsed % 4 == 0) {
                spawnChargeParticles(sl, sp, elapsed);
            }
            return;
        }
        // Souffle : rayon + drain durabilité
        applyBreathBeam(sl, sp);
        if (!sp.getAbilities().instabuild && elapsed % BREATH_DRAIN_INTERVAL == 0) {
            EquipmentSlot slot = sp.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            stack.hurtAndBreak(1, sp, slot);
        }
        if (stack.isEmpty() || stack.getDamageValue() >= stack.getMaxDamage()) {
            sp.stopUsingItem();
            return;
        }
        if (sl.getGameTime() % 10L == 0L) {
            float pitch = 0.72F + sl.random.nextFloat() * 0.18F;
            sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDER_DRAGON_AMBIENT, SoundSource.PLAYERS, 0.11F, pitch);
        }
    }

    private static void spawnChargeParticles(ServerLevel level, ServerPlayer player, int elapsed) {
        Vec3 eye = player.getEyePosition(1f);
        Vec3 look = player.getLookAngle().normalize();
        double warm = Mth.clamp((double) elapsed / FIREBALL_MIN_CHARGE_TICKS, 0.0, 1.0);
        Vec3 p = eye.add(look.scale(0.4 + warm * 0.55));
        level.sendParticles(ParticleTypes.DRAGON_BREATH, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.006);
        if ((elapsed & 7) < 3) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
    }

    private static void applyBreathBeam(ServerLevel level, ServerPlayer player) {
        Vec3 start = player.getEyePosition(1f);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(BREATH_RANGE));
        HitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 tip = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        spawnBreathParticles(level, start, tip);
        AABB sweep = new AABB(start, tip).inflate(BREATH_RAY_HALF_WIDTH + 0.35);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep)) {
            if (!target.isAlive() || target == player || !target.isPickable()) {
                continue;
            }
            Vec3 c = target.getBoundingBox().getCenter();
            if (distancePointToSegmentSqr(c, start, tip) > BREATH_RAY_HALF_WIDTH * BREATH_RAY_HALF_WIDTH) {
                continue;
            }
            double along = c.subtract(start).dot(look);
            if (along < 0 || along > BREATH_RANGE + 0.5) {
                continue;
            }
            target.hurt(level.damageSources().dragonBreath(), BREATH_DAMAGE_PER_TICK);
        }
    }

    private static void spawnBreathParticles(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 dir = end.subtract(start);
        double len = dir.length();
        if (len < 0.08) {
            return;
        }
        Vec3 n = dir.scale(1.0 / len);
        double step = 0.62;
        int steps = Mth.clamp((int) (len / step), 3, 28);
        for (int i = 0; i < steps; i++) {
            Vec3 p = start.add(n.scale(i * step));
            if ((i & 1) == 0) {
                level.sendParticles(ParticleTypes.DRAGON_BREATH, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.004);
            } else {
                level.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.002);
            }
        }
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

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer sp)) {
            return;
        }
        int mode = getMode(stack);
        if (mode == MODE_FIREBALL) {
            int used = DRAW_DURATION - timeLeft;
            if (used < FIREBALL_MIN_CHARGE_TICKS) {
                if (used > 3) {
                    sp.displayClientMessage(Component.translatable("message.bmcmod.dragon_staff.fireball_cancelled").withStyle(ChatFormatting.GRAY), true);
                }
                return;
            }
            if (stack.getDamageValue() >= stack.getMaxDamage()) {
                return;
            }
            if (!sp.getAbilities().instabuild && sp.getCooldowns().isOnCooldown(this)) {
                return;
            }
            shootDragonFireball(sp);
            if (!sp.getAbilities().instabuild) {
                EquipmentSlot slot = sp.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                for (int i = 0; i < COST_FIREBALL; i++) {
                    if (stack.getDamageValue() >= stack.getMaxDamage()) {
                        break;
                    }
                    stack.hurtAndBreak(1, sp, slot);
                }
                sp.getCooldowns().addCooldown(this, CD_FIREBALL);
            }
            sp.awardStat(Stats.ITEM_USED.get(this));
            return;
        }
        // Mode souffle : fin du maintien
        sp.awardStat(Stats.ITEM_USED.get(this));
        if (!sp.getAbilities().instabuild) {
            sp.getCooldowns().addCooldown(this, CD_BREATH_RELEASE);
        }
    }

    private static void shootDragonFireball(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 spawn = player.getEyePosition(1f).add(look.scale(0.65));
        // Le 3e argument est la direction / vélocité initiale, pas un point monde (sinon trajectoire figée).
        DragonFireball ball = new DragonFireball(level, player, look);
        ball.setPos(spawn.x, spawn.y, spawn.z);
        level.addFreshEntity(ball);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 0.7F, 0.85F + level.random.nextFloat() * 0.15F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!StaffTooltip.showDetailedLines(tooltip, flag)) {
            return;
        }
        int mode = getMode(stack);
        tooltip.add(Component.translatable("item.bmcmod.dragon_staff.mode_line", Component.translatable("item.bmcmod.dragon_staff.mode." + mode))
                .withStyle(ChatFormatting.DARK_PURPLE));
        int left = stack.getMaxDamage() - stack.getDamageValue();
        tooltip.add(Component.translatable("item.bmcmod.dragon_staff.fuel", left, stack.getMaxDamage()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bmcmod.dragon_staff.usage").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.bmcmod.dragon_staff.mode_switch").withStyle(ChatFormatting.DARK_GRAY));
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
        return Mth.hsvToRgb(0.88F - f * 0.08F, 0.9F, 1.0F);
    }

    @Override
    public boolean canFitInsideContainerItems(ItemStack stack) {
        return false;
    }
}
