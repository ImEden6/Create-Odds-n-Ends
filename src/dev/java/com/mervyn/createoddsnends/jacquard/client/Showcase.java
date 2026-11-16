package com.mervyn.createoddsnends.jacquard.client;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.cnc.CncStructure;
import com.mervyn.createoddsnends.cnc.CncTests;
import com.mervyn.createoddsnends.jacquard.JacquardCommands;
import com.mervyn.createoddsnends.jacquard.JacquardHeadBlockEntity;
import com.mervyn.createoddsnends.lathe.LatheStructure;
import com.mervyn.createoddsnends.organ.OrganCommands;

import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

// Dev only, switched on by the createoddsnends.showcase system property that the clientShowcase
// run sets. Once in the world it builds the showcase, flies the camera through fixed views,
// saves a screenshot at each into run/screenshots/showcase_*.png and quits.
@EventBusSubscriber(modid = Tracer.MODID, value = Dist.CLIENT)
final class Showcase {
    static final boolean ENABLED = Boolean.getBoolean("createoddsnends.showcase");

    @SubscribeEvent
    static void clientSetup(FMLClientSetupEvent event) {
        if (ENABLED)
            NeoForge.EVENT_BUS.addListener(Showcase::tick);
    }

    // eye position and target, in blocks: ahead of the starting spot, to its right, and up.
    // move is false for a follow-up shot from the same spot.
    private record View(String name, int at, boolean move, double ahead, double right, double up, double lookAhead, double lookRight, double lookUp,
                        Function<ServerLevel, Vec3> target) {}

    private static View view(String name, int at, double ahead, double right, double up, double lookAhead, double lookRight, double lookUp) {
        return new View(name, at, true, ahead, right, up, lookAhead, lookRight, lookUp, null);
    }

    private static View again(String name, int at) {
        return new View(name, at, false, 0, 0, 0, 0, 0, 0, null);
    }

    // aimed at a block the showcase built, the eye offset from it in the same ahead/right/up terms
    private static View close(String name, int at, Function<ServerLevel, Vec3> target, double ahead, double right, double up) {
        return new View(name, at, true, ahead, right, up, 0, 0, 0, target);
    }

    private static int tick;
    private static BlockPos anchor;
    private static Direction ahead;

