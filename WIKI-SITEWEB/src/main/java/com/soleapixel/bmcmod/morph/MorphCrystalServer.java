package com.soleapixel.bmcmod.morph;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import com.soleapixel.bmcmod.item.MorphCrystalItem;
import com.soleapixel.bmcmod.registry.ModItems;

public final class MorphCrystalServer {
    /** Durée maximale d’une métamorphose (8 minutes, cristal). */
    public static final int MAX_MORPH_DURATION_TICKS = 8 * 60 * 20;

    private MorphCrystalServer() {
    }

    public static boolean isMorphed(Player player) {
        return MorphPlayerState.isMorphed(player);
    }

    /**
     * Métamorphose sans cristal (ex. {@code /metamorph}). {@code soul} doit contenir au moins {@code id} (registre entité).
     * Remplace une métamorphose déjà active ; ne consomme pas de cristal ni n’applique le cooldown morph.
     */
    public static void applyMorphFromCommand(ServerPlayer player, CompoundTag soul) {
        CompoundTag copy = soul.copy();
        if (!copy.contains("id", Tag.TAG_STRING)) {
            return;
        }
        forceEndMorph(player, true);
        MorphPlayerState.setMorphed(player, copy, player.level().getGameTime());
        MorphBodyAttributes.applyFromSoul(player, MorphPlayerState.getMorphSoul(player));
        broadcastMorph(player, true, MorphPlayerState.getMorphSoul(player));
        syncCooldownHud(player);
        sendMorphTimeLeftActionBar(player);
    }

    public static void tryCompleteMorph(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        if (isMorphed(player)) {
            return;
        }
        if (!MorphCrystalSoul.hasSoul(stack)) {
            return;
        }
        CompoundTag soul = MorphCrystalSoul.getSoul(stack).copy();
        MorphPlayerState.setMorphed(player, soul, player.level().getGameTime());
        MorphBodyAttributes.applyFromSoul(player, MorphPlayerState.getMorphSoul(player));
        MorphCrystalSoul.clearSoul(stack);
        int cost = MorphDurabilityCosts.costForSoulNbt(soul);
        applyDurabilityAfterMorph(player, stack, cost, hand);
        broadcastMorph(player, true, MorphPlayerState.getMorphSoul(player));
        syncCooldownHud(player);
    }

    private static void applyDurabilityAfterMorph(ServerPlayer player, ItemStack stack, int cost, InteractionHand hand) {
        if (!(stack.getItem() instanceof MorphCrystalItem)) {
            return;
        }
        int max = stack.getMaxDamage();
        int cur = stack.getDamageValue();
        int next = cur + cost;
        if (next >= max) {
            ItemStack cracked = new ItemStack(ModItems.CRACKED_CRYSTAL.get());
            player.setItemInHand(hand, cracked);
            player.connection.send(
                    new ClientboundSetActionBarTextPacket(
                            Component.translatable("message.bmcmod.morph_crystal.shattered")));
        } else {
            stack.setDamageValue(next);
        }
    }

    public static void forceEndMorph(ServerPlayer player, boolean clearVisual) {
        MorphPoseSync.resetPose(player);
        MorphMovementRules.clear(player);
        MorphAquaticEffects.clear(player);
        MorphPlayerState.clearMorph(player);
        MorphBodyAttributes.restore(player);
        MorphAbilities.resetFlight(player);
        if (clearVisual) {
            broadcastMorph(player, false, new CompoundTag());
        }
    }

