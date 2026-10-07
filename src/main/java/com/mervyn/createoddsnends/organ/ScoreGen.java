package com.mervyn.createoddsnends.organ;

import java.util.ArrayList;
import java.util.List;

// Port of genScore() from reference/organ-preview.html, kept line for line so a seed gives the same
// tune as the preview. It fills the preview's cells and turns runs into notes the way onRow() and
// runLength() read them.
public final class ScoreGen {
    public static final String[] STYLES = {"march", "waltz", "carousel"};
    private static final int[] SCALE = {0, 2, 4, 5, 7, 9, 11};
    // VOICES: melody, bass, chords, drums
    private static final int[] VOICE_DYES = {14, 11, 13, 15};
    private static final int[] KEYS = {0, 5, 7, 2, 10, 3};
    private static final int[][] PROGS = {{0, 3, 4, 0}, {0, 5, 3, 4}, {0, 4, 5, 3}, {0, 3, 0, 4}, {5, 3, 0, 4}, {0, 0, 3, 4}};
    private static final int[][] MARCH = {{0, 4, 8, 12}, {0, 2, 4, 8, 12}, {0, 4, 6, 8, 10, 12}, {0, 6, 8, 12}, {0, 4, 8, 10, 12, 14}, {0, 2, 4, 6, 8, 12}};
    private static final int[][] WALTZ = {{0, 4, 8}, {0, 4, 6, 8}, {0, 2, 4, 8}, {0, 6, 8, 10}, {0, 4, 8, 10}};
    private static final int[][] CAROUSEL = {{0, 2, 3, 6, 8, 9}, {0, 3, 6, 9}, {0, 2, 3, 5, 6, 9}, {0, 6, 8, 9}, {0, 3, 6, 8, 9, 11}};

    private ScoreGen() {}

    // mulberry32()
    private static final class Rng {
        private int a;

        Rng(int seed) {
            a = seed;
        }

        double next() {
            a = a + 0x6D2B79F5;
            int t = (a ^ a >>> 15) * (1 | a);
            t = t + (t ^ t >>> 7) * (61 | t) ^ t;
            return Integer.toUnsignedLong(t ^ t >>> 14) / 4294967296.0;
        }

        int[] pick(int[][] a) {
            return a[(int) Math.floor(next() * a.length)];
        }

        int pick(int[] a) {
            return a[(int) Math.floor(next() * a.length)];
        }
    }

    private record Melody(int row, int len, int midi) {}

