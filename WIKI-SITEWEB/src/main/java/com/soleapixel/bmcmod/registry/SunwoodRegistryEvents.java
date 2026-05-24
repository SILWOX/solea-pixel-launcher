package com.soleapixel.bmcmod.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;

/** Enregistré sur le bus mod depuis {@link com.soleapixel.bmcmod.BmcMod}. */
public final class SunwoodRegistryEvents {
    private SunwoodRegistryEvents() {
    }

    public static void onBlockEntityTypeAddBlocks(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityType.SIGN, SunwoodBlocks.SUNWOOD_SIGN.get(), SunwoodBlocks.SUNWOOD_WALL_SIGN.get());
        event.modify(BlockEntityType.HANGING_SIGN, SunwoodBlocks.SUNWOOD_HANGING_SIGN.get(), SunwoodBlocks.SUNWOOD_WALL_HANGING_SIGN.get());
        event.modify(
                BlockEntityType.BANNER,
                ModBlocks.UNDEAD_INVASION_CAPTAIN_BANNER.get(),
                ModBlocks.UNDEAD_INVASION_CAPTAIN_WALL_BANNER.get(),
                ModBlocks.END_STORM_CAPTAIN_BANNER.get(),
                ModBlocks.END_STORM_CAPTAIN_WALL_BANNER.get());
    }
}
