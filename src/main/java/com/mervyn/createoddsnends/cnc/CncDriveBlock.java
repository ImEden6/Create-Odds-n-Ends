package com.mervyn.createoddsnends.cnc;

import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FACING;
import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FORMED;
import static com.mervyn.createoddsnends.cnc.CncFrameBlock.PART;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

// The frame in the middle of the mill's left end, with a shaft socket facing out. Facing is the
// mill's facing, so the shaft side is clockwise of it, same as the way the frames run from the pendant.
public class CncDriveBlock extends KineticBlock implements IBE<CncDriveBlockEntity> {
    public CncDriveBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(FORMED, false).setValue(PART, CncStructure.DRIVE_PART));
    }

    public static Direction shaftSide(BlockState state) {
        return state.getValue(FACING).getClockWise();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, PART);
        super.createBlockStateDefinition(builder);
    }

    // placed while looking at the left end of the mill, so the socket faces back at you
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise());
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return shaftSide(state).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == shaftSide(state);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        BlockPos pendant = CncStructure.findPendant(context.getLevel(), context.getClickedPos(), state);
        if (pendant == null)
            return super.onWrenched(state, context);
        return CncStructure.wrench(context.getLevel(), pendant, context.getPlayer());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        CncBlockEntity cnc = CncStructure.find(level, pos);
        return cnc != null ? cnc.useItem(player, stack) : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        CncBlockEntity cnc = CncStructure.find(level, pos);
        return cnc != null ? cnc.useEmptyHand(player) : InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && state.getValue(FORMED)) {
            BlockPos pendant = CncStructure.findPendant(level, pos, state);
            if (pendant != null)
                CncStructure.unform(level, pendant, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, isMoving);
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
    public Class<CncDriveBlockEntity> getBlockEntityClass() {
        return CncDriveBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CncDriveBlockEntity> getBlockEntityType() {
        return Cnc.CNC_DRIVE_BE.get();
    }
}
