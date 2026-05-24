package com.soleapixel.bmcmod.morph;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.CaptureCrystalItem;
import com.soleapixel.bmcmod.item.MorphCrystalItem;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Vanilla appelle {@code Entity#interact} avant {@code Item#interactLivingEntity} : clic sur loup / cheval avec
 * cristal lançait s’asseoir / monter au lieu du canal de capture ou de morph. On court-circuite via
 * {@link PlayerInteractEvent.EntityInteract} (déjà invoqué avant {@code entity.interact}).
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class CrystalEntityInteractEvents {
    private CrystalEntityInteractEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void prioritizeCrystalOverEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        InteractionResult capture = CaptureCrystalItem.tryBeginCaptureChannelOnEntity(
                event.getEntity(), event.getHand(), stack, event.getTarget());
        if (capture.consumesAction()) {
            event.setCanceled(true);
            event.setCancellationResult(capture);
            return;
        }
        InteractionResult morph = MorphCrystalItem.tryBeginMorphChannelOnEntity(
                event.getEntity(), event.getHand(), stack, event.getTarget());
        if (morph.consumesAction()) {
            event.setCanceled(true);
            event.setCancellationResult(morph);
        }
    }
}
