package com.soleapixel.bmcmod.mixin;

import java.util.HashSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.TheEndBiomeSource;

import com.soleapixel.bmcmod.worldgen.ModBiomes;

/**
 * Étend les ensembles « possible biomes » pour {@code TheEndBiomeSource} (Hollow Garden) et
 * {@code MultiNoiseBiomeSource} (Soleá Grove) afin que {@code /locate biome} et la génération voient ces biomes.
 */
@Mixin(BiomeSource.class)
public abstract class BiomeSourceMixin {

    @Inject(method = "possibleBiomes", at = @At("RETURN"), cancellable = true)
    private void bmcmod$expandPossibleBiomes(CallbackInfoReturnable<Set<Holder<Biome>>> cir) {
        Set<Holder<Biome>> original = cir.getReturnValue();
        if (original == null) {
            return;
        }
        Set<Holder<Biome>> expanded = null;
        if ((Object) this instanceof TheEndBiomeSource) {
            Holder<Biome> hollow = ModBiomes.hollowGardenHolder();
            if (hollow != null && !original.contains(hollow)) {
                expanded = new HashSet<>(original);
                expanded.add(hollow);
            }
        }
        if ((Object) this instanceof MultiNoiseBiomeSource) {
            Holder<Biome> solea = ModBiomes.soleaGroveHolder();
            if (solea != null && !original.contains(solea)) {
                if (expanded == null) {
                    expanded = new HashSet<>(original);
                }
                expanded.add(solea);
            }
        }
        if (expanded != null) {
            cir.setReturnValue(Set.copyOf(expanded));
        }
    }
}
