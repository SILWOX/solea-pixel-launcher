package com.soleapixel.bmcmod.client;

import com.mojang.datafixers.util.Pair;

import com.soleapixel.bmcmod.BmcMod;

import net.minecraft.client.model.ListModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;

/** Textures comme vanilla : {@code textures/entity/(chest_)boat/<bois>.png} via {@link ResourceLocation#withPrefix}. */
public final class SunwoodBoatRenderer extends BoatRenderer {
    private static final ResourceLocation BOAT_TEXTURE = texture(BmcMod.MODID, "sunwood", false);
    private static final ResourceLocation CHEST_BOAT_TEXTURE = texture(BmcMod.MODID, "sunwood", true);

    private static ResourceLocation texture(String namespace, String woodName, boolean chestBoat) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, woodName);
        String prefix = chestBoat ? "textures/entity/chest_boat/" : "textures/entity/boat/";
        return id.withPrefix(prefix).withSuffix(".png");
    }

    private final boolean chestBoat;

    public SunwoodBoatRenderer(EntityRendererProvider.Context context, boolean chestBoat) {
        super(context, chestBoat);
        this.chestBoat = chestBoat;
    }

    @Override
    public Pair<ResourceLocation, ListModel<Boat>> getModelWithLocation(Boat boat) {
        Pair<ResourceLocation, ListModel<Boat>> base = super.getModelWithLocation(boat);
        ResourceLocation tex = chestBoat ? CHEST_BOAT_TEXTURE : BOAT_TEXTURE;
        return Pair.of(tex, base.getSecond());
    }
}
