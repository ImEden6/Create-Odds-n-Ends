package com.mervyn.createoddsnends.organ;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.jacquard.CardPunchMenu;
import com.mervyn.createoddsnends.jacquard.SaveDesign;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// The Score editor's Save button. The notes travel as raw packed ints so a bad one is dropped here
// instead of kicking the player, same as SaveDesign.
public record SaveScore(int containerId, int rows, int barLength, int key, byte[] notes) implements CustomPacketPayload {
    public static final Type<SaveScore> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "save_score"));
    public static final StreamCodec<ByteBuf, SaveScore> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SaveScore::containerId,
            ByteBufCodecs.VAR_INT, SaveScore::rows,
            ByteBufCodecs.VAR_INT, SaveScore::barLength,
            ByteBufCodecs.VAR_INT, SaveScore::key,
            ByteBufCodecs.BYTE_ARRAY, SaveScore::notes,
            SaveScore::new);

    public static final int MAX_NOTES = 4096;

    public static SaveScore of(int containerId, Score score) {
        ByteBuffer out = ByteBuffer.allocate(score.notes().size() * Integer.BYTES);
        for (Score.Note n : score.notes())
            out.putInt(n.packed());
        return new SaveScore(containerId, score.rowCount(), score.barLength(), score.key(), out.array());
    }

    @Override
    public Type<SaveScore> type() {
        return TYPE;
    }

    static void handle(SaveScore payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof CardPunchMenu menu)
                || menu.containerId != payload.containerId)
            return;
        if (payload.notes.length % Integer.BYTES != 0 || payload.notes.length / Integer.BYTES > MAX_NOTES)
            return;
        ByteBuffer in = ByteBuffer.wrap(payload.notes);
        List<Score.Note> notes = new ArrayList<>(payload.notes.length / Integer.BYTES);
        while (in.hasRemaining())
            notes.add(Score.Note.unpack(in.getInt()));
        Score score = new Score(payload.rows, payload.barLength, payload.key, notes);
        if (Score.check(score).isError() || !SaveDesign.allowed(player))
            return;
        menu.punch.setPendingScore(Scores.get(player.server).add(score));
    }
}
