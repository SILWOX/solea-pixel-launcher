package com.soleapixel.bmcmod.villager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import com.soleapixel.bmcmod.BmcMod;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class WitchCuringEvents {
    public static final String TAG_CURED_WITCH_LOCK = "BmcModCuredWitch";
    private static final String TAG_WITCH_CONVERT_TICKS = "BmcModWitchConversionTicks";

    private WitchCuringEvents() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Witch witch)) {
            return;
        }
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.GOLDEN_APPLE)) {
            return;
        }
        if (!witch.hasEffect(MobEffects.WEAKNESS)) {
            return;
        }
        if (witch.getPersistentData().getInt(TAG_WITCH_CONVERT_TICKS) > 0) {
            return;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        int duration = 2400 + level.getRandom().nextIntBetweenInclusive(0, 2400);
        witch.getPersistentData().putInt(TAG_WITCH_CONVERT_TICKS, duration);
        witch.playSound(SoundEvents.ZOMBIE_VILLAGER_CURE, 1.0F, 1.0F);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onWitchTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Witch witch)) {
            return;
        }
        if (witch.level().isClientSide()) {
            return;
        }
        int ticks = witch.getPersistentData().getInt(TAG_WITCH_CONVERT_TICKS);
        if (ticks <= 0) {
            return;
        }
        ticks--;
        witch.getPersistentData().putInt(TAG_WITCH_CONVERT_TICKS, ticks);
        if (witch.level() instanceof ServerLevel serverLevel && witch.tickCount % 10 == 0) {
            serverLevel.sendParticles(
                    ParticleTypes.ENCHANT,
                    witch.getX(),
                    witch.getY() + witch.getBbHeight() * 0.5,
                    witch.getZ(),
                    6,
                    0.2,
                    0.4,
                    0.2,
                    0.5);
        }
        if (ticks <= 0) {
            finishWitchConversion((ServerLevel) witch.level(), witch);
        }
    }

    private static void finishWitchConversion(ServerLevel level, Witch witch) {
        BlockPos pos = BlockPos.containing(witch.getX(), witch.getY(), witch.getZ());
        VillagerType type = VillagerType.byBiome(level.getBiome(witch.blockPosition()));
        Villager villager = (Villager) EntityType.VILLAGER.spawn(
                level, null, pos, MobSpawnType.CONVERSION, true, false);
        if (villager == null) {
            return;
        }
        villager.setYRot(witch.getYRot());
        villager.setXRot(witch.getXRot());
        villager.setVillagerData(new VillagerData(type, ModProfessions.CURED_WITCH.get(), 1));
        villager.getPersistentData().putBoolean(TAG_CURED_WITCH_LOCK, true);
        villager.setVillagerXp(0);
        witch.discard();
        // Re-assert after EntityType.spawn + finalizeSpawn: keep profession and tag in sync.
        villager.getPersistentData().putBoolean(TAG_CURED_WITCH_LOCK, true);
        villager.setVillagerData(new VillagerData(type, ModProfessions.CURED_WITCH.get(), 1));
        villager.refreshBrain(level);
        level.playSound(null, villager.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, villager.getSoundSource(), 1.0F, 1.0F);
    }

}
