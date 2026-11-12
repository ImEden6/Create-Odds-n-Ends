package com.mervyn.createoddsnends.organ.client;

import com.mervyn.createoddsnends.Tracer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public final class OrganPartials {
    public static final PartialModel[] PIPES = new PartialModel[9];
    public static final PartialModel[] PIPES_LIT = new PartialModel[9];
    public static final PartialModel[] PAGES = new PartialModel[6];
    public static final PartialModel BEATER = of("beater");
    public static final PartialModel CRANK = of("crank");
    public static final PartialModel SHAFT = of("shaft");
    public static final PartialModel SHELF = of("shelf");
    public static final PartialModel BADGE = of("badge");
    public static final PartialModel[] BORES = {of("bore_small"), of("bore_medium"), of("bore_large")};
    public static final PartialModel KICK_BEATER = instrument("kick_drum_beater");
    public static final PartialModel SNARE_STICKS = instrument("snare_drum_sticks");
    public static final PartialModel REED_BODY = instrument("reed_pipe_body");
    public static final PartialModel REED_BODY_LIT = instrument("reed_pipe_body_lit");
    public static final PartialModel WOOD_STOPPER = instrument("wood_pipe_stopper");
    public static final PartialModel BELLOWS = instrument("reed_box_bellows");

    static {
        for (int i = 0; i < 9; i++) {
            PIPES[i] = of("pipe_" + i);
            PIPES_LIT[i] = of("pipe_lit_" + i);
        }
        for (int i = 0; i < 6; i++)
            PAGES[i] = of("page_" + i);
    }

    private OrganPartials() {}

    private static PartialModel of(String name) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "block/barrel_organ/" + name));
    }

    private static PartialModel instrument(String name) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "block/instrument/" + name));
    }

    public static void init() {}
}
