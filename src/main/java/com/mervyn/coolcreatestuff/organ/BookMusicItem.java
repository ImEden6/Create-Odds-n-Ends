package com.mervyn.coolcreatestuff.organ;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.loading.FMLEnvironment;

public class BookMusicItem extends Item {
    private static final String[] KEYS = {"C", "C♯", "D", "E♭", "E", "F", "F♯", "G", "A♭", "A", "B♭", "B"};

    public BookMusicItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Integer id = stack.get(Organ.SCORE.get());
        if (id == null) {
            tooltip.add(Component.translatable("createcoolstuffs.tooltip.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        Score score = FMLEnvironment.dist.isClient() ? ScoreSync.client(id) : null;
        if (score != null) {
            tooltip.add(Component.translatable("createcoolstuffs.organ.tooltip.length", score.bars(), score.pages()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("createcoolstuffs.organ.tooltip.key", KEYS[score.key()]).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.literal("#" + id).withStyle(ChatFormatting.DARK_GRAY));
    }
}
