package com.soleapixel.bmcmod.morph;

import java.util.UUID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;

public final class MorphCrystalPackets {
    private MorphCrystalPackets() {
    }

    /** Serveur → clients : rendu métamorphose (âme complète pour le proxy). */
    public record MorphDisguisePayload(UUID playerId, boolean active, net.minecraft.nbt.CompoundTag soulOrEmpty)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MorphDisguisePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "morph_disguise"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MorphDisguisePayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG,
                p -> p.playerId.getMostSignificantBits(),
                ByteBufCodecs.VAR_LONG,
                p -> p.playerId.getLeastSignificantBits(),
                ByteBufCodecs.BOOL,
                MorphDisguisePayload::active,
                ByteBufCodecs.TRUSTED_COMPOUND_TAG,
                MorphDisguisePayload::soulOrEmpty,
                (msb, lsb, active, soul) -> new MorphDisguisePayload(new UUID(msb, lsb), active, soul));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Cooldown morph (ticks restants) pour la barre d’action. */
    public record MorphCooldownPayload(int ticksLeft) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MorphCooldownPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "morph_cooldown"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MorphCooldownPayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, MorphCooldownPayload::ticksLeft, MorphCooldownPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
