package com.soleapixel.bmcmod.block.endstonefurnace;

import com.mojang.serialization.MapCodec;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import com.soleapixel.bmcmod.registry.ModBlockEntityTypes;

/** Four en pierre de l’End : même logique qu’un four vanilla (orientation, lit, tick), BE et menu dédiés. */
public class EndstoneFurnaceBlock extends AbstractFurnaceBlock {
    public static final MapCodec<EndstoneFurnaceBlock> CODEC = BlockBehaviour.simpleCodec(EndstoneFurnaceBlock::new);

    public EndstoneFurnaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends AbstractFurnaceBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EndstoneFurnaceBlockEntity(pos, state);
    }

    @Override
    @Nullable
    @SuppressWarnings("DataFlowIssue")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(
                type,
                ModBlockEntityTypes.ENDSTONE_FURNACE.get(),
                net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity::serverTick);
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EndstoneFurnaceBlockEntity furnace) {
                serverPlayer.openMenu(furnace, buf -> buf.writeBlockPos(pos));
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        if (random.nextDouble() < 0.1) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
        Direction dir = state.getValue(FACING);
        Direction.Axis axis = dir.getAxis();
        double h = random.nextDouble() * 0.6 - 0.3;
        double ox = axis == Direction.Axis.X ? dir.getStepX() * 0.52 : h;
        double oy = random.nextDouble() * 6.0 / 16.0;
        double oz = axis == Direction.Axis.Z ? dir.getStepZ() * 0.52 : h;
        level.addParticle(ParticleTypes.SMOKE, x + ox, y + oy, z + oz, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x + ox, y + oy, z + oz, 0.0, 0.0, 0.0);
    }
}
