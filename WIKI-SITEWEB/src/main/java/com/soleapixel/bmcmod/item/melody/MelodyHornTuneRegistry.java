package com.soleapixel.bmcmod.item.melody;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import com.soleapixel.bmcmod.BmcMod;

/**
 * Horn loops: fixed-order catalog from {@code bmcmod:melody_horn/tunes/*.json}, then optional datapack-only tunes.
 */
public final class MelodyHornTuneRegistry {
    private static volatile List<MelodyHornTune> tunes = List.of();

    private MelodyHornTuneRegistry() {
    }

    public static void reload(ResourceManager resourceManager) {
        List<MelodyHornTune> built = new ArrayList<>();
        MelodyHornTuneJsonLoader.loadCatalog(resourceManager, built);
        tunes = Collections.unmodifiableList(built);
        BmcMod.LOGGER.info("Melody Horn: loaded {} tune(s) (catalog + extras).", built.size());
    }

    public static int size() {
        return tunes.size();
    }

    public static MelodyHornTune get(int index) {
        List<MelodyHornTune> t = tunes;
        if (index < 0 || index >= t.size()) {
            return null;
        }
        return t.get(index);
    }

    public static String translationKey(int index) {
        MelodyHornTune tune = get(index);
        return tune == null ? "bmcmod.melody_horn.tune.unknown" : tune.translationKeyOrDefault();
    }

    /** Before first reload (should not happen); keep empty-safe. */
    public static void bootstrapIfEmpty(ResourceManager resourceManager) {
        if (tunes.isEmpty()) {
            reload(resourceManager);
        }
    }
}
