package com.soleapixel.bmcmod.block.feeder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Accès hopper : haut et côtés = remplissage, bas = retrait (mêmes règles que manuellement).
 * <p>
 * Côté entrée uniquement : le slot est exposé vide à {@link #getStackInSlot} pour que transferts /
 * helpers qui refusent d’empiler des items différents (ex. foin entré vs réserve vue comme blé)
 * appellent quand même {@link #insertItem}. Le stock réel pour le tirage passe par extraction dessous.
 */
public final class FeederItemHandler implements IItemHandler {
    private final FeederBlockEntity feeder;
    private final boolean allowInsert;
    private final boolean allowExtract;
    /** Si faux pour les faces insertion : vue « slot vide », voir ci-dessus. */
    private final boolean exposeStoredInSlotView;

    public FeederItemHandler(
            FeederBlockEntity feeder, boolean allowInsert, boolean allowExtract, boolean exposeStoredInSlotView) {
        this.feeder = feeder;
        this.allowInsert = allowInsert;
        this.allowExtract = allowExtract;
        this.exposeStoredInSlotView = exposeStoredInSlotView;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        return this.exposeStoredInSlotView ? feeder.getStoredPrototype() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!allowInsert || slot != 0 || stack.isEmpty()) {
            return stack;
        }
        int moved = feeder.insertFrom(stack, stack.getCount(), simulate);
        if (moved <= 0) {
            return stack;
        }
        if (simulate) {
            ItemStack rest = stack.copy();
            rest.shrink(moved);
            return rest;
        }
        stack.shrink(moved);
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!allowExtract || slot != 0 || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int take = Math.min(amount, feeder.getStoredCount());
        if (take <= 0) {
            return ItemStack.EMPTY;
        }
        return feeder.extractItems(take, simulate);
    }

    /**
     * Capacité totale du feeder ({@link FeederFoodRules#MAX_ITEMS}), pas la taille d’une pile vanilla (64).
     * Sinon les hoppers considèrent le slot « plein » dès 64 alors que le stock interne peut aller jusqu’à 384.
     */
    @Override
    public int getSlotLimit(int slot) {
        return slot == 0 ? FeederFoodRules.MAX_ITEMS : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot != 0 || stack.isEmpty()) {
            return false;
        }
        if (!FeederFoodRules.isAcceptedFood(stack)) {
            return false;
        }
        if (feeder.isEmpty()) {
            return true;
        }
        if (stack.is(Items.HAY_BLOCK)) {
            return feeder.getRepresentativeStack().getItem() == Items.WHEAT;
        }
        return stack.getItem() == feeder.getRepresentativeStack().getItem();
    }
}
