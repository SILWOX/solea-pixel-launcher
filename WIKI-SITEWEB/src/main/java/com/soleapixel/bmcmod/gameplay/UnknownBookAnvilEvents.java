package com.soleapixel.bmcmod.gameplay;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.UnknownBookItem;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class UnknownBookAnvilEvents {
    private UnknownBookAnvilEvents() {
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!event.getOutput().isEmpty()) {
            return;
        }
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (right.getItem() instanceof UnknownBookItem && left.isEnchantable() && left.getEnchantmentValue() > 0) {
            ItemEnchantments before = EnchantmentHelper.getEnchantmentsForCrafting(left);
            ItemStack out = UnknownBookLogic.applyRandomEnchant(event.getPlayer().getRandom(), event.getPlayer().level().registryAccess(), left);
            ItemEnchantments after = EnchantmentHelper.getEnchantmentsForCrafting(out);
            if (!after.equals(before)) {
                event.setOutput(out);
                event.setCost(6 + event.getPlayer().getRandom().nextInt(7));
                event.setMaterialCost(1);
            }
        }
    }
}
