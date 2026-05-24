package com.soleapixel.bmcmod.morph;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/**
 * Lecture de l’identité du template de métamorphose (âme NBT) pour règles de mouvement, vol, pose, etc.
 */
public final class MorphAppearanceIds {
    /**
     * Plafond par défaut de la vitesse au sol en morph (base d’attribut), pour éviter les valeurs de boss / mod.
     * Les mobs dont {@link #morphLandSpeedBaseCap} renvoie moins sont déjà bornés en dessous.
     */
    public static final double DEFAULT_MORPH_MOVEMENT_CAP = 0.52;

    /** Règles de respiration / dégâts hors eau pour métamorphoses aquatiques. */
    public enum AquaticKind {
        NONE,
        /** Poissons, calmars : air plein sous l’eau, sècheresse / air hors eau. */
        GILLS,
        /** Dauphin : confort sous l’eau, dégâts « dry out » sur la terre ferme. */
        DOLPHIN
    }

    private MorphAppearanceIds() {
    }

    public static ResourceLocation soulEntityId(CompoundTag soul) {
        if (soul == null || !soul.contains("id", Tag.TAG_STRING)) {
            return null;
        }
        return ResourceLocation.tryParse(soul.getString("id"));
    }

    public static String soulPath(CompoundTag soul) {
        ResourceLocation id = soulEntityId(soul);
        return id == null ? "" : id.getPath();
    }

    /** Mobs dont le sneak affichera une pose « assis » (chat, renard…). */
    public static boolean morphSupportsSitPose(ResourceLocation entityId) {
        if (entityId == null) {
            return false;
        }
        String p = entityId.getPath();
        return p.equals("cat")
                || p.equals("ocelot")
                || p.equals("fox")
                || p.equals("wolf")
                || p.equals("parrot");
    }

    public static boolean morphIsSlimeLike(ResourceLocation entityId) {
        if (entityId == null) {
            return false;
        }
        String p = entityId.getPath();
        return p.equals("slime") || p.equals("magma_cube");
    }

    /**
     * Vol style survie (abilities.mayfly) pour mobs aériens — pas le créatif/spectateur.
     */
    public static boolean morphGrantsSurvivalFlight(ResourceLocation entityId) {
        if (entityId == null) {
            return false;
        }
        String p = entityId.getPath();
        return p.equals("ghast")
                || p.equals("blaze")
                || p.equals("phantom")
                || p.equals("allay")
                || p.equals("vex")
                || p.equals("bat")
                || p.equals("breeze")
                || p.equals("bee");
    }

    public static AquaticKind aquaticKind(ResourceLocation entityId) {
        if (entityId == null) {
            return AquaticKind.NONE;
        }
        String p = entityId.getPath();
        if (p.equals("dolphin")) {
            return AquaticKind.DOLPHIN;
        }
        if (gilledAquaticPath(p)) {
            return AquaticKind.GILLS;
        }
        return AquaticKind.NONE;
    }

    /** Pose nage (hitbox / rendu) sous l’eau pour mobs aquatiques. */
    public static boolean morphUsesAquaticSwimPose(ResourceLocation entityId) {
        return aquaticKind(entityId) != AquaticKind.NONE;
    }

    private static boolean gilledAquaticPath(String p) {
        return p.equals("cod")
                || p.equals("salmon")
                || p.equals("tropical_fish")
                || p.equals("pufferfish")
                || p.equals("squid")
                || p.equals("glow_squid")
                || p.equals("tadpole");
    }

    /**
     * Plafond de la <strong>valeur de base</strong> de {@link net.minecraft.world.entity.ai.attributes.Attributes#MOVEMENT_SPEED}
     * en métamorphose. Certains mobs (villageois, golem…) ont une base élevée pour l’IA alors qu’ils avancent lentement
     * visuellement ; sans plafond, le joueur + sprint devient trop rapide.
     */
    public static double morphLandSpeedBaseCap(ResourceLocation entityId) {
        if (entityId == null) {
            return DEFAULT_MORPH_MOVEMENT_CAP;
        }
        String p = entityId.getPath();
        if (p.equals("villager") || p.equals("wandering_trader")) {
            return 0.115;
        }
        if (p.equals("iron_golem") || p.equals("snow_golem")) {
            return 0.125;
        }
        if (p.equals("warden")) {
            return 0.22;
        }
        return DEFAULT_MORPH_MOVEMENT_CAP;
    }
}
