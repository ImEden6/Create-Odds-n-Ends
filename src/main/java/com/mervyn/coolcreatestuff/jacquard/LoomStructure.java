package com.mervyn.coolcreatestuff.jacquard;

import static com.mervyn.coolcreatestuff.jacquard.LoomPartBlock.FACING;
import static com.mervyn.coolcreatestuff.jacquard.LoomPartBlock.FORMED;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// Layout is relative to the jacquard head, which sits top, back, middle. Cells are (x, y, z)
// with x running left to right seen from the front (counter-clockwise of facing), y up and z
// toward the front (facing). The two middle cells stay hollow and get filled with loom voids.
public final class LoomStructure {
    record Spot(int x, int y, int z, @Nullable Supplier<? extends Block> block, @Nullable LoomCell cell) {}

    static final List<Spot> SPOTS = spots();

    private LoomStructure() {}

    private static List<Spot> spots() {
        List<Spot> out = new ArrayList<>();
        out.add(new Spot(1, 2, 0, Jacquard.JACQUARD_HEAD, null));
        out.add(new Spot(0, 0, 1, Jacquard.LOOM_DRIVE, null));
        for (LoomCell cell : LoomCell.values())
            out.add(new Spot(cell.x, cell.y, cell.z, Jacquard.LOOM_FRAME, cell));
        out.add(new Spot(1, 1, 0, null, null));
        out.add(new Spot(1, 1, 1, null, null));
        return out;
    }

    static BlockPos at(BlockPos head, Direction facing, int x, int y, int z) {
        return head.relative(facing.getCounterClockWise(), x - 1).above(y - 2).relative(facing, z);
    }

    static InteractionResult wrench(Level level, BlockPos head, @Nullable Player player) {
        if (level.isClientSide || player == null)
            return InteractionResult.SUCCESS;
        if (level.getBlockState(head).getValue(FORMED))
            return InteractionResult.SUCCESS;
        Direction facing = level.getBlockState(head).getValue(FACING);
        Component problem = check(level, head, facing);
        if (problem != null) {
            player.displayClientMessage(problem, true);
            AllSoundEvents.DENY.playOnServer(level, head);
            return InteractionResult.SUCCESS;
        }
        form(level, head, facing);
        IWrenchable.playRotateSound(level, head);
        player.displayClientMessage(Component.translatable("createcoolstuffs.loom.formed"), true);
        return InteractionResult.SUCCESS;
    }

    @Nullable
    private static Component check(Level level, BlockPos head, Direction facing) {
        for (Spot spot : SPOTS) {
            BlockPos pos = at(head, facing, spot.x, spot.y, spot.z);
            BlockState state = level.getBlockState(pos);
            String where = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            if (spot.block == null) {
                if (!state.isAir())
                    return Component.translatable("createcoolstuffs.loom.blocked", where);
                continue;
            }
            Block expected = spot.block.get();
            if (!state.is(expected))
                return Component.translatable("createcoolstuffs.loom.missing", expected.getName(), where);
            if (state.getValue(FORMED))
                return Component.translatable("createcoolstuffs.loom.taken", expected.getName(), where);
        }
        return null;
    }

    private static void form(Level level, BlockPos head, Direction facing) {
        for (Spot spot : SPOTS) {
            BlockPos pos = at(head, facing, spot.x, spot.y, spot.z);
            if (spot.block == null) {
                level.setBlock(pos, Jacquard.LOOM_VOID.get().defaultBlockState(), Block.UPDATE_ALL);
                continue;
            }
            BlockState state = level.getBlockState(pos).setValue(FORMED, true).setValue(FACING, facing);
            if (spot.cell != null)
                state = state.setValue(LoomFrameBlock.POSITION, spot.cell);
            if (state.getBlock() instanceof LoomDriveBlock)
                KineticBlockEntity.switchToBlockState(level, pos, state);
            else
                level.setBlock(pos, state, Block.UPDATE_ALL);
        }
    }

    static void unform(Level level, BlockPos head, Direction facing) {
        for (Spot spot : SPOTS) {
            BlockPos pos = at(head, facing, spot.x, spot.y, spot.z);
            BlockState state = level.getBlockState(pos);
            if (spot.block == null) {
                if (state.is(Jacquard.LOOM_VOID.get()))
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                continue;
            }
            if (!state.is(spot.block.get()) || !state.getValue(FORMED) || state.getValue(FACING) != facing)
                continue;
            if (spot.cell != null && state.getValue(LoomFrameBlock.POSITION) != spot.cell)
                continue;
            BlockState unformed = state.setValue(FORMED, false);
            if (state.getBlock() instanceof LoomDriveBlock)
                KineticBlockEntity.switchToBlockState(level, pos, unformed);
            else
                level.setBlock(pos, unformed, Block.UPDATE_ALL);
        }
    }

    static void breakFrom(Level level, BlockPos pos, BlockState removed) {
        if (level.isClientSide || !removed.getValue(FORMED))
            return;
        BlockPos head = findHead(pos, removed);
        BlockState headState = level.getBlockState(head);
        if (head.equals(pos) || headState.is(Jacquard.JACQUARD_HEAD.get()) && headState.getValue(FORMED))
            unform(level, head, removed.getValue(FACING));
    }

    // every formed part knows its own cell and facing, so the head is a fixed offset away
    public static BlockPos findHead(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        int x = 1, y = 2, z = 0;
        if (state.getBlock() instanceof LoomDriveBlock) {
            x = 0;
            y = 0;
            z = 1;
        } else if (state.hasProperty(LoomFrameBlock.POSITION)) {
            LoomCell cell = state.getValue(LoomFrameBlock.POSITION);
            x = cell.x;
            y = cell.y;
            z = cell.z;
        }
        return pos.relative(facing.getCounterClockWise(), 1 - x).above(2 - y).relative(facing, -z);
    }
}
