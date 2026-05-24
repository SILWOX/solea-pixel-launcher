package com.soleapixel.bmcmod.item.builderwand;

import java.util.List;

import net.minecraft.util.StringRepresentable;

/** Palier : durabilité et liste de modes (forme + taille). */
public enum BuilderWandTier implements StringRepresentable {
    GOLD("gold", 384, List.of(
            L(6), L(10), L(14),
            P(3, 3), P(4, 4),
            H(4, 4), H(5, 5),
            Cr(5, 5), Co(5, 5),
            D(8), D(12),
            W(4, 3), Hw(5, 5))),
    DIAMOND("diamond", 768, List.of(
            L(8), L(12), L(16), L(20),
            P(4, 4), P(5, 5), P(6, 4),
            H(5, 5), H(6, 6),
            Cr(5, 5), Cr(7, 7),
            Co(6, 6), Co(8, 6),
            D(10), D(16), D(20),
            W(5, 4), W(6, 5), W(7, 5),
            Hw(6, 6), Hw(7, 5))),
    EMERALD("emerald", 1536, List.of(
            L(10), L(16), L(22), L(28),
            P(5, 5), P(6, 6), P(7, 7), P(8, 6),
            H(6, 6), H(7, 7), H(8, 8),
            Cr(7, 7), Cr(9, 9),
            Co(8, 8), Co(10, 8), Co(8, 10),
            D(14), D(22), D(28),
            W(7, 5), W(8, 6), W(9, 6), W(10, 7),
            Hw(7, 7), Hw(8, 6), Hw(9, 7))),
    NETHERITE("netherite", 3072, List.of(
            L(12), L(18), L(24), L(32), L(40),
            P(6, 6), P(7, 7), P(8, 8), P(9, 7), P(10, 8),
            H(7, 7), H(8, 8), H(9, 9), H(10, 10),
            Cr(7, 7), Cr(9, 9), Cr(11, 11),
            Co(8, 10), Co(10, 10), Co(12, 8),
            D(16), D(24), D(32), D(40),
            W(8, 6), W(9, 7), W(10, 8), W(11, 8), W(12, 9),
            Hw(8, 8), Hw(9, 8), Hw(10, 9), Hw(11, 9))),
    ENDERITE("enderite", 6144, List.of(
            L(14), L(20), L(28), L(36), L(48), L(64),
            P(7, 7), P(8, 8), P(9, 9), P(10, 10), P(11, 9), P(12, 10), P(13, 11),
            H(8, 8), H(9, 9), H(10, 10), H(11, 11), H(12, 12), H(13, 13),
            Cr(9, 9), Cr(11, 11), Cr(13, 13), Cr(15, 15),
            Co(10, 10), Co(12, 10), Co(10, 12), Co(14, 12),
            D(18), D(28), D(40), D(52), D(64),
            W(9, 7), W(10, 8), W(11, 9), W(12, 9), W(13, 10), W(14, 10), W(15, 11),
            Hw(9, 9), Hw(10, 9), Hw(11, 10), Hw(12, 10), Hw(13, 11), Hw(14, 11)));

    private final String id;
    private final int durability;
    private final List<WandModeSpec> modes;

    BuilderWandTier(String id, int durability, List<WandModeSpec> modes) {
        this.id = id;
        this.durability = durability;
        this.modes = modes;
    }

    public String id() {
        return id;
    }

    public int durability() {
        return durability;
    }

    public int modeCount() {
        return modes.size();
    }

    public WandModeSpec mode(int index) {
        return modes.get(Math.floorMod(index, modes.size()));
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public enum PlacementKind {
        /** Ligne droite le long de la face cliquée (paramètre a = longueur). */
        LINE,
        /** Rectangle plein sur la face (a × b). */
        PLANE,
        /** Cadre seul sur la face (bord du a × b). */
        HOLLOW_PLANE,
        /** Croix : ligne centrale horizontale + verticale sur le rectangle a × b. */
        CROSS,
        /** Forme en L depuis le coin (a le long d’un côté, b le long de l’autre). */
        CORNER,
        /** Diagonale sur la face (a blocs). */
        DIAGONAL,
        /** Mur : largeur a, hauteur b. */
        WALL,
        /** Mur creux (cadre du rectangle a × b). */
        HOLLOW_WALL
    }

    public record WandModeSpec(PlacementKind kind, int a, int b) {
    }

    private static WandModeSpec L(int a) {
        return new WandModeSpec(PlacementKind.LINE, a, 0);
    }

    private static WandModeSpec P(int a, int b) {
        return new WandModeSpec(PlacementKind.PLANE, a, b);
    }

    private static WandModeSpec H(int a, int b) {
        return new WandModeSpec(PlacementKind.HOLLOW_PLANE, a, b);
    }

    private static WandModeSpec Cr(int a, int b) {
        return new WandModeSpec(PlacementKind.CROSS, a, b);
    }

    private static WandModeSpec Co(int a, int b) {
        return new WandModeSpec(PlacementKind.CORNER, a, b);
    }

    private static WandModeSpec D(int a) {
        return new WandModeSpec(PlacementKind.DIAGONAL, a, 0);
    }

    private static WandModeSpec W(int a, int b) {
        return new WandModeSpec(PlacementKind.WALL, a, b);
    }

    private static WandModeSpec Hw(int a, int b) {
        return new WandModeSpec(PlacementKind.HOLLOW_WALL, a, b);
    }
}
