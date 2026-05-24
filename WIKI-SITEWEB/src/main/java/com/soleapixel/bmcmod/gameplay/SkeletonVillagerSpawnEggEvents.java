package com.soleapixel.bmcmod.gameplay;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.SkeletonVillager;
import com.soleapixel.bmcmod.registry.ModEntities;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class SkeletonVillagerSpawnEggEvents {
    private static final ThreadLocal<int[]> SPAWN_DEPTH = ThreadLocal.withInitial(() -> new int[] { 0, 0 });

    private SkeletonVillagerSpawnEggEvents() {
    }

    public static void beginSpawn(boolean soulVariant) {
        int[] depths = SPAWN_DEPTH.get();
        depths[soulVariant ? 1 : 0]++;
    }

    public static void endSpawn() {
        int[] depths = SPAWN_DEPTH.get();
        if (depths[0] > 0) {
            depths[0]--;
        }
        if (depths[1] > 0) {
            depths[1]--;
        }
    }

    private static boolean isSoulPending() {
        return SPAWN_DEPTH.get()[1] > 0;
    }

    private static boolean isNormalPending() {
        return SPAWN_DEPTH.get()[0] > 0;
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof SkeletonVillager sv)) {
            return;
        }
        if (sv.getSpawnType() == MobSpawnType.SPAWN_EGG) {
            if (isSoulPending()) {
                sv.setSoulVariant(true);
                sv.setMimicked(false);
                sv.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                sv.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                sv.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                sv.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                sv.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
                sv.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
                sv.setCanPickUpLoot(false);
            } else if (isNormalPending()) {
                sv.setSoulVariant(false);
                sv.setMimicked(false);
            }
        }
    }

    /** Spawn naturel biome-dépendant : plus rare qu'un squelette vanilla. */
    @SubscribeEvent
    public static void onNaturalSkeletonJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getEntity() instanceof Skeleton skeleton) || event.getEntity() instanceof SkeletonVillager) {
            return;
        }
        MobSpawnType spawnType = skeleton.getSpawnType();
        if (spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION) {
            return;
        }
        float chance = replacementChance(level, skeleton.blockPosition());
        if (chance <= 0.0F || level.random.nextFloat() > chance) {
            return;
        }
        SkeletonVillager replacement = ModEntities.SKELETON_VILLAGER.get().create(level);
        if (replacement == null) {
            return;
        }
        replacement.moveTo(skeleton.getX(), skeleton.getY(), skeleton.getZ(), skeleton.getYRot(), skeleton.getXRot());
        replacement.finalizeSpawn(level, level.getCurrentDifficultyAt(replacement.blockPosition()), spawnType, null);
        level.addFreshEntity(replacement);
        skeleton.discard();
    }

    /**
     * Vanilla skeletons hors surface en plein jour (grottes, etc.). On aligne là-dessus : pas de blocage jour
     * si peu ou pas de lumière du ciel au point de spawn.
     */
    private static boolean allowOverworldSkeletonReplacement(ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return false;
        }
        if (!level.isDay()) {
            return true;
        }
        // Jour surface ensoleillée : aucun squelette naturel vanilla ici → pas de remplacement.
        return level.getBrightness(LightLayer.SKY, pos) < 8;
    }

    private static float replacementChance(ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (!allowOverworldSkeletonReplacement(level, pos)) {
            return 0.0F;
        }

        int sections = level.sectionsToVillage(SectionPos.of(pos));
        // sectionsToVillage : plus petit = plus près du village (voir ServerLevel#setDefaultSpawnPos).
        boolean veryNearVillage = sections >= 0 && sections <= 3;
        boolean nearVillage = sections >= 0 && sections <= 7;

        // Un peu plus rare que de croiser encore un squelette vanilla : quelques conversions par poignée de squelettes.
        float chance = 0.075F;
        if (veryNearVillage) {
            chance += 0.09F;
        } else if (nearVillage) {
            chance += 0.045F;
        }

        return Mth.clamp(chance, 0.0F, 0.22F);
    }
}
