package com.soleapixel.bmcmod.registry;

import com.soleapixel.bmcmod.block.hollow.HollowBarrelBlock;
import com.soleapixel.bmcmod.block.hollow.HollowLogBlock;
import com.soleapixel.bmcmod.block.hollow.HollowSaplingBlock;
import com.soleapixel.bmcmod.worldgen.HollowTreeGrower;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Bois « Hollow » (End) : tronc haut, feuillage violet, pousse sur pierre de l’End / chorus grass.
 */
public final class HollowWoodBlocks {
    /** Sons portes / plaques / portillon : cherry (proche du set violet). */
    public static final BlockSetType HOLLOW_BLOCK_SET = BlockSetType.CHERRY;

    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_HOLLOW_LOG = ModBlocks.BLOCKS.register("stripped_hollow_log",
            () -> new RotatedPillarBlock(hollowLogProperties()));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_HOLLOW_WOOD = ModBlocks.BLOCKS.register("stripped_hollow_wood",
            () -> new RotatedPillarBlock(hollowWoodProperties()));

    public static final DeferredBlock<HollowLogBlock> HOLLOW_LOG = ModBlocks.BLOCKS.register("hollow_log",
            () -> new HollowLogBlock(hollowLogProperties(), () -> STRIPPED_HOLLOW_LOG.get()));
    public static final DeferredBlock<HollowLogBlock> HOLLOW_WOOD = ModBlocks.BLOCKS.register("hollow_wood",
            () -> new HollowLogBlock(hollowWoodProperties(), () -> STRIPPED_HOLLOW_WOOD.get()));

    public static final DeferredBlock<Block> HOLLOW_PLANKS = ModBlocks.BLOCKS.register("hollow_planks",
            () -> new Block(hollowPlanksProperties()));

    public static final DeferredBlock<HollowBarrelBlock> HOLLOW_BARREL = ModBlocks.BLOCKS.register("hollow_barrel",
            () -> new HollowBarrelBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).mapColor(MapColor.COLOR_PURPLE)));
    public static final DeferredBlock<Block> HOLLOW_BOOKSHELF = ModBlocks.BLOCKS.register("hollow_bookshelf",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BOOKSHELF).mapColor(MapColor.COLOR_PURPLE)));

    public static final DeferredBlock<StairBlock> HOLLOW_STAIRS = ModBlocks.BLOCKS.register("hollow_stairs",
            () -> new StairBlock(HOLLOW_PLANKS.get().defaultBlockState(), hollowPlanksProperties()));
    public static final DeferredBlock<SlabBlock> HOLLOW_SLAB = ModBlocks.BLOCKS.register("hollow_slab",
            () -> new SlabBlock(hollowPlanksProperties()));

    public static final DeferredBlock<FenceBlock> HOLLOW_FENCE = ModBlocks.BLOCKS.register("hollow_fence",
            () -> new FenceBlock(hollowPlanksProperties()));
    public static final DeferredBlock<FenceGateBlock> HOLLOW_FENCE_GATE = ModBlocks.BLOCKS.register("hollow_fence_gate",
            () -> new FenceGateBlock(WoodType.CHERRY, hollowPlanksProperties()));

    public static final DeferredBlock<DoorBlock> HOLLOW_DOOR = ModBlocks.BLOCKS.register("hollow_door",
            () -> new DoorBlock(HOLLOW_BLOCK_SET, hollowDoorProperties().noOcclusion()));
    public static final DeferredBlock<TrapDoorBlock> HOLLOW_TRAPDOOR = ModBlocks.BLOCKS.register("hollow_trapdoor",
            () -> new TrapDoorBlock(HOLLOW_BLOCK_SET, hollowDoorProperties().noOcclusion().isValidSpawn((s, l, p, t) -> false)));

    public static final DeferredBlock<PressurePlateBlock> HOLLOW_PRESSURE_PLATE = ModBlocks.BLOCKS.register("hollow_pressure_plate",
            () -> new PressurePlateBlock(HOLLOW_BLOCK_SET, hollowPlateProperties()));
    public static final DeferredBlock<ButtonBlock> HOLLOW_BUTTON = ModBlocks.BLOCKS.register("hollow_button",
            () -> new ButtonBlock(HOLLOW_BLOCK_SET, 30, hollowButtonProperties()));

    public static final DeferredBlock<LeavesBlock> HOLLOW_LEAVES = ModBlocks.BLOCKS.register("hollow_leaves",
            () -> new LeavesBlock(hollowLeavesProperties()));
    public static final DeferredBlock<HollowSaplingBlock> HOLLOW_SAPLING = ModBlocks.BLOCKS.register("hollow_sapling",
            () -> new HollowSaplingBlock(HollowTreeGrower.GROWER, hollowSaplingProperties()));

    private HollowWoodBlocks() {
    }

    private static BlockBehaviour.Properties hollowLogProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.65F, 3.0F)
                .sound(SoundType.CHERRY_WOOD);
    }

    private static BlockBehaviour.Properties hollowWoodProperties() {
        return hollowLogProperties();
    }

    private static BlockBehaviour.Properties hollowPlanksProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.45F, 3.0F)
                .sound(SoundType.CHERRY_WOOD);
    }

    private static BlockBehaviour.Properties hollowDoorProperties() {
        return hollowPlanksProperties().noOcclusion().pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties hollowPlateProperties() {
        return hollowPlanksProperties().forceSolidOn().noCollission().pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties hollowButtonProperties() {
        return hollowPlanksProperties().noCollission().pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties hollowLeavesProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(0.2F)
                .randomTicks()
                .sound(SoundType.CHERRY_LEAVES)
                .noOcclusion()
                .isValidSpawn((state, level, pos, type) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false)
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties hollowSaplingProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.CHERRY_SAPLING)
                .pushReaction(PushReaction.DESTROY);
    }
}
