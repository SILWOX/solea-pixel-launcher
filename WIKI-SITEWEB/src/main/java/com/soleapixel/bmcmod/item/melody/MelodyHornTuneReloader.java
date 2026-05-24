package com.soleapixel.bmcmod.item.melody;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Reloads {@link MelodyHornTuneRegistry} when server data packs reload.
 */
public final class MelodyHornTuneReloader implements PreparableReloadListener {
    @Override
    public CompletableFuture<Void> reload(
            PreparableReloadListener.PreparationBarrier barrier,
            ResourceManager resourceManager,
            ProfilerFiller prepareProfiler,
            ProfilerFiller applyProfiler,
            Executor backgroundExecutor,
            Executor gameExecutor) {
        return CompletableFuture.runAsync(() -> MelodyHornTuneRegistry.reload(resourceManager), backgroundExecutor)
                .thenCompose(barrier::wait);
    }

    @Override
    public String getName() {
        return "MelodyHornTunes";
    }
}
