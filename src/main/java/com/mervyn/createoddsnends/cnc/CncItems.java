package com.mervyn.createoddsnends.cnc;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

// What funnels, chutes and arms see on any part of a formed mill: the first nine slots are the input
// (insert only), the next nine the output (extract only). Looked up on every call like LatheItems.
public class CncItems implements IItemHandler {
    private final Level level;
    private final BlockPos pos;

    public CncItems(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    @Nullable
    private CncBlockEntity cnc() {
        return CncStructure.find(level, pos);
    }

    @Override
    public int getSlots() {
        return cnc() == null ? 0 : CncBlockEntity.SLOTS * 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        CncBlockEntity cnc = cnc();
        if (cnc == null)
            return ItemStack.EMPTY;
        return slot < CncBlockEntity.SLOTS ? cnc.input.getStackInSlot(slot) : cnc.output.getStackInSlot(slot - CncBlockEntity.SLOTS);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        CncBlockEntity cnc = cnc();
        return cnc == null || slot >= CncBlockEntity.SLOTS || !CncJob.accepts(level, stack) ? stack : cnc.input.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        CncBlockEntity cnc = cnc();
        return cnc == null || slot < CncBlockEntity.SLOTS ? ItemStack.EMPTY : cnc.output.extractItem(slot - CncBlockEntity.SLOTS, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot < CncBlockEntity.SLOTS && cnc() != null && CncJob.accepts(level, stack);
    }
}
