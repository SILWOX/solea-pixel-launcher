package com.soleapixel.bmcmod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.soleapixel.bmcmod.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Hollow Garden: lake near hollow-grass patch centroid. End islands (XP): sparse placement + carved bowl in end stone.
 */
public final class HollowGardenPondFeature extends Feature<PondConfiguration> {
    private static final int CENTROID_SCAN = 44;
    private static final int MIN_HOLLOW_SAMPLES = 18;
    private static final int MAX_ORIGIN_DIST_FROM_CENTROID = 28;
    /** Few scattered columns (old logic) would look broken; require a coherent pool. */
    private static final int MIN_EXPERIENCE_FLUID_COLUMNS = 14;
    private static final int EXPERIENCE_MAX_SLOPE_FALLBACK = 1;
    private static final int EXPERIENCE_MAX_SLOPE_GARDEN = 2;

    public HollowGardenPondFeature(Codec<PondConfiguration> codec) {
        super(codec);
    }

    private static boolean isGround(BlockState state) {
        return state.is(ModBlocks.HOLLOW_GRASS.get()) || state.is(Blocks.END_STONE);
    }

    private static boolean canWrite(WorldGenLevel level, BlockPos pos) {
        return level.hasChunkAt(pos.getX(), pos.getZ());
    }

    private static int surfaceYAt(WorldGenLevel level, int wx, int wz, int hintY) {
        if (!level.hasChunkAt(wx, wz)) {
            return hintY;
        }
        int hy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz);
        hy = Mth.clamp(hy, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        BlockPos probe = new BlockPos(wx, hy, wz);
        BlockState at = level.getBlockState(probe);
        if (at.isAir()) {
            for (int y = hy - 1; y >= hintY - 16 && y >= level.getMinBuildHeight(); y--) {
                BlockState s = level.getBlockState(new BlockPos(wx, y, wz));
                if (!s.isAir()) {
                    return y;
                }
            }
            return hintY;
        }
        return hy;
    }

    private static BlockPos hollowGrassPatchCentroid(WorldGenLevel level, int ox, int oz) {
        long sumX = 0;
        long sumZ = 0;
        int n = 0;
        for (int dx = -CENTROID_SCAN; dx <= CENTROID_SCAN; dx += 2) {
            for (int dz = -CENTROID_SCAN; dz <= CENTROID_SCAN; dz += 2) {
                int wx = ox + dx;
                int wz = oz + dz;
                if (!level.hasChunkAt(wx, wz)) {
                    continue;
                }
                int sy = surfaceYAt(level, wx, wz, ox);
                BlockState s = level.getBlockState(new BlockPos(wx, sy, wz));
                if (s.is(ModBlocks.HOLLOW_GRASS.get())) {
                    sumX += wx;
                    sumZ += wz;
                    n++;
                }
            }
        }
        if (n < MIN_HOLLOW_SAMPLES) {
            return null;
        }
        return new BlockPos((int) Math.round((double) sumX / n), 0, (int) Math.round((double) sumZ / n));
    }

    private static int fluidMaskRadiusSq(RandomSource rand, int wx, int wz, int rFluidSq) {
        int n = ((int) (Mth.getSeed(wx, 31, wz) >>> 48) & 7) - 3;
        return Mth.clamp(rFluidSq + n, rFluidSq - 2, rFluidSq + 4);
    }

    private static boolean isReplaceableForExperienceCarve(BlockState state) {
        return state.is(Blocks.END_STONE)
                || state.is(ModBlocks.HOLLOW_GRASS.get())
                || state.is(ModBlocks.END_SAND.get())
                || state.isAir()
                || state.is(ModBlocks.EXPERIENCE_LIQUID.get());
    }

    /** Smooth bowl: shallow at rim, deeper in the middle (2–5 blocks tall). */
    private static int experienceBowlDepth(int dist2, int rFluidSq) {
        float rFluid = Mth.sqrt((float) rFluidSq);
        float d = Mth.sqrt((float) dist2);
        float t = Mth.clamp(1f - d / Math.max(rFluid, 1f), 0f, 1f);
        return Mth.clamp(2 + Mth.floor(t * t * 3f), 2, 5);
    }

