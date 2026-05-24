package com.soleapixel.bmcmod.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.RecipeType;

import com.soleapixel.bmcmod.block.endstonefurnace.EndstoneFurnaceBlockEntity;
import com.soleapixel.bmcmod.registry.ModMenus;

public class EndstoneFurnaceMenu extends AbstractFurnaceMenu {
    public EndstoneFurnaceMenu(int containerId, Inventory playerInventory, EndstoneFurnaceBlockEntity be) {
        super(ModMenus.ENDSTONE_FURNACE.get(), RecipeType.SMELTING, RecipeBookType.FURNACE, containerId, playerInventory, be, be.getFurnaceDataAccess());
    }
}
