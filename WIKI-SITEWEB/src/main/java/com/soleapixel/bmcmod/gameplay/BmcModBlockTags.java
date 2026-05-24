package com.soleapixel.bmcmod.gameplay;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import com.soleapixel.bmcmod.BmcMod;

public final class BmcModBlockTags {
    public static final TagKey<Block> VEIN_WHISPER_STONE = TagKey.create(Registries.BLOCK, BmcMod.loc("mineable/vein_whisper_stone"));

    private BmcModBlockTags() {
    }
}
