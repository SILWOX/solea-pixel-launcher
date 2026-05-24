package com.soleapixel.bmcmod.network;

import com.soleapixel.bmcmod.BmcMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Toggle du mode Excavator (Ctrl + clic droit). */
public final class ExcavatorPackets {
    private ExcavatorPackets() {
    }

    public record ExcavatorToggleModePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ExcavatorToggleModePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "excavator_toggle_mode"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ExcavatorToggleModePayload> CODEC =
                StreamCodec.unit(new ExcavatorToggleModePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
