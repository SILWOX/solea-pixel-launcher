package com.soleapixel.bmcmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.block.feeder.FeederBlockEntity;
import com.soleapixel.bmcmod.registry.ModBlocks;

@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class FeederLookHud {
    private static final int LINE_SPACING = 10;
    private static final int MARGIN_BOTTOM = 10;
    private static final int MARGIN_LEFT = 8;
    private static final int PAD_LEFT_ICON = 10;
    private static final int PAD_RIGHT_TEXT = 14;
    private static final int GAP_ICON_TEXT = 10;
    private static final int PAD_TOP_BOTTOM = 10;

    private FeederLookHud() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null) {
            return;
        }
        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult bhr) || bhr.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockEntity be = mc.level.getBlockEntity(bhr.getBlockPos());
        if (!(be instanceof FeederBlockEntity feeder)) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        int h = mc.getWindow().getGuiScaledHeight();
        int count = feeder.getStoredCount();

        Component title = Component.translatable("hud.bmcmod.feeder.title");
        Component line2;
        Component line3 = null;
        if (count <= 0 || feeder.getRepresentativeStack().isEmpty()) {
            line2 = Component.translatable("hud.bmcmod.feeder.empty_detail");
        } else {
            Component food = feeder.getRepresentativeStack().getHoverName();
            line2 = Component.translatable("hud.bmcmod.feeder.food_line", food);
            line3 = Component.translatable("hud.bmcmod.feeder.quantity_line", count);
        }

        int lineCount = line3 != null ? 3 : 2;
        int textBlockH = lineCount * LINE_SPACING;
        int panelH = PAD_TOP_BOTTOM + textBlockH + PAD_TOP_BOTTOM;

        int maxText = font.width(title);
        maxText = Math.max(maxText, font.width(line2));
        if (line3 != null) {
            maxText = Math.max(maxText, font.width(line3));
        }

        int px = MARGIN_LEFT;
        int py = h - panelH - MARGIN_BOTTOM;
        int iconSlotX = px + PAD_LEFT_ICON;
        int iconX = iconSlotX + 1;
        int iconY = py + (panelH - 16) / 2;
        int textX = iconSlotX + 16 + GAP_ICON_TEXT;
        int panelW = Math.max(140, textX - px + maxText + PAD_RIGHT_TEXT);

        drawPanel(g, px, py, panelW, panelH);
        drawIconSlot(g, iconSlotX - 1, iconY - 1);

        ItemStack iconStack;
        if (count <= 0 || feeder.getRepresentativeStack().isEmpty()) {
            iconStack = new ItemStack(ModBlocks.FEEDER.get().asItem());
        } else {
            iconStack = feeder.getRepresentativeStack().copy();
            iconStack.setCount(1);
        }
        g.renderItem(iconStack, iconX, iconY);
        g.renderItemDecorations(font, iconStack, iconX, iconY);

        int ty = py + PAD_TOP_BOTTOM;
        int colorTitle = 0xFFF8F4EC;
        int colorBody = 0xFFE4DFD4;
        drawOutlinedString(g, font, title, textX, ty, colorTitle);
        ty += LINE_SPACING;
        g.drawString(font, line2, textX, ty, colorBody, true);
        if (line3 != null) {
            ty += LINE_SPACING;
            g.drawString(font, line3, textX, ty, colorBody, true);
        }
    }

    /** Contour discret pour le titre (plus lisible sur le fond). */
    private static void drawOutlinedString(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        int outline = 0xFF101010;
        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                if (ox != 0 || oy != 0) {
                    g.drawString(font, text, x + ox, y + oy, outline, false);
                }
            }
        }
        g.drawString(font, text, x, y, color, false);
    }

    private static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xAA050608);
        g.fill(x, y, x + w, y + h, 0xD8142018);
        g.fill(x, y, x + w, y + 1, 0x5590C8A8);
        g.fill(x, y + h - 1, x + w, y + h, 0x44000000);
        g.fill(x, y, x + 1, y + h, 0x44807068);
        g.fill(x + w - 1, y, x + w, y + h, 0x33000000);
    }

    private static void drawIconSlot(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF12100E);
        g.fill(x, y, x + 16, y + 16, 0xFF2C2620);
        g.fill(x + 1, y + 1, x + 15, y + 3, 0x28FFFFFF);
        g.fill(x + 1, y + 14, x + 15, y + 15, 0x18000000);
    }
}
