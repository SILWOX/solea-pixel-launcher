package com.soleapixel.bmcmod.capture;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Une seule entité par cristal : NBT complète ({@code id} + {@link net.minecraft.world.entity.Entity#saveWithoutId}).
 */
public final class CaptureCrystalSoul {
    public static final String CAPTURE_KEY = "BmcModCaptureEntity";

    private CaptureCrystalSoul() {
    }

    /**
     * Une capture valide doit avoir un {@code id} d’entité (certains NBT « non vides » ne suffisent pas côté client).
     */
    public static boolean hasCaptured(ItemStack stack) {
        CompoundTag cap = getCaptured(stack);
        return cap.contains("id", net.minecraft.nbt.Tag.TAG_STRING) && !cap.getString("id").isEmpty();
    }

    public static CompoundTag getCaptured(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(CAPTURE_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return new CompoundTag();
        }
        return root.getCompound(CAPTURE_KEY);
    }

    public static void setCaptured(ItemStack stack, CompoundTag entityTag) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(CAPTURE_KEY, entityTag.copy());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static void clearCaptured(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.remove(CAPTURE_KEY);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
}
