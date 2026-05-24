package com.soleapixel.bmcmod.worldgen.feature;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import com.soleapixel.bmcmod.registry.SunwoodBlocks;

/**
 * Buisson bas : feuilles persistantes en petit nuage au-dessus d’un sol herbe / mousse / sable.
 */
public final class SoleaGroveBushFeature extends Feature<NoneFeatureConfiguration> {
    private static final int[][] OFFSETS = {
            { 0, 0, 0 },
            { 1, 0, 0 },
            { -1, 0, 0 },
            { 0, 0, 1 },
            { 0, 0, -1 },
            { 1, 0, 1 },
            { -1, 0, -1 },
            { 1, 0, -1 },
            { -1, 0, 1 },
            { 0, 1, 0 },
            { 1, 1, 0 },
            { -1, 1, 0 },
            { 0, 1, 1 },
            { 0, 1, -1 },
            { 0, 2, 0 }
    };

    public SoleaGroveBushFeature(Codec<NoneFeatureConfiguration> codec) {
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
                || s.is(Blocks.MOSS_BLOCK)
                || s.is(SunwoodBlocks.SURFACE_MOSS.get());
    }

    private static BlockState pickLeaves(RandomSource rand) {
        if (rand.nextBoolean()) {
            return Blocks.DARK_OAK_LEAVES.defaultBlockState()
                    .setValue(LeavesBlock.PERSISTENT, true)
                    .setValue(LeavesBlock.DISTANCE, 1);
        }
        return Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true)
                .setValue(LeavesBlock.DISTANCE, 1);
    }

    private static boolean canHostLeaf(WorldGenLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos.getX(), pos.getZ())) {
            return false;
        }
        BlockState here = level.getBlockState(pos);
        if (here.getFluidState().getType() != Fluids.EMPTY) {
            return false;
        }
        return here.isAir() || here.is(BlockTags.REPLACEABLE_BY_TREES);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        BlockPos origin = context.origin();
        int x = origin.getX();
        int z = origin.getZ();
        int y = surfaceY(level, x, z);
        y = Mth.clamp(y, level.getMinBuildHeight(), level.getMaxBuildHeight() - 3);
        BlockPos base = new BlockPos(x, y, z);
        if (!validGround(level.getBlockState(base))) {
            return false;
        }
        int placed = 0;
        for (int[] o : OFFSETS) {
            if (rand.nextFloat() > 0.78f) {
                continue;
            }
            BlockPos p = base.offset(o[0], o[1] + 1, o[2]);
            if (canHostLeaf(level, p)) {
                level.setBlock(p, pickLeaves(rand), 2);
                placed++;
            }
        }
        return placed > 0;
    }
}
