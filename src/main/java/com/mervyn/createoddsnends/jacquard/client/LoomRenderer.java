package com.mervyn.createoddsnends.jacquard.client;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import com.mervyn.createoddsnends.jacquard.Design;
import com.mervyn.createoddsnends.jacquard.DesignGen;
import com.mervyn.createoddsnends.jacquard.JacquardHeadBlockEntity;
import com.mervyn.createoddsnends.jacquard.LoomPartBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

// Everything on the loom that moves, ported from buildLoom(). Drawn in the preview's own
// coordinates: pixels, origin at the footprint's bottom centre, +z toward the front.
public class LoomRenderer extends SafeBlockEntityRenderer<JacquardHeadBlockEntity> {
    // CARD_PATH
    private static final float CX = 27, TOP = 44, BOT = 30, R = 3.5f;
    private static final int WARP = 0xE9E2CF, CORD = 0xC9B78F, HOLE = 0x3E3428;
    // top of the shuttle race, just under the lower shed
    private static final float RACE = 24.6f;
    // the beater, reed and shuttle boxes sit this far further out than the preview's, to clear a
    // 48 px wide cloth
    private static final float WIDE = 6;
    private static final float FRAME = 34;

    public LoomRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public boolean shouldRenderOffScreen(JacquardHeadBlockEntity be) {
        return true;
    }

    // cardPathPoint(): position and angle s px along the card chain's loop
    static double[] cardPathPoint(double s) {
        double h = TOP - BOT, arc = Math.PI * R, L = 2 * h + 2 * arc;
        s = ((s % L) + L) % L;
        if (s < h)
            return new double[] {CX - R, TOP - s, -Math.PI / 2};
        s -= h;
        if (s < arc) {
            double th = Math.PI + s / R;
            return new double[] {CX + R * Math.cos(th), BOT + R * Math.sin(th), th + Math.PI / 2};
        }
        s -= arc;
        if (s < h)
            return new double[] {CX + R, BOT + s, Math.PI / 2};
        s -= h;
        double th = s / R;
        return new double[] {CX + R * Math.cos(th), TOP + R * Math.sin(th), th + Math.PI / 2};
    }

