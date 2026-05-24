package com.soleapixel.bmcmod.registry;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.block.endstonefurnace.EndstoneFurnaceBlock;
import com.soleapixel.bmcmod.block.foundry.FoundryBlock;
import com.soleapixel.bmcmod.block.EndAnchorBlock;
import com.soleapixel.bmcmod.block.feeder.FeederBlock;
import com.soleapixel.bmcmod.block.EndSandBlock;
import com.soleapixel.bmcmod.block.HollowGrassBlock;
import com.soleapixel.bmcmod.block.JaerysBlock;
import com.soleapixel.bmcmod.block.QuicksandBlock;
import com.soleapixel.bmcmod.block.PurpleBerryBushBlock;
import com.soleapixel.bmcmod.block.SkeletonVillagerHeadBlock;
import com.soleapixel.bmcmod.block.SkeletonVillagerWallHeadBlock;
import com.soleapixel.bmcmod.block.banner.UndeadCaptainBannerBlock;
import com.soleapixel.bmcmod.block.banner.UndeadCaptainWallBannerBlock;
import com.soleapixel.bmcmod.block.banner.EndStormBannerBlock;
import com.soleapixel.bmcmod.block.banner.EndStormWallBannerBlock;
import com.soleapixel.bmcmod.block.chest.EnchantedChestBlock;
import com.soleapixel.bmcmod.block.chest.FakeChestBlock;
import com.soleapixel.bmcmod.block.infusion.InfusionTableBlock;
import com.soleapixel.bmcmod.block.nebrith.BuddingNebrithBlock;
import com.soleapixel.bmcmod.block.nebrith.NebrithBudBlock;
import com.soleapixel.bmcmod.block.upgradetable.UpgradeTableBlock;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BmcMod.MODID);

    /**
     * Mélange nylium (pas / placement / chute) et pierre de l’End (casse / coup) — sons issus des {@link SoundType}
     * vanilla {@link SoundType#NYLIUM} (même famille que le nylium carmin) pour pas / pose / chute, et casser / coup
     * avec les sons plus « pierre de l’End » de {@link SoundType#DEEPSLATE} (1.21.1 n’expose pas encore
     * {@code BASE_STONE_END} dans cette chaîne de mappings).
     */
    private static final SoundType HOLLOW_GRASS_SOUND = new SoundType(
            1.0F,
            1.0F,
            SoundType.DEEPSLATE.getBreakSound(),
            SoundType.NYLIUM.getStepSound(),
            SoundType.NYLIUM.getPlaceSound(),
            SoundType.DEEPSLATE.getHitSound(),
            SoundType.NYLIUM.getFallSound());

    public static final DeferredBlock<Block> NEBRITH_BLOCK = BLOCKS.registerSimpleBlock("nebrith_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.5F).sound(SoundType.AMETHYST).requiresCorrectToolForDrops());

    public static final DeferredBlock<AmethystClusterBlock> NEBRITH_CLUSTER = BLOCKS.register("nebrith_cluster",
            () -> new AmethystClusterBlock(7.0F, 3.0F, clusterProperties()));

    public static final DeferredBlock<NebrithBudBlock> LARGE_NEBRITH_BUD = BLOCKS.register("large_nebrith_bud",
            () -> new NebrithBudBlock(5.0F, 4.0F, budProperties(), () -> NEBRITH_CLUSTER.get()));

    public static final DeferredBlock<NebrithBudBlock> MEDIUM_NEBRITH_BUD = BLOCKS.register("medium_nebrith_bud",
            () -> new NebrithBudBlock(4.0F, 4.0F, budProperties(), () -> LARGE_NEBRITH_BUD.get()));

    public static final DeferredBlock<NebrithBudBlock> SMALL_NEBRITH_BUD = BLOCKS.register("small_nebrith_bud",
            () -> new NebrithBudBlock(3.0F, 4.0F, budProperties(), () -> MEDIUM_NEBRITH_BUD.get()));

    public static final DeferredBlock<BuddingNebrithBlock> BUDDING_NEBRITH = BLOCKS.register("budding_nebrith",
            () -> new BuddingNebrithBlock(buddingProperties(), () -> SMALL_NEBRITH_BUD.get()));

    public static final DeferredBlock<Block> OPAL_BLOCK = BLOCKS.registerSimpleBlock("opal_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5F).sound(SoundType.AMETHYST).requiresCorrectToolForDrops());
    public static final DeferredBlock<AmethystClusterBlock> OPAL_CLUSTER = BLOCKS.register("opal_cluster",
            () -> new AmethystClusterBlock(7.0F, 3.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noOcclusion().strength(0.2F).sound(SoundType.AMETHYST_CLUSTER)
                            .requiresCorrectToolForDrops()));
    public static final DeferredBlock<NebrithBudBlock> LARGE_OPAL_BUD = BLOCKS.register("large_opal_bud",
            () -> new NebrithBudBlock(5.0F, 4.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noOcclusion().randomTicks().strength(0.2F).sound(SoundType.AMETHYST_CLUSTER),
                    () -> OPAL_CLUSTER.get()));
    public static final DeferredBlock<NebrithBudBlock> MEDIUM_OPAL_BUD = BLOCKS.register("medium_opal_bud",
            () -> new NebrithBudBlock(4.0F, 4.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noOcclusion().randomTicks().strength(0.2F).sound(SoundType.AMETHYST_CLUSTER),
                    () -> LARGE_OPAL_BUD.get()));
    public static final DeferredBlock<NebrithBudBlock> SMALL_OPAL_BUD = BLOCKS.register("small_opal_bud",
            () -> new NebrithBudBlock(3.0F, 4.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noOcclusion().randomTicks().strength(0.2F).sound(SoundType.AMETHYST_CLUSTER),
                    () -> MEDIUM_OPAL_BUD.get()));
    public static final DeferredBlock<BuddingNebrithBlock> BUDDING_OPAL = BLOCKS.register("budding_opal",
            () -> new BuddingNebrithBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).randomTicks().strength(1.5F).sound(SoundType.AMETHYST).requiresCorrectToolForDrops(),
                    () -> SMALL_OPAL_BUD.get()));

    public static final DeferredBlock<Block> TOPAZ_BLOCK = BLOCKS.registerSimpleBlock("topaz_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5F).sound(SoundType.AMETHYST).requiresCorrectToolForDrops());
    public static final DeferredBlock<AmethystClusterBlock> TOPAZ_CLUSTER = BLOCKS.register("topaz_cluster",
            () -> new AmethystClusterBlock(7.0F, 3.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).noOcclusion().strength(0.2F).sound(SoundType.AMETHYST_CLUSTER)
                            .requiresCorrectToolForDrops()));
    public static final DeferredBlock<NebrithBudBlock> LARGE_TOPAZ_BUD = BLOCKS.register("large_topaz_bud",
            () -> new NebrithBudBlock(5.0F, 4.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).noOcclusion().randomTicks().strength(0.2F).sound(SoundType.AMETHYST_CLUSTER),
                    () -> TOPAZ_CLUSTER.get()));
    public static final DeferredBlock<NebrithBudBlock> MEDIUM_TOPAZ_BUD = BLOCKS.register("medium_topaz_bud",
            () -> new NebrithBudBlock(4.0F, 4.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).noOcclusion().randomTicks().strength(0.2F).sound(SoundType.AMETHYST_CLUSTER),
                    () -> LARGE_TOPAZ_BUD.get()));
    public static final DeferredBlock<NebrithBudBlock> SMALL_TOPAZ_BUD = BLOCKS.register("small_topaz_bud",
            () -> new NebrithBudBlock(3.0F, 4.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).noOcclusion().randomTicks().strength(0.2F).sound(SoundType.AMETHYST_CLUSTER),
                    () -> MEDIUM_TOPAZ_BUD.get()));
    public static final DeferredBlock<BuddingNebrithBlock> BUDDING_TOPAZ = BLOCKS.register("budding_topaz",
            () -> new BuddingNebrithBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).randomTicks().strength(1.5F).sound(SoundType.AMETHYST).requiresCorrectToolForDrops(),
                    () -> SMALL_TOPAZ_BUD.get()));

    public static final DeferredBlock<Block> BERYL_BLOCK = BLOCKS.registerSimpleBlock("beryl_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.5F).sound(SoundType.AMETHYST).requiresCorrectToolForDrops());
    public static final DeferredBlock<AmethystClusterBlock> BERYL_CLUSTER = BLOCKS.register("beryl_cluster",
            () -> new AmethystClusterBlock(7.0F, 3.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).noOcclusion().strength(0.2F).sound(SoundType.AMETHYST_CLUSTER)
                            .requiresCorrectToolForDrops()));
    public static final DeferredBlock<NebrithBudBlock> LARGE_BERYL_BUD = BLOCKS.register("large_beryl_bud",
            () -> new NebrithBudBlock(5.0F, 4.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).noOcclusion().randomTicks().strength(0.2F).sound(SoundType.AMETHYST_CLUSTER),
                    () -> BERYL_CLUSTER.get()));
    public static final DeferredBlock<NebrithBudBlock> MEDIUM_BERYL_BUD = BLOCKS.register("medium_beryl_bud",
            () -> new NebrithBudBlock(4.0F, 4.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).noOcclusion().randomTicks().strength(0.2F).sound(SoundType.AMETHYST_CLUSTER),
                    () -> LARGE_BERYL_BUD.get()));
    public static final DeferredBlock<NebrithBudBlock> SMALL_BERYL_BUD = BLOCKS.register("small_beryl_bud",
            () -> new NebrithBudBlock(3.0F, 4.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).noOcclusion().randomTicks().strength(0.2F).sound(SoundType.AMETHYST_CLUSTER),
                    () -> MEDIUM_BERYL_BUD.get()));
    public static final DeferredBlock<BuddingNebrithBlock> BUDDING_BERYL = BLOCKS.register("budding_beryl",
            () -> new BuddingNebrithBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).randomTicks().strength(1.5F).sound(SoundType.AMETHYST).requiresCorrectToolForDrops(),
                    () -> SMALL_BERYL_BUD.get()));

    public static final DeferredBlock<UpgradeTableBlock> UPGRADE_TABLE = BLOCKS.register("upgrade_table",
            () -> new UpgradeTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.5F).sound(SoundType.STONE)));

    /** Débris fossilisés : filons très rares sous terre (Y −64…−30, biais vers le bas), pierre / ardoise des abîmes. */
    public static final DeferredBlock<Block> FOSSIL_DEBRIS = BLOCKS.registerSimpleBlock("fossil_debris",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(30.0F, 1200.0F).sound(SoundType.ANCIENT_DEBRIS).requiresCorrectToolForDrops());

    /** Minerai de rubis (pierre) : génération overworld (couches moyennes / hautes). */
    public static final DeferredBlock<Block> RUBY_ORE = BLOCKS.register("ruby_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7),
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F, 3.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    /** Minerai de rubis des abîmes : même logique de hauteur que le diamant (trapèze), veines plus petites et moins d’essais que le diamant. */
    public static final DeferredBlock<Block> DEEPSLATE_RUBY_ORE = BLOCKS.register("deepslate_ruby_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7),
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> RUBY_BLOCK = BLOCKS.registerSimpleBlock("ruby_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    /** Pierre claire : bloc décoratif type andésite / diorite (pas cuisson four comme la pierre vanilla). */
    public static final DeferredBlock<Block> LIGHT_STONE = BLOCKS.registerSimpleBlock("light_stone",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> LIGHT_SMOOTH_STONE = BLOCKS.registerSimpleBlock("light_smooth_stone",
            BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE).mapColor(MapColor.COLOR_LIGHT_GRAY));

    public static final DeferredBlock<Block> CHISELED_LIGHT_SMOOTH_STONE = BLOCKS.registerSimpleBlock("chiseled_light_smooth_stone",
            BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE).mapColor(MapColor.COLOR_LIGHT_GRAY));

    public static final DeferredBlock<SlabBlock> LIGHT_SMOOTH_STONE_SLAB = BLOCKS.register("light_smooth_stone_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE_SLAB).mapColor(MapColor.COLOR_LIGHT_GRAY)));

    /** Pierre sombre : bloc décoratif (pas cuisson four comme la blackstone vanilla). */
    public static final DeferredBlock<Block> DARK_STONE = BLOCKS.registerSimpleBlock("dark_stone",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> DARK_SMOOTH_STONE = BLOCKS.registerSimpleBlock("dark_smooth_stone",
            BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE));

    public static final DeferredBlock<Block> CHISELED_DARK_SMOOTH_STONE = BLOCKS.registerSimpleBlock("chiseled_dark_smooth_stone",
            BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE));

    public static final DeferredBlock<SlabBlock> DARK_SMOOTH_STONE_SLAB = BLOCKS.register("dark_smooth_stone_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE_SLAB)));

    public static final DeferredBlock<StairBlock> LIGHT_STONE_STAIRS = BLOCKS.register("light_stone_stairs",
            () -> new StairBlock(LIGHT_STONE.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_STAIRS).mapColor(MapColor.COLOR_LIGHT_GRAY)));
    public static final DeferredBlock<SlabBlock> LIGHT_STONE_SLAB = BLOCKS.register("light_stone_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_SLAB).mapColor(MapColor.COLOR_LIGHT_GRAY)));
    public static final DeferredBlock<WallBlock> LIGHT_STONE_WALL = BLOCKS.register("light_stone_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_WALL).mapColor(MapColor.COLOR_LIGHT_GRAY)));
    public static final DeferredBlock<PressurePlateBlock> LIGHT_STONE_PRESSURE_PLATE = BLOCKS.register("light_stone_pressure_plate",
            () -> new PressurePlateBlock(BlockSetType.STONE, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_PRESSURE_PLATE).mapColor(MapColor.COLOR_LIGHT_GRAY)));
    public static final DeferredBlock<ButtonBlock> LIGHT_STONE_BUTTON = BLOCKS.register("light_stone_button",
            () -> new ButtonBlock(BlockSetType.STONE, 20, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON).mapColor(MapColor.COLOR_LIGHT_GRAY)));

    public static final DeferredBlock<StairBlock> LIGHT_SMOOTH_STONE_STAIRS = BLOCKS.register("light_smooth_stone_stairs",
            () -> new StairBlock(LIGHT_SMOOTH_STONE.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_STAIRS).mapColor(MapColor.COLOR_LIGHT_GRAY)));
    public static final DeferredBlock<WallBlock> LIGHT_SMOOTH_STONE_WALL = BLOCKS.register("light_smooth_stone_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_WALL).mapColor(MapColor.COLOR_LIGHT_GRAY)));
    public static final DeferredBlock<PressurePlateBlock> LIGHT_SMOOTH_STONE_PRESSURE_PLATE = BLOCKS.register("light_smooth_stone_pressure_plate",
            () -> new PressurePlateBlock(BlockSetType.STONE, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_PRESSURE_PLATE).mapColor(MapColor.COLOR_LIGHT_GRAY)));
    public static final DeferredBlock<ButtonBlock> LIGHT_SMOOTH_STONE_BUTTON = BLOCKS.register("light_smooth_stone_button",
            () -> new ButtonBlock(BlockSetType.STONE, 20, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON).mapColor(MapColor.COLOR_LIGHT_GRAY)));

    public static final DeferredBlock<StairBlock> DARK_STONE_STAIRS = BLOCKS.register("dark_stone_stairs",
            () -> new StairBlock(DARK_STONE.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.BLACKSTONE_STAIRS)));
    public static final DeferredBlock<SlabBlock> DARK_STONE_SLAB = BLOCKS.register("dark_stone_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BLACKSTONE_SLAB)));
    public static final DeferredBlock<WallBlock> DARK_STONE_WALL = BLOCKS.register("dark_stone_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BLACKSTONE_WALL)));
    public static final DeferredBlock<PressurePlateBlock> DARK_STONE_PRESSURE_PLATE = BLOCKS.register("dark_stone_pressure_plate",
            () -> new PressurePlateBlock(BlockSetType.STONE, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_PRESSURE_PLATE)));
    public static final DeferredBlock<ButtonBlock> DARK_STONE_BUTTON = BLOCKS.register("dark_stone_button",
            () -> new ButtonBlock(BlockSetType.STONE, 20, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON)));

    public static final DeferredBlock<StairBlock> DARK_SMOOTH_STONE_STAIRS = BLOCKS.register("dark_smooth_stone_stairs",
            () -> new StairBlock(DARK_SMOOTH_STONE.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE_STAIRS)));
    public static final DeferredBlock<WallBlock> DARK_SMOOTH_STONE_WALL = BLOCKS.register("dark_smooth_stone_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE_WALL)));
    public static final DeferredBlock<PressurePlateBlock> DARK_SMOOTH_STONE_PRESSURE_PLATE = BLOCKS.register("dark_smooth_stone_pressure_plate",
            () -> new PressurePlateBlock(BlockSetType.POLISHED_BLACKSTONE, BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE)));
    public static final DeferredBlock<ButtonBlock> DARK_SMOOTH_STONE_BUTTON = BLOCKS.register("dark_smooth_stone_button",
            () -> new ButtonBlock(BlockSetType.POLISHED_BLACKSTONE, 20, BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE_BUTTON)));

    /** Débris de l’End : génération Y 20–50, filons 1–3. */
    public static final DeferredBlock<Block> FORGOTTEN_DEBRIS = BLOCKS.registerSimpleBlock("forgotten_debris",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(30.0F, 1200.0F).sound(SoundType.ANCIENT_DEBRIS).requiresCorrectToolForDrops());

    /** Sable de l’End : bordures des lacs ; gravité comme le sable vanille. */
    public static final DeferredBlock<EndSandBlock> END_SAND = BLOCKS.register("end_sand",
            () -> new EndSandBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.SAND)
                            .instrument(NoteBlockInstrument.SNARE)
                            .strength(0.5F)
                            .sound(SoundType.SAND)
                            ));

    /** Sable mouvant (désert) : noyade + absorption ; voir {@link com.soleapixel.bmcmod.gameplay.QuicksandGameplay}. */
    public static final DeferredBlock<QuicksandBlock> QUICKSAND = BLOCKS.register("quicksand",
            () -> new QuicksandBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .instrument(NoteBlockInstrument.SNARE)
                    .strength(0.6F)
                    .sound(SoundType.SAND)));

    /** Ancre de réapparition (glowstone + clic) : valide uniquement dans l’End ; voir {@link com.soleapixel.bmcmod.mixin.RespawnAnchorBlockMixin}. */
    public static final DeferredBlock<EndAnchorBlock> END_ANCHOR = BLOCKS.register("end_anchor",
            () -> new EndAnchorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.RESPAWN_ANCHOR)));

    /**
     * Sol du Hollow Garden (nom distinct du chorus vanille) ; Silk Touch → ce bloc, sinon pierre de l’End
     * (loot table {@code data/bmcmod/loot_table/blocks/hollow_grass.json}).
     */
    public static final DeferredBlock<Block> HOLLOW_GRASS = BLOCKS.register("hollow_grass",
            () -> new HollowGrassBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_GREEN)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .strength(3.0F, 9.0F)
                            .sound(HOLLOW_GRASS_SOUND)
                            .randomTicks()));

    /** Petite plante de l’End (particules type bâton / portail inversé). */
    public static final DeferredBlock<JaerysBlock> JAERYS = BLOCKS.register("jaerys",
            () -> new JaerysBlock(JaerysBlock.jaerysProperties()));

    /** Baies violettes du Hollow Garden ; pousse sans lumière, poudre d’os, replantable en Overworld. */
    public static final DeferredBlock<PurpleBerryBushBlock> PURPLE_BERRY_BUSH = BLOCKS.register("purple_berry_bush",
            () -> new PurpleBerryBushBlock(PurpleBerryBushBlock.berryBushProperties()));

    public static final DeferredBlock<Block> ENDERITE_BLOCK = BLOCKS.registerSimpleBlock("enderite_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(50.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> BOREAL_BLOCK = BLOCKS.registerSimpleBlock("boreal_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(56.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops());

    /** Comportement coffre vanilla (27/54) ; rendu + buffer XP {@link com.soleapixel.bmcmod.block.chest.EnchantedChestBlockEntity}. */
    public static final DeferredBlock<EnchantedChestBlock> ENCHANTED_CHEST = BLOCKS.register("enchanted_chest",
            () -> new EnchantedChestBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.GOLD)
                            .strength(2.5F, 3.0F)
                            .sound(SoundType.WOOD)
                            .ignitedByLava()
                            .noOcclusion()));

    /** Coffre visuellement vanilla ; clic → {@link com.soleapixel.bmcmod.entity.MimicChest}. */
    public static final DeferredBlock<FakeChestBlock> FAKE_CHEST = BLOCKS.register("fake_chest",
            () -> new FakeChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST).noLootTable()));

    /**
     * Table d’infusion : même forme + animation de livre que la table d’enchantement ; pas de GUI pour l’instant.
     * Propriétés alignées sur la table vanilla.
     */
    public static final DeferredBlock<InfusionTableBlock> INFUSION_TABLE = BLOCKS.register("infusion_table",
            () -> new InfusionTableBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .lightLevel(s -> 7)
                            .strength(5.0F, 1200.0F)));

    /** Mangeoire : nourriture pour animaux + reproduction automatique. */
    public static final DeferredBlock<FeederBlock> FEEDER = BLOCKS.register("feeder",
            () -> new FeederBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).noLootTable()));

    /** Four End : cuisson accélérée + plus d’XP (voir mixins). */
    public static final DeferredBlock<EndstoneFurnaceBlock> ENDSTONE_FURNACE = BLOCKS.register("endstone_furnace",
            () -> new EndstoneFurnaceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE)));
    /** Foundry : four lent spécialisé dans la fonte d’équipements avec rendement selon durabilité. */
    public static final DeferredBlock<FoundryBlock> FOUNDRY = BLOCKS.register("foundry",
            () -> new FoundryBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BLAST_FURNACE)));

    /**
     * Torche du Vide : mêmes règles que la torche vanilla (collision, casse, son, lumière, etc.) ;
     * particules de portail inversé + carte violette pour la couleur de bloc.
     */
    public static final DeferredBlock<TorchBlock> VOID_TORCH = BLOCKS.register("void_torch",
            () -> new TorchBlock(
                    ParticleTypes.REVERSE_PORTAL,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.TORCH).mapColor(MapColor.COLOR_PURPLE)));

    /** Torche murale (même logique que {@link Blocks#WALL_TORCH}) : item unique {@code void_torch}. */
    public static final DeferredBlock<WallTorchBlock> VOID_WALL_TORCH = BLOCKS.register("void_wall_torch",
            () -> new WallTorchBlock(
                    ParticleTypes.REVERSE_PORTAL,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WALL_TORCH).mapColor(MapColor.COLOR_PURPLE)));

    /** Lanterne du Vide : lumière forte (15), cadre pierre de l’End + cœur violet (texture animée). */
    public static final DeferredBlock<LanternBlock> VOID_LANTERN = BLOCKS.register("void_lantern",
            () -> new LanternBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(3.5F)
                            .sound(SoundType.LANTERN)
                            .lightLevel(s -> 15)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)));

    /** Tête de Skeleton Villager stable: sol/mur/casque sans SkullBlockEntity (anti-crash). */
    public static final DeferredBlock<SkeletonVillagerHeadBlock> SKELETON_VILLAGER_SKULL = BLOCKS.register("skeleton_villager_skull",
            () -> new SkeletonVillagerHeadBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SKELETON_SKULL).noOcclusion()));

    public static final DeferredBlock<SkeletonVillagerWallHeadBlock> SKELETON_VILLAGER_WALL_SKULL = BLOCKS.register("skeleton_villager_wall_skull",
            () -> new SkeletonVillagerWallHeadBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SKELETON_WALL_SKULL).noOcclusion()));

    /** Source {@link com.soleapixel.bmcmod.registry.ModFluids#EXPERIENCE_STILL} ; écoulement plus lent / moins étalé que l’eau. */
    /**
     * Bannière invasion : bloc vanilla + motif {@code bmcmod:undead_invasion} (texture
     * {@code assets/bmcmod/textures/entity/banner/undead_invasion_captain.png}).
     */
    public static final DeferredBlock<UndeadCaptainBannerBlock> UNDEAD_INVASION_CAPTAIN_BANNER = BLOCKS.register(
            "undead_invasion_captain_banner",
            () -> new UndeadCaptainBannerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BLACK_BANNER)));

    public static final DeferredBlock<UndeadCaptainWallBannerBlock> UNDEAD_INVASION_CAPTAIN_WALL_BANNER = BLOCKS.register(
            "undead_invasion_captain_wall_banner",
            () -> new UndeadCaptainWallBannerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BLACK_WALL_BANNER)));

    public static final DeferredBlock<EndStormBannerBlock> END_STORM_CAPTAIN_BANNER = BLOCKS.register(
            "end_storm_captain_banner",
            () -> new EndStormBannerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BLACK_BANNER)));

    public static final DeferredBlock<EndStormWallBannerBlock> END_STORM_CAPTAIN_WALL_BANNER = BLOCKS.register(
            "end_storm_captain_wall_banner",
            () -> new EndStormWallBannerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BLACK_WALL_BANNER)));

    public static final DeferredBlock<LiquidBlock> EXPERIENCE_LIQUID = BLOCKS.register("experience_liquid",
            () -> new LiquidBlock(
                    (FlowingFluid) ModFluids.EXPERIENCE_FLOWING.get(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_GREEN)
                            .replaceable()
                            .noCollission()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid()
                            .lightLevel(s -> 9)
                            .sound(SoundType.EMPTY)));

    private static BlockBehaviour.Properties budProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noOcclusion().randomTicks().strength(0.2F).sound(SoundType.AMETHYST_CLUSTER);
    }

    private static BlockBehaviour.Properties clusterProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noOcclusion().strength(0.2F).sound(SoundType.AMETHYST_CLUSTER);
    }

    private static BlockBehaviour.Properties buddingProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).randomTicks().strength(1.5F).sound(SoundType.AMETHYST).requiresCorrectToolForDrops();
    }

    private ModBlocks() {
    }
}
