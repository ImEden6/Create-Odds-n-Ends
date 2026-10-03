package com.mervyn.coolcreatestuff.jacquard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mervyn.coolcreatestuff.Tracer;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Designs reach clients the way maps do: a client asks for an id when it first needs to draw it,
// the server answers with the whole design, and the client keeps it until it disconnects.
public final class DesignSync {
    private static final int REQUESTS_PER_SECOND = 20;

    private DesignSync() {}

    public record Request(int id) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "request_design"));
        public static final StreamCodec<ByteBuf, Request> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Request::new, Request::id);

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    public record Reply(int id, Design design) implements CustomPacketPayload {
        public static final Type<Reply> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "design"));
        public static final StreamCodec<ByteBuf, Reply> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Reply::id,
                Design.STREAM_CODEC, Reply::design,
                Reply::new);

        @Override
        public Type<Reply> type() {
            return TYPE;
        }
    }

    private record Budget(long second, int used) {}

    private static final Map<UUID, Budget> BUDGETS = new HashMap<>();
    private static final Int2ObjectMap<Design> CLIENT = new Int2ObjectOpenHashMap<>();
    private static final IntSet ASKED = new IntOpenHashSet();

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(Request.TYPE, Request.STREAM_CODEC, DesignSync::answer)
                .playToClient(Reply.TYPE, Reply.STREAM_CODEC, (reply, ctx) -> {
                    CLIENT.put(reply.id(), reply.design());
                    ASKED.remove(reply.id());
                });
    }

    private static void answer(Request request, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player))
            return;
        long second = player.serverLevel().getGameTime() / 20;
        Budget budget = BUDGETS.get(player.getUUID());
        if (budget == null || budget.second != second)
            budget = new Budget(second, 0);
        if (budget.used >= REQUESTS_PER_SECOND)
            return;
        BUDGETS.put(player.getUUID(), new Budget(second, budget.used + 1));
        Design design = Designs.get(player.server).get(request.id());
        if (design != null)
            PacketDistributor.sendToPlayer(player, new Reply(request.id(), design));
    }

    public static void forget(ServerPlayer player) {
        BUDGETS.remove(player.getUUID());
    }

    // client side: the design if it has arrived, otherwise asks for it once
    @Nullable
    public static Design client(int id) {
        Design design = CLIENT.get(id);
        if (design == null && id >= 0 && ASKED.add(id))
            PacketDistributor.sendToServer(new Request(id));
        return design;
    }

    public static void clearClient() {
        CLIENT.clear();
        ASKED.clear();
    }
}
