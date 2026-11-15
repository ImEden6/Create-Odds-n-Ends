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
    public enum Hatch { INPUT, OUTPUT, FRONT }

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
        if (b == 0)
            return side == null ? Hatch.FRONT : null;
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
        return cnc() == null ? 0 : hatch == Hatch.FRONT ? CncBlockEntity.SLOTS * 2 : CncBlockEntity.SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        CncBlockEntity cnc = cnc();
        if (cnc == null)
            return ItemStack.EMPTY;
        return takesInput(slot) ? cnc.input.getStackInSlot(slot) : cnc.output.getStackInSlot(outputSlot(slot));
    }

    private boolean takesInput(int slot) {
        return hatch == Hatch.INPUT || hatch == Hatch.FRONT && slot < CncBlockEntity.SLOTS;
    }

    private int outputSlot(int slot) {
        return hatch == Hatch.FRONT ? slot - CncBlockEntity.SLOTS : slot;
    }

    private boolean armReaches(CncBlockEntity cnc) {
        if (hatch != Hatch.FRONT)
            return true;
        if (cnc.armCanReach())
            return true;
        cnc.armWaiting();
        return false;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        CncBlockEntity cnc = cnc();
        if (cnc == null || !takesInput(slot) || !CncJob.accepts(level, stack) || !armReaches(cnc))
            return stack;
        ItemStack left = cnc.input.insertItem(slot, stack, simulate);
        if (!simulate && hatch == Hatch.FRONT && left.getCount() != stack.getCount())
            cnc.armVisit();
        return left;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        CncBlockEntity cnc = cnc();
        if (cnc == null || takesInput(slot) || cnc.output.getStackInSlot(outputSlot(slot)).isEmpty() || !armReaches(cnc))
            return ItemStack.EMPTY;
        ItemStack taken = cnc.output.extractItem(outputSlot(slot), amount, simulate);
        if (!simulate && hatch == Hatch.FRONT && !taken.isEmpty())
            cnc.armVisit();
        return taken;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return takesInput(slot) && cnc() != null && CncJob.accepts(level, stack);
    }
}
