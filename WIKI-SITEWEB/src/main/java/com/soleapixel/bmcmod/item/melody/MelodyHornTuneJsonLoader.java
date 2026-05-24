package com.soleapixel.bmcmod.item.melody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import com.soleapixel.bmcmod.BmcMod;

final class MelodyHornTuneJsonLoader {
    private MelodyHornTuneJsonLoader() {
    }

    /**
     * Loads the fixed catalog in {@link MelodyHornTunes#TUNE_IDS} order from {@code bmcmod:melody_horn/tunes/<id>.json},
     * then appends any other {@code melody_horn/tunes/*.json} from datapacks (not in the core id list).
     */
    static void loadCatalog(ResourceManager resourceManager, List<MelodyHornTune> out) {
        for (int i = 0; i < MelodyHornTunes.TUNE_IDS.length; i++) {
            String id = MelodyHornTunes.TUNE_IDS[i];
            ResourceLocation fileLoc = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "melody_horn/tunes/" + id + ".json");
            ResourceLocation tuneId = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, id);
            Optional<Resource> res = resourceManager.getResource(fileLoc);
            if (res.isPresent()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.get().open(), StandardCharsets.UTF_8))) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    out.add(parseTune(tuneId, json));
                } catch (IOException | JsonParseException e) {
                    BmcMod.LOGGER.warn("Melody Horn: failed to load tune {}: {}", tuneId, e.getMessage());
                    out.add(fallbackTune(i));
                }
            } else {
                out.add(fallbackTune(i));
            }
        }
        appendExtraTunes(resourceManager, out);
    }

    private static MelodyHornTune fallbackTune(int index) {
        String id = MelodyHornTunes.TUNE_IDS[index];
        ResourceLocation bid = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "builtin/" + id);
        return MelodyHornTune.fromBuiltinRow(
                bid,
                MelodyHornTunes.fallbackRow(index),
                MelodyHornTunes.BUILTIN_TICKS_PER_STEP,
                Optional.of("bmcmod.melody_horn.tune." + id));
    }

    private static void appendExtraTunes(ResourceManager resourceManager, List<MelodyHornTune> out) {
        Set<String> core = MelodyHornTunes.coreTuneIdSet();
        String prefix = "melody_horn/tunes";
        Map<ResourceLocation, Resource> found = resourceManager.listResources(prefix, loc -> loc.getPath().endsWith(".json"));
        List<ResourceLocation> fileLocs = new ArrayList<>(found.keySet());
        fileLocs.sort(ResourceLocation::compareTo);
        for (ResourceLocation fileLoc : fileLocs) {
            if (BmcMod.MODID.equals(fileLoc.getNamespace()) && core.contains(tuneBasename(fileLoc))) {
                continue;
            }
            ResourceLocation tuneId = trimJsonSuffix(fileLoc);
            Resource resource = found.get(fileLoc);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                out.add(parseTune(tuneId, json));
            } catch (IOException | JsonParseException e) {
                BmcMod.LOGGER.warn("Melody Horn: failed to load extra tune {}: {}", tuneId, e.getMessage());
            }
        }
    }

    private static String tuneBasename(ResourceLocation fileLoc) {
        String p = fileLoc.getPath();
        String pre = "melody_horn/tunes/";
        if (!p.startsWith(pre)) {
            return "";
        }
        String base = p.substring(pre.length());
        if (base.endsWith(".json")) {
            base = base.substring(0, base.length() - ".json".length());
        }
        return base;
    }

    private static ResourceLocation trimJsonSuffix(ResourceLocation fileLoc) {
        String p = fileLoc.getPath();
        String base = p.substring("melody_horn/tunes/".length());
        if (base.endsWith(".json")) {
            base = base.substring(0, base.length() - ".json".length());
        }
        return ResourceLocation.fromNamespaceAndPath(fileLoc.getNamespace(), base);
    }

    private static MelodyHornTune parseTune(ResourceLocation tuneId, JsonObject json) {
        int loopLen = json.get("loop_length_ticks").getAsInt();
        Optional<String> translation = json.has("translation_key")
                ? Optional.of(json.get("translation_key").getAsString())
                : Optional.empty();
        JsonArray events = json.getAsJsonArray("events");
        Map<Integer, List<MelodyNote>> byTick = new HashMap<>();
        int maxT = -1;
        for (JsonElement el : events) {
            JsonObject o = el.getAsJsonObject();
            int t = o.get("t").getAsInt();
            maxT = Math.max(maxT, t);
            String ins = o.get("i").getAsString();
            int n = o.get("n").getAsInt();
            float vol = o.has("v") ? o.get("v").getAsFloat() : 1.0F;
            MelodyNote note = new MelodyNote(MelodyHornTune.parseInstrument(ins), n, vol);
            byTick.computeIfAbsent(t, k -> new ArrayList<>()).add(note);
        }
        int loop = Math.max(loopLen, maxT + 1);
        return MelodyHornTune.fromParsed(tuneId, loop, byTick, translation);
    }
}
