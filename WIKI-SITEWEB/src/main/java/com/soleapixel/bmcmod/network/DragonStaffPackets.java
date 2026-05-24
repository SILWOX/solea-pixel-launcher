package com.soleapixel.bmcmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;

/** Cycle des modes du Dragon Staff (Ctrl + clic droit). */
public final class DragonStaffPackets {
    private DragonStaffPackets() {
    }

    public record DragonStaffCycleModePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<DragonStaffCycleModePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "dragon_staff_cycle_mode"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DragonStaffCycleModePayload> CODEC =
                StreamCodec.unit(new DragonStaffCycleModePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
