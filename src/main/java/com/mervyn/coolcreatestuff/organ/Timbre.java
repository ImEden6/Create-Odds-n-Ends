package com.mervyn.coolcreatestuff.organ;

public enum Timbre {
    MELODY, BASS, CHORD, KICK, SNARE;

    // by DyeColor id: white, orange, magenta, light blue, yellow, lime, pink, gray, light gray, cyan,
    // purple, blue, brown, green, red, black
    private static final Timbre[] BY_VOICE = {
            CHORD, MELODY, MELODY, BASS, MELODY, CHORD, MELODY, KICK,
            KICK, BASS, BASS, BASS, CHORD, CHORD, MELODY, KICK};

    // columns 0 and 1 are the kick and snare lanes whatever colour is punched there, and a drum
    // colour anywhere else is a kick low down and a snare from the middle up
    public static Timbre of(int voice, int column) {
        if (column == 0)
            return KICK;
        if (column == 1)
            return SNARE;
        Timbre t = BY_VOICE[voice];
        return t == KICK && column >= 24 ? SNARE : t;
    }

    public boolean drum() {
        return this == KICK || this == SNARE;
    }
}