    @Override
    protected void renderSafe(JacquardHeadBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!state.getValue(LoomPartBlock.FORMED))
            return;
        Design d = be.shownDesign();
        int Wp = d.pixelWidth(), Hp = d.pixelHeight();
        JacquardHeadBlockEntity.Phase ph = be.phase(partialTicks);
        int lit = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().below());
        Direction facing = state.getValue(LoomPartBlock.FACING);

        ms.pushPose();
        ms.translate(.5, 0, .5);
        ms.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        ms.translate(-.5, 0, -.5);
        ms.scale(1 / 16f, 1 / 16f, 1 / 16f);
        ms.translate(8, -32, 16);

        Mesh m = new Mesh(ms, buffer.getBuffer(RenderType.solid()), lit, false);
        TextureAtlasSprite oak = Mesh.sprite("createoddsnends:block/loom_timber_h");
        TextureAtlasSprite linen = Mesh.sprite("createoddsnends:block/linen");
        TextureAtlasSprite brass = Mesh.sprite("create:block/brass_block");
        TextureAtlasSprite iron = Mesh.sprite("minecraft:block/iron_block");

        boolean real = be.hasDesign();
        head(m, real ? d : null, brass, iron, ph, Hp - 1 - ph.row());

        // card cylinder and card chain
        double cylAngle = ph.cardS() * Math.PI / 2;
        m.push();
        m.rotZ(cylAngle, CX, TOP);
        m.box(brass, -2.4f, -2.4f, -8, 2.4f, 2.4f, 2);
        m.box(iron, -0.8f, -0.8f, -10, 0.8f, 0.8f, -8);
        m.pop();
        double L = 2 * (TOP - BOT) + 2 * Math.PI * R;
        int nCards = (int) Math.round(L / 5);
        JacquardHeadBlockEntity.Picks picks = be.picks();
        TextureAtlasSprite paper = Mesh.sprite("createoddsnends:block/blank_card");
        int current = (int) Math.floor(ph.cardS());
        for (int i = 0; i < nCards; i++) {
            double s = i * (L / nCards) - ph.cardS() * (L / nCards);
            double[] p = cardPathPoint(s);
            m.push();
            m.rotZ(p[2], (float) p[0], (float) p[1]);
            m.box(paper, -2.2f, -0.2f, -8, 2.2f, 0.2f, 2);
            // the card for pick q has a hole for every thread that pick lifts. Half the loop shows
            // picks already read, half the ones coming, so cards change over out of sight at the bottom.
            int q = current - nCards / 2 + Math.floorMod(i - (current - nCards / 2), nCards);
            if (real && q >= 0 && q < picks.total())
                holes(m, d, picks, q);
            m.pop();
        }

        // drive: cross shaft and riser to the head (the input shaft is Create's, on the drive block)
        m.push();
        m.rotX(ph.spin(), 6, 8);
        m.shaft(0, -33, -1, -1, 36, 1, 1);
        m.pop();
        m.push();
        m.rotY(ph.spin(), -37, 8);
        m.shaft(1, -1, 10, -1, 1, 41, 1);
        m.pop();
        // from the gearbox on the cap beam across to the lift box on the head
        m.push();
        m.rotX(ph.spin(), 42, 8);
        m.shaft(0, -32, -1, -1, -18, 1, 1);
        m.pop();

        // warp beam and cloth roller
        double rb = 3.2 - 0.8 * ph.R() / Hp;
        // one texel per design pixel, so the cloth lines up with tapestries on the block grid
        double rowH = 1;
        float half = Wp / 2f;
        m.prismX(oak, -FRAME, FRAME, 10, -9, 1.4f, 8, -ph.R() / 3);
        m.prismX(linen, -half, half, 10, -9, (float) rb, 10, -ph.R() * rowH / 3);
        double rr = 2 + 1.4 * ph.R() / Hp;
        m.prismX(oak, -FRAME, FRAME, 9, 12, 1.4f, 8, ph.R() * rowH / 2);
        if (ph.R() > 0.5)
            m.prismX(linen, -half, half, 9, 12, (float) rr, 10, ph.R() * rowH / rr);

        // beater: swords, top bar, race, reed, rotating about the floor pivot, shuttle on the race
        int row = Hp - 1 - ph.row();
        int[] weft = DesignGen.DYE_RGB[ph.weft()];
        int weftRgb = weft[0] << 16 | weft[1] << 8 | weft[2];
        m.push();
        m.rotX(ph.beater(), 4, 0);
        // Changed from the preview: its race sat at 17-18.5, well under the warp (25), so the
        // shuttle ran beneath the cloth. The race top is now just under the lower shed at RACE,
        // the shuttle passes through the shed, and the reed and top bar sit higher to match.
        // Beaten forward, the reed now meets the fell.
        m.beam("timber", 1, -22 - WIDE, 0, -1, -20 - WIDE, RACE + 9, 1);
        m.beam("timber", 1, 20 + WIDE, 0, -1, 22 + WIDE, RACE + 9, 1);
        m.beam("timber", 0, -22 - WIDE, RACE + 7, -1, 22 + WIDE, RACE + 9, 1);
        m.beam("timber", 0, -22 - WIDE, RACE - 2, -1, 22 + WIDE, RACE, 3);
        m.box(Mesh.sprite("createoddsnends:block/reed"), -20 - WIDE, RACE, -0.3f, 20 + WIDE, RACE + 7, 0.3f);
        float sx = (float) ph.sx();
        m.box(oak, sx - 3, RACE, 0.6f, sx + 3, RACE + 1.8f, 2.6f);
        m.box(brass, sx - 3.6f, RACE + 0.4f, 1.2f, sx - 3, RACE + 1.4f, 2.0f);
        m.box(brass, sx + 3, RACE + 0.4f, 1.2f, sx + 3.6f, RACE + 1.4f, 2.0f);
        m.box(paper, weftRgb, sx - 1.6f, RACE + 1.3f, 1.1f, sx + 1.6f, RACE + 1.86f, 2.1f);
        if (ph.flying() || (ph.sp() > 0 && ph.sp() < 1)) {
            float start = ph.dir() > 0 ? -19.5f - WIDE : 19.5f + WIDE;
            m.thread(new Vector3f(start, RACE + 0.7f, 1.6f), new Vector3f(sx, RACE + 0.7f, 1.6f), new Vector3f(0, 1, 0), 0.6f, weftRgb);
        }
        shuttleBoxes(m, oak, oak, picks, ph);
        m.pop();

        // warp threads and harness cords, one per design column. Changed from the preview: the shed
        // follows the card (a thread lifts unless its pixel is this pick's weft colour, so the weft
        // shows on the face where the design has it), and threads that aren't lifted stay at rest,
        // held there by their lingoes, instead of dropping.
        int N = Wp;
        Vector3f across = new Vector3f(1, 0, 0);
        for (int i = 0; i < N; i++) {
            float x = i + 0.5f - half;
            boolean up = d.dye(i, row) != ph.weft();
            float hy = (float) (25 + ph.shed() * (up ? 3.4 : 0));
            m.thread(new Vector3f(x, (float) (10 + rb), -9), new Vector3f(x, 26.2f, -13), across, 0.35f, WARP);
            m.thread(new Vector3f(x, 26.2f, -13), new Vector3f(x, hy, -4), across, 0.35f, WARP);
            m.thread(new Vector3f(x, hy, -4), new Vector3f(x, 25, 6), across, 0.35f, WARP);
            // harness: the cord fans out from the hooks packed under the head, through its hole in
            // the comber board, down to a heddle eye the warp runs through, with a lingo weight
            // hanging below to pull it back down
            m.thread(new Vector3f(x * 0.55f, 40, -4), new Vector3f(x, 35, -4), across, 0.3f, CORD);
            m.thread(new Vector3f(x, 34, -4), new Vector3f(x, hy + 0.45f, -4), across, 0.3f, CORD);
            m.box(iron, x - 0.25f, hy - 0.45f, -4.25f, x + 0.25f, hy + 0.45f, -3.75f);
            m.box(iron, x - 0.15f, hy - 9, -4.15f, x + 0.15f, hy - 0.45f, -3.85f);
        }

        // cloth: fell to the breast beam, then down to the roller. Woven rows show the design,
        // the leader beyond them is plain linen.
        float[][] pts = {{25, 6}, {26.3f, 14.3f}, {9, (float) (12 + rr + 0.2)}};
        double dw = ph.R() * rowH, d0 = 0;
        List<float[]> woven = new ArrayList<>();
        for (int k = 0; k < 2; k++) {
            float[] A = pts[k], B = pts[k + 1];
            double len = Math.hypot(B[0] - A[0], B[1] - A[1]);
            // the preview's normals here only fed its shading; this is the cloth's real outward
            // side, up on the stretch to the breast beam and toward the weaver on the way down
            Vector3f n = new Vector3f(0, B[1] - A[1], -(B[0] - A[0])).normalize();
            double wA = d0, wB = Math.min(d0 + len, dw);
            if (wB > wA) {
                float[] a = along(A, B, (wA - d0) / len), b = along(A, B, (wB - d0) / len);
                float va = (float) ((Hp - (ph.R() - wA / rowH)) / Hp), vb = (float) ((Hp - (ph.R() - wB / rowH)) / Hp);
                woven.add(new float[] {a[0], a[1], b[0], b[1], va, vb, n.y, n.z});
            }
            double lA = Math.max(d0, dw), lB = d0 + len;
            if (lB > lA) {
                float[] a = along(A, B, (lA - d0) / len), b = along(A, B, (lB - d0) / len);
                float vl = (float) Math.min(16, lB - lA);
                m.quad(linen, new Vector3f(-half, a[0], a[1]), new Vector3f(half, a[0], a[1]), new Vector3f(half, b[0], b[1]), new Vector3f(-half, b[0], b[1]),
                        new float[] {0, 0, 16, 0, 16, vl, 0, vl}, n, Mesh.WHITE, true);
            }
            d0 += len;
        }
        if (!woven.isEmpty()) {
            ResourceLocation tex = DesignTextures.get(be.getDesignId() >= 0 && d != JacquardHeadBlockEntity.DEBUG_DESIGN ? be.getDesignId() : DesignTextures.DEBUG, d);
            Mesh cloth = new Mesh(ms, buffer.getBuffer(RenderType.entitySolid(tex)), lit, true);
            for (float[] q : woven)
                cloth.quad(null, new Vector3f(-half, q[0], q[1]), new Vector3f(half, q[0], q[1]), new Vector3f(half, q[2], q[3]), new Vector3f(-half, q[2], q[3]),
                        new float[] {0, q[4], 1, q[4], 1, q[5], 0, q[5]}, new Vector3f(0, q[6], q[7]), Mesh.WHITE, true);
        }
        ms.popPose();
    }

    // an open cage, so the hooks and knives show from above
    private static void head(Mesh m, Design d, TextureAtlasSprite brass, TextureAtlasSprite iron, JacquardHeadBlockEntity.Phase ph, int row) {
        for (float x : new float[] {-16, 14})
            for (float z : new float[] {-10, 2})
                m.beam("post", 1, x, 38, z, x + 2, 46, z + 2);
        for (float z : new float[] {-12, 4})
            m.beam("timber", 0, -18, 46, z, 18, 48, z + 2);
        for (float x : new float[] {-18, 16})
            m.beam("timber", 2, x, 46, -10, x + 2, 48, 4);
        m.box(Mesh.sprite("createoddsnends:block/loom_planks_h"), -14, 38, -8, 14, 39, 2);
        for (float z : new float[] {-10, 3}) {
            m.beam("timber", 0, -14, 38, z, 14, 40, z + 1);
            m.beam("timber", 0, -14, 44, z, 14, 46, z + 1);
        }
        for (float x : new float[] {-16, 14})
            m.beam("timber", 2, x + 0.5f, 44, -8, x + 1.5f, 46, 2);

        float lift = (float) (1.4 * ph.shed());
        m.box(brass, -14, 42.5f + lift, -8.5f, -13, 44.6f + lift, 2.5f);
        m.box(brass, 13, 42.5f + lift, -8.5f, 14, 44.6f + lift, 2.5f);
        int cols = 9;
        for (int r = 0; r < 5; r++) {
            float kz = -7 + r * 2;
            m.box(iron, -13, 43 + lift, kz - 0.2f, 13, 44.2f + lift, kz + 0.2f);
            for (int c = 0; c < cols; c++) {
                float x = -12 + 24f * c / (cols - 1);
                int sample = c * 5 + r;
                boolean up = d != null && d.dye(sample * d.pixelWidth() / (cols * 5), row) != ph.weft();
                float y = up ? lift : 0, wz = up ? kz + 0.5f : kz + 1;
                m.box(iron, x - 0.15f, 39.5f + y, wz - 0.15f, x + 0.15f, 44.6f + y, wz + 0.15f);
                m.box(iron, x - 0.15f, 44.3f + y, up ? kz - 0.3f : kz + 0.35f, x + 0.15f, 44.6f + y, wz + 0.15f);
            }
        }
    }

    // one row of holes across the card, a hole for each thread pick q lifts
    private static void holes(Mesh m, Design d, JacquardHeadBlockEntity.Picks picks, int q) {
        int Wp = d.pixelWidth(), row = d.pixelHeight() - 1 - picks.row()[q], weft = picks.weft()[q];
        float half = Math.min(0.35f, 10f / Wp * 0.3f);
        TextureAtlasSprite white = Mesh.sprite("createoddsnends:item/white");
        for (int c = 0; c < Wp; c++) {
            if (d.dye(c, row) == weft)
                continue;
            float z = -8 + 10f * (c + 0.5f) / Wp;
            for (float y : new float[] {0.21f, -0.21f})
                m.quad(white, new Vector3f(-0.35f, y, z - half), new Vector3f(0.35f, y, z - half),
                        new Vector3f(0.35f, y, z + half), new Vector3f(-0.35f, y, z + half), new float[] {0, 0, 16, 0, 16, 16, 0, 16},
                        new Vector3f(0, Math.signum(y), 0), HOLE, false);
        }
    }

    // A drop box at each end of the race: a stack of cells, one shuttle per colour, rising and
    // falling so the cell for this pick's weft lines up with the race. A picker in each throws the
    // shuttle out when its side starts the pick. Rides on the batten, so it's drawn in the beater's frame.
    private static void shuttleBoxes(Mesh m, TextureAtlasSprite spruce, TextureAtlasSprite leather, JacquardHeadBlockEntity.Picks picks,
                                     JacquardHeadBlockEntity.Phase ph) {
        int cells = Math.max(1, picks.palette().length);
        double shift = Mth.lerp(ph.boxShift(), cell(picks, ph.lastWeft()), cell(picks, ph.weft()));
        float cellH = 2.2f, base = (float) (RACE - shift * cellH), top = base + cells * cellH;
        for (int side : new int[] {-1, 1}) {
            float inner = side * (18.5f + WIDE), outer = side * (23f + WIDE), x0 = Math.min(inner, outer), x1 = Math.max(inner, outer);
            m.box(spruce, x0, base - 0.3f, -0.6f, x1, top, -0.2f);
            m.box(spruce, x0, base - 0.3f, 2.8f, x1, top, 3.2f);
            m.box(spruce, side > 0 ? 22.6f + WIDE : -23f - WIDE, base - 0.3f, -0.2f, side > 0 ? 23f + WIDE : -22.6f - WIDE, top, 2.8f);
            for (int j = 0; j <= cells; j++)
                m.box(spruce, x0, base + j * cellH - 0.3f, -0.2f, x1, base + j * cellH, 2.8f);
            // the picker on the side the shuttle leaves from snaps inward as it throws
            boolean throwing = side == -ph.dir() && ph.sp() > 0 && ph.sp() < 1;
            float stroke = throwing ? (float) (2 * Math.sin(Math.min(1, ph.sp() / 0.3) * Math.PI)) : 0;
            float pin = side * (22.6f + WIDE - stroke), pout = side * (21.4f + WIDE - stroke);
            m.box(leather, Math.min(pin, pout), RACE + 0.1f, 0.8f, Math.max(pin, pout), RACE + 1.9f, 2.4f);
        }
    }

    private static int cell(JacquardHeadBlockEntity.Picks picks, int dye) {
        int[] palette = picks.palette();
        for (int i = 0; i < palette.length; i++)
            if (palette[i] == dye)
                return i;
        return 0;
    }

    private static float[] along(float[] A, float[] B, double f) {
        return new float[] {(float) Mth.lerp(f, A[0], B[0]), (float) Mth.lerp(f, A[1], B[1])};
    }
}
