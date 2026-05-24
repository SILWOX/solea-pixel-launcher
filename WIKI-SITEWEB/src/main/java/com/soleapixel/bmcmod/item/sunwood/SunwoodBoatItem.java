package com.soleapixel.bmcmod.item.sunwood;

import com.soleapixel.bmcmod.entity.vehicle.SunwoodBoat;
import com.soleapixel.bmcmod.entity.vehicle.SunwoodChestBoat;
import com.soleapixel.bmcmod.registry.ModEntities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Comportement identique à {@link BoatItem} vanilla (clic droit, eau, collisions, stats…),
 * seules les entités posées sont {@link SunwoodBoat} / {@link SunwoodChestBoat} pour les textures custom.
 */
public final class SunwoodBoatItem extends BoatItem {
    private final boolean chestBoat;

    public SunwoodBoatItem(boolean chestBoat, Properties properties) {
        super(chestBoat, Boat.Type.OAK, properties);
        this.chestBoat = chestBoat;
    }

    @Override
    protected Boat getBoat(Level level, HitResult hit, ItemStack stack, Player player) {
        Vec3 pos = hit.getLocation();
        Boat boat = chestBoat
                ? new SunwoodChestBoat(ModEntities.SUNWOOD_CHEST_BOAT.get(), level)
                : new SunwoodBoat(ModEntities.SUNWOOD_BOAT.get(), level);
        boat.setPos(pos.x, pos.y, pos.z);
        boat.setOldPosAndRot();
        if (level instanceof ServerLevel server) {
            EntityType.createDefaultStackConfig(server, stack, player).accept(boat);
        }
        return boat;
    }
}
