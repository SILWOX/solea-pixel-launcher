package com.soleapixel.bmcmod.network;

import com.soleapixel.bmcmod.BmcMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class UndeadTotemPackets {
    private UndeadTotemPackets() {
    }

    public record SummonClonesPayload(boolean slimModel) implements CustomPacketPayload {
        public static final Type<SummonClonesPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "undead_totem_summon_clones"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SummonClonesPayload> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeBoolean(payload.slimModel()),
                buf -> new SummonClonesPayload(buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
