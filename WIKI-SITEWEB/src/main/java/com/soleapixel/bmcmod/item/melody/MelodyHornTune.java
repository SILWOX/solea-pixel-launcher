package com.soleapixel.bmcmod.item.melody;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import com.soleapixel.bmcmod.BmcMod;

/**
 * One loopable horn tune: timed note-block events (several notes may share the same tick for chords).
 */
public final class MelodyHornTune {
    private final ResourceLocation id;
    private final int loopLengthTicks;
    private final Map<Integer, List<MelodyNote>> notesByTick;
    private final Optional<String> translationKey;

    private MelodyHornTune(
            ResourceLocation id,
            int loopLengthTicks,
            Map<Integer, List<MelodyNote>> notesByTick,
            Optional<String> translationKey) {
        this.id = id;
        this.loopLengthTicks = loopLengthTicks;
        this.notesByTick = notesByTick;
        this.translationKey = translationKey;
    }

    public ResourceLocation id() {
        return id;
    }

    public int loopLengthTicks() {
        return loopLengthTicks;
    }

    public List<MelodyNote> notesAt(int tick) {
        return notesByTick.getOrDefault(Math.floorMod(tick, loopLengthTicks), List.of());
    }

    public String translationKeyOrDefault() {
        return translationKey.orElseGet(() -> defaultTranslationKey(id));
    }

    public static String defaultTranslationKey(ResourceLocation tuneId) {
        String p = tuneId.getPath();
        if (BmcMod.MODID.equals(tuneId.getNamespace()) && p.startsWith("builtin/")) {
            p = p.substring("builtin/".length());
        }
        if (BmcMod.MODID.equals(tuneId.getNamespace())) {
            return "bmcmod.melody_horn.tune." + p.replace('/', '.');
        }
        return "bmcmod.melody_horn.tune." + tuneId.getNamespace() + "." + p.replace('/', '.');
    }

    /** Compact builtin row: one note every {@code spacing} ticks. */
    public static MelodyHornTune fromBuiltinRow(ResourceLocation id, MelodyNote[] row, int spacing, Optional<String> translationKey) {
        if (row.length == 0) {
            return new MelodyHornTune(id, 1, Map.of(0, List.of()), translationKey);
        }
        Map<Integer, List<MelodyNote>> map = new HashMap<>();
        for (int i = 0; i < row.length; i++) {
            int t = i * spacing;
            map.computeIfAbsent(t, k -> new ArrayList<>()).add(row[i]);
        }
        int loop = row.length * spacing;
        return new MelodyHornTune(id, loop, map, translationKey);
    }

    public static MelodyHornTune fromParsed(
            ResourceLocation id, int loopLengthTicks, Map<Integer, List<MelodyNote>> notesByTick, Optional<String> translationKey) {
        int maxT = notesByTick.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
        int loop = Math.max(loopLengthTicks, maxT + 1);
        Map<Integer, List<MelodyNote>> frozen = new HashMap<>();
        for (var e : notesByTick.entrySet()) {
            frozen.put(e.getKey(), List.copyOf(e.getValue()));
        }
        return new MelodyHornTune(id, loop, Collections.unmodifiableMap(frozen), translationKey);
    }

    public static NoteBlockInstrument parseInstrument(String raw) throws IllegalArgumentException {
        String s = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return NoteBlockInstrument.valueOf(s);
    }
}
