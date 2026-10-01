package com.mervyn.tracer.template;

import com.mervyn.tracer.Profile;
import com.mervyn.tracer.Tracer;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TemplateTableMenu extends AbstractContainerMenu {
    public static final int TEMPLATE_X = 8;
    public static final int TEMPLATE_Y = 76;

    private final Container template = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    private final ContainerLevelAccess access;

    public TemplateTableMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public TemplateTableMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(Tracer.TEMPLATE_TABLE_MENU.get(), id);
        this.access = access;
        addSlot(new Slot(template, 0, TEMPLATE_X, TEMPLATE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Tracer.PROFILE_TEMPLATE.get());
            }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 108 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, 8 + col * 18, 166));
    }

    public ItemStack template() {
        return template.getItem(0);
    }

    // called from SaveProfile once the payload checks out
    void save(Profile profile) {
        ItemStack stack = template();
        if (!stack.is(Tracer.PROFILE_TEMPLATE.get()))
            return;
        stack.set(Tracer.PROFILE.get(), profile);
        template.setChanged();
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
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
        return stillValid(access, player, Tracer.TEMPLATE_TABLE.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, template));
    }
}
