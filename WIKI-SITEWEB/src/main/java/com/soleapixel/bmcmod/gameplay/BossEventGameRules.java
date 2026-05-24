package com.soleapixel.bmcmod.gameplay;

import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/**
 * {@code doBossEventSpawn} — défaut {@code true} : miniboss aléatoires ({@link BossEventManager}).
 */
public final class BossEventGameRules {
    public static final GameRules.Key<GameRules.BooleanValue> DO_BOSS_EVENT_SPAWN =
            GameRules.register("doBossEventSpawn", GameRules.Category.MOBS, GameRules.BooleanValue.create(true));

    private BossEventGameRules() {}

    public static boolean allowsBossSpawns(Level level) {
        return level.getGameRules().getBoolean(DO_BOSS_EVENT_SPAWN);
    }
}
