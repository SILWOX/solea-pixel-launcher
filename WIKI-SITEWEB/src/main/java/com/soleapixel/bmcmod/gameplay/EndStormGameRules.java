package com.soleapixel.bmcmod.gameplay;

import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/**
 * {@code endStormOnlyEnd} — défaut {@code true} : End Storm (potions / raid) réservé à l’End.
 */
public final class EndStormGameRules {
    public static final GameRules.Key<GameRules.BooleanValue> END_STORM_ONLY_END =
            GameRules.register("endStormOnlyEnd", GameRules.Category.MISC, GameRules.BooleanValue.create(true));

    private EndStormGameRules() {}

    /** Si vrai, l’End Storm n’est autorisé que dans la dimension End. */
    public static boolean isRestrictedToEnd(Level level) {
        return level.getGameRules().getBoolean(END_STORM_ONLY_END);
    }
}
