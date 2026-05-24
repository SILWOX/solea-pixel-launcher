package com.soleapixel.bmcmod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Contexte thread-local pour l’End Anchor (mixins {@code RespawnAnchorBlock} / {@code ServerPlayer}) : ne doit pas
 * vivre sous {@code com.soleapixel.bmcmod.mixin} (restriction Mixin : pas de classe utilitaire dans le package mixin).
 */
public final class EndAnchorSpawnContext {

    public static final ThreadLocal<Level> CAN_SET_SPAWN_LEVEL = new ThreadLocal<>();
    public static final ThreadLocal<BlockState> USE_WITHOUT_STATE = new ThreadLocal<>();
    public static final ThreadLocal<BlockPos> USE_WITHOUT_POS = new ThreadLocal<>();
    /** Position du bloc de spawn en cours de validation (respawn après mort). */
    public static final ThreadLocal<BlockPos> RESPAWN_ANCHOR_CHECK_POS = new ThreadLocal<>();

    private EndAnchorSpawnContext() {}
}
