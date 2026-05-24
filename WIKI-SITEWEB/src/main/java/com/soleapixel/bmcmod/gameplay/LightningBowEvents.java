package com.soleapixel.bmcmod.gameplay;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.LightningBowItem;
import com.soleapixel.bmcmod.registry.ModItems;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class LightningBowEvents {
    /** Flèches encore à marquer après un tir (multishot = 3). */
    private static final String PENDING_LIGHTNING_ARROWS = BmcMod.MODID + ":pending_lightning_arrows";
    /** Marqueur sur la flèche. */
    private static final String LIGHTNING_ARROW = BmcMod.MODID + ":lightning_arrow";

    private static final int GLOW_TICKS = 50;
    private static final float VELOCITY_SCALE = 1.42F;
    private static final float DAMAGE_BONUS = 1.25F;

    private LightningBowEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (!event.hasAmmo()) {
            return;
        }
        ItemStack bow = event.getBow();
        if (!(bow.getItem() instanceof LightningBowItem)) {
            return;
        }
        // ArrowLooseEvent est aussi posté côté client (LocalPlayer) : ne pas caster en ServerPlayer.
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        RegistryAccess reg = player.level().registryAccess();
        int multishot = reg.lookupOrThrow(Registries.ENCHANTMENT)
                .get(Enchantments.MULTISHOT)
                .map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, bow))
                .orElse(0);
        int pending = multishot > 0 ? 3 : 1;
        player.getPersistentData().putInt(PENDING_LIGHTNING_ARROWS, pending);
        int ch = event.getCharge();
        event.setCharge(Mth.clamp((int) (ch * 1.22f) + 3, 1, 40));
    }

    @SubscribeEvent
    public static void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        if (!(arrow.getOwner() instanceof ServerPlayer player)) {
            return;
        }
        if (!(arrow instanceof Arrow)) {
            return;
        }
        int pending = player.getPersistentData().getInt(PENDING_LIGHTNING_ARROWS);
        if (pending <= 0) {
            return;
        }
        arrow.getPersistentData().putBoolean(LIGHTNING_ARROW, true);
        player.getPersistentData().putInt(PENDING_LIGHTNING_ARROWS, pending - 1);

        Vec3 v = arrow.getDeltaMovement();
        if (v.lengthSqr() > 1.0E-6) {
            Vec3 aim = player.getLookAngle().normalize();
            Vec3 cur = v.normalize();
            Vec3 blended = new Vec3(
                    Mth.lerp(0.22, cur.x, aim.x),
                    Mth.lerp(0.22, cur.y, aim.y),
                    Mth.lerp(0.22, cur.z, aim.z)
            ).normalize().scale(v.length() * VELOCITY_SCALE);
            arrow.setDeltaMovement(blended);
        }

        arrow.setBaseDamage(arrow.getBaseDamage() * DAMAGE_BONUS);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || arrow.level().isClientSide()) {
            return;
        }
        if (!arrow.getPersistentData().getBoolean(LIGHTNING_ARROW)) {
            return;
        }
        Level level = arrow.level();
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        HitResult hit = event.getRayTraceResult();
        Vec3 pos = hit.getLocation();

        if (hit.getType() == HitResult.Type.ENTITY) {
            EntityHitResult ehr = (EntityHitResult) hit;
            if (ehr.getEntity() instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, false, true, true));
                pos = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0);
            }
        }

        spawnLightningStrike(sl, pos);
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.35f, 0.9f);
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.5f, 1.1f);
        for (int i = 0; i < 14; i++) {
            double ox = (sl.random.nextDouble() - 0.5) * 0.4;
            double oy = sl.random.nextDouble() * 0.5;
            double oz = (sl.random.nextDouble() - 0.5) * 0.4;
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x + ox, pos.y + oy, pos.z + oz, 2, 0.02, 0.08, 0.02, 0.01);
        }

        arrow.getPersistentData().remove(LIGHTNING_ARROW);
    }

    private static void spawnLightningStrike(ServerLevel level, Vec3 pos) {
        var bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        level.addFreshEntity(bolt);
    }
}
