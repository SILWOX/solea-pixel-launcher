package com.soleapixel.bmcmod.gameplay;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.soleapixel.bmcmod.BmcMod;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.projectile.Snowball;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class IceStaffGolemEvents {
    public static final String TAG_OWNER = "bmcmod:ice_staff_owner";
    public static final String TAG_EXPIRES_AT = "bmcmod:ice_staff_expires_at";
    private static final List<TempBridgeBlock> TEMP_BRIDGE_BLOCKS = new ArrayList<>();

    private IceStaffGolemEvents() {
    }

    public static void registerTempBridgeBlock(ServerLevel level, BlockPos pos, BlockState previousState, int ttlTicks) {
        if (ttlTicks <= 0) {
            return;
        }
        long expiresAt = level.getGameTime() + ttlTicks;
        synchronized (TEMP_BRIDGE_BLOCKS) {
            TEMP_BRIDGE_BLOCKS.removeIf(e -> e.level == level && e.pos.equals(pos));
            TEMP_BRIDGE_BLOCKS.add(new TempBridgeBlock(level, pos.immutable(), previousState, expiresAt));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        synchronized (TEMP_BRIDGE_BLOCKS) {
            if (TEMP_BRIDGE_BLOCKS.isEmpty()) {
                return;
            }
            Iterator<TempBridgeBlock> it = TEMP_BRIDGE_BLOCKS.iterator();
            while (it.hasNext()) {
                TempBridgeBlock e = it.next();
                if (e.level == null || !e.level.isLoaded(e.pos)) {
                    continue;
                }
                if (e.level.getGameTime() < e.expiresAtGameTime) {
                    continue;
                }
                BlockState current = e.level.getBlockState(e.pos);
                if (current.is(Blocks.PACKED_ICE)) {
                    e.level.setBlock(e.pos, e.previousState, 3);
                }
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof SnowGolem golem)) {
            return;
        }
        if (!(golem.level() instanceof ServerLevel level) || level.isClientSide()) {
            return;
        }
        if (!golem.getPersistentData().hasUUID(TAG_OWNER)) {
            return;
        }
        if (!golem.getPersistentData().contains(TAG_EXPIRES_AT, Tag.TAG_LONG)) {
            return;
        }
        if (level.getGameTime() >= golem.getPersistentData().getLong(TAG_EXPIRES_AT)) {
            golem.discard();
        }
    }

    @SubscribeEvent
    public static void onSnowballImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Snowball snowball)) {
            return;
        }
        Entity owner = snowball.getOwner();
        if (!(owner instanceof SnowGolem golem) || !golem.getPersistentData().hasUUID(TAG_OWNER)) {
            return;
        }
        if (!(event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit)) {
            return;
        }
        if (!(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (target == owner) {
            return;
        }
        target.hurt(target.damageSources().indirectMagic(snowball, owner), 4.0F);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, true));
    }

    private record TempBridgeBlock(ServerLevel level, BlockPos pos, BlockState previousState, long expiresAtGameTime) {
    }
}
