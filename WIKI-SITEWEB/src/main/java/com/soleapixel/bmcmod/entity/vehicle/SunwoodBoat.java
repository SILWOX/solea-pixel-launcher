package com.soleapixel.bmcmod.entity.vehicle;

import com.soleapixel.bmcmod.registry.ModItems;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class SunwoodBoat extends Boat {
    public SunwoodBoat(EntityType<? extends Boat> entityType, Level level) {
        super(entityType, level);
        // Requis pour tick / contrôle / modèle vanilla ; la texture reste celle du renderer Sunwood.
        this.setVariant(Boat.Type.OAK);
    }

    @Override
    public Item getDropItem() {
        return ModItems.SUNWOOD_BOAT.get();
    }
}
