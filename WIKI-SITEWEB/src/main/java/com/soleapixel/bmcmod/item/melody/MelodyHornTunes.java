package com.soleapixel.bmcmod.item.melody;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import com.soleapixel.bmcmod.BmcMod;

/**
 * Mélodies courtes « style » sagas / culture pop — uniquement des sons de blocs-notes Minecraft.
 * Des partitions JSON datapack (dossier data, sous-chemin melody_horn/tunes) permettent des séquences longues
 * et précises (comme un lecteur ABC/MIDI simplifié) ; ces lignes restent des reprises courtes intégrées au jar.
 */
public final class MelodyHornTunes {
    /** Ancien rythme des boucles courtes (une note toutes les N ticks). */
    public static final int BUILTIN_TICKS_PER_STEP = 3;
    /** Identifiants des 30 thèmes (ordre = roulette de la corne). */
    public static final String[] TUNE_IDS = {
            "game_of_thrones",
            "harry_potter",
            "lotr_shire",
            "star_wars",
            "imperial_march",
            "indiana_jones",
            "jurassic_park",
            "pirates",
            "avengers",
            "mission_impossible",
            "james_bond",
            "pink_panther",
            "simpsons",
            "jaws",
            "good_bad_ugly",
            "doctor_who",
            "stranger_things",
            "halloween",
            "mario",
            "zelda_lullaby",
            "frozen",
            "ghostbusters",
            "back_to_the_future",
            "superman",
            "rocky",
            "x_files",
            "sherlock",
            "lotr_rivendell",
            "star_wars_force",
            "got_dark",
    };

    private static final NoteBlockInstrument H = NoteBlockInstrument.HARP;
    private static final NoteBlockInstrument P = NoteBlockInstrument.PLING;
    private static final NoteBlockInstrument B = NoteBlockInstrument.BASS;
    private static final NoteBlockInstrument F = NoteBlockInstrument.FLUTE;
    private static final NoteBlockInstrument G = NoteBlockInstrument.GUITAR;
    private static final NoteBlockInstrument X = NoteBlockInstrument.XYLOPHONE;
    private static final NoteBlockInstrument C = NoteBlockInstrument.CHIME;
    private static final NoteBlockInstrument I = NoteBlockInstrument.IRON_XYLOPHONE;
    private static final NoteBlockInstrument T = NoteBlockInstrument.BIT;

