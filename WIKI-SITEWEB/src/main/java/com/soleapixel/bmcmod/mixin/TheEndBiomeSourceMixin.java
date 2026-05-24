package com.soleapixel.bmcmod.mixin;

import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;

import com.soleapixel.bmcmod.worldgen.ModBiomes;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Remplace une partie des îles externes (highlands, midlands, barrens, petites îles) par {@link ModBiomes#HOLLOW_GARDEN}.
 * <p>
 * L’élargissement de {@link net.minecraft.world.level.biome.BiomeSource#possibleBiomes()} pour l’End est fait dans
 * {@link BiomeSourceMixin} (évite d’appeler {@link TheEndBiomeSource#create} pendant le bootstrap : erreur
 * « Unreferenced key: bmcmod:hollow_garden »).
 * <p>
 * {@link TheEndBiomeSource#collectPossibleBiomes()} : vanilla n’y met que les 5 biomes de l’End ; on concatène
 * encore le jardin si besoin.
 */
@Mixin(TheEndBiomeSource.class)
public abstract class TheEndBiomeSourceMixin {

    /**
     * {@link net.minecraft.world.level.biome.BiomeSource} mémorise {@code possibleBiomes} au premier appel : il faut
     * remplir le holder avant ce tout premier {@code collectPossibleBiomes()}.
     */
    @Inject(method = "collectPossibleBiomes", at = @At("HEAD"))
    private void bmcmod$cacheHollowGardenHolderBeforePossibleBiomes(CallbackInfoReturnable<Stream<Holder<net.minecraft.world.level.biome.Biome>>> cir) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ModBiomes.cacheBiomeHolders(server);
        }
    }

    @Inject(method = "collectPossibleBiomes", at = @At("RETURN"), cancellable = true)
    private void bmcmod$addHollowGardenToPossibleBiomes(CallbackInfoReturnable<Stream<Holder<net.minecraft.world.level.biome.Biome>>> cir) {
        Holder<net.minecraft.world.level.biome.Biome> hollow = ModBiomes.hollowGardenHolder();
        if (hollow == null) {
            return;
        }
        Stream<Holder<net.minecraft.world.level.biome.Biome>> base = cir.getReturnValue();
        if (base == null) {
            cir.setReturnValue(Stream.of(hollow));
            return;
        }
        cir.setReturnValue(Stream.concat(base, Stream.of(hollow)));
    }

    @Inject(method = "getNoiseBiome", at = @At("RETURN"), cancellable = true)
    private void bmcmod$hollowGarden(int x, int y, int z, Climate.Sampler sampler, CallbackInfoReturnable<Holder<net.minecraft.world.level.biome.Biome>> cir) {
        Holder<net.minecraft.world.level.biome.Biome> vanilla = cir.getReturnValue();
        if (vanilla == null) {
            return;
        }
        Holder<net.minecraft.world.level.biome.Biome> hollow = ModBiomes.hollowGardenHolder();
        if (hollow == null) {
            return;
        }
        // Grille >>6 sur coords quart (~256 blocs XZ) : tiles plus larges et plus lisibles.
        // On évite d'inclure Y dans le seed pour empêcher des transitions "bords d'îles" trop agressives.
        int macroX = x >> 6;
        int macroZ = z >> 6;
        long mix = Mth.getSeed(macroX, 0, macroZ);
        if (vanilla.is(Biomes.END_HIGHLANDS)) {
            if (Mth.positiveModulo((int) ((mix ^ 0xBADC0FFEEL) >>> 32), 10) < 2) {
                cir.setReturnValue(hollow);
            }
            return;
        }
        if (vanilla.is(Biomes.END_MIDLANDS)) {
            if (Mth.positiveModulo((int) ((mix ^ 0x4D1D4D5DL) >>> 32), 10) < 2) {
                cir.setReturnValue(hollow);
            }
            return;
        }
        // Ne remplace pas barrens/small islands : réduit la sensation "biome uniquement sur les bords".
    }
}
