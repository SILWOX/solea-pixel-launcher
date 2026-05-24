package com.soleapixel.bmcmod.block.upgradetable;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class UpgradeTableBlock extends BaseEntityBlock implements EntityBlock {
    public static final MapCodec<UpgradeTableBlock> CODEC = BlockBehaviour.simpleCodec(UpgradeTableBlock::new);

    public UpgradeTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UpgradeTableBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof UpgradeTableBlockEntity upgradeTable) {
                serverPlayer.openMenu(upgradeTable, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof UpgradeTableBlockEntity upgradeTable) {
                for (int i = 0; i < UpgradeTableBlockEntity.SLOT_COUNT; i++) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, upgradeTable.getItems().getStackInSlot(i));
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof UpgradeTableBlockEntity upgradeTable) {
            return net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(new net.minecraft.world.SimpleContainer(
                    upgradeTable.getItems().getStackInSlot(UpgradeTableBlockEntity.SLOT_UPGRADE),
                    upgradeTable.getItems().getStackInSlot(UpgradeTableBlockEntity.SLOT_CHESTPLATE),
                    upgradeTable.getItems().getStackInSlot(UpgradeTableBlockEntity.SLOT_SHARD),
                    upgradeTable.getItems().getStackInSlot(UpgradeTableBlockEntity.SLOT_OUTPUT)));
        }
        return super.getAnalogOutputSignal(blockState, level, blockPos);
    }
}
