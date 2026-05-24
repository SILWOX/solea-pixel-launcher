package com.soleapixel.bmcmod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Stable wall-mounted head block, paired with SkeletonVillagerHeadBlock item.
 */
public class SkeletonVillagerWallHeadBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<SkeletonVillagerWallHeadBlock> CODEC = simpleCodec(SkeletonVillagerWallHeadBlock::new);
    private static final VoxelShape NORTH_SHAPE = Block.box(4.0D, 4.0D, 8.0D, 12.0D, 12.0D, 16.0D);
    private static final VoxelShape SOUTH_SHAPE = Block.box(4.0D, 4.0D, 0.0D, 12.0D, 12.0D, 8.0D);
    private static final VoxelShape WEST_SHAPE = Block.box(8.0D, 4.0D, 4.0D, 16.0D, 12.0D, 12.0D);
    private static final VoxelShape EAST_SHAPE = Block.box(0.0D, 4.0D, 4.0D, 8.0D, 12.0D, 12.0D);

    public SkeletonVillagerWallHeadBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (Direction dir : context.getNearestLookingDirections()) {
            if (!dir.getAxis().isHorizontal()) {
                continue;
            }
            Direction facing = dir.getOpposite();
            BlockState candidate = this.defaultBlockState().setValue(FACING, facing);
            if (candidate.canSurvive(level, pos)) {
                return candidate;
            }
        }
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}

