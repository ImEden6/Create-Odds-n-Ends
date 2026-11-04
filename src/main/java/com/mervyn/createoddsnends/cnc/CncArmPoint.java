package com.mervyn.createoddsnends.cnc;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

// Arms only target registered point types. Any block of a formed mill takes parts into the input
// and gives up finished pieces, the same as a funnel on it would.
public class CncArmPoint extends ArmInteractionPoint {
    public CncArmPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
        super(type, level, pos, state);
    }

    @Override
    protected IItemHandler getHandler(ArmBlockEntity arm) {
        return new CncItems(level, pos);
    }

    public static class Type extends ArmInteractionPointType {
        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return state.hasProperty(CncFrameBlock.FORMED) && state.getValue(CncFrameBlock.FORMED);
        }

        @Override
        public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new CncArmPoint(this, level, pos, state);
        }
    }
}
