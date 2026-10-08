package com.mervyn.createoddsnends.jacquard.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A Standard MIDI File, format 0 or 1, read straight from the bytes: the notes with their ticks,
// the track names, and the first tempo and time signature.
final class MidiFile {
    record Note(int track, int channel, int key, long start, long end) {}

    final int division;
    final List<Note> notes = new ArrayList<>();
    final Map<Integer, String> trackNames = new HashMap<>();
    int beats = 4, beatUnit = 4;
    int microsPerBeat = 500000;
    private boolean sawTempo, sawMeter;

    private final byte[] data;
    private int at;

    private MidiFile(byte[] data) throws IOException {
        this.data = data;
        if (!tag("MThd"))
            throw new IOException("not a MIDI file");
        int headerEnd = int32() + at;
        int format = int16();
        int tracks = int16();
        division = int16();
        if (format > 1)
            throw new IOException("format " + format + " isn't supported");
        if ((division & 0x8000) != 0 || division == 0)
            throw new IOException("SMPTE timing isn't supported");
        at = headerEnd;
        for (int track = 0; track < tracks && at + 8 <= data.length; ) {
            boolean isTrack = tag("MTrk");
            int length = int32();
            if (length < 0)
                throw new IOException("bad chunk length");
            int end = Math.min(data.length, at + length);
            if (isTrack)
                readTrack(track++, end);
            at = end;
        }
    }

    static MidiFile parse(byte[] data) throws IOException {
        try {
            return new MidiFile(data);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException("the file ends early");
        }
    }

    private boolean tag(String name) {
        boolean match = name.equals(new String(data, at, 4, StandardCharsets.ISO_8859_1));
        at += 4;
        return match;
    }

    private int u8() {
        return data[at++] & 0xFF;
    }

    private int int16() {
        return u8() << 8 | u8();
    }

    private int int32() {
        return int16() << 16 | int16();
    }

    private int varLength() {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int b = u8();
            value = value << 7 | b & 0x7F;
            if ((b & 0x80) == 0)
                break;
        }
        return value;
    }

    private void readTrack(int track, int end) {
        Map<Integer, Deque<Long>> open = new HashMap<>();
        long tick = 0;
        int running = 0;
        while (at < end) {
            tick += varLength();
            int status = data[at] & 0xFF;
            if (status >= 0x80) {
                at++;
                if (status < 0xF0)
                    running = status;
            } else {
                status = running;
            }
            if (status == 0xFF) {
                int type = u8(), length = varLength();
                meta(track, type, at, length);
                at += length;
                if (type == 0x2F)
                    break;
            } else if (status == 0xF0 || status == 0xF7) {
                int length = varLength();
                at += length;
            } else if (status >= 0x80) {
                int kind = status & 0xF0, channel = status & 0x0F;
                int first = u8(), second = kind == 0xC0 || kind == 0xD0 ? 0 : u8();
                if (kind == 0x90 && second > 0)
                    open.computeIfAbsent(channel << 7 | first, k -> new ArrayDeque<>()).addLast(tick);
                else if (kind == 0x80 || kind == 0x90)
                    close(open, track, channel, first, tick);
            } else {
                at++;
            }
        }
        for (Map.Entry<Integer, Deque<Long>> e : open.entrySet())
            for (long start : e.getValue())
                notes.add(new Note(track, e.getKey() >> 7, e.getKey() & 0x7F, start, tick));
    }

    private void close(Map<Integer, Deque<Long>> open, int track, int channel, int key, long tick) {
        Deque<Long> starts = open.get(channel << 7 | key);
        if (starts != null && !starts.isEmpty())
            notes.add(new Note(track, channel, key, starts.removeFirst(), tick));
    }

    private void meta(int track, int type, int from, int length) {
        if (type == 0x03 && length > 0)
            trackNames.putIfAbsent(track, new String(data, from, length, StandardCharsets.ISO_8859_1).trim());
        else if (type == 0x51 && length >= 3 && !sawTempo) {
            sawTempo = true;
            microsPerBeat = (data[from] & 0xFF) << 16 | (data[from + 1] & 0xFF) << 8 | data[from + 2] & 0xFF;
        } else if (type == 0x58 && length >= 2 && !sawMeter) {
            sawMeter = true;
            beats = Math.max(1, data[from] & 0xFF);
            beatUnit = 1 << Math.min(8, data[from + 1] & 0xFF);
        }
    }
}
