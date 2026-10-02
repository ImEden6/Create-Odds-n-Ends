package com.mervyn.coolcreatestuff.template;

import java.util.List;

import com.mervyn.coolcreatestuff.Profile;
import com.mervyn.coolcreatestuff.Tracer;
import com.mervyn.coolcreatestuff.turned.TurnedBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ProfileTemplateItem extends Item {
    public ProfileTemplateItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null || !player.isSecondaryUseActive()
                || !(level.getBlockEntity(context.getClickedPos()) instanceof TurnedBlockEntity be))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            context.getItemInHand().set(Tracer.PROFILE.get(), be.getProfile());
            level.playSound(null, context.getClickedPos(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1, 1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Profile profile = stack.get(Tracer.PROFILE.get());
        if (profile == null)
            tooltip.add(Component.translatable("tracer.tooltip.blank").withStyle(ChatFormatting.GRAY));
        else
            tooltip.add(Component.translatable("tracer.tooltip.profile", profile.digits()).withStyle(ChatFormatting.GRAY));
    }
}
