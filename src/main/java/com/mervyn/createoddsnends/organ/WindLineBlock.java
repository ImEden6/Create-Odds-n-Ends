package com.mervyn.createoddsnends.organ;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// A thin copper pipe, 2 px wide at the height the preview runs it (y 3 to 5), that joins up with
// other wind lines, wind chests and the organ.
public class WindLineBlock extends Block {
    private static final VoxelShape CORE = Block.box(6.5, 2.5, 6.5, 9.5, 5.5, 9.5);
    private static final Map<Direction, VoxelShape> ARMS = Map.of(
            Direction.NORTH, Block.box(7, 3, 0, 9, 5, 8),
            Direction.SOUTH, Block.box(7, 3, 8, 9, 5, 16),
            Direction.WEST, Block.box(0, 3, 7, 8, 5, 9),
            Direction.EAST, Block.box(8, 3, 7, 16, 5, 9),
            Direction.UP, Block.box(7, 4, 7, 9, 16, 9),
            Direction.DOWN, Block.box(7, 0, 7, 9, 4, 9));
    private final Map<BlockState, VoxelShape> shapes = new HashMap<>();

    public WindLineBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty side : PipeBlock.PROPERTY_BY_DIRECTION.values())
            state = state.setValue(side, false);
        registerDefaultState(state);
        for (BlockState s : stateDefinition.getPossibleStates())
            shapes.put(s, shape(s));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PipeBlock.PROPERTY_BY_DIRECTION.values().toArray(BooleanProperty[]::new));
    }

    private static boolean joins(BlockState neighbor, Direction towards) {
        if (neighbor.is(Organ.BARREL_ORGAN.get()))
            return BarrelOrganBlock.windSide(neighbor) == towards.getOpposite();
        return neighbor.is(Organ.WIND_LINE.get()) || neighbor.is(Organ.WIND_CHEST.get());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction d : Direction.values())
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d), joins(context.getLevel().getBlockState(context.getClickedPos().relative(d)), d));
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), joins(neighbor, direction));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide)
            WindNetwork.changed(level);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide)
            WindNetwork.changed(level);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state);
    }

    private static VoxelShape shape(BlockState state) {
        VoxelShape shape = CORE;
        for (Direction d : Direction.values())
            if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d)))
                shape = Shapes.or(shape, ARMS.get(d));
        return shape;
    }
}
