package com.soleapixel.bmcmod.block.chest;

import com.soleapixel.bmcmod.registry.ModBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Coffre factice : rendu vanilla ; pas d’inventaire exploitable (voir {@link FakeChestBlock}). */
public class FakeChestBlockEntity extends ChestBlockEntity {

    public FakeChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.FAKE_CHEST.get(), pos, state);
    }
}
