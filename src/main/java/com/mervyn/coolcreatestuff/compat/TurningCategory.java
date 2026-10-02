package com.mervyn.coolcreatestuff.compat;

import java.util.List;

import com.mervyn.coolcreatestuff.Profile;
import com.mervyn.coolcreatestuff.Tracer;
import com.mervyn.coolcreatestuff.turned.TurnedBlockItem;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class TurningCategory extends AbstractRecipeCategory<TurningCategory.Turning> {
    public static final RecipeType<Turning> TYPE = RecipeType.create(Tracer.MODID, "turning", Turning.class);

    public record Turning(ItemStack pattern, Profile profile, List<Block> blocks, String hint) {}

    public TurningCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("tracer.jei.turning"), gui.createDrawableItemLike(Tracer.LATHE_HEADSTOCK_ITEM.get()), 112, 40);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Turning recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 2)
                .setStandardSlotBackground()
                .addItemStack(recipe.pattern())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(recipe.hint()).withStyle(ChatFormatting.GRAY)));

        IRecipeSlotBuilder blocks = builder.addInputSlot(1, 22)
                .setStandardSlotBackground()
                .addItemStacks(recipe.blocks().stream().map(ItemStack::new).toList())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("tracer.jei.any_block").withStyle(ChatFormatting.GRAY)));

        IRecipeSlotBuilder turned = builder.addOutputSlot(91, 12)
                .setOutputSlotBackground()
                .addItemStacks(recipe.blocks().stream().map(block -> TurnedBlockItem.of(recipe.profile(), block)).toList());

        builder.createFocusLink(blocks, turned);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Turning recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(60).setPosition(48, 12);
    }
}
