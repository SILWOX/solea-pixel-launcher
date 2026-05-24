package com.soleapixel.bmcmod.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Ancre de réapparition pour l’End : même logique que l’ancre Nether vanilla
 * ({@link RespawnAnchorBlock}), activée uniquement dans {@link net.minecraft.world.level.Level#END}
 * via {@link com.soleapixel.bmcmod.mixin.RespawnAnchorBlockMixin}.
 */
public final class EndAnchorBlock extends RespawnAnchorBlock {

    public static final MapCodec<EndAnchorBlock> CODEC = BlockBehaviour.simpleCodec(EndAnchorBlock::new);

    public EndAnchorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MapCodec<RespawnAnchorBlock> codec() {
        return (MapCodec<RespawnAnchorBlock>) (MapCodec<?>) CODEC;
    }
}
