package com.soleapixel.bmcmod.item;

import java.util.List;
import java.util.stream.IntStream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Jusqu’à 5 types de flèches (5 canaux), 192 max par type ; indice 0–4 du type actif pour le tir / le cycle.
 */
public record QuiverContents(
        ItemStack c0, ItemStack c1, ItemStack c2, ItemStack c3, ItemStack c4, int selectedIndex) {

    public static final int SLOT_COUNT = 5;
    public static final int MAX_PER_TYPE = 192;
    public static final QuiverContents EMPTY = new QuiverContents(
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, 0);

    public static final Codec<QuiverContents> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    ItemStack.OPTIONAL_CODEC.fieldOf("c0").forGetter(QuiverContents::c0),
                    ItemStack.OPTIONAL_CODEC.fieldOf("c1").forGetter(QuiverContents::c1),
                    ItemStack.OPTIONAL_CODEC.fieldOf("c2").forGetter(QuiverContents::c2),
                    ItemStack.OPTIONAL_CODEC.fieldOf("c3").forGetter(QuiverContents::c3),
                    ItemStack.OPTIONAL_CODEC.fieldOf("c4").forGetter(QuiverContents::c4),
                    Codec.intRange(0, 4).fieldOf("selected").forGetter(QuiverContents::selectedIndex))
            .apply(i, QuiverContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuiverContents> STREAM_CODEC = StreamCodec.of(
            (buf, v) -> {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, v.c0);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, v.c1);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, v.c2);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, v.c3);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, v.c4);
                buf.writeVarInt(v.selectedIndex);
            },
            buf -> new QuiverContents(
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    buf.readVarInt()));

    public QuiverContents {
        if (selectedIndex < 0 || selectedIndex > 4) {
            throw new IllegalArgumentException("selectedIndex out of range: " + selectedIndex);
        }
    }

    public static NonNullList<ItemStack> asList(QuiverContents c) {
        NonNullList<ItemStack> n = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        n.set(0, c.c0);
        n.set(1, c.c1);
        n.set(2, c.c2);
        n.set(3, c.c3);
        n.set(4, c.c4);
        return n;
    }

    public static QuiverContents fromList(NonNullList<ItemStack> list, int selected) {
        if (list.size() != SLOT_COUNT) {
            throw new IllegalArgumentException("expected 5 channels");
        }
        return new QuiverContents(
                list.get(0), list.get(1), list.get(2), list.get(3), list.get(4), selected);
    }

    public ItemStack getChannel(int i) {
        return switch (i) {
            case 0 -> c0;
            case 1 -> c1;
            case 2 -> c2;
            case 3 -> c3;
            case 4 -> c4;
            default -> ItemStack.EMPTY;
        };
    }

    public QuiverContents withChannel(int i, ItemStack stack) {
        return switch (i) {
            case 0 -> new QuiverContents(stack, c1, c2, c3, c4, selectedIndex);
            case 1 -> new QuiverContents(c0, stack, c2, c3, c4, selectedIndex);
            case 2 -> new QuiverContents(c0, c1, stack, c3, c4, selectedIndex);
            case 3 -> new QuiverContents(c0, c1, c2, stack, c4, selectedIndex);
            case 4 -> new QuiverContents(c0, c1, c2, c3, stack, selectedIndex);
            default -> this;
        };
    }

    public QuiverContents withSelectedIndex(int sel) {
        return new QuiverContents(c0, c1, c2, c3, c4, sel);
    }

    /** Prochain indice (0–4) ayant au moins une flèche. Si aucun : {@code -1}. */
    public int nextNonEmptyFrom(int startInclusive) {
        for (int step = 0; step < SLOT_COUNT; step++) {
            int idx = (startInclusive + step) % SLOT_COUNT;
            if (!getChannel(idx).isEmpty()) {
                return idx;
            }
        }
        return -1;
    }

    /** Premier canal non vide ; sinon -1. */
    public int firstNonEmpty() {
        return nextNonEmptyFrom(0);
    }

    public boolean hasAnyArrow() {
        return firstNonEmpty() >= 0;
    }

    public int totalCount() {
        int t = 0;
        for (int i = 0; i < SLOT_COUNT; i++) {
            t += getChannel(i).getCount();
        }
        return t;
    }

    /** Indice du canal pour une pile donnée (même item), ou -1. */
    public int indexOfType(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack ch = getChannel(i);
            if (!ch.isEmpty() && ItemStack.isSameItemSameComponents(ch, stack)) {
                return i;
            }
        }
        return -1;
    }

    /** Nombre de types (canaux) non vides. */
    public int nonEmptyChannelCount() {
        int n = 0;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!getChannel(i).isEmpty()) {
                n++;
            }
        }
        return n;
    }

    /** Indices des canaux non vides, dans l’ordre 0–4. */
    public List<Integer> nonEmptyIndices() {
        return IntStream.range(0, SLOT_COUNT).filter(i -> !getChannel(i).isEmpty()).boxed().toList();
    }
}
