package com.mervyn.createoddsnends.ponder;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.jacquard.Jacquard;
import com.mervyn.createoddsnends.organ.Organ;

import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// Dev only, switched on by the createoddsnends.pondershots system property that the clientPonder
// run sets. Opens every scene of ours, saves run/screenshots/ponder_<scene>_<n>.png just after each
// keyframe and at the end, then quits.
public final class PonderShots {
    public static final boolean ENABLED = Boolean.getBoolean("createoddsnends.pondershots");
    private static final int SETUP = 60, SETTLE = 12;

    private static List<ResourceLocation> components;
    private static final Set<ResourceLocation> seen = new HashSet<>();
    private static int tick, component, wait, shot = -1, scrolled;
    private static PonderUI ui;

    private PonderShots() {}

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ++tick < SETUP || --wait > 0)
            return;
        if (components == null)
            components = List.of(Tracer.LATHE_HEADSTOCK.getId(), Tracer.TRACER_HOLDER.getId(), Tracer.TEMPLATE_TABLE.getId(),
                    Jacquard.JACQUARD_HEAD.getId(), Jacquard.TAPESTRY.getId(), Jacquard.CARD_PUNCH.getId(),
                    Organ.BARREL_ORGAN.getId(), Organ.WIND_CHEST.getId());
        if (ui == null || mc.screen != ui) {
            if (component >= components.size()) {
                mc.stop();
                return;
            }
            ui = PonderUI.of(components.get(component++));
            mc.setScreen(ui);
            shot = -1;
            scrolled = 0;
            wait = SETTLE;
            return;
        }
        PonderScene scene = ui.getActiveScene();
        if (shot == -1) {
            if (!seen.add(scene.getId())) {
                next(mc);
                return;
            }
            shot = 0;
        }
        int stops = scene.getKeyframeCount() + 1;
        if (shot > 0)
            Screenshot.grab(mc.gameDirectory, "ponder_" + scene.getId().getPath() + "_" + shot + ".png", mc.getMainRenderTarget(), message -> {});
        if (shot == stops) {
            next(mc);
            return;
        }
        ui.seekToTime(shot < scene.getKeyframeCount() ? scene.getKeyframeTime(shot) + 40 : scene.getTotalTime() - 5);
        shot++;
        wait = SETTLE;
    }

    private static void next(Minecraft mc) {
        shot = -1;
        wait = SETTLE;
        try {
            Method scroll = PonderUI.class.getDeclaredMethod("scroll", boolean.class);
            scroll.setAccessible(true);
            if (++scrolled < 10 && (boolean) scroll.invoke(ui, true))
                return;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        mc.setScreen(null);
        ui = null;
    }
}
