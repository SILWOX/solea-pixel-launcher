package com.soleapixel.bmcmod.client.upgradetable;

import org.jetbrains.annotations.NotNull;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.menu.UpgradeTableMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class UpgradeTableScreen extends AbstractContainerScreen<UpgradeTableMenu> {
    private static final ResourceLocation GUI = BmcMod.loc("textures/gui/upgrade_table/upgrade_table.png");
    private static final int SLOT_UPGRADE_X = 34;
    private static final int SLOT_CHEST_X = 52;
    private static final int SLOT_SHARD_X = 70;
    private static final int SLOT_OUTPUT_X = 124;
    private static final int SLOT_Y = 48;

    public UpgradeTableScreen(UpgradeTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 41;
        this.titleLabelY = 19;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(GUI, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            return;
        }
        if (isHovering(SLOT_UPGRADE_X, SLOT_Y, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.bmcmod.upgrade_table.slot.upgrade"), mouseX, mouseY);
        } else if (isHovering(SLOT_CHEST_X, SLOT_Y, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.bmcmod.upgrade_table.slot.chestplate"), mouseX, mouseY);
        } else if (isHovering(SLOT_SHARD_X, SLOT_Y, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.bmcmod.upgrade_table.slot.shard"), mouseX, mouseY);
        } else if (isHovering(SLOT_OUTPUT_X, SLOT_Y, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.bmcmod.upgrade_table.slot.result"), mouseX, mouseY);
        }
    }
}
