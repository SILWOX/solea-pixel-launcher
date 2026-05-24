package com.soleapixel.bmcmod.morph;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Retire du NBT d’entité les champs transitoires (blessure, mort, chute, feu, effets au moment du kill, etc.)
 * pour que la métamorphose reflète l’identité (villageois, bébé, propriétaire, biome…) sans l’état du décès.
 */
public final class MorphSoulSanitizer {
    /** Champs racine typiques de l’état court terme / combat (save {@code LivingEntity#addAdditionalSaveData}). */
    private static final String[] TRANSIENT_ROOT_KEYS = {
            "Motion",
            "DeltaMovement",
            "Health",
            "AbsorptionAmount",
            "HurtTime",
            "HurtByTimestamp",
            "DeathTime",
            "FallDistance",
            "Fire",
            "Air",
            "PortalCooldown",
            "FallFlying",
            "TicksFrozen",
            "SleepTimer",
            "PowderSnowFrozenTime",
            "ActiveEffects",
            "Passengers",
            "ExplosionRadius",
            "Fuse",
            "Invulnerable",
            "NoGravity",
    };

    private MorphSoulSanitizer() {
    }

    public static CompoundTag sanitize(CompoundTag soul) {
        if (soul == null || soul.isEmpty()) {
            return soul == null ? new CompoundTag() : soul;
        }
        CompoundTag out = soul.copy();
        for (String k : TRANSIENT_ROOT_KEYS) {
            out.remove(k);
        }
        fixPose(out);
        return out;
    }

    private static void fixPose(CompoundTag tag) {
        if (!tag.contains("Pose", Tag.TAG_STRING)) {
            return;
        }
        String p = tag.getString("Pose");
        if ("DYING".equalsIgnoreCase(p) || "DECAYING".equalsIgnoreCase(p)) {
            tag.remove("Pose");
        }
    }
}
