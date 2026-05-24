package com.soleapixel.bmcmod.gameplay;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.registry.ModItems;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class SkeletonVillagerSkullArmorStandEvents {
    private SkeletonVillagerSkullArmorStandEvents() {
    }

    @SubscribeEvent
    public static void onArmorStandInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof ArmorStand stand)) {
            return;
        }
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());
        if (!held.is(ModItems.SKELETON_VILLAGER_SKULL_ITEM.get())) {
            return;
        }
        if (!stand.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            return;
        }
        if (player.level().isClientSide()) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        stand.setItemSlot(EquipmentSlot.HEAD, held.copyWithCount(1));
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}

