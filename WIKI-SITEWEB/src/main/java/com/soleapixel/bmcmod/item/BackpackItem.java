package com.soleapixel.bmcmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

import com.soleapixel.bmcmod.menu.BackpackChestMenu;
import com.soleapixel.bmcmod.menu.BackpackContainer;
import com.soleapixel.bmcmod.menu.BackpackStorage;
import com.soleapixel.bmcmod.registry.ModMenus;

public final class BackpackItem extends Item {
    private final int rows;

    public BackpackItem(Properties properties, int rows) {
        super(properties);
        this.rows = rows;
    }

    public int getRows() {
        return this.rows;
    }

    /**
     * Titre du menu coffre : style par défaut (gris comme « Inventory »), sans jaune de rareté sur le nom d’objet.
     */
    public static Component menuTitle(ItemStack stack) {
        Component base;
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            base = stack.get(DataComponents.CUSTOM_NAME).copy();
        } else if (stack.has(DataComponents.ITEM_NAME)) {
            base = stack.get(DataComponents.ITEM_NAME).copy();
        } else {
            base = Component.translatable(stack.getDescriptionId());
        }
        return base.plainCopy();
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.getRarity() != Rarity.COMMON) {
            return super.getName(stack);
        }
        return Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.YELLOW);
    }

    @Override
    public boolean canFitInsideContainerItems(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        final int rowsToSend = this.rows;
        final InteractionHand openHand = hand;
        serverPlayer.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return BackpackItem.menuTitle(stack);
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                BackpackContainer container = new BackpackContainer(rowsToSend * 9);
                BackpackStorage.loadInto(p.getItemInHand(openHand), container);
                return new BackpackChestMenu(ModMenus.BACKPACK.get(), syncId, inv, container, rowsToSend, openHand);
            }
        }, buf -> {
            buf.writeVarInt(rowsToSend);
            buf.writeBoolean(openHand == InteractionHand.OFF_HAND);
        });
        return InteractionResultHolder.success(stack);
    }
}
