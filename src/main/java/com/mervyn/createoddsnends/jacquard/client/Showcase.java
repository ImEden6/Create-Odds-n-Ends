package com.mervyn.createoddsnends.jacquard.client;

import java.util.List;

import com.mervyn.createoddsnends.jacquard.JacquardCommands;
import com.mervyn.createoddsnends.jacquard.JacquardHeadBlockEntity;
import com.mervyn.createoddsnends.organ.OrganCommands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// Dev only, switched on by the createoddsnends.showcase system property that the clientShowcase
// run sets. Once in the world it builds the showcase, flies the camera through fixed views,
// saves a screenshot at each into run/screenshots/showcase_*.png and quits.
final class Showcase {
    static final boolean ENABLED = Boolean.getBoolean("createoddsnends.showcase");

    // eye position and target, in blocks: ahead of the starting spot, to its right, and up.
    // move is false for a follow-up shot from the same spot.
    private record View(String name, int at, boolean move, double ahead, double right, double up, double lookAhead, double lookRight, double lookUp) {}

    private static View view(String name, int at, double ahead, double right, double up, double lookAhead, double lookRight, double lookUp) {
        return new View(name, at, true, ahead, right, up, lookAhead, lookRight, lookUp);
    }

    private static View again(String name, int at) {
        return new View(name, at, false, 0, 0, 0, 0, 0, 0);
    }

    private static final int SETUP = 60;
    private static final List<View> VIEWS = List.of(
            view("organ_wind", 130, 0.5, 12, 5.5, 5, 19, 1),
            again("organ_wind_2", 700),
            again("organ_wind_3", 1300),
            again("organ_wind_4", 1900));
    private static final int QUIT = 2160;

    private static int tick;
    private static BlockPos anchor;
    private static Direction ahead;

    private Showcase() {}

    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null)
            return;
        tick++;
        if (tick == SETUP) {
            mc.options.hideGui = true;
            anchor = mc.player.blockPosition();
            ahead = mc.player.getDirection();
            server.execute(() -> {
                JacquardCommands.showcase(player(server), anchor, ahead);
                OrganCommands.showcase(player(server), anchor.relative(ahead, 5).relative(ahead.getClockWise(), 13), ahead.getOpposite());
            });
        }
        if (anchor == null)
            return;
        for (View view : VIEWS) {
            if (tick == view.at - 50 && view.move)
                server.execute(() -> moveTo(player(server), view));
            if (tick == view.at - 20 && view.move)
                server.execute(() -> {
                    if (player(server).level().getBlockEntity(anchor.relative(ahead, 5).above(2)) instanceof JacquardHeadBlockEntity head)
                        head.showcaseJump(0.55);
                });
            if (tick == view.at)
                Screenshot.grab(mc.gameDirectory, "showcase_" + view.name + ".png", mc.getMainRenderTarget(), message -> {});
        }
        if (tick == QUIT)
            mc.stop();
    }

    private static ServerPlayer player(IntegratedServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }

    private static Vec3 point(double a, double r, double u) {
        Direction right = ahead.getClockWise();
        return Vec3.atBottomCenterOf(anchor).add(ahead.getStepX() * a + right.getStepX() * r, u, ahead.getStepZ() * a + right.getStepZ() * r);
    }

    private static void moveTo(ServerPlayer player, View view) {
        Vec3 eye = point(view.ahead, view.right, view.up), look = point(view.lookAhead, view.lookRight, view.lookUp);
        Vec3 d = look.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(player.serverLevel(), eye.x, eye.y - player.getEyeHeight(), eye.z, yaw, pitch);
    }
}
