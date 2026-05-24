package com.soleapixel.bmcmod.block.banner;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class UndeadCaptainWallBannerBlock extends WallBannerBlock {
    public UndeadCaptainWallBannerBlock(BlockBehaviour.Properties props) {
        super(DyeColor.BLACK, props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BannerBlockEntity(pos, state, DyeColor.BLACK);
    }
}
