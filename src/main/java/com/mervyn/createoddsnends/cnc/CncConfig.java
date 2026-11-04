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

    static final ModConfigSpec.IntValue ASSEMBLY_LOOPS = BUILDER
            .comment("How many loops of a sequenced assembly the mill charges parts for. 0 charges every loop, like Create's own line.")
            .defineInRange("cnc.assemblyLoopsCharged", 1, 0, 64);

    static final ModConfigSpec.IntValue STEP_TICKS = BUILDER
            .comment("Ticks at 32 rpm per sequenced assembly step the mill does, and for a deploying recipe.")
            .defineInRange("cnc.assemblyStepTicks", 40, 1, 2000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private CncConfig() {}

    // a recipe viewer can ask before the server config has loaded, the defaults stand in until then
    static boolean on(ModConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    static int number(ModConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
