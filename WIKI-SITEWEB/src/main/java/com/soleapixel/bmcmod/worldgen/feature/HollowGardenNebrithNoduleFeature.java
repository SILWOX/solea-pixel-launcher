package com.soleapixel.bmcmod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.soleapixel.bmcmod.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Rare : socle calcite + {@link ModBlocks#BUDDING_NEBRITH} et calcite sur les côtés au même niveau. */
public final class HollowGardenNebrithNoduleFeature extends Feature<NoneFeatureConfiguration> {
    public HollowGardenNebrithNoduleFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        BlockPos origin = ctx.origin();
        if (!level.hasChunkAt(origin.getX(), origin.getZ())) {
            return false;
        }
        if (rand.nextFloat() > 0.28F) {
            return false;
        }
        // Placement heightmap = surface solide ; le budding va dans l’air au-dessus.
        BlockPos base = origin;
        if (!level.hasChunkAt(base.getX(), base.getZ())) {
            return false;
        }
        BlockPos bud = base.above();
        if (!level.hasChunkAt(bud.getX(), bud.getZ()) || !level.getBlockState(bud).isAir()) {
            return false;
        }
        BlockState baseState = level.getBlockState(base);
        if (baseState.is(ModBlocks.HOLLOW_GRASS.get()) || baseState.is(Blocks.END_STONE)) {
            level.setBlock(base, Blocks.CALCITE.defaultBlockState(), 3);
        } else if (!baseState.is(Blocks.CALCITE)) {
            return false;
        }
        level.setBlock(bud, ModBlocks.BUDDING_NEBRITH.get().defaultBlockState(), 3);
        for (BlockPos d : new BlockPos[] { bud.north(), bud.south(), bud.east(), bud.west() }) {
            if (!level.hasChunkAt(d.getX(), d.getZ())) {
                continue;
            }
            BlockState s = level.getBlockState(d);
            if (s.isAir()) {
                level.setBlock(d, Blocks.CALCITE.defaultBlockState(), 3);
            }
        }
        return true;
    }
}
