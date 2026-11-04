package com.mervyn.createoddsnends.cnc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public record CncJob(ResourceLocation id, List<Ingredient> needs, List<Ingredient> tools, List<ProcessingOutput> results, int duration, boolean weighted) {
    static final int DEFAULT_DURATION = 200;

    private record Cached(List<CncJob> jobs, String config) {}

    private static final Map<RecipeManager, Cached> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    public static List<CncJob> all(Level level) {
        String config = CncConfig.on(CncConfig.DEPLOYING) + "/" + CncConfig.on(CncConfig.SEQUENCED_ASSEMBLY) + "/"
                + CncConfig.number(CncConfig.ASSEMBLY_LOOPS) + "/" + CncConfig.number(CncConfig.STEP_TICKS);
        Cached cached = CACHE.get(level.getRecipeManager());
        if (cached == null || !cached.config().equals(config)) {
            cached = new Cached(build(level), config);
            CACHE.put(level.getRecipeManager(), cached);
        }
        return cached.jobs();
    }

    static boolean accepts(Level level, ItemStack stack) {
        for (CncJob job : all(level)) {
            for (Ingredient need : job.needs)
                if (need.test(stack))
                    return true;
            for (Ingredient tool : job.tools)
                if (tool.test(stack))
                    return true;
        }
        return false;
    }

    private static List<CncJob> build(Level level) {
        List<CncJob> jobs = recipes(level);
        if (CncConfig.on(CncConfig.DEPLOYING)) {
            RecipeType<DeployerApplicationRecipe> deploying = AllRecipeTypes.DEPLOYING.getType();
            for (RecipeHolder<DeployerApplicationRecipe> holder : level.getRecipeManager().getAllRecipesFor(deploying))
                deploying(holder).ifPresent(jobs::add);
        }
        if (CncConfig.on(CncConfig.SEQUENCED_ASSEMBLY)) {
            RecipeType<SequencedAssemblyRecipe> assemblies = AllRecipeTypes.SEQUENCED_ASSEMBLY.getType();
            for (RecipeHolder<SequencedAssemblyRecipe> holder : level.getRecipeManager().getAllRecipesFor(assemblies))
                assembly(holder).ifPresent(jobs::add);
        }
        return jobs;
    }

    public static List<CncJob> recipes(Level level) {
        List<CncJob> jobs = new ArrayList<>();
        for (RecipeHolder<CncRecipe> holder : level.getRecipeManager().getAllRecipesFor(Cnc.CNC_TYPE.get())) {
            CncRecipe recipe = holder.value();
            int duration = recipe.getProcessingDuration() > 0 ? recipe.getProcessingDuration() : DEFAULT_DURATION;
            jobs.add(new CncJob(holder.id(), recipe.getIngredients(), List.of(), recipe.getRollableResults(), duration, false));
        }
        return jobs;
    }

    private static Optional<CncJob> deploying(RecipeHolder<DeployerApplicationRecipe> holder) {
        DeployerApplicationRecipe recipe = holder.value();
        Ingredient held = recipe.getRequiredHeldItem();
        boolean keep = recipe.shouldKeepHeldItem();
        List<Ingredient> needs = keep ? List.of(recipe.getProcessedItem()) : List.of(recipe.getProcessedItem(), held);
        return Optional.of(new CncJob(holder.id(), needs, keep ? List.of(held) : List.of(), recipe.getRollableResults(), CncConfig.number(CncConfig.STEP_TICKS), false));
    }

    private static Optional<CncJob> assembly(RecipeHolder<SequencedAssemblyRecipe> holder) {
        SequencedAssemblyRecipe recipe = holder.value();
        List<Ingredient> needs = new ArrayList<>();
        List<Ingredient> tools = new ArrayList<>();
        needs.add(recipe.getIngredient());
        int charged = CncConfig.number(CncConfig.ASSEMBLY_LOOPS);
        int loops = charged == 0 ? recipe.getLoops() : Math.min(charged, recipe.getLoops());
        for (int loop = 0; loop < loops; loop++)
            for (SequencedRecipe<?> step : recipe.getSequence()) {
                if (!step.getRecipe().getFluidIngredients().isEmpty())
                    return Optional.empty();
                List<Ingredient> extra = step.getRecipe().getIngredients();
                extra = extra.subList(Math.min(1, extra.size()), extra.size());
                if (!(step.getRecipe() instanceof ItemApplicationRecipe applied && applied.shouldKeepHeldItem()))
                    needs.addAll(extra);
                else
                    for (Ingredient tool : extra)
                        if (!tools.contains(tool))
                            tools.add(tool);
            }
        int duration = CncConfig.number(CncConfig.STEP_TICKS) * recipe.getSequence().size() * loops;
        return Optional.of(new CncJob(holder.id(), needs, tools, recipe.resultPool, duration, true));
    }

    @Nullable
    static CncJob find(Level level, IItemHandlerModifiable input) {
        CncJob best = null;
        for (CncJob job : all(level))
            if ((best == null || job.size() > best.size()) && job.take(input, true))
                best = job;
        return best;
    }

    private int size() {
        return needs.size() + tools.size();
    }

    boolean take(IItemHandlerModifiable input, boolean simulate) {
        int[] left = new int[input.getSlots()];
        for (int i = 0; i < left.length; i++)
            left[i] = input.getStackInSlot(i).getCount();
        if (!pick(needs, input, left) || !pick(tools, input, left.clone()))
            return false;
        if (!simulate)
            for (int i = 0; i < left.length; i++) {
                ItemStack stack = input.getStackInSlot(i);
                if (stack.getCount() != left[i])
                    input.setStackInSlot(i, stack.copyWithCount(left[i]));
            }
        return true;
    }

    private static boolean pick(List<Ingredient> wanted, IItemHandlerModifiable input, int[] left) {
        for (Ingredient need : wanted) {
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
        return true;
    }

    List<ItemStack> roll(RandomSource random) {
        if (!weighted)
            return results.stream().map(out -> out.rollOutput(random)).filter(stack -> !stack.isEmpty()).toList();
        float total = 0;
        for (ProcessingOutput out : results)
            total += out.getChance();
        float pick = random.nextFloat() * total;
        for (ProcessingOutput out : results) {
            pick -= out.getChance();
            if (pick <= 0)
                return List.of(out.getStack().copy());
        }
        return List.of();
    }

    public List<ProcessingOutput> shown() {
        if (!weighted)
            return results;
        float total = 0;
        for (ProcessingOutput out : results)
            total += out.getChance();
        List<ProcessingOutput> odds = new ArrayList<>();
        for (ProcessingOutput out : results)
            odds.add(new ProcessingOutput(out.getStack(), total <= 0 ? 0 : out.getChance() / total));
        odds.sort((a, b) -> Float.compare(b.getChance(), a.getChance()));
        return odds;
    }

    List<List<ItemStack>> possible() {
        if (!weighted)
            return List.of(results.stream().map(out -> out.getStack().copy()).toList());
        return results.stream().map(out -> List.of(out.getStack().copy())).toList();
    }

    ItemStack showcase() {
        return results.isEmpty() ? ItemStack.EMPTY : results.getFirst().getStack();
    }
}
