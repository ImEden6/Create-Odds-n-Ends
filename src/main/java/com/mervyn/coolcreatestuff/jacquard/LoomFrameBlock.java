package com.mervyn.coolcreatestuff.jacquard;

import java.util.EnumMap;
import java.util.Map;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LoomFrameBlock extends LoomPartBlock {
    public static final EnumProperty<LoomCell> POSITION = EnumProperty.create("position", LoomCell.class);

    private static final Map<LoomCell, VoxelShaper> SHAPES = new EnumMap<>(LoomCell.class);

    static {
        for (LoomCell cell : LoomCell.values())
            SHAPES.put(cell, VoxelShaper.forHorizontal(LoomShapes.frame(cell.getSerializedName()), Direction.SOUTH));
    }

    public LoomFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POSITION, LoomCell.BOTTOM_BACK_LEFT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POSITION);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(FORMED))
            return super.getShape(state, level, pos, context);
        return SHAPES.get(state.getValue(POSITION)).get(state.getValue(FACING));
    }
}
