package com.soleapixel.bmcmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;

public final class IceStaffPackets {
    private IceStaffPackets() {
    }

    public record IceStaffCycleModePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<IceStaffCycleModePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "ice_staff_cycle_mode"));
        public static final StreamCodec<RegistryFriendlyByteBuf, IceStaffCycleModePayload> CODEC =
                StreamCodec.unit(new IceStaffCycleModePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
