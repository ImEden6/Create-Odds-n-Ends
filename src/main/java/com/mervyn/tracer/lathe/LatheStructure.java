package com.mervyn.tracer.lathe;

import static com.mervyn.tracer.lathe.LathePartBlock.FACING;
import static com.mervyn.tracer.lathe.LathePartBlock.FORMED;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.mervyn.tracer.Tracer;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

// Layout is relative to the headstock: "along" runs toward the tailstock, "back" is the
// side the tracer row sits on (counter-clockwise of facing, away from the operator). The tracer
// row stands one block back from the lathe, leaving a gap for the stylus.
public final class LatheStructure {
    record Spot(int along, int up, int back, @Nullable Supplier<? extends Block> block) {}

    static final List<Spot> LATHE = List.of(
            new Spot(0, 0, 0, Tracer.LATHE_HEADSTOCK),
            new Spot(1, 0, 0, null),
            new Spot(2, 0, 0, Tracer.LATHE_TAILSTOCK),
            new Spot(0, -1, 0, Tracer.LATHE_BED),
            new Spot(1, -1, 0, Tracer.LATHE_BED),
            new Spot(2, -1, 0, Tracer.LATHE_BED));

    static final List<Spot> TRACER = List.of(
            new Spot(0, 0, 2, Tracer.TRACER_HOLDER),
            new Spot(1, 0, 2, null),
            new Spot(2, 0, 2, Tracer.TRACER_HOLDER),
            new Spot(0, -1, 2, Tracer.TRACER_RAIL),
            new Spot(1, -1, 2, Tracer.TRACER_RAIL),
            new Spot(2, -1, 2, Tracer.TRACER_RAIL));

    private LatheStructure() {}

    public static Direction back(Direction facing) {
        return facing.getCounterClockWise();
    }

    static BlockPos at(BlockPos controller, Direction facing, Spot spot) {
        return controller.relative(facing, spot.along).above(spot.up).relative(back(facing), spot.back);
    }

    static InteractionResult wrench(Level level, BlockPos controller, @Nullable Player player) {
        if (level.isClientSide || player == null)
            return InteractionResult.SUCCESS;
        if (assemble(level, controller, player))
            IWrenchable.playRotateSound(level, controller);
        else
            AllSoundEvents.DENY.playOnServer(level, controller);
        return InteractionResult.SUCCESS;
    }

    private static boolean assemble(Level level, BlockPos controller, Player player) {
        Direction facing = level.getBlockState(controller).getValue(FACING);
        Component problem = check(level, controller, facing, LATHE);
        if (problem != null) {
            player.displayClientMessage(problem, true);
            return false;
        }

        Component tracerProblem = check(level, controller, facing, TRACER);
        boolean tracer = tracerProblem == null;
        form(level, controller, facing, LATHE);
        if (tracer)
            form(level, controller, facing, TRACER);
        if (level.getBlockEntity(controller) instanceof HeadstockBlockEntity be)
            be.setTracer(tracer);

        Component message;
        if (tracer)
            message = Component.translatable("tracer.lathe.formed_tracer");
        else if (startedTracer(level, controller, facing))
            message = Component.translatable("tracer.lathe.formed_tracer_missing", tracerProblem);
        else
            message = Component.translatable("tracer.lathe.formed");
        player.displayClientMessage(message, true);
        return true;
    }

    private static boolean startedTracer(Level level, BlockPos controller, Direction facing) {
        return TRACER.stream().anyMatch(spot -> spot.block != null && level.getBlockState(at(controller, facing, spot)).is(spot.block.get()));
    }

    @Nullable
    private static Component check(Level level, BlockPos controller, Direction facing, List<Spot> spots) {
        for (Spot spot : spots) {
            BlockPos pos = at(controller, facing, spot);
            BlockState state = level.getBlockState(pos);
            String where = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();

            if (spot.block == null) {
                if (!state.isAir())
                    return Component.translatable("tracer.lathe.blocked", where);
                continue;
            }

            Block expected = spot.block.get();
            if (!state.is(expected))
                return Component.translatable("tracer.lathe.missing", expected.getName(), where);
            if (state.getValue(FORMED) && !controller.equals(findController(level, pos, state)))
                return Component.translatable("tracer.lathe.taken", expected.getName(), where);
        }
        return null;
    }

    private static void form(Level level, BlockPos controller, Direction facing, List<Spot> spots) {
        for (Spot spot : spots) {
            if (spot.block == null)
                continue;
            BlockPos pos = at(controller, facing, spot);
            BlockState state = level.getBlockState(pos);
            BlockState formed = state.setValue(FORMED, true).setValue(FACING, facing);
            if (formed != state)
                level.setBlock(pos, formed, Block.UPDATE_ALL);
        }
    }

    static void unform(Level level, BlockPos controller, Direction facing) {
        Stream.concat(LATHE.stream(), TRACER.stream()).forEach(spot -> {
            if (spot.block == null)
                return;
            BlockPos pos = at(controller, facing, spot);
            BlockState state = level.getBlockState(pos);
            if (state.is(spot.block.get()) && state.getValue(FORMED) && state.getValue(FACING) == facing)
                level.setBlock(pos, state.setValue(FORMED, false), Block.UPDATE_ALL);
        });
    }

    static void breakFrom(Level level, BlockPos pos, BlockState removed) {
        if (level.isClientSide)
            return;
        BlockPos controller = findController(level, pos, removed);
        if (controller != null)
            unform(level, controller, removed.getValue(FACING));
    }

    // Parts don't store their controller. Every part was given the controller's facing when it
    // formed, so the headstock can only sit at one of a few offsets from it.
    @Nullable
    public static BlockPos findController(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof HeadstockBlock)
            return pos;
        Direction facing = state.getValue(FACING);
        return Stream.concat(LATHE.stream(), TRACER.stream())
                .filter(spot -> spot.block != null && state.is(spot.block.get()))
                .map(spot -> pos.relative(facing, -spot.along).below(spot.up).relative(back(facing), -spot.back))
                .filter(candidate -> {
                    BlockState c = level.getBlockState(candidate);
                    return c.getBlock() instanceof HeadstockBlock && c.getValue(FORMED) && c.getValue(FACING) == facing;
                })
                .findFirst()
                .orElse(null);
    }
}
