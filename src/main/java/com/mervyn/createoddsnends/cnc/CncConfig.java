package com.mervyn.createoddsnends.cnc;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CncConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static final ModConfigSpec.BooleanValue SEQUENCED_ASSEMBLY = BUILDER
            .comment("Let the CNC mill run whole sequenced assembly recipes in one go. Off, it skips them.")
            .define("cnc.sequencedAssembly", true);

    static final ModConfigSpec.BooleanValue DEPLOYING = BUILDER
            .comment("Let the CNC mill do deploying recipes, the part and the deployer's item going in together. If the deployer would keep its item, like an axe, it has to be in the mill too but doesn't get used up.")
            .define("cnc.deploying", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private CncConfig() {}
}
