package com.soleapixel.bmcmod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record PondConfiguration(boolean experience) implements FeatureConfiguration {
    public static final Codec<PondConfiguration> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.BOOL.fieldOf("experience").forGetter(PondConfiguration::experience))
                    .apply(instance, PondConfiguration::new));
}
