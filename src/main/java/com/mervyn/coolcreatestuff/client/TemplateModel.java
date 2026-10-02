package com.mervyn.coolcreatestuff.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mervyn.coolcreatestuff.Profile;
import com.mervyn.coolcreatestuff.Tracer;

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
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

// Profile template icon, built the way item/generated builds a flat item but one texel at a time,
// so each row can show the piece's width in the cut sheet over the brass plate.
public class TemplateModel implements BakedModel {
    private static final FaceBakery BAKERY = new FaceBakery();
    private static final ResourceLocation PLATE = ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "item/profile_template");
    private static final ResourceLocation CUT = ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "item/profile_template_cut");
    private static final Map<Profile, TemplateModel> CACHE = new LinkedHashMap<>(16, .75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Profile, TemplateModel> eldest) {
            return size() > 1024;
        }
    };

    private final BakedModel base;
    @Nullable
    private final Profile profile;
    @Nullable
    private List<BakedQuad> quads;
    private final ItemOverrides overrides;

    public TemplateModel(BakedModel base) {
        this(base, null);
    }

    private TemplateModel(BakedModel base, @Nullable Profile profile) {
        this.base = base;
        this.profile = profile;
        this.overrides = profile != null ? ItemOverrides.EMPTY : new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                Profile written = stack.get(Tracer.PROFILE.get());
                return written == null ? model : CACHE.computeIfAbsent(written, p -> new TemplateModel(base, p));
            }
        };
    }

    public static void clear() {
        CACHE.clear();
    }

    private boolean cut(int x, int y) {
        if (profile == null)
            return false;
        int r = profile.radius(Profile.LAYERS - 1 - y);
        return x >= Profile.MAX_RADIUS - r && x < Profile.MAX_RADIUS + r;
    }

    private List<BakedQuad> build() {
        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite plate = atlas.getSprite(PLATE);
        TextureAtlasSprite cutSprite = atlas.getSprite(CUT);
        List<BakedQuad> out = new ArrayList<>();
        for (int y = 0; y < 16; y++) {
            int start = 0;
            for (int x = 1; x <= 16; x++) {
                if (x < 16 && cut(x, y) == cut(start, y))
                    continue;
                TextureAtlasSprite sprite = cut(start, y) ? cutSprite : plate;
                float top = 16 - y;
                Vector3f from = new Vector3f(start, top - 1, 7.5f);
                Vector3f to = new Vector3f(x, top, 8.5f);
                out.add(quad(from, to, Direction.SOUTH, new float[] {start, y, x, y + 1}, sprite));
                out.add(quad(from, to, Direction.NORTH, new float[] {x, y, start, y + 1}, sprite));
                start = x;
            }
        }
        for (Direction dir : Direction.values())
            if (dir.getAxis() != Direction.Axis.Z)
                edge(out, dir, plate, cutSprite);
        return out;
    }

    private void edge(List<BakedQuad> out, Direction dir, TextureAtlasSprite plate, TextureAtlasSprite cutSprite) {
        int start = 0;
        for (int i = 1; i <= 16; i++) {
            if (i < 16 && edgeCut(dir, i) == edgeCut(dir, start))
                continue;
            TextureAtlasSprite sprite = edgeCut(dir, start) ? cutSprite : plate;
            switch (dir) {
                case UP -> out.add(quad(new Vector3f(start, 16, 7.5f), new Vector3f(i, 16, 8.5f), dir, new float[] {start, 0, i, 1}, sprite));
                case DOWN -> out.add(quad(new Vector3f(start, 0, 7.5f), new Vector3f(i, 0, 8.5f), dir, new float[] {start, 15, i, 16}, sprite));
                case WEST -> out.add(quad(new Vector3f(0, 16 - i, 7.5f), new Vector3f(0, 16 - start, 8.5f), dir, new float[] {0, start, 1, i}, sprite));
                default -> out.add(quad(new Vector3f(16, 16 - i, 7.5f), new Vector3f(16, 16 - start, 8.5f), dir, new float[] {15, start, 16, i}, sprite));
            }
            start = i;
        }
    }

    private boolean edgeCut(Direction dir, int i) {
        return switch (dir) {
            case UP -> cut(i, 0);
            case DOWN -> cut(i, 15);
            case WEST -> cut(0, i);
            default -> cut(15, i);
        };
    }

    private static BakedQuad quad(Vector3f from, Vector3f to, Direction dir, float[] uv, TextureAtlasSprite sprite) {
        BlockElementFace face = new BlockElementFace(null, -1, "", new BlockFaceUV(uv, 0));
        return BAKERY.bakeQuad(from, to, face, sprite, dir, BlockModelRotation.X0_Y0, null, true);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        if (side != null)
            return List.of();
        if (quads == null)
            quads = build();
        return quads;
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
