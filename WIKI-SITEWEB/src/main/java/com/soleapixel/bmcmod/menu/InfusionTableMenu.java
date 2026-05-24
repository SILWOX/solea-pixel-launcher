package com.soleapixel.bmcmod.menu;

import com.soleapixel.bmcmod.block.infusion.InfusionTableBlockEntity;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class InfusionTableMenu extends AbstractContainerMenu {
    public static final int BUTTON_ID_START = 0;
    public final InfusionTableBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public InfusionTableMenu(
            @NotNull MenuType<?> type, int id, @NotNull Inventory inv, @NotNull InfusionTableBlockEntity be) {
        super(type, id);
        this.blockEntity = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        var items = be.getItems();
        // Coords calées sur l’art 0…176 (les slots “8,8 / 8,40” tombaient ~17 px à gauche des découpes).
        this.addSlot(new SlotItemHandler(items, InfusionTableBlockEntity.SLOT_CRYSTAL, 25, 20));
        this.addSlot(new SlotItemHandler(items, 1, 25, 49));
        this.addSlot(new SlotItemHandler(items, 2, 46, 49));
        this.addSlot(new SlotItemHandler(items, 3, 25, 70));
        this.addSlot(new SlotItemHandler(items, 4, 46, 70));
        this.addSlot(new SlotItemHandler(items, InfusionTableBlockEntity.SLOT_OUTPUT, 130, 59));
        this.addDataSlots(be.dataAccess);
        /* Fenêtre 176x184 (coin haut-gauche) : 3x9 + hotbar, comme le four, décalage pour
         * tenir dans la hauteur (hautbar 160+16<184 ; avant y=140 faisait tomber l’inventaire sous le panneau). */
        int yPlayer = 102;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                this.addSlot(
                        new Slot(inv, c + 9 + r * 9, 8 + c * 18, yPlayer + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(inv, c, 8 + c * 18, yPlayer + 58));
        }
    }

    public int getField(int index) {
        return this.blockEntity.dataAccess.get(index);
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
        int invStart = 6;
        int invEnd = this.slots.size();
        if (index < invStart) {
            if (!this.moveItemStackTo(stack, invStart, invEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, 0, 6, false)) {
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
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id == BUTTON_ID_START) {
            if (player instanceof net.minecraft.server.level.ServerPlayer) {
                this.blockEntity.tryStartInfusion();
            }
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        this.blockEntity.returnContentsToPlayerOnMenuClosed(player);
    }
}
