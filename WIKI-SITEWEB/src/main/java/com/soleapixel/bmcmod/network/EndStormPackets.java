package com.soleapixel.bmcmod.network;

import com.soleapixel.bmcmod.BmcMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class EndStormPackets {
    private EndStormPackets() {
    }

    public record EndStormPopPayload(int level) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<EndStormPopPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "end_storm_pop"));
        public static final StreamCodec<RegistryFriendlyByteBuf, EndStormPopPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                EndStormPopPayload::level,
                EndStormPopPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}

