package com.soleapixel.bmcmod.util;

import java.lang.reflect.Field;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.world.entity.WalkAnimationState;

/**
 * Copie l’état interne de marche (jambes) d’une entité vers une autre — rendu proxy sorcière.
 * <p>
 * Ne pas lancer d’exception au chargement de la classe : certains noms de champs varient entre
 * versions / cartes, et l’ancien {@code static {}} provoquait un {@code ExceptionInInitializerError}
 * au démarrage.
 */
public final class WalkAnimationStateCopy {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static Field speedOldField;
    private static Field speedField;
    private static Field positionField;
    private static volatile boolean resolveAttempted;
    private static volatile boolean resolveOk;

    private WalkAnimationStateCopy() {
    }

    private static boolean resolveFields() {
        if (resolveAttempted) {
            return resolveOk;
        }
        synchronized (WalkAnimationStateCopy.class) {
            if (resolveAttempted) {
                return resolveOk;
            }
            resolveAttempted = true;
            Class<WalkAnimationState> c = WalkAnimationState.class;
            String[][] nameSets = {
                    {"speedOld", "speed", "position"},
                    {"prevSpeed", "speed", "pos"},
                    {"f_267406_", "f_267371_", "f_267358_"},
            };
            for (String[] names : nameSets) {
                if (names.length != 3) {
                    continue;
                }
                try {
                    Field f0 = c.getDeclaredField(names[0]);
                    Field f1 = c.getDeclaredField(names[1]);
                    Field f2 = c.getDeclaredField(names[2]);
                    f0.setAccessible(true);
                    f1.setAccessible(true);
                    f2.setAccessible(true);
                    speedOldField = f0;
                    speedField = f1;
                    positionField = f2;
                    resolveOk = true;
                    return true;
                } catch (ReflectiveOperationException ignored) {
                    // essayer l’ensemble de noms suivant
                }
            }
            LOGGER.warn("WalkAnimationStateCopy: champs WalkAnimationState introuvables — sync des jambes (morph) désactivée.");
            return false;
        }
    }

    public static void copyFromTo(WalkAnimationState from, WalkAnimationState to) {
        if (!resolveFields() || speedOldField == null) {
            return;
        }
        try {
            speedOldField.setFloat(to, speedOldField.getFloat(from));
            speedField.setFloat(to, speedField.getFloat(from));
            positionField.setFloat(to, positionField.getFloat(from));
        } catch (IllegalAccessException e) {
            LOGGER.warn("WalkAnimationStateCopy.copyFromTo a échoué", e);
        }
    }
}
