package com.mervyn.createoddsnends.cnc;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CncConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static final ModConfigSpec.BooleanValue SEQUENCED_ASSEMBLY = BUILDER
            .comment("Let the CNC mill run whole sequenced assembly recipes in one go. Off, it only makes createoddsnends:cnc recipes.")
            .define("cnc.sequencedAssembly", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private CncConfig() {}
}
