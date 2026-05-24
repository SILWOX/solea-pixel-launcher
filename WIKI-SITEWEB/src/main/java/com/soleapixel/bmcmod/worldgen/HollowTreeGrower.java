package com.soleapixel.bmcmod.worldgen;

import java.util.Optional;

import net.minecraft.world.level.block.grower.TreeGrower;

public final class HollowTreeGrower {
    /**
     * Ordre vanilla {@link TreeGrower} : nom, chance variante rare, mega, mega secondaire,
     * arbre normal, arbre secondaire, variante fleurs, fleurs secondaires.
     */
    public static final TreeGrower GROWER = new TreeGrower(
            "hollow",
            0.0F,
            Optional.empty(),
            Optional.empty(),
            Optional.of(ModConfiguredFeatures.HOLLOW_TREE),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    private HollowTreeGrower() {
    }
}
