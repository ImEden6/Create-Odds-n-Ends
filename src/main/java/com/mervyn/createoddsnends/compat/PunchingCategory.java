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

public class PunchingCategory extends AbstractRecipeCategory<PunchingCategory.Punching> {
    public static final RecipeType<Punching> TYPE = RecipeType.create(Tracer.MODID, "punching", Punching.class);

    public record Punching(List<ItemStack> copyFrom, ItemStack blanks, String blanksHint, ItemStack output, String hint) {}

    public PunchingCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("createoddsnends.jei.punching"), gui.createDrawableItemLike(Jacquard.CARD_PUNCH_ITEM.get()), 112, 40);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Punching recipe, IFocusGroup focuses) {
        if (!recipe.copyFrom().isEmpty())
            builder.addInputSlot(1, 2)
                    .setStandardSlotBackground()
                    .addItemStacks(recipe.copyFrom())
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("createoddsnends.jei.copy_slot").withStyle(ChatFormatting.GRAY)));

        builder.addInputSlot(1, 22)
                .setStandardSlotBackground()
                .addItemStack(recipe.blanks())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(recipe.blanksHint()).withStyle(ChatFormatting.GRAY)));

        builder.addOutputSlot(91, 12)
                .setOutputSlotBackground()
                .addItemStack(recipe.output())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(recipe.hint()).withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Punching recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(80).setPosition(48, 12);
    }
}
