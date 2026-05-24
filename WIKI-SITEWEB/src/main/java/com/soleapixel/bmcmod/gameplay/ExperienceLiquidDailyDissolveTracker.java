package com.soleapixel.bmcmod.gameplay;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Compte les objets dissous dans le liquide d’expérience par « jour » Minecraft
 * ({@value #TICKS_PER_MINECRAFT_DAY} ticks ≈ 20 minutes IRL à 20 TPS). Au-delà de la limite,
 * le comportement des Radiant Slimes devient plus agressif (voir {@link RadiantSlimeSpawnLogic}).
 */
public final class ExperienceLiquidDailyDissolveTracker {
    /** Un cycle jour/nuit Minecraft complet en ticks (20 min à 20 TPS). */
    public static final int TICKS_PER_MINECRAFT_DAY = 24000;

    /**
     * Nombre d’objets dissous dans le liquide avant de considérer la journée « surchargée »
     * pour cette dimension.
     */
    public static final int DEFAULT_DAILY_ITEM_LIMIT = 220;

    private static final Map<ResourceKey<Level>, PeriodState> BY_DIMENSION = new HashMap<>();

    private ExperienceLiquidDailyDissolveTracker() {
    }

    /** À appeler une fois par objet effectivement dissous dans le liquide d’XP. */
    public static void recordDissolve(ServerLevel level) {
        long period = level.getGameTime() / TICKS_PER_MINECRAFT_DAY;
        PeriodState st = BY_DIMENSION.computeIfAbsent(level.dimension(), k -> new PeriodState());
        if (st.periodIndex != period) {
            st.periodIndex = period;
            st.dissolveCount = 0;
        }
        st.dissolveCount++;
    }

    public static boolean isOverDailyDissolveLimit(ServerLevel level) {
        return dissolveCountThisPeriod(level) > DEFAULT_DAILY_ITEM_LIMIT;
    }

    public static int dissolveCountThisPeriod(ServerLevel level) {
        long period = level.getGameTime() / TICKS_PER_MINECRAFT_DAY;
        PeriodState st = BY_DIMENSION.get(level.dimension());
        if (st == null || st.periodIndex != period) {
            return 0;
        }
        return st.dissolveCount;
    }

    public static int remainingUntilLimit(ServerLevel level) {
        return Math.max(0, DEFAULT_DAILY_ITEM_LIMIT - dissolveCountThisPeriod(level));
    }

    private static final class PeriodState {
        long periodIndex = Long.MIN_VALUE;
        int dissolveCount;
    }
}
