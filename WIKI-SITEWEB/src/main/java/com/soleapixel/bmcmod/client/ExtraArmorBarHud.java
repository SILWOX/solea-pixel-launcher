package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import com.soleapixel.bmcmod.BmcMod;

/**
 * Seconde rangée d’icônes d’armure pour les points au-delà de 20 (la barre vanilla n’en affiche que 10 × 2).
 */
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class ExtraArmorBarHud {
    private static final ResourceLocation ARMOR_EMPTY = ResourceLocation.withDefaultNamespace("hud/armor_empty");
    private static final ResourceLocation ARMOR_HALF = ResourceLocation.withDefaultNamespace("hud/armor_half");
    private static final ResourceLocation ARMOR_FULL = ResourceLocation.withDefaultNamespace("hud/armor_full");

    private ExtraArmorBarHud() {
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !mc.gameMode.canHurtPlayer()) {
            return;
        }
        Player player = mc.player;
        if (player == null) {
            return;
        }
        int totalArmor = player.getArmorValue();
        if (totalArmor <= 20) {
            return;
        }
        int overflow = Math.min(20, totalArmor - 20);

        var graphics = event.getGuiGraphics();
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        int x0 = screenW / 2 - 91;
        // Après Gui.renderArmorLevel, leftHeight inclut +10 si le joueur a au moins 1 point d'armure.
        int leftHeightAfter = mc.gui.leftHeight;
        int leftHeightBeforeArmor = leftHeightAfter - (totalArmor > 0 ? 10 : 0);
        int vanillaArmorRowTop = screenH - leftHeightBeforeArmor;
        int y = vanillaArmorRowTop - 10;

        RenderSystem.enableBlend();
        for (int slot = 0; slot < 10; slot++) {
            int x = x0 + slot * 8;
            int v = slot * 2 + 1;
            if (v < overflow) {
                graphics.blitSprite(ARMOR_FULL, x, y, 9, 9);
            } else if (v == overflow) {
                graphics.blitSprite(ARMOR_HALF, x, y, 9, 9);
            } else {
                graphics.blitSprite(ARMOR_EMPTY, x, y, 9, 9);
            }
        }
        RenderSystem.disableBlend();
    }
}
