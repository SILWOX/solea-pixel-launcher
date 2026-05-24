package com.soleapixel.bmcmod.menu;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class BackpackStorage {
    private BackpackStorage() {
    }

    public static void loadInto(ItemStack backpack, SimpleContainer container) {
        NonNullList<ItemStack> list = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(list);
        for (int i = 0; i < list.size(); i++) {
            ItemStack fromData = list.get(i).copy();
            if (!BackpackRules.mayPlaceInBackpack(fromData)) {
                container.setItem(i, ItemStack.EMPTY);
            } else {
                container.setItem(i, fromData);
            }
        }
    }

    public static void saveFrom(ItemStack backpack, Container container) {
        if (backpack.isEmpty() || !BackpackRules.isBackpack(backpack)) {
            return;
        }
        NonNullList<ItemStack> list = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++) {
            ItemStack inSlot = container.getItem(i);
            if (!BackpackRules.mayPlaceInBackpack(inSlot)) {
                list.set(i, ItemStack.EMPTY);
            } else {
                list.set(i, inSlot.copy());
            }
        }
        backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
    }
}
