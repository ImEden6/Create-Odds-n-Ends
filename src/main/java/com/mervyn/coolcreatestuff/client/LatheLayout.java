package com.mervyn.coolcreatestuff.client;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import com.mervyn.coolcreatestuff.Profile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// Everything about the formed lathe is drawn in "model space": the lathe as if the headstock
// faced north. The slot is at z -1, the tailstock at z -2, the bed at y -1, the operator stands
// on the +x side and the tracer row sits at x -2. Shared by the block entity renderer and the
// Flywheel visual so the two can't drift apart.
public final class LatheLayout {
    private static final float PARKED_RADIUS = 10;
    // the master pattern is shown in a wood of its own, like the mockup
    public static final BlockState MASTER = Blocks.SPRUCE_PLANKS.defaultBlockState();
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

    // Where the carriage sits for a carriage position along the piece in px (16 at the tailstock
    // end, 0 at the chuck). The cutter follows the stylus over the template, easing in from parked
    // over the first pixel; with nothing to cut it stays backed off.
    public static Carriage carriage(@Nullable Profile template, float layerPos) {
        float radius = PARKED_RADIUS;
        if (template != null && layerPos < Profile.LAYERS) {
            radius = stylusRadius(template, layerPos, STYLUS_TIP);
            if (layerPos > Profile.LAYERS - 1)
                radius = Mth.lerp(layerPos - (Profile.LAYERS - 1), radius, PARKED_RADIUS);
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
