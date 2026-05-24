package com.soleapixel.bmcmod.witch;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import com.soleapixel.bmcmod.registry.ModItems;

/**
 * État serveur : transformation en sorcière, cooldown, synchronisation réseau.
 */
public final class WitchMetamorphServer {
    public static final int TRANSFORM_TICKS = 5 * 60 * 20;
    public static final int COOLDOWN_TICKS = 5 * 60 * 20;

    /** Fin de transformation : tick monde absolu (inclusif jusqu’à ce tick). */
    private static final Map<UUID, Long> TRANSFORM_UNTIL_GAME_TIME = new ConcurrentHashMap<>();
    /** Après une transformation (ou annulation), prochain tick où une nouvelle transformation est permise. */
    private static final Map<UUID, Long> COOLDOWN_UNTIL_GAME_TIME = new ConcurrentHashMap<>();

    private WitchMetamorphServer() {
    }

    public static boolean isTransformed(Player player) {
        long gt = player.level().getGameTime();
        Long until = TRANSFORM_UNTIL_GAME_TIME.get(player.getUUID());
        return until != null && gt < until;
    }

    public static void handleTryTransform(ServerPlayer player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel)) {
            return;
        }
        long gt = player.level().getGameTime();
        if (isTransformed(player)) {
            return;
        }
        long cooldownUntil = COOLDOWN_UNTIL_GAME_TIME.getOrDefault(player.getUUID(), 0L);
        if (gt < cooldownUntil) {
            syncHud(player);
            return;
        }
        var hat = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!hat.is(ModItems.VILLAGER_HAT_WITCH.get())) {
            syncHud(player);
            return;
        }
        long end = gt + TRANSFORM_TICKS;
        TRANSFORM_UNTIL_GAME_TIME.put(player.getUUID(), end);
        broadcastDisguise(player, true);
        syncHud(player);
    }

    public static void tickPlayer(ServerPlayer player) {
        long gt = player.level().getGameTime();
        UUID id = player.getUUID();
        Long until = TRANSFORM_UNTIL_GAME_TIME.get(id);
        if (until != null) {
            boolean hatOn = player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.VILLAGER_HAT_WITCH.get());
            if (!hatOn) {
                onWitchHatUnequipped(player);
            } else if (gt >= until) {
                finishNaturalTransform(player);
            }
        }
        if (gt % 20 == 0) {
            syncHud(player);
        }
    }

    /** Fin normale (timer 5 min écoulé) : cooldown avant une nouvelle métamorphose. */
    public static void finishNaturalTransform(ServerPlayer player) {
        UUID id = player.getUUID();
        if (TRANSFORM_UNTIL_GAME_TIME.remove(id) == null) {
            return;
        }
        long gt = player.level().getGameTime();
        COOLDOWN_UNTIL_GAME_TIME.put(id, gt + COOLDOWN_TICKS);
        broadcastDisguise(player, false);
        syncHud(player);
    }

    /**
     * Chapeau de sorcière retiré : annule la métamorphose en cours et supprime tout cooldown affiché
     * (pas de pénalité 5 min tant que le chapeau n’est pas porté).
     */
    public static void onWitchHatUnequipped(ServerPlayer player) {
        UUID id = player.getUUID();
        TRANSFORM_UNTIL_GAME_TIME.remove(id);
        broadcastDisguise(player, false);
        COOLDOWN_UNTIL_GAME_TIME.remove(id);
        syncHud(player);
    }

    public static void broadcastDisguise(ServerPlayer player, boolean disguised) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new WitchMetamorphPackets.WitchDisguisePayload(player.getUUID(), disguised));
    }

    public static void syncHud(ServerPlayer player) {
        long gt = player.level().getGameTime();
        UUID id = player.getUUID();
        int transformLeft = 0;
        Long until = TRANSFORM_UNTIL_GAME_TIME.get(id);
        if (until != null && gt < until) {
            transformLeft = (int) Math.min(Integer.MAX_VALUE, until - gt);
        }
        int cooldownLeft = 0;
        long cdUntil = COOLDOWN_UNTIL_GAME_TIME.getOrDefault(id, 0L);
        if (gt < cdUntil && !isTransformed(player)) {
            cooldownLeft = (int) Math.min(Integer.MAX_VALUE, cdUntil - gt);
        }
        PacketDistributor.sendToPlayer(player, new WitchMetamorphPackets.WitchHudPayload(transformLeft, cooldownLeft));
    }

    public static void clearPlayerState(UUID id) {
        TRANSFORM_UNTIL_GAME_TIME.remove(id);
        COOLDOWN_UNTIL_GAME_TIME.remove(id);
    }

    /** Déconnexion : retirer le déguisement côté autres clients sans appliquer de cooldown supplémentaire. */
    public static void onPlayerLogout(ServerPlayer player) {
        UUID id = player.getUUID();
        if (TRANSFORM_UNTIL_GAME_TIME.remove(id) != null) {
            broadcastDisguise(player, false);
        }
        COOLDOWN_UNTIL_GAME_TIME.remove(id);
    }
}
