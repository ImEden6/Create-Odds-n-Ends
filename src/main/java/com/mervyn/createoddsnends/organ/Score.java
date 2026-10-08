package com.mervyn.createoddsnends.organ;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Score(int rowCount, int barLength, int key, List<Note> notes) {
    public static final int COLUMNS = 48;
    public static final int LOW_MIDI = 36;
    public static final int MAX_ROWS = 1024;
    public static final int ROWS_PER_PAGE = 8;

    public record Note(int row, int column, int length, int voice) {
        public int packed() {
            return row | column << 10 | voice << 16 | (length - 1) << 20;
        }

        static Note unpack(int p) {
            return new Note(p & 1023, p >> 10 & 63, (p >> 20 & 1023) + 1, p >> 16 & 15);
        }

        public int midi() {
            return column + LOW_MIDI;
        }
    }

    private static final Comparator<Note> ORDER = Comparator.comparingInt(Note::row).thenComparingInt(Note::column);

    public Score {
        List<Note> sorted = new ArrayList<>(notes);
        sorted.sort(ORDER);
        notes = List.copyOf(sorted);
    }

    private static List<Integer> pack(Score s) {
        return s.notes.stream().map(Note::packed).toList();
    }

    private static Score unpack(int rows, int bar, int key, List<Integer> packed) {
        return new Score(rows, bar, key, packed.stream().map(Note::unpack).toList());
    }

    public static final Codec<Score> CODEC = RecordCodecBuilder.<Score>create(i -> i.group(
            Codec.INT.fieldOf("rows").forGetter(Score::rowCount),
            Codec.INT.fieldOf("bar_length").forGetter(Score::barLength),
            Codec.INT.fieldOf("key").forGetter(Score::key),
            Codec.INT.listOf().fieldOf("notes").forGetter(Score::pack)
    ).apply(i, Score::unpack)).validate(Score::check);

    public static final StreamCodec<ByteBuf, Score> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Score::rowCount,
            ByteBufCodecs.VAR_INT, Score::barLength,
            ByteBufCodecs.VAR_INT, Score::key,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX_ROWS * COLUMNS)), Score::pack,
            Score::unpack
    ).map(s -> {
        DataResult<Score> checked = check(s);
        if (checked.isError())
            throw new DecoderException(checked.error().get().message());
        return s;
    }, s -> s);

    public static DataResult<Score> check(Score s) {
        if (s.rowCount < 1 || s.rowCount > MAX_ROWS)
            return DataResult.error(() -> "score has " + s.rowCount + " rows");
        if (s.barLength < 1 || s.key < 0 || s.key > 11)
            return DataResult.error(() -> "bad bar length or key");
        int[] freeFrom = new int[COLUMNS];
        for (Note n : s.notes) {
            if (n.row < 0 || n.column < 0 || n.column >= COLUMNS || n.voice < 0 || n.voice > 15 || n.length < 1 || n.row + n.length > s.rowCount)
                return DataResult.error(() -> "note out of range: " + n);
            if (n.row < freeFrom[n.column])
                return DataResult.error(() -> "notes overlap in column " + n.column);
            freeFrom[n.column] = n.row + n.length;
        }
        return DataResult.success(s);
    }

    public int pages() {
        return (rowCount + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
    }

    public int bars() {
        return (rowCount + barLength - 1) / barLength;
    }

    public int firstAt(int row) {
        int lo = 0, hi = notes.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (notes.get(mid).row < row)
                lo = mid + 1;
            else
                hi = mid;
        }
        return lo;
    }

    public int[] grid() {
        int[] cells = new int[rowCount * COLUMNS];
        for (Note n : notes)
            for (int r = n.row; r < n.row + n.length; r++)
                cells[r * COLUMNS + n.column] = n.voice + 1;
        return cells;
    }
}
