package com.soleapixel.bmcmod.client;

import com.soleapixel.bmcmod.registry.ModParticles;

import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

public final class ModParticleProviders {
    private ModParticleProviders() {
    }

    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.UNDEAD_INVASION.get(), UndeadInvasionParticle.Provider::new);
        event.registerSpriteSet(ModParticles.END_STORM.get(), EndStormParticle.Provider::new);
    }
}
