package com.soleapixel.bmcmod.gameplay;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import com.soleapixel.bmcmod.entity.RadiantSlime;
import com.soleapixel.bmcmod.registry.ModEntities;
import com.soleapixel.bmcmod.registry.ModFluids;

/**
 * Spawns Radiant Slimes depuis le liquide d’expérience (surcharge d’objets ou nuit / lac sombre).
 */
public final class RadiantSlimeSpawnLogic {
    /** Remplace un spawn simple par une « tour » gros → moyen → petit empilés (très rare). */
    private static final float PYRAMID_SPAWN_CHANCE = 0.022F;
    private static final double AGGRO_PLAYER_RANGE = 36.0;
    private static final double AGGRO_PLAYER_RANGE_SQ = AGGRO_PLAYER_RANGE * AGGRO_PLAYER_RANGE;

    private static final int DUMP_WINDOW_TICKS = 40;
    private static final int DUMP_THRESHOLD = 22;
    /** Seuil plus bas quand la limite quotidienne d’objets dissous est dépassée (même fenêtre 40 ticks). */
    private static final int DUMP_THRESHOLD_OVER_DAILY_LIMIT = 10;
    /** Après un spawn « surcharge », n’accepte plus de déclenchement pendant ce délai (évite rafales / re-spawns même tick). */
    private static final int DUMP_SPAWN_COOLDOWN_TICKS = 120;

    private RadiantSlimeSpawnLogic() {
    }

    public static boolean isExperienceFluid(FluidState state) {
        return state.getType() == ModFluids.EXPERIENCE_STILL.get() || state.getType() == ModFluids.EXPERIENCE_FLOWING.get();
    }

    /**
     * Position d’apparition (pieds, air) près d’un bloc de liquide d’XP autour d’un point — pour spawns « proximité lac ».
     */
    @Nullable
    public static BlockPos findNearbyExperienceFluidSurface(ServerLevel level, BlockPos center, int horizontalRadius, int verticalRadius) {
        for (int i = 0; i < 48; i++) {
            int dx = level.random.nextInt(horizontalRadius * 2 + 1) - horizontalRadius;
            int dz = level.random.nextInt(horizontalRadius * 2 + 1) - horizontalRadius;
            int dy = level.random.nextInt(verticalRadius * 2 + 1) - verticalRadius;
            BlockPos t = center.offset(dx, dy, dz);
            if (!level.isLoaded(t)) {
                continue;
            }
            if (isExperienceFluid(level.getFluidState(t))) {
                return surfaceAbove(t, level);
            }
            if (isExperienceFluid(level.getFluidState(t.below())) && level.getBlockState(t).isAir()) {
                return t.immutable();
            }
        }
        return null;
    }

