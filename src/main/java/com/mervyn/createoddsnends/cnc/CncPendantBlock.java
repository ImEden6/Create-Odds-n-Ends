package com.mervyn.createoddsnends.cnc;

import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FACING;
import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FORMED;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CncPendantBlock extends Block implements IBE<CncBlockEntity>, IWrenchable {
    private static final VoxelShaper SHAPE = VoxelShaper.forHorizontal(Shapes.or(box(2, 1, 2, 14, 15, 8), box(9, 11, 5, 16, 13, 7)), Direction.NORTH);

    public CncPendantBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.get(state.getValue(FACING));
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return CncStructure.wrench(context.getLevel(), context.getClickedPos(), context.getPlayer());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        CncBlockEntity cnc = CncStructure.find(level, pos);
        return cnc != null ? cnc.pressDoorButton() : InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && state.getValue(FORMED))
            CncStructure.unform(level, pos, state.getValue(FACING));
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public Class<CncBlockEntity> getBlockEntityClass() {
        return CncBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CncBlockEntity> getBlockEntityType() {
        return Cnc.CNC_BE.get();
    }
}
