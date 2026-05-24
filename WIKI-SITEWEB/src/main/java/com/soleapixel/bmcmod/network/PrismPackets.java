package com.soleapixel.bmcmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;

public final class PrismPackets {
    private PrismPackets() {
    }

    /**
     * État de la réserve Prisme (cœurs bleu) pour le rendu côté client.
     * {@code current} et {@code max} sont en points de vie (0,5 cœur = 1,0 point).
     */
    public record PrismSyncPayload(float current, float max) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PrismSyncPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "prism_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PrismSyncPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT,
                PrismSyncPayload::current,
                ByteBufCodecs.FLOAT,
                PrismSyncPayload::max,
                PrismSyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
