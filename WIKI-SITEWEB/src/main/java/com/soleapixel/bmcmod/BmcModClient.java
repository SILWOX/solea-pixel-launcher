package com.soleapixel.bmcmod;

import net.minecraft.client.Minecraft;
import com.soleapixel.bmcmod.client.BmcModConfigurationScreen;
import com.soleapixel.bmcmod.client.BackpackChestScreen;
import com.soleapixel.bmcmod.client.BackpackTooltipClient;
import com.soleapixel.bmcmod.client.CloneRenderer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.world.level.GrassColor;
import com.soleapixel.bmcmod.client.CloneChestScreen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import com.soleapixel.bmcmod.client.ScytheSweepClientHandler;
import com.soleapixel.bmcmod.client.ScytheTornadoRenderer;
import com.soleapixel.bmcmod.client.ModParticleProviders;
import com.soleapixel.bmcmod.client.SunwoodBoatRenderer;
import com.soleapixel.bmcmod.client.BlinkModel;
import com.soleapixel.bmcmod.client.BlinkRenderer;
import com.soleapixel.bmcmod.client.DiamondGolemRenderer;
import com.soleapixel.bmcmod.client.DummyRenderer;
import com.soleapixel.bmcmod.client.EndGolemRenderer;
import com.soleapixel.bmcmod.client.EndlingModel;
import com.soleapixel.bmcmod.client.EndlingRenderer;
import com.soleapixel.bmcmod.client.RadiantSlimeRenderer;
import com.soleapixel.bmcmod.client.SkeletonVillagerModel;
import com.soleapixel.bmcmod.client.SkeletonVillagerRenderer;
import com.soleapixel.bmcmod.client.TargetDummyModel;
import com.soleapixel.bmcmod.client.VoidShardProjectileModel;
import com.soleapixel.bmcmod.client.VoidShardProjectileRenderer;
import com.soleapixel.bmcmod.client.BmcModShieldItemExtensions;
import com.soleapixel.bmcmod.client.EnchantedChestItemExtensions;
import com.soleapixel.bmcmod.client.FakeChestItemExtensions;
import com.soleapixel.bmcmod.client.EnchantedChestRenderer;
import com.soleapixel.bmcmod.client.FakeChestRenderer;
import com.soleapixel.bmcmod.client.MimicChestModel;
import com.soleapixel.bmcmod.client.MimicChestRenderer;
import com.soleapixel.bmcmod.client.endstonefurnace.EndstoneFurnaceScreen;
import com.soleapixel.bmcmod.client.foundry.FoundryScreen;
import com.soleapixel.bmcmod.client.InfusionTableMenuScreen;
import com.soleapixel.bmcmod.client.InfusionTableRenderer;
import com.soleapixel.bmcmod.client.UndeadIllagerRenderer;
import com.soleapixel.bmcmod.client.EnchantmentCodexScreen;
import com.soleapixel.bmcmod.client.VlinxRenderer;
import com.soleapixel.bmcmod.client.upgradetable.UpgradeTableScreen;
import com.soleapixel.bmcmod.client.ExperienceLiquidFluidClientExtensions;
import com.soleapixel.bmcmod.registry.HollowWoodBlocks;
import com.soleapixel.bmcmod.registry.ModBlocks;
import com.soleapixel.bmcmod.registry.ModBlockEntityTypes;
import com.soleapixel.bmcmod.registry.ModEntities;
import com.soleapixel.bmcmod.registry.ModFluids;
import com.soleapixel.bmcmod.registry.ModItems;
import com.soleapixel.bmcmod.item.EnchantmentCodexItem;
import com.soleapixel.bmcmod.registry.ModMenus;
import com.soleapixel.bmcmod.registry.SunwoodBlocks;

