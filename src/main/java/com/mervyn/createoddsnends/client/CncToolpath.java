package com.mervyn.createoddsnends.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Where the tool tip goes over the part on the table, worked out once per item and cached.
// Points are px from the middle of the part's footprint (x, z) and px above the table (y). The part
// is drawn 8 px across, so the footprint runs -4..4. Flat items lie on the table and get a raster
// pass over their solid pixels, blocks get one over the top of their model, anything that gives
// nothing usable gets a plain square pass.
public final class CncToolpath {
    static final float SIZE = 8;
    private static final int GRID = 16;
    private static final float CLEAR = 2;
    private static final Map<Item, CncToolpath> CACHE = new HashMap<>();

    final boolean flat;
    private final float[][] points;
    private final float[] along;

    private CncToolpath(boolean flat, List<float[]> points) {
        this.flat = flat;
        this.points = points.toArray(float[][]::new);
        along = new float[this.points.length];
        for (int i = 1; i < along.length; i++)
            along[i] = along[i - 1] + (float) Math.sqrt(dist2(this.points[i - 1], this.points[i]));
    }

    private static float dist2(float[] a, float[] b) {
        float x = a[0] - b[0], y = a[1] - b[1], z = a[2] - b[2];
        return x * x + y * y + z * z;
    }

    public static CncToolpath of(ItemStack stack, Level level) {
        return CACHE.computeIfAbsent(stack.getItem(), item -> build(stack, level));
    }

    // position after travelling this far along the path, looping
    float[] at(float distance) {
        float total = along[along.length - 1];
        if (total <= 0)
            return points[0];
        float d = distance % total;
        int i = 1;
        while (i < along.length - 1 && along[i] < d)
            i++;
        float span = along[i] - along[i - 1];
        float k = span <= 0 ? 0 : (d - along[i - 1]) / span;
        float[] a = points[i - 1], b = points[i];
        return new float[] {Mth.lerp(k, a[0], b[0]), Mth.lerp(k, a[1], b[1]), Mth.lerp(k, a[2], b[2])};
    }

    private static CncToolpath build(ItemStack stack, Level level) {
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, level, null, 0);
        float[][] height = new float[GRID][GRID];
        boolean flat = !model.isGui3d();
        if (flat)
            sprite(model, height);
        else
            tops(model, height);
        List<float[]> path = raster(height);
        if (path.size() < 2)
            path = raster(square(flat ? .5f / SIZE : 1));
        return new CncToolpath(flat, path);
    }

    private static void sprite(BakedModel model, float[][] height) {
        SpriteContents sprite = model.getParticleIcon().contents();
        int w = sprite.width(), h = sprite.height();
        for (int x = 0; x < GRID; x++)
            for (int z = 0; z < GRID; z++)
                if (!sprite.isTransparent(0, x * w / GRID, z * h / GRID))
                    height[x][z] = .5f / SIZE;
    }

    // tallest point of the model over each column, in model units 0..1
    private static void tops(BakedModel model, float[][] height) {
        RandomSource random = RandomSource.create(42);
        List<BakedQuad> quads = new ArrayList<>(model.getQuads(null, null, random));
        for (Direction d : Direction.values())
            quads.addAll(model.getQuads(null, d, random));
        int stride = DefaultVertexFormat.BLOCK.getVertexSize() / 4;
        for (BakedQuad quad : quads) {
            int[] v = quad.getVertices();
            float minX = 1, maxX = 0, minZ = 1, maxZ = 0, top = 0;
            for (int i = 0; i < 4; i++) {
                float x = Float.intBitsToFloat(v[i * stride]), y = Float.intBitsToFloat(v[i * stride + 1]), z = Float.intBitsToFloat(v[i * stride + 2]);
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
                top = Math.max(top, y);
            }
            for (int x = Mth.clamp((int) (minX * GRID), 0, GRID - 1); x < Mth.clamp(Mth.ceil(maxX * GRID), 1, GRID); x++)
                for (int z = Mth.clamp((int) (minZ * GRID), 0, GRID - 1); z < Mth.clamp(Mth.ceil(maxZ * GRID), 1, GRID); z++)
                    height[x][z] = Math.max(height[x][z], Mth.clamp(top, 0, 1));
        }
    }

    private static float[][] square(float h) {
        float[][] height = new float[GRID][GRID];
        for (int x = 4; x < GRID - 4; x++)
            for (int z = 4; z < GRID - 4; z++)
                height[x][z] = h;
        return height;
    }

    // every other row, back and forth, cutting along each solid run and lifting clear between runs
    private static List<float[]> raster(float[][] height) {
        List<float[]> path = new ArrayList<>();
        boolean forward = true;
        for (int z = 0; z < GRID; z += 2) {
            List<int[]> runs = new ArrayList<>();
            int start = -1;
            for (int x = 0; x <= GRID; x++) {
                boolean solid = x < GRID && height[x][z] > 0;
                if (solid && start < 0)
                    start = x;
                if (!solid && start >= 0) {
                    runs.add(new int[] {start, x - 1});
                    start = -1;
                }
            }
            if (!forward)
                Collections.reverse(runs);
            for (int[] run : runs) {
                int from = forward ? run[0] : run[1], to = forward ? run[1] : run[0];
                float top = 0;
                for (int x = run[0]; x <= run[1]; x++)
                    top = Math.max(top, height[x][z]);
                float y = top * SIZE;
                float pz = px(z);
                path.add(new float[] {px(from), y + CLEAR, pz});
                path.add(new float[] {px(from), y, pz});
                int step = forward ? 1 : -1;
                for (int x = from; x != to + step; x += step)
                    path.add(new float[] {px(x), height[x][z] * SIZE, pz});
                path.add(new float[] {px(to), y + CLEAR, pz});
            }
            if (!runs.isEmpty())
                forward = !forward;
        }
        return path;
    }

    private static float px(int cell) {
        return (cell + .5f) / GRID * SIZE - SIZE / 2;
    }
}
