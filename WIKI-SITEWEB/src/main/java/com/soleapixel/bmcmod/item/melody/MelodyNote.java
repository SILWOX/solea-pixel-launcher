package com.soleapixel.bmcmod.item.melody;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

/**
 * Une note de bloc-note (instrument + hauteur 0–24 + volume relatif, défaut 1).
 * Le champ {@code volume} permet des nuances façon vélocité MIDI (0,05–3,0).
 */
public record MelodyNote(NoteBlockInstrument instrument, int note, float volume) {
    public MelodyNote(NoteBlockInstrument instrument, int note) {
        this(instrument, note, 1.0F);
    }

    public MelodyNote {
        note = Math.max(0, Math.min(24, note));
        volume = Mth.clamp(volume, 0.05F, 3.0F);
    }
}