    /** Un slime « agressé » par le lac surchargé : pas de retour au lac au lever du jour. */
    public static void spawnProximityAggroSlime(ServerLevel level, BlockPos feetPos) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        BlockPos pos = feetPos.immutable();
        server.execute(() -> runProximitySpawn(level, pos));
    }

    private static void runProximitySpawn(ServerLevel level, BlockPos feetPos) {
        if (level.isClientSide()) {
            return;
        }
        int size = level.random.nextFloat() < 0.82F ? 1 : 2;
        spawnOne(level, feetPos, false, null, size);
    }

    public static void onItemDissolvedInChunk(ServerLevel level, ChunkPos chunk, BlockPos dissolvePos) {
        ExperienceLiquidRadiantBurst.onDissolve(level, chunk, dissolvePos);
    }

    /**
     * Planifie le spawn après le tick courant : évite d’ajouter des entités pendant
     * {@link net.neoforged.neoforge.event.tick.EntityTickEvent} (cause fréquente de crash / CME).
     */
    public static void spawnFromItemDump(ServerLevel level, BlockPos dissolvePos) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        BlockPos pos = dissolvePos.immutable();
        server.execute(() -> runItemDumpSpawn(level, pos));
    }

    private static void runItemDumpSpawn(ServerLevel level, BlockPos dissolvePos) {
        if (level.isClientSide()) {
            return;
        }
        boolean over = ExperienceLiquidDailyDissolveTracker.isOverDailyDissolveLimit(level);
        int count = over ? 2 + level.random.nextInt(4) : 1 + level.random.nextInt(3);
        for (int i = 0; i < count; i++) {
            spawnOne(level, surfaceAbove(dissolvePos, level), false, null, randomDumpSize(level));
        }
    }

    public static void tryNaturalNightSpawn(ServerLevel level) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        if (level.isDay()) {
            return;
        }
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }
        ServerPlayer player = players.get(level.random.nextInt(players.size()));
        for (int attempt = 0; attempt < 14; attempt++) {
            int dx = level.random.nextInt(65) - 32;
            int dz = level.random.nextInt(65) - 32;
            int dy = level.random.nextInt(17) - 8;
            BlockPos base = player.blockPosition().offset(dx, dy, dz);
            BlockPos surface = findDimSurfaceNear(level, base);
            if (surface == null) {
                continue;
            }
            if (level.getMaxLocalRawBrightness(surface) >= 9) {
                continue;
            }
            BlockPos fluidBelow = surface.below();
            if (!isExperienceFluid(level.getFluidState(fluidBelow))) {
                continue;
            }
            int volume = estimateLakeVolume(level, fluidBelow, 520);
            int count = Mth.clamp(2 + volume / 140 + level.random.nextInt(3), 2, 9);
            count += level.random.nextInt(3);
            BlockPos anchor = findLakeAnchor(level, fluidBelow, 48);
            for (int s = 0; s < count; s++) {
                BlockPos spawnPos = surface.offset(level.random.nextInt(5) - 2, 0, level.random.nextInt(5) - 2);
                if (!level.getBlockState(spawnPos).isAir()) {
                    spawnPos = surface;
                }
                spawnOne(level, spawnPos, true, anchor, randomNaturalSize(level));
            }
            return;
        }
    }

    private static int randomDumpSize(ServerLevel level) {
        return RadiantSlime.randomRadiantSpawnSize(level.random);
    }

    private static int randomNaturalSize(ServerLevel level) {
        return RadiantSlime.randomRadiantSpawnSize(level.random);
    }

    private static BlockPos surfaceAbove(BlockPos fluid, ServerLevel level) {
        BlockPos p = fluid.above();
        if (level.getBlockState(p).isAir()) {
            return p;
        }
        return fluid;
    }

    @Nullable
    private static BlockPos findDimSurfaceNear(ServerLevel level, BlockPos start) {
        BlockPos.MutableBlockPos m = start.mutable();
        for (int i = 0; i < 24; i++) {
            if (!level.isLoaded(m)) {
                return null;
            }
            BlockState st = level.getBlockState(m);
            if (st.isAir() && isExperienceFluid(level.getFluidState(m.below()))) {
                return m.immutable();
            }
            m.move(Direction.DOWN);
        }
        return null;
    }

    private static BlockPos findLakeAnchor(ServerLevel level, BlockPos fluidStart, int maxSteps) {
        long sx = 0;
        long sy = 0;
        long sz = 0;
        int n = 0;
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        q.add(fluidStart.immutable());
        seen.add(fluidStart);
        int steps = 0;
        while (!q.isEmpty() && steps < maxSteps) {
            BlockPos c = q.poll();
            steps++;
            sx += c.getX();
            sy += c.getY();
            sz += c.getZ();
            n++;
            for (Direction d : Direction.values()) {
                BlockPos nxt = c.relative(d);
                if (!level.isLoaded(nxt) || seen.contains(nxt)) {
                    continue;
                }
                if (isExperienceFluid(level.getFluidState(nxt))) {
                    seen.add(nxt);
                    q.add(nxt.immutable());
                }
            }
        }
        if (n == 0) {
            return fluidStart;
        }
        return new BlockPos((int) (sx / n), (int) (sy / n), (int) (sz / n));
    }

    private static int estimateLakeVolume(ServerLevel level, BlockPos start, int maxBlocks) {
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        q.add(start.immutable());
        seen.add(start);
        int count = 0;
        while (!q.isEmpty() && count < maxBlocks) {
            BlockPos c = q.poll();
            count++;
            for (Direction d : Direction.values()) {
                BlockPos nxt = c.relative(d);
                if (!level.isLoaded(nxt) || seen.contains(nxt)) {
                    continue;
                }
                if (isExperienceFluid(level.getFluidState(nxt))) {
                    seen.add(nxt);
                    q.add(nxt.immutable());
                }
            }
        }
        return count;
    }

    private static void spawnOne(ServerLevel level, BlockPos pos, boolean lakeReturn, @Nullable BlockPos lakeAnchor, int size) {
        if (trySpawnPyramidTower(level, pos, lakeReturn, lakeAnchor)) {
            return;
        }
        RadiantSlime slime = ModEntities.RADIANT_SLIME.get().create(level);
        if (slime == null) {
            return;
        }
        slime.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        slime.setPersistenceRequired();
        slime.configureSpawn(lakeReturn, lakeAnchor != null ? lakeAnchor : pos);
        slime.setSize(size, true);
        level.addFreshEntity(slime);
        aggroNearestPlayer(level, slime);
    }

    /**
     * Tour de trois slimes : gros en bas, moyen sur la tête, tout petit au sommet.
     *
     * @return {@code true} si la tour a été créée (pas de spawn simple en plus).
     */
    private static boolean trySpawnPyramidTower(
            ServerLevel level, BlockPos pos, boolean lakeReturn, @Nullable BlockPos lakeAnchor) {
        if (level.random.nextFloat() >= PYRAMID_SPAWN_CHANCE) {
            return false;
        }
        RadiantSlime big = ModEntities.RADIANT_SLIME.get().create(level);
        RadiantSlime mid = ModEntities.RADIANT_SLIME.get().create(level);
        RadiantSlime top = ModEntities.RADIANT_SLIME.get().create(level);
        if (big == null || mid == null || top == null) {
            return false;
        }
        BlockPos anchor = lakeAnchor != null ? lakeAnchor : pos;
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        float yaw = level.random.nextFloat() * 360.0F;
        configureRadiantSpawn(big, x, y, z, yaw, lakeReturn, anchor, 4);
        configureRadiantSpawn(mid, x, y, z, yaw, lakeReturn, anchor, 2);
        configureRadiantSpawn(top, x, y, z, yaw, lakeReturn, anchor, 1);
        level.addFreshEntity(big);
        level.addFreshEntity(mid);
        mid.startRiding(big, true);
        level.addFreshEntity(top);
        top.startRiding(mid, true);
        aggroNearestPlayer(level, big);
        aggroNearestPlayer(level, mid);
        aggroNearestPlayer(level, top);
        return true;
    }

    private static void configureRadiantSpawn(
            RadiantSlime slime, double x, double y, double z, float yaw, boolean lakeReturn, BlockPos anchor, int size) {
        slime.moveTo(x, y, z, yaw, 0.0F);
        slime.setPersistenceRequired();
        slime.configureSpawn(lakeReturn, anchor);
        slime.setSize(size, true);
    }

    /**
     * Variante dédiée aux spawns via œuf: transforme parfois le slime invoqué en base d'une tour.
     *
     * @return {@code true} si la transformation en tour a été appliquée.
     */
    public static boolean tryConvertEggSpawnToPyramidTower(ServerLevel level, RadiantSlime base, float chance) {
        if (base == null || !base.isAlive() || base.isRemoved()) {
            return false;
        }
        if (base.getVehicle() != null || !base.getPassengers().isEmpty()) {
            return false;
        }
        if (level.random.nextFloat() >= chance) {
            return false;
        }
        RadiantSlime mid = ModEntities.RADIANT_SLIME.get().create(level);
        RadiantSlime top = ModEntities.RADIANT_SLIME.get().create(level);
        if (mid == null || top == null) {
            return false;
        }

        double x = base.getX();
        double y = base.getY();
        double z = base.getZ();
        float yaw = base.getYRot();
        base.setPersistenceRequired();
        base.setSize(Math.max(4, base.getSize()), true);
        configureRadiantSpawn(mid, x, y, z, yaw, false, base.blockPosition(), 2);
        configureRadiantSpawn(top, x, y, z, yaw, false, base.blockPosition(), 1);

        level.addFreshEntity(mid);
        mid.startRiding(base, true);
        level.addFreshEntity(top);
        top.startRiding(mid, true);
        aggroNearestPlayer(level, base);
        aggroNearestPlayer(level, mid);
        aggroNearestPlayer(level, top);
        return true;
    }

    /** Donne une cible joueur dès l’apparition (lac, dissolution, proximité). */
    public static void aggroNearestPlayer(ServerLevel level, RadiantSlime slime) {
        Player nearest = null;
        double best = AGGRO_PLAYER_RANGE_SQ;
        double cx = slime.getX();
        double cy = slime.getY();
        double cz = slime.getZ();
        for (Player p : level.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) {
                continue;
            }
            double d = p.distanceToSqr(cx, cy, cz);
            if (d < best) {
                best = d;
                nearest = p;
            }
        }
        if (nearest != null) {
            slime.setTarget(nearest);
        }
    }

    static final class ExperienceLiquidRadiantBurst {
        private static final HashMap<ChunkPos, Burst> BY_CHUNK = new HashMap<>();

        private ExperienceLiquidRadiantBurst() {
        }

        static void onDissolve(ServerLevel level, ChunkPos chunk, BlockPos dissolvePos) {
            long now = level.getGameTime();
            Burst b = BY_CHUNK.computeIfAbsent(chunk, k -> new Burst());
            if (now < b.cooldownUntilGameTime) {
                return;
            }
            if (now - b.windowStart > DUMP_WINDOW_TICKS) {
                b.windowStart = now;
                b.count = 0;
            }
            b.count++;
            b.lastDissolve = dissolvePos.immutable();
            int threshold = ExperienceLiquidDailyDissolveTracker.isOverDailyDissolveLimit(level)
                    ? DUMP_THRESHOLD_OVER_DAILY_LIMIT
                    : DUMP_THRESHOLD;
            if (b.count >= threshold) {
                BlockPos spawnAt = b.lastDissolve.immutable();
                b.count = 0;
                b.windowStart = now;
                b.cooldownUntilGameTime = now + DUMP_SPAWN_COOLDOWN_TICKS;
                RadiantSlimeSpawnLogic.spawnFromItemDump(level, spawnAt);
            }
            if (BY_CHUNK.size() > 512) {
                BY_CHUNK.clear();
            }
        }

        private static final class Burst {
            long windowStart;
            int count;
            long cooldownUntilGameTime = Long.MIN_VALUE;
            BlockPos lastDissolve = BlockPos.ZERO;
        }
    }
}
