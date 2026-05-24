package com.soleapixel.bmcmod.soul;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Probabilité de capturer une âme (cristal en main secondaire) : cibles paisibles de ferme
 * ≈ 30 %, mobs dangereux / nether (ex. wither squelette) souvent 45–55 %.
 */
public final class SoulCaptureTuning {
    private SoulCaptureTuning() {
    }

    public static float getCaptureChance(LivingEntity victim) {
        return getCaptureChance(victim.getType());
    }

    public static float getCaptureChance(EntityType<?> type) {
        @Nullable ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) {
            return 0.40F;
        }
        if (!"minecraft".equals(id.getNamespace())) {
            return 0.40F;
        }
        return switch (id.getPath()) {
            case "chicken", "cow", "pig", "sheep" -> 0.30F;
            case "goat", "rabbit" -> 0.31F;
            case "mooshroom" -> 0.32F;
            case "horse", "mule", "donkey" -> 0.33F;
            case "cat", "ocelot" -> 0.32F;
            case "wolf" -> 0.34F;
            case "fox" -> 0.33F;
            case "parrot" -> 0.31F;
            case "bee", "frog" -> 0.32F;
            case "wither_skeleton" -> 0.55F;
            case "skeleton" -> 0.44F;
            case "stray" -> 0.46F;
            case "zombie", "zombie_villager", "husk" -> 0.42F;
            case "drowned" -> 0.43F;
            case "zombified_piglin" -> 0.45F;
            case "creeper" -> 0.40F;
            case "enderman" -> 0.48F;
            case "blaze" -> 0.48F;
            case "ghast" -> 0.47F;
            case "hoglin" -> 0.46F;
            case "piglin" -> 0.47F;
            case "piglin_brute" -> 0.50F;
            case "magma_cube" -> 0.46F;
            case "slime" -> 0.35F;
            case "cave_spider" -> 0.41F;
            case "spider" -> 0.40F;
            case "vindicator" -> 0.47F;
            case "pillager" -> 0.44F;
            case "evoker" -> 0.49F;
            case "ravager" -> 0.50F;
            case "witch" -> 0.46F;
            case "phantom" -> 0.45F;
            case "guardian", "elder_guardian" -> 0.47F;
            case "warden" -> 0.58F;
            case "wither" -> 0.58F;
            case "ender_dragon" -> 0.60F;
            case "breeze" -> 0.50F;
            default -> 0.38F;
        };
    }
}
