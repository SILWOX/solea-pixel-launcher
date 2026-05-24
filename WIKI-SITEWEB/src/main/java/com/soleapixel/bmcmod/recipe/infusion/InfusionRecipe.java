package com.soleapixel.bmcmod.recipe.infusion;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Recette shapeless d'infusion : multiset des 4 emplacements (somme des stacks par type), ordre libre.
 * Les quantités peuvent dépasser la recette (surplus dans une même case) ; aucun type d’objet hors recette.
 */
public final class InfusionRecipe {
    public final int soulCost;
    public final ItemStack result;
    public final Map<Item, Integer> required;

    public InfusionRecipe(int soulCost, ItemStack result, Map<Item, Integer> required) {
        this.soulCost = soulCost;
        this.result = result.copy();
        this.required = Map.copyOf(new HashMap<>(required));
    }

    public static Map<Item, Integer> multisetFromInputs(
            @Nullable ItemStack a, @Nullable ItemStack b, @Nullable ItemStack c, @Nullable ItemStack d) {
        Map<Item, Integer> m = new HashMap<>();
        if (a != null) {
            accumulate(m, a);
        }
        if (b != null) {
            accumulate(m, b);
        }
        if (c != null) {
            accumulate(m, c);
        }
        if (d != null) {
            accumulate(m, d);
        }
        return m;
    }

    private static void accumulate(Map<Item, Integer> m, ItemStack s) {
        if (s.isEmpty()) {
            return;
        }
        Item it = s.getItem();
        m.merge(it, s.getCount(), Integer::sum);
    }

    public boolean matchesGrid(Map<Item, Integer> grid) {
        for (var e : this.required.entrySet()) {
            if (grid.getOrDefault(e.getKey(), 0) < e.getValue()) {
                return false;
            }
        }
        for (var e : grid.entrySet()) {
            if (e.getValue() <= 0) {
                continue;
            }
            if (!this.required.containsKey(e.getKey())) {
                return false;
            }
        }
        return true;
    }
}
