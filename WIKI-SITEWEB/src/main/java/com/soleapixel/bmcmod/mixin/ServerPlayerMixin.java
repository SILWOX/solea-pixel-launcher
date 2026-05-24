package com.soleapixel.bmcmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import com.soleapixel.bmcmod.util.EndAnchorSpawnContext;

import java.util.Optional;

/**
 * Fournit la position du bloc testé à {@link RespawnAnchorBlockMixin} pendant
 * {@link ServerPlayer#findRespawnAndUseSpawnBlock} (sinon {@code canSetSpawn} ne sait pas si c’est une End Anchor).
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Unique
    private static final String FIND_RESPAWN_AND_USE_SPAWN_BLOCK =
            "findRespawnAndUseSpawnBlock(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;FZZ)Ljava/util/Optional;";

    @Inject(method = FIND_RESPAWN_AND_USE_SPAWN_BLOCK, at = @At("HEAD"))
    private static void bmcmod$captureRespawnAnchorPos(
            ServerLevel level,
            BlockPos pos,
            float yaw,
            boolean forced,
            boolean consumeCharge,
            CallbackInfoReturnable<Optional<?>> cir) {
        EndAnchorSpawnContext.RESPAWN_ANCHOR_CHECK_POS.set(pos);
    }

    @Inject(method = FIND_RESPAWN_AND_USE_SPAWN_BLOCK, at = @At("RETURN"))
    private static void bmcmod$clearRespawnAnchorPos(
            ServerLevel level,
            BlockPos pos,
            float yaw,
            boolean forced,
            boolean consumeCharge,
            CallbackInfoReturnable<Optional<?>> cir) {
        EndAnchorSpawnContext.RESPAWN_ANCHOR_CHECK_POS.remove();
    }
}
