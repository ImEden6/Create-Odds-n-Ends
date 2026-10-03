package com.mervyn.coolcreatestuff.jacquard;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

// The frame cells of the 3x3x2 loom. x: 0 left, 1 middle, 2 right seen from the front;
// y: 0 bottom to 2 top; z: 0 back, 1 front. Names match tools/jacquard.py's model files.
public enum LoomCell implements StringRepresentable {
    BOTTOM_BACK_LEFT(0, 0, 0),
    BOTTOM_BACK_MIDDLE(1, 0, 0),
    BOTTOM_BACK_RIGHT(2, 0, 0),
    BOTTOM_FRONT_MIDDLE(1, 0, 1),
    BOTTOM_FRONT_RIGHT(2, 0, 1),
    MIDDLE_BACK_LEFT(0, 1, 0),
    MIDDLE_BACK_RIGHT(2, 1, 0),
    MIDDLE_FRONT_LEFT(0, 1, 1),
    MIDDLE_FRONT_RIGHT(2, 1, 1),
    TOP_BACK_LEFT(0, 2, 0),
    TOP_BACK_RIGHT(2, 2, 0),
    TOP_FRONT_LEFT(0, 2, 1),
    TOP_FRONT_MIDDLE(1, 2, 1),
    TOP_FRONT_RIGHT(2, 2, 1);

    final int x, y, z;

    LoomCell(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
