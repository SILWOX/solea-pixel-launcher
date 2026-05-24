package com.soleapixel.bmcmod.client;

import com.soleapixel.bmcmod.item.CrystalItem;
import com.soleapixel.bmcmod.menu.InfusionTableMenu;
import com.soleapixel.bmcmod.recipe.infusion.InfusionRecipe;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Logique client (comme le livre vanilla) : assez d’objets dans la grille + inventaire, âmes dans
 * le cristal, place pour le résultat dans le slot de sortie.
 */
@OnlyIn(Dist.CLIENT)
public final class InfusionRecipeBookHelper {
    private InfusionRecipeBookHelper() {
    }

    /** Compte les items du joueur (slots menu ≥ 6) + ce déjà posés dans la grille 1–4. */
    public static @NotNull Map<Item, Integer> availableIngredientPool(@NotNull InfusionTableMenu menu) {
        Map<Item, Integer> m = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            accumulate(m, menu.getSlot(i).getItem());
        }
        for (int i = 6; i < menu.slots.size(); i++) {
            accumulate(m, menu.getSlot(i).getItem());
        }
        return m;
    }

    private static void accumulate(Map<Item, Integer> m, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        Item it = stack.getItem();
        m.merge(it, stack.getCount(), Integer::sum);
    }

    public static boolean hasEnoughSouls(@NotNull InfusionTableMenu menu, @NotNull InfusionRecipe recipe) {
        ItemStack crystal = menu.getSlot(0).getItem();
        if (crystal.isEmpty()) {
            return false;
        }
        return CrystalItem.getTotalSoulCount(crystal) >= recipe.soulCost;
    }

    public static boolean hasEnoughIngredients(@NotNull InfusionTableMenu menu, @NotNull InfusionRecipe recipe) {
        Map<Item, Integer> pool = new HashMap<>(availableIngredientPool(menu));
        for (var e : recipe.required.entrySet()) {
            int have = pool.getOrDefault(e.getKey(), 0);
            if (have < e.getValue()) {
                return false;
            }
        }
        return true;
    }

    /** Même idée que {@code canOutputAccept} côté BE : le slot sortie peut prendre le résultat. */
    public static boolean outputCanAcceptResult(@NotNull InfusionTableMenu menu, @NotNull InfusionRecipe recipe) {
        Slot out = menu.getSlot(5);
        ItemStack cur = out.getItem();
        if (cur.isEmpty()) {
            return true;
        }
        ItemStack want = recipe.result;
        return ItemStack.isSameItemSameComponents(cur, want)
                && cur.getCount() + want.getCount() <= cur.getMaxStackSize();
    }

    public static boolean isFullyCraftable(@NotNull InfusionTableMenu menu, @NotNull InfusionRecipe recipe) {
        return hasEnoughIngredients(menu, recipe)
                && hasEnoughSouls(menu, recipe)
                && outputCanAcceptResult(menu, recipe);
    }

    /**
     * Pour l’affichage shapeless en 4 cases (ordre stable), indique si la case {@code displayIndex}
     * manque encore d’objets une fois les cases précédentes “consommées” dans le pool.
     */
    public static boolean displaySlotMissing(
            @NotNull InfusionTableMenu menu,
            @NotNull InfusionRecipe recipe,
            int displayIndex,
            @NotNull ItemStack[] displayFour) {
        Map<Item, Integer> pool = new HashMap<>(availableIngredientPool(menu));
        for (int j = 0; j < displayIndex; j++) {
            ItemStack st = displayFour[j];
            if (!st.isEmpty()) {
                pool.merge(st.getItem(), -st.getCount(), Integer::sum);
            }
        }
        ItemStack here = displayFour[displayIndex];
        if (here.isEmpty()) {
            return false;
        }
        return pool.getOrDefault(here.getItem(), 0) < here.getCount();
    }
}
