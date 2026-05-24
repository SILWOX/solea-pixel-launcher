package com.soleapixel.bmcmod.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class EndStormParticle extends TextureSheetParticle {
    private EndStormParticle(
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
        this.lifetime = 24 + this.random.nextInt(18);
        float s = 0.34F + this.random.nextFloat() * 0.30F;
        this.scale(s);
        this.friction = 0.95F;
        this.xd = xSpeed * 0.10F;
        this.yd = ySpeed * 0.10F;
        this.zd = zSpeed * 0.10F;
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
            return new EndStormParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}

