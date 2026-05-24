package com.soleapixel.bmcmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;

public final class ShulkerArmorPackets {
    private ShulkerArmorPackets() {
    }

    /** Demande de tir (client → serveur). */
    public record ShulkerShootPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ShulkerShootPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "shulker_shoot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ShulkerShootPayload> CODEC =
                StreamCodec.unit(new ShulkerShootPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * État des charges / rechargement pour le HUD (serveur → client).
     * {@code charges == -1} : masquer l’indicateur (pas de set complet).
     */
    public record ShulkerSyncPayload(int charges, int ticksUntilNextCharge) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ShulkerSyncPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "shulker_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ShulkerSyncPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                ShulkerSyncPayload::charges,
                ByteBufCodecs.VAR_INT,
                ShulkerSyncPayload::ticksUntilNextCharge,
                ShulkerSyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
