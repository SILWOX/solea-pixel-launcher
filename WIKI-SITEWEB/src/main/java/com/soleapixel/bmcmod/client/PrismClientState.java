package com.soleapixel.bmcmod.client;

/**
 * Pool Prisme reçu du serveur (affichage des cœurs, pas l’absorption vanilla).
 */
public final class PrismClientState {
    private static float current;
    private static float max;

    private PrismClientState() {
    }

    public static void set(float currentIn, float maxIn) {
        current = currentIn;
        max = maxIn;
    }

    public static float getCurrent() {
        return current;
    }

    public static float getMax() {
        return max;
    }
}
