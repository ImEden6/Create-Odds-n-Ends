package com.mervyn.createoddsnends.cnc;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

public class CncRecipe extends StandardProcessingRecipe<RecipeWrapper> {
    public CncRecipe(ProcessingRecipeParams params) {
        super(Cnc.TYPE_INFO, params);
    }

    @Override
    protected int getMaxInputCount() {
        return 9;
    }

    @Override
    protected int getMaxOutputCount() {
        return 4;
    }

    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }

    @Override
    public boolean matches(RecipeWrapper input, Level level) {
        return false;
    }
}
