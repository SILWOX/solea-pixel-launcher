package com.soleapixel.bmcmod.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.util.Mth;

/**
 * Carquois : stockage de flèches comme le sac (clic-droit), sans écran d’inventaire dédié.
 */
public class QuiverItem extends Item {
    public QuiverItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !QuiverHelper.get(stack).equals(QuiverContents.EMPTY);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        QuiverContents c = QuiverHelper.get(stack);
        int total = c.totalCount();
        int cap = QuiverContents.SLOT_COUNT * QuiverContents.MAX_PER_TYPE;
        return Math.round(13.0F - (float) total * 13.0F / (float) cap);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(0.08F, 0.85F, 0.95F);
    }

    @Override
    public boolean canFitInsideContainerItems(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        QuiverContents qc = QuiverHelper.normalize(QuiverHelper.get(stack));
        if (!qc.hasAnyArrow()) {
            tooltip.add(Component.translatable("item.bmcmod.quiver.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        int sel = qc.selectedIndex();
        for (int i = 0; i < QuiverContents.SLOT_COUNT; i++) {
            ItemStack ch = qc.getChannel(i);
            if (ch.isEmpty()) {
                continue;
            }
            tooltip.add(
                    Component.empty()
                            .append(Component.translatable("item.bmcmod.quiver.slot_line", ch.getHoverName(), ch.getCount()))
                            .withStyle(i == sel ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack quiverStack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }
        ItemStack one = QuiverHelper.peekOneArrow(quiverStack);
        if (one.isEmpty()) {
            return false;
        }
        if (!slot.mayPlace(one)) {
            return false;
        }
        ItemStack existing = slot.getItem();
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, one)) {
            return false;
        }
        if (!existing.isEmpty() && existing.getCount() >= existing.getMaxStackSize()) {
            return false;
        }
        QuiverHelper.extractOne(quiverStack, false);
        if (existing.isEmpty()) {
            slot.setByPlayer(one);
        } else {
            existing.grow(1);
            slot.setByPlayer(existing);
        }
        playRemoveOne(player);
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(
            ItemStack quiverStack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (other.isEmpty() || !other.is(ItemTags.ARROWS)) {
            return false;
        }
        int want = action == ClickAction.PRIMARY ? other.getCount() : 1;
        int put = QuiverHelper.insert(quiverStack, other, want, false);
        if (put <= 0) {
            return false;
        }
        other.shrink(put);
        access.set(other);
        playInsert(player);
        return true;
    }

    private static void playInsert(Player player) {
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playRemoveOne(Player player) {
        player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
    }
}
