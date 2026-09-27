package com.mervyn.tracer.lathe;

import java.util.List;

import com.mervyn.tracer.Profile;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class HeadstockBlockEntity extends KineticBlockEntity {
    private static final float PLACEHOLDER_STRESS = 4;

    private Profile profile = Profile.BALUSTER;
    private Block source = Blocks.STRIPPED_OAK_LOG;
    private boolean tracer;

    public HeadstockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public float calculateStressApplied() {
        lastStressApplied = PLACEHOLDER_STRESS;
        return PLACEHOLDER_STRESS;
    }

    // covers the formed lathe, the tracer row behind it and the carriage hanging off the front, in any facing
    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(getBlockPos()).inflate(2, 1, 2);
    }

    public Profile getProfile() {
        return profile;
    }

    public Block getSource() {
        return source;
    }

    public boolean hasTracer() {
        return tracer;
    }

    void setTracer(boolean tracer) {
        if (this.tracer == tracer)
            return;
        this.tracer = tracer;
        notifyUpdate();
    }

    public void setShape(Profile profile, Block source) {
        this.profile = profile;
        this.source = source;
        notifyUpdate();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putByteArray("Profile", profile.toBytes());
        tag.putString("Source", BuiltInRegistries.BLOCK.getKey(source).toString());
        tag.putBoolean("Tracer", tracer);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("Profile"))
            profile = Profile.fromBytes(tag.getByteArray("Profile"), Profile.BALUSTER);
        if (tag.contains("Source"))
            source = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(tag.getString("Source")));
        tracer = tag.getBoolean("Tracer");
    }
}
