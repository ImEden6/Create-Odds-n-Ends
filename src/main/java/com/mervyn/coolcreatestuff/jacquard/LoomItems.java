package com.mervyn.coolcreatestuff.jacquard;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

// What funnels, hoppers and arms see on any loom part: slot 0 takes a card chain, slot 1 takes
// wool into the thread buffer, slot 2 is the woven output and the only one that extracts.
// Like the lathe, the head is looked up on every call so forming and breaking need no invalidation.
public class LoomItems implements IItemHandler {
    private final Level level;
    private final BlockPos pos;

    public LoomItems(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    @Nullable
    private JacquardHeadBlockEntity loom() {
        return LoomStructure.loom(level, pos);
    }

    @Override
    public int getSlots() {
        return loom() == null ? 0 : 3;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        JacquardHeadBlockEntity loom = loom();
        if (loom == null)
            return ItemStack.EMPTY;
        return slot == 0 ? loom.chain.getStackInSlot(0) : slot == 2 ? loom.output.getStackInSlot(0) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        JacquardHeadBlockEntity loom = loom();
        if (loom == null)
            return stack;
        if (slot == 0)
            return loom.chain.insertItem(0, stack, simulate);
        if (slot == 1)
            return loom.addThread(stack, simulate);
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        JacquardHeadBlockEntity loom = loom();
        return loom != null && slot == 2 ? loom.output.extractItem(0, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == 0 ? 1 : 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        JacquardHeadBlockEntity loom = loom();
        if (loom == null)
            return false;
        return slot == 0 ? loom.chain.isItemValid(0, stack) : slot == 1 && JacquardHeadBlockEntity.woolColour(stack) >= 0;
    }
}
