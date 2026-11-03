package com.mervyn.createoddsnends.jacquard.client;

import com.mervyn.createoddsnends.Tracer;

import dev.emi.emi.api.EmiApi;
import net.minecraft.resources.ResourceLocation;

// with JEI left out of the dev run (ODDSNENDS_NOJEI=true) the jei_ shots open EMI's pages instead
final class ShowcaseEmi {
    private ShowcaseEmi() {}

    static void show(String type) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Tracer.MODID, type);
        EmiApi.getRecipeManager().getCategories().stream()
                .filter(category -> category.getId().equals(id))
                .findFirst()
                .ifPresent(EmiApi::displayRecipeCategory);
    }
}
