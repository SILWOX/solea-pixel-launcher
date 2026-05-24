package com.soleapixel.bmcmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;

/**
 * Cycle du mode de l’End Staff (déclenché côté client avec Ctrl + clic droit).
 */
public final class EndStaffPackets {
    private EndStaffPackets() {
    }

    public record EndStaffCycleModePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<EndStaffCycleModePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "end_staff_cycle_mode"));
        public static final StreamCodec<RegistryFriendlyByteBuf, EndStaffCycleModePayload> CODEC =
                StreamCodec.unit(new EndStaffCycleModePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
