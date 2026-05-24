package com.soleapixel.bmcmod.block.hollow;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;

public class HollowLogBlock extends RotatedPillarBlock {
    private final Supplier<Block> strippedBlock;

    public HollowLogBlock(Properties properties, Supplier<Block> strippedBlock) {
        super(properties);
        this.strippedBlock = strippedBlock;
    }

    @Override
    public @Nullable BlockState getToolModifiedState(
            BlockState state,
            UseOnContext context,
            net.neoforged.neoforge.common.ItemAbility itemAbility,
            boolean simulate) {
        if (itemAbility == ItemAbilities.AXE_STRIP) {
            return strippedBlock.get().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
        }
        return super.getToolModifiedState(state, context, itemAbility, simulate);
    }
}
