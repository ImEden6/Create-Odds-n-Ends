package com.mervyn.coolcreatestuff.organ.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mervyn.coolcreatestuff.jacquard.DesignGen;
import com.mervyn.coolcreatestuff.organ.Organ;
import com.mervyn.coolcreatestuff.organ.Score;
import com.mervyn.coolcreatestuff.organ.ScoreSync;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

// The book music icon drawn from its score, one vertex coloured quad per run of pixels on a white
// sprite like the Jacquard icons. The plain book from the item's json shows until the score syncs.
public class BookIconModel implements BakedModel {
    private static final FaceBakery BAKERY = new FaceBakery();
    private static BookIconModel current;

    private final BakedModel base;
    private final Int2ObjectMap<BakedModel> cache = new Int2ObjectOpenHashMap<>();
    @Nullable
    private final List<BakedQuad> quads;
    private final ItemOverrides overrides;

    public BookIconModel(BakedModel base) {
        this(base, null);
        current = this;
    }

    private BookIconModel(BakedModel base, @Nullable List<BakedQuad> quads) {
        this.base = base;
        this.quads = quads;
        this.overrides = quads != null ? ItemOverrides.EMPTY : new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                Integer id = stack.get(Organ.SCORE.get());
                if (id == null)
                    return base;
                BakedModel cached = cache.get(id.intValue());
                if (cached != null)
                    return cached;
                Score score = ScoreSync.client(id);
                if (score == null)
                    return base;
                BakedModel built = new BookIconModel(base, build(icon(score)));
                cache.put(id.intValue(), built);
                return built;
            }
        };
    }

    static void clearAll() {
        if (current != null)
            current.cache.clear();
    }

    // drawBookIcon()
    static int[] icon(Score score) {
        int[] g = new int[256];
        int[] cells = score.grid();
        for (int i = 0; i < 6; i++) {
            int y = 2 + i * 2, x0 = 2 + (i % 2);
            for (int x = x0; x < x0 + 12; x++) {
                g[y * 16 + x] = 0xFFE2D2AC;
                g[(y + 1) * 16 + x] = 0xFFB39F74;
            }
            for (int j = 0; j < 4; j++) {
                int r = (int) Math.floor((i * 4 + j + 0.5) / 24 * score.rowCount());
                int col = 0;
                for (int c = Score.COLUMNS - 1; c >= 2; c--) {
                    int cell = cells[r * Score.COLUMNS + c];
                    if (cell != 0) {
                        int[] rgb = DesignGen.DYE_RGB[cell - 1];
                        col = 0xFF000000 | rgb[0] << 16 | rgb[1] << 8 | rgb[2];
                        break;
                    }
                }
                if (col != 0) {
                    g[y * 16 + x0 + 1 + j * 3] = col;
                    g[y * 16 + x0 + 2 + j * 3] = col;
                }
            }
        }
        for (int y = 1; y < 14; y++) {
            g[y * 16 + 1] = 0xFF7D5F24;
            g[y * 16 + 14] = 0xFF7D5F24;
        }
        return g;
    }

    private static List<BakedQuad> build(int[] argb) {
        TextureAtlasSprite white = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(ResourceLocation.parse("createcoolstuffs:item/white"));
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
        int abgr = 0xFF000000 | (argb & 255) << 16 | (argb >> 8 & 255) << 8 | (argb >> 16 & 255);
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
