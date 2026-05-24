package com.soleapixel.bmcmod.registry;

import com.soleapixel.bmcmod.BmcMod;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Particules enregistrées côté jeu ; le rendu est branché côté client
 * (voir {@link com.soleapixel.bmcmod.client.UndeadInvasionParticle}).
 * Utiliser {@code ModParticles.UNDEAD_INVASION.get()} pour
 * {@code level.sendParticles(...)} ou tout autre effet.
 */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, BmcMod.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> UNDEAD_INVASION =
            PARTICLE_TYPES.register("undead_invasion", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> END_STORM =
            PARTICLE_TYPES.register("end_storm", () -> new SimpleParticleType(false));

    private ModParticles() {
    }
}
