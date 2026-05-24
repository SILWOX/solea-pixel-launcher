package com.soleapixel.bmcmod.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;

import com.soleapixel.bmcmod.rarity.BmcModRarity;
import com.soleapixel.bmcmod.rarity.RarityStickItem;

/**
 * Coffre de l’End portable : même inventaire persistant que le bloc coffre de l’End.
 */
public final class PocketEnderChestItem extends RarityStickItem {
    public PocketEnderChestItem(Properties properties) {
        super(properties, BmcModRarity.EXOTIC);
    }

    @Override
    public boolean canFitInsideContainerItems(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        sp.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> ChestMenu.threeRows(syncId, inv, p.getEnderChestInventory()),
                Component.translatable("container.enderchest")));
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDER_CHEST_OPEN, SoundSource.PLAYERS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.bmcmod.pocket_ender_chest.usage").withStyle(ChatFormatting.GRAY));
    }
}
