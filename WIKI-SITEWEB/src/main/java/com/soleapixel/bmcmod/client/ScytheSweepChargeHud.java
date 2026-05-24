package com.soleapixel.bmcmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import com.soleapixel.bmcmod.BmcMod;

/** Barre + texte (ombre) pendant le maintien clic droit et une fois le tourbillon armé. */
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class ScytheSweepChargeHud {
    private static final int BAR_W = 120;
    private static final int BAR_H = 6;
    /** Plus haut que la hotbar + barre d’expérience (GUI réduit / échelle). */
    private static final int OFFSET_BOTTOM = 80;
    private static final int LABEL_ABOVE_BAR = 14;

    private ScytheSweepChargeHud() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null || mc.isPaused()) {
            return;
        }
        if (!ScytheSweepClientHandler.shouldDisplayChargeHud(mc)) {
            return;
        }

        float ratio = ScytheSweepClientHandler.getWhirlwindChargeRatio();
        boolean armed = ScytheSweepClientHandler.isSweepArmed();

        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();
        int bx = (guiW - BAR_W) / 2;
        int by = guiH - OFFSET_BOTTOM;

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;

        int border = 0xFF1E2430;
        int bg = 0x88101828;
        int fill = armed ? 0xFF6BE8B0 : 0xFFC5E0F0;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, border);
        g.fill(bx, by, bx + BAR_W, by + BAR_H, bg);
        int fillPx = Math.round(BAR_W * ratio);
        if (fillPx > 0) {
            g.fill(bx, by, bx + fillPx, by + BAR_H, fill);
        }

        Component title = armed
                ? Component.translatable("hud.bmcmod.scythe.whirlwind_ready")
                : Component.translatable("hud.bmcmod.scythe.whirlwind_charging");
        int tw = font.width(title);
        int titleColor = armed ? 0xFFB8FFD8 : 0xFFE8F4FF;
        drawOutlinedString(g, font, title, (guiW - tw) / 2, by - LABEL_ABOVE_BAR, titleColor);

        if (!armed) {
            int pct = Math.min(100, Math.round(ratio * 100.0F));
            Component sub = Component.translatable("hud.bmcmod.scythe.whirlwind_percent", pct);
            int sw = font.width(sub);
            int subY = by - LABEL_ABOVE_BAR + font.lineHeight + 2;
            g.drawString(font, sub, (guiW - sw) / 2, subY, 0xFFB0B8C8, true);
        }
    }

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
}
