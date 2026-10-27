package com.mervyn.createoddsnends.cnc;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class CncFrameBlock extends Block implements IWrenchable {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, CncStructure.WIDE * CncStructure.HIGH * CncStructure.DEEP - 1);

    public CncFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(FORMED, false).setValue(PART, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, PART);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        BlockPos pendant = CncStructure.findPendant(context.getLevel(), context.getClickedPos(), state);
        if (pendant == null)
            return InteractionResult.PASS;
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
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && state.getValue(FORMED)) {
            BlockPos pendant = CncStructure.findPendant(level, pos, state);
            if (pendant != null)
                CncStructure.unform(level, pendant, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
