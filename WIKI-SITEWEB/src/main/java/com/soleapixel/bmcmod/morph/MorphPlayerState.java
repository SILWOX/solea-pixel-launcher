package com.soleapixel.bmcmod.morph;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * État métamorphose (persistant sur le joueur : déco/reco serveur). Champ cooldown réservé / nettoyé à la connexion.
 */
public final class MorphPlayerState {
    public static final String TAG_ACTIVE = "BmcModMorphCrystalActive";
    public static final String TAG_SOUL = "BmcModMorphCrystalMorphSoul";
    /** {@link net.minecraft.world.level.Level#getGameTime()} au moment de la métamorphose (durée max). */
    public static final String TAG_MORPH_SINCE = "BmcModMorphCrystalMorphSince";
    public static final String TAG_COOLDOWN_UNTIL = "BmcModMorphCrystalCdUntil";

    private MorphPlayerState() {
    }

    public static boolean isMorphed(Player player) {
        return player.getPersistentData().getBoolean(TAG_ACTIVE);
    }

    /** Copie nettoyée (sans état blessure/mort) pour rendu, paquets et effets. */
    public static CompoundTag getMorphSoul(Player player) {
        return MorphSoulSanitizer.sanitize(player.getPersistentData().getCompound(TAG_SOUL).copy());
    }

    public static void setMorphed(Player player, CompoundTag soulCopy, long gameTimeWhenStarted) {
        player.getPersistentData().putBoolean(TAG_ACTIVE, true);
        player.getPersistentData().put(TAG_SOUL, MorphSoulSanitizer.sanitize(soulCopy.copy()));
        player.getPersistentData().putLong(TAG_MORPH_SINCE, gameTimeWhenStarted);
    }

    /** Réécrit l’âme persistée si une ancienne sauvegarde contenait encore des champs transitoires. */
    public static void migrateStoredMorphSoulIfNeeded(Player player) {
        if (!isMorphed(player)) {
            return;
        }
        CompoundTag raw = player.getPersistentData().getCompound(TAG_SOUL);
        CompoundTag clean = MorphSoulSanitizer.sanitize(raw.copy());
        player.getPersistentData().put(TAG_SOUL, clean);
    }

    /** Horodatage début morph ; 0 si absent (sauvegarde ancienne). */
    public static long getMorphSinceGameTime(Player player) {
        return player.getPersistentData().getLong(TAG_MORPH_SINCE);
    }

    public static void clearMorph(Player player) {
        player.getPersistentData().putBoolean(TAG_ACTIVE, false);
        player.getPersistentData().remove(TAG_SOUL);
        player.getPersistentData().remove(TAG_MORPH_SINCE);
    }

    public static long getCooldownUntil(Player player) {
        return player.getPersistentData().getLong(TAG_COOLDOWN_UNTIL);
    }

    public static void setCooldownUntil(Player player, long gameTimeUntil) {
        player.getPersistentData().putLong(TAG_COOLDOWN_UNTIL, gameTimeUntil);
    }

    public static void clearCooldown(Player player) {
        player.getPersistentData().remove(TAG_COOLDOWN_UNTIL);
    }
}
