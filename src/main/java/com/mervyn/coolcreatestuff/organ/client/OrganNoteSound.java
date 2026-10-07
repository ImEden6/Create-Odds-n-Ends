package com.mervyn.coolcreatestuff.organ.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

// A held note: sounds for its length in ticks, then fades out over a couple of ticks instead of
// running to the end of the sample.
public class OrganNoteSound extends AbstractTickableSoundInstance {
    private int remaining;

    public OrganNoteSound(SoundEvent event, BlockPos pos, float pitch, int ticks) {
        super(event, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        x = pos.getX() + .5;
        y = pos.getY() + 1;
        z = pos.getZ() + .5;
        this.pitch = pitch;
        volume = 1;
        remaining = ticks;
    }

    @Override
    public void tick() {
        if (--remaining > 0)
            return;
        volume -= 0.5f;
        if (volume <= 0)
            stop();
    }

    void release() {
        remaining = 0;
    }
}
