package com.mervyn.coolcreatestuff.jacquard;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

// punched_card_chain: a design's rows as cards, carrying only the design id
public class DesignCardItem extends Item {
    public DesignCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        designTooltip(stack, tooltip);
    }

    static void designTooltip(ItemStack stack, List<Component> tooltip) {
        Integer id = stack.get(Jacquard.DESIGN.get());
        tooltip.add((id == null ? Component.translatable("createcoolstuffs.tooltip.no_design")
                : Component.translatable("createcoolstuffs.tooltip.design", id)).withStyle(ChatFormatting.GRAY));
    }
}
