package com.mervyn.createoddsnends.compat;

import java.util.List;

import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.jacquard.Jacquard;
import com.mervyn.createoddsnends.jacquard.client.AnimatedPunch;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class PunchingCategory extends AbstractRecipeCategory<PunchingCategory.Punching> {
    public static final RecipeType<Punching> TYPE = RecipeType.create(Tracer.MODID, "punching", Punching.class);

    public record Punching(List<ItemStack> copyFrom, ItemStack blanks, String blanksHint, ItemStack output, String hint) {}

    public PunchingCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("createoddsnends.jei.punching"), gui.createDrawableItemLike(Jacquard.CARD_PUNCH_ITEM.get()), 150, 60);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Punching recipe, IFocusGroup focuses) {
        if (!recipe.copyFrom().isEmpty())
            builder.addInputSlot(5, 11)
                    .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                    .addItemStacks(recipe.copyFrom())
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("createoddsnends.jei.copy_slot").withStyle(ChatFormatting.GRAY)));

        builder.addInputSlot(5, 33)
                .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                .addItemStack(recipe.blanks())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(recipe.blanksHint()).withStyle(ChatFormatting.GRAY)));

        builder.addOutputSlot(128, 22)
                .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                .addItemStack(recipe.output())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(recipe.hint()).withStyle(ChatFormatting.GRAY)));
    }

    private final AnimatedPunch punch = new AnimatedPunch();

    @Override
    public void draw(Punching recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        AllGuiTextures.JEI_SHADOW.render(graphics, 40, 46);
        punch.draw(graphics, 40, 4);
    }
}
