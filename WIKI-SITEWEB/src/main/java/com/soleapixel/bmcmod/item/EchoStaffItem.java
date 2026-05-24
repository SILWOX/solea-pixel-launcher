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
import net.minecraft.world.level.GameRules;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.rarity.BmcModRarity;
import com.soleapixel.bmcmod.rarity.RarityStickItem;

/**
 * Bâton d’écho : maintien 5 s (animation arc), relâcher envoie une onde type sonic boom du Warden.
 * 6 charges (durabilité) ; cooldown sur la barre d’action rapide après chaque tir.
 */
public final class EchoStaffItem extends RarityStickItem {
    /** Durée maximale de maintien (comme un arc) ; le déclenchement exige {@link #CHARGE_TICKS} ticks réels. */
    public static final int DRAW_DURATION = 72_000;
    /** 4.2 secondes à 20 t/s : plus réactif sans casser l'équilibrage. */
    public static final int CHARGE_TICKS = 84;
    /**
     * Dégâts de l’onde (même {@link net.minecraft.world.damagesource.DamageTypes#SONIC_BOOM} que le Warden).
     * Le warden vanilla applique {@code 10f} (5 cœurs) ; ici valeur relevée pour que l’arme seulement
     * quelques charges / 5 s de canalisation ait un impact nettement ressenti en combat.
     */
    /** Dégâts par cible touchée par le rayon (onde + impact visuel renforcés). */
    public static final float BLAST_DAMAGE = 55.0F;
    public static final double BLAST_RANGE = 26.0;
    /** Demi-largeur du « tube » de détection : plus large = plus de créatures touchées par l’onde. */
    public static final double RAY_HALF_WIDTH = 2.75;
    /** Cooldown après un tir réussi (barre grise sur le slot). */
    public static final int POST_FIRE_COOLDOWN_TICKS = 5 * 20;

