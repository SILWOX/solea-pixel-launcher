package com.soleapixel.bmcmod.item;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Téléporte vers le point de réapparition du joueur (lit / ancre / etc.), sinon spawn partagé de l’Overworld.
 */
public final class BackTotemTeleport {
    private BackTotemTeleport() {
    }

    public static void teleportToSpawn(ServerPlayer player) {
        MinecraftServer server = player.server;
        BlockPos respawnBlockPos = player.getRespawnPosition();
        ResourceKey<Level> respawnDim = player.getRespawnDimension();

        if (respawnBlockPos != null && respawnDim != null) {
            ServerLevel destination = server.getLevel(respawnDim);
            if (destination != null) {
                Optional<ServerPlayer.RespawnPosAngle> safe = ServerPlayer.findRespawnAndUseSpawnBlock(
                        destination,
                        respawnBlockPos,
                        player.getRespawnAngle(),
                        false,
                        false);
                if (safe.isPresent()) {
                    ServerPlayer.RespawnPosAngle r = safe.get();
                    player.teleportTo(destination, r.position().x, r.position().y, r.position().z, r.yaw(), 0.0F);
                    player.resetFallDistance();
                    return;
                }
            }
        }

        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        BlockPos worldSpawn = overworld.getSharedSpawnPos();
        double x = worldSpawn.getX() + 0.5;
        double z = worldSpawn.getZ() + 0.5;
        int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldSpawn.getX(), worldSpawn.getZ());
        float yaw = overworld.getSharedSpawnAngle();
        player.teleportTo(overworld, x, y, z, yaw, 0.0F);
        player.resetFallDistance();
    }
}
