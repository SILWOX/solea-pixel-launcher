package com.soleapixel.bmcmod.worldgen;

import org.slf4j.Logger;

import com.soleapixel.bmcmod.BmcMod;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Biomes datapack du mod ; les {@link Holder} sont résolus côté serveur pour les mixins de bruit (End, Overworld)
 * et les checks de gameplay.
 */
public final class ModBiomes {
    private static final Logger LOGGER = BmcMod.LOGGER;

    public static final ResourceKey<Biome> HOLLOW_GARDEN = ResourceKey.create(Registries.BIOME, BmcMod.loc("hollow_garden"));
    /** Bosquet tropical Sunwood près des côtes chaudes (placement via mixin bruit multi-noise). */
    public static final ResourceKey<Biome> SOLEA_GROVE = ResourceKey.create(Registries.BIOME, BmcMod.loc("solea_grove"));

    private static volatile Holder<Biome> hollowGardenHolder;
    private static volatile Holder<Biome> soleaGroveHolder;

    private ModBiomes() {
    }

    public static void cacheBiomeHolders(MinecraftServer server) {
        if (server == null) {
            return;
        }
        try {
            var lookup = server.registryAccess().lookupOrThrow(Registries.BIOME);
            hollowGardenHolder = lookup.get(HOLLOW_GARDEN).orElse(null);
            soleaGroveHolder = lookup.get(SOLEA_GROVE).orElse(null);
        } catch (Exception e) {
            LOGGER.warn("bmcmod: impossible de mettre en cache les biomes du mod (Soleá / Hollow Garden) — substitution désactivée jusqu’au prochain chargement.", e);
            hollowGardenHolder = null;
            soleaGroveHolder = null;
        }
    }

    public static void clearBiomeHolders() {
        hollowGardenHolder = null;
        soleaGroveHolder = null;
    }

    /**
     * Utilisé par le mixin End. Résolution paresseuse si le cache n’a pas encore tourné (évite que le biome ne
     * s’applique pas avant {@code ServerStartedEvent}).
     */
    public static Holder<Biome> hollowGardenHolder() {
        if (hollowGardenHolder != null) {
            return hollowGardenHolder;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            cacheBiomeHolders(server);
        }
        return hollowGardenHolder;
    }

    public static Holder<Biome> soleaGroveHolder() {
        if (soleaGroveHolder != null) {
            return soleaGroveHolder;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            cacheBiomeHolders(server);
        }
        return soleaGroveHolder;
    }
}
