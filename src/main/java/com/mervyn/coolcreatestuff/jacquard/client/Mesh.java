package com.mervyn.coolcreatestuff.jacquard.client;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

// The preview's box6 / quadT / prismX drawn straight into a vertex consumer, in preview pixels
// (16 = 1 block). Faces are tinted with the preview's shadeOf(), the block game face shading.
final class Mesh {
    static final int WHITE = 0xFFFFFF;

    private final PoseStack ms;
    private final VertexConsumer vc;
    private final int light;
    private final boolean entity;

    Mesh(PoseStack ms, VertexConsumer vc, int light, boolean entity) {
        this.ms = ms;
        this.vc = vc;
        this.light = light;
        this.entity = entity;
    }

    static TextureAtlasSprite sprite(String id) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.parse(id));
    }

    // rotX(phi, py, pz): rotation about the x axis through (py, pz)
    void rotX(double phi, float py, float pz) {
        ms.translate(0, py, pz);
        ms.mulPose(Axis.XP.rotation((float) phi));
    }

    // rotY(phi, px, pz)
    void rotY(double phi, float px, float pz) {
        ms.translate(px, 0, pz);
        ms.mulPose(Axis.YP.rotation((float) phi));
    }

    // rotZ(th, px, py)
    void rotZ(double th, float px, float py) {
        ms.translate(px, py, 0);
        ms.mulPose(Axis.ZP.rotation((float) th));
    }

    void push() {
        ms.pushPose();
    }

    void pop() {
        ms.popPose();
    }

    // box6(): every face tiles its texture once per 16 px, like the preview's world-space UVs
    void box(TextureAtlasSprite s, float x0, float y0, float z0, float x1, float y1, float z1) {
        box(s, WHITE, x0, y0, z0, x1, y1, z1);
    }

    void box(TextureAtlasSprite s, int rgb, float x0, float y0, float z0, float x1, float y1, float z1) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        face(s, new Vector3f(x0, y1, z1), new Vector3f(1, 0, 0), dx, new Vector3f(0, 0, -1), dz, new Vector3f(0, 1, 0), rgb);
        face(s, new Vector3f(x0, y0, z0), new Vector3f(1, 0, 0), dx, new Vector3f(0, 0, 1), dz, new Vector3f(0, -1, 0), rgb);
        face(s, new Vector3f(x1, y1, z1), new Vector3f(-1, 0, 0), dx, new Vector3f(0, -1, 0), dy, new Vector3f(0, 0, 1), rgb);
        face(s, new Vector3f(x0, y1, z0), new Vector3f(1, 0, 0), dx, new Vector3f(0, -1, 0), dy, new Vector3f(0, 0, -1), rgb);
        face(s, new Vector3f(x1, y1, z0), new Vector3f(0, 0, 1), dz, new Vector3f(0, -1, 0), dy, new Vector3f(1, 0, 0), rgb);
        face(s, new Vector3f(x0, y1, z1), new Vector3f(0, 0, -1), dz, new Vector3f(0, -1, 0), dy, new Vector3f(-1, 0, 0), rgb);
    }

    // tiles are centred on the face, the same way tools/jacquard.py lays out the baked parts
    private void face(TextureAtlasSprite s, Vector3f o, Vector3f u, float uLen, Vector3f v, float vLen, Vector3f n, int rgb) {
        float uStart = start(uLen), vStart = start(vLen);
        for (float ut = uStart; ut < uLen - 1e-4f; ut += 16)
            for (float vt = vStart; vt < vLen - 1e-4f; vt += 16) {
                float ua = Math.max(0, ut), ub = Math.min(ut + 16, uLen), va = Math.max(0, vt), vb = Math.min(vt + 16, vLen);
                quad(s, at(o, u, ua, v, va), at(o, u, ub, v, va), at(o, u, ub, v, vb), at(o, u, ua, v, vb),
                        new float[] {ua - ut, va - vt, ub - ut, va - vt, ub - ut, vb - vt, ua - ut, vb - vt}, n, rgb, false);
            }
    }

    private static float start(float len) {
        int tiles = Math.max(1, (int) Math.ceil(len / 16 - 1e-4f));
        return -(tiles * 16 - len) / 2;
    }

    // A shaft along one axis (0 x, 1 y, 2 z), textured the way Create's shaft model is: the 4 px
    // strip down the middle of axis.png runs along the length, axis_top caps the ends.
    void shaft(int axis, float x0, float y0, float z0, float x1, float y1, float z1) {
        TextureAtlasSprite side = sprite("create:block/axis"), end = sprite("create:block/axis_top");
        float[] lo = {x0, y0, z0}, hi = {x1, y1, z1};
        float len = hi[axis] - lo[axis];
        for (int b = 0; b < 3; b++) {
            if (b == axis)
                continue;
            int c = 3 - axis - b;
            for (int s = 0; s < 2; s++) {
                Vector3f o = new Vector3f(lo[0], lo[1], lo[2]);
                o.setComponent(b, s == 0 ? lo[b] : hi[b]);
                for (float la = 0; la < len - 1e-4f; la += 16) {
                    float lb = Math.min(la + 16, len);
                    Vector3f p0 = new Vector3f(o).setComponent(axis, lo[axis] + la), p1 = new Vector3f(o).setComponent(axis, lo[axis] + lb);
                    Vector3f p2 = new Vector3f(p1).setComponent(c, hi[c]), p3 = new Vector3f(p0).setComponent(c, hi[c]);
                    quad(side, p0, p1, p2, p3, new float[] {6, 0, 6, lb - la, 10, lb - la, 10, 0}, unit(b, s == 0 ? -1 : 1), WHITE, false);
                }
            }
        }
        int b = (axis + 1) % 3, c = (axis + 2) % 3;
        for (int s = 0; s < 2; s++) {
            Vector3f p0 = new Vector3f(lo[0], lo[1], lo[2]).setComponent(axis, s == 0 ? lo[axis] : hi[axis]);
            Vector3f p1 = new Vector3f(p0).setComponent(b, hi[b]), p2 = new Vector3f(p1).setComponent(c, hi[c]), p3 = new Vector3f(p0).setComponent(c, hi[c]);
            quad(end, p0, p1, p2, p3, new float[] {6, 6, 10, 6, 10, 10, 6, 10}, unit(axis, s == 0 ? -1 : 1), WHITE, false);
        }
    }

    private static Vector3f unit(int axis, float sign) {
        return new Vector3f().setComponent(axis, sign);
    }

    private static Vector3f at(Vector3f o, Vector3f u, float a, Vector3f v, float b) {
        return new Vector3f(o).add(u.x * a + v.x * b, u.y * a + v.y * b, u.z * a + v.z * b);
    }

    // quadT(), uv in sprite pixels (0-16). Winding follows the given normal.
    void quad(TextureAtlasSprite s, Vector3f a, Vector3f b, Vector3f c, Vector3f d, float[] uv, Vector3f n, int rgb, boolean doubleSided) {
        Vector3f[] p = {a, b, c, d};
        Vector3f cross = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
        if (cross.dot(n) < 0) {
            p = new Vector3f[] {a, d, c, b};
            uv = new float[] {uv[0], uv[1], uv[6], uv[7], uv[4], uv[5], uv[2], uv[3]};
        }
        float[] u = new float[4], v = new float[4];
        for (int i = 0; i < 4; i++) {
            u[i] = s == null ? uv[i * 2] : s.getU(uv[i * 2] / 16);
            v[i] = s == null ? uv[i * 2 + 1] : s.getV(uv[i * 2 + 1] / 16);
        }
        emit(p, u, v, n, rgb);
        if (doubleSided)
            emit(new Vector3f[] {p[0], p[3], p[2], p[1]}, new float[] {u[0], u[3], u[2], u[1]}, new float[] {v[0], v[3], v[2], v[1]},
                    new Vector3f(n).negate(), rgb);
    }

    private void emit(Vector3f[] p, float[] u, float[] v, Vector3f n, int rgb) {
        PoseStack.Pose pose = ms.last();
        Vector3f world = pose.normal().transform(new Vector3f(n)).normalize();
        float shade = shadeOf(world);
        int r = (int) ((rgb >> 16 & 255) * shade), g = (int) ((rgb >> 8 & 255) * shade), b = (int) ((rgb & 255) * shade);
        for (int i = 0; i < 4; i++) {
            vc.addVertex(pose, p[i].x, p[i].y, p[i].z).setColor(r, g, b, 255).setUv(u[i], v[i]);
            if (entity)
                vc.setOverlay(OverlayTexture.NO_OVERLAY);
            vc.setLight(light).setNormal(pose, n.x, n.y, n.z);
        }
    }

    // shadeOf()
    private static float shadeOf(Vector3f n) {
        float x = n.x, y = n.y, z = n.z;
        return Math.min(1, x * x * 0.62f + z * z * 0.82f + (y > 0 ? y * y : y * y * 0.5f));
    }

    // prismX(): an N-sided prism along x through (cy, cz), rotated by phi
    void prismX(TextureAtlasSprite s, float x0, float x1, float cy, float cz, float r, int N, double phi) {
        double half = Math.PI / N, R = r / Math.cos(half), side = 2 * r * Math.tan(half);
        for (int k = 0; k < N; k++) {
            double a0 = phi + half + k * 2 * Math.PI / N, a1 = phi + half + (k + 1) * 2 * Math.PI / N, am = phi + half + (k + 0.5) * 2 * Math.PI / N;
            float ya = (float) (cy + R * Math.cos(a0)), za = (float) (cz + R * Math.sin(a0));
            float yb = (float) (cy + R * Math.cos(a1)), zb = (float) (cz + R * Math.sin(a1));
            float len = Math.min(16, x1 - x0), sv = (float) Math.min(16, side);
            quad(s, new Vector3f(x0, ya, za), new Vector3f(x1, ya, za), new Vector3f(x1, yb, zb), new Vector3f(x0, yb, zb),
                    new float[] {0, 0, len, 0, len, sv, 0, sv}, new Vector3f(0, (float) Math.cos(am), (float) Math.sin(am)), WHITE, false);
            for (float xx : new float[] {x0, x1}) {
                Vector3f c = new Vector3f(xx, cy, cz);
                quad(s, c, new Vector3f(xx, ya, za), new Vector3f(xx, yb, zb), c,
                        new float[] {8, 8, 8 + (ya - cy), 8 + (za - cz), 8 + (yb - cy), 8 + (zb - cz), 8, 8},
                        new Vector3f(xx == x0 ? -1 : 1, 0, 0), WHITE, false);
            }
        }
    }

    // the preview's thread lines, as thin flat strips facing both ways; across is the strip's width direction
    void thread(Vector3f a, Vector3f b, Vector3f across, float width, int rgb) {
        Vector3f off = new Vector3f(across).normalize().mul(width / 2);
        Vector3f n = new Vector3f(b).sub(a).cross(off);
        if (n.lengthSquared() < 1e-8f)
            return;
        quad(sprite("createcoolstuffs:item/white"), new Vector3f(a).sub(off), new Vector3f(a).add(off), new Vector3f(b).add(off), new Vector3f(b).sub(off),
                new float[] {0, 0, 16, 0, 16, 16, 0, 16}, n.normalize(), rgb, true);
    }
}
