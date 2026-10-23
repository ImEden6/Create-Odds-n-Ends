package com.mervyn.createoddsnends.organ;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mervyn.createoddsnends.organ.client.InstrumentPlayback;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// Client only state for the animation: hit jumps to 1 on every note and falls off, held counts
// down the ticks the longest note is still sounding.
public class InstrumentBlockEntity extends SmartBlockEntity {
    public float hit, lastHit;
    public int held;
    @Nullable
    private BlockPos owner;
    private long claimedAt;

    public InstrumentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public Instrument instrument() {
        return ((InstrumentBlock) getBlockState().getBlock()).instrument;
    }

    // also how Ponder plays one, it has no organ feeding its chests
    public void strike(int ticks) {
        hit = 1;
        held = Math.max(held, ticks);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (level != null && level.isClientSide)
            InstrumentPlayback.release(this);
    }

    // with two organs on one network, the first to play through an instrument keeps it until it
    // goes quiet for a few seconds, so the scores don't pile up on top of each other
    public boolean claim(BlockPos organ, long now) {
        if (owner != null && !owner.equals(organ) && now - claimedAt < 100)
            return false;
        owner = organ;
        claimedAt = now;
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide)
            return;
        lastHit = hit;
        hit = Math.max(0, hit - 0.2f);
        if (held > 0)
            held--;
    }
}
