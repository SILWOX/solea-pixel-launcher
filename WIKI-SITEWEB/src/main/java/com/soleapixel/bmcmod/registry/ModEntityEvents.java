package com.soleapixel.bmcmod.registry;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.BountyHunterEntity;
import com.soleapixel.bmcmod.entity.Blink;
import com.soleapixel.bmcmod.entity.CloneEntity;
import com.soleapixel.bmcmod.entity.Dummy;
import com.soleapixel.bmcmod.entity.EndGolem;
import com.soleapixel.bmcmod.entity.Endling;
import com.soleapixel.bmcmod.entity.MimicChest;
import com.soleapixel.bmcmod.entity.QuestTrader;
import com.soleapixel.bmcmod.entity.RadiantSlime;
import com.soleapixel.bmcmod.entity.SkeletonVillager;
import com.soleapixel.bmcmod.entity.UndeadIllager;
import com.soleapixel.bmcmod.entity.Vlinx;
import com.soleapixel.bmcmod.gameplay.RadiantSlimeSpawnLogic;
import com.soleapixel.bmcmod.worldgen.ModBiomes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class ModEntityEvents {
    private ModEntityEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.BLINK.get(), Blink.createAttributes().build());
        event.put(ModEntities.ENDLING.get(), Endling.createAttributes().build());
        event.put(ModEntities.MIMIC_CHEST.get(), MimicChest.createAttributes().build());
        event.put(ModEntities.BOUNTY_HUNTER.get(), BountyHunterEntity.createAttributes().build());
        event.put(ModEntities.END_GOLEM.get(), EndGolem.createAttributes().build());
        event.put(ModEntities.SKELETON_VILLAGER.get(), SkeletonVillager.createAttributes().build());
        event.put(ModEntities.UNDEAD_ILLAGER.get(), UndeadIllager.createAttributes().build());
        event.put(ModEntities.VLINX.get(), Vlinx.createAttributes().build());
        event.put(ModEntities.QUEST_TRADER.get(), QuestTrader.createAttributes().build());
        // Custom ArmorStand-like entity still needs attributes registered.
        event.put(ModEntities.DUMMY.get(), ArmorStand.createAttributes().build());
        event.put(ModEntities.CLONE.get(), CloneEntity.createAttributes().build());
        // Comme les slimes vanilla (hostiles) : MAX_HEALTH / MOVEMENT_SPEED / ATTACK_DAMAGE requis dès Slime#setSize.
        event.put(ModEntities.RADIANT_SLIME.get(), Monster.createMonsterAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                ModEntities.BLINK.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModEntityEvents::checkBlinkSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                ModEntities.RADIANT_SLIME.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModEntityEvents::checkRadiantSlimeSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                ModEntities.SKELETON_VILLAGER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                ModEntities.QUEST_TRADER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                ModEntities.UNDEAD_ILLAGER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                ModEntities.VLINX.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                ModEntities.MIMIC_CHEST.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                ModEntities.BOUNTY_HUNTER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SuppressWarnings("unused")
    private static boolean checkBlinkSpawn(EntityType<Blink> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).isSolid();
    }

    /**
     * Sans entrée dans {@link RegisterSpawnPlacementsEvent}, l’œuf et les spawns scriptés échouent.
     * Œuf / commande / spawner : sol, liquide d’XP, ou au moins un appui ; sinon règles mob vanilla.
     */
    @SuppressWarnings("unused")
    private static boolean checkRadiantSlimeSpawn(EntityType<RadiantSlime> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        if (reason == MobSpawnType.SPAWN_EGG
                || reason == MobSpawnType.DISPENSER
                || reason == MobSpawnType.COMMAND
                || reason == MobSpawnType.SPAWNER
                || reason == MobSpawnType.MOB_SUMMONED) {
            BlockPos below = pos.below();
            BlockState ground = level.getBlockState(below);
            if (ground.blocksMotion()) {
                return true;
            }
            // Chemins en terre battue, dalles, etc. : pas « solide » pour blocksMotion() mais avec collision.
            if (!ground.getCollisionShape(level, below).isEmpty()) {
                return true;
            }
            return RadiantSlimeSpawnLogic.isExperienceFluid(level.getFluidState(pos))
                    || RadiantSlimeSpawnLogic.isExperienceFluid(level.getFluidState(below));
        }
        if (level.getBiome(pos).is(ModBiomes.HOLLOW_GARDEN)) {
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
                return false;
            }
            BlockPos below = pos.below();
            BlockState ground = level.getBlockState(below);
            boolean soil = ground.is(ModBlocks.HOLLOW_GRASS.get()) || ground.is(Blocks.END_STONE)
                    || ground.is(Blocks.CALCITE) || ground.is(ModBlocks.END_SAND.get());
            if (soil && random.nextInt(3) == 0) {
                return true;
            }
            return (RadiantSlimeSpawnLogic.isExperienceFluid(level.getFluidState(pos))
                    || RadiantSlimeSpawnLogic.isExperienceFluid(level.getFluidState(below)))
                    && random.nextInt(2) == 0;
        }
        return Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }
}
