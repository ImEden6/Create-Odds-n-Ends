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
            view("front", 200, 0.5, 0, 1.4, 4.5, 0, 1.5),
            again("front_later", 215),
            view("preview_angle", 300, 0.8, -3, 3.2, 4.5, 0, 1.4),
            view("harness", 380, 2.2, 0.4, 1.2, 4.6, 0, 1.6),
            view("card_chain", 460, 3.5, 4.2, 2.6, 4.8, 1.6, 2.3),
            view("top", 540, 3.4, 0, 6.5, 4.6, 0, 1.5),
            view("drive_side", 620, 4.2, -4.5, 1.2, 4.5, -1.5, 0.8),
            view("card_punch", 700, 1.2, -3.4, 1.6, 3, -5, 0.6),
            view("tapestries", 780, 1, 1.5, 2.2, 5.5, 5.5, 1.2),
            // the race from above the breast beam, a burst at each end so one catches a throw
            view("race_right_1", 860, 2.6, 1, 2.1, 4.45, 1.25, 1.2),
            again("race_right_2", 864),
            again("race_right_3", 868),
            view("race_left_1", 940, 2.6, -1, 2.1, 4.45, -1.25, 1.2),
            again("race_left_2", 944),
            again("race_left_3", 948),
            view("organ_front", 1000, 2, 13, 1.6, 5, 13, 1.1),
            again("organ_front_later", 1010),
            view("organ_close", 1080, 3.6, 13, 2.4, 5, 13, 1.3),
            view("organ_crank_side", 1160, 5, 16.5, 1.7, 5, 13, 1.1),
            view("organ_back", 1240, 8.5, 14.8, 2.2, 5, 13, 1),
            view("organ_wind", 1320, 0.5, 12, 5.5, 5, 19, 1),
            view("organ_pipes", 1400, 3.4, 12.2, 2.8, 5, 13, 1.8),
            view("organ_preview_angle", 1480, 2, 15.5, 4.2, 5, 13, 1.2),
            view("chest_close", 1540, 2.2, 18.2, 1.9, 4, 19.5, 0.7),
            view("front_woven", 1600, 0.5, 0, 1.4, 4.5, 0, 1.5),
            view("loose_parts", 1700, 1.5, -11, 2.2, 4.5, -8, 0.5));
    private static final int QUIT = 1760;

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
