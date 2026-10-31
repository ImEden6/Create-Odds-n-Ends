package com.mervyn.createoddsnends.cnc;

import java.util.List;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CncDriveBlockEntity extends KineticBlockEntity {
    static final float STRESS_IMPACT = 8;

    public CncDriveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public float calculateStressApplied() {
        lastStressApplied = STRESS_IMPACT;
        return STRESS_IMPACT;
    }
}
