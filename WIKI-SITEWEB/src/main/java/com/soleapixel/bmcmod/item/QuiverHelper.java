package com.soleapixel.bmcmod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import com.soleapixel.bmcmod.registry.ModDataComponents;

/**
 * Carquois « actif » : plus petit index d’inventaire parmi les slots 0–32 ; insertion / prélèvement / cycle.
 */
public final class QuiverHelper {
    /** Inclus : slots joueur 0–32 (hotbar + majeure partie de la grille principale). */
    public static final int SLOT_SCAN_MIN = 0;
    public static final int SLOT_SCAN_MAX = 32;

    private QuiverHelper() {
    }

    public static QuiverContents get(ItemStack quiverStack) {
        return quiverStack.getOrDefault(ModDataComponents.QUIVER_CONTENTS.get(), QuiverContents.EMPTY);
    }

    public static void set(ItemStack quiverStack, QuiverContents contents) {
        QuiverContents n = normalize(contents);
        if (n.equals(QuiverContents.EMPTY)) {
            quiverStack.remove(ModDataComponents.QUIVER_CONTENTS.get());
        } else {
            quiverStack.set(ModDataComponents.QUIVER_CONTENTS.get(), n);
        }
    }

    public static QuiverContents normalize(QuiverContents c) {
        if (!c.hasAnyArrow()) {
            return QuiverContents.EMPTY;
        }
        int sel = c.selectedIndex();
        if (c.getChannel(sel).isEmpty()) {
            int next = c.firstNonEmpty();
            return next >= 0 ? c.withSelectedIndex(next) : QuiverContents.EMPTY;
        }
        return c;
    }

    /** Plus petit indice dans [0,32] contenant cet item, ou -1. */
    public static int findPrimaryQuiverSlot(Player player, Item quiverItem) {
        Inventory inv = player.getInventory();
        int limit = Math.min(SLOT_SCAN_MAX, inv.getContainerSize() - 1);
        for (int i = SLOT_SCAN_MIN; i <= limit; i++) {
            if (inv.getItem(i).is(quiverItem)) {
                return i;
            }
        }
        return -1;
    }

    public static ItemStack peekOneArrow(ItemStack quiverStack) {
        QuiverContents c = normalize(get(quiverStack));
        if (!c.hasAnyArrow()) {
            return ItemStack.EMPTY;
        }
        int idx = c.selectedIndex();
        ItemStack ch = c.getChannel(idx);
        if (ch.isEmpty()) {
            idx = c.firstNonEmpty();
            ch = c.getChannel(idx);
        }
        if (ch.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ch.copyWithCount(1);
    }

    /** Insère jusqu’à {@code amount} sur ce stack ; retourne la quantité réellement ajoutée. */
    public static int insert(ItemStack quiverStack, ItemStack incoming, int amount, boolean simulate) {
        if (incoming.isEmpty() || amount <= 0 || !incoming.is(ItemTags.ARROWS)) {
            return 0;
        }
        QuiverContents cur = normalize(get(quiverStack));
        ItemStack slice = incoming.copyWithCount(Math.min(amount, incoming.getCount()));

        int existing = cur.indexOfType(slice);
        if (existing >= 0) {
            ItemStack ch = cur.getChannel(existing);
            int space = QuiverContents.MAX_PER_TYPE - ch.getCount();
            if (space <= 0) {
                return 0;
            }
            int add = Math.min(space, slice.getCount());
            if (!simulate) {
                ItemStack grown = ch.copy();
                grown.grow(add);
                set(quiverStack, cur.withChannel(existing, grown));
            }
            return add;
        }

        if (cur.nonEmptyChannelCount() >= QuiverContents.SLOT_COUNT) {
            return 0;
        }
        int emptySlot = -1;
        for (int i = 0; i < QuiverContents.SLOT_COUNT; i++) {
            if (cur.getChannel(i).isEmpty()) {
                emptySlot = i;
                break;
            }
        }
        if (emptySlot < 0) {
            return 0;
        }
        int add = Math.min(slice.getCount(), QuiverContents.MAX_PER_TYPE);
        if (!simulate) {
            ItemStack placed = slice.copyWithCount(add);
            QuiverContents next = normalize(cur.withChannel(emptySlot, placed));
            set(quiverStack, next);
        }
        return add;
    }

    /** Retire une flèche du canal sélectionné (ou premier non vide). Retourne la pile retirée (count 1). */
    public static ItemStack extractOne(ItemStack quiverStack, boolean simulate) {
        QuiverContents cur = normalize(get(quiverStack));
        int idx = cur.selectedIndex();
        if (cur.getChannel(idx).isEmpty()) {
            idx = cur.firstNonEmpty();
        }
        if (idx < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack ch = cur.getChannel(idx);
        if (ch.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack one = ch.copyWithCount(1);
        if (!simulate) {
            ItemStack left = ch.copy();
            left.shrink(1);
            QuiverContents next = cur.withChannel(idx, left.isEmpty() ? ItemStack.EMPTY : left);
            set(quiverStack, normalize(next));
        }
        return one;
    }

    /** Retire jusqu’à {@code max} flèches du canal correspondant à {@code matchingType} (même item). */
    public static int consumeMatching(ItemStack quiverStack, ItemStack matchingType, int max, boolean simulate) {
        if (matchingType.isEmpty() || max <= 0) {
            return 0;
        }
        QuiverContents cur = normalize(get(quiverStack));
        int idx = cur.indexOfType(matchingType);
        if (idx < 0) {
            return 0;
        }
        ItemStack ch = cur.getChannel(idx);
        int take = Math.min(max, ch.getCount());
        if (take <= 0) {
            return 0;
        }
        if (!simulate) {
            ItemStack left = ch.copy();
            left.shrink(take);
            set(quiverStack, normalize(cur.withChannel(idx, left.isEmpty() ? ItemStack.EMPTY : left)));
        }
        return take;
    }

    /** @return {@code true} si la sélection a changé (au moins 2 types présents). */
    public static boolean cycleSelection(ServerPlayer player, Item quiverItem) {
        int slot = findPrimaryQuiverSlot(player, quiverItem);
        if (slot < 0) {
            return false;
        }
        ItemStack qs = player.getInventory().getItem(slot);
        QuiverContents c = normalize(get(qs));
        java.util.List<Integer> indices = c.nonEmptyIndices();
        if (indices.size() <= 1) {
            return false;
        }
        int curIdx = c.selectedIndex();
        int pos = indices.indexOf(curIdx);
        if (pos < 0) {
            set(qs, c.withSelectedIndex(indices.get(0)));
            return true;
        }
        int nextPos = (pos + 1) % indices.size();
        set(qs, c.withSelectedIndex(indices.get(nextPos)));
        return true;
    }

    public static int consumptionAmountForWeapon(ServerPlayer player, ItemStack weapon) {
        var reg = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int multishot = reg.get(Enchantments.MULTISHOT)
                .map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, weapon))
                .orElse(0);
        return multishot > 0 ? 3 : 1;
    }
}
