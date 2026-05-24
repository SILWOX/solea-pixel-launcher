package com.soleapixel.bmcmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;

public final class BuilderWandPackets {
    private BuilderWandPackets() {
    }

    public record BuilderWandCycleModePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BuilderWandCycleModePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "builder_wand_cycle_mode"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BuilderWandCycleModePayload> CODEC =
                StreamCodec.unit(new BuilderWandCycleModePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Ctrl + molette : fait tourner l’orientation du placement dans le plan de la face (±1 par cran). */
    public record BuilderWandAdjustPlacementTurnPayload(int delta) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BuilderWandAdjustPlacementTurnPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "builder_wand_adjust_placement"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BuilderWandAdjustPlacementTurnPayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.INT, BuilderWandAdjustPlacementTurnPayload::delta, BuilderWandAdjustPlacementTurnPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Maj + Ctrl + clic droit : cycle le bloc de construction parmi les {@link BlockItem} présents dans l’inventaire. */
    public record BuilderWandCycleMaterialPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BuilderWandCycleMaterialPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "builder_wand_cycle_material"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BuilderWandCycleMaterialPayload> CODEC =
                StreamCodec.unit(new BuilderWandCycleMaterialPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}

