package com.soleapixel.bmcmod.block.chest;

import com.soleapixel.bmcmod.registry.ModBlockEntityTypes;
import com.soleapixel.bmcmod.registry.ModItems;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.MenuProvider;

/**
 * Coffre vanilla (GUI identique) ; améliorations 0–3 : clic droit avec
 * {@link ModItems#ENCHANTED_CHEST_UPGRADE} pour en ajouter ; si 3/3, message d’action « max atteint »
 * (sans ouvrir). Retrait d’1 amélioration : cisaille ou n’importe quelle hache (1 item rendu, usure de l’outil).
 */
public class EnchantedChestBlock extends ChestBlock {
    public EnchantedChestBlock(Properties properties) {
        super(properties, () -> ModBlockEntityTypes.ENCHANTED_CHEST.get());
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnchantedChestBlockEntity(pos, state);
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        BlockEntityTicker<T> vanilla = (BlockEntityTicker<T>) super.getTicker(level, state, type);
        if (level.isClientSide) {
            return vanilla;
        }
        if (vanilla == null) {
            return (l, p, s, be) -> {
                if (be instanceof EnchantedChestBlockEntity e) {
                    e.tickEnchanted();
                }
            };
        }
        return (l, p, s, be) -> {
            vanilla.tick(l, p, s, be);
            if (be instanceof EnchantedChestBlockEntity e) {
                e.tickEnchanted();
            }
        };
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide() && !isMoving) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EnchantedChestBlockEntity e) {
                e.dropUpgrades();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
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
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof EnchantedChestBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (EnchantedChestBlockEntity.isToolForRemovalUpgrade(stack) && be.getVisibleUpgradeCount() > 0) {
            if (level.isClientSide) {
                return ItemInteractionResult.sidedSuccess(true);
            }
            if (be.tryRemoveOneUpgradeWithTool(player, stack, hand)) {
                return ItemInteractionResult.CONSUME;
            }
        }
        if (stack.is(ModItems.ENCHANTED_CHEST_UPGRADE.get())) {
            if (be.getVisibleUpgradeCount() < 3) {
                if (level.isClientSide) {
                    return ItemInteractionResult.sidedSuccess(true);
                }
                if (be.tryApplyUpgradeFromItem(player, stack, hand)) {
                    return ItemInteractionResult.CONSUME;
                }
            } else {
                if (level.isClientSide) {
                    return ItemInteractionResult.sidedSuccess(true);
                }
                if (player instanceof ServerPlayer sp) {
                    sp.displayClientMessage(
                            Component.translatable("message.bmcmod.enchanted_chest_upgrade_max"), true);
                }
                return ItemInteractionResult.sidedSuccess(true);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    @Nullable
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof EnchantedChestBlockEntity chest)) {
            return super.getMenuProvider(state, level, pos);
        }
        MenuProvider inner = super.getMenuProvider(state, level, pos);
        if (inner == null) {
            return null;
        }
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return chest.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
                return inner.createMenu(containerId, inv, player);
            }
        };
    }
}
