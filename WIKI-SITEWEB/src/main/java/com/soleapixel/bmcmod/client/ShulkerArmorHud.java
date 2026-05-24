package com.soleapixel.bmcmod.client;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import com.mojang.blaze3d.platform.InputConstants;
import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.network.ShulkerArmorPackets;

@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class ShulkerArmorHud {
    private static int syncedCharges = -1;
    private static int syncedTicksUntilCharge;

    private static KeyMapping shootKey;

    private ShulkerArmorHud() {
    }

    public static void setSyncedState(int charges, int ticksUntilNextCharge) {
        syncedCharges = charges;
        syncedTicksUntilCharge = ticksUntilNextCharge;
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        shootKey = new KeyMapping(
                "key.bmcmod.shulker_shoot",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "key.categories.bmcmod");
        event.register(shootKey);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        if (shootKey != null && shootKey.consumeClick()) {
            PacketDistributor.sendToServer(new ShulkerArmorPackets.ShulkerShootPayload());
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (syncedCharges < 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        var graphics = event.getGuiGraphics();
        int h = mc.getWindow().getGuiScaledHeight();
        int baseY = h - 28;
        int startX = 8;

        for (int i = 0; i < 3; i++) {
            int x = startX + i * 14;
            boolean filled = i < syncedCharges;
            int bg = filled ? 0xFF7B5FA8 : 0x55333344;
            int border = 0xFF2A1A33;
            graphics.fill(x - 1, baseY - 1, x + 9, baseY + 9, border);
            graphics.fill(x, baseY, x + 8, baseY + 8, bg);
        }

        if (syncedCharges < 3 && syncedTicksUntilCharge > 0) {
            float sec = syncedTicksUntilCharge / 20.0F;
            Component line = Component.translatable("hud.bmcmod.shulker_recharge", String.format("%.1f", sec));
            graphics.drawString(mc.font, line, startX, baseY - 11, 0xFFCCCCCC, false);
        }
    }
}
