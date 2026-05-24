package com.soleapixel.bmcmod.client.foundry;

import org.jetbrains.annotations.NotNull;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.menu.FoundryMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class FoundryScreen extends AbstractContainerScreen<FoundryMenu> {
    private static final ResourceLocation GUI = BmcMod.loc("textures/gui/foundry/foundry.png");
    private static final ResourceLocation LIT_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    private static final ResourceLocation BURN_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");

    public FoundryScreen(FoundryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(GUI, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        if (this.menu.isLit()) {
            int lit = this.menu.getBurnLeftScaled(13);
            guiGraphics.blitSprite(LIT_PROGRESS_SPRITE, 14, 14, 0, 14 - lit, x + 56, y + 36 + (14 - lit), 14, lit);
        }
        int cook = this.menu.getCookProgressScaled(24);
        guiGraphics.blitSprite(BURN_PROGRESS_SPRITE, 24, 16, 0, 0, x + 79, y + 34, cook, 16);
    }
}
