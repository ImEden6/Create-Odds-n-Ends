package com.mervyn.coolcreatestuff.jacquard;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class LoomDriveBlockEntity extends KineticBlockEntity {
    public LoomDriveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide || !getBlockState().getValue(LoomPartBlock.FORMED))
            return;
        BlockPos head = LoomStructure.findHead(worldPosition, getBlockState());
        if (level.getBlockEntity(head) instanceof JacquardHeadBlockEntity be)
            be.setSpeed(getSpeed());
    }
}
