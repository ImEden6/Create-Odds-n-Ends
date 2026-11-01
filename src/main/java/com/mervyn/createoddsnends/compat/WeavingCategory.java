package com.mervyn.createoddsnends.compat;

import java.util.List;

import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.jacquard.Jacquard;
import com.mervyn.createoddsnends.jacquard.client.AnimatedLoom;
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

public class WeavingCategory extends AbstractRecipeCategory<WeavingCategory.Weaving> {
    public static final RecipeType<Weaving> TYPE = RecipeType.create(Tracer.MODID, "weaving", Weaving.class);
    private static final int PER_ROW = 5;

    public record Weaving(ItemStack chain, List<ItemStack> wool, ItemStack tapestry) {}

    public WeavingCategory(IGuiHelper gui, int maxColours) {
        super(TYPE, Component.translatable("createoddsnends.jei.weaving"), gui.createDrawableItemLike(Jacquard.JACQUARD_HEAD_ITEM.get()),
                180, Math.max(62, 26 + 19 * ((maxColours + PER_ROW - 1) / PER_ROW)));
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Weaving recipe, IFocusGroup focuses) {
        builder.addInputSlot(5, 5)
                .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                .addItemStack(recipe.chain())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("createoddsnends.jei.chain_stays").withStyle(ChatFormatting.GRAY)));

        for (int i = 0; i < recipe.wool().size(); i++)
            builder.addInputSlot(5 + 19 * (i % PER_ROW), 26 + 19 * (i / PER_ROW))
                    .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                    .addItemStack(recipe.wool().get(i))
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("createoddsnends.jei.thread").withStyle(ChatFormatting.GRAY)));

        builder.addOutputSlot(158, getHeight() / 2 - 9)
                .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                .addItemStack(recipe.tapestry())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("createoddsnends.jei.tapestry").withStyle(ChatFormatting.GRAY)));
    }

    private final AnimatedLoom loom = new AnimatedLoom();

    @Override
    public void draw(Weaving recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        AllGuiTextures.JEI_SHADOW.render(graphics, 102, getHeight() / 2 + 14);
        loom.draw(graphics, 98, getHeight() / 2 - 27);
    }
}
