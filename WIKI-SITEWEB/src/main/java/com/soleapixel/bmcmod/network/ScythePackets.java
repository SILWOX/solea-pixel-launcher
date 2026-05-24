package com.soleapixel.bmcmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.soleapixel.bmcmod.BmcMod;

/** Ctrl + clic droit : cycle zone ; tourbillon (charge client puis attaque) ; S2C : animation spin. */
public final class ScythePackets {
    private ScythePackets() {
    }

    public record ScytheCycleAreaPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ScytheCycleAreaPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "scythe_cycle_area"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ScytheCycleAreaPayload> CODEC =
                StreamCodec.unit(new ScytheCycleAreaPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client → serveur : id de l’entité vivante visée au moment du déclenchement. */
    public record ScytheSweepAttackPayload(int primaryEntityId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ScytheSweepAttackPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "scythe_sweep_attack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ScytheSweepAttackPayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, ScytheSweepAttackPayload::primaryEntityId, ScytheSweepAttackPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client → serveur : le joueur maintient la charge du tourbillon (bloque labour au clic droit). */
    public record ScytheWhirlwindChargingPayload(boolean active) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ScytheWhirlwindChargingPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "scythe_whirlwind_charging"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ScytheWhirlwindChargingPayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, ScytheWhirlwindChargingPayload::active, ScytheWhirlwindChargingPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Serveur → clients : animation toupie (durée totale + nombre de tours 360°).
     */
    public record ScytheSpinVisualPayload(int playerEntityId, int totalSpinTicks, int fullRotations) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ScytheSpinVisualPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "scythe_spin_visual"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ScytheSpinVisualPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                ScytheSpinVisualPayload::playerEntityId,
                ByteBufCodecs.VAR_INT,
                ScytheSpinVisualPayload::totalSpinTicks,
                ByteBufCodecs.VAR_INT,
                ScytheSpinVisualPayload::fullRotations,
                ScytheSpinVisualPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
