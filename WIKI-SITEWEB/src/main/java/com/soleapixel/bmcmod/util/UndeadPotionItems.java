package com.soleapixel.bmcmod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;

import com.soleapixel.bmcmod.registry.ModItems;

/** Bouteilles de potion « Undead Invasion » par palier (1–6) : source unique pour loot / patrouilles / marchand. */
public final class UndeadPotionItems {
    private UndeadPotionItems() {
    }

    public static Item bottleForTier(int tier) {
        return switch (Mth.clamp(tier, 1, 6)) {
            case 1 -> ModItems.UNDEAD_BOTTLE_1.get();
            case 2 -> ModItems.UNDEAD_BOTTLE_2.get();
            case 3 -> ModItems.UNDEAD_BOTTLE_3.get();
            case 4 -> ModItems.UNDEAD_BOTTLE_4.get();
            case 5 -> ModItems.UNDEAD_BOTTLE_5.get();
            default -> ModItems.UNDEAD_BOTTLE_6.get();
        };
    }
}
