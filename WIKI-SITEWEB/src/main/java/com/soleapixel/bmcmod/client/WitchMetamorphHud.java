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
import com.soleapixel.bmcmod.witch.WitchMetamorphPackets;

@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class WitchMetamorphHud {
    private static KeyMapping metamorphKey;

    private WitchMetamorphHud() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        metamorphKey = new KeyMapping(
                "key.bmcmod.witch_metamorph",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "key.categories.bmcmod");
        event.register(metamorphKey);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        if (metamorphKey != null && metamorphKey.consumeClick()) {
            PacketDistributor.sendToServer(new WitchMetamorphPackets.WitchTryTransformPayload());
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        int transform = WitchMetamorphClient.getHudTransformTicksLeft();
        int cooldown = WitchMetamorphClient.getHudCooldownTicksLeft();
        if (transform <= 0 && cooldown <= 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        var graphics = event.getGuiGraphics();
        int h = mc.getWindow().getGuiScaledHeight();
        int startX = 8;
        int baseY = h - 52;
        int y = baseY;
        if (transform > 0) {
            int sec = (transform + 19) / 20;
            int min = sec / 60;
            sec = sec % 60;
            Component line = Component.translatable("hud.bmcmod.witch_metamorph_time", min, sec);
            graphics.drawString(mc.font, line, startX, y, 0xFFEECC88, false);
            y += 11;
        }
        if (cooldown > 0 && transform <= 0) {
            float sec = cooldown / 20.0F;
            Component line = Component.translatable("hud.bmcmod.witch_metamorph_cooldown", String.format("%.1f", sec));
            graphics.drawString(mc.font, line, startX, y, 0xFFAAAAAA, false);
        }
    }
}