    public static final MelodyNote[][] MELODIES = {
            // GoT opening: repeated high then stepwise minor fall.
            tune(H, 10, 10, 10, 7, 5, 3, 2, 3, 5, 7, 5),
            // Hedwig: minor 3rd up, resolve, arpeggio tail.
            tune(P, 12, 15, 12, 10, 12, 15, 17, 15, 12, 10, 12),
            // Shire: bouncy do-re-mi / pentatonic skip.
            tune(F, 12, 14, 15, 17, 19, 17, 15, 14, 12, 15, 12),
            // Star Wars fanfare: repeated tonic then fifth jump + hook.
            tune(H, 12, 12, 12, 19, 15, 12, 10, 8, 7, 8, 12),
            // Imperial: G G G Eb, answer phrase (minor).
            tune(B, 8, 8, 8, 5, 7, 8, 5, 3, 5, 8, 5),
            // Indiana: long rising then peak + step down.
            tune(G, 10, 12, 15, 17, 19, 20, 19, 17, 15, 12, 10),
            // Jurassic: slow minor third + gentle climb + resolution.
            tune(H, 10, 10, 12, 15, 19, 17, 15, 14, 12, 10, 8),
            // Pirates: minor arpeggio + rhythmic turn.
            tune(G, 12, 15, 17, 16, 14, 12, 10, 12, 15, 12, 10),
            // Avengers: short fanfare fifths + peak.
            tune(H, 12, 12, 15, 19, 17, 15, 12, 15, 19, 22, 19),
            // Mission Impossible: 5-4-3-2 syncopated cell.
            tune(X, 15, 15, 18, 15, 12, 10, 12, 15, 18, 15, 12),
            // Bond: chromatic / spy half-step climb.
            tune(B, 10, 11, 12, 14, 12, 10, 8, 10, 12, 14, 12),
            // Pink Panther: chromatic creep + plateau.
            tune(F, 15, 14, 13, 15, 17, 15, 13, 12, 15, 13, 12),
            // Simpsons: Lydian / bright opening intervals.
            tune(P, 12, 12, 14, 12, 15, 17, 12, 8, 10, 12, 15),
            // Jaws: semitone oscillation.
            tune(B, 6, 5, 6, 5, 6, 5, 6, 5, 6, 5, 6),
            // GBU: whistle / fourth + minor fall.
            tune(H, 10, 7, 10, 12, 10, 7, 5, 7, 10, 12, 10),
            // Doctor Who: tritone / rising minor motif.
            tune(T, 12, 10, 15, 18, 15, 12, 10, 12, 15, 18, 15),
            // Stranger Things: slow minor arpeggio in low register.
            tune(T, 10, 8, 5, 8, 10, 12, 10, 8, 10, 12, 15),
            // Halloween: stabbing minor seconds + climb.
            tune(H, 10, 7, 10, 7, 5, 3, 5, 7, 10, 7, 5),
            // Mario overworld: E E E C E G G (then step down).
            tune(P, 12, 12, 12, 8, 12, 15, 15, 12, 10, 8, 12),
            // Zelda lullaby: thirds + gentle descent.
            tune(H, 12, 15, 12, 10, 8, 7, 8, 12, 15, 12, 10),
            // Frozen “Let It Go” head: rise then fall.
            tune(F, 12, 15, 19, 17, 15, 12, 10, 12, 15, 17, 15),
            // Ghostbusters: syncopated fifth / call motif.
            tune(G, 10, 10, 12, 15, 17, 15, 12, 10, 12, 15, 12),
            // BTTF: bright fanfare outline.
            tune(H, 12, 15, 17, 19, 20, 19, 17, 15, 12, 15, 17),
            // Superman: noble rising fifths.
            tune(H, 10, 12, 15, 17, 19, 17, 15, 12, 10, 12, 15),
            // Rocky / Gonna Fly Now: anthem climb.
            tune(P, 12, 12, 15, 17, 19, 17, 15, 12, 10, 12, 15),
            // X-Files: narrow minor cell, repeated.
            tune(T, 13, 10, 15, 10, 13, 15, 13, 10, 15, 10, 13),
            // Sherlock: staccato detective motif.
            tune(I, 15, 12, 15, 17, 15, 12, 10, 12, 14, 12, 10),
            // Rivendell: wide arpeggio, ethereal.
            tune(C, 12, 15, 17, 20, 17, 15, 12, 10, 8, 12, 15),
            // Force theme: long notes, minor fall.
            tune(H, 15, 12, 10, 8, 7, 8, 10, 12, 15, 12, 10),
            // GoT dark: low pedal + minor answer.
            tune(B, 7, 7, 5, 3, 2, 3, 5, 7, 5, 3, 2),
    };

    static {
        if (TUNE_IDS.length != MELODIES.length) {
            throw new IllegalStateException("TUNE_IDS and MELODIES must stay the same length");
        }
    }

    private MelodyHornTunes() {
    }

    private static MelodyNote[] tune(NoteBlockInstrument instrument, int n0, int... rest) {
        MelodyNote[] out = new MelodyNote[1 + rest.length];
        out[0] = new MelodyNote(instrument, n0);
        for (int i = 0; i < rest.length; i++) {
            out[i + 1] = new MelodyNote(instrument, rest[i]);
        }
        return out;
    }

    /** Secours si le JSON datapack du thème est absent (ne devrait pas arriver avec le jar complet). */
    public static void addBuiltinTunes(List<MelodyHornTune> out, int ticksPerStep) {
        for (int i = 0; i < TUNE_IDS.length; i++) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "builtin/" + TUNE_IDS[i]);
            Optional<String> translation = Optional.of("bmcmod.melody_horn.tune." + TUNE_IDS[i]);
            out.add(MelodyHornTune.fromBuiltinRow(id, MELODIES[i], ticksPerStep, translation));
        }
    }

    public static MelodyNote[] fallbackRow(int index) {
        return MELODIES[index];
    }

    public static java.util.Set<String> coreTuneIdSet() {
        return java.util.Set.copyOf(Arrays.asList(TUNE_IDS));
    }
}
