package com.mervyn.coolcreatestuff.jacquard.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mervyn.coolcreatestuff.jacquard.Design;
import com.mervyn.coolcreatestuff.jacquard.DesignGen;
import com.mervyn.coolcreatestuff.jacquard.DesignSync;
import com.mervyn.coolcreatestuff.jacquard.Jacquard;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

// A flat item icon drawn from a 16x16 picture of the design, built like item/generated but one
// vertex coloured quad per run of pixels on a white sprite. Until the design has synced the
// plain icon from the item's json stays up.
public class DesignIconModel implements BakedModel {
    private static final FaceBakery BAKERY = new FaceBakery();
    private static final List<DesignIconModel> ALL = new ArrayList<>();

    private final BakedModel base;
    private final Function<Design, int[]> picture;
    private final Int2ObjectMap<BakedModel> cache = new Int2ObjectOpenHashMap<>();
    @Nullable
    private final List<BakedQuad> quads;
    private final ItemOverrides overrides;

    public DesignIconModel(BakedModel base, Function<Design, int[]> picture) {
        this(base, picture, null);
        ALL.add(this);
    }

    private DesignIconModel(BakedModel base, Function<Design, int[]> picture, @Nullable List<BakedQuad> quads) {
        this.base = base;
        this.picture = picture;
        this.quads = quads;
        this.overrides = quads != null ? ItemOverrides.EMPTY : new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                Integer id = stack.get(Jacquard.DESIGN.get());
                if (id == null)
                    return base;
                BakedModel cached = cache.get(id.intValue());
                if (cached != null)
                    return cached;
                Design design = DesignSync.client(id);
                if (design == null)
                    return base;
                BakedModel built = new DesignIconModel(base, picture, build(picture.apply(design)));
                cache.put(id.intValue(), built);
                return built;
            }
        };
    }

    static void clearAll() {
        for (DesignIconModel model : ALL)
            model.cache.clear();
    }

    static void forgetModels() {
        ALL.clear();
    }

    // drawChainIcon()
    static int[] chain(Design d) {
        int[] g = new int[256];
        int Wp = d.pixelWidth(), Hp = d.pixelHeight();
        for (int i = 0; i < 6; i++) {
            int y = 2 + i * 2, x0 = 2 + (i % 2);
            for (int x = x0; x < x0 + 12; x++) {
                g[y * 16 + x] = 0xFFE2D2AC;
                g[(y + 1) * 16 + x] = 0xFFB39F74;
            }
            for (int j = 0; j < 3; j++) {
                int sx = (int) Math.floor((j + 0.5) / 3 * Wp), sy = (int) Math.floor((i + 0.5) / 6 * Hp);
                int[] c = DesignGen.DYE_RGB[d.dye(sx, sy)];
                int rgb = 0xFF000000 | c[0] << 16 | c[1] << 8 | c[2];
                g[y * 16 + x0 + 2 + j * 4] = rgb;
                g[y * 16 + x0 + 3 + j * 4] = rgb;
            }
        }
        for (int y = 1; y < 14; y++) {
            g[y * 16 + 1] = 0xFF7D5F24;
            g[y * 16 + 14] = 0xFF7D5F24;
        }
        return g;
    }

    // the design shrunk to fit 14x14, box-filtered, inside a 1px linen border
    static int[] thumbnail(Design d) {
        int Wp = d.pixelWidth(), Hp = d.pixelHeight();
        double scale = Math.min(14.0 / Wp, 14.0 / Hp);
        int w = Math.max(1, (int) Math.round(Wp * scale)), h = Math.max(1, (int) Math.round(Hp * scale));
        int ox = (16 - w - 2) / 2, oy = (16 - h - 2) / 2;
        int[] g = new int[256];
        for (int y = 0; y < h + 2; y++)
            for (int x = 0; x < w + 2; x++)
                g[(oy + y) * 16 + ox + x] = 0xFFE2DAC6;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int sx0 = x * Wp / w, sx1 = Math.max(sx0 + 1, (x + 1) * Wp / w), sy0 = y * Hp / h, sy1 = Math.max(sy0 + 1, (y + 1) * Hp / h);
                long r = 0, gr = 0, b = 0, n = 0;
                for (int sy = sy0; sy < sy1; sy++)
                    for (int sx = sx0; sx < sx1; sx++) {
                        int[] c = DesignGen.DYE_RGB[d.dye(sx, sy)];
                        r += c[0];
                        gr += c[1];
                        b += c[2];
                        n++;
                    }
                g[(oy + 1 + y) * 16 + ox + 1 + x] = (int) (0xFF000000L | (r / n) << 16 | (gr / n) << 8 | (b / n));
            }
        return g;
    }

    private static List<BakedQuad> build(int[] argb) {
        TextureAtlasSprite white = Mesh.sprite("createcoolstuffs:item/white");
        List<BakedQuad> out = new ArrayList<>();
        for (int y = 0; y < 16; y++) {
            int start = 0;
            for (int x = 1; x <= 16; x++) {
                if (x < 16 && argb[y * 16 + x] == argb[y * 16 + start])
                    continue;
                int c = argb[y * 16 + start];
                if (c >>> 24 != 0) {
                    Vector3f from = new Vector3f(start, 15 - y, 7.5f), to = new Vector3f(x, 16 - y, 8.5f);
                    out.add(quad(from, to, Direction.SOUTH, white, c));
                    out.add(quad(from, to, Direction.NORTH, white, c));
                }
                start = x;
            }
        }
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int c = argb[y * 16 + x];
                if (c >>> 24 == 0)
                    continue;
                if (y == 0 || argb[(y - 1) * 16 + x] >>> 24 == 0)
                    out.add(quad(new Vector3f(x, 16 - y, 7.5f), new Vector3f(x + 1, 16 - y, 8.5f), Direction.UP, white, c));
                if (y == 15 || argb[(y + 1) * 16 + x] >>> 24 == 0)
                    out.add(quad(new Vector3f(x, 15 - y, 7.5f), new Vector3f(x + 1, 15 - y, 8.5f), Direction.DOWN, white, c));
                if (x == 0 || argb[y * 16 + x - 1] >>> 24 == 0)
                    out.add(quad(new Vector3f(x, 15 - y, 7.5f), new Vector3f(x, 16 - y, 8.5f), Direction.WEST, white, c));
                if (x == 15 || argb[y * 16 + x + 1] >>> 24 == 0)
                    out.add(quad(new Vector3f(x + 1, 15 - y, 7.5f), new Vector3f(x + 1, 16 - y, 8.5f), Direction.EAST, white, c));
            }
        return out;
    }

    // item rendering multiplies by the colour baked into each vertex, stored ABGR
    private static BakedQuad quad(Vector3f from, Vector3f to, Direction dir, TextureAtlasSprite white, int argb) {
        BlockElementFace face = new BlockElementFace(null, -1, "", new BlockFaceUV(new float[] {4, 4, 12, 12}, 0));
        BakedQuad quad = BAKERY.bakeQuad(from, to, face, white, dir, BlockModelRotation.X0_Y0, null, true);
        int[] data = quad.getVertices();
        int abgr = DesignTextures.abgr(argb >> 16 & 255, argb >> 8 & 255, argb & 255);
        int stride = data.length / 4;
        for (int i = 0; i < 4; i++)
            data[i * stride + 3] = abgr;
        return quad;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        if (quads == null)
            return base.getQuads(state, side, rand);
        return side == null ? quads : List.of();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return base.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return base.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }
}
