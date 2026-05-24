package com.soleapixel.bmcmod;

import java.util.ArrayList;
import java.util.Optional;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import com.soleapixel.bmcmod.command.MorphTemplateEntityArgument;
import com.soleapixel.bmcmod.brewing.BmcModBrewing;
import com.soleapixel.bmcmod.gameplay.ShieldEnchantmentEvents;
import com.soleapixel.bmcmod.copper.CopperEquipment;
import com.soleapixel.bmcmod.emerald.EmeraldEquipment;
import com.soleapixel.bmcmod.enderite.EnderiteEquipment;
import com.soleapixel.bmcmod.boreal.BorealEquipment;
import com.soleapixel.bmcmod.registry.ModEnchantmentKeys;
import com.soleapixel.bmcmod.registry.ModGlobalLoot;
import com.soleapixel.bmcmod.registry.ModMobEffects;
import com.soleapixel.bmcmod.registry.ModParticles;
import com.soleapixel.bmcmod.registry.ModPotions;
import com.soleapixel.bmcmod.util.PlayerHeadProfileUtil;
import com.soleapixel.bmcmod.shulker.ShulkerEquipment;
import com.soleapixel.bmcmod.villager.ModProfessions;
import com.soleapixel.bmcmod.villager.VillagerHatEquipment;
import com.soleapixel.bmcmod.obsidian.ObsidianEquipment;
import com.soleapixel.bmcmod.sky.SkyEquipment;
import com.soleapixel.bmcmod.gameplay.ExperienceLiquidEvents;
import com.soleapixel.bmcmod.gameplay.FeederInteractionEvents;
import com.soleapixel.bmcmod.gameplay.HollowGardenEvents;
import com.soleapixel.bmcmod.gameplay.BossEventGameRules;
import com.soleapixel.bmcmod.gameplay.EndStormGameRules;
import com.soleapixel.bmcmod.gameplay.EndStormManager;
import com.soleapixel.bmcmod.gameplay.UndeadInvasionManager;
import com.soleapixel.bmcmod.gameplay.WitherStaffAllyEvents;
import com.soleapixel.bmcmod.worldgen.ModBiomes;
import com.soleapixel.bmcmod.registry.ModDataComponents;
import com.soleapixel.bmcmod.registry.ModBlockEntityTypes;
import com.soleapixel.bmcmod.registry.ModBlocks;
import com.soleapixel.bmcmod.registry.ModFeatures;
import com.soleapixel.bmcmod.registry.ModEntities;
import com.soleapixel.bmcmod.registry.ModMenus;
import com.soleapixel.bmcmod.registry.ModFluids;
import com.soleapixel.bmcmod.registry.ModItems;
import com.soleapixel.bmcmod.registry.ModSounds;
import com.soleapixel.bmcmod.registry.SunwoodRegistryEvents;
import com.soleapixel.bmcmod.block.feeder.FeederCapabilities;
import com.soleapixel.bmcmod.entity.vehicle.SunwoodChestBoatCapabilities;
import com.soleapixel.bmcmod.recipe.infusion.InfusionRecipes;
import com.soleapixel.bmcmod.item.melody.MelodyHornTuneRegistry;
import com.soleapixel.bmcmod.item.melody.MelodyHornTuneReloader;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(BmcMod.MODID)
public class BmcMod {
    public static final String MODID = "bmcmod";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BMCMOD_TAB = CREATIVE_MODE_TABS.register("bmcmod_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.bmcmod"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> new ItemStack(ModItems.NEBRITH_SHARD.get()))
            .displayItems(BmcMod::fillBetterMinecraftTab)
            .build());

    public BmcMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerGameplayConfig.SPEC);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(BmcMod::registerCommandArgumentTypes);

        EnderiteEquipment.ARMOR_MATERIALS.register(modEventBus);
        BorealEquipment.ARMOR_MATERIALS.register(modEventBus);
        CopperEquipment.ARMOR_MATERIALS.register(modEventBus);
        EmeraldEquipment.ARMOR_MATERIALS.register(modEventBus);
        ShulkerEquipment.ARMOR_MATERIALS.register(modEventBus);
        VillagerHatEquipment.ARMOR_MATERIALS.register(modEventBus);
        ModProfessions.PROFESSIONS.register(modEventBus);
        ObsidianEquipment.ARMOR_MATERIALS.register(modEventBus);
        SkyEquipment.ARMOR_MATERIALS.register(modEventBus);
        ModDataComponents.REGISTRY.register(modEventBus);
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        ModBlockEntityTypes.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENU_TYPES.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        ModPotions.POTIONS.register(modEventBus);
        ModGlobalLoot.GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(BmcModBrewing::registerRecipes);
        NeoForge.EVENT_BUS.register(ShieldEnchantmentEvents.class);
        NeoForge.EVENT_BUS.register(ExperienceLiquidEvents.class);
        NeoForge.EVENT_BUS.register(HollowGardenEvents.class);
        NeoForge.EVENT_BUS.register(FeederInteractionEvents.class);
        NeoForge.EVENT_BUS.register(WitherStaffAllyEvents.class);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(BmcMod::onAddReloadListeners);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(SunwoodRegistryEvents::onBlockEntityTypeAddBlocks);
        modEventBus.addListener(SunwoodChestBoatCapabilities::register);
        modEventBus.addListener(FeederCapabilities::register);

    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            java.util.Objects.requireNonNull(EndStormGameRules.END_STORM_ONLY_END);
            java.util.Objects.requireNonNull(BossEventGameRules.DO_BOSS_EVENT_SPAWN);
            InfusionRecipes.registerDefaultRecipes();
        });
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new MelodyHornTuneReloader());
    }

    private static void registerCommandArgumentTypes(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.COMMAND_ARGUMENT_TYPE)) {
            return;
        }
        var info = SingletonArgumentInfo.contextFree(MorphTemplateEntityArgument::mobTemplate);
        ArgumentTypeInfos.registerByClass(MorphTemplateEntityArgument.class, info);
        event.register(Registries.COMMAND_ARGUMENT_TYPE, BmcMod.loc("morph_template_entity"), () -> info);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            for (ItemStack stack : new ArrayList<>(event.getParentEntries())) {
                if (isBmcModPotionStack(stack)) {
                    event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                }
            }
            for (ItemStack stack : new ArrayList<>(event.getSearchEntries())) {
                if (isBmcModPotionStack(stack)) {
                    event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                }
            }
            event.accept(ModItems.PURPLE_BERRY.get());
            event.accept(ModItems.GOLD_BERRY.get());
            event.accept(ModItems.SUPER_BONE.get());
            event.accept(ModItems.UNDEAD_BOTTLE_1.get());
            event.accept(ModItems.UNDEAD_BOTTLE_2.get());
            event.accept(ModItems.UNDEAD_BOTTLE_3.get());
            event.accept(ModItems.UNDEAD_BOTTLE_4.get());
            event.accept(ModItems.UNDEAD_BOTTLE_5.get());
            event.accept(ModItems.UNDEAD_BOTTLE_6.get());
            event.accept(ModItems.END_STORM_BOTTLE_1.get());
            event.accept(ModItems.END_STORM_BOTTLE_2.get());
            event.accept(ModItems.END_STORM_BOTTLE_3.get());
            event.accept(ModItems.END_STORM_BOTTLE_4.get());
            event.accept(ModItems.UNDEAD_TOTEM.get());
        }
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.ILLUSIONER_SPAWN_EGG.get());
            event.accept(ModItems.SKELETON_VILLAGER_SPAWN_EGG.get());
            event.accept(ModItems.SOUL_SKELETON_VILLAGER_SPAWN_EGG.get());
            event.accept(ModItems.UNDEAD_ILLAGER_SPAWN_EGG.get());
            event.accept(ModItems.VLINX_SPAWN_EGG.get());
            event.accept(ModItems.MIMIC_CHEST_SPAWN_EGG.get());
            event.accept(ModItems.QUEST_TRADER_SPAWN_EGG.get());
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.VOID_TORCH_ITEM.get());
            event.accept(ModItems.VOID_LANTERN_ITEM.get());
            event.accept(ModItems.SKELETON_VILLAGER_SKULL_ITEM.get());
            event.accept(ModItems.DUMMY_ITEM.get());
        }
        if (event.getTabKey() == CreativeModeTabs.OP_BLOCKS) {
            event.accept(ModItems.RARITY_STICK_UNCOMMON);
            event.accept(ModItems.RARITY_STICK_COMMON);
            event.accept(ModItems.RARITY_STICK_RARE);
            event.accept(ModItems.RARITY_STICK_EPIC);
            event.accept(ModItems.RARITY_STICK_LEGENDARY);
            event.accept(ModItems.RARITY_STICK_EXOTIC);
            event.accept(ModItems.RARITY_STICK_MYTHIC);
            event.accept(ModItems.RARITY_STICK_FRAGMENTED);
        }
    }

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        // Avant chargement des mondes : évite que possibleBiomes() mémorise sans Hollow Garden (Suppliers.memoize).
        ModBiomes.cacheBiomeHolders(event.getServer());
        MelodyHornTuneRegistry.bootstrapIfEmpty(event.getServer().getResourceManager());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ModBiomes.cacheBiomeHolders(event.getServer());
    }

    /**
     * Casque → bottes sur une seule ligne du créatif (grille 9 colonnes) : le bloc doit être précédé d’un nombre d’items multiple de 9.
     */
    private static void acceptCreativeArmorQuad(CreativeModeTab.Output out, Item helmet, Item chestplate, Item leggings, Item boots) {
        out.accept(helmet);
        out.accept(chestplate);
        out.accept(leggings);
        out.accept(boots);
    }

    /**
     * Contenu de l’onglet créatif : groupes logiques ; premières rangées alignées sur 9 colonnes (armures sur une ligne).
     */
    private static void fillBetterMinecraftTab(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        HolderLookup.Provider registries = parameters.holders();

        // Ligne 1 (9) : intro + templates + début cuivre
        output.accept(createSilwoxHead());
        output.accept(ModItems.ENDERITE_UPGRADE_SMITHING_TEMPLATE.get());
        output.accept(ModItems.BOREAL_UPGRADE_SMITHING_TEMPLATE.get());
        output.accept(ModItems.BOW_UPGRADE_SMITHING_TEMPLATE.get());
        output.accept(ModItems.STAFF_UPGRADE_SMITHING_TEMPLATE.get());
        output.accept(ModItems.COPPER_NUGGET.get());
        output.accept(ModItems.COPPER_SWORD.get());
        output.accept(ModItems.COPPER_PICKAXE.get());
        output.accept(ModItems.COPPER_SHOVEL.get());

        // Ligne 2 (9) : fin outils cuivre + armure cuivre (H–B sur une ligne) + 3 premières faux
        output.accept(ModItems.COPPER_AXE.get());
        output.accept(ModItems.COPPER_HOE.get());
        acceptCreativeArmorQuad(
                output,
                ModItems.COPPER_HELMET.get(),
                ModItems.COPPER_CHESTPLATE.get(),
                ModItems.COPPER_LEGGINGS.get(),
                ModItems.COPPER_BOOTS.get());
        output.accept(ModItems.IRON_SCYTHE.get());
        output.accept(ModItems.GOLDEN_SCYTHE.get());
        output.accept(ModItems.DIAMOND_SCYTHE.get());

        // Ligne 3 (9) : outils émeraude + armure émeraude (H–B sur une ligne)
        output.accept(ModItems.EMERALD_SWORD.get());
        output.accept(ModItems.EMERALD_PICKAXE.get());
        output.accept(ModItems.EMERALD_SHOVEL.get());
        output.accept(ModItems.EMERALD_AXE.get());
        output.accept(ModItems.EMERALD_HOE.get());
        acceptCreativeArmorQuad(
                output,
                ModItems.EMERALD_HELMET.get(),
                ModItems.EMERALD_CHESTPLATE.get(),
                ModItems.EMERALD_LEGGINGS.get(),
                ModItems.EMERALD_BOOTS.get());

        // Ligne 4 (9) : faux haut palier + matériaux enderite + boréal
        output.accept(ModItems.EMERALD_SCYTHE.get());
        output.accept(ModItems.NETHERITE_SCYTHE.get());
        output.accept(ModItems.ENDERITE_SCYTHE.get());
        output.accept(ModItems.ENDERITE_SCRAP.get());
        output.accept(ModItems.ENDERITE_INGOT.get());
        output.accept(ModItems.ENDERITE_BLOCK_ITEM.get());
        output.accept(ModItems.BOREAL_FRAGMENT.get());
        output.accept(ModItems.BOREAL_INGOT.get());
        output.accept(ModItems.BOREAL_BLOCK_ITEM.get());

        // Ligne 5 (9) : outils + armure enderite
        output.accept(ModItems.ENDERITE_SWORD.get());
        output.accept(ModItems.ENDERITE_PICKAXE.get());
        output.accept(ModItems.ENDERITE_SHOVEL.get());
        output.accept(ModItems.ENDERITE_AXE.get());
        output.accept(ModItems.ENDERITE_HOE.get());
        acceptCreativeArmorQuad(
                output,
                ModItems.ENDERITE_HELMET.get(),
                ModItems.ENDERITE_CHESTPLATE.get(),
                ModItems.ENDERITE_LEGGINGS.get(),
                ModItems.ENDERITE_BOOTS.get());

        // Ligne 6 (9) : outils + armure boréal (sans houe)
        output.accept(ModItems.BOREAL_SWORD.get());
        output.accept(ModItems.BOREAL_PICKAXE.get());
        output.accept(ModItems.BOREAL_SHOVEL.get());
        output.accept(ModItems.BOREAL_AXE.get());
        output.accept(ModItems.BOREAL_SCYTHE.get());
        acceptCreativeArmorQuad(
                output,
                ModItems.BOREAL_HELMET.get(),
                ModItems.BOREAL_CHESTPLATE.get(),
                ModItems.BOREAL_LEGGINGS.get(),
                ModItems.BOREAL_BOOTS.get());

        // Ligne 7 (9) : armures obsidienne + shulker (chacune sur une ligne) + bouclier diamant
        acceptCreativeArmorQuad(
                output,
                ModItems.OBSIDIAN_HELMET.get(),
                ModItems.OBSIDIAN_CHESTPLATE.get(),
                ModItems.OBSIDIAN_LEGGINGS.get(),
                ModItems.OBSIDIAN_BOOTS.get());
        acceptCreativeArmorQuad(
                output,
                ModItems.SHULKER_HELMET.get(),
                ModItems.SHULKER_CHESTPLATE.get(),
                ModItems.SHULKER_LEGGINGS.get(),
                ModItems.SHULKER_BOOTS.get());
        output.accept(ModItems.DIAMOND_SHIELD.get());

        // Ligne 8 (9) : boucliers restants + arcs + carquois + bottes ciel (72 items avant Nebrith = alignement avec l’ancien ordre)
        output.accept(ModItems.NETHERITE_SHIELD.get());
        output.accept(ModItems.ENDERITE_SHIELD.get());
        output.accept(ModItems.LIGHTNING_BOW.get());
        output.accept(ModItems.IRON_BOW.get());
        output.accept(ModItems.GOLD_BOW.get());
        output.accept(ModItems.DIAMOND_BOW.get());
        output.accept(ModItems.EMERALD_BOW.get());
        output.accept(ModItems.QUIVER.get());
        output.accept(ModItems.SKY_BOOTS.get());

        // Nebrith (bloc → bourgeon → stades → cluster → éclat)
        output.accept(ModItems.NEBRITH_BLOCK_ITEM.get());
        output.accept(ModItems.BUDDING_NEBRITH_ITEM.get());
        output.accept(ModItems.SMALL_NEBRITH_BUD_ITEM.get());
        output.accept(ModItems.MEDIUM_NEBRITH_BUD_ITEM.get());
        output.accept(ModItems.LARGE_NEBRITH_BUD_ITEM.get());
        output.accept(ModItems.NEBRITH_CLUSTER_ITEM.get());
        output.accept(ModItems.NEBRITH_SHARD.get());
        output.accept(ModItems.OPAL_BLOCK_ITEM.get());
        output.accept(ModItems.BUDDING_OPAL_ITEM.get());
        output.accept(ModItems.SMALL_OPAL_BUD_ITEM.get());
        output.accept(ModItems.MEDIUM_OPAL_BUD_ITEM.get());
        output.accept(ModItems.LARGE_OPAL_BUD_ITEM.get());
        output.accept(ModItems.OPAL_CLUSTER_ITEM.get());
        output.accept(ModItems.OPAL_SHARD.get());
        output.accept(ModItems.TOPAZ_BLOCK_ITEM.get());
        output.accept(ModItems.BUDDING_TOPAZ_ITEM.get());
        output.accept(ModItems.SMALL_TOPAZ_BUD_ITEM.get());
        output.accept(ModItems.MEDIUM_TOPAZ_BUD_ITEM.get());
        output.accept(ModItems.LARGE_TOPAZ_BUD_ITEM.get());
        output.accept(ModItems.TOPAZ_CLUSTER_ITEM.get());
        output.accept(ModItems.TOPAZ_SHARD.get());
        output.accept(ModItems.BERYL_BLOCK_ITEM.get());
        output.accept(ModItems.BUDDING_BERYL_ITEM.get());
        output.accept(ModItems.SMALL_BERYL_BUD_ITEM.get());
        output.accept(ModItems.MEDIUM_BERYL_BUD_ITEM.get());
        output.accept(ModItems.LARGE_BERYL_BUD_ITEM.get());
        output.accept(ModItems.BERYL_CLUSTER_ITEM.get());
        output.accept(ModItems.BERYL_SHARD.get());

        // Tables & débris (worldgen / progression)
        output.accept(ModItems.UPGRADE_TABLE_ITEM.get());
        output.accept(ModItems.UPGRADE_PLATE.get());
        output.accept(ModItems.ARMOR_UPGRADE.get());
        output.accept(ModItems.DISCRETION_UPGRADE.get());
        output.accept(ModItems.FROST_WALK_UPGRADE.get());
        output.accept(ModItems.HEALTH_UPGRADE.get());
        output.accept(ModItems.LUCK_UPGRADE.get());
        output.accept(ModItems.RANGE_UPGRADE.get());
        output.accept(ModItems.SPEED_UPGRADE.get());
        output.accept(ModItems.STEP_UPGRADE.get());
        output.accept(ModItems.STRENGHT_UPGRADE.get());
        output.accept(ModItems.DASH_UPGRADE.get());
        output.accept(ModItems.RAGE_UPGRADE.get());
        output.accept(ModItems.HEAL_UPGRADE.get());
        output.accept(ModItems.CRITICAL_UPGRADE.get());
        output.accept(ModItems.SWIM_UPGRADE.get());
        output.accept(ModItems.CAMOUFLAGE_UPGRADE.get());
        output.accept(ModItems.FOSSIL_DEBRIS_ITEM.get());
        output.accept(ModItems.FOSSIL_SCRAP.get());
        output.accept(ModItems.FORGOTTEN_DEBRIS_ITEM.get());
        output.accept(ModItems.HOLLOW_GRASS_ITEM.get());
        output.accept(ModItems.END_SAND_ITEM.get());
        output.accept(ModItems.QUICKSAND_ITEM.get());
        output.accept(ModItems.END_ANCHOR_ITEM.get());
        output.accept(ModItems.VOID_TORCH_ITEM.get());
        output.accept(ModItems.VOID_LANTERN_ITEM.get());
        output.accept(ModItems.JAERYS_ITEM.get());
        output.accept(ModItems.PURPLE_BERRY.get());
        output.accept(ModItems.GOLD_BERRY.get());
        output.accept(ModItems.SUPER_BONE.get());
        output.accept(ModItems.UNSTABLE_PEARL.get());

        // Bois Hollow (End)
        output.accept(ModItems.HOLLOW_SAPLING_ITEM.get());
        output.accept(ModItems.HOLLOW_LOG_ITEM.get());
        output.accept(ModItems.HOLLOW_WOOD_ITEM.get());
        output.accept(ModItems.STRIPPED_HOLLOW_LOG_ITEM.get());
        output.accept(ModItems.STRIPPED_HOLLOW_WOOD_ITEM.get());
        output.accept(ModItems.HOLLOW_PLANKS_ITEM.get());
        output.accept(ModItems.HOLLOW_STAIRS_ITEM.get());
        output.accept(ModItems.HOLLOW_SLAB_ITEM.get());
        output.accept(ModItems.HOLLOW_FENCE_ITEM.get());
        output.accept(ModItems.HOLLOW_FENCE_GATE_ITEM.get());
        output.accept(ModItems.HOLLOW_DOOR_ITEM.get());
        output.accept(ModItems.HOLLOW_TRAPDOOR_ITEM.get());
        output.accept(ModItems.HOLLOW_PRESSURE_PLATE_ITEM.get());
        output.accept(ModItems.HOLLOW_BUTTON_ITEM.get());
        output.accept(ModItems.HOLLOW_LEAVES_ITEM.get());
        output.accept(ModItems.HOLLOW_BARREL_ITEM.get());
        output.accept(ModItems.HOLLOW_BOOKSHELF_ITEM.get());
        output.accept(ModItems.FEEDER.get());

        // Bois Sunwood + plantes
        output.accept(ModItems.SUNWOOD_SAPLING_ITEM.get());
        output.accept(ModItems.SUNWOOD_LOG_ITEM.get());
        output.accept(ModItems.SUNWOOD_WOOD_ITEM.get());
        output.accept(ModItems.STRIPPED_SUNWOOD_LOG_ITEM.get());
        output.accept(ModItems.STRIPPED_SUNWOOD_WOOD_ITEM.get());
        output.accept(ModItems.SUNWOOD_PLANKS_ITEM.get());
        output.accept(ModItems.SUNWOOD_STAIRS_ITEM.get());
        output.accept(ModItems.SUNWOOD_SLAB_ITEM.get());
        output.accept(ModItems.SUNWOOD_FENCE_ITEM.get());
        output.accept(ModItems.SUNWOOD_FENCE_GATE_ITEM.get());
        output.accept(ModItems.SUNWOOD_DOOR_ITEM.get());
        output.accept(ModItems.SUNWOOD_TRAPDOOR_ITEM.get());
        output.accept(ModItems.SUNWOOD_PRESSURE_PLATE_ITEM.get());
        output.accept(ModItems.SUNWOOD_BUTTON_ITEM.get());
        output.accept(ModItems.SUNWOOD_SIGN_ITEM.get());
        output.accept(ModItems.SUNWOOD_HANGING_SIGN_ITEM.get());
        output.accept(ModItems.SUNWOOD_LEAVES_ITEM.get());
        output.accept(ModItems.SUNWOOD_BOAT.get());
        output.accept(ModItems.SUNWOOD_CHEST_BOAT.get());
        output.accept(ModItems.SUNBLOOM_ITEM.get());
        output.accept(ModItems.SURFACE_MOSS_ITEM.get());

        // Rubis (minerai pierre → abîmes → bloc → gemme)
        output.accept(ModItems.RUBY_ORE_ITEM.get());
        output.accept(ModItems.DEEPSLATE_RUBY_ORE_ITEM.get());
        output.accept(ModItems.RUBY_BLOCK_ITEM.get());
        output.accept(ModItems.RUBY.get());

        output.accept(ModItems.LIGHT_STONE_ITEM.get());
        output.accept(ModItems.LIGHT_SMOOTH_STONE_ITEM.get());
        output.accept(ModItems.CHISELED_LIGHT_SMOOTH_STONE_ITEM.get());
        output.accept(ModItems.LIGHT_SMOOTH_STONE_SLAB_ITEM.get());
        output.accept(ModItems.DARK_STONE_ITEM.get());
        output.accept(ModItems.DARK_SMOOTH_STONE_ITEM.get());
        output.accept(ModItems.CHISELED_DARK_SMOOTH_STONE_ITEM.get());
        output.accept(ModItems.DARK_SMOOTH_STONE_SLAB_ITEM.get());
        output.accept(ModItems.LIGHT_STONE_STAIRS_ITEM.get());
        output.accept(ModItems.LIGHT_STONE_SLAB_ITEM.get());
        output.accept(ModItems.LIGHT_STONE_WALL_ITEM.get());
        output.accept(ModItems.LIGHT_STONE_PRESSURE_PLATE_ITEM.get());
        output.accept(ModItems.LIGHT_STONE_BUTTON_ITEM.get());
        output.accept(ModItems.LIGHT_SMOOTH_STONE_STAIRS_ITEM.get());
        output.accept(ModItems.LIGHT_SMOOTH_STONE_WALL_ITEM.get());
        output.accept(ModItems.LIGHT_SMOOTH_STONE_PRESSURE_PLATE_ITEM.get());
        output.accept(ModItems.LIGHT_SMOOTH_STONE_BUTTON_ITEM.get());
        output.accept(ModItems.DARK_STONE_STAIRS_ITEM.get());
        output.accept(ModItems.DARK_STONE_SLAB_ITEM.get());
        output.accept(ModItems.DARK_STONE_WALL_ITEM.get());
        output.accept(ModItems.DARK_STONE_PRESSURE_PLATE_ITEM.get());
        output.accept(ModItems.DARK_STONE_BUTTON_ITEM.get());
        output.accept(ModItems.DARK_SMOOTH_STONE_STAIRS_ITEM.get());
        output.accept(ModItems.DARK_SMOOTH_STONE_WALL_ITEM.get());
        output.accept(ModItems.DARK_SMOOTH_STONE_PRESSURE_PLATE_ITEM.get());
        output.accept(ModItems.DARK_SMOOTH_STONE_BUTTON_ITEM.get());

        // Potions du mod (pour chaque effet : normale / splash / persistante)
        acceptPotionVariants(registries, output, ModPotions.SHRINK.getKey());
        acceptPotionVariants(registries, output, ModPotions.GROW.getKey());
        acceptPotionVariants(registries, output, ModPotions.VEIN_WHISPER.getKey());
        acceptPotionVariants(registries, output, ModPotions.STRONG_VEIN_WHISPER.getKey());

        // Livres enchantés (enchantements du mod)
        acceptEnchantedBook(registries, output, ModEnchantmentKeys.BLEEDING);
        acceptEnchantedBook(registries, output, ModEnchantmentKeys.LIFE_STEAL);
        acceptEnchantedBook(registries, output, ModEnchantmentKeys.TIMBER);
        acceptEnchantedBook(registries, output, ModEnchantmentKeys.CRUSHING_BLOW);
        acceptEnchantedBook(registries, output, ModEnchantmentKeys.FIRE_THORN);
        acceptEnchantedBook(registries, output, ModEnchantmentKeys.EXPLOSIVE_SHOT);
        acceptEnchantedBook(registries, output, ModEnchantmentKeys.EXCAVATOR);
        acceptEnchantedBook(registries, output, ModEnchantmentKeys.AUTO_SMELT);
        acceptEnchantedBook(registries, output, ModEnchantmentKeys.EMPATHIC_STRIKE);
        acceptEnchantedBook(registries, output, ModEnchantmentKeys.CURSE_OF_LAUNCHSTRIKE);
        acceptEnchantedBook(registries, output, ModEnchantmentKeys.SHIELD_CHARGE);
        // Enchantements faux (livres au niveau max pour l’aperçu créatif)
        acceptEnchantedBook(registries, output, ModEnchantmentKeys.WHIRLWIND);
        acceptEnchantedBook(registries, output, ModEnchantmentKeys.WIDE_SWEEP);
        acceptEnchantedBook(registries, output, ModEnchantmentKeys.REAPING);
        acceptEnchantedBook(registries, output, ModEnchantmentKeys.BRIAR_RING);

        // End, staffs & mobilité (staffs regroupés pour navigation propre)
        output.accept(ModItems.VOID_SHARD.get());
        output.accept(ModItems.UNSTABLE_PEARL.get());
        output.accept(ModItems.MELODY_HORN.get());
        output.accept(ModItems.WARDEN_TENDRIL.get());
        output.accept(ModItems.ECHO_STAFF.get());
        output.accept(ModItems.FLAME_STAFF.get());
        output.accept(ModItems.ICE_STAFF.get());
        output.accept(ModItems.END_STAFF.get());
        output.accept(ModItems.DRAGON_STAFF.get());
        output.accept(ModItems.WITHER_STAFF.get());
        output.accept(ModItems.BUILDER_WAND_GOLD.get());
        output.accept(ModItems.BUILDER_WAND_DIAMOND.get());
        output.accept(ModItems.BUILDER_WAND_EMERALD.get());
        output.accept(ModItems.BUILDER_WAND_NETHERITE.get());
        output.accept(ModItems.BUILDER_WAND_ENDERITE.get());
        output.accept(ModItems.POCKET_ENDER_CHEST.get());
        output.accept(ModItems.BLINK_ROD.get());
        output.accept(ModItems.END_GOLEM_HEART.get());
        output.accept(ModItems.DIAMOND_APPLE.get());
        output.accept(ModItems.ENCHANTED_DIAMOND_APPLE.get());

        // Coffre enchanté + fiole (stockage d'XP) + table d'infusion (déco / futur)
        output.accept(ModItems.ENCHANTED_CHEST_ITEM.get());
        output.accept(ModItems.FAKE_CHEST_ITEM.get());
        output.accept(ModItems.INFUSION_TABLE_ITEM.get());
        output.accept(ModItems.ENDSTONE_FURNACE_ITEM.get());
        output.accept(ModItems.FOUNDRY_ITEM.get());
        output.accept(ModItems.CRYSTAL.get());
        output.accept(ModItems.MORPH_CRYSTAL.get());
        output.accept(ModItems.CRACKED_CRYSTAL.get());
        output.accept(ModItems.CAPTURE_CRYSTAL.get());
        output.accept(ModItems.SEALED_EXPERIENCE_BOTTLE.get());
        output.accept(ModItems.UNKNOWN_BOOK.get());
        output.accept(ModItems.ENCHANTMENT_CODEX.get());
        output.accept(ModItems.EXPERIENCE_LIQUID_BUCKET.get());
        output.accept(ModItems.ENCHANTED_CHEST_UPGRADE.get());

        // Sacs à dos
        output.accept(ModItems.BACKPACK.get());
        output.accept(ModItems.IRON_BACKPACK.get());
        output.accept(ModItems.GOLD_BACKPACK.get());
        output.accept(ModItems.DIAMOND_BACKPACK.get());
        output.accept(ModItems.EMERALD_BACKPACK.get());

        // Undead Invasion / quêtes
        output.accept(ModItems.UNDEAD_BOTTLE_1.get());
        output.accept(ModItems.UNDEAD_BOTTLE_2.get());
        output.accept(ModItems.UNDEAD_BOTTLE_3.get());
        output.accept(ModItems.UNDEAD_BOTTLE_4.get());
        output.accept(ModItems.UNDEAD_BOTTLE_5.get());
        output.accept(ModItems.UNDEAD_BOTTLE_6.get());
        output.accept(ModItems.END_STORM_BOTTLE_1.get());
        output.accept(ModItems.END_STORM_BOTTLE_2.get());
        output.accept(ModItems.END_STORM_BOTTLE_3.get());
        output.accept(ModItems.END_STORM_BOTTLE_4.get());
        output.accept(EndStormManager.createEndStormBannerStack(registries));
        output.accept(ModItems.UNDEAD_TOTEM.get());
        output.accept(ModItems.BACK_TOTEM.get());
        output.accept(ModItems.UNDEAD_CROWN.get());
        output.accept(UndeadInvasionManager.createUndeadInvasionBannerStack(registries));
        output.accept(ModItems.QUEST_LOG.get());
        output.accept(ModItems.QUEST_TRADER_SPAWN_EGG.get());

        // Musique & créatures
        output.accept(ModItems.MUSIC_DISC_BEYOND_THE_ENDERMAN.get());
        output.accept(ModItems.DIAMOND_GOLEM_SPAWN_EGG.get());
        output.accept(ModItems.CHARGED_CREEPER_SPAWN_EGG.get());
        output.accept(ModItems.BLINK_SPAWN_EGG.get());
        output.accept(ModItems.END_GOLEM_SPAWN_EGG.get());
        output.accept(ModItems.ENDLING_SPAWN_EGG.get());
        output.accept(ModItems.RADIANT_SLIME_SPAWN_EGG.get());
        output.accept(ModItems.SKELETON_VILLAGER_SPAWN_EGG.get());
        output.accept(ModItems.SOUL_SKELETON_VILLAGER_SPAWN_EGG.get());
        output.accept(ModItems.UNDEAD_ILLAGER_SPAWN_EGG.get());
        output.accept(ModItems.VLINX_SPAWN_EGG.get());
        output.accept(ModItems.MIMIC_CHEST_SPAWN_EGG.get());
        output.accept(ModItems.DUMMY_ITEM.get());
        output.accept(ModItems.SKELETON_VILLAGER_SKULL_ITEM.get());
        output.accept(ModItems.ILLUSIONER_SPAWN_EGG.get());

        // Chapeaux / coiffes de villageois (même série)
        output.accept(ModItems.VILLAGER_HAT_BUTCHER.get());
        output.accept(ModItems.VILLAGER_HAT_LIBRARIAN.get());
        output.accept(ModItems.VILLAGER_HAT_WEAPONSMITH.get());
        output.accept(ModItems.VILLAGER_HAT_SHEPHERD.get());
        output.accept(ModItems.VILLAGER_HAT_FISHERMAN.get());
        output.accept(ModItems.VILLAGER_HAT_CARTOGRAPHER.get());
        output.accept(ModItems.VILLAGER_HAT_ARMORER.get());
        output.accept(ModItems.VILLAGER_HAT_FARMER.get());
        output.accept(ModItems.VILLAGER_HAT_WITCH.get());
    }

    private static void acceptPotionVariants(HolderLookup.Provider registries, CreativeModeTab.Output out, ResourceKey<Potion> key) {
        var pot = registries.lookupOrThrow(Registries.POTION).getOrThrow(key);
        out.accept(PotionContents.createItemStack(Items.POTION, pot));
        out.accept(PotionContents.createItemStack(Items.SPLASH_POTION, pot));
        out.accept(PotionContents.createItemStack(Items.LINGERING_POTION, pot));
    }

    private static ItemStack createSilwoxHead() {
        ItemStack stack = PlayerHeadProfileUtil.createPlayerHead("SILWOX", null);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("SILWOX"));
        return stack;
    }

    private static void acceptEnchantedBook(HolderLookup.Provider registries, CreativeModeTab.Output out, ResourceKey<Enchantment> key) {
        var ench = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mut.set(ench, ench.value().getMaxLevel());
        book.set(DataComponents.STORED_ENCHANTMENTS, mut.toImmutable());
        out.accept(book);
    }

    private static boolean isBmcModPotionStack(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        Optional<Holder<Potion>> pot = contents.potion();
        if (pot.isEmpty()) {
            return false;
        }
        return pot.get().unwrapKey().map(k -> MODID.equals(k.location().getNamespace())).orElse(false);
    }
}
