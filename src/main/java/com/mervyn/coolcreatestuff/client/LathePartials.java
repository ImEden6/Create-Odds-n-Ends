package com.mervyn.coolcreatestuff.client;

import com.mervyn.coolcreatestuff.Tracer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public final class LathePartials {
    public static final PartialModel CHUCK = of("chuck");
    public static final PartialModel LIVE_CENTER = of("live_center");
    public static final PartialModel SADDLE = of("saddle");
    public static final PartialModel CROSS_SLIDE = of("cross_slide");
    public static final PartialModel TOOL_POST = of("tool_post");
    public static final PartialModel CUTTER = of("cutter");
    public static final PartialModel STYLUS = of("stylus");

    private LathePartials() {}

    private static PartialModel of(String name) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "block/lathe/" + name));
    }

    public static void init() {}
}
