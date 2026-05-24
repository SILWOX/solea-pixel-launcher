package com.soleapixel.bmcmod.block.nebrith;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class NebrithBudBlock extends AmethystClusterBlock {
    private final Supplier<Block> nextStage;

    public NebrithBudBlock(float height, float xzOffset, Properties properties, Supplier<Block> nextStage) {
        super(height, xzOffset, properties);
        this.nextStage = nextStage;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(20) == 0) {
            BlockState grown = nextStage.get().defaultBlockState()
                    .setValue(FACING, state.getValue(FACING))
                    .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
            level.setBlock(pos, grown, 3);
        }
    }
}
