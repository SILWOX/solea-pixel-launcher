package com.soleapixel.bmcmod.worldgen.feature;

import com.mojang.serialization.Codec;

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

import com.soleapixel.bmcmod.registry.SunwoodBlocks;

/**
 * Petit affleurement pierreux : croix 5 blocs au sol + un bloc au centre au-dessus (style caillou oasis).
 */
public final class SoleaGroveStoneClusterFeature extends Feature<NoneFeatureConfiguration> {
    public SoleaGroveStoneClusterFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static int surfaceY(WorldGenLevel level, int x, int z) {
        if (!level.hasChunkAt(x, z)) {
            return level.getMinBuildHeight();
        }
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    private static boolean validGround(BlockState s) {
        return s.is(Blocks.GRASS_BLOCK)
                || s.is(Blocks.SAND)
                || s.is(Blocks.DIRT)
                || s.is(Blocks.COARSE_DIRT)
                || s.is(Blocks.ROOTED_DIRT)
                || s.is(Blocks.MOSS_BLOCK)
                || s.is(SunwoodBlocks.SURFACE_MOSS.get());
    }

    private static BlockState pickStone(RandomSource rand) {
        return switch (rand.nextInt(4)) {
            case 0 -> Blocks.COBBLESTONE.defaultBlockState();
            case 1 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            default -> Blocks.STONE.defaultBlockState();
        };
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        BlockPos origin = context.origin();
        int x = origin.getX();
        int z = origin.getZ();
        int y = surfaceY(level, x, z);
        y = Mth.clamp(y, level.getMinBuildHeight(), level.getMaxBuildHeight() - 2);
        BlockPos base = new BlockPos(x, y, z);
        if (!level.hasChunkAt(base.getX(), base.getZ())) {
            return false;
        }
        if (!validGround(level.getBlockState(base))) {
            return false;
        }
        int[][] arms = { { 0, 0 }, { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        BlockState stone = pickStone(rand);
        for (int[] d : arms) {
            BlockPos p = base.offset(d[0], 0, d[1]);
            if (!level.hasChunkAt(p.getX(), p.getZ())) {
                continue;
            }
            BlockState ground = level.getBlockState(p);
            if (validGround(ground)) {
                level.setBlock(p, stone, 2);
            }
        }
        BlockPos top = base.above();
        if (level.getBlockState(top).isAir()) {
            level.setBlock(top, pickStone(rand), 2);
        }
        return true;
    }
}
