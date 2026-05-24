package com.soleapixel.bmcmod.morph;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

/**
 * Synchronise la pose serveur (hitbox F3+B) avec le rendu : sneak → assis pour chats, renards, etc.
 */
public final class MorphPoseSync {
    private MorphPoseSync() {
    }

    public static void tick(ServerPlayer player) {
        if (!MorphPlayerState.isMorphed(player)) {
            return;
        }
        ResourceLocation id = MorphAppearanceIds.soulEntityId(MorphPlayerState.getMorphSoul(player));
        if (id == null) {
            return;
        }

        boolean eyeInWater = player.isEyeInFluid(FluidTags.WATER);
        if (MorphAppearanceIds.morphUsesAquaticSwimPose(id) && eyeInWater) {
            if (!player.isPassenger() && !player.isFallFlying() && !player.isShiftKeyDown()) {
                if (player.getPose() != Pose.SWIMMING) {
                    player.setPose(Pose.SWIMMING);
                }
            }
            return;
        }
        if (MorphAppearanceIds.morphUsesAquaticSwimPose(id) && player.getPose() == Pose.SWIMMING) {
            player.setPose(Pose.STANDING);
        }

        if (!MorphAppearanceIds.morphSupportsSitPose(id)) {
            return;
        }
        if (player.isPassenger() || player.isInWater() || player.isSwimming() || player.isFallFlying()) {
            return;
        }
        if (player.isShiftKeyDown()) {
            if (player.getPose() != Pose.SITTING) {
                player.setPose(Pose.SITTING);
            }
        } else if (player.getPose() == Pose.SITTING) {
            player.setPose(Pose.STANDING);
        }
    }

    public static void resetPose(Player player) {
        Pose pose = player.getPose();
        if (pose == Pose.SITTING || pose == Pose.SWIMMING) {
            player.setPose(Pose.STANDING);
        }
    }
}
