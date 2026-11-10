package com.mervyn.createoddsnends.jacquard.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.mervyn.createoddsnends.organ.Score;
import com.mervyn.createoddsnends.organ.Score.Note;

import net.minecraft.util.Mth;

final class MidiImport {
    static final int SKIP = -1;
    private static final int DRUM_CHANNEL = 9, KICK_LANE = 0, SNARE_LANE = 1;
    private static final int FIRST_PITCH_COLUMN = 2;
    private static final int[] DEFAULT_VOICES = {14, 13, 11, 1, 3, 5, 10, 4, 2, 6, 9, 0, 12};
    private static final int DRUM_VOICE = 15;

    static final class Group {
        final int track, channel;
        final String name;
        int count;
        int voice;

        Group(int track, int channel, String name) {
            this.track = track;
            this.channel = channel;
            this.name = name;
        }

        boolean drums() {
            return channel == DRUM_CHANNEL;
        }
    }

    record Result(int bars, int barLength, List<Note> notes, int droppedDrums, int barsInFile, int barsKept, int overflow, int rpm) {}

    final MidiFile file;
    final List<Group> groups = new ArrayList<>();
    private final Map<Integer, Group> byKey = new TreeMap<>();
    int rowsPerBeat = 4, startBar = 1, transpose;

    MidiImport(MidiFile file) {
        this.file = file;
        for (MidiFile.Note n : file.notes)
            byKey.computeIfAbsent(n.track() << 4 | n.channel(), k -> new Group(n.track(), n.channel(), file.trackNames.getOrDefault(n.track(), ""))).count++;
        int next = 0;
        for (Group g : byKey.values()) {
            g.voice = g.drums() ? DRUM_VOICE : DEFAULT_VOICES[next++ % DEFAULT_VOICES.length];
            groups.add(g);
        }
    }

    int barLength() {
        return Mth.clamp(Math.round(file.beats * rowsPerBeat * 4f / file.beatUnit), 1, Score.MAX_ROWS);
    }

    int rpm() {
        double rowsPerSecond = 60_000_000.0 / file.microsPerBeat / 60 * rowsPerBeat;
        return Mth.clamp((int) Math.round(rowsPerSecond * 16 / 16) * 16, ScorePanel.MIN_RPM, ScorePanel.MAX_RPM);
    }

    private Group group(MidiFile.Note n) {
        return byKey.get(n.track() << 4 | n.channel());
    }

    private int ticksToRow(long tick) {
        return (int) Math.round(tick * (double) rowsPerBeat / file.division);
    }

    int autoFit() {
        int best = 0, bestCount = -1;
        for (int t : searchOrder()) {
            int inside = 0;
            for (MidiFile.Note n : file.notes) {
                if (n.channel() == DRUM_CHANNEL || group(n).voice == SKIP)
                    continue;
                int column = n.key() + t - Score.LOW_MIDI;
                if (column >= FIRST_PITCH_COLUMN && column < Score.COLUMNS)
                    inside++;
            }
            if (inside > bestCount) {
                bestCount = inside;
                best = t;
            }
        }
        return best;
    }

    private static int[] searchOrder() {
        int[] order = new int[97];
        for (int i = 1, t = 1; i < order.length; t++) {
            order[i++] = t;
            order[i++] = -t;
        }
        return order;
    }

    Result convert() {
        int barLength = barLength(), offset = (startBar - 1) * barLength, dropped = 0;
        List<Note> raw = new ArrayList<>();
        int lastEnd = 0;
        for (MidiFile.Note n : file.notes) {
            int voice = group(n).voice;
            if (voice == SKIP)
                continue;
            int column;
            if (n.channel() == DRUM_CHANNEL) {
                column = switch (n.key()) {
                    case 35, 36 -> KICK_LANE;
                    case 38, 40 -> SNARE_LANE;
                    default -> -1;
                };
                if (column < 0) {
                    dropped++;
                    continue;
                }
            } else {
                column = n.key() + transpose - Score.LOW_MIDI;
                while (column < FIRST_PITCH_COLUMN)
                    column += 12;
                while (column >= Score.COLUMNS)
                    column -= 12;
            }
            int start = ticksToRow(n.start()) - offset, end = ticksToRow(n.end()) - offset;
            if (start < 0)
                continue;
            int length = Math.max(1, end - start);
            lastEnd = Math.max(lastEnd, start + length);
            raw.add(new Note(start, column, length, voice));
        }

        int barsInFile = Math.max(1, (lastEnd + barLength - 1) / barLength);
        int barsKept = Math.min(barsInFile, Score.MAX_ROWS / barLength);
        int cut = barsKept * barLength;
        raw.sort(Comparator.comparingInt(Note::row).thenComparingInt(Note::column));

        Note[] last = new Note[Score.COLUMNS];
        int[] lastAt = new int[Score.COLUMNS];
        List<Note> out = new ArrayList<>(raw.size());
        for (Note n : raw) {
            if (n.row() >= cut)
                continue;
            Note clipped = n.row() + n.length() > cut ? new Note(n.row(), n.column(), cut - n.row(), n.voice()) : n;
            Note before = last[n.column()];
            if (before != null && clipped.row() == before.row()) {
                out.set(lastAt[n.column()], clipped);
                last[n.column()] = clipped;
                continue;
            }
            if (before != null && clipped.row() < before.row() + before.length())
                out.set(lastAt[n.column()], new Note(before.row(), before.column(), Math.max(1, clipped.row() - before.row() - 1), before.voice()));
            lastAt[n.column()] = out.size();
            out.add(clipped);
            last[n.column()] = clipped;
        }
        int overflow = Math.max(0, out.size() - ScoreCanvas.MAX_NOTES);
        return new Result(barsKept, barLength, overflow > 0 ? new ArrayList<>(out.subList(0, ScoreCanvas.MAX_NOTES)) : out, dropped, barsInFile, barsKept, overflow, rpm());
    }
}
