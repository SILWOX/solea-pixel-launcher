package com.soleapixel.bmcmod.menu;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Conteneur coffre vanilla ({@link net.minecraft.world.inventory.ChestMenu}) avec règles d’insertion pour les sacs.
 */
public final class BackpackContainer extends SimpleContainer {
    public BackpackContainer(int size) {
        super(size);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return BackpackRules.mayPlaceInBackpack(stack);
    }
}
