package com.soleapixel.bmcmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.EchoStaffItem;
import com.soleapixel.bmcmod.registry.ModItems;

/**
 * Zoom FOV pendant la charge (style arc). Les particules d’anneau / aura viennent du serveur
 * ({@link com.soleapixel.bmcmod.item.EchoStaffItem#spawnChargeParticles}).
 */
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class EchoStaffClientFX {
    private EchoStaffClientFX() {
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !player.isUsingItem()) {
            return;
        }
        ItemStack using = player.getUseItem();
        if (!using.is(ModItems.ECHO_STAFF.get())) {
            return;
        }
        int remaining = player.getUseItemRemainingTicks();
        int elapsed = EchoStaffItem.DRAW_DURATION - remaining;
        if (elapsed <= 0) {
            return;
        }
        float warm = Mth.clamp((float) elapsed / EchoStaffItem.CHARGE_TICKS, 0.0F, 1.0F);
        // FOV plus bas = léger zoom « tir à l’arc » (comme arc vanilla).
        double zoom = 1.0 - 0.11 * warm * warm;
        event.setFOV(event.getFOV() * zoom);
    }
}
