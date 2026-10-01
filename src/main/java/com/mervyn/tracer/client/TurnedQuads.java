package com.mervyn.tracer.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mervyn.tracer.Profile;
import com.mervyn.tracer.turned.TurnedBlock;

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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.data.ModelData;

// Builds a turned shape out of the source block's own textures. Faces flush with the block edge
// are kept apart so chunk rendering can cull them like a normal block.
public final class TurnedQuads {
    private record Key(Profile profile, BlockState source, Direction.Axis axis) {}

    // one texture layer of a source face; grass sides have two, the dirt and a tinted fringe
    private record Layer(TextureAtlasSprite sprite, int tint) {}

    private static final int UNCULLED = 6;
    private static final int ALL = 7;
    private static final Map<Key, List<List<BakedQuad>>> CACHE = new ConcurrentHashMap<>();
    private static final FaceBakery BAKERY = new FaceBakery();

    private TurnedQuads() {}

    public static List<BakedQuad> get(Profile profile, BlockState source, Direction.Axis axis, @Nullable Direction cull) {
        return lists(profile, source, axis).get(cull == null ? UNCULLED : cull.get3DDataValue());
    }

    public static List<BakedQuad> all(Profile profile, BlockState source) {
        return lists(profile, source, Direction.Axis.Y).get(ALL);
    }

    public static void clear() {
        CACHE.clear();
    }

    private static List<List<BakedQuad>> lists(Profile profile, BlockState source, Direction.Axis axis) {
        return CACHE.computeIfAbsent(new Key(profile, source, axis), k -> build(k.profile, k.source, rotation(k.axis)));
    }

    // the same turns a log's blockstate uses; TurnedBlock.layer mirrors these for the outline
    private static BlockModelRotation rotation(Direction.Axis axis) {
        return switch (axis) {
            case Y -> BlockModelRotation.X0_Y0;
            case X -> BlockModelRotation.X90_Y90;
            case Z -> BlockModelRotation.X90_Y0;
        };
    }

    private static List<List<BakedQuad>> build(Profile profile, BlockState source, BlockModelRotation rotation) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(source);
        List<List<Layer>> faces = new ArrayList<>();
        RandomSource rand = RandomSource.create();
        for (Direction d : Direction.values()) {
            rand.setSeed(42);
            List<BakedQuad> quads = model.getQuads(source, d, rand, ModelData.EMPTY, null);
            if (quads.isEmpty()) {
                rand.setSeed(42);
                quads = model.getQuads(source, null, rand, ModelData.EMPTY, null).stream()
                        .filter(q -> q.getDirection() == d)
                        .toList();
            }
            List<Layer> layers = new ArrayList<>();
            for (BakedQuad quad : quads)
                layers.add(new Layer(quad.getSprite(), quad.getTintIndex()));
            if (layers.isEmpty())
                layers.add(new Layer(model.getParticleIcon(ModelData.EMPTY), -1));
            faces.add(layers);
        }

        boolean endGrain = source.hasProperty(BlockStateProperties.AXIS) || source.is(TurnedBlock.END_GRAIN);
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
                add(lists, from, to, side, r == Profile.MAX_RADIUS, faces, side, uvs(from, to, side), rotation);
            if (start == 0)
                add(lists, from, to, Direction.DOWN, true, faces, Direction.DOWN, uvs(from, to, Direction.DOWN), rotation);
            else if (profile.radius(start - 1) < r)
                ledge(lists, start, end, r, profile.radius(start - 1), Direction.DOWN, start, endGrain, faces, rotation);
            if (end == Profile.LAYERS)
                add(lists, from, to, Direction.UP, true, faces, Direction.UP, uvs(from, to, Direction.UP), rotation);
            else if (profile.radius(end) < r)
                ledge(lists, start, end, r, profile.radius(end), Direction.UP, end - 1, endGrain, faces, rotation);
            start = end;
        }
        return lists;
    }

    private static void add(List<List<BakedQuad>> lists, Vector3f from, Vector3f to, Direction dir, boolean flush,
                            List<List<Layer>> faces, Direction textureFrom, float[] uv, BlockModelRotation rotation) {
        add(lists, from, to, dir, flush, faces, textureFrom, new BlockFaceUV(uv, 0), rotation);
    }

    private static void add(List<List<BakedQuad>> lists, Vector3f from, Vector3f to, Direction dir, boolean flush,
                            List<List<Layer>> faces, Direction textureFrom, BlockFaceUV uv, BlockModelRotation rotation) {
        for (Layer layer : faces.get(textureFrom.get3DDataValue())) {
            BlockElementFace face = new BlockElementFace(flush ? dir : null, layer.tint, "", uv);
            BakedQuad quad = BAKERY.bakeQuad(from, to, face, layer.sprite, dir, rotation, null, true);
            lists.get(flush ? quad.getDirection().get3DDataValue() : UNCULLED).add(quad);
            lists.get(ALL).add(quad);
        }
    }

    // A ledge between steps shows what the block is made of at that height. It's split into four
    // strips, each showing the one-pixel row for layer y of the side it faces, stretched inward:
    // grass gets its fringe near the top and dirt lower down, and a front face only shows on its
    // own side. End grain sources (logs, pillars) show their end texture instead.
    private static void ledge(List<List<BakedQuad>> lists, int start, int end, int r, int inner, Direction dir, int y,
                              boolean endGrain, List<List<Layer>> faces, BlockModelRotation rotation) {
        Vector3f from = new Vector3f(8 - r, start, 8 - r);
        Vector3f to = new Vector3f(8 + r, end, 8 + r);
        if (endGrain) {
            add(lists, from, to, dir, false, faces, dir, uvs(from, to, dir), rotation);
            return;
        }
        float lo = 8 - inner;
        float hi = 8 + inner;
        float v0 = 15 - y;
        float v1 = 16 - y;
        boolean up = dir == Direction.UP;
        // up and down faces run u along x; the east and west strips turn it 90 degrees to run along z,
        // which flips its direction on a down face
        add(lists, from, new Vector3f(to.x, end, lo), dir, false, faces, Direction.NORTH,
                new float[] {16 - from.x, v0, 16 - to.x, v1}, rotation);
        add(lists, new Vector3f(from.x, start, hi), to, dir, false, faces, Direction.SOUTH,
                new float[] {from.x, v0, to.x, v1}, rotation);
        add(lists, new Vector3f(from.x, start, lo), new Vector3f(lo, end, hi), dir, false, faces, Direction.WEST,
                new BlockFaceUV(up ? new float[] {lo, v0, hi, v1} : new float[] {hi, v0, lo, v1}, 90), rotation);
        add(lists, new Vector3f(hi, start, lo), new Vector3f(to.x, end, hi), dir, false, faces, Direction.EAST,
                new BlockFaceUV(up ? new float[] {16 - lo, v0, 16 - hi, v1} : new float[] {16 - hi, v0, 16 - lo, v1}, 90), rotation);
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
