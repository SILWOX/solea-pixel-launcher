package com.soleapixel.bmcmod.client;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.network.UndeadTotemPackets;
import com.soleapixel.bmcmod.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class UndeadTotemClientHandler {
    private static final int HOLD_TICKS = 60; // 3 seconds
    /** Sous les barres HUD (hotbar, XP…) : éviter guiH − petit nombre pour ne pas masquer sous la hotbar. */
    private static final int PROGRESS_BAR_OFFSET_FROM_BOTTOM = 74;
    private static final int PROGRESS_LABEL_GAP_ABOVE_BAR = 14;
    private static int holdTicks;
    /** Maintien prolongé → invocation déjà envoyée pour cette pression. */
    private static boolean sentSummonForCurrentHold;
    private static boolean wasKeyDownLastTick;

    private UndeadTotemClientHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused()) {
            holdTicks = 0;
            sentSummonForCurrentHold = false;
            wasKeyDownLastTick = false;
            return;
        }
        boolean keyDown = BmcModKeyMappings.undeadTotemCloneKey != null && BmcModKeyMappings.undeadTotemCloneKey.isDown();
        boolean holdingTotem = player.getMainHandItem().is(ModItems.UNDEAD_TOTEM.get()) || player.getOffhandItem().is(ModItems.UNDEAD_TOTEM.get());
        if (mc.screen != null || !holdingTotem) {
            holdTicks = 0;
            sentSummonForCurrentHold = false;
            wasKeyDownLastTick = keyDown;
            return;
        }
        if (keyDown) {
            if (!wasKeyDownLastTick) {
                holdTicks = 0;
            }
            holdTicks++;
            if (!sentSummonForCurrentHold && holdTicks >= HOLD_TICKS) {
                var skin = player.getSkin();
                boolean slim = skin != null && skin.model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM;
                PacketDistributor.sendToServer(new UndeadTotemPackets.SummonClonesPayload(slim));
                sentSummonForCurrentHold = true;
            }
        } else {
            sentSummonForCurrentHold = false;
            holdTicks = 0;
        }
        wasKeyDownLastTick = keyDown;
    }

    /** Barre + texte un peu au-dessus de la zone hotbar/XP tant qu’on maintient l’invocation (touche configurée). */
    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        if (mc.screen != null || mc.isPaused()) {
            return;
        }
        LocalPlayer player = mc.player;
        boolean keyDown = BmcModKeyMappings.undeadTotemCloneKey != null && BmcModKeyMappings.undeadTotemCloneKey.isDown();
        boolean holdingTotem = player.getMainHandItem().is(ModItems.UNDEAD_TOTEM.get()) || player.getOffhandItem().is(ModItems.UNDEAD_TOTEM.get());
        if (!keyDown || !holdingTotem) {
            return;
        }

        float progress = sentSummonForCurrentHold ? 1.0F : Math.min(holdTicks, HOLD_TICKS) / (float) HOLD_TICKS;
        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();
        final int barW = 110;
        final int barH = 6;
        int bx = (guiW - barW) / 2;
        int by = guiH - PROGRESS_BAR_OFFSET_FROM_BOTTOM;
        var g = event.getGuiGraphics();

        int border = 0xFF1A2828;
        int bg = 0x66101218;
        int fill = sentSummonForCurrentHold ? 0xFF8FFFCF : 0xFFCFE8DD;
        g.fill(bx - 1, by - 1, bx + barW + 1, by + barH + 1, border);
        g.fill(bx, by, bx + barW, by + barH, bg);
        int fillPx = Math.round(barW * progress);
        if (fillPx > 0) {
            g.fill(bx, by, bx + fillPx, by + barH, fill);
        }

        Component line = sentSummonForCurrentHold
                ? Component.translatable("hud.bmcmod.undead_totem_summon_cast")
                : Component.translatable("hud.bmcmod.undead_totem_summon_hold",
                        String.format("%.1f", Math.max(0F, (HOLD_TICKS - Math.min(holdTicks, HOLD_TICKS)) / 20F)));

        int textW = mc.font.width(line);
        g.drawString(mc.font, line, (guiW - textW) / 2, by - PROGRESS_LABEL_GAP_ABOVE_BAR, 0xFFEAECEB, true);
    }
}