@Mod(value = BmcMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public class BmcModClient {
    public BmcModClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(ModParticleProviders::onRegisterParticleProviders);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, BmcModConfigurationScreen::new);
        NeoForge.EVENT_BUS.addListener(BackpackTooltipClient::onItemTooltip);
    }

    @SubscribeEvent
    static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.BACKPACK.get(), BackpackChestScreen::new);
        event.register(ModMenus.INFUSION_TABLE.get(), InfusionTableMenuScreen::new);
        event.register(ModMenus.ENDSTONE_FURNACE.get(), EndstoneFurnaceScreen::new);
        event.register(ModMenus.FOUNDRY.get(), FoundryScreen::new);
        event.register(ModMenus.UPGRADE_TABLE.get(), UpgradeTableScreen::new);
        event.register(ModMenus.CLONE_INVENTORY.get(), CloneChestScreen::new);
    }

    @SubscribeEvent
    static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> BackpackChestScreen.clearTextureSizeCache());
    }

    @SubscribeEvent
    static void registerEnchantedChestItemBewlr(RegisterClientExtensionsEvent event) {
        event.registerFluidType(ExperienceLiquidFluidClientExtensions.INSTANCE, ModFluids.EXPERIENCE_FLUID_TYPE.get());
        event.registerItem(EnchantedChestItemExtensions.INSTANCE, ModItems.ENCHANTED_CHEST_ITEM.get());
        event.registerItem(FakeChestItemExtensions.INSTANCE, ModItems.FAKE_CHEST_ITEM.get());
        event.registerItem(
                BmcModShieldItemExtensions.INSTANCE,
                ModItems.DIAMOND_SHIELD.get(),
                ModItems.NETHERITE_SHIELD.get(),
                ModItems.ENDERITE_SHIELD.get());
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerMelodyHornItemProperties();
            RenderType cutout = RenderType.cutout();
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SMALL_NEBRITH_BUD.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MEDIUM_NEBRITH_BUD.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LARGE_NEBRITH_BUD.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.NEBRITH_CLUSTER.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.EXPERIENCE_LIQUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.JAERYS.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.VOID_TORCH.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.VOID_WALL_TORCH.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.VOID_LANTERN.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SKELETON_VILLAGER_SKULL.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SKELETON_VILLAGER_WALL_SKULL.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNDEAD_INVASION_CAPTAIN_BANNER.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNDEAD_INVASION_CAPTAIN_WALL_BANNER.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.END_STORM_CAPTAIN_BANNER.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.END_STORM_CAPTAIN_WALL_BANNER.get(), cutout);

            RenderType cutoutMipped = RenderType.cutoutMipped();
            ItemBlockRenderTypes.setRenderLayer(HollowWoodBlocks.HOLLOW_SAPLING.get(), cutoutMipped);
            ItemBlockRenderTypes.setRenderLayer(HollowWoodBlocks.HOLLOW_LEAVES.get(), cutoutMipped);
            ItemBlockRenderTypes.setRenderLayer(HollowWoodBlocks.HOLLOW_DOOR.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(HollowWoodBlocks.HOLLOW_TRAPDOOR.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(HollowWoodBlocks.HOLLOW_FENCE.get(), cutoutMipped);
            ItemBlockRenderTypes.setRenderLayer(HollowWoodBlocks.HOLLOW_FENCE_GATE.get(), cutoutMipped);

            Sheets.addWoodType(SunwoodBlocks.SUNWOOD_WOOD_TYPE);
            ItemBlockRenderTypes.setRenderLayer(SunwoodBlocks.SUNWOOD_SAPLING.get(), cutoutMipped);
            ItemBlockRenderTypes.setRenderLayer(SunwoodBlocks.SUNWOOD_LEAVES.get(), cutoutMipped);
            ItemBlockRenderTypes.setRenderLayer(SunwoodBlocks.SUNWOOD_DOOR.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(SunwoodBlocks.SUNWOOD_TRAPDOOR.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(SunwoodBlocks.SUNWOOD_FENCE.get(), cutoutMipped);
            ItemBlockRenderTypes.setRenderLayer(SunwoodBlocks.SUNWOOD_FENCE_GATE.get(), cutoutMipped);
            ItemBlockRenderTypes.setRenderLayer(SunwoodBlocks.SUNWOOD_SIGN.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(SunwoodBlocks.SUNWOOD_WALL_SIGN.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(SunwoodBlocks.SUNWOOD_HANGING_SIGN.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(SunwoodBlocks.SUNWOOD_WALL_HANGING_SIGN.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(SunwoodBlocks.SUNBLOOM.get(), cutout);
            ItemBlockRenderTypes.setRenderLayer(SunwoodBlocks.SURFACE_MOSS.get(), cutoutMipped);

            registerLightningBowItemProperties();
            registerUpgradeBowItemProperties();
            registerBmcShieldItemProperties();
            registerScytheSweepItemProperties();

            EnchantmentCodexItem.setClientOpenAction(EnchantmentCodexScreen::openFromClient);
        });
    }

    /** Prédicat {@code bmcmod:sweep_pull} pour overrides du modèle en main (balayage 1ʳᵉ personne). */
    private static void registerScytheSweepItemProperties() {
        var sweepPull = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "sweep_pull");
        for (var scythe : new net.minecraft.world.item.Item[] {
                ModItems.IRON_SCYTHE.get(),
                ModItems.GOLDEN_SCYTHE.get(),
                ModItems.DIAMOND_SCYTHE.get(),
                ModItems.EMERALD_SCYTHE.get(),
                ModItems.NETHERITE_SCYTHE.get(),
                ModItems.ENDERITE_SCYTHE.get(),
                ModItems.BOREAL_SCYTHE.get()
        }) {
            ItemProperties.register(scythe, sweepPull, (stack, level, entity, seed) -> {
                if (entity != Minecraft.getInstance().player) {
                    return 0.0F;
                }
                return ScytheSweepClientHandler.getSweepModelPull();
            });
        }
    }

    /**
     * Même prédicat que le bouclier vanilla : la propriété "blocking" pilote
     * {@code item/…_shield_blocking} dans le JSON, sinon l’icône / 3D restent tordus.
     */
    private static void registerBmcShieldItemProperties() {
        var id = ResourceLocation.withDefaultNamespace("blocking");
        for (var s : new net.minecraft.world.item.Item[] {
            ModItems.DIAMOND_SHIELD.get(),
            ModItems.NETHERITE_SHIELD.get(),
            ModItems.ENDERITE_SHIELD.get()
        }) {
            ItemProperties.register(s, id, (stack, level, entity, seed) -> {
                if (entity == null || !entity.isUsingItem()) {
                    return 0.0F;
                }
                return entity.getUseItem() == stack ? 1.0F : 0.0F;
            });
        }
    }

    /**
     * Comme l’arc vanilla : enregistre les prédicats {@code pull} / {@code pulling} pour que les
     * {@code overrides} du modèle JSON affichent les textures de bandage.
     */
    /**
     * Prédicat vanilla {@code tooting} : sans enregistrement explicite, l’override du modèle « souffler »
     * peut ne pas s’appliquer aux items moddés ; aligne la corne sur la corne de chèvre.
     */
    private static void registerMelodyHornItemProperties() {
        var tooting = ResourceLocation.withDefaultNamespace("tooting");
        ItemProperties.register(
                ModItems.MELODY_HORN.get(),
                tooting,
                (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }

    private static void registerLightningBowItemProperties() {
        var bow = ModItems.LIGHTNING_BOW.get();
        ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) -> {
            if (entity == null || entity.getUseItem() != stack) {
                return 0.0F;
            }
            return (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F;
        });
        ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) -> {
            return entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F;
        });
    }

    /** Arcs fer / or / diamant / émeraude : mêmes prédicats que l’arc vanilla pour overrides + animation de bandage. */
    private static void registerUpgradeBowItemProperties() {
        var pull = ResourceLocation.withDefaultNamespace("pull");
        var pulling = ResourceLocation.withDefaultNamespace("pulling");
        for (var bow : new net.minecraft.world.item.Item[] {
                ModItems.IRON_BOW.get(),
                ModItems.GOLD_BOW.get(),
                ModItems.DIAMOND_BOW.get(),
                ModItems.EMERALD_BOW.get()
        }) {
            ItemProperties.register(bow, pull, (stack, level, entity, seed) -> {
                if (entity == null || entity.getUseItem() != stack) {
                    return 0.0F;
                }
                return (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F;
            });
            ItemProperties.register(bow, pulling, (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        }
    }

    @SubscribeEvent
    static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BlinkModel.LAYER_LOCATION, BlinkModel::createBodyLayer);
        event.registerLayerDefinition(VoidShardProjectileModel.LAYER_LOCATION, VoidShardProjectileModel::createBodyLayer);
        event.registerLayerDefinition(EndlingModel.LAYER_LOCATION, EndlingModel::createBodyLayer);
        event.registerLayerDefinition(SkeletonVillagerModel.LAYER_LOCATION, SkeletonVillagerModel::createBodyLayer);
        event.registerLayerDefinition(TargetDummyModel.LAYER_LOCATION, TargetDummyModel::createBodyLayer);
        event.registerLayerDefinition(MimicChestModel.LAYER_LOCATION, MimicChestModel::createBodyLayer);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntityTypes.ENCHANTED_CHEST.get(), EnchantedChestRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.FAKE_CHEST.get(), FakeChestRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.INFUSION_TABLE.get(), InfusionTableRenderer::new);
        event.registerEntityRenderer(ModEntities.DIAMOND_GOLEM.get(), DiamondGolemRenderer::new);
        event.registerEntityRenderer(ModEntities.BLINK.get(), BlinkRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_SHARD_PROJECTILE.get(), VoidShardProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.UNSTABLE_PEARL_PROJECTILE.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.0F, true));
        event.registerEntityRenderer(ModEntities.SCYTHE_TORNADO.get(), ScytheTornadoRenderer::new);
        event.registerEntityRenderer(ModEntities.END_GOLEM.get(), EndGolemRenderer::new);
        event.registerEntityRenderer(ModEntities.ENDLING.get(), EndlingRenderer::new);
        event.registerEntityRenderer(ModEntities.MIMIC_CHEST.get(), MimicChestRenderer::new);
        event.registerEntityRenderer(ModEntities.RADIANT_SLIME.get(), RadiantSlimeRenderer::new);
        event.registerEntityRenderer(ModEntities.SKELETON_VILLAGER.get(), SkeletonVillagerRenderer::new);
        event.registerEntityRenderer(ModEntities.BOUNTY_HUNTER.get(), ZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.UNDEAD_ILLAGER.get(), UndeadIllagerRenderer::new);
        event.registerEntityRenderer(ModEntities.VLINX.get(), VlinxRenderer::new);
        event.registerEntityRenderer(ModEntities.DUMMY.get(), DummyRenderer::new);
        event.registerEntityRenderer(ModEntities.CLONE.get(), CloneRenderer::new);
        event.registerEntityRenderer(ModEntities.QUEST_TRADER.get(), ctx -> new WanderingTraderRenderer(ctx) {
            @Override
            public net.minecraft.resources.ResourceLocation getTextureLocation(net.minecraft.world.entity.npc.WanderingTrader entity) {
                return BmcMod.loc("textures/entity/mob/quest_trader.png");
            }
        });
        event.registerEntityRenderer(ModEntities.SUNWOOD_BOAT.get(), ctx -> new SunwoodBoatRenderer(ctx, false));
        event.registerEntityRenderer(ModEntities.SUNWOOD_CHEST_BOAT.get(), ctx -> new SunwoodBoatRenderer(ctx, true));
    }

    @SubscribeEvent
    static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> {
                    if (tintIndex != 0) {
                        return 0xFFFFFF;
                    }
                    if (level == null || pos == null) {
                        return GrassColor.getDefaultColor();
                    }
                    return BiomeColors.getAverageGrassColor(level, pos);
                },
                SunwoodBlocks.SURFACE_MOSS.get());
    }

    @SubscribeEvent
    static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> GrassColor.getDefaultColor(), ModItems.SURFACE_MOSS_ITEM.get());
    }
}
