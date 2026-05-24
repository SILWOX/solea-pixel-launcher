package com.soleapixel.bmcmod.registry;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.boreal.BorealEquipment;
import com.soleapixel.bmcmod.enderite.EnderiteEquipment;
import com.soleapixel.bmcmod.copper.CopperEquipment;
import com.soleapixel.bmcmod.emerald.EmeraldEquipment;
import com.soleapixel.bmcmod.shulker.ShulkerEquipment;
import com.soleapixel.bmcmod.item.BmcShieldItem;
import com.soleapixel.bmcmod.item.DiamondAppleItem;
import com.soleapixel.bmcmod.item.EnchantmentCodexItem;
import com.soleapixel.bmcmod.item.DummyItem;
import com.soleapixel.bmcmod.item.EchoStaffItem;
import com.soleapixel.bmcmod.item.FlameStaffItem;
import com.soleapixel.bmcmod.item.IceStaffItem;
import com.soleapixel.bmcmod.item.DragonStaffItem;
import com.soleapixel.bmcmod.item.EndStaffItem;
import com.soleapixel.bmcmod.item.WitherStaffItem;
import com.soleapixel.bmcmod.item.PocketEnderChestItem;
import com.soleapixel.bmcmod.item.QuestLogItem;
import com.soleapixel.bmcmod.item.UnknownBookItem;
import com.soleapixel.bmcmod.item.BackTotemItem;
import com.soleapixel.bmcmod.item.UndeadTotemItem;
import com.soleapixel.bmcmod.item.SealedExperienceBottleItem;
import com.soleapixel.bmcmod.item.EndStormBottleItem;
import com.soleapixel.bmcmod.item.BuilderWandItem;
import com.soleapixel.bmcmod.item.BackpackItem;
import com.soleapixel.bmcmod.item.CaptureCrystalItem;
import com.soleapixel.bmcmod.item.ChargedCreeperSpawnEggItem;
import com.soleapixel.bmcmod.item.CrystalItem;
import com.soleapixel.bmcmod.item.CrackedCrystalItem;
import com.soleapixel.bmcmod.item.LightningBowItem;
import com.soleapixel.bmcmod.item.UpgradeBowItem;
import com.soleapixel.bmcmod.item.MorphCrystalItem;
import com.soleapixel.bmcmod.item.MelodyHornItem;
import com.soleapixel.bmcmod.item.ScytheItem;
import com.soleapixel.bmcmod.item.SuperBoneItem;
import com.soleapixel.bmcmod.item.SkeletonVillagerSpawnEggItem;
import com.soleapixel.bmcmod.item.UndeadBottleItem;
import com.soleapixel.bmcmod.item.UnstablePearlItem;
import com.soleapixel.bmcmod.item.VoidShardItem;
import com.soleapixel.bmcmod.item.sunwood.SunwoodBoatItem;
import com.soleapixel.bmcmod.item.PurpleBerryItem;
import com.soleapixel.bmcmod.item.QuiverContents;
import com.soleapixel.bmcmod.item.QuiverItem;
import com.soleapixel.bmcmod.obsidian.ObsidianEquipment;
import com.soleapixel.bmcmod.sky.SkyEquipment;
import com.soleapixel.bmcmod.equipment.EmeraldToolTier;
import com.soleapixel.bmcmod.equipment.EnderiteToolTier;
import com.soleapixel.bmcmod.equipment.BorealToolTier;
import com.soleapixel.bmcmod.item.builderwand.BuilderWandTier;
import com.soleapixel.bmcmod.registry.ModDataComponents;
import com.soleapixel.bmcmod.registry.ModBlocks;
import com.soleapixel.bmcmod.registry.ModEntities;
import com.soleapixel.bmcmod.registry.ModFluids;
import com.soleapixel.bmcmod.villager.VillagerHatEquipment;
import com.soleapixel.bmcmod.rarity.BmcModRarity;
import com.soleapixel.bmcmod.rarity.BmcRarityBannerItem;
import com.soleapixel.bmcmod.rarity.RarityStickItem;

