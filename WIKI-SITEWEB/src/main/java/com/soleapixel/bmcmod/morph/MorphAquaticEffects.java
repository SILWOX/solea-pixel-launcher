package com.soleapixel.bmcmod.morph;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;

/**
 * Respiration cohérente selon le mob : poissons / calmars sous l’eau, dauphin qui tient l’eau et sèche hors eau.
 */
public final class MorphAquaticEffects {
    private MorphAquaticEffects() {
    }

    public static void clear(Player player) {
        // Réserve si on ajoute des effets statiques ; l’air se rétablit naturellement hors morph.
    }

    public static void tick(ServerPlayer player) {
        if (!MorphPlayerState.isMorphed(player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        ResourceLocation id = MorphAppearanceIds.soulEntityId(MorphPlayerState.getMorphSoul(player));
        MorphAppearanceIds.AquaticKind kind = MorphAppearanceIds.aquaticKind(id);
        if (kind == MorphAppearanceIds.AquaticKind.NONE) {
            return;
        }
        boolean eyeInWater = player.isEyeInFluid(FluidTags.WATER);
        switch (kind) {
            case GILLS -> tickGilled(player, eyeInWater);
            case DOLPHIN -> tickDolphin(player, eyeInWater);
            default -> {
            }
        }
    }

    private static void tickGilled(ServerPlayer player, boolean eyeInWater) {
        if (eyeInWater) {
            player.setAirSupply(player.getMaxAirSupply());
        } else {
            int air = player.getAirSupply();
            player.setAirSupply(air - 5);
            if (player.getAirSupply() < -15 && player.tickCount % 10 == 0) {
                player.hurt(player.damageSources().dryOut(), 2.0F);
            }
        }
    }

    private static void tickDolphin(ServerPlayer player, boolean eyeInWater) {
        if (eyeInWater) {
            // Le joueur n’est pas un {@code WaterAnimal} vanilla : on refouille l’air sous l’eau pour éviter
            // la noyade « inversée » (air qui se vide uniquement immergé).
            int air = player.getAirSupply();
            int max = player.getMaxAirSupply();
            player.setAirSupply(Math.min(max, air + Math.max(8, max / 20)));
        } else if (player.tickCount % 40 == 0) {
            player.hurt(player.damageSources().dryOut(), 1.0F);
        }
    }
}