    /** Do not carve under chorus trees / solid overhangs. */
    private static boolean columnOpenAbove(WorldGenLevel level, int wx, int sy, int wz, int upwardCheck) {
        for (int u = 1; u <= upwardCheck; u++) {
            BlockState above = level.getBlockState(new BlockPos(wx, sy + u, wz));
            if (!above.isAir()) {
                return false;
            }
        }
        return true;
    }

    private static void markExperienceFluidMask(
            WorldGenLevel level,
            int cx,
            int cz,
            int rOuter,
            int rFluidSq,
            int rOuterSq,
            BlockPos origin,
            int yAnchor,
            int maxSlope,
            boolean[][] isFluid,
            RandomSource rand) {
        float fuzz = 0.92f + 0.08f * rand.nextFloat();
        int capSq = Mth.ceil((float) rFluidSq * fuzz * fuzz);
        for (int dx = -rOuter; dx <= rOuter; dx++) {
            for (int dz = -rOuter; dz <= rOuter; dz++) {
                int dist2 = dx * dx + dz * dz;
                if (dist2 > rOuterSq || dist2 > capSq) {
                    continue;
                }
                int wx = cx + dx;
                int wz = cz + dz;
                int sy = surfaceYAt(level, wx, wz, origin.getY());
                if (Math.abs(sy - yAnchor) > maxSlope) {
                    continue;
                }
                BlockPos colTop = new BlockPos(wx, sy, wz);
                if (!canWrite(level, colTop)) {
                    continue;
                }
                BlockState top = level.getBlockState(colTop);
                boolean okSurface = top.isAir() || top.is(ModBlocks.HOLLOW_GRASS.get()) || top.is(Blocks.END_STONE);
                if (!okSurface) {
                    continue;
                }
                if (top.isAir()) {
                    BlockState under = level.getBlockState(colTop.below());
                    if (!isGround(under)) {
                        continue;
                    }
                }
                if (!columnOpenAbove(level, wx, sy, wz, 14)) {
                    continue;
                }
                isFluid[dx + rOuter][dz + rOuter] = true;
            }
        }
    }

    private static int countFluidMask(boolean[][] isFluid) {
        int n = 0;
        for (boolean[] row : isFluid) {
            for (boolean b : row) {
                if (b) {
                    n++;
                }
            }
        }
        return n;
    }

    private static boolean placeExperienceBowlColumns(
            WorldGenLevel level,
            int cx,
            int cz,
            int rOuter,
            int rFluidSq,
            BlockPos origin,
            BlockState fluid,
            int yAnchor,
            int maxSlope,
            boolean[][] isFluid) {
        boolean placedFluid = false;
        for (int dx = -rOuter; dx <= rOuter; dx++) {
            for (int dz = -rOuter; dz <= rOuter; dz++) {
                int ix = dx + rOuter;
                int iz = dz + rOuter;
                if (!isFluid[ix][iz]) {
                    continue;
                }
                int wx = cx + dx;
                int wz = cz + dz;
                int sy = surfaceYAt(level, wx, wz, origin.getY());
                if (Math.abs(sy - yAnchor) > maxSlope) {
                    continue;
                }
                BlockPos colTop = new BlockPos(wx, sy, wz);
                if (!canWrite(level, colTop)) {
                    continue;
                }
                BlockState surf = level.getBlockState(colTop);
                if (!(surf.isAir() || surf.is(ModBlocks.HOLLOW_GRASS.get()) || surf.is(Blocks.END_STONE))) {
                    continue;
                }

                int dist2 = dx * dx + dz * dz;
                int depth = experienceBowlDepth(dist2, rFluidSq);

                for (int i = 0; i < depth; i++) {
                    BlockPos p = new BlockPos(wx, sy - i, wz);
                    if (!canWrite(level, p)) {
                        break;
                    }
                    BlockState s = level.getBlockState(p);
                    if (!isReplaceableForExperienceCarve(s)) {
                        break;
                    }
                    boolean floorLayer = i == depth - 1;
                    if (floorLayer) {
                        if (s.is(Blocks.END_STONE) || s.is(ModBlocks.END_SAND.get())) {
                            level.setBlock(p, Blocks.CALCITE.defaultBlockState(), 3);
                        } else if (s.is(ModBlocks.HOLLOW_GRASS.get()) || s.isAir()) {
                            level.setBlock(p, Blocks.CALCITE.defaultBlockState(), 3);
                        } else if (s.is(ModBlocks.EXPERIENCE_LIQUID.get())) {
                            level.setBlock(p, Blocks.CALCITE.defaultBlockState(), 3);
                        }
                    } else {
                        level.setBlock(p, fluid, 3);
                        placedFluid = true;
                    }
                }
            }
        }
        return placedFluid;
    }

