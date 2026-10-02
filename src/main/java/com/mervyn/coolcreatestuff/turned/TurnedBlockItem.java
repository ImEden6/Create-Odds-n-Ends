package com.mervyn.coolcreatestuff.turned;

import java.util.List;

import com.mervyn.coolcreatestuff.Profile;
import com.mervyn.coolcreatestuff.Tracer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class TurnedBlockItem extends BlockItem {
    public TurnedBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Block source = stack.getOrDefault(Tracer.SOURCE_BLOCK.get(), Blocks.OAK_PLANKS);
        Profile profile = stack.getOrDefault(Tracer.PROFILE.get(), Profile.FULL);
        tooltip.add(source.getName().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tracer.tooltip.profile", profile.digits()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
