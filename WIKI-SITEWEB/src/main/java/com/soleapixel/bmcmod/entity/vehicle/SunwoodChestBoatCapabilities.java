package com.soleapixel.bmcmod.entity.vehicle;

import com.soleapixel.bmcmod.registry.ModEntities;

import net.minecraft.world.Container;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/**
 * Même enregistrement que {@link net.neoforged.neoforge.capabilities.CapabilityHooks#registerVanillaProviders}
 * pour {@code EntityType.CHEST_BOAT} : sans cela, hopper / pipes ne voient pas l’inventaire du bateau coffre custom.
 */
public final class SunwoodChestBoatCapabilities {
    private SunwoodChestBoatCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        var type = ModEntities.SUNWOOD_CHEST_BOAT.get();
        event.registerEntity(Capabilities.ItemHandler.ENTITY, type, (entity, ctx) -> new InvWrapper((Container) entity));
        event.registerEntity(Capabilities.ItemHandler.ENTITY_AUTOMATION, type, (entity, ctx) -> new InvWrapper((Container) entity));
    }
}
