package com.soleapixel.bmcmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;

public final class SkyBootsPackets {
    private SkyBootsPackets() {
    }

    /**
     * Client → serveur : double saut. {@code forwardDash} = le joueur marchait / strafait / sprintait au moment du
     * second saut (sinon boost vertical seul, sans dash regard).
     */
    public record SkyDoubleJumpPayload(boolean forwardDash) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SkyDoubleJumpPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "sky_double_jump"));
        /** Encodage explicite (évite tout piège d’inférence sur {@code composite} à un seul champ). */
        public static final StreamCodec<RegistryFriendlyByteBuf, SkyDoubleJumpPayload> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeBoolean(payload.forwardDash()),
                buf -> new SkyDoubleJumpPayload(buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
