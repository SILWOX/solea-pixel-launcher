package com.soleapixel.bmcmod.registry;

import java.util.function.Supplier;

import com.soleapixel.bmcmod.BmcMod;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import com.soleapixel.bmcmod.item.QuiverContents;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Composants de données d’items (enchantements cachés du livre inconnu).
 */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents REGISTRY =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, BmcMod.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemEnchantments>> LATENT_STORED_ENCHANTMENTS =
            REGISTRY.registerComponentType(
                    "latent_stored_enchantments",
                    b -> b.persistent(ItemEnchantments.CODEC).networkSynchronized(ItemEnchantments.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<QuiverContents>> QUIVER_CONTENTS =
            REGISTRY.registerComponentType(
                    "quiver_contents",
                    b -> b.persistent(QuiverContents.CODEC).networkSynchronized(QuiverContents.STREAM_CODEC));

    public static Supplier<DataComponentType<ItemEnchantments>> latentStoredEnchantments() {
        return LATENT_STORED_ENCHANTMENTS;
    }

    private ModDataComponents() {
    }
}
