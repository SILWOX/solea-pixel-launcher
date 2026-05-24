package com.soleapixel.bmcmod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.soleapixel.bmcmod.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Pilier d’obsidienne pour le Hollow Garden : colonne dans l’air au-dessus de la surface, bouchon parfois larmes,
 * et rarement un bloc {@code minecraft:end_crystal} (les mappings NeoForge n’exposent pas toujours {@code Blocks#END_CRYSTAL}).
 */
public final class HollowGardenObsidianSpireFeature extends Feature<NoneFeatureConfiguration> {
    public HollowGardenObsidianSpireFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static BlockState endCrystalWithBase() {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ResourceLocation.withDefaultNamespace("end_crystal"));
        Block b = BuiltInRegistries.BLOCK.getHolder(key).map(Holder::value).orElse(Blocks.AIR);
        if (b == Blocks.AIR) {
            return Blocks.AIR.defaultBlockState();
        }
        BlockState state = b.defaultBlockState();
        for (var p : b.getStateDefinition().getProperties()) {
            if (p instanceof BooleanProperty bp && "show_bottom".equals(p.getName())) {
                return state.setValue(bp, true);
            }
        }
        return state;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource rand = ctx.random();

        if (!level.getBlockState(origin).isAir()) {
            return false;
        }
        BlockState surface = level.getBlockState(origin.below());
        if (!surface.is(Blocks.END_STONE) && !surface.is(ModBlocks.HOLLOW_GRASS.get())) {
            return false;
        }

        int height = Mth.nextInt(rand, 5, 12);
        int placed = 0;
        for (int i = 0; i < height; i++) {
            BlockPos p = origin.above(i);
            BlockState existing = level.getBlockState(p);
            if (!existing.isAir() && !existing.is(Blocks.CHORUS_PLANT) && !existing.is(Blocks.CHORUS_FLOWER)) {
                break;
            }
            if (!existing.canBeReplaced() && !existing.isAir()) {
                break;
            }
            level.setBlock(p, Blocks.OBSIDIAN.defaultBlockState(), 3);
            placed++;
        }
        if (placed == 0) {
            return false;
        }

        BlockPos topObsidian = origin.above(placed - 1);
        if (rand.nextFloat() < 0.4F) {
            level.setBlock(
                    topObsidian,
                    rand.nextBoolean() ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState(),
                    3);
        }

        if (placed >= 7 && rand.nextFloat() < 0.2F) {
            BlockPos crystalPos = origin.above(placed);
            if (level.getBlockState(crystalPos).isAir() && level.getBlockState(crystalPos.above()).isAir()) {
                BlockState crystal = endCrystalWithBase();
                if (!crystal.isAir() && crystal.canSurvive(level, crystalPos)) {
                    level.setBlock(crystalPos, crystal, 3);
                }
            }
        }
        return true;
    }
}
