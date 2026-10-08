package com.mervyn.createoddsnends.compat;

import java.util.List;

import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.jacquard.Jacquard;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class WeavingCategory extends AbstractRecipeCategory<WeavingCategory.Weaving> {
    public static final RecipeType<Weaving> TYPE = RecipeType.create(Tracer.MODID, "weaving", Weaving.class);
    private static final int PER_ROW = 5;

    public record Weaving(ItemStack chain, List<ItemStack> wool, ItemStack tapestry) {}

    public WeavingCategory(IGuiHelper gui, int maxColours) {
        super(TYPE, Component.translatable("createoddsnends.jei.weaving"), gui.createDrawableItemLike(Jacquard.JACQUARD_HEAD_ITEM.get()),
                150, 20 + 18 * Math.max(1, (maxColours + PER_ROW - 1) / PER_ROW));
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Weaving recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 1)
                .setStandardSlotBackground()
                .addItemStack(recipe.chain())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("createoddsnends.jei.chain_stays").withStyle(ChatFormatting.GRAY)));

        for (int i = 0; i < recipe.wool().size(); i++)
            builder.addInputSlot(1 + 18 * (i % PER_ROW), 21 + 18 * (i / PER_ROW))
                    .setStandardSlotBackground()
                    .addItemStack(recipe.wool().get(i))
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("createoddsnends.jei.thread").withStyle(ChatFormatting.GRAY)));

        builder.addOutputSlot(129, getHeight() / 2 - 9)
                .setOutputSlotBackground()
                .addItemStack(recipe.tapestry())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("createoddsnends.jei.tapestry").withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Weaving recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(120).setPosition(97, getHeight() / 2 - 9);
    }
}
