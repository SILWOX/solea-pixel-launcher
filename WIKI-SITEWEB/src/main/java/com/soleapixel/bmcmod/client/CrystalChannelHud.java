package com.soleapixel.bmcmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.capture.CaptureCrystalSoul;
import com.soleapixel.bmcmod.item.CaptureCrystalItem;
import com.soleapixel.bmcmod.item.MorphCrystalItem;
import com.soleapixel.bmcmod.registry.ModItems;

/**
 * Compte à rebours canal morph / capture et cooldown après pose : même emplacement que la barre d’action
 * vanilla (texte centré au-dessus de la hotbar, comme le nom d’item ou un message serveur).
 */
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class CrystalChannelHud {
    private static boolean lastTickOwnedOverlay;

    private CrystalChannelHud() {
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) {
            if (lastTickOwnedOverlay) {
                mc.gui.setOverlayMessage(CommonComponents.EMPTY, false);
            }
            lastTickOwnedOverlay = false;
            return;
        }

        float partialTick = 0f;
        Component message = null;

        if (player.isUsingItem()) {
            ItemStack use = player.getUseItem();
            if (use.getItem() instanceof MorphCrystalItem) {
                int rem = player.getUseItemRemainingTicks();
                float sec = Math.max(0.0F, (rem - partialTick) / 20.0F);
                message = Component.translatable("hud.bmcmod.morph_crystal.channel", String.format("%.1f", sec));
            } else if (use.getItem() instanceof CaptureCrystalItem && !CaptureCrystalSoul.hasCaptured(use)) {
                int rem = player.getUseItemRemainingTicks();
                float sec = Math.max(0.0F, (rem - partialTick) / 20.0F);
                message = Component.translatable("hud.bmcmod.capture_crystal.channel", String.format("%.1f", sec));
            }
        }

        if (message == null && player.getCooldowns().isOnCooldown(ModItems.CAPTURE_CRYSTAL.get())) {
            float pct = player.getCooldowns().getCooldownPercent(ModItems.CAPTURE_CRYSTAL.get(), partialTick);
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();
            boolean crystalWithSoulInHand =
                    (main.getItem() instanceof CaptureCrystalItem && CaptureCrystalSoul.hasCaptured(main))
                            || (off.getItem() instanceof CaptureCrystalItem && CaptureCrystalSoul.hasCaptured(off));
            int durationTicks = crystalWithSoulInHand
                    ? CaptureCrystalItem.POST_CAPTURE_PLACE_DELAY_TICKS
                    : CaptureCrystalItem.RELEASE_AFTER_PLACE_COOLDOWN_TICKS;
            float sec = durationTicks * pct / 20.0F;
            String langKey = crystalWithSoulInHand
                    ? "hud.bmcmod.capture_crystal.post_capture_cooldown"
                    : "hud.bmcmod.capture_crystal.release_cooldown";
            message = Component.translatable(langKey, String.format("%.1f", Math.max(0.0F, sec)));
        }

        if (message != null) {
            mc.gui.setOverlayMessage(message, false);
            lastTickOwnedOverlay = true;
        } else if (lastTickOwnedOverlay) {
            mc.gui.setOverlayMessage(CommonComponents.EMPTY, false);
            lastTickOwnedOverlay = false;
        }
    }
}
