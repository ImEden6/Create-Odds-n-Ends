package com.mervyn.createoddsnends.organ;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// The lower half: the kinetic controller with the shaft going in at the back. The upper half is
// BarrelOrganTopBlock and only forwards to this one.
public class BarrelOrganBlock extends HorizontalKineticBlock implements IBE<BarrelOrganBlockEntity> {
    public BarrelOrganBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos above = context.getClickedPos().above();
        Level level = context.getLevel();
        if (above.getY() >= level.getMaxBuildHeight() || !level.getBlockState(above).canBeReplaced(context))
            return null;
        return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos.above(), Organ.BARREL_ORGAN_TOP.get().defaultBlockState()
                .setValue(BarrelOrganTopBlock.FACING, state.getValue(HORIZONTAL_FACING)), Block.UPDATE_ALL);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockState(pos.above()).is(Organ.BARREL_ORGAN_TOP.get()))
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static Direction back(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        InteractionResult result = super.onWrenched(state, context);
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState turned = level.getBlockState(pos);
        if (!level.isClientSide && turned.is(this) && level.getBlockState(pos.above()).is(Organ.BARREL_ORGAN_TOP.get()))
            level.setBlock(pos.above(), level.getBlockState(pos.above()).setValue(BarrelOrganTopBlock.FACING, turned.getValue(HORIZONTAL_FACING)), Block.UPDATE_ALL);
        return result;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == back(state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Organ.BOOK_MUSIC.get()))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return level.getBlockEntity(pos) instanceof BarrelOrganBlockEntity be ? be.useBook(player, stack) : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return level.getBlockEntity(pos) instanceof BarrelOrganBlockEntity be ? be.takeBook(player) : InteractionResult.PASS;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    // all sides but up, so the pulse doesn't pause the organ through its own upper half
    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return direction != Direction.DOWN && level.getBlockEntity(pos) instanceof BarrelOrganBlockEntity be && be.pulsing() ? 15 : 0;
    }

    @Override
    public Class<BarrelOrganBlockEntity> getBlockEntityClass() {
        return BarrelOrganBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BarrelOrganBlockEntity> getBlockEntityType() {
        return Organ.BARREL_ORGAN_BE.get();
    }
}
