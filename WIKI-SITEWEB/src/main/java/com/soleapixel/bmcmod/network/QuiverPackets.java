package com.soleapixel.bmcmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;

/** Client → serveur : cycle du type de flèche actif sur le carquois principal. */
public final class QuiverPackets {
    private QuiverPackets() {
    }

    public record CycleSelectedTypePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CycleSelectedTypePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "quiver_cycle_type"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CycleSelectedTypePayload> CODEC =
                StreamCodec.unit(new CycleSelectedTypePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
