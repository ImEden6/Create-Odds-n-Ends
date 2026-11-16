package com.mervyn.createoddsnends.jacquard.client;

import java.util.List;
import java.util.function.Supplier;

import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.organ.Organ;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

// The sounds showcase set: nothing gets built, each machine sound plays on its own, loops once slow
// and once fast, with its name on the action bar and in the log.
final class ShowcaseSounds {
    private record Clip(Supplier<SoundEvent> event, int ticks, float pitch, boolean loop) {}

    private static final int START = 100, GAP = 20;
    private static final List<Clip> CLIPS = List.of(
            new Clip(Tracer.LATHE_TURNING, 80, .8f, true),
            new Clip(Tracer.LATHE_TURNING, 80, 1.3f, true),
            new Clip(Tracer.LATHE_FINISH, 30, 1, false),
            new Clip(Tracer.CNC_CUTTING, 80, .8f, true),
            new Clip(Tracer.CNC_CUTTING, 80, 1.3f, true),
            new Clip(Tracer.CNC_DOOR_OPEN, 30, 1, false),
            new Clip(Tracer.CNC_DOOR_CLOSE, 30, 1, false),
            new Clip(Tracer.LOOM_WEAVING, 80, .8f, true),
            new Clip(Tracer.LOOM_WEAVING, 80, 1.3f, true),
            new Clip(Tracer.LOOM_FINISH, 30, 1, false),
            new Clip(Tracer.CARD_PUNCH, 30, 1, false),
            new Clip(Tracer.TEMPLATE_WRITE, 30, 1, false),
            new Clip(Organ.WIND, 80, .8f, true),
            new Clip(Organ.WIND, 80, 1.3f, true));
    static final int QUIT;

    static {
        int t = START;
        for (Clip clip : CLIPS)
            t += clip.ticks + GAP;
        QUIT = t + 20;
    }

    private static SoundInstance playing;

    private ShowcaseSounds() {}

    static void tick(Minecraft mc, int tick) {
        int at = START;
        for (Clip clip : CLIPS) {
            if (tick == at) {
                SoundEvent event = clip.event.get();
                playing = new SimpleSoundInstance(event.getLocation(), SoundSource.MASTER, .8f, clip.pitch, SoundInstance.createUnseededRandom(),
                        clip.loop, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
                mc.getSoundManager().play(playing);
                String name = event.getLocation().getPath() + (clip.loop ? " at pitch " + clip.pitch : "");
                mc.gui.setOverlayMessage(Component.literal(name), false);
                Tracer.LOGGER.info("SOUNDSHOW {}", name);
            }
            if (tick == at + clip.ticks && clip.loop)
                mc.getSoundManager().stop(playing);
            at += clip.ticks + GAP;
        }
    }
}
