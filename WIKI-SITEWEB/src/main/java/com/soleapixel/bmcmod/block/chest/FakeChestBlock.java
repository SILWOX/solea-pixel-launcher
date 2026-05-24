package com.soleapixel.bmcmod.block.chest;

import org.jetbrains.annotations.Nullable;

import com.soleapixel.bmcmod.entity.MimicChest;
import com.soleapixel.bmcmod.registry.ModBlockEntityTypes;
import com.soleapixel.bmcmod.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Apparence et hitbox type coffre vanilla ; clic (main vide ou avec item) : supprime le bloc et fait apparaître un mimic.
 */
public class FakeChestBlock extends ChestBlock {

    public FakeChestBlock(Properties properties) {
        super(properties, () -> ModBlockEntityTypes.FAKE_CHEST.get());
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FakeChestBlockEntity(pos, state);
    }

    @Nullable
    @Override
    protected net.minecraft.world.MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return null;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }
        spawnMimicAndClear(level, pos, state);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult) {
        if (level.isClientSide) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        spawnMimicAndClear(level, pos, state);
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    private static void spawnMimicAndClear(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        ChestType type = state.getValue(ChestBlock.TYPE);
        ChestBlockEntity withLoot = null;
        BlockEntity at = sl.getBlockEntity(pos);
        if (at instanceof ChestBlockEntity ce && ce.getLootTable() != null) {
            withLoot = ce;
        } else if (type == ChestType.LEFT || type == ChestType.RIGHT) {
            BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
            BlockEntity ato = sl.getBlockEntity(other);
            if (ato instanceof ChestBlockEntity oce && oce.getLootTable() != null) {
                withLoot = oce;
            }
        }
        if (withLoot != null) {
            withLoot.unpackLootTable(null);
        }
        BlockEntity dropFrom = sl.getBlockEntity(pos);
        if (dropFrom instanceof ChestBlockEntity chestDrop) {
            Containers.dropContents(sl, pos, chestDrop);
        }
        if (type == ChestType.LEFT || type == ChestType.RIGHT) {
            BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
            sl.removeBlock(other, false);
        }
        sl.removeBlock(pos, false);

        MimicChest mimic = ModEntities.MIMIC_CHEST.get().create(sl);
        if (mimic == null) {
            return;
        }
        float yRot = state.getValue(ChestBlock.FACING).toYRot();
        mimic.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yRot, 0.0F);
        mimic.setYHeadRot(yRot);
        mimic.setYBodyRot(yRot);
        mimic.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.TRIGGERED, null);
        sl.addFreshEntityWithPassengers(mimic);
        sl.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.55F, 0.85F + sl.random.nextFloat() * 0.1F);
    }
}
