package com.mervyn.createoddsnends.cnc;

import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FACING;
import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FORMED;
import static com.mervyn.createoddsnends.cnc.CncFrameBlock.PART;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public class CncItems implements IItemHandler {
    public enum Hatch { INPUT, OUTPUT }

    private final Level level;
    private final BlockPos pos;
    private final Hatch hatch;

    public CncItems(Level level, BlockPos pos, Hatch hatch) {
        this.level = level;
        this.pos = pos;
        this.hatch = hatch;
    }

    @Nullable
    public static Hatch hatch(BlockState state, @Nullable Direction side) {
        if (!state.is(Cnc.CNC_FRAME.get()) || !state.getValue(FORMED))
            return null;
        int part = state.getValue(PART);
        int a = part % CncStructure.WIDE, u = part / CncStructure.WIDE % CncStructure.HIGH, b = part / (CncStructure.WIDE * CncStructure.HIGH);
        if (b != 1)
            return null;
        if (u == CncStructure.HIGH - 1 && (a == 1 || a == 2) && (side == null || side == Direction.UP))
            return Hatch.INPUT;
        if (u == 0 && a == 0 && (side == null || side == state.getValue(FACING).getCounterClockWise()))
            return Hatch.OUTPUT;
        return null;
    }

    @Nullable
    private CncBlockEntity cnc() {
        return CncStructure.find(level, pos);
    }

    @Override
    public int getSlots() {
        return cnc() == null ? 0 : CncBlockEntity.SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        CncBlockEntity cnc = cnc();
        if (cnc == null)
            return ItemStack.EMPTY;
        return hatch == Hatch.INPUT ? cnc.input.getStackInSlot(slot) : cnc.output.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        CncBlockEntity cnc = cnc();
        return cnc == null || hatch != Hatch.INPUT || !CncJob.accepts(level, stack) ? stack : cnc.input.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        CncBlockEntity cnc = cnc();
        return cnc == null || hatch != Hatch.OUTPUT ? ItemStack.EMPTY : cnc.output.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return hatch == Hatch.INPUT && cnc() != null && CncJob.accepts(level, stack);
    }
}
