package com.mervyn.coolcreatestuff.lathe;

import org.jetbrains.annotations.Nullable;

import com.mervyn.coolcreatestuff.Tracer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

// What funnels, hoppers and arms see on any lathe part. On the lathe itself slot 0 is the input
// (insert only) and slot 1 the output (extract only); on the tracer row the one slot is the master,
// insert only so nothing pulls it out mid cut.
// The headstock is looked up on every call, so forming, breaking and re-forming need no
// capability invalidation; an unformed part just has no slots.
public class LatheItems implements IItemHandler {
    private final Level level;
    private final BlockPos pos;

    public LatheItems(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    @Nullable
    private HeadstockBlockEntity lathe() {
        return HeadstockBlockEntity.find(level, pos);
    }

    private boolean tracerPart() {
        BlockState state = level.getBlockState(pos);
        return state.is(Tracer.TRACER_RAIL.get()) || state.is(Tracer.TRACER_HOLDER.get());
    }

    @Nullable
    private ItemStackHandler slot(int slot) {
        HeadstockBlockEntity lathe = lathe();
        if (lathe == null)
            return null;
        if (tracerPart())
            return slot == 0 && lathe.hasTracer() ? lathe.master : null;
        return slot == 0 ? lathe.input : slot == 1 ? lathe.output : null;
    }

    @Override
    public int getSlots() {
        HeadstockBlockEntity lathe = lathe();
        if (lathe == null)
            return 0;
        if (tracerPart())
            return lathe.hasTracer() ? 1 : 0;
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        ItemStackHandler handler = slot(slot);
        return handler == null ? ItemStack.EMPTY : handler.getStackInSlot(0);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        ItemStackHandler handler = slot == 0 ? slot(0) : null;
        return handler == null ? stack : handler.insertItem(0, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStackHandler handler = slot == 1 ? slot(1) : null;
        return handler == null ? ItemStack.EMPTY : handler.extractItem(0, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == 1 ? 64 : 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        ItemStackHandler handler = slot == 0 ? slot(0) : null;
        return handler != null && handler.isItemValid(0, stack);
    }
}
