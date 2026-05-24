package com.soleapixel.bmcmod.morph;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

/**
 * Coût en durabilité par morph (cristal 200 max par défaut). Valeurs par défaut pour les types non listés : 5.
 */
public final class MorphDurabilityCosts {
    private static final int DEFAULT = 5;

    private MorphDurabilityCosts() {
    }

    public static int costForSoulNbt(CompoundTag soul) {
        if (!soul.contains("id", net.minecraft.nbt.Tag.TAG_STRING)) {
            return DEFAULT;
        }
        ResourceLocation id = ResourceLocation.tryParse(soul.getString("id"));
        if (id == null) {
            return DEFAULT;
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(MorphDurabilityCosts::costForType).orElse(DEFAULT);
    }

    public static int costForType(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) {
            return DEFAULT;
        }
        String p = id.getPath();
        return switch (p) {
            case "villager", "zombie_villager" -> 2;
            case "wither_skeleton" -> 8;
            case "warden" -> 25;
            case "ender_dragon", "wither" -> 80;
            case "iron_golem", "ravager" -> 12;
            case "ghast", "blaze" -> 10;
            case "slime", "magma_cube" -> 4;
            case "creeper" -> 6;
            case "enderman" -> 7;
            case "chicken", "cow", "pig", "sheep", "rabbit", "bat" -> 1;
            default -> DEFAULT;
        };
    }
}
