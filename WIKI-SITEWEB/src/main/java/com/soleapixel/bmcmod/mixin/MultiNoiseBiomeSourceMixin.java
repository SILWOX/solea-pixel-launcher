package com.soleapixel.bmcmod.mixin;

import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;

import com.soleapixel.bmcmod.worldgen.ModBiomes;
import com.soleapixel.bmcmod.worldgen.SoleaGrovePlacement;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Soleá Grove : ajout au flux {@link MultiNoiseBiomeSource#collectPossibleBiomes()} puis substitution dans
 * {@code getNoiseBiome} — <strong>les deux</strong> surcharges (coordonnées + sampler <em>et</em> TargetPoint),
 * car la génération des chunks utilise souvent uniquement {@code getNoiseBiome(Climate.TargetPoint)}.
 */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin {

    @Inject(method = "collectPossibleBiomes", at = @At("HEAD"))
    private void bmcmod$cacheSoleaBeforeCollectPossibleBiomes(CallbackInfoReturnable<Stream<Holder<Biome>>> cir) {
        cacheSoleaHolder();
    }

    @Inject(method = "collectPossibleBiomes", at = @At("RETURN"), cancellable = true)
    private void bmcmod$addSoleaGroveToCollectPossibleBiomes(CallbackInfoReturnable<Stream<Holder<Biome>>> cir) {
        Holder<Biome> solea = ModBiomes.soleaGroveHolder();
        if (solea == null) {
            return;
        }
        Stream<Holder<Biome>> base = cir.getReturnValue();
        if (base == null) {
            cir.setReturnValue(Stream.of(solea));
            return;
        }
        cir.setReturnValue(Stream.concat(base, Stream.of(solea)));
    }

    @Inject(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;", at = @At("HEAD"))
    private void bmcmod$cacheSoleaHolderBeforeNoiseInt(int x, int y, int z, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
        cacheSoleaHolder();
    }

    @Inject(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;", at = @At("RETURN"), cancellable = true)
    private void bmcmod$soleaGroveInt(int x, int y, int z, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
        applySoleaIfEligible(cir, sampler.sample(x, y, z));
    }

    @Inject(method = "getNoiseBiome(Lnet/minecraft/world/level/biome/Climate$TargetPoint;)Lnet/minecraft/core/Holder;", at = @At("HEAD"))
    private void bmcmod$cacheSoleaHolderBeforeNoiseTp(Climate.TargetPoint point, CallbackInfoReturnable<Holder<Biome>> cir) {
        cacheSoleaHolder();
    }

    @Inject(method = "getNoiseBiome(Lnet/minecraft/world/level/biome/Climate$TargetPoint;)Lnet/minecraft/core/Holder;", at = @At("RETURN"), cancellable = true)
    private void bmcmod$soleaGroveTargetPoint(Climate.TargetPoint point, CallbackInfoReturnable<Holder<Biome>> cir) {
        applySoleaIfEligible(cir, point);
    }

    private static void cacheSoleaHolder() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ModBiomes.cacheBiomeHolders(server);
        }
    }

    private static void applySoleaIfEligible(CallbackInfoReturnable<Holder<Biome>> cir, Climate.TargetPoint point) {
        Holder<Biome> vanilla = cir.getReturnValue();
        if (vanilla == null) {
            return;
        }
        Holder<Biome> solea = ModBiomes.soleaGroveHolder();
        if (solea == null || vanilla.is(solea)) {
            return;
        }
        if (!SoleaGrovePlacement.shouldReplaceWithSoleaGrove(vanilla, point)) {
            return;
        }
        cir.setReturnValue(solea);
    }
}
