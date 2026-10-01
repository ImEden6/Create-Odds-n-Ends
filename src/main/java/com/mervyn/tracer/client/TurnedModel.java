package com.mervyn.tracer.client;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mervyn.tracer.Profile;
import com.mervyn.tracer.Tracer;
import com.mervyn.tracer.turned.TurnedBlock;
import com.mervyn.tracer.turned.TurnedBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;

public class TurnedModel implements IDynamicBakedModel {
    private final BakedModel base;
    @Nullable
    private final Profile fixedProfile;
    @Nullable
    private final BlockState fixedSource;

    private final ItemOverrides overrides;

    public TurnedModel(BakedModel base) {
        this(base, null, null);
    }

    // a model frozen to one shape, for item rendering and Flywheel meshes
    public static TurnedModel of(Profile profile, BlockState source) {
        return new TurnedModel(Minecraft.getInstance().getBlockRenderer().getBlockModel(source), profile, source);
    }

    private TurnedModel(BakedModel base, @Nullable Profile fixedProfile, @Nullable BlockState fixedSource) {
        this.base = base;
        this.fixedProfile = fixedProfile;
        this.fixedSource = fixedSource;
        this.overrides = fixedProfile != null ? ItemOverrides.EMPTY : new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                Profile profile = stack.getOrDefault(Tracer.PROFILE.get(), Profile.FULL);
                BlockState source = stack.getOrDefault(Tracer.SOURCE_BLOCK.get(), Blocks.OAK_PLANKS).defaultBlockState();
                return new TurnedModel(base, profile, source);
            }
        };
    }

    private Profile profile(ModelData data) {
        if (fixedProfile != null)
            return fixedProfile;
        Profile profile = data.get(TurnedBlockEntity.PROFILE);
        return profile != null ? profile : Profile.FULL;
    }

    private BlockState source(ModelData data) {
        if (fixedSource != null)
            return fixedSource;
        BlockState source = data.get(TurnedBlockEntity.SOURCE);
        return source != null ? source : Blocks.OAK_PLANKS.defaultBlockState();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        Direction.Axis axis = fixedProfile == null && state != null && state.hasProperty(TurnedBlock.AXIS) ? state.getValue(TurnedBlock.AXIS) : Direction.Axis.Y;
        return TurnedQuads.get(profile(data), source(data), axis, side);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        BlockState source = source(data);
        ChunkRenderTypeSet types = Minecraft.getInstance().getBlockRenderer().getBlockModel(source).getRenderTypes(source, rand, ModelData.EMPTY);
        return types.isEmpty() ? ChunkRenderTypeSet.of(RenderType.solid()) : ChunkRenderTypeSet.of(types.iterator().next());
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        BlockState source = source(data);
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(source).getParticleIcon(ModelData.EMPTY);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
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
