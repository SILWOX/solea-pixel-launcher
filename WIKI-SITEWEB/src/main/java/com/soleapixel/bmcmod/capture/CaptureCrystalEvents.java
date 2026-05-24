package com.soleapixel.bmcmod.capture;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.CaptureCrystalItem;
import com.soleapixel.bmcmod.item.CrackedCrystalItem;
@EventBusSubscriber(modid = BmcMod.MODID)
public final class CaptureCrystalEvents {
    private CaptureCrystalEvents() {
    }

    @SubscribeEvent
    public static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (left.getItem() instanceof CaptureCrystalItem
                || right.getItem() instanceof CaptureCrystalItem
                || left.getItem() instanceof CrackedCrystalItem
                || right.getItem() instanceof CrackedCrystalItem) {
            event.setCanceled(true);
        }
    }
}