    public EchoStaffItem(Properties properties) {
        super(properties, BmcModRarity.EXOTIC);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild && player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
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
        int elapsed = getUseDuration(stack, entity) - remainingUseDuration;
        if (level instanceof ServerLevel sl && entity instanceof ServerPlayer sp) {
            spawnChargeParticles(sl, sp, stack, elapsed);
            if (elapsed > 0 && elapsed % 10 == 0 && elapsed < CHARGE_TICKS) {
                int pct = Mth.clamp((int) (100L * elapsed / CHARGE_TICKS), 0, 99);
                sp.displayClientMessage(
                        Component.translatable("message.bmcmod.echo_staff.charging", pct).withStyle(ChatFormatting.DARK_AQUA),
                        true);
            }
            if (elapsed == CHARGE_TICKS) {
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.2F, 1.0F);
                sp.displayClientMessage(
                        Component.translatable("message.bmcmod.echo_staff.ready").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                        true);
            }
        }
    }

    private static void spawnChargeParticles(ServerLevel level, ServerPlayer player, ItemStack stack, int elapsed) {
        Vec3 eye = player.getEyePosition(1f);
        Vec3 look = player.getLookAngle().normalize();
        double warm = Mth.clamp((double) elapsed / CHARGE_TICKS, 0.0, 1.0);
        // Anneau d’âmes : plan éloigné des yeux + moins de points = moins de masque à l’écran en mouvement / recul.
        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 right = look.cross(worldUp);
        if (right.lengthSqr() < 1.0E-4) {
            right = look.cross(new Vec3(1.0, 0.0, 0.0));
        }
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();
        Vec3 center = eye.add(look.scale(0.45 + warm * 0.58));
        double baseRadius = 0.72 + warm * 1.05;
        int points = 8 + (int) (warm * 10);
        double spin = elapsed * 0.038;
        if ((elapsed & 1) == 0) {
            for (int i = 0; i < points; i++) {
                double ang = Mth.TWO_PI * i / points + spin;
                double wobble = (level.random.nextDouble() - 0.5) * 0.035;
                double r = baseRadius + wobble;
                Vec3 p = center.add(right.scale(Mth.cos((float) ang) * r)).add(up.scale(Mth.sin((float) ang) * r));
                level.sendParticles(ParticleTypes.SCULK_SOUL, p.x, p.y, p.z, 1, 0.01, 0.01, 0.01, 0.006);
            }
        }
        if (warm > 0.78) {
            double innerR = baseRadius * 0.52;
            int innerPts = 5 + (int) (warm * 5);
            for (int i = 0; i < innerPts; i++) {
                double ang = Mth.TWO_PI * i / innerPts - spin * 0.5;
                Vec3 p = center.add(right.scale(Mth.cos((float) ang) * innerR)).add(up.scale(Mth.sin((float) ang) * innerR));
                level.sendParticles(ParticleTypes.SCULK_SOUL, p.x, p.y, p.z, 1, 0.006, 0.006, 0.006, 0.004);
            }
        }
        if (elapsed >= CHARGE_TICKS && stack == player.getUseItem() && level.getGameTime() % 5L == 0L) {
            double ar = baseRadius + 0.04;
            for (int k = 0; k < 7; k++) {
                double ang = Mth.TWO_PI * k / 7.0 + spin;
                Vec3 p = center.add(right.scale(Mth.cos((float) ang) * ar)).add(up.scale(Mth.sin((float) ang) * ar));
                level.sendParticles(ParticleTypes.ENCHANT, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.08);
            }
        }
        spawnChargeAuraBehind(level, player, warm, elapsed);
    }

    /** Gros effets (sonic + sculk) <strong>derrière</strong> le joueur : aura de puissance sans masquer la visée. */
    private static void spawnChargeAuraBehind(ServerLevel level, ServerPlayer player, double warm, int elapsed) {
        Vec3 look = player.getLookAngle();
        Vec3 back = echoStaffHorizontalBack(look);
        Vec3 right = new Vec3(-back.z, 0.0, back.x);
        if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
        }
        right = right.normalize();
        Vec3 feet = player.position();
        int sonic = elapsed >= CHARGE_TICKS ? 3 : 1 + (int) (warm * 2);
        for (int i = 0; i < sonic; i++) {
            double dist = 0.62 + level.random.nextDouble() * (0.5 + warm * 0.55);
            double side = (level.random.nextDouble() - 0.5) * (0.38 + warm * 0.42);
            double height = 0.2 + level.random.nextDouble() * (1.05 + warm * 0.65);
            Vec3 p = feet.add(0.0, height, 0.0).add(back.scale(dist)).add(right.scale(side));
            level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.06, 0.04, 0.06, 0.0);
        }
        int souls = 4 + (int) (warm * 7) + (elapsed >= CHARGE_TICKS ? 6 : 0);
        for (int i = 0; i < souls; i++) {
            double dist = 0.52 + level.random.nextDouble() * (0.5 + warm * 0.45);
            double side = (level.random.nextDouble() - 0.5) * (0.62 + warm * 0.38);
            double height = 0.15 + level.random.nextDouble() * (1.2 + warm * 0.45);
            Vec3 p = feet.add(0.0, height, 0.0).add(back.scale(dist)).add(right.scale(side));
            level.sendParticles(ParticleTypes.SCULK_SOUL, p.x, p.y, p.z, 1, 0.025, 0.018, 0.025, 0.014);
        }
        if (elapsed >= CHARGE_TICKS - 8 && level.getGameTime() % 4L == 0L) {
            for (int r = 0; r < 4; r++) {
                double dist = 0.68 + r * 0.14;
                double ang = level.random.nextDouble() * Mth.TWO_PI;
                Vec3 p = feet.add(0.0, 0.35 + r * 0.24, 0.0).add(back.scale(dist)).add(right.scale(Mth.cos((float) ang) * 0.2));
                level.sendParticles(ParticleTypes.ENCHANT, p.x, p.y, p.z, 1, 0.04, 0.06, 0.04, 0.12);
            }
        }
    }

    private static Vec3 echoStaffHorizontalBack(Vec3 look) {
        Vec3 b = new Vec3(-look.x, 0.0, -look.z);
        if (b.lengthSqr() < 1.0E-6) {
            b = new Vec3(0.0, 0.0, 1.0);
        }
        return b.normalize();
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer sp)) {
            return;
        }
        int used = getUseDuration(stack, entity) - timeLeft;
        if (used < CHARGE_TICKS) {
            if (used > 4) {
                sp.displayClientMessage(Component.translatable("message.bmcmod.echo_staff.cancelled").withStyle(ChatFormatting.GRAY), true);
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.WARDEN_NEARBY_CLOSE, SoundSource.PLAYERS, 0.35F, 1.4F);
            }
            return;
        }
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            return;
        }
        if (!sp.getAbilities().instabuild && sp.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }
        fireEchoBlast(sp, stack);
        if (!sp.getAbilities().instabuild) {
            EquipmentSlot slot = sp.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            stack.hurtAndBreak(1, sp, slot);
        }
        sp.awardStat(Stats.ITEM_USED.get(this));
        if (!sp.getAbilities().instabuild) {
            sp.getCooldowns().addCooldown(stack.getItem(), POST_FIRE_COOLDOWN_TICKS);
        }
    }

    private void fireEchoBlast(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition(1f);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(BLAST_RANGE));
        HitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 visualEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        drawBlastParticles(level, start, visualEnd);
        spawnBlastImpact(level, player, visualEnd, blockHit);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.35F, 0.88F + level.random.nextFloat() * 0.1F);
        AABB sweep = new AABB(start, end).inflate(RAY_HALF_WIDTH + 0.65);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep)) {
            if (!target.isAlive() || target == player || !target.isPickable()) {
                continue;
            }
            if (target.isInvulnerableTo(level.damageSources().sonicBoom(player))) {
                continue;
            }
            Vec3 c = target.getBoundingBox().getCenter();
            if (distancePointToSegmentSqr(c, start, end) > RAY_HALF_WIDTH * RAY_HALF_WIDTH) {
                continue;
            }
            double along = c.subtract(start).dot(look);
            if (along < 0 || along > BLAST_RANGE + 1) {
                continue;
            }
            target.hurt(level.damageSources().sonicBoom(player), BLAST_DAMAGE);
            if (!target.isAlive()) {
                spawnGroundSculkBloom(level, target);
            }
        }
    }

    /** Gerbe sculk au sol quand l’onde tue une créature (effet « propagation »). */
    private static void spawnGroundSculkBloom(ServerLevel level, LivingEntity victim) {
        double cx = victim.getX();
        double cz = victim.getZ();
        int bx = Mth.floor(cx);
        int bz = Mth.floor(cz);
        double y;
        if (level.dimensionType().hasSkyLight()) {
            y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz) + 0.1;
        } else {
            y = victim.getBoundingBox().minY + 0.08;
        }
        double baseX = bx + 0.5;
        double baseZ = bz + 0.5;
        for (int ring = 0; ring < 4; ring++) {
            double rad = 0.2 + ring * 0.28 + level.random.nextDouble() * 0.12;
            int pts = 10 + ring * 4;
            for (int i = 0; i < pts; i++) {
                double ang = Mth.TWO_PI * i / pts + level.random.nextDouble() * 0.15;
                double px = baseX + Mth.cos((float) ang) * rad;
                double pz = baseZ + Mth.sin((float) ang) * rad;
                level.sendParticles(ParticleTypes.SCULK_SOUL, px, y, pz, 1, 0.12, 0.02, 0.12, 0.025);
            }
        }
        for (int k = 0; k < 20; k++) {
            double ox = (level.random.nextDouble() - 0.5) * 1.4;
            double oz = (level.random.nextDouble() - 0.5) * 1.4;
            level.sendParticles(ParticleTypes.SCULK_SOUL, baseX + ox, y, baseZ + oz, 1, 0.08, 0.015, 0.08, 0.02);
        }
        for (int k = 0; k < 6; k++) {
            double ox = (level.random.nextDouble() - 0.5) * 0.9;
            double oz = (level.random.nextDouble() - 0.5) * 0.9;
            level.sendParticles(ParticleTypes.ENCHANT, baseX + ox, y + 0.04, baseZ + oz, 1, 0.06, 0.0, 0.06, 0.08);
        }
        if (level.random.nextFloat() < 0.35f) {
            level.sendParticles(ParticleTypes.SONIC_BOOM, baseX, y + 0.05, baseZ, 1, 0.2, 0.0, 0.2, 0.0);
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

    private static void drawBlastParticles(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 dir = end.subtract(start);
        double len = dir.length();
        if (len < 0.05) {
            return;
        }
        Vec3 n = dir.normalize();
        Vec3 step = n.scale(0.26);
        int steps = Mth.clamp((int) (len / 0.26), 6, 110);
        Vec3 p = start;
        for (int i = 0; i < steps; i++) {
            level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.0);
            level.sendParticles(ParticleTypes.SCULK_SOUL, p.x, p.y, p.z, 3, 0.1, 0.1, 0.1, 0.03);
            p = p.add(step);
        }
        // Impact : onde « explosion » sculk (dome + rafale)
        for (int k = 0; k < 28; k++) {
            double ox = (level.random.nextDouble() - 0.5) * 1.15;
            double oy = (level.random.nextDouble() - 0.5) * 1.15;
            double oz = (level.random.nextDouble() - 0.5) * 1.15;
            level.sendParticles(ParticleTypes.SONIC_BOOM, end.x + ox, end.y + oy, end.z + oz, 1, 0.12, 0.12, 0.12, 0.0);
        }
        for (int k = 0; k < 40; k++) {
            level.sendParticles(
                    ParticleTypes.SCULK_SOUL,
                    end.x + (level.random.nextDouble() - 0.5) * 1.25,
                    end.y + (level.random.nextDouble() - 0.5) * 1.25,
                    end.z + (level.random.nextDouble() - 0.5) * 1.25,
                    1,
                    0.18,
                    0.18,
                    0.18,
                    0.045);
        }
        for (int ring = 0; ring < 5; ring++) {
            double rad = 0.15 + ring * 0.22;
            int pts = 12 + ring * 3;
            for (int i = 0; i < pts; i++) {
                double ang = Mth.TWO_PI * i / pts;
                double px = end.x + Mth.cos((float) ang) * rad;
                double pz = end.z + Mth.sin((float) ang) * rad;
                level.sendParticles(ParticleTypes.SCULK_SOUL, px, end.y + ring * 0.08, pz, 1, 0.06, 0.04, 0.06, 0.02);
            }
        }
    }

    /** Son + particules d’impact ; propagation sculk sur blocs remplaçables si mobGriefing. */
    private static void spawnBlastImpact(ServerLevel level, ServerPlayer player, Vec3 impact, HitResult blockHit) {
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.SCULK_BLOCK_BREAK, SoundSource.PLAYERS, 1.35F, 0.55F + level.random.nextFloat() * 0.25F);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.6F, 1.6F + level.random.nextFloat() * 0.15F);
        for (int k = 0; k < 14; k++) {
            double ox = (level.random.nextDouble() - 0.5) * 1.4;
            double oy = (level.random.nextDouble() - 0.5) * 1.4;
            double oz = (level.random.nextDouble() - 0.5) * 1.4;
            level.sendParticles(ParticleTypes.SONIC_BOOM, impact.x + ox, impact.y + oy, impact.z + oz, 1, 0.15, 0.15, 0.15, 0.0);
        }
        for (int k = 0; k < 32; k++) {
            level.sendParticles(
                    ParticleTypes.SCULK_SOUL,
                    impact.x + (level.random.nextDouble() - 0.5) * 1.6,
                    impact.y + (level.random.nextDouble() - 0.5) * 1.6,
                    impact.z + (level.random.nextDouble() - 0.5) * 1.6,
                    1,
                    0.2,
                    0.2,
                    0.2,
                    0.05);
        }
        if (blockHit.getType() == HitResult.Type.BLOCK && blockHit instanceof BlockHitResult bhr) {
            spreadSculkAtBlastHit(level, player, bhr.getBlockPos());
        }
    }

    private static void spreadSculkAtBlastHit(ServerLevel level, ServerPlayer player, BlockPos hitPos) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && !player.getAbilities().instabuild) {
            return;
        }
        int radius = 4;
        int placed = 0;
        int budget = 88;
        for (int dx = -radius; dx <= radius && placed < budget; dx++) {
            for (int dz = -radius; dz <= radius && placed < budget; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                for (int dy = -3; dy <= 4 && placed < budget; dy++) {
                    BlockPos pos = hitPos.offset(dx, dy, dz);
                    if (!level.isInWorldBounds(pos)) {
                        continue;
                    }
                    if (!player.mayInteract(level, pos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (!state.is(BlockTags.SCULK_REPLACEABLE)) {
                        continue;
                    }
                    float edge = (float) (dx * dx + dz * dz) / (float) (radius * radius + 1);
                    if (level.random.nextFloat() > 0.58f - edge * 0.22f) {
                        continue;
                    }
                    BlockState sculk = Blocks.SCULK.defaultBlockState();
                    if (level.setBlock(pos, sculk, Block.UPDATE_ALL)) {
                        placed++;
                    }
                }
            }
        }
        tryPlaceEchoBlastSculkSensors(level, player, hitPos, radius);
    }

    /** 0 à 3 capteurs sculk sur des appuis valides près de l’impact (aléatoire). */
    private static void tryPlaceEchoBlastSculkSensors(ServerLevel level, ServerPlayer player, BlockPos hitPos, int radius) {
        float u = level.random.nextFloat();
        int maxSensors = u < 0.5f ? 0 : (u < 0.88f ? 1 : 2);
        if (maxSensors <= 0) {
            return;
        }
        int placed = 0;
        for (int attempt = 0; attempt < 72 && placed < maxSensors; attempt++) {
            int dx = level.random.nextInt(radius * 2 + 1) - radius;
            int dz = level.random.nextInt(radius * 2 + 1) - radius;
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }
            int y = hitPos.getY() + level.random.nextInt(7) - 3;
            BlockPos pos = new BlockPos(hitPos.getX() + dx, y, hitPos.getZ() + dz);
            if (!level.isInWorldBounds(pos)) {
                continue;
            }
            if (!player.mayInteract(level, pos) || !player.mayInteract(level, pos.below())) {
                continue;
            }
            if (!level.getBlockState(pos).isAir()) {
                continue;
            }
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (!belowState.isFaceSturdy(level, below, Direction.UP)) {
                continue;
            }
            BlockState sensor = Blocks.SCULK_SENSOR.defaultBlockState();
            if (!sensor.canSurvive(level, pos)) {
                continue;
            }
            if (level.random.nextFloat() > 0.42f) {
                continue;
            }
            if (level.setBlock(pos, sensor, Block.UPDATE_ALL)) {
                placed++;
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!StaffTooltip.showDetailedLines(tooltip, flag)) {
            return;
        }
        int left = stack.getMaxDamage() - stack.getDamageValue();
        tooltip.add(Component.translatable("item.bmcmod.echo_staff.charges", left, stack.getMaxDamage()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bmcmod.echo_staff.usage").withStyle(ChatFormatting.GRAY));
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
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }
}