    public static void broadcastMorph(ServerPlayer player, boolean active, CompoundTag soul) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new MorphCrystalPackets.MorphDisguisePayload(player.getUUID(), active, soul));
    }

    public static void syncCooldownHud(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new MorphCrystalPackets.MorphCooldownPayload(0));
    }

    /**
     * Mort du joueur métamorphosé : retire l’état morph et le rendu pour les autres clients, sans
     * {@link MorphBodyAttributes#restore} sur le corps (les PV / attributs sont réappliqués au respawn via {@link #onPlayerClone}).
     */
    public static void stripMorphOnDeath(ServerPlayer dying) {
        if (!MorphPlayerState.isMorphed(dying)) {
            return;
        }
        MorphMovementRules.clear(dying);
        MorphPoseSync.resetPose(dying);
        MorphAquaticEffects.clear(dying);
        MorphPlayerState.clearMorph(dying);
        broadcastMorph(dying, false, new CompoundTag());
        MorphAbilities.resetFlight(dying);
    }

    public static void tickPlayer(ServerPlayer player) {
        if (player.level().isClientSide()) {
            return;
        }
        if (MorphPlayerState.isMorphed(player)) {
            long gt = player.level().getGameTime();
            long since = MorphPlayerState.getMorphSinceGameTime(player);
            if (since == 0L) {
                player.getPersistentData().putLong(MorphPlayerState.TAG_MORPH_SINCE, gt);
                since = gt;
            }
            if (gt - since >= MAX_MORPH_DURATION_TICKS) {
                endMorphExpired(player);
            } else {
                MorphAbilities.tick(player);
                MorphAquaticEffects.tick(player);
                MorphPoseSync.tick(player);
            }
        }
        if (player.tickCount % 20 == 0) {
            if (MorphPlayerState.isMorphed(player)) {
                sendMorphTimeLeftActionBar(player);
            } else {
                syncCooldownHud(player);
            }
        }
    }

    /** Barre d’action : temps restant avant la fin forcée de la métamorphose (8 min). */
    public static void sendMorphTimeLeftActionBar(ServerPlayer player) {
        long gt = player.level().getGameTime();
        long since = MorphPlayerState.getMorphSinceGameTime(player);
        if (since == 0L) {
            return;
        }
        int elapsed = (int) Math.min(MAX_MORPH_DURATION_TICKS, gt - since);
        int remainingTicks = Math.max(0, MAX_MORPH_DURATION_TICKS - elapsed);
        int totalSec = remainingTicks / 20;
        int min = totalSec / 60;
        int sec = totalSec % 60;
        player.connection.send(
                new ClientboundSetActionBarTextPacket(
                        Component.translatable("hud.bmcmod.morph_time_left", min, sec)));
    }

    /** Fin volontaire : maintien clic droit avec le Morph Crystal (main principale ou secondaire). */
    public static void tryCompleteDemorph(ServerPlayer player) {
        if (!isMorphed(player)) {
            return;
        }
        forceEndMorph(player, true);
        syncCooldownHud(player);
    }

    private static void endMorphExpired(ServerPlayer player) {
        forceEndMorph(player, true);
        syncCooldownHud(player);
        player.connection.send(
                new ClientboundSetActionBarTextPacket(
                        Component.translatable("message.bmcmod.morph_crystal.expired")));
    }

    public static void onPlayerLogout(ServerPlayer player) {
        if (MorphPlayerState.isMorphed(player)) {
            broadcastMorph(player, false, new CompoundTag());
        }
        MorphMovementRules.clear(player);
        MorphPoseSync.resetPose(player);
        MorphAquaticEffects.clear(player);
        MorphPlayerState.clearMorph(player);
        MorphBodyAttributes.restore(player);
        MorphAbilities.resetFlight(player);
    }

    public static void onPlayerLogin(ServerPlayer joined) {
        var server = joined.getServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (MorphPlayerState.isMorphed(other)) {
                PacketDistributor.sendToPlayer(
                        joined,
                        new MorphCrystalPackets.MorphDisguisePayload(
                                other.getUUID(), true, MorphPlayerState.getMorphSoul(other)));
            }
        }
        if (MorphPlayerState.isMorphed(joined)) {
            var payload = new MorphCrystalPackets.MorphDisguisePayload(
                    joined.getUUID(), true, MorphPlayerState.getMorphSoul(joined));
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                PacketDistributor.sendToPlayer(p, payload);
            }
        }
        MorphPlayerState.migrateStoredMorphSoulIfNeeded(joined);
        MorphPlayerState.clearCooldown(joined);
        MorphBodyAttributes.handleLogin(joined);
        if (MorphPlayerState.isMorphed(joined)) {
            sendMorphTimeLeftActionBar(joined);
        } else {
            syncCooldownHud(joined);
        }
    }

    public static void onPlayerClone(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newP)) {
            return;
        }
        if (event.isWasDeath() && event.getOriginal() instanceof ServerPlayer oldP) {
            MorphPlayerState.clearMorph(newP);
            MorphBodyAttributes.restore(newP);
            MorphAbilities.resetFlight(newP);
            MorphPlayerState.clearCooldown(newP);
            broadcastMorph(newP, false, new CompoundTag());
        }
    }
}
