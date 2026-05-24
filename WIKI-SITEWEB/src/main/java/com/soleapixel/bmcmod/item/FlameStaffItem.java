package com.soleapixel.bmcmod.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.rarity.BmcModRarity;
import com.soleapixel.bmcmod.rarity.RarityStickItem;

/**
 * Bâton de flammes : maintien du clic = cône de feu dans la direction du regard ; dégâts continus + brûlure.
 * La durabilité diminue pendant l’utilisation (pas seulement au relâchement).
 */
public final class FlameStaffItem extends RarityStickItem {
    /** Durée d’utilisation « illimitée » tant que le joueur maintient (comme l’Echo Staff). */
    public static final int USE_DURATION = 72_000;
    /** Portée du jet (blocs). */
    public static final double CONE_RANGE = 13.5;
    /** Demi-largeur du cône au bout du segment (approximation par tube le long du rayon). */
    public static final double RAY_HALF_WIDTH = 1.22;
    /** Dégâts par tick serveur sur une cible dans le cône. */
    public static final float DAMAGE_PER_TICK = 0.26F;
    /** Dégâts bonus si la cible brûle déjà (jet continu). */
    public static final float BONUS_DAMAGE_IF_ON_FIRE = 0.14F;
    /** Augmentation des ticks de feu par tick tant que la cible reste dans le jet (plafonné). */
    public static final int FIRE_TICKS_ADD_PER_TICK = 18;
    public static final int FIRE_TICKS_CAP = 12 * 20;
    /** Perte de 1 durabilité tous les N ticks d’utilisation (hors créatif). */
    public static final int DURABILITY_DRAIN_INTERVAL = 2;
    /** Durabilité max : ~40 s de jet continu à 20 t/s avec drain 1/2 ticks → 400 points. */
    public static final int MAX_USES = 400;

    public FlameStaffItem(Properties properties) {
        super(properties, BmcModRarity.EXOTIC);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
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
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            sp.stopUsingItem();
            return;
        }
        applyFlameCone(sl, sp, stack);
        if (!sp.getAbilities().instabuild && (USE_DURATION - remainingUseDuration) % DURABILITY_DRAIN_INTERVAL == 0) {
            EquipmentSlot slot = sp.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            stack.hurtAndBreak(1, sp, slot);
        }
        if (stack.isEmpty() || stack.getDamageValue() >= stack.getMaxDamage()) {
            sp.stopUsingItem();
            return;
        }
        if (sl.getGameTime() % 5L == 0L) {
            sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.12F, 0.75F + sl.random.nextFloat() * 0.35F);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer sp)) {
            return;
        }
        sp.awardStat(Stats.ITEM_USED.get(this));
    }

    private static void applyFlameCone(ServerLevel level, ServerPlayer player, ItemStack stack) {
        Vec3 start = player.getEyePosition(1f);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(CONE_RANGE));
        HitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 tip = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        spawnFlameParticles(level, start, tip);
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            spawnBlockFireTrail(level, look, tip);
        }
        AABB sweep = new AABB(start, tip).inflate(RAY_HALF_WIDTH + 0.35);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep)) {
            if (!target.isAlive() || target == player || !target.isPickable()) {
                continue;
            }
            Vec3 c = target.getBoundingBox().getCenter();
            if (distancePointToSegmentSqr(c, start, tip) > RAY_HALF_WIDTH * RAY_HALF_WIDTH) {
                continue;
            }
            double along = c.subtract(start).dot(look);
            if (along < 0 || along > CONE_RANGE + 0.5) {
                continue;
            }
            float dmg = DAMAGE_PER_TICK;
            if (target.getRemainingFireTicks() > 0) {
                dmg += BONUS_DAMAGE_IF_ON_FIRE;
            }
            target.hurt(level.damageSources().playerAttack(player), dmg);
            if (!target.fireImmune()) {
                int fire = target.getRemainingFireTicks();
                target.setRemainingFireTicks(Math.min(FIRE_TICKS_CAP, Math.max(fire, 20) + FIRE_TICKS_ADD_PER_TICK));
            }
        }
    }

    private static void spawnFlameParticles(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 dir = end.subtract(start);
        double len = dir.length();
        if (len < 0.08) {
            return;
        }
        Vec3 n = dir.scale(1.0 / len);
        double step = 0.58;
        int steps = Mth.clamp((int) (len / step), 3, 26);
        for (int i = 0; i < steps; i++) {
            Vec3 p = start.add(n.scale(i * step));
            level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.025, 0.025, 0.025, 0.001);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.SMALL_FLAME, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    /** Courte traînée sur le bloc touché pour marquer l’impact sans saturer l’écran. */
    private static void spawnBlockFireTrail(ServerLevel level, Vec3 look, Vec3 tip) {
        Vec3 back = look.scale(-1.0);
        for (int i = 0; i < 7; i++) {
            Vec3 p = tip.add(back.scale(i * 0.11));
            level.sendParticles(ParticleTypes.SMALL_FLAME, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.0);
            if ((i & 1) == 0) {
                level.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.003);
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!StaffTooltip.showDetailedLines(tooltip, flag)) {
            return;
        }
        int left = stack.getMaxDamage() - stack.getDamageValue();
        tooltip.add(Component.translatable("item.bmcmod.flame_staff.fuel", left, stack.getMaxDamage()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bmcmod.flame_staff.usage").withStyle(ChatFormatting.GRAY));
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
        return Mth.hsvToRgb(0.08F - f * 0.08F, 0.95F, 1.0F);
    }
}
