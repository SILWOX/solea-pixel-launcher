package com.soleapixel.bmcmod.registry;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.block.hollow.HollowLogBlock;
import com.soleapixel.bmcmod.block.sunwood.SurfaceMossBlock;
import com.soleapixel.bmcmod.worldgen.SunwoodTreeGrower;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.effect.MobEffects;

import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Bois Sunwood (thème soleil) : set complet + fleur {@link #SUNBLOOM} + {@link #SURFACE_MOSS}.
 */
public final class SunwoodBlocks {
    public static final BlockSetType SUNWOOD_BLOCK_SET = BlockSetType.register(new BlockSetType("sunwood"));
    /** Nom avec namespace : {@link net.minecraft.client.renderer.Sheets} parse ce string pour les textures entity/signs. */
    public static final WoodType SUNWOOD_WOOD_TYPE = WoodType.register(new WoodType(BmcMod.MODID + ":sunwood", SUNWOOD_BLOCK_SET));

    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_SUNWOOD_LOG = ModBlocks.BLOCKS.register("stripped_sunwood_log",
            () -> new RotatedPillarBlock(sunwoodLogProperties()));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_SUNWOOD_WOOD = ModBlocks.BLOCKS.register("stripped_sunwood_wood",
            () -> new RotatedPillarBlock(sunwoodLogProperties()));

    public static final DeferredBlock<HollowLogBlock> SUNWOOD_LOG = ModBlocks.BLOCKS.register("sunwood_log",
            () -> new HollowLogBlock(sunwoodLogProperties(), () -> STRIPPED_SUNWOOD_LOG.get()));
    public static final DeferredBlock<HollowLogBlock> SUNWOOD_WOOD = ModBlocks.BLOCKS.register("sunwood_wood",
            () -> new HollowLogBlock(sunwoodLogProperties(), () -> STRIPPED_SUNWOOD_WOOD.get()));

    public static final DeferredBlock<Block> SUNWOOD_PLANKS = ModBlocks.BLOCKS.register("sunwood_planks",
            () -> new Block(sunwoodPlanksProperties()));

    public static final DeferredBlock<StairBlock> SUNWOOD_STAIRS = ModBlocks.BLOCKS.register("sunwood_stairs",
            () -> new StairBlock(SUNWOOD_PLANKS.get().defaultBlockState(), sunwoodPlanksProperties()));
    public static final DeferredBlock<SlabBlock> SUNWOOD_SLAB = ModBlocks.BLOCKS.register("sunwood_slab",
            () -> new SlabBlock(sunwoodPlanksProperties()));

    public static final DeferredBlock<FenceBlock> SUNWOOD_FENCE = ModBlocks.BLOCKS.register("sunwood_fence",
            () -> new FenceBlock(sunwoodPlanksProperties()));
    public static final DeferredBlock<FenceGateBlock> SUNWOOD_FENCE_GATE = ModBlocks.BLOCKS.register("sunwood_fence_gate",
            () -> new FenceGateBlock(SUNWOOD_WOOD_TYPE, sunwoodPlanksProperties()));

    public static final DeferredBlock<DoorBlock> SUNWOOD_DOOR = ModBlocks.BLOCKS.register("sunwood_door",
            () -> new DoorBlock(SUNWOOD_BLOCK_SET, sunwoodDoorProperties().noOcclusion()));
    public static final DeferredBlock<TrapDoorBlock> SUNWOOD_TRAPDOOR = ModBlocks.BLOCKS.register("sunwood_trapdoor",
            () -> new TrapDoorBlock(SUNWOOD_BLOCK_SET, sunwoodDoorProperties().noOcclusion().isValidSpawn((s, l, p, t) -> false)));

    public static final DeferredBlock<PressurePlateBlock> SUNWOOD_PRESSURE_PLATE = ModBlocks.BLOCKS.register("sunwood_pressure_plate",
            () -> new PressurePlateBlock(SUNWOOD_BLOCK_SET, sunwoodPlateProperties()));
    public static final DeferredBlock<ButtonBlock> SUNWOOD_BUTTON = ModBlocks.BLOCKS.register("sunwood_button",
            () -> new ButtonBlock(SUNWOOD_BLOCK_SET, 30, sunwoodButtonProperties()));

    public static final DeferredBlock<StandingSignBlock> SUNWOOD_SIGN = ModBlocks.BLOCKS.register("sunwood_sign",
            () -> new StandingSignBlock(SUNWOOD_WOOD_TYPE, sunwoodSignProperties().noCollission()));
    public static final DeferredBlock<WallSignBlock> SUNWOOD_WALL_SIGN = ModBlocks.BLOCKS.register("sunwood_wall_sign",
            () -> new WallSignBlock(SUNWOOD_WOOD_TYPE, sunwoodSignProperties().noCollission()));

    public static final DeferredBlock<CeilingHangingSignBlock> SUNWOOD_HANGING_SIGN = ModBlocks.BLOCKS.register("sunwood_hanging_sign",
            () -> new CeilingHangingSignBlock(SUNWOOD_WOOD_TYPE, sunwoodSignProperties().noCollission()));
    public static final DeferredBlock<WallHangingSignBlock> SUNWOOD_WALL_HANGING_SIGN = ModBlocks.BLOCKS.register("sunwood_wall_hanging_sign",
            () -> new WallHangingSignBlock(SUNWOOD_WOOD_TYPE, sunwoodSignProperties().noCollission()));

    public static final DeferredBlock<LeavesBlock> SUNWOOD_LEAVES = ModBlocks.BLOCKS.register("sunwood_leaves",
            () -> new LeavesBlock(sunwoodLeavesProperties()));
    public static final DeferredBlock<SaplingBlock> SUNWOOD_SAPLING = ModBlocks.BLOCKS.register("sunwood_sapling",
            () -> new SaplingBlock(SunwoodTreeGrower.GROWER, sunwoodSaplingProperties()));

    public static final DeferredBlock<FlowerBlock> SUNBLOOM = ModBlocks.BLOCKS.register("sunbloom",
            () -> new FlowerBlock(MobEffects.REGENERATION, 6.0F, sunbloomProperties()));
    public static final DeferredBlock<SurfaceMossBlock> SURFACE_MOSS = ModBlocks.BLOCKS.register("surface_moss",
            () -> new SurfaceMossBlock(surfaceMossProperties()));

    private SunwoodBlocks() {
    }

    private static BlockBehaviour.Properties sunwoodLogProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.2F, 3.0F)
                .sound(SoundType.CHERRY_WOOD);
    }

    private static BlockBehaviour.Properties sunwoodPlanksProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_YELLOW)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.0F, 3.0F)
                .sound(SoundType.CHERRY_WOOD);
    }

    private static BlockBehaviour.Properties sunwoodDoorProperties() {
        return sunwoodPlanksProperties().noOcclusion().pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties sunwoodPlateProperties() {
        return sunwoodPlanksProperties().forceSolidOn().noCollission().pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties sunwoodButtonProperties() {
        return sunwoodPlanksProperties().noCollission().pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties sunwoodSignProperties() {
        return sunwoodPlanksProperties().forceSolidOn().noCollission().pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties sunwoodLeavesProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_YELLOW)
                .strength(0.2F)
                .randomTicks()
                .sound(SoundType.CHERRY_LEAVES)
                .noOcclusion()
                .isValidSpawn((state, level, pos, type) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false)
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties sunwoodSaplingProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.CHERRY_SAPLING)
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties sunbloomProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.CHERRY_SAPLING)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties surfaceMossProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(0.1F)
                .sound(SoundType.MOSS_CARPET)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY);
    }
}
