package com.mervyn.createoddsnends.organ;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// Only stands on a wind chest and drops off when the chest goes.
public class InstrumentBlock extends HorizontalDirectionalBlock implements IBE<InstrumentBlockEntity> {
    private static final VoxelShaper KICK = VoxelShaper.forHorizontal(Shapes.or(box(2, 0, 2, 14, 13, 12), box(6, 0, 12, 10, 11, 15.5)), Direction.SOUTH),
            SNARE = VoxelShaper.forHorizontal(box(2.5, 0, 3.5, 15.5, 11.5, 14.5), Direction.SOUTH);
    private static final VoxelShape PIPE = box(4, 0, 4, 12, 16, 12), BOX = box(2, 0, 2, 14, 12, 14);

    public final Instrument instrument;
    private final MapCodec<InstrumentBlock> codec;

    public InstrumentBlock(Instrument instrument, Properties properties) {
        super(properties);
        this.instrument = instrument;
        codec = simpleCodec(p -> new InstrumentBlock(instrument, p));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return codec;
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
            case KICK_DRUM -> KICK.get(state.getValue(FACING));
            case SNARE_DRUM -> SNARE.get(state.getValue(FACING));
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