    public static Score generate(int seed, String style, int bars, int nVoices, int transpose) {
        Rng rng = new Rng((int) ((long) seed * 4241 + 11));
        boolean march = style.equals("march"), waltz = style.equals("waltz");
        int barLen = march ? 16 : 12;
        int rows = bars * barLen;
        int key = ((rng.pick(KEYS) + transpose) % 12 + 12) % 12;
        int[] prog = rng.pick(PROGS);
        int[] cells = new int[rows * Score.COLUMNS];

        // melody
        int[][] tmpl = march ? MARCH : waltz ? WALTZ : CAROUSEL;
        int[] motifA = rng.pick(tmpl), motifB = rng.pick(tmpl);
        int idx = 7;
        List<Melody> notes = new ArrayList<>();
        for (int b = 0; b < bars; b++) {
            int deg = chordOf(b, bars, prog);
            int[] tones = {deg % 7, (deg + 2) % 7, (deg + 4) % 7};
            int[] rhythm = b == bars - 1 ? new int[] {0} : (b % 4 == 3 ? rng.pick(tmpl) : (b % 2 == 0 ? motifA : motifB));
            for (int j = 0; j < rhythm.length; j++) {
                int pos = rhythm[j];
                boolean strong = pos % (march ? 4 : waltz ? 4 : 3) == 0;
                if (b == bars - 1)
                    idx = (int) Math.round(idx / 7.0) * 7;
                else if (strong || j == 0) {
                    int best = idx, bd = 99;
                    for (int c = idx - 3; c <= idx + 3; c++)
                        if (contains(tones, Math.floorMod(c, 7)) && Math.abs(c - idx) < bd && c != idx) {
                            bd = Math.abs(c - idx);
                            best = c;
                        }
                    idx = best;
                } else {
                    int step = rng.next() < 0.75 ? 1 : 2;
                    int dir = idx > 10 ? -1 : idx < 5 ? 1 : (rng.next() < 0.5 ? -1 : 1);
                    idx += step * dir;
                }
                idx = Math.max(3, Math.min(13, idx));
                int next = j + 1 < rhythm.length ? rhythm[j + 1] : barLen;
                notes.add(new Melody(b * barLen + pos, next - pos, deg2midi(idx, 60, key)));
            }
        }
        for (int i = 0; i < notes.size(); i++) {
            Melody n = notes.get(i), nx = i + 1 < notes.size() ? notes.get(i + 1) : null;
            int dur = n.len;
            if (nx != null && nx.midi == n.midi)
                dur = Math.max(1, dur - 1);
            else if (rng.next() < 0.25 && dur > 1)
                dur -= 1;
            write(cells, rows, n.row, n.midi, dur, 0);
        }

        for (int b = 0; b < bars; b++) {
            int deg = chordOf(b, bars, prog), r0 = b * barLen;
            int root = deg2midi(deg, 36, key), fifth = deg2midi(deg + 4, 36, key);
            int[] triad = {deg2midi(deg, 48, key), deg2midi(deg + 2, 48, key), deg2midi(deg + 4, 48, key)};
            int[][] bassAt = march ? new int[][] {{0, root, 3}, {8, fifth, 3}} : waltz ? new int[][] {{0, root, 3}} : new int[][] {{0, root, 3}, {6, fifth, 3}};
            int[] chordAt = march ? new int[] {4, 12} : waltz ? new int[] {4, 8} : new int[] {3, 9};
            int[] kick = march ? new int[] {0, 8} : waltz ? new int[] {0} : new int[] {0, 6};
            int[] snare = march ? new int[] {4, 12} : waltz ? new int[] {4, 8} : new int[] {3, 9};
            for (int[] p : bassAt)
                write(cells, rows, r0 + p[0], p[1], b == bars - 1 && p[0] == 0 ? barLen - 1 : p[2], 1);
            if (nVoices >= 3 && b < bars - 1)
                for (int p : chordAt)
                    for (int m : triad)
                        write(cells, rows, r0 + p, m, 2, 2);
            if (nVoices >= 3 && b == bars - 1)
                for (int m : triad)
                    write(cells, rows, r0, m, barLen - 1, 2);
            if (nVoices >= 4) {
                for (int p : kick)
                    cells[(r0 + p) * Score.COLUMNS] = VOICE_DYES[3] + 1;
                if (b < bars - 1)
                    for (int p : snare)
                        cells[(r0 + p) * Score.COLUMNS + 1] = VOICE_DYES[3] + 1;
            }
        }
        return new Score(rows, barLen, key, toNotes(cells, rows));
    }

    private static int chordOf(int b, int bars, int[] prog) {
        return b == bars - 1 ? 0 : b == bars - 2 ? 4 : prog[b % 4];
    }

    private static int deg2midi(int idx, int base, int key) {
        int o = Math.floorDiv(idx, 7);
        return base + key + o * 12 + SCALE[Math.floorMod(idx, 7)];
    }

    private static void write(int[] cells, int rows, int row, int midi, int dur, int v) {
        int col = midi - Score.LOW_MIDI;
        int min = v == 3 ? 0 : 2;
        while (col < min)
            col += 12;
        while (col >= Score.COLUMNS)
            col -= 12;
        for (int r = row; r < Math.min(rows, row + dur); r++)
            cells[r * Score.COLUMNS + col] = VOICE_DYES[v] + 1;
    }

    private static boolean contains(int[] a, int v) {
        for (int x : a)
            if (x == v)
                return true;
        return false;
    }

    // the drum lanes strike on every filled cell, the other columns hold through a run of equal cells
    private static List<Score.Note> toNotes(int[] cells, int rows) {
        List<Score.Note> out = new ArrayList<>();
        for (int c = 0; c < Score.COLUMNS; c++)
            for (int r = 0; r < rows; ) {
                int v = cells[r * Score.COLUMNS + c];
                if (v == 0) {
                    r++;
                    continue;
                }
                int n = 1;
                if (c > 1)
                    while (r + n < rows && cells[(r + n) * Score.COLUMNS + c] == v)
                        n++;
                out.add(new Score.Note(r, c, n, v - 1));
                r += n;
            }
        return out;
    }
}
