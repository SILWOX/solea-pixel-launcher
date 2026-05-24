package com.soleapixel.bmcmod.entity.vehicle;

import com.soleapixel.bmcmod.registry.ModItems;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class SunwoodChestBoat extends ChestBoat {
    public SunwoodChestBoat(EntityType<? extends ChestBoat> entityType, Level level) {
        super(entityType, level);
        this.setVariant(Boat.Type.OAK);
    }

    @Override
    public Item getDropItem() {
        return ModItems.SUNWOOD_CHEST_BOAT.get();
    }
}
