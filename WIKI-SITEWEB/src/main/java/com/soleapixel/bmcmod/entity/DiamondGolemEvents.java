package com.soleapixel.bmcmod.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.SpawnPlacements;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.registry.ModEntities;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class DiamondGolemEvents {
    private DiamondGolemEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.DIAMOND_GOLEM.get(), DiamondGolem.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                ModEntities.DIAMOND_GOLEM.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SpawnPlacements::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public static void onPumpkinPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState placed = event.getPlacedBlock();
        if (!DiamondGolemFormation.isPumpkinHead(placed)) {
            return;
        }
        DiamondGolemFormation.trySpawnDiamondGolem(level, event.getPos());
    }

    @SubscribeEvent
    public static void onOwnerHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        for (DiamondGolem golem : player.level().getEntitiesOfClass(DiamondGolem.class, player.getBoundingBox().inflate(32.0D))) {
            if (golem.isCompanion() && golem.getOwnerUUID().filter(uuid -> uuid.equals(player.getUUID())).isPresent()) {
                golem.setTarget(attacker);
            }
        }
    }
}
