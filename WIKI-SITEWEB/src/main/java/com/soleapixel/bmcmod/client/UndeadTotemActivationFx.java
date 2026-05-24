package com.soleapixel.bmcmod.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.soleapixel.bmcmod.registry.ModItems;

/**
 * Vanilla passes {@link Items#TOTEM_OF_UNDYING} to {@link net.minecraft.client.renderer.GameRenderer#displayItemActivation(ItemStack)};
 * swap in the activating player's undead totem stack so HUD/particles use {@code bmcmod:item/undead_totem} model.
 */
public final class UndeadTotemActivationFx {
    private UndeadTotemActivationFx() {
    }

    public static ItemStack swapActivationStackIfUndeadTotem(Player activatingPlayer, ItemStack vanillaTotemPlaceholder) {
        if (activatingPlayer == null || !vanillaTotemPlaceholder.is(Items.TOTEM_OF_UNDYING)) {
            return vanillaTotemPlaceholder;
        }
        ItemStack off = activatingPlayer.getOffhandItem();
        if (off.is(ModItems.UNDEAD_TOTEM.get())) {
            return off.copy();
        }
        ItemStack main = activatingPlayer.getMainHandItem();
        if (main.is(ModItems.UNDEAD_TOTEM.get())) {
            return main.copy();
        }
        return vanillaTotemPlaceholder;
    }
}
