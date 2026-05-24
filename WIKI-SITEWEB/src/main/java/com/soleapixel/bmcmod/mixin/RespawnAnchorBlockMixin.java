package com.soleapixel.bmcmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.BlockHitResult;

import com.soleapixel.bmcmod.block.EndAnchorBlock;
import com.soleapixel.bmcmod.util.EndAnchorSpawnContext;

import net.minecraft.world.entity.player.Player;

/**
 * Fait accepter l’{@link EndAnchorBlock} comme une ancre « dimension haute énergie » uniquement dans {@code the_end}.
 * <p>
 * {@link RespawnAnchorBlock#canSetSpawn(net.minecraft.world.level.Level)} est appelé depuis le clic
 * <em>et</em> depuis {@link net.minecraft.server.level.ServerPlayer#findRespawnAndUseSpawnBlock} : dans ce second cas,
 * seule la position est connue — voir {@link EndAnchorSpawnContext#RESPAWN_ANCHOR_CHECK_POS} (rempli par le mixin
 * {@code ServerPlayer}).
 */
@Mixin(RespawnAnchorBlock.class)
public abstract class RespawnAnchorBlockMixin {

    private static final String CAN_SET_SPAWN = "canSetSpawn(Lnet/minecraft/world/level/Level;)Z";

    @Inject(method = CAN_SET_SPAWN, at = @At("HEAD"))
    private static void bmcmod$canSetSpawnHead(Level level, CallbackInfoReturnable<Boolean> cir) {
        EndAnchorSpawnContext.CAN_SET_SPAWN_LEVEL.set(level);
    }

    @Inject(method = CAN_SET_SPAWN, at = @At("RETURN"))
    private static void bmcmod$canSetSpawnReturn(Level level, CallbackInfoReturnable<Boolean> cir) {
        EndAnchorSpawnContext.CAN_SET_SPAWN_LEVEL.remove();
    }

    @Inject(method = "useWithoutItem", at = @At("HEAD"))
    private void bmcmod$captureUseWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit,
            CallbackInfoReturnable<?> cir) {
        EndAnchorSpawnContext.USE_WITHOUT_STATE.set(state);
        EndAnchorSpawnContext.USE_WITHOUT_POS.set(pos);
    }

    @Inject(method = "useWithoutItem", at = @At("RETURN"))
    private void bmcmod$clearUseWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit,
            CallbackInfoReturnable<?> cir) {
        EndAnchorSpawnContext.USE_WITHOUT_STATE.remove();
        EndAnchorSpawnContext.USE_WITHOUT_POS.remove();
    }

    @Redirect(
            method = CAN_SET_SPAWN,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;respawnAnchorWorks()Z"))
    private static boolean bmcmod$endAnchorWorks(DimensionType dimensionType) {
        Level level = EndAnchorSpawnContext.CAN_SET_SPAWN_LEVEL.get();
        BlockState heldState = EndAnchorSpawnContext.USE_WITHOUT_STATE.get();
        BlockPos usePos = EndAnchorSpawnContext.USE_WITHOUT_POS.get();
        BlockPos respawnCheckPos = EndAnchorSpawnContext.RESPAWN_ANCHOR_CHECK_POS.get();

        if (heldState != null && level != null && heldState.getBlock() instanceof EndAnchorBlock) {
            return level.dimension() == Level.END;
        }
        if (level != null && usePos != null) {
            BlockState at = level.getBlockState(usePos);
            if (at.getBlock() instanceof EndAnchorBlock) {
                return level.dimension() == Level.END;
            }
        }
        if (level != null && respawnCheckPos != null) {
            BlockState at = level.getBlockState(respawnCheckPos);
            if (at.getBlock() instanceof EndAnchorBlock) {
                return level.dimension() == Level.END;
            }
        }
        return dimensionType.respawnAnchorWorks();
    }
}
