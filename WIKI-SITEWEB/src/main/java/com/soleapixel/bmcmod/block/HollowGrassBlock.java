package com.soleapixel.bmcmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;

/**
 * Sol végétalisé du Hollow Garden : ne dépend pas d’un bloc en dessous.
 * Repasse en pierre de l’End uniquement si le bloc du dessus {@linkplain BlockState#canOcclude() bloque la lumière}
 * est directement au-dessus (pas sur simple lumière / herbe courte).
 */
public final class HollowGrassBlock extends Block {
    public HollowGrassBlock(Properties properties) {
        super(properties);
    }

    private static boolean hasOpaqueCrushingBlockAbove(LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState state = level.getBlockState(above);
        if (state.isAir()) {
            return false;
        }
        return state.canOcclude();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (hasOpaqueCrushingBlockAbove(level, pos)) {
            level.setBlock(pos, Blocks.END_STONE.defaultBlockState(), 3);
        }
    }
}
