package com.mervyn.tracer.lathe;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

// Arms only target registered point types, and the default point wants a block entity at the
// clicked spot. Lathe parts have none, so the point hands over LatheItems directly.
public class LatheArmPoint extends ArmInteractionPoint {
    public LatheArmPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
        super(type, level, pos, state);
    }

    @Override
    protected IItemHandler getHandler(ArmBlockEntity arm) {
        return new LatheItems(level, pos);
    }

    public static class Type extends ArmInteractionPointType {
        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return state.hasProperty(LathePartBlock.FORMED) && state.getValue(LathePartBlock.FORMED);
        }

        @Override
        public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new LatheArmPoint(this, level, pos, state);
        }
    }
}
