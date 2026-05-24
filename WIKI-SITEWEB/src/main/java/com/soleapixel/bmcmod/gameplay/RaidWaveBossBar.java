package com.soleapixel.bmcmod.gameplay;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

/**
 * Barre de raid : remplissage = menace restante (vivants + file), linéaire pour réagir à chaque kill.
 * Les encoches vanilla suivent le nombre de salves prévues (~ une salve = taille {@code miniWaveSize}).
 */
public final class RaidWaveBossBar {
    private RaidWaveBossBar() {}

    /**
     * Nombre approximatif de micro-vagues (pulses de spawn) pour vider la file à taille de salve donnée.
     */
    public static int microWaveCount(int expectedTotal, int miniWaveSize) {
        if (expectedTotal <= 0) {
            return 1;
        }
        int m = Math.max(1, miniWaveSize);
        return Math.max(1, Mth.ceil(expectedTotal / (float) m));
    }

    /**
     * Choisit un overlay à encoches dont la densité reflète le nombre de micro-vagues (sans dépasser les types vanilla).
     */
    public static BossEvent.BossBarOverlay overlayForMicroWaveCount(int microWaves) {
        int n = Mth.clamp(microWaves, 1, 20);
        if (n <= 6) {
            return BossEvent.BossBarOverlay.NOTCHED_6;
        }
        if (n <= 10) {
            return BossEvent.BossBarOverlay.NOTCHED_10;
        }
        if (n <= 12) {
            return BossEvent.BossBarOverlay.NOTCHED_12;
        }
        return BossEvent.BossBarOverlay.NOTCHED_20;
    }

    /**
     * Progression lisse : baisse dès qu’un mob est vaincu (somme vivants + file diminue).
     */
    public static float linearRemainingProgress(int alive, int pending, int expectedTotal) {
        if (expectedTotal <= 0) {
            return 0.0F;
        }
        return Mth.clamp((alive + pending) / (float) expectedTotal, 0.0F, 1.0F);
    }

    public static void applyWaveStyle(ServerBossEvent boss, int expectedTotal, int miniWaveSize) {
        int micro = microWaveCount(expectedTotal, miniWaveSize);
        boss.setOverlay(overlayForMicroWaveCount(micro));
    }
}
