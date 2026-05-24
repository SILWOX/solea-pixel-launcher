package com.soleapixel.bmcmod.witch;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.phys.Vec3;

/**
 * Effet visuel : particules le long du trajet de la sorcière vers le joueur.
 */
public final class WitchSoulParticles {
    private WitchSoulParticles() {
    }

    public static void spawn(ServerPlayer killer, Witch witch) {
        if (!(killer.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 from = witch.position().add(0.0, witch.getBbHeight() * 0.6, 0.0);
        Vec3 to = killer.position().add(0.0, killer.getBbHeight() * 0.85, 0.0);
        Vec3 delta = to.subtract(from);
        int steps = 24;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 p = from.add(delta.scale(t));
            level.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 2, 0.04, 0.06, 0.04, 0.002);
            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.SOUL, p.x, p.y, p.z, 1, 0.02, 0.04, 0.02, 0.01);
            }
        }
    }
}
