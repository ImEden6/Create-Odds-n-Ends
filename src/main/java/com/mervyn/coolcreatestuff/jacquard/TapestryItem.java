package com.mervyn.coolcreatestuff.jacquard;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

// Places like a painting, but floors and ceilings work too. A rug's top points where the placer faces.
public class TapestryItem extends Item {
    public TapestryItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        Direction face = context.getClickedFace();
        BlockPos pos = context.getClickedPos().relative(face);
        Integer id = stack.get(Jacquard.DESIGN.get());
        if (id == null || player != null && !player.mayUseItemAt(pos, face, stack))
            return InteractionResult.FAIL;
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel server))
            return InteractionResult.SUCCESS;
        Design design = Designs.get(server.getServer()).get(id);
        if (design == null)
            return InteractionResult.FAIL;
        Direction top = context.getHorizontalDirection();
        TapestryEntity tapestry = new TapestryEntity(level, pos, face, face == Direction.DOWN ? top.getOpposite() : top, id, design);
        if (!tapestry.survives())
            return InteractionResult.CONSUME;
        tapestry.playPlacementSound();
        level.gameEvent(player, GameEvent.ENTITY_PLACE, tapestry.position());
        level.addFreshEntity(tapestry);
        stack.consume(1, player);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DesignCardItem.designTooltip(stack, tooltip);
    }
}
