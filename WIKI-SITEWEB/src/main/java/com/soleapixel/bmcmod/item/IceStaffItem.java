package com.soleapixel.bmcmod.item;

import java.util.List;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.gameplay.IceStaffGolemEvents;
import com.soleapixel.bmcmod.rarity.BmcModRarity;
import com.soleapixel.bmcmod.rarity.RarityStickItem;

public final class IceStaffItem extends RarityStickItem {
    public static final String MODE_KEY = "IceStaffMode";
    public static final int MODE_BRIDGE = 1;
    public static final int MODE_ATTACK = 2;
    public static final int MODE_GOLEMS = 3;

    public static final int USE_DURATION = 72_000;
    public static final int MAX_USES = 420;
    public static final int GOLEM_COUNT = 3;
    public static final int GOLEM_LIFETIME_TICKS = 20 * 90;

    private static final int COST_BRIDGE = 6;
    private static final int COST_ATTACK_DRAIN_INTERVAL = 2;
    private static final int COST_GOLEMS = 30;
    private static final int CD_BRIDGE = 30;
    private static final int CD_GOLEMS = 200;
    private static final int CD_ATTACK_RELEASE = 8;

    private static final int BRIDGE_LENGTH = 18;
    private static final int BRIDGE_HALF_WIDTH_BASE = 1;
    private static final int BRIDGE_HALF_WIDTH_MAX = 2;

    private static final double ATTACK_RANGE = 14.0;
    private static final double ATTACK_HALF_WIDTH = 1.18;
    private static final float ATTACK_DAMAGE_PER_TICK = 0.24F;
    private static final int ATTACK_SLOW_TICKS = 60;

    public IceStaffItem(Properties properties) {
        super(properties, BmcModRarity.EXOTIC);
    }

    public static int getMode(ItemStack stack) {
        int m = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(MODE_KEY);
        return m >= MODE_BRIDGE && m <= MODE_GOLEMS ? m : MODE_BRIDGE;
    }

