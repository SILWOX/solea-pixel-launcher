package com.soleapixel.bmcmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.ScytheItem;
import com.soleapixel.bmcmod.registry.ModEnchantmentKeys;

/**
 * Clic gauche armé + créature → tourbillon ; clic gauche pendant charge incomplète → reset (voir handler).
 */
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class ScytheSweepInputHandler {
    private ScytheSweepInputHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        if (!(mc.player.getMainHandItem().getItem() instanceof ScytheItem)) {
            return;
        }
        Options opt = mc.options;
        if (event.getKeyMapping() != opt.keyAttack) {
            return;
        }

        if (ScytheSweepClientHandler.shouldSuppressAttackInteraction()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }

        if (ScytheSweepClientHandler.isSweepArmed()) {
            var stack = mc.player.getMainHandItem();
            var reg = mc.player.level().registryAccess();
            boolean whirlwind = ModEnchantmentKeys.enchantmentLevel(stack, reg, ModEnchantmentKeys.WHIRLWIND) > 0;
            if (whirlwind) {
                ScytheSweepClientHandler.fireSweepAttack(-1);
                event.setCanceled(true);
                event.setSwingHand(false);
            } else {
                var target = ScytheSweepClientHandler.findLivingUnderCrosshair(mc);
                if (target != null) {
                    ScytheSweepClientHandler.fireSweepAttack(target.getId());
                    event.setCanceled(true);
                    event.setSwingHand(false);
                }
            }
            return;
        }

        ScytheSweepClientHandler.onPrematureAttackWhileCharging();
    }
}
