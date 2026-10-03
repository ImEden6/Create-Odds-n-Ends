package com.mervyn.coolcreatestuff.jacquard;

import com.simibubi.create.foundation.block.IBE;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class JacquardHeadBlock extends LoomPartBlock implements IBE<JacquardHeadBlockEntity> {
    private static final VoxelShaper FORMED_SHAPE = VoxelShaper.forHorizontal(LoomShapes.HEAD, Direction.SOUTH);

    public JacquardHeadBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return LoomStructure.wrench(context.getLevel(), context.getClickedPos(), context.getPlayer());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FORMED) ? FORMED_SHAPE.get(state.getValue(FACING)) : super.getShape(state, level, pos, context);
    }

    @Override
    public Class<JacquardHeadBlockEntity> getBlockEntityClass() {
        return JacquardHeadBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends JacquardHeadBlockEntity> getBlockEntityType() {
        return Jacquard.JACQUARD_HEAD_BE.get();
    }
}