    public static void setMode(ItemStack stack, int mode) {
        int m = Mth.clamp(mode, MODE_BRIDGE, MODE_GOLEMS);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(MODE_KEY, m);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void cycleMode(ItemStack stack) {
        int next = getMode(stack) == MODE_GOLEMS ? MODE_BRIDGE : getMode(stack) + 1;
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
        if (mode == MODE_ATTACK) {
            sp.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        boolean ok = mode == MODE_BRIDGE ? makeIceBridge(sp) : spawnIceGolems(sp);
        if (!ok) {
            return InteractionResultHolder.fail(stack);
        }
        if (!sp.getAbilities().instabuild) {
            int cost = mode == MODE_BRIDGE ? COST_BRIDGE : COST_GOLEMS;
            int cd = mode == MODE_BRIDGE ? CD_BRIDGE : CD_GOLEMS;
            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            for (int i = 0; i < cost; i++) {
                if (stack.getDamageValue() >= stack.getMaxDamage()) {
                    break;
                }
                stack.hurtAndBreak(1, sp, slot);
            }
            sp.getCooldowns().addCooldown(this, cd);
        }
        sp.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.success(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
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
        if (getMode(stack) != MODE_ATTACK) {
            return;
        }
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            sp.stopUsingItem();
            return;
        }
        applyIceBeam(sl, sp);
        int elapsed = USE_DURATION - remainingUseDuration;
        if (!sp.getAbilities().instabuild && elapsed % COST_ATTACK_DRAIN_INTERVAL == 0) {
            EquipmentSlot slot = sp.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            stack.hurtAndBreak(1, sp, slot);
        }
        if (stack.isEmpty() || stack.getDamageValue() >= stack.getMaxDamage()) {
            sp.stopUsingItem();
            return;
        }
        if (sl.getGameTime() % 6L == 0L) {
            sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.POWDER_SNOW_HIT, SoundSource.PLAYERS, 0.14F, 0.8F + sl.random.nextFloat() * 0.25F);
            sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.1F, 0.95F + sl.random.nextFloat() * 0.15F);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer sp)) {
            return;
        }
        if (getMode(stack) == MODE_ATTACK && !sp.getAbilities().instabuild) {
            sp.getCooldowns().addCooldown(this, CD_ATTACK_RELEASE);
        }
        sp.awardStat(Stats.ITEM_USED.get(this));
    }

    private static boolean makeIceBridge(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 dir = player.getLookAngle().normalize();
        if (dir.lengthSqr() < 1.0E-6) {
            return false;
        }
        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 right = dir.cross(worldUp);
        if (right.lengthSqr() < 1.0E-6) {
            // Fallback when looking almost straight up/down.
            right = new Vec3(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }

        Vec3 start = player.position().add(0.0, 0.15, 0.0);
        int placed = 0;
        for (int i = 1; i <= BRIDGE_LENGTH; i++) {
            Vec3 center = start.add(dir.scale(i));
            int halfWidth = bridgeHalfWidthAt(i);
            for (int w = -halfWidth; w <= halfWidth; w++) {
                Vec3 p = center.add(right.scale(w));
                BlockPos pos = BlockPos.containing(p);
                if (tryPlaceBridgeIce(level, pos)) {
                    placed++;
                }
            }
        }
        if (placed > 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.6F, 0.85F);
            level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 0.9D, player.getZ(), 26, 0.6D, 0.4D, 0.6D, 0.02D);
            return true;
        }
        return false;
    }

    private static int bridgeHalfWidthAt(int step) {
        if (step >= 7 && step <= 14) {
            return BRIDGE_HALF_WIDTH_MAX;
        }
        if ((step & 3) == 0) {
            return BRIDGE_HALF_WIDTH_MAX;
        }
        return BRIDGE_HALF_WIDTH_BASE;
    }

    private static boolean tryPlaceBridgeIce(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        boolean canReplace = state.isAir() || state.canBeReplaced();
        if (!canReplace) {
            return false;
        }
        level.setBlockAndUpdate(pos, Blocks.PACKED_ICE.defaultBlockState());
        IceStaffGolemEvents.registerTempBridgeBlock(level, pos, state, Mth.nextInt(level.random, 70, 150));
        return true;
    }

    private static void applyIceBeam(ServerLevel level, ServerPlayer player) {
        Vec3 start = player.getEyePosition(1f);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(ATTACK_RANGE));
        HitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 tip = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        spawnIceParticles(level, start, tip);
        AABB sweep = new AABB(start, tip).inflate(ATTACK_HALF_WIDTH + 0.35);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep)) {
            if (!target.isAlive() || target == player || !target.isPickable()) {
                continue;
            }
            Vec3 c = target.getBoundingBox().getCenter();
            if (distancePointToSegmentSqr(c, start, tip) > ATTACK_HALF_WIDTH * ATTACK_HALF_WIDTH) {
                continue;
            }
            double along = c.subtract(start).dot(look);
            if (along < 0 || along > ATTACK_RANGE + 0.5) {
                continue;
            }
            target.hurt(level.damageSources().playerAttack(player), ATTACK_DAMAGE_PER_TICK);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ATTACK_SLOW_TICKS, 1, true, true));
        }
    }

    private static void spawnIceParticles(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 dir = end.subtract(start);
        double len = dir.length();
        if (len < 0.08) {
            return;
        }
        Vec3 n = dir.scale(1.0 / len);
        int steps = Mth.clamp((int) (len / 0.55), 3, 28);
        for (int i = 0; i < steps; i++) {
            Vec3 p = start.add(n.scale(i * 0.55));
            level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.001);
            if ((i & 1) == 0) {
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.0);
            }
        }
    }

    private static boolean spawnIceGolems(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        UUID owner = player.getUUID();
        int existing = countOwnedIceGolems(level, owner);
        int allowed = Math.max(0, GOLEM_COUNT - existing);
        if (allowed <= 0) {
            return false;
        }
        long expiresAt = level.getGameTime() + GOLEM_LIFETIME_TICKS;
        int spawned = 0;
        for (int i = 0; i < allowed; i++) {
            Vec3 pos = findSummonPos(level, player, i);
            SnowGolem golem = EntityType.SNOW_GOLEM.create(level);
            if (golem == null) {
                continue;
            }
            golem.setPos(pos.x, pos.y, pos.z);
            golem.setPumpkin(false);
            golem.setPersistenceRequired();
            golem.setCanPickUpLoot(false);
            golem.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40.0D);
            golem.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(1.28D);
            golem.setHealth(40.0F);
            golem.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, GOLEM_LIFETIME_TICKS, 1, false, false));
            golem.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, GOLEM_LIFETIME_TICKS, 0, false, false));
            golem.getPersistentData().putUUID(IceStaffGolemEvents.TAG_OWNER, owner);
            golem.getPersistentData().putLong(IceStaffGolemEvents.TAG_EXPIRES_AT, expiresAt);
            if (level.addFreshEntity(golem)) {
                spawned++;
                level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 1.1, pos.z, 20, 0.3, 0.5, 0.3, 0.02);
            }
        }
        if (spawned > 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOW_GOLEM_AMBIENT, SoundSource.PLAYERS, 0.7F, 0.95F);
            return true;
        }
        return false;
    }

    private static int countOwnedIceGolems(ServerLevel level, UUID owner) {
        int count = 0;
        for (SnowGolem golem : level.getEntitiesOfClass(SnowGolem.class, new AABB(
                -3.0E7, -1024.0, -3.0E7,
                3.0E7, 2048.0, 3.0E7))) {
            if (!golem.isAlive() || !golem.getPersistentData().hasUUID(IceStaffGolemEvents.TAG_OWNER)) {
                continue;
            }
            if (owner.equals(golem.getPersistentData().getUUID(IceStaffGolemEvents.TAG_OWNER))) {
                count++;
            }
        }
        return count;
    }

    private static Vec3 findSummonPos(ServerLevel level, ServerPlayer player, int index) {
        Vec3 base = player.position();
        double angle = (Mth.TWO_PI * index) / Math.max(GOLEM_COUNT, 1);
        for (int attempt = 0; attempt < 8; attempt++) {
            double r = 2.0 + attempt * 0.45;
            double x = base.x + Mth.cos((float) angle) * r;
            double z = base.z + Mth.sin((float) angle) * r;
            double y = player.getY();
            if (level.noCollision(EntityType.SNOW_GOLEM.getDimensions().makeBoundingBox(x, y, z))) {
                return new Vec3(x, y, z);
            }
        }
        return base.add(0.0, 0.1, 0.0);
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!StaffTooltip.showDetailedLines(tooltip, flag)) {
            return;
        }
        int mode = getMode(stack);
        tooltip.add(Component.translatable("item.bmcmod.ice_staff.mode_line", Component.translatable("item.bmcmod.ice_staff.mode." + mode))
                .withStyle(ChatFormatting.DARK_AQUA));
        int left = stack.getMaxDamage() - stack.getDamageValue();
        tooltip.add(Component.translatable("item.bmcmod.ice_staff.fuel", left, stack.getMaxDamage()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bmcmod.ice_staff.usage").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.bmcmod.ice_staff.mode_switch").withStyle(ChatFormatting.DARK_GRAY));
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
        return Mth.hsvToRgb(0.58F - f * 0.08F, 0.72F, 0.95F);
    }
}
