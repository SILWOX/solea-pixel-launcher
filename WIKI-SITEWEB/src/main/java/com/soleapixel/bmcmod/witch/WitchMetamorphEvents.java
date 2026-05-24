package com.soleapixel.bmcmod.witch;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.registry.ModItems;
import com.soleapixel.bmcmod.witch.WitchMetamorphPackets;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class WitchMetamorphEvents {
    private WitchMetamorphEvents() {
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot() != EquipmentSlot.HEAD || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        boolean hadWitchHat = event.getFrom().is(ModItems.VILLAGER_HAT_WITCH.get());
        boolean hasWitchHat = event.getTo().is(ModItems.VILLAGER_HAT_WITCH.get());
        if (hadWitchHat && !hasWitchHat) {
            WitchMetamorphServer.onWitchHatUnequipped(player);
        } else if (!hadWitchHat && hasWitchHat) {
            WitchMetamorphServer.syncHud(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        WitchMetamorphServer.tickPlayer(player);
    }

    /**
     * Force la cible des {@link Mob} qui pourraient attaquer une sorcière (même logique que {@link EntityType#WITCH}
     * pour {@link Mob#canAttackType}), car l’IA vanilla ne considère pas le joueur comme une sorcière.
     */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel sl)) {
            return;
        }
        if ((sl.getGameTime() & 19) != 0) {
            return;
        }
        for (ServerPlayer sp : sl.players()) {
            if (!WitchMetamorphServer.isTransformed(sp)) {
                continue;
            }
            for (Mob mob : sl.getEntitiesOfClass(Mob.class, sp.getBoundingBox().inflate(24))) {
                if (mob.getTarget() != null && mob.getTarget() != sp) {
                    continue;
                }
                if (!mob.canAttackType(EntityType.WITCH)) {
                    continue;
                }
                if (!mob.canAttack(sp)) {
                    continue;
                }
                mob.setTarget(sp);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        WitchMetamorphServer.onPlayerLogout(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joined)) {
            return;
        }
        MinecraftServer server = joined.getServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (WitchMetamorphServer.isTransformed(other)) {
                PacketDistributor.sendToPlayer(joined, new WitchMetamorphPackets.WitchDisguisePayload(other.getUUID(), true));
            }
        }
        WitchMetamorphServer.syncHud(joined);
    }

    private static Player resolveAttackingPlayer(Entity entity) {
        if (entity instanceof Player p) {
            return p;
        }
        if (entity instanceof Projectile proj && proj.getOwner() instanceof Player p) {
            return p;
        }
        return null;
    }
}
