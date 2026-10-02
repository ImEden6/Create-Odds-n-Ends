package com.mervyn.coolcreatestuff.lathe;

import static com.mervyn.coolcreatestuff.lathe.LathePartBlock.FORMED;

import com.mervyn.coolcreatestuff.Tracer;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class HeadstockBlock extends HorizontalKineticBlock implements IBE<HeadstockBlockEntity> {

    public HeadstockBlock(Properties properties) {
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
        return defaultBlockState().setValue(HORIZONTAL_FACING, LathePartBlock.placedFacing(context));
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return LatheStructure.wrench(context.getLevel(), context.getClickedPos(), context.getPlayer());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        HeadstockBlockEntity lathe = HeadstockBlockEntity.find(level, pos);
        return lathe != null ? lathe.useItem(player, stack) : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        HeadstockBlockEntity lathe = HeadstockBlockEntity.find(level, pos);
        return lathe != null && player.getMainHandItem().isEmpty() ? lathe.useEmptyHand(player) : InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && state.getValue(FORMED))
            LatheStructure.unform(level, pos, state.getValue(HORIZONTAL_FACING));
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public Class<HeadstockBlockEntity> getBlockEntityClass() {
        return HeadstockBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HeadstockBlockEntity> getBlockEntityType() {
        return Tracer.LATHE_HEADSTOCK_BE.get();
    }
}
