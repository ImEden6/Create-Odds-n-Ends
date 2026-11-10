package com.mervyn.createoddsnends.jacquard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mervyn.createoddsnends.Tracer;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// The editor's Save button. Decoded without Design's checks so a bad one is dropped here
// instead of kicking the player.
public record SaveDesign(int containerId, int width, int height, byte[] pixels) implements CustomPacketPayload {
    public static final Type<SaveDesign> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "save_design"));
    public static final StreamCodec<ByteBuf, SaveDesign> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SaveDesign::containerId,
            ByteBufCodecs.VAR_INT, SaveDesign::width,
            ByteBufCodecs.VAR_INT, SaveDesign::height,
            ByteBufCodecs.BYTE_ARRAY, SaveDesign::pixels,
            SaveDesign::new);

    private static final Map<UUID, Long> LAST_SAVE = new HashMap<>();

    @Override
    public Type<SaveDesign> type() {
        return TYPE;
    }

    static void handle(SaveDesign payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof CardPunchMenu menu)
                || menu.containerId != payload.containerId)
            return;
        Design design = new Design(payload.width, payload.height, payload.pixels);
        if (!design.valid())
            return;
        long now = player.server.getTickCount();
        Long last = LAST_SAVE.get(player.getUUID());
        if (last != null && now - last < 20)
            return;
        LAST_SAVE.put(player.getUUID(), now);
        menu.punch.setPending(Designs.get(player.server).add(design));
    }

    static void forget(ServerPlayer player) {
        LAST_SAVE.remove(player.getUUID());
    }
}
