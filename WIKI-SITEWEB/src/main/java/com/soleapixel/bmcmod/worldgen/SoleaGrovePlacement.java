package com.soleapixel.bmcmod.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

/**
 * Soleá Grove — savane, plateau savane, désert : taches larges (continentalness / depth), relief filtré (érosion +
 * weirdness), sans savane venteuse ni carver canyon dans le JSON du biome.
 */
public final class SoleaGrovePlacement {
    private SoleaGrovePlacement() {
    }

    public static boolean shouldReplaceWithSoleaGrove(Holder<Biome> vanilla, Climate.TargetPoint point) {
        if (!vanilla.is(Biomes.SAVANNA)
                && !vanilla.is(Biomes.SAVANNA_PLATEAU)
                && !vanilla.is(Biomes.DESERT)) {
            return false;
        }
        float temperature = Climate.unquantizeCoord(point.temperature());
        if (temperature < -0.02f) {
            return false;
        }
        if (!warmStableInland(point)) {
            return false;
        }
        if (!suitableLandSurface(point)) {
            return false;
        }
        if (!flatLikePlains(vanilla, point)) {
            return false;
        }
        return true;
    }

    /**
     * Bande continentale assez large pour de grosses taches ; le relief plat est géré séparément dans
     * {@link #flatLikePlains(Holder, Climate.TargetPoint)}.
     */
    private static boolean warmStableInland(Climate.TargetPoint point) {
        float continentalness = Climate.unquantizeCoord(point.continentalness());
        // Légèrement resserré : un peu moins de Soleá Grove qu’avant, sans casser les grosses taches.
        return continentalness >= -0.10f && continentalness <= 0.50f;
    }

    private static boolean suitableLandSurface(Climate.TargetPoint point) {
        float depth = Climate.unquantizeCoord(point.depth());
        return depth > -0.20f && depth < 0.60f;
    }

    /**
     * Haute érosion + weirdness resserré = relief doux. Savane : léger assouplissement vs {@code 0.26f} pour agrandir
     * les taches sans retomber dans le chaos ; désert : seuil un peu plus bas (terrain déjà peu accidenté).
     */
    private static boolean flatLikePlains(Holder<Biome> vanilla, Climate.TargetPoint point) {
        float erosion = Climate.unquantizeCoord(point.erosion());
        float weirdness = Climate.unquantizeCoord(point.weirdness());
        if (vanilla.is(Biomes.DESERT)) {
            return erosion >= 0.19f && weirdness > -0.46f && weirdness < 0.46f;
        }
        return erosion >= 0.25f && weirdness > -0.35f && weirdness < 0.35f;
    }
}
