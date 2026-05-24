package com.soleapixel.bmcmod.prism;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.network.PrismPackets;
import com.soleapixel.bmcmod.registry.ModMobEffects;
import com.soleapixel.bmcmod.util.EntityHiddenFrom;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class PrismHandler {
    public static final String NBT_KEY_POOL = "PrismPool";
    public static final String NBT_KEY_INIT = "PrismInit";
    public static final float RECHARGE_PER_TICK = 0.03F;

    private static final int GLOW_DURATION = 30;
    private static final int GLOW_INTERVAL = 8;
    private static final int REVEAL_RADIUS = 20;
    private static int tickCounter = 0;

    private PrismHandler() {
    }

    private static String poolKey() {
        return BmcMod.MODID + ":" + NBT_KEY_POOL;
    }

    private static String initKey() {
        return BmcMod.MODID + ":" + NBT_KEY_INIT;
    }

    public static float maxPoolFor(MobEffectInstance ex) {
        if (ex == null) {
            return 0.0F;
        }
        return 6.0F * (ex.getAmplifier() + 1);
    }

    public static float getPool(Player player) {
        return player.getPersistentData().getFloat(poolKey());
    }

    public static void setPool(Player player, float value) {
        float capped = cappedToEffect(player, value);
        player.getPersistentData().putFloat(poolKey(), capped);
    }

    public static void syncToClient(ServerPlayer player) {
        MobEffectInstance p = player.getEffect(ModMobEffects.PRISM);
        float max = p == null ? 0.0F : maxPoolFor(p);
        float c = p == null ? 0.0F : cappedToEffect(player, getPool(player));
        PacketDistributor.sendToPlayer(player, new PrismPackets.PrismSyncPayload(c, max));
    }

    private static void clearPrismNbt(Player player) {
        var tag = player.getPersistentData();
        tag.remove(poolKey());
        tag.remove(initKey());
    }

    private static float cappedToEffect(Player player, float v) {
        MobEffectInstance p = player.getEffect(ModMobEffects.PRISM);
        if (p == null) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(maxPoolFor(p), v));
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        MobEffectInstance p = sp.getEffect(ModMobEffects.PRISM);
        if (p == null) {
            return;
        }
        float pool = getPool(sp);
        if (pool <= 0.0F) {
            return;
        }
        float dmg = event.getNewDamage();
        if (dmg <= 0.0F) {
            return;
        }
        float absorb = Math.min(pool, dmg);
        if (absorb > 0.0F) {
            setPool(sp, pool - absorb);
            event.setNewDamage(dmg - absorb);
            syncToClient(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || sp.isSpectator()) {
            return;
        }
        tickCounter = (tickCounter + 1) & 0x3FFF;
        MobEffectInstance p = sp.getEffect(ModMobEffects.PRISM);
        if (p == null) {
            boolean needClear = getPool(sp) > 0.0F || sp.getPersistentData().getBoolean(initKey());
            if (needClear) {
                clearPrismNbt(sp);
            }
            if (needClear || sp.tickCount % 20 == 0) {
                syncToClient(sp);
            }
            return;
        }
        if (!sp.getPersistentData().getBoolean(initKey())) {
            setPool(sp, maxPoolFor(p));
            sp.getPersistentData().putBoolean(initKey(), true);
            syncToClient(sp);
        } else {
            setPool(sp, cappedToEffect(sp, getPool(sp)));
        }
        float pool = getPool(sp);
        float m = maxPoolFor(p);
        if (pool < m) {
            setPool(sp, Math.min(m, pool + RECHARGE_PER_TICK));
        }
        if (tickCounter % 4 == 0) {
            syncToClient(sp);
        }
        if (sp.level() instanceof ServerLevel && sp.tickCount % GLOW_INTERVAL == 0) {
            applyGlowInRadius(sp);
        }
    }

    private static void applyGlowInRadius(ServerPlayer sp) {
        AABB aabb = sp.getBoundingBox().inflate(REVEAL_RADIUS);
        for (LivingEntity e : sp.serverLevel().getEntitiesOfClass(LivingEntity.class, aabb, en -> en != sp && en.isAlive())) {
            if (!EntityHiddenFrom.isHiddenTo(sp, e)) {
                continue;
            }
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION, 0, false, false, true));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            syncToClient(sp);
        }
    }
}
