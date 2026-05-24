package com.soleapixel.bmcmod.gameplay;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.registry.ModEnchantmentKeys;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class CurseOfLaunchstrikeEvents {
    private static final String ARROW_FLAG = "BmcModCurseOfLaunchstrikeArrow";
    private static final String PDC_NO_FALL_UNTIL = "bmcmod:launchstrike_no_fall_until";
    /**
     * Joueur qui « colle » à une flèche maudite : l’id entité sert à {@link ServerLevel#getEntity(int)}.
     * (Les UUID n’ont pas d’appel O(1) côté monde.)
     */
    private static final String PDC_FOLLOW_ARROW_ID = "bmcmod:launchstrike_follow_id";

    private static final int NO_FALL_TICKS = 400;
    /** Nausée : durée (ticks) + amplificateur (0 = I, 1 = II…). */
    private static final int POISON_TICKS = 300;
    private static final int NAUSEA_TICKS = 300;
    private static final int POISON_LEVEL = 1;
    private static final int NAUSEA_LEVEL = 1;
    public static final int CROSSBOW_RELOAD_COOLDOWN_TICKS = 400;
    /** Dégâts de durabilité <em>en plus</em> de ceux du tir vanilla (Défense / etc. pris en compte par le jeu). */
    private static final int LAUNCHSTRIKE_EXTRA_DURABILITY = 1;

    private CurseOfLaunchstrikeEvents() {
    }

    private static Vec3 adjustFeetOnImpact(ServerLevel level, Vec3 v) {
        BlockPos p = BlockPos.containing(v);
        for (int d = 0; d < 5; d++) {
            BlockPos t = p.above(d);
            if (isTwoHighAir(level, t)) {
                return new Vec3(t.getX() + 0.5, t.getY() + 0.01, t.getZ() + 0.5);
            }
        }
        return v;
    }

    private static boolean isTwoHighAir(ServerLevel level, BlockPos foot) {
        return isPosPassableForBody(level, foot) && isPosPassableForBody(level, foot.above());
    }

    private static boolean isPosPassableForBody(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos).blocksMotion();
    }

    private static int curseLevel(ItemStack stack, RegistryAccess reg) {
        return reg.lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantmentKeys.CURSE_OF_LAUNCHSTRIKE)
                .map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack))
                .orElse(0);
    }

    private static ItemStack firstCursedCrossbowInHands(ServerPlayer p, RegistryAccess reg) {
        ItemStack main = p.getMainHandItem();
        if (main.getItem() instanceof CrossbowItem && curseLevel(main, reg) > 0) {
            return main;
        }
        ItemStack off = p.getOffhandItem();
        if (off.getItem() instanceof CrossbowItem && curseLevel(off, reg) > 0) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    private static void clearLaunchFollow(ServerPlayer p) {
        p.getPersistentData().remove(PDC_FOLLOW_ARROW_ID);
    }

    @Nullable
    private static Integer getFollowArrowId(ServerPlayer p) {
        var tag = p.getPersistentData();
        if (!tag.contains(PDC_FOLLOW_ARROW_ID)) {
            return null;
        }
        return tag.getInt(PDC_FOLLOW_ARROW_ID);
    }

    /**
     * Même rechargement qu’en vanilla pour l’Enderpearl : barre grisée + overlay sur toutes les arbalètes du même item.
     */
    private static boolean isCrossbowItemCooldownActive(ServerPlayer p) {
        return p.getCooldowns().isOnCooldown(Items.CROSSBOW);
    }

    private static void startCrossbowReloadLikeEnderpearl(ServerPlayer p) {
        p.getCooldowns().addCooldown(Items.CROSSBOW, CROSSBOW_RELOAD_COOLDOWN_TICKS);
    }

    private static void applyExtraDurabilityOnCursedLaunchCrossbow(ServerPlayer p, RegistryAccess reg) {
        ItemStack cursed = firstCursedCrossbowInHands(p, reg);
        if (cursed.isEmpty()) {
            return;
        }
        EquipmentSlot slot = cursed == p.getMainHandItem() ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        cursed.hurtAndBreak(LAUNCHSTRIKE_EXTRA_DURABILITY, p, slot);
    }

    private static boolean isLaunchCurseOnThisShot(AbstractArrow arrow, ServerPlayer p, RegistryAccess reg) {
        if (reg.lookupOrThrow(Registries.ENCHANTMENT).get(ModEnchantmentKeys.CURSE_OF_LAUNCHSTRIKE).isEmpty()) {
            return false;
        }
        ItemStack w = arrow.getWeaponItem();
        if (w.getItem() instanceof BowItem) {
            return false;
        }
        if (w.getItem() instanceof CrossbowItem) {
            return curseLevel(w, reg) > 0;
        }
        if (w.isEmpty()) {
            return !firstCursedCrossbowInHands(p, reg).isEmpty();
        }
        if (w.is(Items.ARROW) || w.is(Items.SPECTRAL_ARROW) || w.is(Items.TIPPED_ARROW)) {
            return !firstCursedCrossbowInHands(p, reg).isEmpty();
        }
        return false;
    }

    private static void markArrowAndStartFlight(ServerPlayer p, AbstractArrow arrow) {
        RegistryAccess reg = p.level().registryAccess();
        ItemStack w = arrow.getWeaponItem();
        boolean curseOnFiring = w.getItem() instanceof CrossbowItem && curseLevel(w, reg) > 0;
        if (firstCursedCrossbowInHands(p, reg).isEmpty() && !curseOnFiring) {
            return;
        }
        if (isCrossbowItemCooldownActive(p)) {
            return;
        }
        arrow.getPersistentData().putBoolean(ARROW_FLAG, true);
        p.getPersistentData().putInt(PDC_FOLLOW_ARROW_ID, arrow.getId());
        startCrossbowReloadLikeEnderpearl(p);
        applyExtraDurabilityOnCursedLaunchCrossbow(p, reg);
    }

    @SubscribeEvent
    public static void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        if (!(arrow.getOwner() instanceof ServerPlayer p)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel sl)) {
            return;
        }
        MinecraftServer server = sl.getServer();
        if (server == null) {
            return;
        }
        RegistryAccess reg = p.level().registryAccess();
        if (!isLaunchCurseOnThisShot(arrow, p, reg)) {
            return;
        }
        markArrowAndStartFlight(p, arrow);
        server.execute(() -> {
            if (arrow.isRemoved() || !arrow.isAlive() || p.isRemoved() || p.level() != arrow.level()) {
                return;
            }
            if (arrow.getPersistentData().getBoolean(ARROW_FLAG)) {
                return;
            }
            if (!isLaunchCurseOnThisShot(arrow, p, p.level().registryAccess())) {
                return;
            }
            if (!(p.level() instanceof ServerLevel)) {
                return;
            }
            markArrowAndStartFlight(p, arrow);
        });
    }

    /**
     * Fait coller le joueur à la flèche à chaque tick (trajectoire identique jusqu’au bloc / entité).
     */
    @SubscribeEvent
    public static void onArrowChasePlayer(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        if (!arrow.getPersistentData().getBoolean(ARROW_FLAG)) {
            return;
        }
        if (!(arrow.getOwner() instanceof ServerPlayer p)) {
            return;
        }
        if (p.isRemoved() || p.level() != arrow.level() || p.level().isClientSide()) {
            return;
        }
        Integer expect = getFollowArrowId(p);
        if (expect == null || expect != arrow.getId()) {
            return;
        }
        p.stopRiding();
        Vec3 ap = arrow.getPosition(1.0F);
        p.setPos(ap.x, ap.y, ap.z);
        p.setYRot(arrow.getYRot());
        p.setXRot(arrow.getXRot());
        p.setYHeadRot(arrow.getYHeadRot());
        p.setYBodyRot(arrow.getYHeadRot());
        p.setDeltaMovement(arrow.getDeltaMovement());
        p.hurtMarked = true;
        p.fallDistance = 0.0F;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile proj = event.getProjectile();
        if (proj.level().isClientSide() || !(proj instanceof AbstractArrow arrow)) {
            return;
        }
        if (!(arrow.getOwner() instanceof ServerPlayer p)) {
            return;
        }
        if (p.level() != arrow.level()) {
            return;
        }
        ServerLevel level = (ServerLevel) p.level();
        RegistryAccess reg = p.level().registryAccess();
        boolean tagged = arrow.getPersistentData().getBoolean(ARROW_FLAG);
        if (!tagged) {
            if (!isLaunchCurseOnThisShot(arrow, p, reg)) {
                return;
            }
            if (isCrossbowItemCooldownActive(p)) {
                return;
            }
        }
        p.stopRiding();
        event.setCanceled(true);
        clearLaunchFollow(p);
        HitResult hit = event.getRayTraceResult();
        Vec3 at = adjustFeetOnImpact(level, hit.getLocation());
        level.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getEyeY(), p.getZ(), 12, 0.15, 0.2, 0.15, 0.1);
        p.setPos(at.x, at.y, at.z);
        p.setDeltaMovement(Vec3.ZERO);
        p.hurtMarked = true;
        p.fallDistance = 0.0F;
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_TICKS, NAUSEA_LEVEL, false, true, true));
        p.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, POISON_LEVEL, false, true, true));
        long t = level.getGameTime();
        p.getPersistentData().putLong(PDC_NO_FALL_UNTIL, t + NO_FALL_TICKS);
        if (!tagged) {
            startCrossbowReloadLikeEnderpearl(p);
            applyExtraDurabilityOnCursedLaunchCrossbow(p, reg);
        }
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 12, 0.15, 0.2, 0.15, 0.01);
        level.playSound(null, p.blockPosition(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.35f, 0.6f);
        level.playSound(null, p.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.5f, 0.3f);
        arrow.discard();
    }

    @SubscribeEvent
    public static void onFallDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.level().isClientSide()) {
            return;
        }
        if (!isFall(event.getSource())) {
            return;
        }
        if (!p.getPersistentData().contains(PDC_NO_FALL_UNTIL)) {
            return;
        }
        long t = p.level().getGameTime();
        if (t >= p.getPersistentData().getLong(PDC_NO_FALL_UNTIL)) {
            p.getPersistentData().remove(PDC_NO_FALL_UNTIL);
            return;
        }
        event.setNewDamage(0.0F);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.level().isClientSide()) {
            return;
        }
        if (p.getPersistentData().contains(PDC_NO_FALL_UNTIL)
                && p.level().getGameTime() >= p.getPersistentData().getLong(PDC_NO_FALL_UNTIL)) {
            p.getPersistentData().remove(PDC_NO_FALL_UNTIL);
        }
        Integer id = getFollowArrowId(p);
        if (id == null) {
            return;
        }
        Entity e = p.serverLevel().getEntity(id);
        if (!(e instanceof AbstractArrow a) || !a.isAlive() || !a.getPersistentData().getBoolean(ARROW_FLAG)) {
            clearLaunchFollow(p);
        }
    }

    private static boolean isFall(DamageSource s) {
        if (s.is(DamageTypeTags.IS_FALL)) {
            return true;
        }
        return s.getMsgId().endsWith("fall");
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.level().isClientSide()) {
            return;
        }
        if (!(event.getItem().getItem() instanceof CrossbowItem)) {
            return;
        }
        if (p.getCooldowns().isOnCooldown(event.getItem().getItem())) {
            event.setCanceled(true);
        }
    }
}
