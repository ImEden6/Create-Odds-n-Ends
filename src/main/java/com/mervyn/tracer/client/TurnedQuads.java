package com.mervyn.tracer.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mervyn.tracer.Profile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

// Builds a turned shape out of the source block's own textures. Faces flush with the block edge
// are kept apart so chunk rendering can cull them like a normal block.
public final class TurnedQuads {
    private record Key(Profile profile, BlockState source) {}

    private static final int UNCULLED = 6;
    private static final int ALL = 7;
    private static final Map<Key, List<List<BakedQuad>>> CACHE = new ConcurrentHashMap<>();
    private static final FaceBakery BAKERY = new FaceBakery();

    private TurnedQuads() {}

    public static List<BakedQuad> get(Profile profile, BlockState source, @Nullable Direction cull) {
        return lists(profile, source).get(cull == null ? UNCULLED : cull.get3DDataValue());
    }

    public static List<BakedQuad> all(Profile profile, BlockState source) {
        return lists(profile, source).get(ALL);
    }

    public static void clear() {
        CACHE.clear();
    }

    private static List<List<BakedQuad>> lists(Profile profile, BlockState source) {
        return CACHE.computeIfAbsent(new Key(profile, source), k -> build(k.profile, k.source));
    }

    private static List<List<BakedQuad>> build(Profile profile, BlockState source) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(source);
        TextureAtlasSprite[] sprites = new TextureAtlasSprite[6];
        int[] tints = new int[6];
        RandomSource rand = RandomSource.create();
        for (Direction d : Direction.values()) {
            rand.setSeed(42);
            BakedQuad quad = model.getQuads(source, d, rand, ModelData.EMPTY, null).stream().findFirst().orElse(null);
            if (quad == null) {
                rand.setSeed(42);
                quad = model.getQuads(source, null, rand, ModelData.EMPTY, null).stream()
                        .filter(q -> q.getDirection() == d)
                        .findFirst().orElse(null);
            }
            sprites[d.get3DDataValue()] = quad != null ? quad.getSprite() : model.getParticleIcon(ModelData.EMPTY);
            tints[d.get3DDataValue()] = quad != null ? quad.getTintIndex() : -1;
        }

        List<List<BakedQuad>> lists = new ArrayList<>();
        for (int i = 0; i <= ALL; i++)
            lists.add(new ArrayList<>());

        int start = 0;
        for (int end = 1; end <= Profile.LAYERS; end++) {
            if (end < Profile.LAYERS && profile.radius(end) == profile.radius(start))
                continue;
            int r = profile.radius(start);
            Vector3f from = new Vector3f(8 - r, start, 8 - r);
            Vector3f to = new Vector3f(8 + r, end, 8 + r);

            for (Direction side : Direction.Plane.HORIZONTAL)
                add(lists, from, to, side, r == Profile.MAX_RADIUS, sprites, tints);
            if (start == 0 || profile.radius(start - 1) < r)
                add(lists, from, to, Direction.DOWN, start == 0, sprites, tints);
            if (end == Profile.LAYERS || profile.radius(end) < r)
                add(lists, from, to, Direction.UP, end == Profile.LAYERS, sprites, tints);
            start = end;
        }
        return lists;
    }

    private static void add(List<List<BakedQuad>> lists, Vector3f from, Vector3f to, Direction dir, boolean flush,
                            TextureAtlasSprite[] sprites, int[] tints) {
        int i = dir.get3DDataValue();
        BlockElementFace face = new BlockElementFace(flush ? dir : null, tints[i], "", new BlockFaceUV(uvs(from, to, dir), 0));
        BakedQuad quad = BAKERY.bakeQuad(from, to, face, sprites[i], dir, BlockModelRotation.X0_Y0, null, true);
        lists.get(flush ? i : UNCULLED).add(quad);
        lists.get(ALL).add(quad);
    }

    // same auto UVs vanilla gives a model element with no "uv" set
    private static float[] uvs(Vector3f from, Vector3f to, Direction dir) {
        return switch (dir) {
            case DOWN -> new float[] {from.x, 16 - to.z, to.x, 16 - from.z};
            case UP -> new float[] {from.x, from.z, to.x, to.z};
            case NORTH -> new float[] {16 - to.x, 16 - to.y, 16 - from.x, 16 - from.y};
            case SOUTH -> new float[] {from.x, 16 - to.y, to.x, 16 - from.y};
            case WEST -> new float[] {from.z, 16 - to.y, to.z, 16 - from.y};
            case EAST -> new float[] {16 - to.z, 16 - to.y, 16 - from.z, 16 - from.y};
        };
    }
}
