package com.mervyn.tracer.turned;

import com.mervyn.tracer.Profile;
import com.mervyn.tracer.Tracer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public class TurnedBlockEntity extends BlockEntity {
    public static final ModelProperty<Profile> PROFILE = new ModelProperty<>();
    public static final ModelProperty<BlockState> SOURCE = new ModelProperty<>();

    private Profile profile = Profile.FULL;
    private Block source = Blocks.OAK_PLANKS;

    public TurnedBlockEntity(BlockPos pos, BlockState state) {
        super(Tracer.TURNED_BLOCK_BE.get(), pos, state);
    }

    public Profile getProfile() {
        return profile;
    }

    public Block getSource() {
        return source;
    }

    public void setShape(Profile profile, Block source) {
        this.profile = profile;
        this.source = source;
        setChanged();
        updateLight();
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateLight();
    }

    private void updateLight() {
        if (level == null)
            return;
        AuxiliaryLightManager lights = level.getAuxLightManager(worldPosition);
        int light = source.defaultBlockState().getLightEmission();
        if (lights != null)
            lights.setLightAt(worldPosition, light);
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder()
                .with(PROFILE, profile)
                .with(SOURCE, source.defaultBlockState())
                .build();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByteArray("Profile", profile.toBytes());
        tag.putString("Source", BuiltInRegistries.BLOCK.getKey(source).toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        profile = Profile.fromBytes(tag.getByteArray("Profile"), Profile.FULL);
        if (tag.contains("Source"))
            source = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(tag.getString("Source")));
        if (level != null && level.isClientSide) {
            updateLight();
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        profile = input.getOrDefault(Tracer.PROFILE.get(), Profile.FULL);
        source = input.getOrDefault(Tracer.SOURCE_BLOCK.get(), Blocks.OAK_PLANKS);
        updateLight();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(Tracer.PROFILE.get(), profile);
        components.set(Tracer.SOURCE_BLOCK.get(), source);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("Profile");
        tag.remove("Source");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
