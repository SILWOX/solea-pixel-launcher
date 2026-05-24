package com.soleapixel.bmcmod.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/**
 * Bouclier (mêmes compétences qu’un {@link ShieldItem} vanilla) avec matériau de réparation
 * (diamant, Netherite, Enderite) et durabilité augmentée.
 */
public final class BmcShieldItem extends ShieldItem {
    /** Variante (texture, équilibrage) ; les réparations sont enregistrées côté {@code ModItems}. */
    public enum BmcKind {
        DIAMOND,
        NETHERITE,
        ENDERITE
    }

    public final BmcKind kind;
    private final Item repair;

    public BmcShieldItem(BmcKind kind, Item.Properties properties, Item repair) {
        super(properties);
        this.kind = kind;
        this.repair = repair;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack material) {
        return !material.isEmpty() && material.getItem() == repair;
    }
}
