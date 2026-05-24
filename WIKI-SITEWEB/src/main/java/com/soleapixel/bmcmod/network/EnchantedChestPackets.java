package com.soleapixel.bmcmod.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;

/**
 * Synchronise l’XP (dixièmes) du coffre enchanté vers le client. Complète {@code getUpdateTag} (parfois peu fiable pour le visuel en direct).
 */
public final class EnchantedChestPackets {
    private EnchantedChestPackets() {
    }

    public record XpStatePayload(int x, int y, int z, long storedTenths, long lastPeriodTenths, int upgradeCount) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<XpStatePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "enchanted_chest_xp"));
        public static final StreamCodec<RegistryFriendlyByteBuf, XpStatePayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                XpStatePayload::x,
                ByteBufCodecs.VAR_INT,
                XpStatePayload::y,
                ByteBufCodecs.VAR_INT,
                XpStatePayload::z,
                ByteBufCodecs.VAR_LONG,
                XpStatePayload::storedTenths,
                ByteBufCodecs.VAR_LONG,
                XpStatePayload::lastPeriodTenths,
                ByteBufCodecs.VAR_INT,
                XpStatePayload::upgradeCount,
                XpStatePayload::new);

        public BlockPos getPos() {
            return new BlockPos(this.x, this.y, this.z);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
