package com.soleapixel.bmcmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;

/** Cycle des modes du Wither Staff (Ctrl + clic droit). */
public final class WitherStaffPackets {
    private WitherStaffPackets() {
    }

    public record WitherStaffCycleModePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<WitherStaffCycleModePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "wither_staff_cycle_mode"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WitherStaffCycleModePayload> CODEC =
                StreamCodec.unit(new WitherStaffCycleModePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
