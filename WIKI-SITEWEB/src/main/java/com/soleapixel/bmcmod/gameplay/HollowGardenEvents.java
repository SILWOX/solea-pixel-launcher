package com.soleapixel.bmcmod.gameplay;

import com.soleapixel.bmcmod.registry.ModFluids;
import com.soleapixel.bmcmod.registry.ModItems;
import com.soleapixel.bmcmod.worldgen.ModBiomes;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Hollow Garden : cache biome + disque Beyond The Enderman (30 % si l’enderman meurt et le joueur tueur baigne dans le liquide d’XP dans ce biome).
 */
public final class HollowGardenEvents {

    private HollowGardenEvents() {
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ModBiomes.clearBiomeHolders();
    }

    /**
     * Après chargement / reload des datapacks, les tags (et le registre) peuvent changer ; on rafraîchit les
     * {@link net.minecraft.core.Holder} biome du mod pour les mixins multi-noise. Voir
     * {@link TagsUpdatedEvent#shouldUpdateStaticData()}.
     */
    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (!event.shouldUpdateStaticData()) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ModBiomes.cacheBiomeHolders(server);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof EnderMan ender) || ender.level().isClientSide()) {
            return;
        }
        Level level = ender.level();
        if (!level.getBiome(ender.blockPosition()).is(ModBiomes.HOLLOW_GARDEN)) {
            return;
        }
        DamageSource source = event.getSource();
        if (source == null || !(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getFluidTypeHeight(ModFluids.EXPERIENCE_FLUID_TYPE.get()) <= 0.18) {
            return;
        }
        if (level.getRandom().nextFloat() >= 0.30f) {
            return;
        }
        ItemStack disc = new ItemStack(ModItems.MUSIC_DISC_BEYOND_THE_ENDERMAN.get());
        event.getDrops().add(new ItemEntity(level, ender.getX(), ender.getY(), ender.getZ(), disc));
    }
}
