package com.soleapixel.bmcmod.client.endstonefurnace;

import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.recipebook.SmeltingRecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import com.soleapixel.bmcmod.menu.EndstoneFurnaceMenu;

/** Même GUI que le four vanilla (fond + sprites de progression). */
public class EndstoneFurnaceScreen extends AbstractFurnaceScreen<EndstoneFurnaceMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final ResourceLocation LIT_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    private static final ResourceLocation BURN_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");

    public EndstoneFurnaceScreen(EndstoneFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, new SmeltingRecipeBookComponent(), inventory, title, TEXTURE, LIT_PROGRESS_SPRITE, BURN_PROGRESS_SPRITE);
    }
}
