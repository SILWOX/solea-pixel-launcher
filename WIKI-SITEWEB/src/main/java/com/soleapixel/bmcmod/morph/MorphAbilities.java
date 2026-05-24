package com.soleapixel.bmcmod.morph;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * Effets partiels selon l’âme stockée (burn zombie, TP enderman, vol pour mobs aériens, etc.).
 */
public final class MorphAbilities {
    private MorphAbilities() {
    }

    public static void resetFlight(ServerPlayer player) {
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    public static void tick(ServerPlayer player) {
        CompoundTag soul = MorphPlayerState.getMorphSoul(player);
        if (soul.isEmpty()) {
            return;
        }
        ResourceLocation id = MorphAppearanceIds.soulEntityId(soul);
        if (id == null) {
            return;
        }
        String path = id.getPath();
        Level level = player.level();
        RandomSource rnd = player.getRandom();

        if (path.contains("zombie") || path.equals("drowned") || path.equals("husk")) {
            if (level.isDay()
                    && !player.isInPowderSnow
                    && level.canSeeSky(BlockPos.containing(player.getEyePosition()))) {
                player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 20));
            }
        }

        if (path.equals("skeleton") || path.equals("stray") || path.equals("bogged")) {
            if (level.isDay() && level.canSeeSky(BlockPos.containing(player.getEyePosition()))) {
                player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 20));
            }
        }

        if (path.equals("enderman") && rnd.nextInt(160) == 0) {
            randomTeleport(player, rnd);
        }

        if (MorphAppearanceIds.morphGrantsSurvivalFlight(id)) {
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        } else {
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    private static void randomTeleport(ServerPlayer player, RandomSource rnd) {
        double dx = (rnd.nextDouble() - 0.5) * 32;
        double dz = (rnd.nextDouble() - 0.5) * 32;
        double x = player.getX() + dx;
        double z = player.getZ() + dz;
        int y = Mth.floor(player.getY());
        BlockPos target = new BlockPos(Mth.floor(x), y, Mth.floor(z));
        if (player.level() instanceof ServerLevel sl) {
            for (int dy = 0; dy < 8; dy++) {
                BlockPos p = target.below(dy);
                if (sl.getBlockState(p).isCollisionShapeFullBlock(sl, p)) {
                    player.teleportTo(sl, x + 0.5, p.getY() + 1.1, z + 0.5, Set.of(), player.getYRot(), player.getXRot());
                    return;
                }
            }
        }
    }
}
