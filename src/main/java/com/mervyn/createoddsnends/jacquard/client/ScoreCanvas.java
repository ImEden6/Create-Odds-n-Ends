package com.mervyn.createoddsnends.jacquard.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.mervyn.createoddsnends.organ.Score;
import com.mervyn.createoddsnends.organ.Score.Note;

// The Score mode's notes with their undo history. Only lives on the client until Save. Notes in a
// column never overlap, so every edit goes through place().
final class ScoreCanvas {
    static final int MAX_NOTES = 4096;
    private static final int HISTORY = 64;

    private record Snapshot(int bars, int barLength, int key, List<Note> notes) {}

    int bars = 8, barLength = 16, key;
    List<Note> notes = new ArrayList<>();
    final Set<Note> selected = new HashSet<>();
    private List<Note> clipboard = List.of();
    private final Deque<Snapshot> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();

    int rows() {
        return bars * barLength;
    }

    int maxBars() {
        return Score.MAX_ROWS / barLength;
    }

    private Snapshot snapshot() {
        return new Snapshot(bars, barLength, key, List.copyOf(notes));
    }

    void checkpoint() {
        undo.push(snapshot());
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
        to.push(snapshot());
        Snapshot s = from.pop();
        bars = s.bars;
        barLength = s.barLength;
        key = s.key;
        notes = new ArrayList<>(s.notes);
        selected.clear();
        return true;
    }

    void load(Score score) {
        set(Math.max(1, Math.min((score.rowCount() + score.barLength() - 1) / score.barLength(), Score.MAX_ROWS / score.barLength())),
                score.barLength(), score.key(), score.notes());
    }

    void set(int bars, int barLength, int key, List<Note> incoming) {
        this.bars = bars;
        this.barLength = barLength;
        this.key = key;
        notes = new ArrayList<>(incoming);
        selected.clear();
        trim();
    }

    Score toScore() {
        return new Score(rows(), barLength, key, notes);
    }

    private void trim() {
        int rows = rows();
        List<Note> out = new ArrayList<>(notes.size());
        for (Note n : notes)
            if (n.row() < rows)
                out.add(n.row() + n.length() > rows ? new Note(n.row(), n.column(), rows - n.row(), n.voice()) : n);
        notes = out;
        selected.retainAll(notes);
    }

    void setBars(int count) {
        bars = count;
        trim();
    }

    void setBarLength(int length) {
        barLength = length;
        bars = Math.min(bars, maxBars());
        trim();
    }

    @Nullable
    Note noteAt(int row, int column) {
        for (Note n : notes)
            if (n.column() == column && row >= n.row() && row < n.row() + n.length())
                return n;
        return null;
    }

    boolean full() {
        return notes.size() >= MAX_NOTES;
    }

    void place(Note placed) {
        List<Note> out = new ArrayList<>(notes.size() + 1);
        for (Note n : notes) {
            if (n.column() != placed.column() || n.row() + n.length() <= placed.row() || n.row() >= placed.row() + placed.length())
                out.add(n);
            else if (n.row() < placed.row())
                out.add(new Note(n.row(), n.column(), placed.row() - n.row(), n.voice()));
        }
        out.add(placed);
        notes = out;
        selected.retainAll(notes);
    }

    boolean erase(int row, int column) {
        Note n = noteAt(row, column);
        if (n == null)
            return false;
        notes.remove(n);
        selected.remove(n);
        return true;
    }

    void clear() {
        notes.clear();
        selected.clear();
    }

    void selectBox(int rowA, int colA, int rowB, int colB) {
        int r0 = Math.min(rowA, rowB), r1 = Math.max(rowA, rowB), c0 = Math.min(colA, colB), c1 = Math.max(colA, colB);
        selected.clear();
        for (Note n : notes)
            if (n.column() >= c0 && n.column() <= c1 && n.row() <= r1 && n.row() + n.length() > r0)
                selected.add(n);
    }

    void deleteSelected() {
        notes.removeAll(selected);
        selected.clear();
    }

    void move(int rows, int columns) {
        List<Note> moved = new ArrayList<>(selected.size());
        for (Note n : selected)
            moved.add(new Note(n.row() + rows, n.column() + columns, n.length(), n.voice()));
        deleteSelected();
        placeAll(moved);
    }

    private void placeAll(List<Note> incoming) {
        for (Note n : incoming)
            place(n);
        selected.addAll(incoming);
        selected.retainAll(notes);
    }

    boolean canTranspose(int semitones) {
        boolean any = false;
        for (Note n : selected) {
            if (n.column() < 2)
                continue;
            any = true;
            if (n.column() + semitones < 2 || n.column() + semitones >= Score.COLUMNS)
                return false;
        }
        return any;
    }

    void transpose(int semitones) {
        List<Note> moved = new ArrayList<>(selected.size());
        for (Note n : selected)
            moved.add(n.column() < 2 ? n : new Note(n.row(), n.column() + semitones, n.length(), n.voice()));
        deleteSelected();
        placeAll(moved);
    }

    void copy() {
        int first = selected.stream().mapToInt(Note::row).min().orElse(0);
        clipboard = selected.stream().map(n -> new Note(n.row() - first, n.column(), n.length(), n.voice())).toList();
    }

    boolean hasClipboard() {
        return !clipboard.isEmpty();
    }

    boolean canPaste() {
        return notes.size() + clipboard.size() <= MAX_NOTES;
    }

    void paste(int row) {
        selected.clear();
        int rows = rows();
        List<Note> pasted = new ArrayList<>(clipboard.size());
        for (Note n : clipboard)
            if (row + n.row() < rows)
                pasted.add(new Note(row + n.row(), n.column(), Math.min(n.length(), rows - row - n.row()), n.voice()));
        placeAll(pasted);
    }
}
