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

    public record Weaving(List<ItemStack> wool) {}

    public WeavingCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("createoddsnends.jei.weaving"), gui.createDrawableItemLike(Jacquard.JACQUARD_HEAD_ITEM.get()), 112, 40);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Weaving recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 2)
                .setStandardSlotBackground()
                .addItemStack(new ItemStack(Jacquard.PUNCHED_CARD_CHAIN.get()))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("createoddsnends.jei.chain_stays").withStyle(ChatFormatting.GRAY)));

        builder.addInputSlot(1, 22)
                .setStandardSlotBackground()
                .addItemStacks(recipe.wool())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("createoddsnends.jei.thread").withStyle(ChatFormatting.GRAY)));

        builder.addOutputSlot(91, 12)
                .setOutputSlotBackground()
                .addItemStack(new ItemStack(Jacquard.TAPESTRY.get()))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("createoddsnends.jei.tapestry").withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Weaving recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(120).setPosition(48, 12);
    }
}
