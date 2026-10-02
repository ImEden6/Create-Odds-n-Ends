package com.mervyn.coolcreatestuff.lathe;

import com.mervyn.coolcreatestuff.Tracer;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.createmod.catnip.math.VoxelShaper;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.level.BlockEvent;

public class LathePartBlock extends Block implements IWrenchable {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    // outlines follow the models, authored facing north
    public record Outline(VoxelShaper unformed, VoxelShaper formed) {
        static Outline of(VoxelShape unformed, VoxelShape formed) {
            return new Outline(VoxelShaper.forHorizontal(unformed, Direction.NORTH), VoxelShaper.forHorizontal(formed, Direction.NORTH));
        }
    }

    public static final Outline BED = Outline.of(
            Shapes.or(box(0, 0, 0, 16, 13, 16), box(1, 13, 1, 15, 16, 15)),
            Shapes.or(box(0, 0, 0, 16, 13, 16), box(1, 13, 0, 15, 16, 16)));
    public static final Outline TAILSTOCK = Outline.of(
            Shapes.or(box(1, 0, 1, 15, 14, 14), box(6, 6, 14, 10, 10, 16)),
            Shapes.or(box(0, 0, 0, 16, 16, 12), box(5, 5, 12, 11, 11, 16)));
    public static final Outline RAIL = Outline.of(
            Shapes.or(box(0, 0, 0, 16, 13, 16), box(5, 13, 1, 11, 16, 15)),
            Shapes.or(box(0, 0, 0, 16, 13, 16), box(5, 13, 0, 11, 16, 16)));
    public static final Outline HOLDER = Outline.of(
            Shapes.or(box(2, 0, 2, 14, 12, 14), box(6, 12, 6, 10, 16, 10)),
            Shapes.or(box(2, 0, 3, 14, 14, 13), box(6, 6, 0, 10, 10, 16)));

    private final Outline outline;

    public LathePartBlock(Properties properties, Outline outline) {
        super(properties);
        this.outline = outline;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(FORMED) ? outline.formed() : outline.unformed()).get(state.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, placedFacing(context));
    }

    // headstock on the player's left, bed running off to their right, like a real lathe
    static Direction placedFacing(BlockPlaceContext context) {
        return context.getHorizontalDirection().getClockWise();
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (!state.getValue(FORMED))
            return IWrenchable.super.onWrenched(state, context);
        BlockPos controller = LatheStructure.findController(context.getLevel(), context.getClickedPos(), state);
        if (controller == null)
            return InteractionResult.PASS;
        return LatheStructure.wrench(context.getLevel(), controller, context.getPlayer());
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
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && state.getValue(FORMED))
            LatheStructure.breakFrom(level, pos, state);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static void keepSlotsClear(BlockEvent.EntityPlaceEvent event) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockState next = event.getLevel().getBlockState(event.getPos().relative(d));
            boolean headstock = next.is(Tracer.LATHE_HEADSTOCK.get());
            boolean holder = next.is(Tracer.TRACER_HOLDER.get())
                    && event.getLevel().getBlockState(event.getPos().relative(d.getOpposite())).is(Tracer.TRACER_HOLDER.get());
            if ((headstock || holder) && next.getValue(FORMED) && next.getValue(FACING) == d.getOpposite()) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