    private static boolean placeWaterColumns(
            WorldGenLevel level,
            int cx,
            int cz,
            int rOuter,
            BlockPos origin,
            BlockState fluid,
            int yAnchor,
            int maxSlope,
            boolean[][] isFluid) {
        boolean placedFluid = false;
        for (int dx = -rOuter; dx <= rOuter; dx++) {
            for (int dz = -rOuter; dz <= rOuter; dz++) {
                int ix = dx + rOuter;
                int iz = dz + rOuter;
                if (!isFluid[ix][iz]) {
                    continue;
                }
                int wx = cx + dx;
                int wz = cz + dz;
                if (Math.abs(surfaceYAt(level, wx, wz, origin.getY()) - yAnchor) > maxSlope) {
                    continue;
                }
                BlockPos colTop = new BlockPos(wx, surfaceYAt(level, wx, wz, origin.getY()), wz);
                if (!canWrite(level, colTop)) {
                    continue;
                }
                int depth = 2;
                int minFluidY = colTop.getY();
                for (int dy = 0; dy < depth; dy++) {
                    BlockPos p = colTop.offset(0, -dy, 0);
                    if (!canWrite(level, p)) {
                        break;
                    }
                    BlockState s = level.getBlockState(p);
                    if (s.is(ModBlocks.HOLLOW_GRASS.get()) || s.isAir()) {
                        level.setBlock(p, fluid, 3);
                        placedFluid = true;
                        minFluidY = p.getY();
                    } else {
                        break;
                    }
                }
                for (int d = 1; d <= 2; d++) {
                    BlockPos fp = new BlockPos(wx, minFluidY - d, wz);
                    if (!canWrite(level, fp)) {
                        continue;
                    }
                    BlockState fs = level.getBlockState(fp);
                    if (fs.is(Blocks.END_STONE)) {
                        level.setBlock(fp, Blocks.CALCITE.defaultBlockState(), 3);
                    }
                }
            }
        }
        return placedFluid;
    }

