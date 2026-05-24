package com.soleapixel.bmcmod.worldgen;

import com.soleapixel.bmcmod.BmcMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public final class ModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> HOLLOW_TREE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            BmcMod.loc("hollow_tree"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> SUNWOOD_TREE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            BmcMod.loc("sunwood_tree"));

    private ModConfiguredFeatures() {
    }
}

