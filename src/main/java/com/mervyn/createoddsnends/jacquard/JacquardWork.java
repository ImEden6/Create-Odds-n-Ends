package com.mervyn.createoddsnends.jacquard;

public final class JacquardWork {
    // work units per card, the punch adds |rpm| a tick. A 2x3 design is 48 cards:
    // 48 * 533 / 64 rpm = 400 ticks, 20 s
    public static final float CARD_WORK = 533;
    // su per rpm, same as Create's millstone
    public static final float PUNCH_STRESS = 4;

    // picks per second = |rpm| / PICK_RPM
    public static final float PICK_RPM = 32;
    public static final float LOOM_STRESS = 6;
    // thread is counted in design pixels
    public static final int THREAD_PER_WOOL = 256;
    public static final int THREAD_CAP = 64 * THREAD_PER_WOOL;

    private JacquardWork() {}
}
