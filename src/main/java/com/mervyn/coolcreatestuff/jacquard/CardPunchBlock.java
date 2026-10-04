package com.mervyn.coolcreatestuff.jacquard;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// Facing is the front, where punched cards come out; the shaft goes in on the left.
public class CardPunchBlock extends HorizontalKineticBlock implements IBE<CardPunchBlockEntity> {
    private static final VoxelShaper SHAPE = VoxelShaper.forHorizontal(LoomShapes.PUNCH, Direction.SOUTH);

    public CardPunchBlock(Properties properties) {
        super(properties);
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
        return SHAPE.get(state.getValue(HORIZONTAL_FACING));
    }

    @Override
    public Class<CardPunchBlockEntity> getBlockEntityClass() {
        return CardPunchBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CardPunchBlockEntity> getBlockEntityType() {
        return Jacquard.CARD_PUNCH_BE.get();
    }
}
