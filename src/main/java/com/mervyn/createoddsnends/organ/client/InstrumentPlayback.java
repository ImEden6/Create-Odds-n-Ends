package com.mervyn.createoddsnends.organ.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import com.mervyn.createoddsnends.organ.Instrument;
import com.mervyn.createoddsnends.organ.InstrumentBlockEntity;
import com.mervyn.createoddsnends.organ.Score;

import net.minecraft.world.item.DyeColor;

// Notes the organ hands to an instrument on one of its chests. Each instrument holds at most
// MAX_VOICES at once, a new note past that cuts the oldest one off.
public final class InstrumentPlayback {
    private static final Map<InstrumentBlockEntity, List<OrganNoteSound>> HELD = new WeakHashMap<>();

    private InstrumentPlayback() {}

    public static void play(InstrumentBlockEntity be, @Nullable DyeColor filter, Score.Note note, float rowsPerTick) {
        Instrument instrument = be.instrument();
        if (!instrument.plays(note, filter))
            return;
        int ticks = Math.max(1, Math.round(note.length() / rowsPerTick));
        be.strike(instrument.drum() ? 0 : ticks);
        List<OrganNoteSound> held = HELD.computeIfAbsent(be, k -> new ArrayList<>());
        held.removeIf(OrganNoteSound::isStopped);
        if (held.size() >= Instrument.MAX_VOICES)
            held.remove(0).release();
        OrganNoteSound sound = OrganPlayback.sound(be.getLevel(), be.getBlockPos(), instrument.timbre, note, rowsPerTick);
        if (sound != null)
            held.add(sound);
    }
}
