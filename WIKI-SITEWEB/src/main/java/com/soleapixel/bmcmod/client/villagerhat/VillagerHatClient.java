package com.soleapixel.bmcmod.client.villagerhat;

import java.util.List;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.registry.ModEntities;

@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class VillagerHatClient {
    private VillagerHatClient() {
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (VillagerHatModelKey key : VillagerHatModelKey.values()) {
            event.registerLayerDefinition(key.layer(), key.mesh());
        }
    }

    @SubscribeEvent
    public static void registerItemExtensions(RegisterClientExtensionsEvent event) {
        VillagerHatItemExtensions ext = new VillagerHatItemExtensions();
        for (VillagerHatModelKey key : VillagerHatModelKey.values()) {
            var id = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, key.assetId());
            // Ne pas utiliser Registry#get : l’entrée par défaut est minecraft:air → extensions sur tous les “air”.
            BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> event.registerItem(ext, item));
        }
    }

    @SubscribeEvent
    public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            var renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                playerRenderer.addLayer(new VillagerHatRenderLayer(playerRenderer, event.getEntityModels()));
            }
        }
    }

    /**
     * Armureaux et mobs humanoïdes : même mesh Undead Crown que le joueur ({@link VillagerHatMobRenderLayer}).
     */
    @SubscribeEvent
    public static void addMobHatLayers(EntityRenderersEvent.AddLayers event) {
        var models = event.getEntityModels();
        for (EntityType<?> type : vanillaMobHatEntityTypes()) {
            addMobHatLayerIfSupported(event, type, models);
        }
        addMobHatLayerIfSupported(event, ModEntities.SKELETON_VILLAGER.get(), models);
    }

    private static void addMobHatLayerIfSupported(EntityRenderersEvent.AddLayers event, EntityType<?> type, EntityModelSet models) {
        if (type == EntityType.PLAYER) {
            return;
        }
        var renderer = event.getRenderer(type);
        if (!(renderer instanceof LivingEntityRenderer livingRenderer)) {
            return;
        }
        EntityModel<?> model = livingRenderer.getModel();
        if (VillagerHatMobRenderLayer.resolveHead(model) == null) {
            return;
        }
        livingRenderer.addLayer(new VillagerHatMobRenderLayer(livingRenderer, models));
    }

    /** Types vanilla uniquement ; le villageois squelette du mod est ajouté après ({@link ModEntities#SKELETON_VILLAGER}), une fois les registres liés. */
    private static List<EntityType<?>> vanillaMobHatEntityTypes() {
        return List.of(
                EntityType.ARMOR_STAND,
                EntityType.ZOMBIE,
                EntityType.ZOMBIE_VILLAGER,
                EntityType.HUSK,
                EntityType.DROWNED,
                EntityType.SKELETON,
                EntityType.STRAY,
                EntityType.WITHER_SKELETON,
                EntityType.WITCH,
                EntityType.VINDICATOR,
                EntityType.EVOKER,
                EntityType.PILLAGER,
                EntityType.ILLUSIONER,
                EntityType.PIGLIN,
                EntityType.ZOMBIFIED_PIGLIN);
    }
}
