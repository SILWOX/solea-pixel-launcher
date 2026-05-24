package com.soleapixel.bmcmod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.soleapixel.bmcmod.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Haute herbe en deux blocs (lower + upper) sur herbe creuse. */
public final class HollowGardenTallGrassFeature extends Feature<NoneFeatureConfiguration> {
    public HollowGardenTallGrassFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        int x = origin.getX();
        int z = origin.getZ();
        if (!level.hasChunkAt(x, z)) {
            return false;
        }
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        for (int yy = top; yy >= top - 10; yy--) {
            BlockPos ground = new BlockPos(x, yy, z);
            if (!level.getBlockState(ground).is(ModBlocks.HOLLOW_GRASS.get())) {
                continue;
            }
            BlockPos lower = ground.above();
            BlockPos upper = lower.above();
            if (!level.getBlockState(lower).isAir() || !level.getBlockState(upper).isAir()) {
                return false;
            }
            BlockState tall = Blocks.TALL_GRASS.defaultBlockState();
            DoublePlantBlock.placeAt(level, tall, lower, 3);
            return true;
        }
        return false;
    }
}
