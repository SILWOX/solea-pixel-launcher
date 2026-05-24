package com.soleapixel.bmcmod.witch;

import java.util.UUID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;

public final class WitchMetamorphPackets {
    private WitchMetamorphPackets() {
    }

    /** Client → serveur : tenter la métamorphose (touche M). */
    public record WitchTryTransformPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<WitchTryTransformPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "witch_try_transform"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WitchTryTransformPayload> CODEC =
                StreamCodec.unit(new WitchTryTransformPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Serveur → clients : état de déguisement (tous les joueurs qui voient l’entité).
     */
    public record WitchDisguisePayload(UUID playerId, boolean disguised) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<WitchDisguisePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "witch_disguise"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WitchDisguisePayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG,
                p -> p.playerId.getMostSignificantBits(),
                ByteBufCodecs.VAR_LONG,
                p -> p.playerId.getLeastSignificantBits(),
                ByteBufCodecs.BOOL,
                WitchDisguisePayload::disguised,
                (msb, lsb, disguised) -> new WitchDisguisePayload(new UUID(msb, lsb), disguised));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Serveur → joueur local : temps restant / cooldown pour le HUD. */
    public record WitchHudPayload(int transformTicksLeft, int cooldownTicksLeft) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<WitchHudPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "witch_hud"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WitchHudPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                WitchHudPayload::transformTicksLeft,
                ByteBufCodecs.VAR_INT,
                WitchHudPayload::cooldownTicksLeft,
                WitchHudPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
