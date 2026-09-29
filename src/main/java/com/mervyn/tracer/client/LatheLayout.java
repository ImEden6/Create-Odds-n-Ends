package com.mervyn.tracer.client;

import org.joml.Quaternionf;

import com.mervyn.tracer.Profile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.core.Direction;

// Everything about the formed lathe is drawn in "model space": the lathe as if the headstock
// faced north. The slot is at z -1, the tailstock at z -2, the bed at y -1, the operator stands
// on the +x side and the tracer row sits at x -2. Shared by the block entity renderer and the
// Flywheel visual so the two can't drift apart.
public final class LatheLayout {
    private static final float PREVIEW_TICKS = 200;
    private static final float STYLUS_TIP = 1.75f;

    private LatheLayout() {}

    // the same turn the blockstate's "y" rotation gives the part models
    public static Quaternionf facingRotation(Direction facing) {
        return Axis.YP.rotationDegrees(-(facing.toYRot() + 180));
    }

    public static void toModelSpace(PoseStack ms, Direction facing) {
        ms.translate(.5, 0, .5);
        ms.mulPose(facingRotation(facing));
        ms.translate(-.5, 0, -.5);
    }

    // turned shapes are built standing up with the foot at y 0. This lays one through a cell
    // along the bed, foot against the chuck end (south, in model space).
    public static void layOnAxis(PoseStack ms) {
        ms.translate(0, 0, 1);
        ms.mulPose(Axis.XP.rotationDegrees(-90));
    }

    // Create's shaders and renderers both turn 6 degrees a second per rpm
    public static float spinDegrees(float speed, float renderTime) {
        return renderTime * speed * .3f % 360;
    }

    // along: z offset of the carriage from the middle of the slot, reach: how far the cross slide
    // (and the stylus arm with it) has moved in from radius 8. Both in blocks.
    public record Carriage(float along, float reach) {}

    // PLACEHOLDER: a client-only loop so the carriage has something to do. Feeds from the
    // tailstock to the chuck with the cutter following the stylus over the profile, backs off, then returns.
    // Swap for real cutting progress synced from the block entity later.
    public static Carriage preview(Profile profile, float renderTime) {
        float t = renderTime % PREVIEW_TICKS / PREVIEW_TICKS;
        float layerPos;
        float radius;
        if (t < .75f) {
            layerPos = Profile.LAYERS * (1 - t / .75f);
            radius = stylusRadius(profile, layerPos, STYLUS_TIP);
        } else if (t < .85f) {
            layerPos = 0;
            float start = stylusRadius(profile, 0, STYLUS_TIP);
            radius = start + (10 - start) * (t - .75f) / .1f;
        } else {
            layerPos = Profile.LAYERS * (t - .85f) / .15f;
            radius = 10;
        }
        return new Carriage(.5f - layerPos / Profile.LAYERS, (radius - Profile.MAX_RADIUS) / 16f);
    }

    // A round tip of radius tipR riding the profile at x px along the piece. Its centre sits on
    // the highest of the circle-vs-layer contacts nearby; the result is the tip's lowest point.
    static float stylusRadius(Profile profile, float x, float tipR) {
        float best = 0;
        int lo = Math.max(0, (int) Math.floor(x - tipR));
        int hi = Math.min(Profile.LAYERS - 1, (int) Math.floor(x + tipR));
        for (int i = lo; i <= hi; i++) {
            float dx = Math.max(0, Math.max(i - x, x - (i + 1)));
            if (dx >= tipR)
                continue;
            best = Math.max(best, profile.radius(i) + (float) Math.sqrt(tipR * tipR - dx * dx) - tipR);
        }
        return best;
    }
}
