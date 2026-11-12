package com.mervyn.createoddsnends.organ;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// Only stands on a wind chest and drops off when the chest goes.
public class InstrumentBlock extends HorizontalDirectionalBlock implements IBE<InstrumentBlockEntity> {
    private static final VoxelShape DRUM = box(1, 0, 1, 15, 14, 15), PIPE = box(4, 0, 4, 12, 16, 12), BOX = box(2, 0, 2, 14, 12, 14);

    public final Instrument instrument;

    public InstrumentBlock(Instrument instrument, Properties properties) {
        super(properties);
        this.instrument = instrument;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(p -> new InstrumentBlock(instrument, p));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide)
            WindNetwork.changed();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
        if (!level.isClientSide)
            WindNetwork.changed();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(Organ.WIND_CHEST.get());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction side, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (instrument) {
            case KICK_DRUM, SNARE_DRUM -> DRUM;
            case REED_PIPE, WOOD_PIPE -> PIPE;
            case REED_BOX -> BOX;
        };
    }

    @Override
    public Class<InstrumentBlockEntity> getBlockEntityClass() {
        return InstrumentBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends InstrumentBlockEntity> getBlockEntityType() {
        return Organ.INSTRUMENT_BE.get();
    }
}
