package com.mervyn.createoddsnends.cnc;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class CncDoors {
    public static final float OPEN_DEGREES = 100;
    private static final Map<Level, Set<BlockPos>> OPEN = Collections.synchronizedMap(new WeakHashMap<>());

    private CncDoors() {}

    static void track(Level level, BlockPos pendant, boolean open) {
        Set<BlockPos> set = OPEN.computeIfAbsent(level, l -> Collections.synchronizedSet(new HashSet<>()));
        if (open)
            set.add(pendant.immutable());
        else
            set.remove(pendant);
    }

    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        if (!player.getMainHandItem().isEmpty())
            return;
        Set<BlockPos> open = OPEN.get(level);
        if (open == null)
            return;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1);
        double reach = eye.distanceTo(event.getHitVec().getLocation());
        BlockPos[] pendants;
        synchronized (open) {
            pendants = open.toArray(BlockPos[]::new);
        }
        for (BlockPos pendant : pendants) {
            if (pendant.distToCenterSqr(eye) > 256 || !(level.getBlockEntity(pendant) instanceof CncBlockEntity cnc))
                continue;
            if (!crossesDoor(pendant, cnc.getBlockState().getValue(CncFrameBlock.FACING), eye, look, reach))
                continue;
            cnc.setDoors(false);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            return;
        }
    }

    static boolean crossesDoor(BlockPos pendant, Direction facing, Vec3 eye, Vec3 look, double reach) {
        Matrix4f toWorld = new Matrix4f()
                .translate(pendant.getX() + .5f, pendant.getY() + .5f, pendant.getZ() + .5f)
                .rotateY(Mth.DEG_TO_RAD * (-facing.toYRot() + 180))
                .translate(-.5f, -1.5f, -.5f)
                .scale(1 / 16f);
        Matrix4f toMachine = toWorld.invert(new Matrix4f());
        Vector3f o = toMachine.transformPosition(new Vector3f((float) eye.x, (float) eye.y, (float) eye.z));
        Vector3f r = toMachine.transformDirection(new Vector3f((float) look.x, (float) look.y, (float) look.z));
        return hits(o, r, toWorld, eye, reach, 72, -1, -OPEN_DEGREES) || hits(o, r, toWorld, eye, reach, 24, 1, OPEN_DEGREES);
    }

    private static boolean hits(Vector3f o, Vector3f r, Matrix4f toWorld, Vec3 eye, double reach, float hinge, float toward, float degrees) {
        float a = Mth.DEG_TO_RAD * degrees;
        float dx = toward * Mth.cos(a), dz = -toward * Mth.sin(a);
        float nx = dz, nz = -dx;
        float facing = nx * r.x + nz * r.z;
        if (Math.abs(facing) < 1e-5f)
            return false;
        float t = (nx * (hinge - o.x) + nz * (2 - o.z)) / facing;
        if (t <= 0)
            return false;
        Vector3f p = new Vector3f(r).mul(t).add(o);
        float along = (p.x - hinge) * dx + (p.z - 2) * dz;
        if (along < 0 || along > 24 || p.y < 10 || p.y > 40)
            return false;
        Vector3f world = toWorld.transformPosition(new Vector3f(p));
        return eye.distanceTo(new Vec3(world.x, world.y, world.z)) < reach;
    }
}
