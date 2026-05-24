package com.soleapixel.bmcmod.menu;

import org.jetbrains.annotations.NotNull;

import com.soleapixel.bmcmod.block.upgradetable.UpgradeTableBlockEntity;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.items.SlotItemHandler;

public class UpgradeTableMenu extends AbstractContainerMenu {
    public final UpgradeTableBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public UpgradeTableMenu(@NotNull MenuType<?> type, int id, @NotNull Inventory inv, @NotNull UpgradeTableBlockEntity be) {
        super(type, id);
        this.blockEntity = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        var items = be.getItems();

        this.addSlot(new SlotItemHandler(items, UpgradeTableBlockEntity.SLOT_UPGRADE, 34, 48));
        this.addSlot(new SlotItemHandler(items, UpgradeTableBlockEntity.SLOT_CHESTPLATE, 52, 48));
        this.addSlot(new SlotItemHandler(items, UpgradeTableBlockEntity.SLOT_SHARD, 70, 48));
        this.addSlot(new SlotItemHandler(items, UpgradeTableBlockEntity.SLOT_OUTPUT, 124, 48) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
                super.onTake(player, stack);
                blockEntity.consumeCraftInputs();
                blockEntity.refreshOutput();
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.85F, 1.0F);
            }
        });

        this.addDataSlots(be.dataAccess);
        int yPlayer = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + 9 + row * 9, 8 + col * 18, yPlayer + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, yPlayer + 58));
        }
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        int machineEnd = 4;
        int invStart = machineEnd;
        int invEnd = this.slots.size();
        if (index < machineEnd) {
            if (!this.moveItemStackTo(stack, invStart, invEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (UpgradeTableBlockEntity.isUpgradeIngredient(stack)) {
                if (!this.moveItemStackTo(stack, UpgradeTableBlockEntity.SLOT_UPGRADE, UpgradeTableBlockEntity.SLOT_UPGRADE + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (UpgradeTableBlockEntity.isChestplate(stack)) {
                if (!this.moveItemStackTo(stack, UpgradeTableBlockEntity.SLOT_CHESTPLATE, UpgradeTableBlockEntity.SLOT_CHESTPLATE + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (UpgradeTableBlockEntity.isShard(stack)) {
                if (!this.moveItemStackTo(stack, UpgradeTableBlockEntity.SLOT_SHARD, UpgradeTableBlockEntity.SLOT_SHARD + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(this.access, player, this.blockEntity.getBlockState().getBlock());
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (player.level().isClientSide()) {
            return;
        }
        var items = this.blockEntity.getItems();
        for (int slot : new int[] {
                UpgradeTableBlockEntity.SLOT_UPGRADE,
                UpgradeTableBlockEntity.SLOT_CHESTPLATE,
                UpgradeTableBlockEntity.SLOT_SHARD
        }) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack.copy());
                items.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        items.setStackInSlot(UpgradeTableBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
        this.blockEntity.refreshOutput();
    }
}
