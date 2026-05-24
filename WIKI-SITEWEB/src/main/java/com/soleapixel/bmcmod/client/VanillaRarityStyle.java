package com.soleapixel.bmcmod.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * Remplace l'affichage des raretés vanilla par la palette BmcMod (sans italique).
 */
public final class VanillaRarityStyle {
    private VanillaRarityStyle() {
    }

    /**
     * Remplace le rendu vanilla des raretés (italique, couleurs) par la palette bmcmod.
     * {@link Rarity#COMMON} est la valeur par défaut pour la quasi-totalité des objets (nom blanc) : on ne force pas le jaune,
     * réservé aux raretés explicites du mod (bâtons de test, etc.).
     */
    public static Style titleStyle(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> Style.EMPTY.withItalic(false);
            case UNCOMMON -> Style.EMPTY.withItalic(false);
            case RARE -> Style.EMPTY
                    .withItalic(false)
                    .withColor(TextColor.fromRgb(0x3399FF));
            case EPIC -> Style.EMPTY
                    .withItalic(false)
                    .withColor(TextColor.fromRgb(0xAA44FF));
        };
    }

    public static Component retitle(ItemStack stack) {
        Style st = titleStyle(stack.getRarity());
        if (!stack.has(DataComponents.CUSTOM_NAME)) {
            return Component.translatable(stack.getDescriptionId()).withStyle(st);
        }
        return Component.literal(stack.getHoverName().getString()).withStyle(st);
    }
}