import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Enregistrement central des objets du mod (un seul {@link DeferredRegister.Items}) pour éviter les doubles enregistrements.
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BmcMod.MODID);

    /** Plus nourrissant que les baies sucrées vanilla (2) ; régénération courte à la consommation. */
    private static final FoodProperties PURPLE_BERRY_FOOD = new FoodProperties.Builder()
            .nutrition(3)
            .saturationModifier(0.18f)
            .alwaysEdible()
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 50, 0), 1.0f)
            .build();

    /**
     * Meilleure carotte d’or vanilla (6 / 1,2) : faim + saturation élevées pour récompenser le farm de baies violettes.
     */
    private static final FoodProperties GOLD_BERRY_FOOD = new FoodProperties.Builder()
            .nutrition(8)
            .saturationModifier(1.55f)
            .alwaysEdible()
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 35, 0), 0.35f)
            .build();

    /** Tous les sacs à dos : insertions, crafts datapack ({@code bmcmod:backpack}). */
    public static final TagKey<Item> BACKPACK_TAG = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "backpack"));

    /** Référence du morceau enregistré dans {@code data/bmcmod/jukebox_song/beyond_the_enderman.json}. */
    public static final ResourceKey<JukeboxSong> JUKEBOX_SONG_BEYOND_THE_ENDERMAN = ResourceKey.create(
            Registries.JUKEBOX_SONG,
            ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "beyond_the_enderman"));

    public static final DeferredItem<Item> DIAMOND_GOLEM_SPAWN_EGG = ITEMS.register("diamond_golem_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.DIAMOND_GOLEM, 0xB8E9FF, 0x1E78C8, new Item.Properties()));
    public static final DeferredItem<Item> DUMMY_ITEM = ITEMS.register("dummy",
            () -> new DummyItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CHARGED_CREEPER_SPAWN_EGG = ITEMS.register("charged_creeper_spawn_egg",
            () -> new ChargedCreeperSpawnEggItem(new Item.Properties()));

    /** Entité vanilla : l’illusionniste n’a pas d’œuf dans le jeu de base. */
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> ILLUSIONER_SPAWN_EGG = ITEMS.register("illusioner_spawn_egg",
            () -> new SpawnEggItem(
                    (EntityType<? extends Mob>) EntityType.ILLUSIONER,
                    0x395174,
                    0x8C8C8C,
                    new Item.Properties()));

    public static final DeferredItem<Item> RARITY_STICK_UNCOMMON = ITEMS.register("rarity_stick_uncommon",
            () -> new RarityStickItem(new Item.Properties().stacksTo(64), BmcModRarity.UNCOMMON));
    public static final DeferredItem<Item> RARITY_STICK_COMMON = ITEMS.register("rarity_stick_common",
            () -> new RarityStickItem(new Item.Properties().stacksTo(64), BmcModRarity.COMMON));
    public static final DeferredItem<Item> RARITY_STICK_RARE = ITEMS.register("rarity_stick_rare",
            () -> new RarityStickItem(new Item.Properties().stacksTo(64), BmcModRarity.RARE));
    public static final DeferredItem<Item> RARITY_STICK_EPIC = ITEMS.register("rarity_stick_epic",
            () -> new RarityStickItem(new Item.Properties().stacksTo(64), BmcModRarity.EPIC));
    public static final DeferredItem<Item> RARITY_STICK_LEGENDARY = ITEMS.register("rarity_stick_legendary",
            () -> new RarityStickItem(new Item.Properties().stacksTo(64), BmcModRarity.LEGENDARY));
    public static final DeferredItem<Item> RARITY_STICK_EXOTIC = ITEMS.register("rarity_stick_exotic",
            () -> new RarityStickItem(new Item.Properties().stacksTo(64), BmcModRarity.EXOTIC));
    public static final DeferredItem<Item> RARITY_STICK_MYTHIC = ITEMS.register("rarity_stick_mythic",
            () -> new RarityStickItem(new Item.Properties().stacksTo(64), BmcModRarity.MYTHIC));
    public static final DeferredItem<Item> RARITY_STICK_FRAGMENTED = ITEMS.register("rarity_stick_fragmented",
            () -> new RarityStickItem(new Item.Properties().stacksTo(64), BmcModRarity.FRAGMENTED));

    public static final DeferredItem<Item> BACKPACK = ITEMS.register("backpack",
            () -> new BackpackItem(backpackProperties(Rarity.UNCOMMON), 1));
    public static final DeferredItem<Item> IRON_BACKPACK = ITEMS.register("iron_backpack",
            () -> new BackpackItem(backpackProperties(Rarity.COMMON), 2));
    public static final DeferredItem<Item> GOLD_BACKPACK = ITEMS.register("gold_backpack",
            () -> new BackpackItem(backpackProperties(Rarity.COMMON), 3));
    public static final DeferredItem<Item> DIAMOND_BACKPACK = ITEMS.register("diamond_backpack",
            () -> new BackpackItem(backpackProperties(Rarity.RARE), 5));
    public static final DeferredItem<Item> EMERALD_BACKPACK = ITEMS.register("emerald_backpack",
            () -> new BackpackItem(backpackProperties(Rarity.EPIC), 7));

    public static final DeferredItem<BlockItem> FEEDER = ITEMS.registerSimpleBlockItem("feeder", ModBlocks.FEEDER);

    public static final DeferredItem<Item> QUIVER = ITEMS.register("quiver",
            () -> new QuiverItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)
                    .component(ModDataComponents.QUIVER_CONTENTS.get(), QuiverContents.EMPTY)));

    public static final DeferredItem<Item> COPPER_NUGGET = ITEMS.registerSimpleItem("copper_nugget", copperProps());
    public static final DeferredItem<SwordItem> COPPER_SWORD = ITEMS.register("copper_sword",
            () -> new SwordItem(CopperEquipment.COPPER_TIER, copperGearProps().attributes(SwordItem.createAttributes(CopperEquipment.COPPER_TIER, 3, -2.4F))));
    public static final DeferredItem<PickaxeItem> COPPER_PICKAXE = ITEMS.register("copper_pickaxe",
            () -> new PickaxeItem(CopperEquipment.COPPER_TIER, copperGearProps().attributes(DiggerItem.createAttributes(CopperEquipment.COPPER_TIER, 1.0F, -2.8F))));
    public static final DeferredItem<ShovelItem> COPPER_SHOVEL = ITEMS.register("copper_shovel",
            () -> new ShovelItem(CopperEquipment.COPPER_TIER, copperGearProps().attributes(DiggerItem.createAttributes(CopperEquipment.COPPER_TIER, 1.5F, -3.0F))));
    public static final DeferredItem<AxeItem> COPPER_AXE = ITEMS.register("copper_axe",
            () -> new AxeItem(CopperEquipment.COPPER_TIER, copperGearProps().attributes(DiggerItem.createAttributes(CopperEquipment.COPPER_TIER, 5.0F, -3.0F))));
    public static final DeferredItem<HoeItem> COPPER_HOE = ITEMS.register("copper_hoe",
            () -> new HoeItem(CopperEquipment.COPPER_TIER, copperGearProps().attributes(DiggerItem.createAttributes(CopperEquipment.COPPER_TIER, 0.0F, -3.0F))));

    /** Faux : labour / récolte en zone ; dégâts légers + lent (~0,5 coups/s) ; tourbillon via maintien clic (réseau). */
    private static final float SCYTHE_ATK_SPEED = -3.5F;

    public static final DeferredItem<ScytheItem> IRON_SCYTHE = ITEMS.register("iron_scythe",
            () -> new ScytheItem(ScytheItem.ScytheTier.IRON,
                    new Item.Properties().stacksTo(1)
                            .attributes(DiggerItem.createAttributes(Tiers.IRON, 0.5F, SCYTHE_ATK_SPEED))
                            .durability(Tiers.IRON.getUses())
                            .component(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)));
    public static final DeferredItem<ScytheItem> GOLDEN_SCYTHE = ITEMS.register("golden_scythe",
            () -> new ScytheItem(ScytheItem.ScytheTier.GOLD,
                    new Item.Properties().stacksTo(1)
                            .attributes(DiggerItem.createAttributes(Tiers.GOLD, 0.5F, SCYTHE_ATK_SPEED))
                            .durability(128)
                            .component(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)));
    public static final DeferredItem<ScytheItem> DIAMOND_SCYTHE = ITEMS.register("diamond_scythe",
            () -> new ScytheItem(ScytheItem.ScytheTier.DIAMOND,
                    new Item.Properties().stacksTo(1)
                            .attributes(DiggerItem.createAttributes(Tiers.DIAMOND, 0.75F, SCYTHE_ATK_SPEED))
                            .durability(Tiers.DIAMOND.getUses())
                            .component(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)));
    public static final DeferredItem<ScytheItem> EMERALD_SCYTHE = ITEMS.register("emerald_scythe",
            () -> new ScytheItem(ScytheItem.ScytheTier.EMERALD,
                    emeraldGearProps()
                            .attributes(DiggerItem.createAttributes(EmeraldEquipment.EMERALD_TIER, 0.85F, SCYTHE_ATK_SPEED))
                            .durability(EmeraldToolTier.INSTANCE.getUses())));
    public static final DeferredItem<ScytheItem> NETHERITE_SCYTHE = ITEMS.register("netherite_scythe",
            () -> new ScytheItem(ScytheItem.ScytheTier.NETHERITE,
                    new Item.Properties().stacksTo(1)
                            .attributes(DiggerItem.createAttributes(Tiers.NETHERITE, 1.0F, SCYTHE_ATK_SPEED))
                            .durability(Tiers.NETHERITE.getUses())
                            .component(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)));
    public static final DeferredItem<ScytheItem> ENDERITE_SCYTHE = ITEMS.register("enderite_scythe",
            () -> new ScytheItem(ScytheItem.ScytheTier.ENDERITE,
                    enderiteGearProps()
                            .attributes(DiggerItem.createAttributes(EnderiteToolTier.INSTANCE, 1.25F, SCYTHE_ATK_SPEED))
                            .durability(EnderiteToolTier.INSTANCE.getUses())));
    public static final DeferredItem<ScytheItem> BOREAL_SCYTHE = ITEMS.register("boreal_scythe",
            () -> new ScytheItem(ScytheItem.ScytheTier.BOREAL,
                    borealGearProps()
                            .attributes(DiggerItem.createAttributes(BorealToolTier.INSTANCE, 1.45F, SCYTHE_ATK_SPEED))
                            .durability(BorealToolTier.INSTANCE.getUses())));

    public static final DeferredItem<ArmorItem> COPPER_HELMET = ITEMS.register("copper_helmet",
            () -> new ArmorItem(CopperEquipment.COPPER_ARMOR_MATERIAL, ArmorItem.Type.HELMET, copperGearProps()));
    public static final DeferredItem<ArmorItem> COPPER_CHESTPLATE = ITEMS.register("copper_chestplate",
            () -> new ArmorItem(CopperEquipment.COPPER_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, copperGearProps()));
    public static final DeferredItem<ArmorItem> COPPER_LEGGINGS = ITEMS.register("copper_leggings",
            () -> new ArmorItem(CopperEquipment.COPPER_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, copperGearProps()));
    public static final DeferredItem<ArmorItem> COPPER_BOOTS = ITEMS.register("copper_boots",
            () -> new ArmorItem(CopperEquipment.COPPER_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, copperGearProps()));

    public static final DeferredItem<SwordItem> EMERALD_SWORD = ITEMS.register("emerald_sword",
            () -> new SwordItem(EmeraldEquipment.EMERALD_TIER, emeraldGearProps().attributes(SwordItem.createAttributes(EmeraldEquipment.EMERALD_TIER, 3, -2.4F))));
    public static final DeferredItem<PickaxeItem> EMERALD_PICKAXE = ITEMS.register("emerald_pickaxe",
            () -> new PickaxeItem(EmeraldEquipment.EMERALD_TIER, emeraldGearProps().attributes(DiggerItem.createAttributes(EmeraldEquipment.EMERALD_TIER, 1.0F, -2.8F))));
    public static final DeferredItem<ShovelItem> EMERALD_SHOVEL = ITEMS.register("emerald_shovel",
            () -> new ShovelItem(EmeraldEquipment.EMERALD_TIER, emeraldGearProps().attributes(DiggerItem.createAttributes(EmeraldEquipment.EMERALD_TIER, 1.5F, -3.0F))));
    public static final DeferredItem<AxeItem> EMERALD_AXE = ITEMS.register("emerald_axe",
            () -> new AxeItem(EmeraldEquipment.EMERALD_TIER, emeraldGearProps().attributes(DiggerItem.createAttributes(EmeraldEquipment.EMERALD_TIER, 5.0F, -3.0F))));
    public static final DeferredItem<HoeItem> EMERALD_HOE = ITEMS.register("emerald_hoe",
            () -> new HoeItem(EmeraldEquipment.EMERALD_TIER, emeraldGearProps().attributes(DiggerItem.createAttributes(EmeraldEquipment.EMERALD_TIER, 0.0F, -3.0F))));

    public static final DeferredItem<ArmorItem> EMERALD_HELMET = ITEMS.register("emerald_helmet",
            () -> new ArmorItem(EmeraldEquipment.EMERALD_ARMOR_MATERIAL, ArmorItem.Type.HELMET, emeraldGearProps()));
    public static final DeferredItem<ArmorItem> EMERALD_CHESTPLATE = ITEMS.register("emerald_chestplate",
            () -> new ArmorItem(EmeraldEquipment.EMERALD_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, emeraldGearProps()));
    public static final DeferredItem<ArmorItem> EMERALD_LEGGINGS = ITEMS.register("emerald_leggings",
            () -> new ArmorItem(EmeraldEquipment.EMERALD_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, emeraldGearProps()));
    public static final DeferredItem<ArmorItem> EMERALD_BOOTS = ITEMS.register("emerald_boots",
            () -> new ArmorItem(EmeraldEquipment.EMERALD_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, emeraldGearProps()));

    /** Infusion : double saut, quasi pas de chute, vitesse, pas hauteur 1, incassable (voir {@link com.soleapixel.bmcmod.gameplay.SkyBootsGameplay}). */
    public static final DeferredItem<ArmorItem> SKY_BOOTS = ITEMS.register("sky_boots",
            () -> new ArmorItem(SkyEquipment.SKY_BOOTS_MATERIAL, ArmorItem.Type.BOOTS, skyBootsProps()));

    public static final DeferredItem<ArmorItem> SHULKER_HELMET = ITEMS.register("shulker_helmet",
            () -> new ArmorItem(ShulkerEquipment.SHULKER_ARMOR_MATERIAL, ArmorItem.Type.HELMET, shulkerGearProps()));
    public static final DeferredItem<ArmorItem> SHULKER_CHESTPLATE = ITEMS.register("shulker_chestplate",
            () -> new ArmorItem(ShulkerEquipment.SHULKER_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, shulkerGearProps()));
    public static final DeferredItem<ArmorItem> SHULKER_LEGGINGS = ITEMS.register("shulker_leggings",
            () -> new ArmorItem(ShulkerEquipment.SHULKER_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, shulkerGearProps()));
    public static final DeferredItem<ArmorItem> SHULKER_BOOTS = ITEMS.register("shulker_boots",
            () -> new ArmorItem(ShulkerEquipment.SHULKER_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, shulkerGearProps()));

    public static final DeferredItem<ArmorItem> VILLAGER_HAT_BUTCHER = ITEMS.register("butcher_headband",
            () -> new ArmorItem(VillagerHatEquipment.BUTCHER, ArmorItem.Type.HELMET, villagerHatProps()));
    public static final DeferredItem<ArmorItem> VILLAGER_HAT_LIBRARIAN = ITEMS.register("librarian_hat",
            () -> new ArmorItem(VillagerHatEquipment.LIBRARIAN, ArmorItem.Type.HELMET, villagerHatProps()));
    public static final DeferredItem<ArmorItem> VILLAGER_HAT_WEAPONSMITH = ITEMS.register("weaponsmith_eyepatch",
            () -> new ArmorItem(VillagerHatEquipment.WEAPONSMITH, ArmorItem.Type.HELMET, villagerHatProps()));
    public static final DeferredItem<ArmorItem> VILLAGER_HAT_SHEPHERD = ITEMS.register("shepherd_hat",
            () -> new ArmorItem(VillagerHatEquipment.SHEPHERD, ArmorItem.Type.HELMET, villagerHatProps()));
    public static final DeferredItem<ArmorItem> VILLAGER_HAT_FISHERMAN = ITEMS.register("fisherman_hat",
            () -> new ArmorItem(VillagerHatEquipment.FISHERMAN, ArmorItem.Type.HELMET, villagerHatProps()));
    public static final DeferredItem<ArmorItem> VILLAGER_HAT_CARTOGRAPHER = ITEMS.register("cartographer_monocle",
            () -> new ArmorItem(VillagerHatEquipment.CARTOGRAPHER, ArmorItem.Type.HELMET, villagerHatProps()));
    public static final DeferredItem<ArmorItem> VILLAGER_HAT_ARMORER = ITEMS.register("armorer_goggles",
            () -> new ArmorItem(VillagerHatEquipment.ARMORER, ArmorItem.Type.HELMET, villagerHatProps()));
    public static final DeferredItem<ArmorItem> VILLAGER_HAT_FARMER = ITEMS.register("farmer_hat",
            () -> new ArmorItem(VillagerHatEquipment.FARMER, ArmorItem.Type.HELMET, villagerHatProps()));
    public static final DeferredItem<ArmorItem> VILLAGER_HAT_WITCH = ITEMS.register("witch_hat",
            () -> new ArmorItem(VillagerHatEquipment.WITCH, ArmorItem.Type.HELMET, villagerHatProps()));
    public static final DeferredItem<ArmorItem> UNDEAD_CROWN = ITEMS.register("undead_crown",
            () -> new ArmorItem(VillagerHatEquipment.UNDEAD_CROWN, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<BlockItem> NEBRITH_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("nebrith_block", ModBlocks.NEBRITH_BLOCK);
    public static final DeferredItem<BlockItem> BUDDING_NEBRITH_ITEM = ITEMS.registerSimpleBlockItem("budding_nebrith", ModBlocks.BUDDING_NEBRITH);
    public static final DeferredItem<BlockItem> SMALL_NEBRITH_BUD_ITEM = ITEMS.registerSimpleBlockItem("small_nebrith_bud", ModBlocks.SMALL_NEBRITH_BUD);
    public static final DeferredItem<BlockItem> MEDIUM_NEBRITH_BUD_ITEM = ITEMS.registerSimpleBlockItem("medium_nebrith_bud", ModBlocks.MEDIUM_NEBRITH_BUD);
    public static final DeferredItem<BlockItem> LARGE_NEBRITH_BUD_ITEM = ITEMS.registerSimpleBlockItem("large_nebrith_bud", ModBlocks.LARGE_NEBRITH_BUD);
    public static final DeferredItem<BlockItem> NEBRITH_CLUSTER_ITEM = ITEMS.registerSimpleBlockItem("nebrith_cluster", ModBlocks.NEBRITH_CLUSTER);
    public static final DeferredItem<BlockItem> OPAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("opal_block", ModBlocks.OPAL_BLOCK);
    public static final DeferredItem<BlockItem> BUDDING_OPAL_ITEM = ITEMS.registerSimpleBlockItem("budding_opal", ModBlocks.BUDDING_OPAL);
    public static final DeferredItem<BlockItem> SMALL_OPAL_BUD_ITEM = ITEMS.registerSimpleBlockItem("small_opal_bud", ModBlocks.SMALL_OPAL_BUD);
    public static final DeferredItem<BlockItem> MEDIUM_OPAL_BUD_ITEM = ITEMS.registerSimpleBlockItem("medium_opal_bud", ModBlocks.MEDIUM_OPAL_BUD);
    public static final DeferredItem<BlockItem> LARGE_OPAL_BUD_ITEM = ITEMS.registerSimpleBlockItem("large_opal_bud", ModBlocks.LARGE_OPAL_BUD);
    public static final DeferredItem<BlockItem> OPAL_CLUSTER_ITEM = ITEMS.registerSimpleBlockItem("opal_cluster", ModBlocks.OPAL_CLUSTER);
    public static final DeferredItem<Item> OPAL_SHARD = ITEMS.register("opal_shard",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<BlockItem> TOPAZ_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("topaz_block", ModBlocks.TOPAZ_BLOCK);
    public static final DeferredItem<BlockItem> BUDDING_TOPAZ_ITEM = ITEMS.registerSimpleBlockItem("budding_topaz", ModBlocks.BUDDING_TOPAZ);
    public static final DeferredItem<BlockItem> SMALL_TOPAZ_BUD_ITEM = ITEMS.registerSimpleBlockItem("small_topaz_bud", ModBlocks.SMALL_TOPAZ_BUD);
    public static final DeferredItem<BlockItem> MEDIUM_TOPAZ_BUD_ITEM = ITEMS.registerSimpleBlockItem("medium_topaz_bud", ModBlocks.MEDIUM_TOPAZ_BUD);
    public static final DeferredItem<BlockItem> LARGE_TOPAZ_BUD_ITEM = ITEMS.registerSimpleBlockItem("large_topaz_bud", ModBlocks.LARGE_TOPAZ_BUD);
    public static final DeferredItem<BlockItem> TOPAZ_CLUSTER_ITEM = ITEMS.registerSimpleBlockItem("topaz_cluster", ModBlocks.TOPAZ_CLUSTER);
    public static final DeferredItem<Item> TOPAZ_SHARD = ITEMS.register("topaz_shard",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<BlockItem> BERYL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("beryl_block", ModBlocks.BERYL_BLOCK);
    public static final DeferredItem<BlockItem> BUDDING_BERYL_ITEM = ITEMS.registerSimpleBlockItem("budding_beryl", ModBlocks.BUDDING_BERYL);
    public static final DeferredItem<BlockItem> SMALL_BERYL_BUD_ITEM = ITEMS.registerSimpleBlockItem("small_beryl_bud", ModBlocks.SMALL_BERYL_BUD);
    public static final DeferredItem<BlockItem> MEDIUM_BERYL_BUD_ITEM = ITEMS.registerSimpleBlockItem("medium_beryl_bud", ModBlocks.MEDIUM_BERYL_BUD);
    public static final DeferredItem<BlockItem> LARGE_BERYL_BUD_ITEM = ITEMS.registerSimpleBlockItem("large_beryl_bud", ModBlocks.LARGE_BERYL_BUD);
    public static final DeferredItem<BlockItem> BERYL_CLUSTER_ITEM = ITEMS.registerSimpleBlockItem("beryl_cluster", ModBlocks.BERYL_CLUSTER);
    public static final DeferredItem<Item> BERYL_SHARD = ITEMS.register("beryl_shard",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<BlockItem> UPGRADE_TABLE_ITEM = ITEMS.registerSimpleBlockItem("upgrade_table", ModBlocks.UPGRADE_TABLE);
    public static final DeferredItem<Item> UPGRADE_PLATE = ITEMS.register("upgrade_plate",
            () -> new Item(new Item.Properties().rarity(Rarity.COMMON)));
    public static final DeferredItem<Item> ARMOR_UPGRADE = ITEMS.register("armor_upgrade",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> DISCRETION_UPGRADE = ITEMS.register("discretion_upgrade",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> FROST_WALK_UPGRADE = ITEMS.register("frost_walk_upgrade",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> HEALTH_UPGRADE = ITEMS.register("health_upgrade",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> LUCK_UPGRADE = ITEMS.register("luck_upgrade",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> RANGE_UPGRADE = ITEMS.register("range_upgrade",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SPEED_UPGRADE = ITEMS.register("speed_upgrade",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> STEP_UPGRADE = ITEMS.register("step_upgrade",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> STRENGHT_UPGRADE = ITEMS.register("strenght_upgrade",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> DASH_UPGRADE = ITEMS.register("dash_upgrade",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> RAGE_UPGRADE = ITEMS.register("rage_upgrade",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> HEAL_UPGRADE = ITEMS.register("heal_upgrade",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> CRITICAL_UPGRADE = ITEMS.register("critical_upgrade",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SWIM_UPGRADE = ITEMS.register("swim_upgrade",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> CAMOUFLAGE_UPGRADE = ITEMS.register("camouflage_upgrade",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<BlockItem> FOSSIL_DEBRIS_ITEM = ITEMS.register("fossil_debris",
            () -> new BlockItem(ModBlocks.FOSSIL_DEBRIS.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> RUBY = ITEMS.register("ruby",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredItem<BlockItem> RUBY_ORE_ITEM = ITEMS.register("ruby_ore",
            () -> new BlockItem(ModBlocks.RUBY_ORE.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<BlockItem> DEEPSLATE_RUBY_ORE_ITEM = ITEMS.register("deepslate_ruby_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_RUBY_ORE.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<BlockItem> RUBY_BLOCK_ITEM = ITEMS.register("ruby_block",
            () -> new BlockItem(ModBlocks.RUBY_BLOCK.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredItem<BlockItem> LIGHT_STONE_ITEM = ITEMS.registerSimpleBlockItem("light_stone", ModBlocks.LIGHT_STONE);
    public static final DeferredItem<BlockItem> LIGHT_SMOOTH_STONE_ITEM = ITEMS.registerSimpleBlockItem("light_smooth_stone", ModBlocks.LIGHT_SMOOTH_STONE);
    public static final DeferredItem<BlockItem> CHISELED_LIGHT_SMOOTH_STONE_ITEM = ITEMS.registerSimpleBlockItem("chiseled_light_smooth_stone", ModBlocks.CHISELED_LIGHT_SMOOTH_STONE);
    public static final DeferredItem<BlockItem> LIGHT_SMOOTH_STONE_SLAB_ITEM = ITEMS.registerSimpleBlockItem("light_smooth_stone_slab", ModBlocks.LIGHT_SMOOTH_STONE_SLAB);
    public static final DeferredItem<BlockItem> DARK_STONE_ITEM = ITEMS.registerSimpleBlockItem("dark_stone", ModBlocks.DARK_STONE);
    public static final DeferredItem<BlockItem> DARK_SMOOTH_STONE_ITEM = ITEMS.registerSimpleBlockItem("dark_smooth_stone", ModBlocks.DARK_SMOOTH_STONE);
    public static final DeferredItem<BlockItem> CHISELED_DARK_SMOOTH_STONE_ITEM = ITEMS.registerSimpleBlockItem("chiseled_dark_smooth_stone", ModBlocks.CHISELED_DARK_SMOOTH_STONE);
    public static final DeferredItem<BlockItem> DARK_SMOOTH_STONE_SLAB_ITEM = ITEMS.registerSimpleBlockItem("dark_smooth_stone_slab", ModBlocks.DARK_SMOOTH_STONE_SLAB);

    public static final DeferredItem<BlockItem> LIGHT_STONE_STAIRS_ITEM = ITEMS.registerSimpleBlockItem("light_stone_stairs", ModBlocks.LIGHT_STONE_STAIRS);
    public static final DeferredItem<BlockItem> LIGHT_STONE_SLAB_ITEM = ITEMS.registerSimpleBlockItem("light_stone_slab", ModBlocks.LIGHT_STONE_SLAB);
    public static final DeferredItem<BlockItem> LIGHT_STONE_WALL_ITEM = ITEMS.registerSimpleBlockItem("light_stone_wall", ModBlocks.LIGHT_STONE_WALL);
    public static final DeferredItem<BlockItem> LIGHT_STONE_PRESSURE_PLATE_ITEM = ITEMS.registerSimpleBlockItem("light_stone_pressure_plate", ModBlocks.LIGHT_STONE_PRESSURE_PLATE);
    public static final DeferredItem<BlockItem> LIGHT_STONE_BUTTON_ITEM = ITEMS.registerSimpleBlockItem("light_stone_button", ModBlocks.LIGHT_STONE_BUTTON);
    public static final DeferredItem<BlockItem> LIGHT_SMOOTH_STONE_STAIRS_ITEM = ITEMS.registerSimpleBlockItem("light_smooth_stone_stairs", ModBlocks.LIGHT_SMOOTH_STONE_STAIRS);
    public static final DeferredItem<BlockItem> LIGHT_SMOOTH_STONE_WALL_ITEM = ITEMS.registerSimpleBlockItem("light_smooth_stone_wall", ModBlocks.LIGHT_SMOOTH_STONE_WALL);
    public static final DeferredItem<BlockItem> LIGHT_SMOOTH_STONE_PRESSURE_PLATE_ITEM = ITEMS.registerSimpleBlockItem("light_smooth_stone_pressure_plate", ModBlocks.LIGHT_SMOOTH_STONE_PRESSURE_PLATE);
    public static final DeferredItem<BlockItem> LIGHT_SMOOTH_STONE_BUTTON_ITEM = ITEMS.registerSimpleBlockItem("light_smooth_stone_button", ModBlocks.LIGHT_SMOOTH_STONE_BUTTON);

    public static final DeferredItem<BlockItem> DARK_STONE_STAIRS_ITEM = ITEMS.registerSimpleBlockItem("dark_stone_stairs", ModBlocks.DARK_STONE_STAIRS);
    public static final DeferredItem<BlockItem> DARK_STONE_SLAB_ITEM = ITEMS.registerSimpleBlockItem("dark_stone_slab", ModBlocks.DARK_STONE_SLAB);
    public static final DeferredItem<BlockItem> DARK_STONE_WALL_ITEM = ITEMS.registerSimpleBlockItem("dark_stone_wall", ModBlocks.DARK_STONE_WALL);
    public static final DeferredItem<BlockItem> DARK_STONE_PRESSURE_PLATE_ITEM = ITEMS.registerSimpleBlockItem("dark_stone_pressure_plate", ModBlocks.DARK_STONE_PRESSURE_PLATE);
    public static final DeferredItem<BlockItem> DARK_STONE_BUTTON_ITEM = ITEMS.registerSimpleBlockItem("dark_stone_button", ModBlocks.DARK_STONE_BUTTON);
    public static final DeferredItem<BlockItem> DARK_SMOOTH_STONE_STAIRS_ITEM = ITEMS.registerSimpleBlockItem("dark_smooth_stone_stairs", ModBlocks.DARK_SMOOTH_STONE_STAIRS);
    public static final DeferredItem<BlockItem> DARK_SMOOTH_STONE_WALL_ITEM = ITEMS.registerSimpleBlockItem("dark_smooth_stone_wall", ModBlocks.DARK_SMOOTH_STONE_WALL);
    public static final DeferredItem<BlockItem> DARK_SMOOTH_STONE_PRESSURE_PLATE_ITEM = ITEMS.registerSimpleBlockItem("dark_smooth_stone_pressure_plate", ModBlocks.DARK_SMOOTH_STONE_PRESSURE_PLATE);
    public static final DeferredItem<BlockItem> DARK_SMOOTH_STONE_BUTTON_ITEM = ITEMS.registerSimpleBlockItem("dark_smooth_stone_button", ModBlocks.DARK_SMOOTH_STONE_BUTTON);

    public static final DeferredItem<BlockItem> FORGOTTEN_DEBRIS_ITEM = ITEMS.register("forgotten_debris",
            () -> new BlockItem(ModBlocks.FORGOTTEN_DEBRIS.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<BlockItem> HOLLOW_GRASS_ITEM = ITEMS.registerSimpleBlockItem("hollow_grass", ModBlocks.HOLLOW_GRASS);
    public static final DeferredItem<BlockItem> END_SAND_ITEM = ITEMS.registerSimpleBlockItem("end_sand", ModBlocks.END_SAND);
    public static final DeferredItem<BlockItem> QUICKSAND_ITEM = ITEMS.registerSimpleBlockItem("quicksand", ModBlocks.QUICKSAND);
    public static final DeferredItem<BlockItem> END_ANCHOR_ITEM = ITEMS.registerSimpleBlockItem("end_anchor", ModBlocks.END_ANCHOR);
    public static final DeferredItem<BlockItem> VOID_TORCH_ITEM = ITEMS.register("void_torch",
            () -> new StandingAndWallBlockItem(
                    ModBlocks.VOID_TORCH.get(),
                    ModBlocks.VOID_WALL_TORCH.get(),
                    new Item.Properties(),
                    Direction.DOWN));
    public static final DeferredItem<BlockItem> VOID_LANTERN_ITEM = ITEMS.registerSimpleBlockItem("void_lantern", ModBlocks.VOID_LANTERN);
    public static final DeferredItem<BlockItem> SKELETON_VILLAGER_SKULL_ITEM = ITEMS.register("skeleton_villager_skull",
            () -> new StandingAndWallBlockItem(
                    ModBlocks.SKELETON_VILLAGER_SKULL.get(),
                    ModBlocks.SKELETON_VILLAGER_WALL_SKULL.get(),
                    mobHeadProps(),
                    Direction.DOWN) {
                @Override
                public net.minecraft.world.InteractionResultHolder<ItemStack> use(
                        net.minecraft.world.level.Level level,
                        net.minecraft.world.entity.player.Player player,
                        net.minecraft.world.InteractionHand hand) {
                    ItemStack held = player.getItemInHand(hand);
                    if (player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty()) {
                        ItemStack one = held.copyWithCount(1);
                        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, one);
                        if (!player.getAbilities().instabuild) {
                            held.shrink(1);
                        }
                        return net.minecraft.world.InteractionResultHolder.sidedSuccess(held, level.isClientSide());
                    }
                    return super.use(level, player, hand);
                }

                @Override
                public net.minecraft.world.InteractionResult interactLivingEntity(
                        ItemStack stack,
                        net.minecraft.world.entity.player.Player player,
                        net.minecraft.world.entity.LivingEntity interactionTarget,
                        net.minecraft.world.InteractionHand usedHand) {
                    if (interactionTarget instanceof net.minecraft.world.entity.decoration.ArmorStand stand) {
                        ItemStack head = stand.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
                        if (head.isEmpty()) {
                            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, stack.copyWithCount(1));
                            if (!player.getAbilities().instabuild) {
                                stack.shrink(1);
                            }
                            return net.minecraft.world.InteractionResult.sidedSuccess(player.level().isClientSide);
                        }
                    }
                    return super.interactLivingEntity(stack, player, interactionTarget, usedHand);
                }

                @Override
                public boolean canEquip(
                        ItemStack stack,
                        net.minecraft.world.entity.EquipmentSlot slot,
                        net.minecraft.world.entity.LivingEntity entity) {
                    return slot == net.minecraft.world.entity.EquipmentSlot.HEAD || super.canEquip(stack, slot, entity);
                }
            });
    public static final DeferredItem<BlockItem> JAERYS_ITEM = ITEMS.registerSimpleBlockItem("jaerys", ModBlocks.JAERYS);

    public static final DeferredItem<PurpleBerryItem> PURPLE_BERRY = ITEMS.register("purple_berry",
            () -> new PurpleBerryItem(new Item.Properties().food(PURPLE_BERRY_FOOD)));

    public static final DeferredItem<Item> GOLD_BERRY = ITEMS.register("gold_berry",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC).food(GOLD_BERRY_FOOD)));

    /** Buff de combat 10 min pour loup apprivoisé (clic avec l’item). */
    public static final DeferredItem<SuperBoneItem> SUPER_BONE = ITEMS.register("super_bone",
            () -> new SuperBoneItem(SuperBoneItem.defaultProperties()));

    /** Perle instable : infusion ; TP cible / boomerang / chaos si retour sur le lanceur. */
    public static final DeferredItem<UnstablePearlItem> UNSTABLE_PEARL = ITEMS.register("unstable_pearl",
            () -> new UnstablePearlItem(UnstablePearlItem.defaultProperties()));
    public static final DeferredItem<UndeadBottleItem> UNDEAD_BOTTLE_1 = ITEMS.register("undead_bottle_1",
            () -> new UndeadBottleItem(new Item.Properties().stacksTo(1).craftRemainder(Items.GLASS_BOTTLE).rarity(Rarity.RARE), 1));
    public static final DeferredItem<UndeadBottleItem> UNDEAD_BOTTLE_2 = ITEMS.register("undead_bottle_2",
            () -> new UndeadBottleItem(new Item.Properties().stacksTo(1).craftRemainder(Items.GLASS_BOTTLE).rarity(Rarity.RARE), 2));
    public static final DeferredItem<UndeadBottleItem> UNDEAD_BOTTLE_3 = ITEMS.register("undead_bottle_3",
            () -> new UndeadBottleItem(new Item.Properties().stacksTo(1).craftRemainder(Items.GLASS_BOTTLE).rarity(Rarity.RARE), 3));
    public static final DeferredItem<UndeadBottleItem> UNDEAD_BOTTLE_4 = ITEMS.register("undead_bottle_4",
            () -> new UndeadBottleItem(new Item.Properties().stacksTo(1).craftRemainder(Items.GLASS_BOTTLE).rarity(Rarity.EPIC), 4));
    public static final DeferredItem<UndeadBottleItem> UNDEAD_BOTTLE_5 = ITEMS.register("undead_bottle_5",
            () -> new UndeadBottleItem(new Item.Properties().stacksTo(1).craftRemainder(Items.GLASS_BOTTLE).rarity(Rarity.EPIC), 5));
    public static final DeferredItem<UndeadBottleItem> UNDEAD_BOTTLE_6 = ITEMS.register("undead_bottle_6",
            () -> new UndeadBottleItem(new Item.Properties().stacksTo(1).craftRemainder(Items.GLASS_BOTTLE).rarity(Rarity.EPIC), 6));
    public static final DeferredItem<EndStormBottleItem> END_STORM_BOTTLE_1 = ITEMS.register("end_storm_bottle_1",
            () -> new EndStormBottleItem(new Item.Properties().stacksTo(1).craftRemainder(Items.GLASS_BOTTLE).rarity(Rarity.RARE), 1));
    public static final DeferredItem<EndStormBottleItem> END_STORM_BOTTLE_2 = ITEMS.register("end_storm_bottle_2",
            () -> new EndStormBottleItem(new Item.Properties().stacksTo(1).craftRemainder(Items.GLASS_BOTTLE).rarity(Rarity.RARE), 2));
    public static final DeferredItem<EndStormBottleItem> END_STORM_BOTTLE_3 = ITEMS.register("end_storm_bottle_3",
            () -> new EndStormBottleItem(new Item.Properties().stacksTo(1).craftRemainder(Items.GLASS_BOTTLE).rarity(Rarity.EPIC), 3));
    public static final DeferredItem<EndStormBottleItem> END_STORM_BOTTLE_4 = ITEMS.register("end_storm_bottle_4",
            () -> new EndStormBottleItem(new Item.Properties().stacksTo(1).craftRemainder(Items.GLASS_BOTTLE).rarity(Rarity.EPIC), 4));
    public static final DeferredItem<BannerItem> END_STORM_BANNER = ITEMS.register("end_storm_banner",
            () -> new BannerItem(
                    ModBlocks.END_STORM_CAPTAIN_BANNER.get(),
                    ModBlocks.END_STORM_CAPTAIN_WALL_BANNER.get(),
                    new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> END_STORM_POP_ICON = ITEMS.register("end_storm_pop_icon",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<UndeadTotemItem> UNDEAD_TOTEM = ITEMS.register("undead_totem",
            () -> new UndeadTotemItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BackTotemItem> BACK_TOTEM = ITEMS.register("back_totem",
            () -> new BackTotemItem(new Item.Properties().rarity(Rarity.EPIC).durability(3).stacksTo(1)));
    public static final DeferredItem<BmcRarityBannerItem> UNDEAD_INVASION_BANNER = ITEMS.register("undead_invasion_banner",
            () -> new BmcRarityBannerItem(
                    ModBlocks.UNDEAD_INVASION_CAPTAIN_BANNER.get(),
                    ModBlocks.UNDEAD_INVASION_CAPTAIN_WALL_BANNER.get(),
                    new Item.Properties(),
                    BmcModRarity.MYTHIC));
    public static final DeferredItem<Item> UNDEAD_INVASION_POP_ICON = ITEMS.register("undead_invasion_pop_icon",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> QUEST_LOG = ITEMS.register("quest_log",
            () -> new QuestLogItem(new Item.Properties().rarity(Rarity.RARE)));
    /** Base bleu marchand ambulant + taches or / parchemin (thème « quête »). */
    public static final DeferredItem<Item> QUEST_TRADER_SPAWN_EGG = ITEMS.register("quest_trader_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.QUEST_TRADER, 0x1E6B82, 0xE8B050, new Item.Properties()));

    public static final DeferredItem<BlockItem> HOLLOW_LOG_ITEM = ITEMS.registerSimpleBlockItem("hollow_log", HollowWoodBlocks.HOLLOW_LOG);
    public static final DeferredItem<BlockItem> HOLLOW_WOOD_ITEM = ITEMS.registerSimpleBlockItem("hollow_wood", HollowWoodBlocks.HOLLOW_WOOD);
    public static final DeferredItem<BlockItem> STRIPPED_HOLLOW_LOG_ITEM = ITEMS.registerSimpleBlockItem("stripped_hollow_log", HollowWoodBlocks.STRIPPED_HOLLOW_LOG);
    public static final DeferredItem<BlockItem> STRIPPED_HOLLOW_WOOD_ITEM = ITEMS.registerSimpleBlockItem("stripped_hollow_wood", HollowWoodBlocks.STRIPPED_HOLLOW_WOOD);
    public static final DeferredItem<BlockItem> HOLLOW_PLANKS_ITEM = ITEMS.registerSimpleBlockItem("hollow_planks", HollowWoodBlocks.HOLLOW_PLANKS);
    public static final DeferredItem<BlockItem> HOLLOW_STAIRS_ITEM = ITEMS.registerSimpleBlockItem("hollow_stairs", HollowWoodBlocks.HOLLOW_STAIRS);
    public static final DeferredItem<BlockItem> HOLLOW_SLAB_ITEM = ITEMS.registerSimpleBlockItem("hollow_slab", HollowWoodBlocks.HOLLOW_SLAB);
    public static final DeferredItem<BlockItem> HOLLOW_FENCE_ITEM = ITEMS.registerSimpleBlockItem("hollow_fence", HollowWoodBlocks.HOLLOW_FENCE);
    public static final DeferredItem<BlockItem> HOLLOW_FENCE_GATE_ITEM = ITEMS.registerSimpleBlockItem("hollow_fence_gate", HollowWoodBlocks.HOLLOW_FENCE_GATE);
    public static final DeferredItem<BlockItem> HOLLOW_DOOR_ITEM = ITEMS.registerSimpleBlockItem("hollow_door", HollowWoodBlocks.HOLLOW_DOOR);
    public static final DeferredItem<BlockItem> HOLLOW_TRAPDOOR_ITEM = ITEMS.registerSimpleBlockItem("hollow_trapdoor", HollowWoodBlocks.HOLLOW_TRAPDOOR);
    public static final DeferredItem<BlockItem> HOLLOW_PRESSURE_PLATE_ITEM = ITEMS.registerSimpleBlockItem("hollow_pressure_plate", HollowWoodBlocks.HOLLOW_PRESSURE_PLATE);
    public static final DeferredItem<BlockItem> HOLLOW_BUTTON_ITEM = ITEMS.registerSimpleBlockItem("hollow_button", HollowWoodBlocks.HOLLOW_BUTTON);
    public static final DeferredItem<BlockItem> HOLLOW_LEAVES_ITEM = ITEMS.registerSimpleBlockItem("hollow_leaves", HollowWoodBlocks.HOLLOW_LEAVES);
    public static final DeferredItem<BlockItem> HOLLOW_SAPLING_ITEM = ITEMS.registerSimpleBlockItem("hollow_sapling", HollowWoodBlocks.HOLLOW_SAPLING);
    public static final DeferredItem<BlockItem> HOLLOW_BARREL_ITEM = ITEMS.registerSimpleBlockItem("hollow_barrel", HollowWoodBlocks.HOLLOW_BARREL);
    public static final DeferredItem<BlockItem> HOLLOW_BOOKSHELF_ITEM = ITEMS.registerSimpleBlockItem("hollow_bookshelf", HollowWoodBlocks.HOLLOW_BOOKSHELF);

    public static final DeferredItem<BlockItem> STRIPPED_SUNWOOD_LOG_ITEM = ITEMS.registerSimpleBlockItem("stripped_sunwood_log", SunwoodBlocks.STRIPPED_SUNWOOD_LOG);
    public static final DeferredItem<BlockItem> STRIPPED_SUNWOOD_WOOD_ITEM = ITEMS.registerSimpleBlockItem("stripped_sunwood_wood", SunwoodBlocks.STRIPPED_SUNWOOD_WOOD);
    public static final DeferredItem<BlockItem> SUNWOOD_LOG_ITEM = ITEMS.registerSimpleBlockItem("sunwood_log", SunwoodBlocks.SUNWOOD_LOG);
    public static final DeferredItem<BlockItem> SUNWOOD_WOOD_ITEM = ITEMS.registerSimpleBlockItem("sunwood_wood", SunwoodBlocks.SUNWOOD_WOOD);
    public static final DeferredItem<BlockItem> SUNWOOD_PLANKS_ITEM = ITEMS.registerSimpleBlockItem("sunwood_planks", SunwoodBlocks.SUNWOOD_PLANKS);
    public static final DeferredItem<BlockItem> SUNWOOD_STAIRS_ITEM = ITEMS.registerSimpleBlockItem("sunwood_stairs", SunwoodBlocks.SUNWOOD_STAIRS);
    public static final DeferredItem<BlockItem> SUNWOOD_SLAB_ITEM = ITEMS.registerSimpleBlockItem("sunwood_slab", SunwoodBlocks.SUNWOOD_SLAB);
    public static final DeferredItem<BlockItem> SUNWOOD_FENCE_ITEM = ITEMS.registerSimpleBlockItem("sunwood_fence", SunwoodBlocks.SUNWOOD_FENCE);
    public static final DeferredItem<BlockItem> SUNWOOD_FENCE_GATE_ITEM = ITEMS.registerSimpleBlockItem("sunwood_fence_gate", SunwoodBlocks.SUNWOOD_FENCE_GATE);
    public static final DeferredItem<BlockItem> SUNWOOD_DOOR_ITEM = ITEMS.registerSimpleBlockItem("sunwood_door", SunwoodBlocks.SUNWOOD_DOOR);
    public static final DeferredItem<BlockItem> SUNWOOD_TRAPDOOR_ITEM = ITEMS.registerSimpleBlockItem("sunwood_trapdoor", SunwoodBlocks.SUNWOOD_TRAPDOOR);
    public static final DeferredItem<BlockItem> SUNWOOD_PRESSURE_PLATE_ITEM = ITEMS.registerSimpleBlockItem("sunwood_pressure_plate", SunwoodBlocks.SUNWOOD_PRESSURE_PLATE);
    public static final DeferredItem<BlockItem> SUNWOOD_BUTTON_ITEM = ITEMS.registerSimpleBlockItem("sunwood_button", SunwoodBlocks.SUNWOOD_BUTTON);
    public static final DeferredItem<SignItem> SUNWOOD_SIGN_ITEM = ITEMS.registerItem("sunwood_sign",
            props -> new SignItem(props.stacksTo(16), SunwoodBlocks.SUNWOOD_SIGN.get(), SunwoodBlocks.SUNWOOD_WALL_SIGN.get()));
    public static final DeferredItem<HangingSignItem> SUNWOOD_HANGING_SIGN_ITEM = ITEMS.registerItem("sunwood_hanging_sign",
            props -> new HangingSignItem(SunwoodBlocks.SUNWOOD_HANGING_SIGN.get(), SunwoodBlocks.SUNWOOD_WALL_HANGING_SIGN.get(), props.stacksTo(16)));
    public static final DeferredItem<BlockItem> SUNWOOD_LEAVES_ITEM = ITEMS.registerSimpleBlockItem("sunwood_leaves", SunwoodBlocks.SUNWOOD_LEAVES);
    public static final DeferredItem<BlockItem> SUNWOOD_SAPLING_ITEM = ITEMS.registerSimpleBlockItem("sunwood_sapling", SunwoodBlocks.SUNWOOD_SAPLING);
    public static final DeferredItem<BlockItem> SUNBLOOM_ITEM = ITEMS.registerSimpleBlockItem("sunbloom", SunwoodBlocks.SUNBLOOM);
    public static final DeferredItem<BlockItem> SURFACE_MOSS_ITEM = ITEMS.registerSimpleBlockItem("surface_moss", SunwoodBlocks.SURFACE_MOSS);
    public static final DeferredItem<BoatItem> SUNWOOD_BOAT = ITEMS.registerItem("sunwood_boat",
            props -> new SunwoodBoatItem(false, props.stacksTo(1)));
    public static final DeferredItem<BoatItem> SUNWOOD_CHEST_BOAT = ITEMS.registerItem("sunwood_chest_boat",
            props -> new SunwoodBoatItem(true, props.stacksTo(1)));

    public static final DeferredItem<BlockItem> ENDERITE_BLOCK_ITEM = ITEMS.register("enderite_block",
            () -> new BlockItem(ModBlocks.ENDERITE_BLOCK.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> BOREAL_BLOCK_ITEM = ITEMS.register("boreal_block",
            () -> new BlockItem(ModBlocks.BOREAL_BLOCK.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredItem<SealedExperienceBottleItem> SEALED_EXPERIENCE_BOTTLE = ITEMS.register("sealed_experience_bottle",
            () -> new SealedExperienceBottleItem(new Item.Properties()));

    public static final DeferredItem<UnknownBookItem> UNKNOWN_BOOK = ITEMS.register("unknown_book",
            () -> new UnknownBookItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));

    /** Codex des enchantements du mod (écran client au clic). */
    public static final DeferredItem<EnchantmentCodexItem> ENCHANTMENT_CODEX = ITEMS.register("enchantment_codex",
            () -> new EnchantmentCodexItem(EnchantmentCodexItem.defaultProperties()));

    public static final DeferredItem<BucketItem> EXPERIENCE_LIQUID_BUCKET = ITEMS.register("experience_liquid_bucket",
            () -> new BucketItem(ModFluids.EXPERIENCE_STILL.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    /**
     * Rareté Exotique (affichage BmcMod) : capture d’âmes (main secondaire) via {@link com.soleapixel.bmcmod.event.CrystalSoulEvents}.
     */
    public static final DeferredItem<CrystalItem> CRYSTAL = ITEMS.register("crystal",
            () -> new CrystalItem(new Item.Properties().stacksTo(1)));

    /** Métamorphose : une âme (NBT entité), durabilité 200, pas d’enchantements. */
    public static final DeferredItem<MorphCrystalItem> MORPH_CRYSTAL = ITEMS.register("morph_crystal",
            () -> new MorphCrystalItem(morphCrystalProps()));
    /** Cristal épuisé (durabilité morph ou capture à 0) — sans fonction. */
    public static final DeferredItem<CrackedCrystalItem> CRACKED_CRYSTAL = ITEMS.register("cracked_crystal",
            () -> new CrackedCrystalItem(crackedCrystalProps()));

    public static final DeferredItem<CaptureCrystalItem> CAPTURE_CRYSTAL = ITEMS.register("capture_crystal",
            () -> new CaptureCrystalItem(captureCrystalProps()));

    public static final DeferredItem<BlockItem> ENCHANTED_CHEST_ITEM = ITEMS.register("enchanted_chest",
            () -> new BlockItem(ModBlocks.ENCHANTED_CHEST.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> FAKE_CHEST_ITEM = ITEMS.register("fake_chest",
            () -> new BlockItem(ModBlocks.FAKE_CHEST.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> INFUSION_TABLE_ITEM = ITEMS.register("infusion_table",
            () -> new BlockItem(ModBlocks.INFUSION_TABLE.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredItem<BlockItem> ENDSTONE_FURNACE_ITEM = ITEMS.register("endstone_furnace",
            () -> new BlockItem(ModBlocks.ENDSTONE_FURNACE.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<BlockItem> FOUNDRY_ITEM = ITEMS.register("foundry",
            () -> new BlockItem(ModBlocks.FOUNDRY.get(), new Item.Properties().rarity(Rarity.EPIC)));

    /** Clic droit sur le coffre enchanté (max 3) ; stack 16. */
    public static final DeferredItem<Item> ENCHANTED_CHEST_UPGRADE = ITEMS.register("enchanted_chest_upgrade",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> ENDERITE_SCRAP = ITEMS.register("enderite_scrap",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> ENDERITE_INGOT = ITEMS.register("enderite_ingot",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> BOREAL_FRAGMENT = ITEMS.register("boreal_fragment",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> BOREAL_INGOT = ITEMS.register("boreal_ingot",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));

    /** Même modèle 3D / animations qu’un bouclier vanilla, textures : {@code bmcmod:textures/entity/shield/*}. */
    public static final DeferredItem<ShieldItem> DIAMOND_SHIELD = ITEMS.register("diamond_shield",
            () -> new BmcShieldItem(
                    BmcShieldItem.BmcKind.DIAMOND,
                    bmcModShieldItemProperties(480, Rarity.UNCOMMON),
                    Items.DIAMOND));
    public static final DeferredItem<ShieldItem> NETHERITE_SHIELD = ITEMS.register("netherite_shield",
            () -> new BmcShieldItem(
                    BmcShieldItem.BmcKind.NETHERITE,
                    bmcModShieldItemProperties(600, Rarity.EPIC),
                    Items.NETHERITE_INGOT));
    public static final DeferredItem<ShieldItem> ENDERITE_SHIELD = ITEMS.register("enderite_shield",
            () -> new BmcShieldItem(
                    BmcShieldItem.BmcKind.ENDERITE,
                    bmcModShieldItemProperties(800, Rarity.EPIC),
                    ModItems.ENDERITE_INGOT.get()));

    public static final DeferredItem<SmithingTemplateItem> ENDERITE_UPGRADE_SMITHING_TEMPLATE = ITEMS.register("enderite_upgrade_smithing_template",
            ModItems::createEnderiteUpgradeSmithingTemplate);
    public static final DeferredItem<SmithingTemplateItem> STAFF_UPGRADE_SMITHING_TEMPLATE = ITEMS.register("staff_upgrade_smithing_template",
            ModItems::createStaffUpgradeSmithingTemplate);
    public static final DeferredItem<SmithingTemplateItem> BOW_UPGRADE_SMITHING_TEMPLATE = ITEMS.register("bow_upgrade_smithing_template",
            ModItems::createBowUpgradeSmithingTemplate);
    public static final DeferredItem<SmithingTemplateItem> BOREAL_UPGRADE_SMITHING_TEMPLATE = ITEMS.register("boreal_upgrade_smithing_template",
            ModItems::createBorealUpgradeSmithingTemplate);

    public static final DeferredItem<SwordItem> ENDERITE_SWORD = ITEMS.register("enderite_sword",
            () -> new SwordItem(EnderiteToolTier.INSTANCE, enderiteGearProps().attributes(SwordItem.createAttributes(EnderiteToolTier.INSTANCE, 3.5F, -2.4F))));
    public static final DeferredItem<PickaxeItem> ENDERITE_PICKAXE = ITEMS.register("enderite_pickaxe",
            () -> new PickaxeItem(EnderiteToolTier.INSTANCE, enderiteGearProps().attributes(DiggerItem.createAttributes(EnderiteToolTier.INSTANCE, 1.2F, -2.8F))));
    public static final DeferredItem<ShovelItem> ENDERITE_SHOVEL = ITEMS.register("enderite_shovel",
            () -> new ShovelItem(EnderiteToolTier.INSTANCE, enderiteGearProps().attributes(DiggerItem.createAttributes(EnderiteToolTier.INSTANCE, 1.75F, -3.0F))));
    public static final DeferredItem<AxeItem> ENDERITE_AXE = ITEMS.register("enderite_axe",
            () -> new AxeItem(EnderiteToolTier.INSTANCE, enderiteGearProps().attributes(DiggerItem.createAttributes(EnderiteToolTier.INSTANCE, 6.0F, -3.0F))));
    public static final DeferredItem<HoeItem> ENDERITE_HOE = ITEMS.register("enderite_hoe",
            () -> new HoeItem(EnderiteToolTier.INSTANCE, enderiteGearProps().attributes(DiggerItem.createAttributes(EnderiteToolTier.INSTANCE, 0.0F, -3.0F))));

    public static final DeferredItem<ArmorItem> ENDERITE_HELMET = ITEMS.register("enderite_helmet",
            () -> new ArmorItem(EnderiteEquipment.ENDERITE_ARMOR_MATERIAL, ArmorItem.Type.HELMET, enderiteGearProps().durability(470)));
    public static final DeferredItem<ArmorItem> ENDERITE_CHESTPLATE = ITEMS.register("enderite_chestplate",
            () -> new ArmorItem(EnderiteEquipment.ENDERITE_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, enderiteGearProps().durability(685)));
    public static final DeferredItem<ArmorItem> ENDERITE_LEGGINGS = ITEMS.register("enderite_leggings",
            () -> new ArmorItem(EnderiteEquipment.ENDERITE_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, enderiteGearProps().durability(640)));
    public static final DeferredItem<ArmorItem> ENDERITE_BOOTS = ITEMS.register("enderite_boots",
            () -> new ArmorItem(EnderiteEquipment.ENDERITE_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, enderiteGearProps().durability(555)));
    public static final DeferredItem<SwordItem> BOREAL_SWORD = ITEMS.register("boreal_sword",
            () -> new SwordItem(BorealToolTier.INSTANCE, borealGearProps().attributes(SwordItem.createAttributes(BorealToolTier.INSTANCE, 4.0F, -2.4F))));
    public static final DeferredItem<PickaxeItem> BOREAL_PICKAXE = ITEMS.register("boreal_pickaxe",
            () -> new PickaxeItem(BorealToolTier.INSTANCE, borealGearProps().attributes(DiggerItem.createAttributes(BorealToolTier.INSTANCE, 1.5F, -2.8F))));
    public static final DeferredItem<ShovelItem> BOREAL_SHOVEL = ITEMS.register("boreal_shovel",
            () -> new ShovelItem(BorealToolTier.INSTANCE, borealGearProps().attributes(DiggerItem.createAttributes(BorealToolTier.INSTANCE, 2.0F, -3.0F))));
    public static final DeferredItem<AxeItem> BOREAL_AXE = ITEMS.register("boreal_axe",
            () -> new AxeItem(BorealToolTier.INSTANCE, borealGearProps().attributes(DiggerItem.createAttributes(BorealToolTier.INSTANCE, 6.5F, -3.0F))));
    public static final DeferredItem<ArmorItem> BOREAL_HELMET = ITEMS.register("boreal_helmet",
            () -> new ArmorItem(BorealEquipment.BOREAL_ARMOR_MATERIAL, ArmorItem.Type.HELMET, borealGearProps().durability(520)));
    public static final DeferredItem<ArmorItem> BOREAL_CHESTPLATE = ITEMS.register("boreal_chestplate",
            () -> new ArmorItem(BorealEquipment.BOREAL_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, borealGearProps().durability(760)));
    public static final DeferredItem<ArmorItem> BOREAL_LEGGINGS = ITEMS.register("boreal_leggings",
            () -> new ArmorItem(BorealEquipment.BOREAL_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, borealGearProps().durability(710)));
    public static final DeferredItem<ArmorItem> BOREAL_BOOTS = ITEMS.register("boreal_boots",
            () -> new ArmorItem(BorealEquipment.BOREAL_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, borealGearProps().durability(620)));

    public static final DeferredItem<VoidShardItem> VOID_SHARD = ITEMS.register("void_shard",
            () -> new VoidShardItem(new Item.Properties().stacksTo(16)));

    /** Corne mythique : boucle de notes (bloc-note) sur un thème aléatoire tant que le joueur maintient. */
    public static final DeferredItem<MelodyHornItem> MELODY_HORN = ITEMS.register("melody_horn",
            () -> new MelodyHornItem(new Item.Properties().stacksTo(1)));

    /** Butin du Warden : 50 % via {@link com.soleapixel.bmcmod.loot.WardenTendrilLootModifier}. */
    public static final DeferredItem<Item> WARDEN_TENDRIL = ITEMS.register("warden_tendril",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE).stacksTo(16)));

    /** Infusion : onde sonic ; 6 charges (voir {@link EchoStaffItem}). */
    public static final DeferredItem<EchoStaffItem> ECHO_STAFF = ITEMS.register("echo_staff",
            () -> new EchoStaffItem(new Item.Properties().stacksTo(1).durability(6)));

    /** Jet de feu maintenu ; durabilité qui fond pendant l’utilisation (voir {@link FlameStaffItem}). */
    public static final DeferredItem<FlameStaffItem> FLAME_STAFF = ITEMS.register("flame_staff",
            () -> new FlameStaffItem(new Item.Properties().stacksTo(1).durability(FlameStaffItem.MAX_USES)));
    /** Pont de glace / rayon de givre / golems de glace améliorés ; Ctrl + clic droit pour cycler les modes. */
    public static final DeferredItem<IceStaffItem> ICE_STAFF = ITEMS.register("ice_staff",
            () -> new IceStaffItem(new Item.Properties().stacksTo(1).durability(IceStaffItem.MAX_USES)));

    /** Coffre de l’End du joueur, ouvert au clic droit (même stockage que le bloc). */
    public static final DeferredItem<PocketEnderChestItem> POCKET_ENDER_CHEST = ITEMS.register("pocket_ender_chest",
            () -> new PocketEnderChestItem(new Item.Properties().stacksTo(1)));

    /** 3 modes (perle, rayon + TP cible, TP aléatoire) ; Ctrl + clic droit pour cycler. */
    public static final DeferredItem<EndStaffItem> END_STAFF = ITEMS.register("end_staff",
            () -> new EndStaffItem(new Item.Properties().stacksTo(1).durability(EndStaffItem.MAX_USES)));

    /** Boule de dragon (charge + relâche) ou souffle maintenu ; Ctrl + clic droit pour cycler. */
    public static final DeferredItem<DragonStaffItem> DRAGON_STAFF = ITEMS.register("dragon_staff",
            () -> new DragonStaffItem(new Item.Properties().stacksTo(1).durability(DragonStaffItem.MAX_USES)));

    /** Tête de wither ou invocation de 3 alliés wither-squelettes ; Ctrl + clic droit pour cycler. */
    public static final DeferredItem<WitherStaffItem> WITHER_STAFF = ITEMS.register("wither_staff",
            () -> new WitherStaffItem(new Item.Properties().stacksTo(1).durability(WitherStaffItem.MAX_USES)));

    /** Placement en masse (ligne / plan / mur) ; paliers = tailles + durabilité. Ctrl + clic droit : mode ; Maj + Ctrl + clic droit sur un bloc : matériau. */
    public static final DeferredItem<BuilderWandItem> BUILDER_WAND_GOLD = ITEMS.register("builder_wand_gold",
            () -> new BuilderWandItem(BuilderWandTier.GOLD, new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<BuilderWandItem> BUILDER_WAND_DIAMOND = ITEMS.register("builder_wand_diamond",
            () -> new BuilderWandItem(BuilderWandTier.DIAMOND, new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<BuilderWandItem> BUILDER_WAND_EMERALD = ITEMS.register("builder_wand_emerald",
            () -> new BuilderWandItem(BuilderWandTier.EMERALD, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BuilderWandItem> BUILDER_WAND_NETHERITE = ITEMS.register("builder_wand_netherite",
            () -> new BuilderWandItem(BuilderWandTier.NETHERITE, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BuilderWandItem> BUILDER_WAND_ENDERITE = ITEMS.register("builder_wand_enderite",
            () -> new BuilderWandItem(BuilderWandTier.ENDERITE, enderiteGearProps()));

    public static final DeferredItem<LightningBowItem> LIGHTNING_BOW = ITEMS.register("lightning_bow",
            () -> new LightningBowItem(lightningBowProps()));
    public static final DeferredItem<UpgradeBowItem> IRON_BOW = ITEMS.register("iron_bow",
            () -> new UpgradeBowItem(new Item.Properties().durability(448), 1.04F, 1.03F, 1.06F));
    public static final DeferredItem<UpgradeBowItem> GOLD_BOW = ITEMS.register("gold_bow",
            () -> new UpgradeBowItem(new Item.Properties().durability(476), 1.08F, 1.07F, 1.12F));
    public static final DeferredItem<UpgradeBowItem> DIAMOND_BOW = ITEMS.register("diamond_bow",
            () -> new UpgradeBowItem(new Item.Properties().durability(560).rarity(Rarity.RARE), 1.14F, 1.12F, 1.20F));
    public static final DeferredItem<UpgradeBowItem> EMERALD_BOW = ITEMS.register("emerald_bow",
            () -> new UpgradeBowItem(new Item.Properties().durability(620).rarity(Rarity.EPIC), 1.18F, 1.16F, 1.28F));
    public static final DeferredItem<Item> BLINK_ROD = ITEMS.register("blink_rod",
            () -> new RarityStickItem(new Item.Properties().stacksTo(64), BmcModRarity.COMMON));
    public static final DeferredItem<Item> NEBRITH_SHARD = ITEMS.register("nebrith_shard",
            () -> new RarityStickItem(new Item.Properties().stacksTo(64), BmcModRarity.COMMON));
    public static final DeferredItem<Item> FOSSIL_SCRAP = ITEMS.register("fossil_scrap",
            () -> new RarityStickItem(new Item.Properties().stacksTo(64), BmcModRarity.EXOTIC));
    public static final DeferredItem<Item> END_GOLEM_HEART = ITEMS.register("end_golem_heart",
            () -> new RarityStickItem(new Item.Properties().stacksTo(1), BmcModRarity.MYTHIC));

    public static final DeferredItem<DiamondAppleItem> DIAMOND_APPLE = ITEMS.register("diamond_apple",
            () -> new DiamondAppleItem(Rarity.RARE, 5400, 0, 3000, 0, 3000, 0, 400, 0, false));
    public static final DeferredItem<DiamondAppleItem> ENCHANTED_DIAMOND_APPLE = ITEMS.register("enchanted_diamond_apple",
            () -> new DiamondAppleItem(Rarity.EPIC, 9600, 2, 6000, 0, 6000, 1, 600, 1, true));

    public static final DeferredItem<ArmorItem> OBSIDIAN_HELMET = ITEMS.register("obsidian_helmet",
            () -> new ArmorItem(ObsidianEquipment.OBSIDIAN_ARMOR_MATERIAL, ArmorItem.Type.HELMET, obsidianGearProps()));
    public static final DeferredItem<ArmorItem> OBSIDIAN_CHESTPLATE = ITEMS.register("obsidian_chestplate",
            () -> new ArmorItem(ObsidianEquipment.OBSIDIAN_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, obsidianGearProps()));
    public static final DeferredItem<ArmorItem> OBSIDIAN_LEGGINGS = ITEMS.register("obsidian_leggings",
            () -> new ArmorItem(ObsidianEquipment.OBSIDIAN_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, obsidianGearProps()));
    public static final DeferredItem<ArmorItem> OBSIDIAN_BOOTS = ITEMS.register("obsidian_boots",
            () -> new ArmorItem(ObsidianEquipment.OBSIDIAN_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, obsidianGearProps()));

    public static final DeferredItem<Item> MUSIC_DISC_BEYOND_THE_ENDERMAN = ITEMS.register("music_disc_beyond_the_enderman",
            () -> new Item(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.RARE)
                    .component(DataComponents.JUKEBOX_PLAYABLE, new JukeboxPlayable(new EitherHolder<>(JUKEBOX_SONG_BEYOND_THE_ENDERMAN), true))));

    public static final DeferredItem<Item> BLINK_SPAWN_EGG = ITEMS.register("blink_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BLINK, 0x3A1F5C, 0xE8E0F5, new Item.Properties()));
    public static final DeferredItem<Item> END_GOLEM_SPAWN_EGG = ITEMS.register("end_golem_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.END_GOLEM, 0x2B0846, 0x9B7BD4, new Item.Properties()));
    public static final DeferredItem<Item> ENDLING_SPAWN_EGG = ITEMS.register("endling_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ENDLING, 0x4A2B6E, 0xC9B8E8, new Item.Properties()));
    /** Couleurs type orbe d’XP : vert vif + jaune citron (taches). */
    public static final DeferredItem<Item> RADIANT_SLIME_SPAWN_EGG = ITEMS.register("radiant_slime_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.RADIANT_SLIME, 0x7FFF55, 0xFFFF99, new Item.Properties()));
    public static final DeferredItem<Item> SKELETON_VILLAGER_SPAWN_EGG = ITEMS.register("skeleton_villager_spawn_egg",
            () -> new SkeletonVillagerSpawnEggItem(false, 0xD4D1C7, 0x4F4034, new Item.Properties()));
    public static final DeferredItem<Item> SOUL_SKELETON_VILLAGER_SPAWN_EGG = ITEMS.register("soul_skeleton_villager_spawn_egg",
            () -> new SkeletonVillagerSpawnEggItem(true, 0x6CC8E8, 0x183245, new Item.Properties()));
    public static final DeferredItem<Item> UNDEAD_ILLAGER_SPAWN_EGG = ITEMS.register("undead_illager_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.UNDEAD_ILLAGER, 0x3C3A34, 0x8A7D62, new Item.Properties()));
    public static final DeferredItem<Item> VLINX_SPAWN_EGG = ITEMS.register("vlinx_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.VLINX, 0x2E2A24, 0xA38B61, new Item.Properties()));
    public static final DeferredItem<Item> MIMIC_CHEST_SPAWN_EGG = ITEMS.register("mimic_chest_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MIMIC_CHEST, 0x8B4513, 0xB22222, new Item.Properties()));

    private static Item.Properties backpackProperties(Rarity rarity) {
        return new Item.Properties()
                .stacksTo(1)
                .rarity(rarity)
                .component(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
    }

    private static Item.Properties copperProps() {
        return new Item.Properties().rarity(Rarity.UNCOMMON);
    }

    /** Arc Exotic : rareté vanilla {@link Rarity#RARE} ; affichage Exotic côté client (voir {@link com.soleapixel.bmcmod.client.RarityTooltipHandler}). */
    private static Item.Properties lightningBowProps() {
        return new Item.Properties().rarity(Rarity.RARE).durability(576)
                .component(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    /** Même principe que {@link CrystalItem} : rareté BmcMod Exotic (infobulle / bordure via {@link com.soleapixel.bmcmod.client.RarityTooltipHandler}). */
    private static Item.Properties morphCrystalProps() {
        return new Item.Properties()
                .stacksTo(1)
                .durability(200)
                .component(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    private static Item.Properties crackedCrystalProps() {
        return new Item.Properties().rarity(Rarity.COMMON).stacksTo(1);
    }

    /** Même affichage Exotic que le cristal d’âmes / morph (voir {@link com.soleapixel.bmcmod.client.RarityTooltipHandler}). */
    private static Item.Properties captureCrystalProps() {
        return new Item.Properties()
                .stacksTo(1)
                .durability(1000)
                .component(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    private static Item.Properties copperGearProps() {
        return copperProps().stacksTo(1)
                .component(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    private static Item.Properties emeraldBaseProps() {
        return new Item.Properties().rarity(Rarity.RARE);
    }

    private static Item.Properties emeraldGearProps() {
        // ItemStack.isEnchantable() exige ENCHANTMENTS non nul ; TieredItem fournit déjà getEnchantmentValue().
        return emeraldBaseProps()
                .stacksTo(1)
                .component(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    /** Rare vanilla pour les systèmes internes ; infobulle Exotic côté client. */
    private static Item.Properties skyBootsProps() {
        return new Item.Properties()
                .rarity(Rarity.RARE)
                .stacksTo(1)
                .component(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false));
    }

    private static Item.Properties shulkerGearProps() {
        return new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)
                .component(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    /** Casques villageois : un peu plus résistants / durables que le cuivre, rareté commune. */
    private static Item.Properties villagerHatProps() {
        return new Item.Properties().rarity(Rarity.COMMON).stacksTo(1).durability(195)
                .component(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    private static Item.Properties obsidianGearProps() {
        return new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)
                .component(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    private static Item.Properties enderiteGearProps() {
        return new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)
                .component(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    private static Item.Properties borealGearProps() {
        return new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)
                .component(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    private static Item.Properties bmcModShieldItemProperties(int durability, Rarity rarity) {
        // Copie les composants du bouclier vanilla (résolution « blocage », bannière, etc.) en excluant la base endommagée.
        ItemStack v = Items.SHIELD.getDefaultInstance();
        var props = new Item.Properties();
        for (DataComponentType<?> t : v.getComponents()
                .keySet()) {
            if (t == DataComponents.DAMAGE) {
                continue;
            }
            if (t == DataComponents.REPAIR_COST) {
                continue;
            }
            if (t == DataComponents.CUSTOM_NAME) {
                continue;
            }
            Object val = v.get(t);
            if (val == null) {
                continue;
            }
            props = props.component(unchecked(t), val);
        }
        return props
                .rarity(rarity)
                .stacksTo(1)
                .durability(durability)
                .component(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    /** Copy vanilla skull equippable behavior, with BmcMod Exotic styling via tooltip handler. */
    private static Item.Properties mobHeadProps() {
        ItemStack v = Items.SKELETON_SKULL.getDefaultInstance();
        var props = new Item.Properties().rarity(Rarity.EPIC);
        for (DataComponentType<?> t : v.getComponents().keySet()) {
            Object val = v.get(t);
            if (val == null) {
                continue;
            }
            props = props.component(unchecked(t), val);
        }
        return props;
    }

    @SuppressWarnings("unchecked, rawtypes")
    private static <T> DataComponentType<T> unchecked(DataComponentType<?> t) {
        return (DataComponentType<T>) t;
    }

    /**
     * Même logique que {@link SmithingTemplateItem#createNetheriteUpgradeTemplate()} mais avec les textes et la texture du mod
     * (évite la ligne « Netherite Upgrade » et l’icône héritée du gabarit vanilla).
     */
    private static SmithingTemplateItem createEnderiteUpgradeSmithingTemplate() {
        return new SmithingTemplateItem(
                Component.translatable("item.bmcmod.smithing_template.enderite_upgrade.applies_to").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.bmcmod.smithing_template.enderite_upgrade.ingredients").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.bmcmod.smithing_template.enderite_upgrade.upgrade_description").withStyle(ChatFormatting.GRAY),
                Component.translatable("item.bmcmod.smithing_template.enderite_upgrade.base_slot_description"),
                Component.translatable("item.bmcmod.smithing_template.enderite_upgrade.additions_slot_description"),
                List.of(
                        ResourceLocation.withDefaultNamespace("item/empty_armor_slot_helmet"),
                        ResourceLocation.withDefaultNamespace("item/empty_slot_sword"),
                        ResourceLocation.withDefaultNamespace("item/empty_armor_slot_chestplate"),
                        ResourceLocation.withDefaultNamespace("item/empty_slot_pickaxe"),
                        ResourceLocation.withDefaultNamespace("item/empty_armor_slot_leggings"),
                        ResourceLocation.withDefaultNamespace("item/empty_slot_axe"),
                        ResourceLocation.withDefaultNamespace("item/empty_armor_slot_boots"),
                        ResourceLocation.withDefaultNamespace("item/empty_slot_hoe"),
                        ResourceLocation.withDefaultNamespace("item/empty_slot_shovel")),
                List.of(ResourceLocation.withDefaultNamespace("item/empty_slot_ingot")));
    }

    /** Template dédié à l'évolution des staffs spéciaux. */
    private static SmithingTemplateItem createStaffUpgradeSmithingTemplate() {
        return new SmithingTemplateItem(
                Component.translatable("item.bmcmod.smithing_template.staff_upgrade.applies_to").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.bmcmod.smithing_template.staff_upgrade.ingredients").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.bmcmod.smithing_template.staff_upgrade.upgrade_description").withStyle(ChatFormatting.GRAY),
                Component.translatable("item.bmcmod.smithing_template.staff_upgrade.base_slot_description"),
                Component.translatable("item.bmcmod.smithing_template.staff_upgrade.additions_slot_description"),
                List.of(
                        ResourceLocation.withDefaultNamespace("item/empty_slot_hoe"),
                        ResourceLocation.withDefaultNamespace("item/empty_slot_shovel"),
                        ResourceLocation.withDefaultNamespace("item/empty_slot_sword")),
                List.of(ResourceLocation.withDefaultNamespace("item/empty_slot_ingot")));
    }

    private static SmithingTemplateItem createBowUpgradeSmithingTemplate() {
        return new SmithingTemplateItem(
                Component.translatable("item.bmcmod.smithing_template.bow_upgrade.applies_to").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.bmcmod.smithing_template.bow_upgrade.ingredients").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.bmcmod.smithing_template.bow_upgrade.upgrade_description").withStyle(ChatFormatting.GRAY),
                Component.translatable("item.bmcmod.smithing_template.bow_upgrade.base_slot_description"),
                Component.translatable("item.bmcmod.smithing_template.bow_upgrade.additions_slot_description"),
                List.of(ResourceLocation.withDefaultNamespace("item/empty_slot_sword")),
                List.of(ResourceLocation.withDefaultNamespace("item/empty_slot_ingot")));
    }

    private static SmithingTemplateItem createBorealUpgradeSmithingTemplate() {
        return new SmithingTemplateItem(
                Component.translatable("item.bmcmod.smithing_template.boreal_upgrade.applies_to").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.bmcmod.smithing_template.boreal_upgrade.ingredients").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.bmcmod.smithing_template.boreal_upgrade.upgrade_description").withStyle(ChatFormatting.GRAY),
                Component.translatable("item.bmcmod.smithing_template.boreal_upgrade.base_slot_description"),
                Component.translatable("item.bmcmod.smithing_template.boreal_upgrade.additions_slot_description"),
                List.of(
                        ResourceLocation.withDefaultNamespace("item/empty_armor_slot_helmet"),
                        ResourceLocation.withDefaultNamespace("item/empty_slot_sword"),
                        ResourceLocation.withDefaultNamespace("item/empty_armor_slot_chestplate"),
                        ResourceLocation.withDefaultNamespace("item/empty_slot_pickaxe"),
                        ResourceLocation.withDefaultNamespace("item/empty_armor_slot_leggings"),
                        ResourceLocation.withDefaultNamespace("item/empty_slot_axe"),
                        ResourceLocation.withDefaultNamespace("item/empty_armor_slot_boots"),
                        ResourceLocation.withDefaultNamespace("item/empty_slot_hoe"),
                        ResourceLocation.withDefaultNamespace("item/empty_slot_shovel")),
                List.of(ResourceLocation.withDefaultNamespace("item/empty_slot_ingot")));
    }

    private ModItems() {
    }
}
