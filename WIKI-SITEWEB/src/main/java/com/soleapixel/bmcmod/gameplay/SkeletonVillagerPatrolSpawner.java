package com.soleapixel.bmcmod.gameplay;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.ServerGameplayConfig;
import com.soleapixel.bmcmod.entity.SkeletonVillager;
import com.soleapixel.bmcmod.registry.ModEntities;
import com.soleapixel.bmcmod.util.UndeadPotionItems;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class SkeletonVillagerPatrolSpawner {
    public static final String PATROL_ID_TAG = "bmcmod_sv_patrol_id";
    public static final String PATROL_CAPTAIN_TAG = "bmcmod_sv_patrol_captain";

    private SkeletonVillagerPatrolSpawner() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ServerGameplayConfig.svPatrolEnabled()) {
            return;
        }
        int interval = ServerGameplayConfig.svPatrolTickInterval();
        if (interval <= 0 || event.getServer().getTickCount() % interval != 0) {
            return;
        }
        ServerLevel overworld = event.getServer().overworld();
        if (overworld.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        float chance = ServerGameplayConfig.svPatrolSpawnChancePerPlayer();
        int minG = ServerGameplayConfig.svPatrolGroupSizeMin();
        int maxG = Math.max(minG, ServerGameplayConfig.svPatrolGroupSizeMax());
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD || sl != overworld) {
                continue;
            }
            if (!player.isAlive() || player.isSpectator() || player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
                continue;
            }
            if (sl.random.nextFloat() > chance) {
                continue;
            }
            if (sl.getEntitiesOfClass(
                    SkeletonVillager.class,
                    player.getBoundingBox().inflate(72.0D),
                    e -> e.getPersistentData().hasUUID(PATROL_ID_TAG)).size() >= 8) {
                continue;
            }
            int groupSize = Mth.randomBetweenInclusive(sl.random, minG, maxG);
            trySpawnPatrol(sl, player, groupSize);
        }
    }

    private static void trySpawnPatrol(ServerLevel level, ServerPlayer player, int groupSize) {
        double ang = level.random.nextDouble() * Math.PI * 2.0D;
        double dist = 32.0D + level.random.nextDouble() * 16.0D;
        int cx = Mth.floor(player.getX() + Math.cos(ang) * dist);
        int cz = Mth.floor(player.getZ() + Math.sin(ang) * dist);
        int cy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
        BlockPos chunkProbe = new BlockPos(cx, cy, cz);
        if (!level.hasChunksAt(chunkProbe.offset(-10, 0, -10), chunkProbe.offset(10, 0, 10))) {
            return;
        }
        BlockPos center = new BlockPos(cx, cy, cz);
        if (!level.getWorldBorder().isWithinBounds(center)) {
            return;
        }
        UUID patrolId = UUID.randomUUID();
        ItemStack banner = UndeadInvasionManager.createUndeadInvasionBannerStack(level.registryAccess());
        for (int i = 0; i < groupSize; i++) {
            double ring = (Math.PI * 2.0D * i) / groupSize;
            int x = cx + Mth.floor(Math.cos(ring) * 2.5D);
            int z = cz + Mth.floor(Math.sin(ring) * 2.5D);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getWorldBorder().isWithinBounds(pos)) {
                continue;
            }
            SkeletonVillager sv = ModEntities.SKELETON_VILLAGER.get().create(level);
            if (sv == null) {
                continue;
            }
            sv.moveTo(x + 0.5D, y, z + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            sv.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            sv.getPersistentData().putUUID(PATROL_ID_TAG, patrolId);
            boolean captain = i == 0;
            sv.getPersistentData().putBoolean(PATROL_CAPTAIN_TAG, captain);
            if (captain) {
                sv.setItemSlot(EquipmentSlot.MAINHAND, banner.copy());
            }
            sv.setPersistenceRequired();
            level.addFreshEntity(sv);
        }
    }

    @SubscribeEvent
    public static void onPatrolMemberDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof SkeletonVillager victim)) {
            return;
        }
        if (!victim.getPersistentData().hasUUID(PATROL_ID_TAG)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        ServerLevel level = killer.serverLevel();
        if (level != victim.level()) {
            return;
        }
        UUID patrolId = victim.getPersistentData().getUUID(PATROL_ID_TAG);
        if (countLivingPatrolMates(level, victim, patrolId) > 0) {
            return;
        }
        if (level.random.nextFloat() > ServerGameplayConfig.svPatrolFullWipePotionChance()) {
            return;
        }
        int tier = 1 + level.random.nextInt(3);
        ItemStack bottle = new ItemStack(UndeadPotionItems.bottleForTier(tier));
        if (!killer.getInventory().add(bottle)) {
            killer.drop(bottle, false);
        }
    }

    private static int countLivingPatrolMates(ServerLevel level, SkeletonVillager victim, UUID patrolId) {
        AABB box = victim.getBoundingBox().inflate(192.0D);
        int n = 0;
        for (SkeletonVillager e : level.getEntitiesOfClass(SkeletonVillager.class, box)) {
            if (!e.isAlive()) {
                continue;
            }
            if (e.getUUID().equals(victim.getUUID())) {
                continue;
            }
            if (!e.getPersistentData().hasUUID(PATROL_ID_TAG)) {
                continue;
            }
            if (patrolId.equals(e.getPersistentData().getUUID(PATROL_ID_TAG))) {
                n++;
            }
        }
        return n;
    }
}
