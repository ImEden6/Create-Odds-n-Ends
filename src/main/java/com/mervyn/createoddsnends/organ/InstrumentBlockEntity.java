package com.mervyn.createoddsnends.organ;

import java.util.List;

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