    private static final int SETUP = 60;
    // ODDSNENDS_SHOWCASE picks which machine the camera visits; everything gets built either way
    private static final Map<String, List<View>> SETS = Map.of(
            "rugs", List.of(view("rugs_done", 760, 0, 4, 3, 3, 4, 0)),
            "lathe", List.of(
                    view("lathe_front", 200, -1.5, -11, 3, 4.5, -11, 1.2),
                    view("lathe_angle", 400, 0, -5, 4, 5, -11, 1),
                    view("lathe_close", 600, 2.6, -11, 2.3, 4.5, -11, 1.5),
                    view("lathe_end", 800, 5, -17, 3, 5, -11, 1),
                    view("lathe_back", 1000, 10.5, -8, 4, 5, -11, 1),
                    view("lathe_top", 1200, 5, -11, 9, 5.01, -11, 0),
                    view("lathe_parts", 1350, -4.5, -10, 3.2, -0.5, -10, 0.2),
                    view("lathe_end_far", 1500, 5, -5, 3, 5, -11, 1),
                    view("lathe_end_high", 1650, 7, -15.5, 4, 5, -11, 1)),
            "loom", List.of(
                    view("front", 200, 0.5, 0, 1.4, 4.5, 0, 1.5),
                    view("preview_angle", 300, 0.8, -3, 3.2, 4.5, 0, 1.4),
                    view("harness", 380, 2.2, 0.4, 1.2, 4.6, 0, 1.6),
                    view("card_chain", 460, 3.5, 4.2, 2.6, 4.8, 1.6, 2.3),
                    view("top", 540, 3.4, 0, 6.5, 4.6, 0, 1.5),
                    view("drive_side", 620, 4.2, -4.5, 1.2, 4.5, -1.5, 0.8),
                    view("back", 700, 9.5, 2, 3, 4.5, 0, 1.2),
                    view("card_punch", 780, 1.2, -3.4, 1.6, 3, -5, 0.6),
                    view("tapestries", 860, 1, 1.5, 2.2, 5.5, 5.5, 1.2),
                    view("race_right_1", 940, 2.6, 1, 2.1, 4.45, 1.25, 1.2),
                    view("loose_parts", 1020, 4.6, -5.6, 3.4, 4.5, -8, 0.3),
                    view("front_woven", 1300, 0.5, 0, 1.4, 4.5, 0, 1.5)),
            "organ", List.of(
                    view("organ_front", 200, 2, 13, 1.6, 5, 13, 1.1),
                    view("organ_close", 280, 3.6, 13, 2.4, 5, 13, 1.3),
                    view("organ_crank_side", 360, 5, 16.5, 1.7, 5, 13, 1.1),
                    view("organ_back", 440, 8.5, 14.8, 2.2, 5, 13, 1),
                    view("organ_wind", 520, 0.5, 12, 5.5, 5, 19, 1),
                    view("organ_pipes", 600, 3.4, 12.2, 2.8, 5, 13, 1.8),
                    view("chest_close", 680, 2.2, 18.2, 1.9, 4, 19.5, 0.7),
                    view("instruments", 760, 1.2, 18, 2.2, 4, 18, 1.2),
                    view("snare_close", 840, 2.4, 20, 2.1, 4, 20, 1.1)),
            "cnc", List.of(
                    view("cnc_front", 200, 0, -26.5, 2.2, 7.5, -26.5, 1.6),
                    view("cnc_angle", 300, 1, -21, 4.5, 7.5, -26.5, 1.4),
                    view("cnc_close", 400, 4, -25.5, 1.9, 7.5, -26, 1),
                    view("cnc_pendant", 500, 3, -20.5, 2.2, 6.5, -24.5, 1.6),
                    view("cnc_top", 600, 7.5, -26.5, 9, 7.51, -26.5, 0),
                    view("cnc_back", 700, 14, -22, 4, 7.5, -26.5, 1.5),
                    view("cnc_left", 800, 4, -33.5, 2.6, 7.5, -29, 1.5),
                    view("cnc_tool_1", 900, 5.2, -27, 2.1, 8, -27, 1.15),
                    again("cnc_tool_2", 912),
                    again("cnc_tool_3", 924),
                    again("cnc_tool_4", 936),
                    view("cnc_doors", 1060, 1, -21, 4.5, 7.5, -26.5, 1.4),
                    view("cnc_doors_high", 1160, 3, -26.5, 6.5, 7.5, -26.5, 1.5),
                    view("cnc_doors_inside", 1210, 5.2, -25.6, 2.3, 8, -26.6, 1.6),
                    again("jei_cnc", 1260),
                    again("jei_turning", 1360),
                    again("jei_punching", 1460),
                    again("jei_weaving", 1560)),
            "trim", List.of(
                    close("trim_cnc_pendant", 200, level -> Vec3.atCenterOf(cnc().above()), -0.9, 0.3, 0.1),
                    close("trim_cnc_front", 280, level -> Vec3.atCenterOf(CncTests.showcaseCell(cnc().above(), ahead.getOpposite(), 1, 1, 0)), -1.1, 0.5, 0.4),
                    close("trim_cnc_hatch", 360, level -> Vec3.atCenterOf(CncTests.showcaseCell(cnc().above(), ahead.getOpposite(), 1, 2, 1)).add(0, 0.5, 0), -0.7, 0.2, 1.9),
                    close("trim_loom_gearbox", 440, level -> point(4, -2.2, 0.5), -1.1, 0.3, 1.3),
                    close("trim_card_punch", 520, level -> Vec3.atCenterOf(anchor.relative(ahead, 3).relative(ahead.getCounterClockWise(), 5)), -0.9, 0.5, 0.6),
                    close("trim_organ_crank", 600, level -> point(5, 13.5, 1.1), 0, 1.1, 0.3)),
            "organ_nomotor", List.of(
                    view("organ_back", 440, 8.5, 14.8, 2.2, 5, 13, 1),
                    view("organ_crank_side", 360, 5, 16.5, 1.7, 5, 13, 1.1),
                    view("organ_front", 200, 2, 13, 1.6, 5, 13, 1.1),
                    view("organ_pipes", 600, 3.4, 12.2, 2.8, 5, 13, 1.8)));
    private static final String SET = System.getenv().getOrDefault("ODDSNENDS_SHOWCASE", "lathe");
    private static final boolean SOUNDS = SET.equals("sounds");
    private static final List<View> VIEWS = SETS.getOrDefault(SET, SETS.get("lathe"));
    private static final int QUIT = VIEWS.get(VIEWS.size() - 1).at + 100;

