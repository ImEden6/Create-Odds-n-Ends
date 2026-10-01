package com.mervyn.tracer.lathe;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

// What funnels, hoppers and arms see on any lathe part: slot 0 is the input (insert only), slot 1
// the output (extract only). The headstock is looked up on every call, so forming, breaking and
// re-forming need no capability invalidation; an unformed part just has no slots.
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

    @Override
    public int getSlots() {
        return lathe() == null ? 0 : 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        HeadstockBlockEntity lathe = lathe();
        if (lathe == null)
            return ItemStack.EMPTY;
        return (slot == 0 ? lathe.input : lathe.output).getStackInSlot(0);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        HeadstockBlockEntity lathe = lathe();
        if (lathe == null || slot != 0)
            return stack;
        return lathe.input.insertItem(0, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        HeadstockBlockEntity lathe = lathe();
        if (lathe == null || slot != 1)
            return ItemStack.EMPTY;
        return lathe.output.extractItem(0, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == 0 ? 1 : 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 && Turnable.allows(stack);
    }
}
