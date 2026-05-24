package com.soleapixel.bmcmod.block.feeder;

import net.minecraft.util.StringRepresentable;

/**
 * Variante de modèle / blockstate (faces avant). La quantité stockée est dans la BlockEntity.
 */
public enum FeederContentKind implements StringRepresentable {
    EMPTY("empty"),
    SEEDS("seeds"),
    WHEAT("wheat"),
    POTATO("potato"),
    CARROT("carrot");

    private final String name;

    FeederContentKind(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