    private Showcase() {}

    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null)
            return;
        tick++;
        if (SOUNDS) {
            ShowcaseSounds.tick(mc, tick);
            if (tick == ShowcaseSounds.QUIT)
                mc.stop();
            return;
        }
        if (tick == SETUP) {
            mc.options.hideGui = true;
            anchor = mc.player.blockPosition();
            ahead = mc.player.getDirection();
            server.execute(() -> {
                JacquardCommands.showcase(player(server), anchor, ahead);
                OrganCommands.showcase(player(server), anchor.relative(ahead, 5).relative(ahead.getClockWise(), 13), ahead.getOpposite());
                LatheStructure.showcase(player(server), lathe(), ahead.getClockWise());
                CncStructure.showcase(player(server), cnc().above(), ahead.getOpposite());
            });
        }
        if (anchor == null)
            return;
        for (View view : VIEWS) {
            if (tick == view.at - 50 && view.move)
                server.execute(() -> moveTo(player(server), view));
            if (tick == view.at - 130)
                server.execute(() -> LatheStructure.showcaseRefill(player(server).level(), lathe()));
            if (tick == view.at - 20 && view.move)
                server.execute(() -> {
                    if (player(server).level().getBlockEntity(anchor.relative(ahead, 5).above(2)) instanceof JacquardHeadBlockEntity head)
                        head.showcaseJump(0.55);
                });
            if (tick == view.at - 12 && view.name.startsWith("cnc_doors"))
                server.execute(() -> CncTests.showcaseDoors(player(server).serverLevel(), cnc().above()));
            if (tick == view.at - 60 && view.name.startsWith("jei_")) {
                if (ModList.get().isLoaded("jei"))
                    ShowcaseJei.show(view.name.substring(4));
                else
                    ShowcaseEmi.show(view.name.substring(4));
            }
            if (tick == view.at)
                Screenshot.grab(mc.gameDirectory, "showcase_" + view.name + ".png", mc.getMainRenderTarget(), message -> {});
        }
        if (tick == QUIT)
            mc.stop();
    }

    private static ServerPlayer player(IntegratedServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }

    private static BlockPos lathe() {
        return anchor.relative(ahead, 4).relative(ahead.getCounterClockWise(), 12).above();
    }

    private static BlockPos cnc() {
        return anchor.relative(ahead, 6).relative(ahead.getCounterClockWise(), 24);
    }

    private static Vec3 point(double a, double r, double u) {
        Direction right = ahead.getClockWise();
        return Vec3.atBottomCenterOf(anchor).add(ahead.getStepX() * a + right.getStepX() * r, u, ahead.getStepZ() * a + right.getStepZ() * r);
    }

    private static void moveTo(ServerPlayer player, View view) {
        Vec3 eye = point(view.ahead, view.right, view.up), look = point(view.lookAhead, view.lookRight, view.lookUp);
        if (view.target != null) {
            look = view.target.apply(player.serverLevel());
            eye = look.add(point(view.ahead, view.right, view.up).subtract(point(0, 0, 0)));
        }
        Vec3 d = look.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(player.serverLevel(), eye.x, eye.y - player.getEyeHeight(), eye.z, yaw, pitch);
    }
}
