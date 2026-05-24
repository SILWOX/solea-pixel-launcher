package com.soleapixel.bmcmod.worldgen;

import java.util.Optional;

import net.minecraft.world.level.block.grower.TreeGrower;

public final class SunwoodTreeGrower {
    public static final TreeGrower GROWER = new TreeGrower(
            "sunwood",
            0.0F,
            Optional.empty(),
            Optional.empty(),
            Optional.of(ModConfiguredFeatures.SUNWOOD_TREE),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    private SunwoodTreeGrower() {
    }
}
