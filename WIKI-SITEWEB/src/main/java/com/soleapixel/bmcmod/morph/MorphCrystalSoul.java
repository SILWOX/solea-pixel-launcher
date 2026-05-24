package com.soleapixel.bmcmod.morph;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Une seule âme par cristal : NBT entité complète (champ {@code id} + {@link net.minecraft.world.entity.Entity#saveWithoutId}).
 */
public final class MorphCrystalSoul {
    public static final String SOUL_KEY = "BmcModMorphSoul";

    private MorphCrystalSoul() {
    }

    public static boolean hasSoul(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return root.contains(SOUL_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)
                && !root.getCompound(SOUL_KEY).isEmpty();
    }

    public static CompoundTag getSoul(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(SOUL_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return new CompoundTag();
        }
        return root.getCompound(SOUL_KEY);
    }

    public static void setSoul(ItemStack stack, CompoundTag soul) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(SOUL_KEY, soul.copy());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static void clearSoul(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.remove(SOUL_KEY);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    /** Nom affichable du type d’entité stocké (ex. Blink) d’après le champ {@code id} du NBT. */
    public static Component getStoredSoulDisplayName(CompoundTag soul) {
        if (!soul.contains("id", net.minecraft.nbt.Tag.TAG_STRING)) {
            return Component.translatable("item.bmcmod.morph_crystal.soul_unknown");
        }
        ResourceLocation id = ResourceLocation.tryParse(soul.getString("id"));
        if (id == null) {
            return Component.literal(soul.getString("id"));
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id)
                .map(t -> Component.translatable(t.getDescriptionId()))
                .orElseGet(() -> Component.literal(id.toString()));
    }
}
