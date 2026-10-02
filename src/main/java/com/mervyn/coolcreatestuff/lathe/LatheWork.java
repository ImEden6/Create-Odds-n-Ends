package com.mervyn.coolcreatestuff.lathe;

import net.minecraft.world.level.block.Block;

public final class LatheWork {
    // work units for one piece of a hardness 0 block; the lathe adds |rpm| units a tick
    public static final float BASE_WORK = 4000;
    public static final float STRESS_IMPACT = 4;

    private LatheWork() {}

    public static float required(Block source) {
        return BASE_WORK * (1 + Math.max(0, source.defaultDestroyTime()) / 2);
    }
}
