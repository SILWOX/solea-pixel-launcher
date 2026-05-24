package com.soleapixel.bmcmod.block.infusion;

import com.mojang.serialization.MapCodec;

import javax.annotation.Nullable;

import com.soleapixel.bmcmod.registry.ModBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Table d'infusion : mêmes particules et animation du livre qu’une table d’enchantement, avec
 * interface de craft.
 */
public class InfusionTableBlock extends BaseEntityBlock {
    public static final MapCodec<InfusionTableBlock> CODEC = simpleCodec(InfusionTableBlock::new);
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);

    public InfusionTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<InfusionTableBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        for (BlockPos blockpos : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (random.nextInt(16) == 0 && EnchantingTableBlock.isValidBookShelf(level, pos, blockpos)) {
                level.addParticle(
                        ParticleTypes.ENCHANT,
                        (double) pos.getX() + 0.5,
                        (double) pos.getY() + 2.0,
                        (double) pos.getZ() + 0.5,
                        (double) ((float) blockpos.getX() + random.nextFloat()) - 0.5,
                        (double) ((float) blockpos.getY() - random.nextFloat() - 1.0F),
                        (double) ((float) blockpos.getZ() + random.nextFloat()) - 0.5);
            }
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfusionTableBlockEntity(pos, state);
    }

    @Override
    @Nullable
    @SuppressWarnings("DataFlowIssue")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(
                type,
                ModBlockEntityTypes.INFUSION_TABLE.get(),
                (l, p, s, t) -> {
                    if (l.isClientSide()) {
                        InfusionTableBlockEntity.bookAnimationTick(l, p, s, t);
                    } else {
                        t.serverTick();
                    }
                });
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof MenuProvider p) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                sp.openMenu(p, output -> output.writeBlockPos(pos));
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof MenuProvider p) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                sp.openMenu(p, output -> output.writeBlockPos(pos));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Nullable
    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof InfusionTableBlockEntity be) {
            return be;
        }
        return null;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && !level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof InfusionTableBlockEntity t) {
                t.dropAllContents(level, pos, state);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
