package com.soleapixel.bmcmod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.client.Minecraft;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.block.chest.EnchantedChestBlockEntity;
import com.soleapixel.bmcmod.network.EnchantedChestPackets;
import com.soleapixel.bmcmod.network.EndStormPackets;
import com.soleapixel.bmcmod.network.PrismPackets;
import com.soleapixel.bmcmod.network.ScythePackets;
import com.soleapixel.bmcmod.network.ShulkerArmorPackets;
import com.soleapixel.bmcmod.network.UndeadInvasionPackets;
import com.soleapixel.bmcmod.morph.MorphCrystalPackets;
import com.soleapixel.bmcmod.registry.ModItems;
import com.soleapixel.bmcmod.witch.WitchMetamorphPackets;

@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class BmcModClientNetworking {
    private BmcModClientNetworking() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("shulker_s2c").playToClient(
                ShulkerArmorPackets.ShulkerSyncPayload.TYPE,
                ShulkerArmorPackets.ShulkerSyncPayload.CODEC,
                BmcModClientNetworking::handleSync);
        event.registrar("witch_s2c").playToClient(
                WitchMetamorphPackets.WitchDisguisePayload.TYPE,
                WitchMetamorphPackets.WitchDisguisePayload.CODEC,
                BmcModClientNetworking::handleWitchDisguise);
        event.registrar("witch_hud_s2c").playToClient(
                WitchMetamorphPackets.WitchHudPayload.TYPE,
                WitchMetamorphPackets.WitchHudPayload.CODEC,
                BmcModClientNetworking::handleWitchHud);
        event.registrar("prism_s2c").playToClient(
                PrismPackets.PrismSyncPayload.TYPE,
                PrismPackets.PrismSyncPayload.CODEC,
                BmcModClientNetworking::handlePrismSync);
        event.registrar("enchanted_chest_s2c").playToClient(
                EnchantedChestPackets.XpStatePayload.TYPE,
                EnchantedChestPackets.XpStatePayload.CODEC,
                BmcModClientNetworking::handleEnchantedChestXp);
        event.registrar("morph_s2c").playToClient(
                MorphCrystalPackets.MorphDisguisePayload.TYPE,
                MorphCrystalPackets.MorphDisguisePayload.CODEC,
                BmcModClientNetworking::handleMorphDisguise);
        event.registrar("morph_cd_s2c").playToClient(
                MorphCrystalPackets.MorphCooldownPayload.TYPE,
                MorphCrystalPackets.MorphCooldownPayload.CODEC,
                BmcModClientNetworking::handleMorphCooldown);
        event.registrar("undead_invasion_s2c").playToClient(
                UndeadInvasionPackets.InvasionPopPayload.TYPE,
                UndeadInvasionPackets.InvasionPopPayload.CODEC,
                BmcModClientNetworking::handleUndeadInvasionPop);
        event.registrar("end_storm_s2c").playToClient(
                EndStormPackets.EndStormPopPayload.TYPE,
                EndStormPackets.EndStormPopPayload.CODEC,
                BmcModClientNetworking::handleEndStormPop);
        event.registrar("scythe_s2c").playToClient(
                ScythePackets.ScytheSpinVisualPayload.TYPE,
                ScythePackets.ScytheSpinVisualPayload.CODEC,
                BmcModClientNetworking::handleScytheSpinVisual);
    }

    private static void handleSync(ShulkerArmorPackets.ShulkerSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ShulkerArmorHud.setSyncedState(payload.charges(), payload.ticksUntilNextCharge()));
    }

    private static void handleWitchDisguise(WitchMetamorphPackets.WitchDisguisePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> WitchMetamorphClient.setDisguised(payload.playerId(), payload.disguised()));
    }

    private static void handleWitchHud(WitchMetamorphPackets.WitchHudPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> WitchMetamorphClient.setHudState(payload.transformTicksLeft(), payload.cooldownTicksLeft()));
    }

    private static void handlePrismSync(PrismPackets.PrismSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PrismClientState.set(payload.current(), payload.max()));
    }

    private static void handleMorphDisguise(MorphCrystalPackets.MorphDisguisePayload payload, IPayloadContext context) {
        context.enqueueWork(
                () -> MorphVisualClient.setMorphVisual(payload.playerId(), payload.active(), payload.soulOrEmpty()));
    }

    private static void handleMorphCooldown(MorphCrystalPackets.MorphCooldownPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> MorphVisualClient.setCooldownHud(payload.ticksLeft()));
    }

    private static void handleEnchantedChestXp(EnchantedChestPackets.XpStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }
            if (!(mc.level.getBlockEntity(payload.getPos()) instanceof EnchantedChestBlockEntity be)) {
                return;
            }
            be.applyClientXpSync(payload.storedTenths(), payload.lastPeriodTenths(), payload.upgradeCount());
        });
    }

    private static void handleUndeadInvasionPop(UndeadInvasionPackets.InvasionPopPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameRenderer == null) {
                return;
            }
            mc.gameRenderer.displayItemActivation(new net.minecraft.world.item.ItemStack(ModItems.UNDEAD_INVASION_POP_ICON.get()));
        });
    }

    private static void handleScytheSpinVisual(ScythePackets.ScytheSpinVisualPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getId() == payload.playerEntityId()) {
                return;
            }
            ScytheSpinVisualTracker.beginSpin(payload.playerEntityId(), payload.totalSpinTicks(), payload.fullRotations());
        });
    }

    private static void handleEndStormPop(EndStormPackets.EndStormPopPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameRenderer == null) {
                return;
            }
            mc.gameRenderer.displayItemActivation(new net.minecraft.world.item.ItemStack(ModItems.END_STORM_POP_ICON.get()));
        });
    }
}
