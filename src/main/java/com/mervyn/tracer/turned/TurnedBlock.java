package com.mervyn.tracer.turned;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mervyn.tracer.Profile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TurnedBlock extends Block implements EntityBlock {
    private static final Map<Profile, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    public TurnedBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurnedBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof TurnedBlockEntity be)
            return shapeOf(be.getProfile());
        return Shapes.block();
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof TurnedBlockEntity be)
            stack.applyComponents(be.collectComponents());
        return stack;
    }

    public static VoxelShape shapeOf(Profile profile) {
        return SHAPES.computeIfAbsent(profile, p -> {
            VoxelShape shape = Shapes.empty();
            int start = 0;
            for (int i = 1; i <= Profile.LAYERS; i++) {
                if (i < Profile.LAYERS && p.radius(i) == p.radius(start))
                    continue;
                int r = p.radius(start);
                shape = Shapes.or(shape, Block.box(8 - r, start, 8 - r, 8 + r, i, 8 + r));
                start = i;
            }
            return shape.optimize();
        });
    }
}
