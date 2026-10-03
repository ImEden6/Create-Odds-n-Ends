package com.mervyn.coolcreatestuff;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Profile(List<Byte> radii) {
    public static final int LAYERS = 16;
    public static final int MAX_RADIUS = 8;

    public static final Codec<Profile> CODEC = Codec.BYTE.listOf()
            .validate(Profile::check)
            .xmap(Profile::new, Profile::radii);

    public static final StreamCodec<ByteBuf, Profile> STREAM_CODEC = ByteBufCodecs.BYTE_ARRAY
            .map(Profile::unchecked, Profile::toBytes);

    public static final Profile FULL = of(new int[] {8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8});
    public static final Profile BALUSTER = of(new int[] {6, 6, 5, 3, 3, 4, 6, 7, 7, 6, 4, 3, 3, 5, 6, 6});
    public static final Profile VASE = of(new int[] {5, 6, 7, 7, 7, 6, 5, 4, 3, 3, 3, 4, 5, 5, 4, 3});

    public Profile {
        radii = List.copyOf(radii);
    }

    public int radius(int layer) {
        return radii.get(layer);
    }

    public boolean valid() {
        return check(radii).isSuccess();
    }

    public Profile mirrored() {
        return new Profile(radii.reversed());
    }

    // same 16 digits /coolstuffs tracer takes, foot first
    public String digits() {
        StringBuilder out = new StringBuilder(LAYERS);
        for (byte b : radii)
            out.append(b);
        return out.toString();
    }

    public byte[] toBytes() {
        byte[] out = new byte[LAYERS];
        for (int i = 0; i < LAYERS; i++)
            out[i] = radii.get(i);
        return out;
    }

    public static Profile of(int[] values) {
        List<Byte> list = new ArrayList<>(LAYERS);
        for (int v : values)
            list.add((byte) v);
        return new Profile(list);
    }

    private static Profile unchecked(byte[] bytes) {
        List<Byte> list = new ArrayList<>(bytes.length);
        for (byte b : bytes)
            list.add(b);
        return new Profile(list);
    }

    public static Profile fromBytes(byte[] bytes, Profile fallback) {
        List<Byte> list = new ArrayList<>(bytes.length);
        for (byte b : bytes)
            list.add(b);
        return check(list).isSuccess() ? new Profile(list) : fallback;
    }

    public static DataResult<Profile> parse(String digits) {
        if (digits.length() != LAYERS)
            return DataResult.error(() -> "need " + LAYERS + " radii, got " + digits.length());
        List<Byte> list = new ArrayList<>(LAYERS);
        for (char c : digits.toCharArray())
            list.add((byte) Character.digit(c, 10));
        return check(list).map(Profile::new);
    }

    private static DataResult<List<Byte>> check(List<Byte> list) {
        if (list.size() != LAYERS)
            return DataResult.error(() -> "profile needs " + LAYERS + " radii, got " + list.size());
        for (byte b : list)
            if (b < 1 || b > MAX_RADIUS)
                return DataResult.error(() -> "radius " + b + " is outside 1.." + MAX_RADIUS);
        return DataResult.success(list);
    }
}
