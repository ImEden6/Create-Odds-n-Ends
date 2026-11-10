package com.mervyn.createoddsnends.organ.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import com.mervyn.createoddsnends.organ.BarrelOrganBlockEntity;
import com.mervyn.createoddsnends.organ.Organ;
import com.mervyn.createoddsnends.organ.Score;
import com.mervyn.createoddsnends.organ.Timbre;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

// Client side of playback: plays the notes that start on each new row, holds and releases them,
// and keeps the facade pipes and the drum beater in step. updateOrgan() and onRow() in the preview.
public final class OrganPlayback {
    private static final int LOW_SAMPLE = 48, HIGH_SAMPLE = 72;
    private static final Map<BarrelOrganBlockEntity, List<OrganNoteSound>> HELD = new WeakHashMap<>();

    private OrganPlayback() {}

    public static void tick(BarrelOrganBlockEntity be, @Nullable Score score) {
        for (int i = 0; i < be.pipeLit.length; i++)
            be.pipeLit[i] = Math.max(0, be.pipeLit[i] - 0.05f * 3);
        List<OrganNoteSound> held = HELD.computeIfAbsent(be, k -> new ArrayList<>());
        held.removeIf(OrganNoteSound::isStopped);
        int rows = score == null ? 0 : score.rowCount();
        int cur = score == null || be.isFinished() ? -1 : Math.min(rows - 1, (int) Math.floor(be.shownRow(0)));
        if (!be.isRunning() || cur < 0) {
            held.forEach(OrganNoteSound::release);
            be.lastRow = be.shownRow(0) == 0 ? -1 : cur;
            return;
        }
        if (cur != be.lastRow) {
            int from = be.lastRow, step = Math.floorMod(cur - from, rows), near = (int) Math.ceil(be.rowsPerTick()) + 1;
            if (from < 0 || step <= near || Math.floorMod(from - cur, rows) > near) {
                if (from < 0 || step > near)
                    onRow(be, score, cur, held);
                else
                    for (int k = 1; k <= step; k++)
                        onRow(be, score, (from + k) % rows, held);
                be.lastRow = cur;
            }
        }
        if (be.gridScore != score) {
            be.gridScore = score;
            be.grid = score.grid();
        }
        int[] grid = be.grid;
        for (int c = 2; c < Score.COLUMNS; c++) {
            int cell = grid[cur * Score.COLUMNS + c];
            if (cell == 0)
                continue;
            Timbre t = Timbre.of(cell - 1, c);
            if (t == Timbre.MELODY || t == Timbre.CHORD)
                be.pipeLit[(c * 7) % 9] = Math.max(be.pipeLit[(c * 7) % 9], 0.7f);
        }
    }

    // onRow(): every note starting on this row, held ones don't retrigger
    private static void onRow(BarrelOrganBlockEntity be, Score score, int row, List<OrganNoteSound> held) {
        BlockPos pos = be.getBlockPos();
        List<Score.Note> notes = score.notes();
        for (int i = score.firstAt(row); i < notes.size() && notes.get(i).row() == row; i++) {
            Score.Note note = notes.get(i);
            Timbre t = Timbre.of(note.voice(), note.column());
            if (t.drum()) {
                be.drumHit = be.getLevel().getGameTime();
                be.getLevel().playLocalSound(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5,
                        (t == Timbre.KICK ? Organ.KICK : Organ.SNARE).get(), SoundSource.RECORDS, 1, 1, false);
                continue;
            }
            if (t == Timbre.MELODY || t == Timbre.CHORD)
                be.pipeLit[(note.column() * 7) % 9] = 1;
            boolean low = note.midi() < 60;
            float pitch = (float) Math.pow(2, (note.midi() - (low ? LOW_SAMPLE : HIGH_SAMPLE)) / 12.0);
            int ticks = Math.max(1, Math.round(note.length() / be.rowsPerTick()));
            OrganNoteSound sound = new OrganNoteSound(sample(t, low), pos, pitch, ticks);
            held.add(sound);
            Minecraft.getInstance().getSoundManager().play(sound);
        }
    }

    private static SoundEvent sample(Timbre t, boolean low) {
        return switch (t) {
            case MELODY -> (low ? Organ.MELODY_LOW : Organ.MELODY_HIGH).get();
            case BASS -> (low ? Organ.BASS_LOW : Organ.BASS_HIGH).get();
            default -> (low ? Organ.CHORD_LOW : Organ.CHORD_HIGH).get();
        };
    }

    public static void stop(BarrelOrganBlockEntity be) {
        List<OrganNoteSound> held = HELD.remove(be);
        if (held != null)
            held.forEach(OrganNoteSound::release);
    }
}
