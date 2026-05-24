package com.soleapixel.bmcmod.client.villagerhat;

import java.util.function.Supplier;

import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;

/**
 * Identifiant de texture / mesh aligné sur les fichiers {@code bmcmod_asset/textures/hat villager}.
 */
public enum VillagerHatModelKey {
    BUTCHER("butcher_headband", VillagerHatMesh::butcherHeadband),
    LIBRARIAN("librarian_hat", VillagerHatMesh::librarianHat),
    WEAPONSMITH("weaponsmith_eyepatch", VillagerHatMesh::weaponsmithEyepatch),
    SHEPHERD("shepherd_hat", VillagerHatMesh::shepherdHat),
    FISHERMAN("fisherman_hat", VillagerHatMesh::fishermanHat),
    CARTOGRAPHER("cartographer_monocle", VillagerHatMesh::cartographerMonocle),
    ARMORER("armorer_goggles", VillagerHatMesh::armorerGoggles),
    FARMER("farmer_hat", VillagerHatMesh::farmerHat),
    WITCH("witch_hat", VillagerHatMesh::witchHat),
    UNDEAD_CROWN("undead_crown", VillagerHatMesh::undeadCrown);

    private final String assetId;
    private final Supplier<LayerDefinition> mesh;
    private final ModelLayerLocation layerLocation;
    private final ResourceLocation entityTexture;

    VillagerHatModelKey(String assetId, Supplier<LayerDefinition> mesh) {
        this.assetId = assetId;
        this.mesh = mesh;
        this.layerLocation = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "villager_hat/" + assetId), "main");
        this.entityTexture = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "textures/entity/villager_hat/" + assetId + ".png");
    }

    public String assetId() {
        return assetId;
    }

    public ModelLayerLocation layer() {
        return layerLocation;
    }

    public Supplier<LayerDefinition> mesh() {
        return mesh;
    }

    public ResourceLocation entityTexture() {
        return entityTexture;
    }

    public ResourceLocation itemTexture() {
        return ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "item/villager_hat/" + assetId);
    }
}
