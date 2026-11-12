package com.mervyn.createoddsnends.organ;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.DyeColor;

// What sits on a wind chest instead of a whistle. Drums answer their own lane of the score and
// ignore the chest's filter, the pitched ones play every pitched note the filter lets through.
public enum Instrument {
    KICK_DRUM(Timbre.KICK),
    SNARE_DRUM(Timbre.SNARE),
    REED_PIPE(Timbre.MELODY),
    WOOD_PIPE(Timbre.BASS),
    REED_BOX(Timbre.CHORD);

    public static final int MAX_VOICES = 6;

    public final Timbre timbre;

    Instrument(Timbre timbre) {
        this.timbre = timbre;
    }

    public boolean drum() {
        return timbre.drum();
    }

    public boolean plays(Score.Note note, @Nullable DyeColor filter) {
        if (this == KICK_DRUM)
            return note.column() == 0;
        if (this == SNARE_DRUM)
            return note.column() == 1;
        if (filter != null && note.voice() != filter.getId())
            return false;
        return note.column() >= 2 && !Timbre.of(note.voice(), note.column()).drum();
    }
}
