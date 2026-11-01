package com.mervyn.createoddsnends.jacquard.client;

import java.util.List;

import com.mervyn.createoddsnends.Tracer;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class ShowcaseJei implements IModPlugin {
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "showcase");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    static void show(String type) {
        if (runtime == null)
            return;
        runtime.getRecipeManager().getRecipeType(ResourceLocation.fromNamespaceAndPath(Tracer.MODID, type))
                .ifPresent(t -> runtime.getRecipesGui().showTypes(List.<RecipeType<?>>of(t)));
    }
}
