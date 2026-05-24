package com.soleapixel.bmcmod.block.nebrith;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Comme {@link net.minecraft.world.level.block.BuddingAmethystBlock} : le bourgeon est placé dans la case voisine,
 * et {@link AmethystClusterBlock#FACING} pointe vers la direction de pousse (donc collé à la face du budding).
 */
public class BuddingNebrithBlock extends Block {
    private final Supplier<Block> smallBud;

    public BuddingNebrithBlock(Properties properties, Supplier<Block> smallBud) {
        super(properties);
        this.smallBud = smallBud;
    }

    private static boolean canGrowInto(BlockState neighbor) {
        return neighbor.isAir() || (neighbor.is(Blocks.WATER) && neighbor.getFluidState().isSource());
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) {
            return;
        }
        Direction direction = Direction.getRandom(random);
        BlockPos face = pos.relative(direction);
        BlockState neighbor = level.getBlockState(face);
        if (!canGrowInto(neighbor)) {
            return;
        }
        Block bud = smallBud.get();
        BlockState placed = bud.defaultBlockState()
                .setValue(AmethystClusterBlock.FACING, direction)
                .setValue(AmethystClusterBlock.WATERLOGGED, neighbor.getFluidState().isSourceOfType(Fluids.WATER));
        if (!placed.canSurvive(level, face)) {
            return;
        }
        level.setBlock(face, placed, 3);
    }
}
