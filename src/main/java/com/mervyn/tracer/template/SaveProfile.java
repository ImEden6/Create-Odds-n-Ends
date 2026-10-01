package com.mervyn.tracer.template;

import com.mervyn.tracer.Profile;
import com.mervyn.tracer.Tracer;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// The table's Save button. Anything that doesn't check out is dropped without a word.
public record SaveProfile(int containerId, Profile profile) implements CustomPacketPayload {
    public static final Type<SaveProfile> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "save_profile"));
    public static final StreamCodec<ByteBuf, SaveProfile> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SaveProfile::containerId,
            Profile.STREAM_CODEC, SaveProfile::profile,
            SaveProfile::new);

    @Override
    public Type<SaveProfile> type() {
        return TYPE;
    }

    public static void handle(SaveProfile payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof TemplateTableMenu menu
                && menu.containerId == payload.containerId
                && payload.profile.valid())
            menu.save(payload.profile);
    }
}
