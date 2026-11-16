package com.mervyn.createoddsnends.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

// A machine's running loop. The block entity calls keep() every client tick while it works; once
// the calls stop the loop fades out over a few ticks.
public class MachineSound extends AbstractTickableSoundInstance {
    private static final float LOUDEST = .3f;
    private static final Map<BlockEntity, MachineSound> PLAYING = new HashMap<>();

    private final BlockEntity be;
    private int quiet;

    private MachineSound(SoundEvent event, BlockEntity be, Vec3 at) {
        super(event, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.be = be;
        x = at.x;
        y = at.y;
        z = at.z;
        looping = true;
        delay = 0;
        volume = LOUDEST / 4;
    }

    public static void keep(BlockEntity be, SoundEvent event, float speed) {
        keep(be, Vec3.atCenterOf(be.getBlockPos()), event, speed);
    }

    // at is only read when the loop starts
    public static void keep(BlockEntity be, Vec3 at, SoundEvent event, float speed) {
        Minecraft mc = Minecraft.getInstance();
        if (be.getLevel() != mc.level)
            return;
        MachineSound sound = PLAYING.get(be);
        if (sound == null || !mc.getSoundManager().isActive(sound)) {
            PLAYING.values().removeIf(s -> !mc.getSoundManager().isActive(s));
            sound = new MachineSound(event, be, at);
            PLAYING.put(be, sound);
            mc.getSoundManager().play(sound);
        }
        sound.quiet = 0;
        sound.pitch = Mth.clamp(Math.abs(speed) / 256f + .7f, .7f, 1.4f);
    }

    @Override
    public void tick() {
        if (be.isRemoved() || ++quiet > 2) {
            volume -= LOUDEST / 4;
            if (volume <= 0)
                stop();
            return;
        }
        volume = Math.min(LOUDEST, volume + LOUDEST / 4);
    }
}
