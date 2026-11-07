package com.mervyn.coolcreatestuff.jacquard.client;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import com.mervyn.coolcreatestuff.jacquard.Design;

// The editor's picture, with its undo history. Only lives on the client until Save.
final class DesignCanvas {
    private static final int HISTORY = 32;

    private record Snapshot(int width, int height, byte[] pixels) {}

    int width = 1, height = 1;
    byte[] pixels = new byte[256];
    boolean mirrorX, mirrorY;
    private final Deque<Snapshot> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    // what was there when a line or box drag started, redrawn under every new preview of it
    private byte[] base;

    int pw() {
        return width * 16;
    }

    int ph() {
        return height * 16;
    }

    boolean inside(int x, int y) {
        return x >= 0 && y >= 0 && x < pw() && y < ph();
    }

    int get(int x, int y) {
        return pixels[y * pw() + x];
    }

    private void set(int x, int y, int dye) {
        if (inside(x, y))
            pixels[y * pw() + x] = (byte) dye;
    }

    void plot(int x, int y, int dye) {
        set(x, y, dye);
        if (mirrorX)
            set(pw() - 1 - x, y, dye);
        if (mirrorY)
            set(x, ph() - 1 - y, dye);
        if (mirrorX && mirrorY)
            set(pw() - 1 - x, ph() - 1 - y, dye);
    }

    void checkpoint() {
        undo.push(new Snapshot(width, height, pixels.clone()));
        while (undo.size() > HISTORY)
            undo.removeLast();
        redo.clear();
    }

    boolean undo() {
        return step(undo, redo);
    }

    boolean redo() {
        return step(redo, undo);
    }

    private boolean step(Deque<Snapshot> from, Deque<Snapshot> to) {
        if (from.isEmpty())
            return false;
        to.push(new Snapshot(width, height, pixels.clone()));
        restore(from.pop());
        return true;
    }

    private void restore(Snapshot s) {
        width = s.width;
        height = s.height;
        pixels = s.pixels;
    }

    void load(Design design) {
        restore(new Snapshot(design.width(), design.height(), design.pixels().clone()));
    }

    Design toDesign() {
        return new Design(width, height, pixels.clone());
    }

    // keeps the top left, new space gets the given dye
    void resize(int w, int h, int dye) {
        byte[] next = new byte[w * 16 * h * 16];
        Arrays.fill(next, (byte) dye);
        int copyW = Math.min(w, width) * 16, copyH = Math.min(h, height) * 16;
        for (int y = 0; y < copyH; y++)
            System.arraycopy(pixels, y * pw(), next, y * w * 16, copyW);
        width = w;
        height = h;
        pixels = next;
    }

    void clear(int dye) {
        Arrays.fill(pixels, (byte) dye);
    }

    void flipX() {
        byte[] next = new byte[pixels.length];
        for (int y = 0; y < ph(); y++)
            for (int x = 0; x < pw(); x++)
                next[y * pw() + x] = pixels[y * pw() + pw() - 1 - x];
        pixels = next;
    }

    void flipY() {
        byte[] next = new byte[pixels.length];
        for (int y = 0; y < ph(); y++)
            System.arraycopy(pixels, (ph() - 1 - y) * pw(), next, y * pw(), pw());
        pixels = next;
    }

    void fill(int x, int y, int dye) {
        flood(x, y, dye);
        if (mirrorX)
            flood(pw() - 1 - x, y, dye);
        if (mirrorY)
            flood(x, ph() - 1 - y, dye);
        if (mirrorX && mirrorY)
            flood(pw() - 1 - x, ph() - 1 - y, dye);
    }

    private void flood(int x, int y, int dye) {
        int from = get(x, y);
        if (from == dye)
            return;
        Deque<int[]> todo = new ArrayDeque<>();
        todo.push(new int[] {x, y});
        while (!todo.isEmpty()) {
            int[] p = todo.pop();
            if (!inside(p[0], p[1]) || get(p[0], p[1]) != from)
                continue;
            set(p[0], p[1], dye);
            todo.push(new int[] {p[0] + 1, p[1]});
            todo.push(new int[] {p[0] - 1, p[1]});
            todo.push(new int[] {p[0], p[1] + 1});
            todo.push(new int[] {p[0], p[1] - 1});
        }
    }

    void beginShape() {
        base = pixels.clone();
    }

    void endShape() {
        base = null;
    }

    void line(int x0, int y0, int x1, int y1, int dye) {
        if (base != null)
            System.arraycopy(base, 0, pixels, 0, pixels.length);
        stroke(x0, y0, x1, y1, dye);
    }

    // a drag stroke for the pencil, so fast moves don't leave gaps
    void stroke(int x0, int y0, int x1, int y1, int dye) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
        while (true) {
            plot(x0, y0, dye);
            if (x0 == x1 && y0 == y1)
                return;
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    void rect(int x0, int y0, int x1, int y1, int dye, boolean filled) {
        if (base != null)
            System.arraycopy(base, 0, pixels, 0, pixels.length);
        int ax = Math.min(x0, x1), bx = Math.max(x0, x1), ay = Math.min(y0, y1), by = Math.max(y0, y1);
        for (int y = ay; y <= by; y++)
            for (int x = ax; x <= bx; x++)
                if (filled || x == ax || x == bx || y == ay || y == by)
                    plot(x, y, dye);
    }
}
