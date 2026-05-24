package com.soleapixel.bmcmod.block.feeder;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Catégories de nourriture du feeder : blé, patate, carotte, graines (tag poules).
 * Bloc de foin : traité comme du blé ; à l’insertion, 1 bloc = 9 unités dans la réserve (stock interne en blé).
 */
public final class FeederFoodRules {
    public static final int MAX_ITEMS = 6 * 64;

    private FeederFoodRules() {
    }

    public static boolean isAcceptedFood(ItemStack stack) {
        return classify(stack) != null;
    }

    /** {@code null} si non accepté. */
    public static FeederContentKind classify(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (item == Items.HAY_BLOCK) {
            return FeederContentKind.WHEAT;
        }
        if (item == Items.WHEAT) {
            return FeederContentKind.WHEAT;
        }
        if (item == Items.POTATO) {
            return FeederContentKind.POTATO;
        }
        if (item == Items.CARROT) {
            return FeederContentKind.CARROT;
        }
        if (stack.is(ItemTags.CHICKEN_FOOD)) {
            return FeederContentKind.SEEDS;
        }
        return null;
    }

    public static FeederContentKind classifyOrEmpty(ItemStack stack) {
        FeederContentKind k = classify(stack);
        return k != null ? k : FeederContentKind.EMPTY;
    }
}
