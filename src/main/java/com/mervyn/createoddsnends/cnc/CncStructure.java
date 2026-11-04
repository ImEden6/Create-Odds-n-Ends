package com.mervyn.createoddsnends.cnc;

import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FACING;
import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FORMED;
import static com.mervyn.createoddsnends.cnc.CncFrameBlock.PART;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

// The pendant hangs on the right of the enclosure as you face the doors; the drive takes the place of
// the frame in the middle of the left end. The 4x3x3 box of frames runs
// from the block beside it ("side", clockwise of facing), from one below to one above it, and back
// away from the doors. Each frame keeps its part index, so it can always find the pendant again.
public final class CncStructure {
    static final int WIDE = 4, HIGH = 3, DEEP = 3;
    static final int DRIVE_PART = 3 + WIDE * HIGH;

    private CncStructure() {}

    static int part(int along, int up, int back) {
        return along + WIDE * (up + HIGH * back);
    }

    static Block block(int along, int up, int back) {
        return part(along, up, back) == DRIVE_PART ? Cnc.CNC_DRIVE.get() : Cnc.CNC_FRAME.get();
    }

    static BlockPos drive(BlockPos pendant, Direction facing) {
        return at(pendant, facing, WIDE - 1, 0, 1);
    }

    static BlockPos at(BlockPos pendant, Direction facing, int along, int up, int back) {
        return pendant.relative(facing.getClockWise(), along + 1).above(up - 1).relative(facing.getOpposite(), back);
    }

    static InteractionResult wrench(Level level, BlockPos pendant, @Nullable Player player) {
        if (level.isClientSide || player == null)
            return InteractionResult.SUCCESS;
        Direction facing = level.getBlockState(pendant).getValue(FACING);
        Component problem = check(level, pendant, facing);
        if (problem != null) {
            player.displayClientMessage(problem, true);
            AllSoundEvents.DENY.playOnServer(level, pendant);
            return InteractionResult.SUCCESS;
        }
        form(level, pendant, facing);
        IWrenchable.playRotateSound(level, pendant);
        player.displayClientMessage(Component.translatable("createoddsnends.cnc.formed"), true);
        return InteractionResult.SUCCESS;
    }

    @Nullable
    private static Component check(Level level, BlockPos pendant, Direction facing) {
        for (int a = 0; a < WIDE; a++)
            for (int u = 0; u < HIGH; u++)
                for (int b = 0; b < DEEP; b++) {
                    BlockPos pos = at(pendant, facing, a, u, b);
                    BlockState state = level.getBlockState(pos);
                    String where = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
                    Block wanted = block(a, u, b);
                    if (!state.is(wanted))
                        return Component.translatable("createoddsnends.cnc.missing", wanted.getName(), where);
                    if (state.getValue(FORMED) && !pendant.equals(findPendant(level, pos, state)))
                        return Component.translatable("createoddsnends.cnc.taken", where);
                }
        return null;
    }

    static void form(Level level, BlockPos pendant, Direction facing) {
        for (int a = 0; a < WIDE; a++)
            for (int u = 0; u < HIGH; u++)
                for (int b = 0; b < DEEP; b++) {
                    BlockPos pos = at(pendant, facing, a, u, b);
                    BlockState state = level.getBlockState(pos);
                    BlockState formed = state.setValue(FORMED, true).setValue(FACING, facing).setValue(PART, part(a, u, b));
                    if (formed != state)
                        level.setBlock(pos, formed, Block.UPDATE_ALL);
                }
        BlockState state = level.getBlockState(pendant);
        level.setBlock(pendant, state.setValue(FORMED, true), Block.UPDATE_ALL);
    }

    static void unform(Level level, BlockPos pendant, Direction facing) {
        for (int a = 0; a < WIDE; a++)
            for (int u = 0; u < HIGH; u++)
                for (int b = 0; b < DEEP; b++) {
                    BlockPos pos = at(pendant, facing, a, u, b);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(block(a, u, b)) && state.getValue(FORMED) && state.getValue(FACING) == facing && state.getValue(PART) == part(a, u, b))
                        level.setBlock(pos, state.setValue(FORMED, false), Block.UPDATE_ALL);
                }
        BlockState state = level.getBlockState(pendant);
        if (state.is(Cnc.CNC_PENDANT.get()) && state.getValue(FORMED))
            level.setBlock(pendant, state.setValue(FORMED, false), Block.UPDATE_ALL);
    }

    @Nullable
    public static BlockPos findPendant(Level level, BlockPos pos, BlockState state) {
        if (state.is(Cnc.CNC_PENDANT.get()))
            return pos;
        if (!(state.is(Cnc.CNC_FRAME.get()) || state.is(Cnc.CNC_DRIVE.get())) || !state.getValue(FORMED))
            return null;
        Direction facing = state.getValue(FACING);
        int part = state.getValue(PART);
        int a = part % WIDE, u = part / WIDE % HIGH, b = part / (WIDE * HIGH);
        BlockPos pendant = pos.relative(facing.getOpposite(), -b).below(u - 1).relative(facing.getClockWise(), -(a + 1));
        BlockState p = level.getBlockState(pendant);
        return p.is(Cnc.CNC_PENDANT.get()) && p.getValue(FORMED) && p.getValue(FACING) == facing ? pendant : null;
    }

    @Nullable
    public static CncBlockEntity find(Level level, BlockPos pos) {
        BlockPos pendant = findPendant(level, pos, level.getBlockState(pos));
        return pendant != null && level.getBlockEntity(pendant) instanceof CncBlockEntity be && be.getBlockState().getValue(FORMED) ? be : null;
    }

    // dev showcase: a formed mill on a creative motor with cobblestone waiting to be cut
    public static void showcase(ServerPlayer player, BlockPos pendant, Direction facing) {
        Level level = player.serverLevel();
        BlockPos.betweenClosed(pendant.relative(facing, 2).relative(facing.getCounterClockWise(), 2).below(),
                        pendant.relative(facing.getOpposite(), DEEP + 1).relative(facing.getClockWise(), WIDE + 3).above(3))
                .forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
        level.setBlock(pendant, Cnc.CNC_PENDANT.get().defaultBlockState().setValue(FACING, facing), Block.UPDATE_ALL);
        for (int a = 0; a < WIDE; a++)
            for (int u = 0; u < HIGH; u++)
                for (int b = 0; b < DEEP; b++)
                    level.setBlock(at(pendant, facing, a, u, b), block(a, u, b).defaultBlockState().setValue(FACING, facing), Block.UPDATE_ALL);
        form(level, pendant, facing);
        BlockPos motor = drive(pendant, facing).relative(facing.getClockWise());
        level.setBlock(motor, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, facing.getCounterClockWise()), Block.UPDATE_ALL);
        if (level.getBlockEntity(motor) instanceof CreativeMotorBlockEntity be)
            be.generatedSpeed.setValue(64);
        if (level.getBlockEntity(pendant) instanceof CncBlockEntity be) {
            be.input.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 64));
        }
    }
}
