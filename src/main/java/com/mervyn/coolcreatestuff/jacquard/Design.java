package com.mervyn.coolcreatestuff.jacquard;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// A woven picture: width and height in blocks, one dye index per pixel, 16 pixels per block,
// rows top to bottom.
public record Design(int width, int height, byte[] pixels) {
    public static final int MAX_WIDTH = 3;
    public static final int MAX_HEIGHT = 4;

    public static final Codec<Design> CODEC = RecordCodecBuilder.<Design>create(i -> i.group(
            Codec.INT.fieldOf("width").forGetter(Design::width),
            Codec.INT.fieldOf("height").forGetter(Design::height),
            Codec.BYTE_BUFFER.fieldOf("pixels").forGetter(d -> ByteBuffer.wrap(d.pixels))
    ).apply(i, (w, h, buf) -> {
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        return new Design(w, h, bytes);
    })).validate(Design::check);

    public static final StreamCodec<ByteBuf, Design> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Design::width,
            ByteBufCodecs.VAR_INT, Design::height,
            ByteBufCodecs.BYTE_ARRAY, Design::pixels,
            Design::new
    ).map(d -> {
        DataResult<Design> checked = check(d);
        if (checked.isError())
            throw new DecoderException(checked.error().get().message());
        return d;
    }, d -> d);

    public int pixelWidth() {
        return width * 16;
    }

    public int pixelHeight() {
        return height * 16;
    }

    public int dye(int x, int y) {
        return pixels[y * pixelWidth() + x];
    }

    private static DataResult<Design> check(Design d) {
        if (d.width < 1 || d.width > MAX_WIDTH || d.height < 1 || d.height > MAX_HEIGHT)
            return DataResult.error(() -> "design size " + d.width + "x" + d.height + " is outside 1-" + MAX_WIDTH + " x 1-" + MAX_HEIGHT);
        if (d.pixels.length != d.pixelWidth() * d.pixelHeight())
            return DataResult.error(() -> "design needs " + d.pixelWidth() * d.pixelHeight() + " pixels, got " + d.pixels.length);
        for (byte b : d.pixels)
            if (b < 0 || b > 15)
                return DataResult.error(() -> "dye index " + b + " is outside 0-15");
        return DataResult.success(d);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Design d && d.width == width && d.height == height && Arrays.equals(d.pixels, pixels);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * width + height) + Arrays.hashCode(pixels);
    }
}
