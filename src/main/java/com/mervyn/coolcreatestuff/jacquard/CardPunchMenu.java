package com.mervyn.coolcreatestuff.jacquard;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

// The punch's slots plus the player inventory. The design editor is the screen around them.
public class CardPunchMenu extends AbstractContainerMenu {
    public static final int SOURCE_X = 221, SOURCE_Y = 113;
    public static final int BLANKS_X = 221, BLANKS_Y = 133;
    public static final int OUTPUT_X = 265, OUTPUT_Y = 123;
    public static final int INVENTORY_X = 65, INVENTORY_Y = 167;

    public final CardPunchBlockEntity punch;
    private final ContainerLevelAccess access;

    public CardPunchMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (CardPunchBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public CardPunchMenu(int id, Inventory inventory, CardPunchBlockEntity punch) {
        super(Jacquard.CARD_PUNCH_MENU.get(), id);
        this.punch = punch;
        access = ContainerLevelAccess.create(punch.getLevel(), punch.getBlockPos());
        addSlot(new SlotItemHandler(punch.source, 0, SOURCE_X, SOURCE_Y));
        addSlot(new SlotItemHandler(punch.blanks, 0, BLANKS_X, BLANKS_Y));
        addSlot(new SlotItemHandler(punch.output, 0, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 3) {
            if (!moveItemStackTo(stack, 3, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 1, 2, false) && !moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, Jacquard.CARD_PUNCH.get());
    }
}
