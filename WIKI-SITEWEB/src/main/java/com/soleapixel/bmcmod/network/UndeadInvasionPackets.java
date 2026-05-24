package com.soleapixel.bmcmod.network;

import com.soleapixel.bmcmod.BmcMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class UndeadInvasionPackets {
    private UndeadInvasionPackets() {
    }

    public record InvasionPopPayload(int level) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<InvasionPopPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "undead_invasion_pop"));
        public static final StreamCodec<RegistryFriendlyByteBuf, InvasionPopPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                InvasionPopPayload::level,
                InvasionPopPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}

