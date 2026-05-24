package com.soleapixel.bmcmod.item;

import java.util.List;

import com.soleapixel.bmcmod.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class UnknownBookItem extends Item {
    public UnknownBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (!tooltipFlag.hasShiftDown()) {
            tooltipComponents.add(Component.translatable("item.bmcmod.unknown_book.tooltip_shift").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltipComponents.add(Component.translatable("item.bmcmod.unknown_book.hint").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public static boolean hasLatentEnchantments(ItemStack stack) {
        ItemEnchantments latent = stack.get(ModDataComponents.LATENT_STORED_ENCHANTMENTS.get());
        return latent != null && !latent.isEmpty();
    }
}
