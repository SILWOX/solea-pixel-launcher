package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import com.soleapixel.bmcmod.BmcMod;

@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class PrismHeartRenderer {
    private static final ResourceLocation CONTAINER = ResourceLocation.withDefaultNamespace("hud/heart/container");
    private static final ResourceLocation PRISM_FULL = BmcMod.loc("textures/gui/hud/prism_full.png");
    private static final ResourceLocation PRISM_HALF = BmcMod.loc("textures/gui/hud/prism_half_blinking.png");
    private static final RandomSource R = RandomSource.create();

    private PrismHeartRenderer() {
    }

    @SubscribeEvent
    public static void onPlayerHealthPost(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) {
            return;
        }
        if (!mc.gameMode.canHurtPlayer()) {
            return;
        }
        if (PrismClientState.getMax() <= 0.0F || PrismClientState.getCurrent() <= 0.0F) {
            return;
        }
        double rowDelta = leftHeightRowDeltaForPrism(mc.player, PrismClientState.getMax());
        if (rowDelta != 0.0) {
            mc.gui.leftHeight += (int) Math.round(rowDelta);
        }
        GuiGraphics g = event.getGuiGraphics();
        float prismHp = Mth.clamp(PrismClientState.getCurrent(), 0.0F, PrismClientState.getMax());
        renderPrismHearts(g, mc, mc.player, prismHp);
    }

    /**
     * Même principe que l’augmentation de hauteur pour l’absorption, mais seulement pour les rangées supplémentaires
     * dues à la réserve Prisme (hors rendu des cœurs, qui utilise {@link #fBar} et l’espace de rangée ajusté).
     */
    private static double leftHeightRowDeltaForPrism(Player player, float maxPrism) {
        float f = fBar(player);
        int k1o = (int) Mth.ceil((double) player.getAbsorptionAmount());
        int k1n = (int) Mth.ceil((double) (player.getAbsorptionAmount() + (double) maxPrism));
        int l1o = (int) Mth.ceil((double) ((f + (float) k1o) / 2.0F / 10.0F));
        int l1n = (int) Mth.ceil((double) ((f + (float) k1n) / 2.0F / 10.0F));
        int i2o = Math.max(10 - (l1o - 2), 3);
        int i2n = Math.max(10 - (l1n - 2), 3);
        return (double) ((l1n - 1) * i2n - (l1o - 1) * i2o);
    }

    private static float fBar(Player player) {
        int i = (int) Mth.ceil((double) player.getHealth());
        return Math.max(
                (float) player.getAttributeValue(Attributes.MAX_HEALTH),
                (float) i);
    }

    private static void renderPrismHearts(GuiGraphics g, Minecraft mc, Player player, float prismHp) {
        float f = fBar(player);
        int i = (int) Mth.ceil((double) f / 2.0);
        int k1Abs = (int) Mth.ceil((double) player.getAbsorptionAmount());
        int jVan = (int) Mth.ceil((double) k1Abs / 2.0);
        R.setSeed((long) (mc.player.tickCount * 312871));
        int w = g.guiWidth();
        int h = g.guiHeight();
        int x0 = w / 2 - 91;
        // Comme renderHealthLevel : marge à gauche avant l’icône santé.
        int j1 = h - 39;
        int k1Layout = (int) Mth.ceil((double) (player.getAbsorptionAmount() + (double) PrismClientState.getMax()));
        int l1p = (int) Mth.ceil((double) ((f + (float) k1Layout) / 2.0F / 10.0F));
        int i2p = Math.max(10 - (l1p - 2), 3);
        int yBase = j1;
        if ((int) Mth.ceil((double) (player.getHealth() + player.getAbsorptionAmount() + prismHp)) <= 4) {
            yBase = j1 + (R.nextBoolean() ? 0 : 1);
        }
        int jP = (int) Mth.ceil((double) ((double) PrismClientState.getMax() / 2.0));
        for (int l = i + jVan + jP - 1; l >= i + jVan; l--) {
            int row = l / 10;
            int col = l % 10;
            int kx = x0 + col * 8;
            int ky = yBase - row * i2p;
            g.blitSprite(CONTAINER, kx, ky, 9, 9);
            int fromStart = l - (i + jVan);
            int halves = (int) Mth.floor((double) (prismHp * 2.0F));
            int before = 2 * fromStart;
            int inCell = Mth.clamp(halves - before, 0, 2);
            if (inCell == 0) {
                continue;
            }
            if (inCell >= 2) {
                g.blit(PRISM_FULL, kx, ky, 0, 0, 9, 9, 9, 9);
            } else {
                g.blit(PRISM_HALF, kx, ky, 0, 0, 9, 9, 9, 9);
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
