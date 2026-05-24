package com.soleapixel.bmcmod.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import com.soleapixel.bmcmod.registry.ModItems;

import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Infobulle : indication Maj + aperçu textuel du contenu du sac (composant {@link DataComponents#CONTAINER}).
 * Enregistré depuis {@link BmcModClient} sur {@link net.neoforged.neoforge.common.NeoForge#EVENT_BUS}.
 */
public final class BackpackTooltipClient {
    private static final int PREVIEW_STACKS = 12;

    private BackpackTooltipClient() {
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(ModItems.BACKPACK_TAG)) {
            return;
        }
        List<Component> tip = event.getToolTip();
        ItemStack stack = event.getItemStack();
        if (!Screen.hasShiftDown()) {
            tip.add(Component.translatable("item.bmcmod.backpack.tooltip_shift").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        java.util.ArrayList<ItemStack> stacks = new java.util.ArrayList<>();
        contents.nonEmptyItems().forEach(stacks::add);
        if (stacks.isEmpty()) {
            tip.add(Component.translatable("item.bmcmod.backpack.preview_empty").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tip.add(Component.translatable("item.bmcmod.backpack.preview_header").withStyle(ChatFormatting.GRAY));
        int n = 0;
        for (ItemStack inside : stacks) {
            if (n >= PREVIEW_STACKS) {
                break;
            }
            tip.add(Component.literal("  • ")
                    .append(inside.getHoverName())
                    .append(Component.literal(" ×" + inside.getCount()))
                    .withStyle(ChatFormatting.GRAY));
            n++;
        }
        if (stacks.size() > PREVIEW_STACKS) {
            tip.add(Component.translatable("item.bmcmod.backpack.preview_more", stacks.size() - PREVIEW_STACKS).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
