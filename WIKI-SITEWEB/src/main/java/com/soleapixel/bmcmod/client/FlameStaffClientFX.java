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
import com.soleapixel.bmcmod.item.FlameStaffItem;
import com.soleapixel.bmcmod.registry.ModItems;

/**
 * Léger zoom FOV pendant le jet (même idée que l’Echo Staff, plus discret).
 */
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class FlameStaffClientFX {
    private FlameStaffClientFX() {
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !player.isUsingItem()) {
            return;
        }
        ItemStack using = player.getUseItem();
        if (!using.is(ModItems.FLAME_STAFF.get())) {
            return;
        }
        int remaining = player.getUseItemRemainingTicks();
        int elapsed = FlameStaffItem.USE_DURATION - remaining;
        if (elapsed <= 0) {
            return;
        }
        float warm = Mth.clamp((float) elapsed / 40.0F, 0.0F, 1.0F);
        double zoom = 1.0 - 0.05 * warm * warm;
        event.setFOV(event.getFOV() * zoom);
    }
}
