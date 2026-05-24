package com.soleapixel.bmcmod.menu;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.soleapixel.bmcmod.item.BackpackItem;
import com.soleapixel.bmcmod.registry.ModItems;

/**
 * Règles d’insertion : aucun autre sac ni shulker box dans un sac ;
 * les sacs ne rentrent pas dans les shulker boxes via {@link BackpackItem#canFitInsideContainerItems}.
 */
public final class BackpackRules {
    /** Tag vanilla : toutes les boîtes de Shulker (toutes couleurs). */
    private static final TagKey<Item> VANILLA_SHULKER_BOXES = TagKey.create(
            Registries.ITEM,
            ResourceLocation.withDefaultNamespace("shulker_boxes"));

    private BackpackRules() {
    }

    public static boolean isBackpack(ItemStack stack) {
        // Tag datapack + type concret : certains chemins (glisser-déposer, sync) ne passent pas par le tag seul.
        return stack.getItem() instanceof BackpackItem || stack.is(ModItems.BACKPACK_TAG);
    }

    public static boolean isShulkerBox(ItemStack stack) {
        return stack.is(VANILLA_SHULKER_BOXES);
    }

    public static boolean mayPlaceInBackpack(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        if (isBackpack(stack)) {
            return false;
        }
        return !isShulkerBox(stack);
    }
}