    @Override
    public boolean place(FeaturePlaceContext<PondConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        BlockPos origin = ctx.origin();
        boolean experience = ctx.config().experience();

        if (!canWrite(level, origin)) {
            return false;
        }

        BlockPos centroid = hollowGrassPatchCentroid(level, origin.getX(), origin.getZ());
        boolean usingFallbackCentroid = false;
        if (centroid == null) {
            if (!experience) {
                return false;
            }
            int sy = surfaceYAt(level, origin.getX(), origin.getZ(), origin.getY());
            BlockPos surface = new BlockPos(origin.getX(), sy, origin.getZ());
            BlockState ground = level.getBlockState(surface);
            if (!(ground.is(Blocks.END_STONE)
                    || ground.is(ModBlocks.HOLLOW_GRASS.get())
                    || ground.is(ModBlocks.END_SAND.get()))) {
                return false;
            }
            centroid = new BlockPos(origin.getX(), 0, origin.getZ());
            usingFallbackCentroid = true;
        }
        if (!usingFallbackCentroid
                && (Mth.abs(origin.getX() - centroid.getX()) > MAX_ORIGIN_DIST_FROM_CENTROID
                        || Mth.abs(origin.getZ() - centroid.getZ()) > MAX_ORIGIN_DIST_FROM_CENTROID)) {
            return false;
        }

        int cx = centroid.getX();
        int cz = centroid.getZ();
        int yAnchor = surfaceYAt(level, cx, cz, origin.getY());
        int maxSlope =
                experience && usingFallbackCentroid ? EXPERIENCE_MAX_SLOPE_FALLBACK : (experience ? EXPERIENCE_MAX_SLOPE_GARDEN : 2);

        boolean big = !experience && rand.nextFloat() < 0.38F;

        final int rFluid;
        final int rOuter;
        if (experience) {
            float scale = 0.92f + 0.08f * rand.nextFloat();
            rFluid = Mth.clamp(Math.round((3 + rand.nextInt(2)) * scale), 3, 4);
            rOuter = rFluid + Mth.ceil(3f + 2f * rand.nextFloat()); // rim band for sand/reeds
        } else {
            rFluid = big ? 5 : 3;
            rOuter = big ? 10 : 6;
        }
        int rFluidSq = rFluid * rFluid;
        int rOuterSq = rOuter * rOuter;

        Block fluidBlock = experience ? ModBlocks.EXPERIENCE_LIQUID.get() : Blocks.WATER;
        BlockState fluid = fluidBlock.defaultBlockState();

        boolean[][] isFluid = new boolean[2 * rOuter + 1][2 * rOuter + 1];

        if (experience) {
            markExperienceFluidMask(level, cx, cz, rOuter, rFluidSq, rOuterSq, origin, yAnchor, maxSlope, isFluid, rand);
            if (countFluidMask(isFluid) < MIN_EXPERIENCE_FLUID_COLUMNS) {
                return false;
            }
        } else {
            for (int dx = -rOuter; dx <= rOuter; dx++) {
                for (int dz = -rOuter; dz <= rOuter; dz++) {
                    int dist2 = dx * dx + dz * dz;
                    if (dist2 > rOuterSq) {
                        continue;
                    }
                    int wx = cx + dx;
                    int wz = cz + dz;
                    int sy = surfaceYAt(level, wx, wz, origin.getY());
                    if (Math.abs(sy - yAnchor) > maxSlope) {
                        continue;
                    }
                    BlockPos colTop = new BlockPos(wx, sy, wz);
                    if (!canWrite(level, colTop)) {
                        continue;
                    }
                    BlockState top = level.getBlockState(colTop);
                    if (!top.isAir() && !top.is(ModBlocks.HOLLOW_GRASS.get())) {
                        continue;
                    }
                    if (!top.is(ModBlocks.HOLLOW_GRASS.get())) {
                        BlockState under = level.getBlockState(colTop.below());
                        if (!isGround(under)) {
                            continue;
                        }
                    }
                    int cap = fluidMaskRadiusSq(rand, wx, wz, rFluidSq);
                    if (dist2 <= cap) {
                        isFluid[dx + rOuter][dz + rOuter] = true;
                    }
                }
            }
        }

        boolean placedFluid =
                experience
                        ? placeExperienceBowlColumns(level, cx, cz, rOuter, rFluidSq, origin, fluid, yAnchor, maxSlope, isFluid)
                        : placeWaterColumns(level, cx, cz, rOuter, origin, fluid, yAnchor, maxSlope, isFluid);

        for (int dx = -rOuter; dx <= rOuter; dx++) {
            for (int dz = -rOuter; dz <= rOuter; dz++) {
                int dist2 = dx * dx + dz * dz;
                if (dist2 > rOuterSq) {
                    continue;
                }
                int ix = dx + rOuter;
                int iz = dz + rOuter;
                if (isFluid[ix][iz]) {
                    continue;
                }
                boolean nearFluid = false;
                if (ix > 0 && isFluid[ix - 1][iz]) {
                    nearFluid = true;
                }
                if (ix < isFluid.length - 1 && isFluid[ix + 1][iz]) {
                    nearFluid = true;
                }
                if (iz > 0 && isFluid[ix][iz - 1]) {
                    nearFluid = true;
                }
                if (iz < isFluid[ix].length - 1 && isFluid[ix][iz + 1]) {
                    nearFluid = true;
                }
                if (!nearFluid) {
                    continue;
                }
                int wx = cx + dx;
                int wz = cz + dz;
                int sy = surfaceYAt(level, wx, wz, origin.getY());
                if (Math.abs(sy - yAnchor) > maxSlope + 1) {
                    continue;
                }
                BlockPos colTop = new BlockPos(wx, sy, wz);
                if (!canWrite(level, colTop)) {
                    continue;
                }
                BlockState rim = level.getBlockState(colTop);
                if (rim.is(ModBlocks.HOLLOW_GRASS.get()) || rim.is(Blocks.END_STONE)) {
                    level.setBlock(colTop, ModBlocks.END_SAND.get().defaultBlockState(), 3);
                }
            }
        }

        return placedFluid;
    }
}
