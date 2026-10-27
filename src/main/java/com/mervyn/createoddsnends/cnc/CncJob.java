package com.mervyn.createoddsnends.cnc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

// One thing the mill can make: every item it eats, what comes out, and how long it takes at 32 rpm.
// Built from a cnc recipe as is, or from a sequenced assembly with every step's extra items
// (deployer held items and so on) added up, so the whole loop runs in one go.
public record CncJob(ResourceLocation id, List<Ingredient> needs, List<ProcessingOutput> results, int duration) {
    static final int DEFAULT_DURATION = 200;
    static final int STEP_DURATION = 100;

    static List<CncJob> all(Level level) {
        List<CncJob> jobs = recipes(level);
        RecipeType<SequencedAssemblyRecipe> assemblies = AllRecipeTypes.SEQUENCED_ASSEMBLY.getType();
        for (RecipeHolder<SequencedAssemblyRecipe> holder : level.getRecipeManager().getAllRecipesFor(assemblies))
            assembly(holder).ifPresent(jobs::add);
        return jobs;
    }

    public static List<CncJob> recipes(Level level) {
        List<CncJob> jobs = new ArrayList<>();
        for (RecipeHolder<CncRecipe> holder : level.getRecipeManager().getAllRecipesFor(Cnc.CNC_TYPE.get())) {
            CncRecipe recipe = holder.value();
            int duration = recipe.getProcessingDuration() > 0 ? recipe.getProcessingDuration() : DEFAULT_DURATION;
            jobs.add(new CncJob(holder.id(), recipe.getIngredients(), recipe.getRollableResults(), duration));
        }
        return jobs;
    }

    private static Optional<CncJob> assembly(RecipeHolder<SequencedAssemblyRecipe> holder) {
        SequencedAssemblyRecipe recipe = holder.value();
        List<Ingredient> needs = new ArrayList<>();
        needs.add(recipe.getIngredient());
        for (int loop = 0; loop < recipe.getLoops(); loop++)
            for (SequencedRecipe<?> step : recipe.getSequence()) {
                if (!step.getRecipe().getFluidIngredients().isEmpty())
                    return Optional.empty();
                List<Ingredient> extra = step.getRecipe().getIngredients();
                needs.addAll(extra.subList(Math.min(1, extra.size()), extra.size()));
            }
        int duration = STEP_DURATION * recipe.getSequence().size() * recipe.getLoops();
        return Optional.of(new CncJob(holder.id(), needs, recipe.resultPool, duration));
    }

    @Nullable
    static CncJob find(Level level, IItemHandlerModifiable input) {
        for (CncJob job : all(level))
            if (job.take(input, true))
                return job;
        return null;
    }

    // Each ingredient takes one item from the first slot that fits it
    boolean take(IItemHandlerModifiable input, boolean simulate) {
        int[] left = new int[input.getSlots()];
        for (int i = 0; i < left.length; i++)
            left[i] = input.getStackInSlot(i).getCount();
        for (Ingredient need : needs) {
            if (need.isEmpty())
                continue;
            boolean found = false;
            for (int i = 0; i < left.length && !found; i++)
                if (left[i] > 0 && need.test(input.getStackInSlot(i))) {
                    left[i]--;
                    found = true;
                }
            if (!found)
                return false;
        }
        if (!simulate)
            for (int i = 0; i < left.length; i++) {
                ItemStack stack = input.getStackInSlot(i);
                if (stack.getCount() != left[i])
                    input.setStackInSlot(i, stack.copyWithCount(left[i]));
            }
        return true;
    }

    ItemStack showcase() {
        return results.isEmpty() ? ItemStack.EMPTY : results.getFirst().getStack();
    }
}
