package com.soleapixel.bmcmod.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class UndeadTotemItem extends Item {
    public static final int MAX_DURABILITY = 35;
    public static final int REVIVE_DAMAGE_COST = 10;
    public static final int CLONE_DAMAGE_COST = 3;

    public UndeadTotemItem(Properties properties) {
        super(properties.durability(MAX_DURABILITY).stacksTo(1));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }
}
