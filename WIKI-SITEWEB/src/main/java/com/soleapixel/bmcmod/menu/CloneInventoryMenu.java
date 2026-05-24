package com.soleapixel.bmcmod.menu;

import com.soleapixel.bmcmod.entity.CloneEntity;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Coffre vanilla 3×9 lié au stockage d’un clone.
 */
public final class CloneInventoryMenu extends ChestMenu {
    private final CloneEntity clone;

    public CloneInventoryMenu(MenuType<? extends ChestMenu> menuType, int syncId, Inventory playerInventory, CloneEntity clone) {
        super(menuType, syncId, playerInventory, clone.getCloneInventory(), 3);
        this.clone = clone;
    }

    public CloneEntity getClone() {
        return clone;
    }

    @Override
    public boolean stillValid(Player player) {
        return clone.isAlive()
                && clone.getUUID() != null
                && !clone.isRemoved()
                && clone.getOwnerUuid() != null
                && clone.getOwnerUuid().equals(player.getUUID());
    }
}
