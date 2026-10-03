package com.mervyn.coolcreatestuff.jacquard;

import java.util.Arrays;

// Ports of the preview's generator (reference/jacquard-preview.html), kept line for line so the
// same seed gives the same design. JS doubles and Math.imul map onto Java doubles and int
// overflow; StrictMath stands in for the browser's trig.
public final class DesignGen {
    // DYES, in DyeColor order
    public static final int[][] DYE_RGB = {
            {0xF9, 0xFF, 0xFE}, {0xF9, 0x80, 0x1D}, {0xC7, 0x4E, 0xBD}, {0x3A, 0xB3, 0xDA},
            {0xFE, 0xD8, 0x3D}, {0x80, 0xC7, 0x1F}, {0xF3, 0x8B, 0xAA}, {0x47, 0x4F, 0x52},
            {0x9D, 0x9D, 0x97}, {0x16, 0x9C, 0x9C}, {0x89, 0x32, 0xB8}, {0x3C, 0x44, 0xAA},
            {0x83, 0x54, 0x32}, {0x5E, 0x7C, 0x16}, {0xB0, 0x2E, 0x26}, {0x1D, 0x1D, 0x21}
    };

    // PALETTES: background, main, second, accent, dark, light
    private static final int[][] PALETTES = {
            {14, 15, 4, 1, 12, 0}, {11, 0, 4, 3, 15, 8}, {15, 4, 14, 0, 12, 1}, {0, 11, 14, 3, 7, 8},
            {13, 4, 12, 0, 15, 5}, {12, 4, 1, 14, 15, 0}, {10, 4, 2, 6, 15, 0}, {9, 0, 15, 4, 13, 3},
            {8, 7, 15, 14, 0, 11}, {6, 2, 10, 0, 15, 11}
    };

    public static final String[] MOTIFS = {"kilim", "medallion", "damask"};

    private DesignGen() {}

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
    }

    // genDesign()
    public static Design generate(int seed, int W, int H, int k, String motif) {
        int Wp = W * 16, Hp = H * 16;
        Rng rng = new Rng((int) ((long) seed * 7919 + 3));
        int[] pal = Arrays.copyOf(PALETTES[(int) Math.floor(rng.next() * PALETTES.length)], k);
        byte[] g = new byte[Wp * Hp];
        Arrays.fill(g, (byte) pal[0]);

        if (motif.equals("kilim")) {
            int s = (W == 1 || rng.next() < 0.4) ? 8 : 16;
            boolean alt = rng.next() < 0.6;
            int band = rng.next() < 0.5 ? 1 : 2;
            for (int y = 0; y < Hp; y++)
                for (int x = 0; x < Wp; x++) {
                    int cy = y / s, off = (alt && cy % 2 != 0) ? s / 2 : 0;
                    int cx = (x + off) / s;
                    double lx = ((x + off) % s) - (s - 1) / 2.0, ly = (y % s) - (s - 1) / 2.0;
                    int ring = (int) Math.floor((Math.abs(lx) + Math.abs(ly)) / band);
                    int par = (cx + cy) % 2;
                    put(g, Wp, Hp, x, y, pal[(ring + par) % k]);
                }
        } else if (motif.equals("medallion")) {
            double a = rng.next();
            int n = rng.next() < 0.5 ? 4 : 8;
            int step = 2 + (int) Math.floor(rng.next() * 2);
            double cx = (Wp - 1) / 2.0, cy = (Hp - 1) / 2.0, R0 = Math.min(Wp, Hp) / 2.0 - 2;
            for (int y = 0; y < Hp; y++)
                for (int x = 0; x < Wp; x++) {
                    double dx = x - cx, dy = y - cy;
                    double r = a * (Math.abs(dx) + Math.abs(dy)) * 0.8 + (1 - a) * Math.max(Math.abs(dx), Math.abs(dy))
                            + 1.4 * StrictMath.sin(n * StrictMath.atan2(dy, dx));
                    if (r < R0) {
                        // a negative index reads undefined in JS, which the Uint8Array stores as 0
                        int i = (int) (((long) Math.floor(r / step)) % k);
                        put(g, Wp, Hp, x, y, i < 0 ? 0 : pal[i]);
                    } else {
                        int gx = ((x % 6) + 6) % 6, gy = ((y % 6) + 6) % 6;
                        if ((gx == 3 && gy >= 2 && gy <= 4) || (gy == 3 && gx >= 2 && gx <= 4))
                            put(g, Wp, Hp, x, y, pal[1 % k]);
                    }
                }
        } else {
            double[][] tiles = new double[2][];
            for (int t = 0; t < 2; t++) {
                double[] v = new double[64];
                for (int i = 0; i < 64; i++)
                    v[i] = rng.next();
                double[] q = new double[64];
                for (int y = 0; y < 8; y++)
                    for (int x = 0; x < 8; x++) {
                        double sum = 0, c = 0;
                        for (int oy = -1; oy <= 1; oy++)
                            for (int ox = -1; ox <= 1; ox++) {
                                int xx = x + ox, yy = y + oy;
                                if (xx >= 0 && yy >= 0 && xx < 8 && yy < 8) {
                                    int w = ox == 0 && oy == 0 ? 2 : 1;
                                    sum += v[yy * 8 + xx] * w;
                                    c += w;
                                }
                            }
                        q[y * 8 + x] = sum / c;
                    }
                tiles[t] = q;
            }
            for (int y = 0; y < Hp; y++)
                for (int x = 0; x < Wp; x++) {
                    int tx = x / 16, ty = y / 16, checker = (tx + ty) % 2;
                    int lx = x % 16, ly = y % 16;
                    if (lx > 7)
                        lx = 15 - lx;
                    if (ly > 7)
                        ly = 15 - ly;
                    double v = tiles[checker][ly * 8 + lx];
                    int c = v < 0.46 ? pal[0] : v < 0.56 ? pal[(checker != 0 ? 2 : 1) % k] : pal[(checker != 0 ? 1 : 2) % k];
                    put(g, Wp, Hp, x, y, c);
                }
        }

        int dark = pal[Math.min(4, k - 1)], accent = pal[1 % k];
        for (int x = 0; x < Wp; x++)
            for (int y = 0; y < Hp; y++) {
                int d = Math.min(Math.min(x, y), Math.min(Wp - 1 - x, Hp - 1 - y));
                if (d == 0)
                    put(g, Wp, Hp, x, y, dark);
                else if (d == 1)
                    put(g, Wp, Hp, x, y, pal[0]);
            }
        if (Math.min(Wp, Hp) >= 32) {
            for (int x = 0; x < Wp; x++)
                for (int y = 0; y < Hp; y++) {
                    int d = Math.min(Math.min(x, y), Math.min(Wp - 1 - x, Hp - 1 - y));
                    if (d < 2 || d > 5)
                        continue;
                    int along = (d == y || d == Hp - 1 - y) ? x : y;
                    if (d == 5) {
                        put(g, Wp, Hp, x, y, pal[0]);
                        continue;
                    }
                    int zig = Math.abs((along % 4) - 2);
                    put(g, Wp, Hp, x, y, d - 2 == zig ? accent : pal[0]);
                }
        }
        return new Design(W, H, g);
    }

    private static void put(byte[] g, int Wp, int Hp, int x, int y, int c) {
        if (x >= 0 && y >= 0 && x < Wp && y < Hp)
            g[y * Wp + x] = (byte) c;
    }
}
