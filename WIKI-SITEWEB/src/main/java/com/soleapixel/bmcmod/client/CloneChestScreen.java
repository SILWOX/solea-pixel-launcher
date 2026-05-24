package com.soleapixel.bmcmod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import com.soleapixel.bmcmod.menu.CloneInventoryMenu;

/** GUI coffre vanilla 3×12 pour inventaire clone. */
public final class CloneChestScreen extends AbstractContainerScreen<CloneInventoryMenu> {
    private static final ResourceLocation VANILLA_CHEST_GUI = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public CloneChestScreen(CloneInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        int rows = menu.getRowCount();
        this.imageWidth = 176;
        this.imageHeight = 114 + rows * 18;
        this.titleLabelY = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int rows = this.menu.getRowCount();
        // Même découpe que ChestScreen vanilla (coffre simple) :
        // - panneau haut = inventaire coffre (rows * 18 + 17)
        // - panneau bas = inventaire joueur (96)
        int upperHeight = rows * 18 + 17;
        g.blit(VANILLA_CHEST_GUI, x, y, 0, 0, this.imageWidth, upperHeight, 256, 256);
        g.blit(VANILLA_CHEST_GUI, x, y + upperHeight, 0, 126, this.imageWidth, 96, 256, 256);
    }
}
