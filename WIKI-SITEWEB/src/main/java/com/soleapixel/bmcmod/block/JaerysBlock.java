package com.soleapixel.bmcmod.block;

import com.soleapixel.bmcmod.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Petite flore de l’End ; pousse sur pierre de l’End ou herbe de chorus.
 * Particules légères type bâton de l’End / portail inversé (côté client).
 */
public class JaerysBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public JaerysBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    private static boolean mayPlaceOn(BlockState state) {
        return state.is(Blocks.END_STONE) || state.is(ModBlocks.HOLLOW_GRASS.get());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return mayPlaceOn(level.getBlockState(pos.below()));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(6) != 0) {
            return;
        }
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.45;
        double y = pos.getY() + 0.15 + random.nextDouble() * 0.55;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.45;
        if (random.nextBoolean()) {
            level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.0, 0.0);
        } else {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0.0, 0.01 + random.nextDouble() * 0.02, 0.0);
        }
    }

    public static BlockBehaviour.Properties jaerysProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GREEN)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .offsetType(Block.OffsetType.XZ)
                .pushReaction(PushReaction.DESTROY);
    }
}
