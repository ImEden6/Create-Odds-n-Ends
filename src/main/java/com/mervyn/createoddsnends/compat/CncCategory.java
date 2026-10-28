package com.mervyn.createoddsnends.compat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.cnc.Cnc;
import com.mervyn.createoddsnends.cnc.CncJob;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class CncCategory extends AbstractRecipeCategory<CncJob> {
    public static final RecipeType<CncJob> TYPE = RecipeType.create(Tracer.MODID, "cnc", CncJob.class);

    public CncCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("createoddsnends.jei.cnc"), gui.createDrawableItemLike(Cnc.CNC_PENDANT_ITEM.get()), 150, 58);
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
        for (int i = 0; i < inputs.size() && i < 9; i++)
            builder.addInputSlot(1 + i % 3 * 18, 2 + i / 3 * 18)
                    .setStandardSlotBackground()
                    .addItemStacks(inputs.get(i));
        List<ProcessingOutput> results = job.results();
        for (int i = 0; i < results.size(); i++) {
            ProcessingOutput out = results.get(i);
            var slot = builder.addOutputSlot(113 + i % 2 * 18, 11 + i / 2 * 18)
                    .setOutputSlotBackground()
                    .addItemStack(out.getStack());
            if (out.getChance() < 1)
                slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("createoddsnends.jei.chance",
                        Math.round(out.getChance() * 100)).withStyle(ChatFormatting.GOLD)));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, CncJob job, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(job.duration()).setPosition(70, 20);
    }
}
