package com.mervyn.createoddsnends.cnc;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mervyn.createoddsnends.Tracer;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class CncDoors {
    public static final float OPEN_DEGREES = 100;
    public static final float HINGE_A = 72, HINGE_B = 24, HINGE_Z = 2, WIDTH = 24, BOTTOM = 10, TOP = 40;
    private static final Map<Level, Set<BlockPos>> OPEN = Collections.synchronizedMap(new WeakHashMap<>());

    private CncDoors() {}

    static void track(Level level, BlockPos pendant, boolean open) {
        if (!level.isClientSide)
            return;
        Set<BlockPos> set = OPEN.computeIfAbsent(level, l -> Collections.synchronizedSet(new HashSet<>()));
        if (open)
            set.add(pendant.immutable());
        else
            set.remove(pendant);
    }

    public static BlockPos[] open(Level level) {
        Set<BlockPos> set = OPEN.get(level);
        if (set == null)
            return new BlockPos[0];
        synchronized (set) {
            return set.toArray(BlockPos[]::new);
        }
    }

    public record Close(BlockPos pendant) implements CustomPacketPayload {
        public static final Type<Close> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "close_cnc_doors"));
        public static final StreamCodec<ByteBuf, Close> STREAM_CODEC = BlockPos.STREAM_CODEC.map(Close::new, Close::pendant);

        @Override
        public Type<Close> type() {
            return TYPE;
        }

        public static void handle(Close payload, IPayloadContext context) {
            Player player = context.player();
            Level level = player.level();
            if (level.isLoaded(payload.pendant) && player.distanceToSqr(Vec3.atCenterOf(payload.pendant)) < 100
                    && level.getBlockEntity(payload.pendant) instanceof CncBlockEntity cnc
                    && crossesDoor(payload.pendant, cnc.getBlockState().getValue(CncFrameBlock.FACING), player.getEyePosition(), player.getViewVector(1),
                    player.blockInteractionRange() + 1))
                cnc.setDoors(false);
        }
    }

    public static boolean crossesDoor(BlockPos pendant, Direction facing, Vec3 eye, Vec3 look, double reach) {
        Matrix4f toWorld = new Matrix4f()
                .translate(pendant.getX() + .5f, pendant.getY() + .5f, pendant.getZ() + .5f)
                .rotateY(Mth.DEG_TO_RAD * (-facing.toYRot() + 180))
                .translate(-.5f, -1.5f, -.5f)
                .scale(1 / 16f);
        Matrix4f toMachine = toWorld.invert(new Matrix4f());
        Vector3f o = toMachine.transformPosition(new Vector3f((float) eye.x, (float) eye.y, (float) eye.z));
        Vector3f r = toMachine.transformDirection(new Vector3f((float) look.x, (float) look.y, (float) look.z));
        return hits(o, r, toWorld, eye, reach, HINGE_A, -1, -OPEN_DEGREES) || hits(o, r, toWorld, eye, reach, HINGE_B, 1, OPEN_DEGREES);
    }

    private static boolean hits(Vector3f o, Vector3f r, Matrix4f toWorld, Vec3 eye, double reach, float hinge, float toward, float degrees) {
        float a = Mth.DEG_TO_RAD * degrees;
        float dx = toward * Mth.cos(a), dz = -toward * Mth.sin(a);
        float nx = dz, nz = -dx;
        float facing = nx * r.x + nz * r.z;
        if (Math.abs(facing) < 1e-5f)
            return false;
        float t = (nx * (hinge - o.x) + nz * (HINGE_Z - o.z)) / facing;
        if (t <= 0)
            return false;
        Vector3f p = new Vector3f(r).mul(t).add(o);
        float along = (p.x - hinge) * dx + (p.z - HINGE_Z) * dz;
        if (along < 0 || along > WIDTH || p.y < BOTTOM || p.y > TOP)
            return false;
        Vector3f world = toWorld.transformPosition(new Vector3f(p));
        return eye.distanceTo(new Vec3(world.x, world.y, world.z)) < reach;
    }
}
