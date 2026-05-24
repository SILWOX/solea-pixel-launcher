package com.soleapixel.bmcmod.client;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.soleapixel.bmcmod.item.ScytheItem;

/**
 * Animation toupie 3ᵉ personne (et durée de phase caméra) ; synchronisé S2C pour les autres joueurs.
 */
public final class ScytheSpinVisualTracker {
    private static final class SpinState {
        int remaining;
        final int totalTicks;
        final int fullRotations;

        SpinState(int totalTicks, int fullRotations) {
            this.remaining = totalTicks;
            this.totalTicks = totalTicks;
            this.fullRotations = fullRotations;
        }
    }

    private static final Map<Integer, SpinState> ENTITY_SPIN = new ConcurrentHashMap<>();

    private ScytheSpinVisualTracker() {
    }

    public static void beginSpin(int entityId, int totalTicks, int fullRotations) {
        if (totalTicks <= 0 || fullRotations <= 0) {
            return;
        }
        ENTITY_SPIN.put(entityId, new SpinState(totalTicks, fullRotations));
    }

    public static int getRemainingTicks(int entityId) {
        SpinState s = ENTITY_SPIN.get(entityId);
        return s != null ? s.remaining : 0;
    }

    public static int getTotalTicks(int entityId) {
        SpinState s = ENTITY_SPIN.get(entityId);
        return s != null ? s.totalTicks : ScytheItem.SWEEP_VISUAL_SPIN_TICKS;
    }

    public static int getFullRotations(int entityId) {
        SpinState s = ENTITY_SPIN.get(entityId);
        return s != null ? s.fullRotations : 1;
    }

    public static void tick() {
        Iterator<Map.Entry<Integer, SpinState>> it = ENTITY_SPIN.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, SpinState> e = it.next();
            int n = e.getValue().remaining - 1;
            if (n <= 0) {
                it.remove();
            } else {
                e.getValue().remaining = n;
            }
        }
    }
}
