package com.soleapixel.bmcmod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.soleapixel.bmcmod.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Affleurements de calcite avec un peu de volume : colonnes courtes + emprise 3×3 irrégulière.
 */
public final class HollowGardenCalciteScatterFeature extends Feature<NoneFeatureConfiguration> {
    public HollowGardenCalciteScatterFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static int surfaceY(WorldGenLevel level, int x, int z, int hintY) {
        if (!level.hasChunkAt(x, z)) {
            return hintY;
        }
        int hy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        hy = Mth.clamp(hy, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        BlockState at = level.getBlockState(new BlockPos(x, hy, z));
        if (at.isAir()) {
            for (int y = hy - 1; y >= hintY - 12 && y >= level.getMinBuildHeight(); y--) {
                BlockState s = level.getBlockState(new BlockPos(x, y, z));
                if (!s.isAir()) {
                    return y;
                }
            }
            return hintY;
        }
        return hy;
    }

    private static boolean isRockBase(BlockState s) {
        return s.is(ModBlocks.HOLLOW_GRASS.get()) || s.is(Blocks.END_STONE);
    }

    private static boolean canWrite(WorldGenLevel level, BlockPos p) {
        return level.hasChunkAt(p.getX(), p.getZ());
    }

    /** Colonne de calcite : remplace le sol puis empile dans l’air. */
    private static boolean placeColumn(WorldGenLevel level, RandomSource rand, int wx, int wz, int hintY) {
        int y = surfaceY(level, wx, wz, hintY);
        BlockPos surf = new BlockPos(wx, y, wz);
        if (!canWrite(level, surf)) {
            return false;
        }
        BlockState ground = level.getBlockState(surf);
        if (!isRockBase(ground)) {
            return false;
        }
        int height = 1 + rand.nextInt(3);
        boolean any = false;
        for (int i = 0; i < height; i++) {
            BlockPos p = surf.offset(0, i, 0);
            if (!canWrite(level, p)) {
                break;
            }
            BlockState s = level.getBlockState(p);
            if (i == 0) {
                if (isRockBase(s)) {
                    level.setBlock(p, Blocks.CALCITE.defaultBlockState(), 3);
                    any = true;
                } else {
                    break;
                }
            } else if (s.isAir()) {
                level.setBlock(p, Blocks.CALCITE.defaultBlockState(), 3);
                any = true;
            } else {
                break;
            }
        }
        if (any && rand.nextFloat() < 0.35F) {
            BlockPos below = surf.below();
            if (canWrite(level, below) && level.getBlockState(below).is(Blocks.END_STONE)) {
                level.setBlock(below, Blocks.CALCITE.defaultBlockState(), 3);
            }
        }
        return any;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        BlockPos origin = ctx.origin();
        if (!level.hasChunkAt(origin.getX(), origin.getZ())) {
            return false;
        }
        boolean any = false;
        int mounds = 1 + rand.nextInt(2);
        for (int m = 0; m < mounds; m++) {
            int ax = origin.getX() + rand.nextInt(7) - 3;
            int az = origin.getZ() + rand.nextInt(7) - 3;
            if (placeColumn(level, rand, ax, az, origin.getY())) {
                any = true;
            }
            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    if (ox == 0 && oz == 0) {
                        continue;
                    }
                    if (rand.nextFloat() > 0.32F) {
                        continue;
                    }
                    if (placeColumn(level, rand, ax + ox, az + oz, origin.getY())) {
                        any = true;
                    }
                }
            }
        }
        return any;
    }
}
