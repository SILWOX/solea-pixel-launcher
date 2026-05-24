package com.soleapixel.bmcmod.menu;

import org.jetbrains.annotations.NotNull;

import com.soleapixel.bmcmod.block.foundry.FoundryBlockEntity;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class FoundryMenu extends AbstractContainerMenu {
    public final FoundryBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public FoundryMenu(@NotNull MenuType<?> type, int id, @NotNull Inventory inv, @NotNull FoundryBlockEntity be) {
        super(type, id);
        this.blockEntity = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        var items = be.getItems();

        this.addSlot(new SlotItemHandler(items, FoundryBlockEntity.SLOT_INPUT, 56, 17));
        this.addSlot(new SlotItemHandler(items, FoundryBlockEntity.SLOT_FUEL, 56, 53));
        this.addSlot(new SlotItemHandler(items, FoundryBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });
        this.addDataSlots(be.dataAccess);

        int yPlayer = 84;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                this.addSlot(new Slot(inv, c + 9 + r * 9, 8 + c * 18, yPlayer + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(inv, c, 8 + c * 18, yPlayer + 58));
        }
    }

    public int getBurnLeftScaled(int pixels) {
        int lit = this.blockEntity.dataAccess.get(0);
        int total = Math.max(1, this.blockEntity.dataAccess.get(1));
        return lit > 0 ? Math.max(1, lit * pixels / total) : 0;
    }

    public int getCookProgressScaled(int pixels) {
        int cook = this.blockEntity.dataAccess.get(2);
        int total = Math.max(1, this.blockEntity.dataAccess.get(3));
        return cook > 0 ? Math.min(pixels, cook * pixels / total) : 0;
    }

    public boolean isLit() {
        return this.blockEntity.dataAccess.get(0) > 0;
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
        int machineEnd = 3;
        int invStart = machineEnd;
        int invEnd = this.slots.size();
        if (index < machineEnd) {
            if (!this.moveItemStackTo(stack, invStart, invEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (FoundryBlockEntity.isFuel(stack)) {
                if (!this.moveItemStackTo(stack, FoundryBlockEntity.SLOT_FUEL, FoundryBlockEntity.SLOT_FUEL + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, FoundryBlockEntity.SLOT_INPUT, FoundryBlockEntity.SLOT_INPUT + 1, false)) {
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
}
