package com.mervyn.createoddsnends.compat;

import java.util.List;

import com.mervyn.createoddsnends.Profile;
import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.client.AnimatedLathe;
import com.mervyn.createoddsnends.turned.TurnedBlockItem;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class TurningCategory extends AbstractRecipeCategory<TurningCategory.Turning> {
    public static final RecipeType<Turning> TYPE = RecipeType.create(Tracer.MODID, "turning", Turning.class);

    public record Turning(ItemStack pattern, Profile profile, List<Block> blocks, String hint) {}

    public TurningCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("createoddsnends.jei.turning"), gui.createDrawableItemLike(Tracer.LATHE_HEADSTOCK_ITEM.get()), 150, 62);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Turning recipe, IFocusGroup focuses) {
        builder.addInputSlot(5, 12)
                .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                .addItemStack(recipe.pattern())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(recipe.hint()).withStyle(ChatFormatting.GRAY)));

        IRecipeSlotBuilder blocks = builder.addInputSlot(5, 34)
                .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                .addItemStacks(recipe.blocks().stream().map(ItemStack::new).toList())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("createoddsnends.jei.any_block").withStyle(ChatFormatting.GRAY)));

        IRecipeSlotBuilder turned = builder.addOutputSlot(128, 23)
                .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                .addItemStacks(recipe.blocks().stream().map(block -> TurnedBlockItem.of(recipe.profile(), block)).toList());

        builder.createFocusLink(blocks, turned);
    }

    @Override
    public void draw(Turning recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        AllGuiTextures.JEI_SHADOW.render(graphics, 45, 48);
        new AnimatedLathe(recipe.profile(), recipe.blocks()).draw(graphics, 40, 4);
    }
}
