package com.soleapixel.bmcmod.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.InteractionHand;

/**
 * Réutilise le coffre vanilla ({@link ChestMenu}) : disposition, texture et {@link net.minecraft.client.gui.screens.inventory.ChestScreen}.
 */
public final class BackpackChestMenu extends ChestMenu {
    private final BackpackContainer backpack;
    private final InteractionHand hand;
    private final Inventory playerMenuInventory;
    /** Évite une récursion infinie quand l’éjection modifie le conteneur pendant {@link #slotsChanged}. */
    private boolean skipRecursiveSlotsChanged;

    public BackpackChestMenu(MenuType<? extends ChestMenu> menuType, int containerId, Inventory playerInventory,
            BackpackContainer backpack, int rows, InteractionHand hand) {
        super(menuType, containerId, playerInventory, backpack, rows);
        this.backpack = backpack;
        this.hand = hand;
        this.playerMenuInventory = playerInventory;
    }

    private Player owner() {
        return this.playerMenuInventory.player;
    }

    /**
     * Bloque shift-clic et transferts vers les emplacements du sac si l’objet est un autre sac ou une shulker box
     * (indices d’emplacements comme {@link ChestMenu} : d’abord le contenu du sac, puis l’inventaire joueur).
     */
    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean fromLast) {
        int backpackSlotCount = this.backpack.getContainerSize();
        int overlapEnd = Math.min(endIndex, backpackSlotCount);
        if (startIndex < overlapEnd && !BackpackRules.mayPlaceInBackpack(stack)) {
            return false;
        }
        return super.moveItemStackTo(stack, startIndex, endIndex, fromLast);
    }

    @Override
    public boolean stillValid(Player player) {
        return BackpackRules.isBackpack(player.getItemInHand(this.hand));
    }

    @Override
    public void slotsChanged(Container container) {
        if (this.skipRecursiveSlotsChanged) {
            super.slotsChanged(container);
            return;
        }
        if (!this.owner().level().isClientSide() && container == this.backpack) {
            this.skipRecursiveSlotsChanged = true;
            try {
                ejectDisallowedStacksFromBackpack();
            } finally {
                this.skipRecursiveSlotsChanged = false;
            }
        }
        super.slotsChanged(container);
        if (!this.owner().level().isClientSide() && container == this.backpack) {
            ItemStack held = this.owner().getItemInHand(this.hand);
            if (!held.isEmpty() && BackpackRules.isBackpack(held)) {
                BackpackStorage.saveFrom(held, this.backpack);
            }
        }
    }

    /**
     * Filet de sécurité : certains transferts (ex. glisser-déposer rapide) peuvent contourner {@link BackpackContainer#canPlaceItem}.
     */
    private void ejectDisallowedStacksFromBackpack() {
        Player p = this.owner();
        for (int i = 0; i < this.backpack.getContainerSize(); i++) {
            ItemStack stack = this.backpack.getItem(i);
            if (stack.isEmpty() || BackpackRules.mayPlaceInBackpack(stack)) {
                continue;
            }
            p.drop(stack, false);
            this.backpack.setItem(i, ItemStack.EMPTY);
        }
    }

    @Override
    public void removed(Player player) {
        if (!player.level().isClientSide()) {
            this.skipRecursiveSlotsChanged = true;
            try {
                ejectDisallowedStacksFromBackpack();
            } finally {
                this.skipRecursiveSlotsChanged = false;
            }
            ItemStack held = player.getItemInHand(this.hand);
            if (!held.isEmpty() && BackpackRules.isBackpack(held)) {
                BackpackStorage.saveFrom(held, this.backpack);
            }
        }
        super.removed(player);
    }
}
