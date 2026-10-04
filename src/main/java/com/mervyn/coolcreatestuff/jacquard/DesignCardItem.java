package com.mervyn.coolcreatestuff.jacquard;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.loading.FMLEnvironment;

// punched_card_chain: a design's rows as cards, carrying only the design id
public class DesignCardItem extends Item {
    public DesignCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        designTooltip(stack, tooltip, true);
    }

    // the design itself only exists on the client once it's been asked for, so size and colours
    // show up a moment after the id
    static void designTooltip(ItemStack stack, List<Component> tooltip, boolean chain) {
        Integer id = stack.get(Jacquard.DESIGN.get());
        if (id == null) {
            tooltip.add(Component.translatable("createcoolstuffs.tooltip.no_design").withStyle(ChatFormatting.GRAY));
            return;
        }
        Design design = FMLEnvironment.dist.isClient() ? DesignSync.client(id) : null;
        if (design != null)
            tooltip.add(Component.translatable("createcoolstuffs.tooltip.size", design.width(), design.height()).withStyle(ChatFormatting.GRAY));
        if (chain) {
            Integer cards = stack.get(Jacquard.CARDS.get());
            if (cards == null && design != null)
                cards = design.pixelHeight();
            if (cards != null)
                tooltip.add(Component.translatable("createcoolstuffs.tooltip.cards", cards).withStyle(ChatFormatting.GRAY));
        } else if (design != null) {
            boolean[] used = new boolean[16];
            for (byte dye : design.pixels())
                used[dye] = true;
            MutableComponent colours = Component.empty();
            for (int c = 0; c < 16; c++)
                if (used[c]) {
                    if (!colours.getSiblings().isEmpty())
                        colours.append(", ");
                    colours.append(Component.translatable("color.minecraft." + DyeColor.byId(c).getName()));
                }
            tooltip.add(Component.translatable("createcoolstuffs.tooltip.colours", colours).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("createcoolstuffs.tooltip.design", id).withStyle(ChatFormatting.DARK_GRAY));
    }
}
