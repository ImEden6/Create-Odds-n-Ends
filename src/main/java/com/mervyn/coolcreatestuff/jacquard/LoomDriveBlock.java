package com.mervyn.coolcreatestuff.jacquard;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import static com.mervyn.coolcreatestuff.jacquard.LoomPartBlock.FORMED;

// Bottom front left of the loom. Takes rotation through its outer left face (left as seen from
// the front, clockwise of facing) and passes the speed on to the head.
public class LoomDriveBlock extends HorizontalKineticBlock implements IBE<LoomDriveBlockEntity> {
    private static final VoxelShaper FORMED_SHAPE = VoxelShaper.forHorizontal(LoomShapes.DRIVE, Direction.SOUTH);

    public LoomDriveBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    public static Direction input(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getClockWise();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return input(state).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == input(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FORMED) ? FORMED_SHAPE.get(state.getValue(HORIZONTAL_FACING)) : Shapes.block();
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return state.getValue(FORMED) ? InteractionResult.PASS : super.onWrenched(state, context);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()))
            LoomStructure.breakFrom(level, pos, state);
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public Class<LoomDriveBlockEntity> getBlockEntityClass() {
        return LoomDriveBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LoomDriveBlockEntity> getBlockEntityType() {
        return Jacquard.LOOM_DRIVE_BE.get();
    }
}
