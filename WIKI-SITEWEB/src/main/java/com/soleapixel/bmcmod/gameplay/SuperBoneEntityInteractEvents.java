package com.soleapixel.bmcmod.gameplay;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.SuperBoneItem;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Vanilla appelle {@code Mob#mobInteract} avant {@link net.minecraft.world.item.Item#interactLivingEntity} : avec un
 * loup, le super os ne passait jamais par l’item (assis / os normal). On intercepte comme les cristaux via
 * {@link PlayerInteractEvent.EntityInteract}, avant l’interaction entité vanilla.
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class SuperBoneEntityInteractEvents {
    private SuperBoneEntityInteractEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        Entity target = event.getTarget();
        InteractionResult r =
                SuperBoneItem.tryApplyToOwnedWolf(stack, player, target, event.getHand());
        if (r.consumesAction()) {
            event.setCanceled(true);
            event.setCancellationResult(r);
        }
    }
}
