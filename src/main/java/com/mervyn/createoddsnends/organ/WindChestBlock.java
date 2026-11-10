package com.mervyn.createoddsnends.organ;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class WindChestBlock extends HorizontalDirectionalBlock implements IBE<WindChestBlockEntity> {
    private static final MapCodec<WindChestBlock> CODEC = simpleCodec(WindChestBlock::new);

    public WindChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
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
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    // the 4 sides for redstone, and straight up into a whistle so it switches to its open model
    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!direction.getAxis().isHorizontal() && !(direction == Direction.DOWN && whistleOnTop(level.getBlockState(pos.above()))))
            return 0;
        return level.getBlockEntity(pos) instanceof WindChestBlockEntity chest && chest.isBlowing() ? 15 : 0;
    }

    public static boolean whistleOnTop(BlockState state) {
        return state.getBlock() instanceof WhistleBlock && WhistleBlock.getAttachedDirection(state) == Direction.DOWN;
    }

    @Override
    public Class<WindChestBlockEntity> getBlockEntityClass() {
        return WindChestBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends WindChestBlockEntity> getBlockEntityType() {
        return Organ.WIND_CHEST_BE.get();
    }
}
