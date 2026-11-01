package com.mervyn.createoddsnends.compat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.cnc.Cnc;
import com.mervyn.createoddsnends.cnc.CncJob;
import com.mervyn.createoddsnends.client.AnimatedCnc;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
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
import net.minecraft.world.item.crafting.Ingredient;

public class CncCategory extends AbstractRecipeCategory<CncJob> {
    public static final RecipeType<CncJob> TYPE = RecipeType.create(Tracer.MODID, "cnc", CncJob.class);

    public CncCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("createoddsnends.jei.cnc"), gui.createDrawableItemLike(Cnc.CNC_PENDANT_ITEM.get()), 180, 66);
    }

    // the same ingredient listed twice shows once with a count
    private static List<List<ItemStack>> grouped(List<Ingredient> needs) {
        List<Ingredient> seen = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Ingredient need : needs) {
            if (need.isEmpty())
                continue;
            int at = -1;
            for (int i = 0; i < seen.size() && at < 0; i++)
                if (Arrays.equals(seen.get(i).getItems(), need.getItems(), (a, b) -> ItemStack.isSameItemSameComponents(a, b) ? 0 : 1))
                    at = i;
            if (at < 0) {
                seen.add(need);
                counts.add(1);
            } else
                counts.set(at, counts.get(at) + 1);
        }
        List<List<ItemStack>> out = new ArrayList<>();
        for (int i = 0; i < seen.size(); i++) {
            int count = counts.get(i);
            out.add(Arrays.stream(seen.get(i).getItems()).map(s -> s.copyWithCount(count)).toList());
        }
        return out;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CncJob job, IFocusGroup focuses) {
        List<List<ItemStack>> inputs = grouped(job.needs());
        List<List<ItemStack>> tools = grouped(job.tools());
        for (int i = 0; i < inputs.size() + tools.size() && i < 9; i++) {
            boolean tool = i >= inputs.size();
            var slot = builder.addInputSlot(4 + i % 3 * 19, 5 + i / 3 * 19)
                    .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                    .addItemStacks(tool ? tools.get(i - inputs.size()) : inputs.get(i));
            if (tool)
                slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("createoddsnends.jei.cnc_tool").withStyle(ChatFormatting.GOLD)));
        }
        List<ProcessingOutput> results = job.results();
        for (int i = 0; i < results.size(); i++) {
            ProcessingOutput out = results.get(i);
            builder.addOutputSlot(143 + i % 2 * 19, 15 + i / 2 * 19)
                    .setBackground(CreateRecipeCategory.getRenderedSlot(out), -1, -1)
                    .addItemStack(out.getStack())
                    .addRichTooltipCallback(CreateRecipeCategory.addStochasticTooltip(out));
        }
    }

    @Override
    public void draw(CncJob job, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        AllGuiTextures.JEI_SHADOW.render(graphics, 75, 50);
        ItemStack[] first = job.needs().isEmpty() ? new ItemStack[0] : job.needs().get(0).getItems();
        new AnimatedCnc(first.length == 0 ? ItemStack.EMPTY : first[0]).draw(graphics, 60, 2);
    }
}
