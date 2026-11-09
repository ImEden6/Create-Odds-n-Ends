package com.mervyn.coolcreatestuff.organ;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.world.item.context.BlockPlaceContext;
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
    public Class<WindChestBlockEntity> getBlockEntityClass() {
        return WindChestBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends WindChestBlockEntity> getBlockEntityType() {
        return Organ.WIND_CHEST_BE.get();
    }
}
