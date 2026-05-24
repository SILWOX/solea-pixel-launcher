package com.soleapixel.bmcmod.gameplay;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobSplitEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.RadiantSlime;
import com.soleapixel.bmcmod.registry.ModFluids;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class RadiantSlimeEvents {
    /** Entre deux spawns « lac enragé » par joueur (évite le spam). */
    private static final Map<UUID, Long> PROXIMITY_SLIME_COOLDOWN = new ConcurrentHashMap<>();
    private static final String TAG_PYRAMID_PROCESSED = "bmcmod:radiant_pyramid_processed";
    private static final float EGG_PYRAMID_CHANCE = 0.08F;

    private RadiantSlimeEvents() {
    }

    @SubscribeEvent
    public static void onMobSplit(MobSplitEvent event) {
        if (!(event.getParent() instanceof RadiantSlime parent)) {
            return;
        }
        if (!(parent.level() instanceof ServerLevel sl)) {
            return;
        }
        MinecraftServer server = sl.getServer();
        for (Mob child : event.getChildren()) {
            if (child instanceof RadiantSlime rs) {
                rs.inheritLakeBehavior(parent);
                if (server != null) {
                    RadiantSlime slimeRef = rs;
                    server.execute(() -> RadiantSlimeSpawnLogic.aggroNearestPlayer(sl, slimeRef));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRadiantSlimeJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof RadiantSlime slime)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel sl)) {
            return;
        }
        if (slime.getSpawnType() != MobSpawnType.SPAWN_EGG) {
            return;
        }
        if (slime.getPersistentData().getBoolean(TAG_PYRAMID_PROCESSED)) {
            return;
        }
        slime.getPersistentData().putBoolean(TAG_PYRAMID_PROCESSED, true);
        MinecraftServer server = sl.getServer();
        if (server == null) {
            return;
        }
        server.execute(() -> RadiantSlimeSpawnLogic.tryConvertEggSpawnToPyramidTower(sl, slime, EGG_PYRAMID_CHANCE));
    }

    /** Noyade / autres dégâts du fluide d’XP : les Radiant Slimes y sont immunisés tant qu’ils y baignent. */
    @SubscribeEvent
    public static void onRadiantSlimeDamageInExperienceLiquid(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof RadiantSlime slime)) {
            return;
        }
        if (slime.level().isClientSide()) {
            return;
        }
        var expType = ModFluids.EXPERIENCE_FLUID_TYPE.get();
        if (slime.getFluidTypeHeight(expType) > 0.12F) {
            event.setNewDamage(0.0F);
        }
    }

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) {
            return;
        }
        if (sl.getGameTime() % 420 == 0 && sl.random.nextFloat() <= 0.35F) {
            RadiantSlimeSpawnLogic.tryNaturalNightSpawn(sl);
        }
        tryProximitySlimesWhenLakeOverfed(sl);
    }

    /**
     * Si la limite quotidienne d’objets dissous est dépassée, des Radiant Slimes peuvent apparaître
     * quand un joueur s’approche du lac (sans continuer à jeter des items).
     */
    private static void tryProximitySlimesWhenLakeOverfed(ServerLevel sl) {
        if (sl.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        if (!ExperienceLiquidDailyDissolveTracker.isOverDailyDissolveLimit(sl)) {
            return;
        }
        if (sl.getGameTime() % 65 != 0) {
            return;
        }
        long now = sl.getGameTime();
        for (ServerPlayer p : sl.players()) {
            if (p.isCreative() || p.isSpectator()) {
                continue;
            }
            if (now < PROXIMITY_SLIME_COOLDOWN.getOrDefault(p.getUUID(), Long.MIN_VALUE)) {
                continue;
            }
            var feet = RadiantSlimeSpawnLogic.findNearbyExperienceFluidSurface(sl, p.blockPosition(), 16, 8);
            if (feet == null) {
                continue;
            }
            if (sl.random.nextFloat() > 0.12F) {
                continue;
            }
            PROXIMITY_SLIME_COOLDOWN.put(p.getUUID(), now + 200);
            RadiantSlimeSpawnLogic.spawnProximityAggroSlime(sl, feet);
        }
    }
}
