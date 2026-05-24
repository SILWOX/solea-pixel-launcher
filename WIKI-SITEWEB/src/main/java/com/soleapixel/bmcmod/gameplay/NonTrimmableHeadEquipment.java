package com.soleapixel.bmcmod.gameplay;

import net.minecraft.world.item.ItemStack;

import com.soleapixel.bmcmod.registry.ModItems;

/**
 * Casques cosmétiques villageois + couronne : pas de smithing trim (voir {@link com.soleapixel.bmcmod.mixin.SmithingTrimRecipeMixin}).
 */
public final class NonTrimmableHeadEquipment {
    private NonTrimmableHeadEquipment() {
    }

    public static boolean blocksSmithingTrim(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.is(ModItems.VILLAGER_HAT_BUTCHER.get())
                || stack.is(ModItems.VILLAGER_HAT_LIBRARIAN.get())
                || stack.is(ModItems.VILLAGER_HAT_WEAPONSMITH.get())
                || stack.is(ModItems.VILLAGER_HAT_SHEPHERD.get())
                || stack.is(ModItems.VILLAGER_HAT_FISHERMAN.get())
                || stack.is(ModItems.VILLAGER_HAT_CARTOGRAPHER.get())
                || stack.is(ModItems.VILLAGER_HAT_ARMORER.get())
                || stack.is(ModItems.VILLAGER_HAT_FARMER.get())
                || stack.is(ModItems.VILLAGER_HAT_WITCH.get())
                || stack.is(ModItems.UNDEAD_CROWN.get());
    }
}
