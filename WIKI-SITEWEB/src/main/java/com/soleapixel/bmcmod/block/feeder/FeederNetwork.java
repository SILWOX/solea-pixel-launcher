package com.soleapixel.bmcmod.block.feeder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Index léger des positions de feeders par dimension (pour « plus proche »).
 */
public final class FeederNetwork {
    private static final Map<ResourceKey<Level>, List<BlockPos>> BY_LEVEL = new HashMap<>();

    private FeederNetwork() {
    }

    public static void add(Level level, BlockPos pos) {
        BY_LEVEL.computeIfAbsent(level.dimension(), d -> new ArrayList<>()).add(pos.immutable());
    }

    public static void remove(Level level, BlockPos pos) {
        List<BlockPos> list = BY_LEVEL.get(level.dimension());
        if (list != null) {
            list.removeIf(p -> p.equals(pos));
        }
    }

    public static List<BlockPos> snapshot(ResourceKey<Level> dim) {
        List<BlockPos> list = BY_LEVEL.get(dim);
        return list == null ? List.of() : List.copyOf(list);
    }
}
