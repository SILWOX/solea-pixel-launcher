package com.soleapixel.bmcmod.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Particule visuelle pour l'effet Undead Invasion ; même sprite réutilisable ailleurs via
 * {@link com.soleapixel.bmcmod.registry.ModParticles#UNDEAD_INVASION}.
 */
public final class UndeadInvasionParticle extends TextureSheetParticle {
    private UndeadInvasionParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites) {
        super(level, x, y, z);
        this.setSpriteFromAge(sprites);
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.lifetime = 28 + this.random.nextInt(14);
        float s = 0.38F + this.random.nextFloat() * 0.22F;
        this.scale(s);
        this.friction = 0.96F;
        this.xd *= 0.08F;
        this.yd *= 0.08F;
        this.zd *= 0.08F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed) {
            return new UndeadInvasionParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
