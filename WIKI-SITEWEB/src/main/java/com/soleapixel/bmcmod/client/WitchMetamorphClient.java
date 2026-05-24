package com.soleapixel.bmcmod.client;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;

import com.soleapixel.bmcmod.util.WalkAnimationStateCopy;

/**
 * Cache client : joueurs rendus comme sorcières + HUD (ticks restants).
 */
public final class WitchMetamorphClient {
    private static final Set<UUID> DISGUISED = ConcurrentHashMap.newKeySet();
    private static int hudTransformTicksLeft;
    private static int hudCooldownTicksLeft;
    private static Witch proxyWitch;

    private WitchMetamorphClient() {
    }

    public static void setDisguised(UUID playerId, boolean disguised) {
        if (disguised) {
            DISGUISED.add(playerId);
        } else {
            DISGUISED.remove(playerId);
        }
    }

    public static boolean isDisguised(UUID playerId) {
        return DISGUISED.contains(playerId);
    }

    public static void setHudState(int transformTicksLeft, int cooldownTicksLeft) {
        hudTransformTicksLeft = transformTicksLeft;
        hudCooldownTicksLeft = cooldownTicksLeft;
    }

    public static int getHudTransformTicksLeft() {
        return hudTransformTicksLeft;
    }

    public static int getHudCooldownTicksLeft() {
        return hudCooldownTicksLeft;
    }

    /**
     * Entité proxy réutilisée pour le rendu (non ajoutée au monde).
     */
    public static Witch getOrCreateProxyWitch(ClientLevel level, Player player) {
        if (proxyWitch == null || proxyWitch.level() != level) {
            proxyWitch = net.minecraft.world.entity.EntityType.WITCH.create(level);
        }
        if (proxyWitch == null) {
            return null;
        }
        Witch w = proxyWitch;
        w.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        w.setYHeadRot(player.getYHeadRot());
        w.yHeadRotO = player.yHeadRotO;
        w.yBodyRot = player.yBodyRot;
        w.yBodyRotO = player.yBodyRotO;
        w.setXRot(player.getXRot());
        w.xRotO = player.xRotO;
        w.setYRot(player.getYRot());
        w.yRotO = player.yRotO;
        w.walkDist = player.walkDist;
        w.walkDistO = player.walkDistO;
        WalkAnimationStateCopy.copyFromTo(player.walkAnimation, w.walkAnimation);
        w.tickCount = player.tickCount;
        w.setPose(player.getPose());
        w.setShiftKeyDown(player.isShiftKeyDown());
        w.setSprinting(player.isSprinting());
        w.setSwimming(player.isSwimming());
        w.setInvisible(false);
        return w;
    }

    public static void clearProxy() {
        proxyWitch = null;
    }
}
