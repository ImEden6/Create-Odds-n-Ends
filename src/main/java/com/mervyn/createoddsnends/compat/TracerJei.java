package com.mervyn.createoddsnends.compat;

import java.util.ArrayList;
import java.util.List;

import com.mervyn.createoddsnends.Profile;
import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.cnc.Cnc;
import com.mervyn.createoddsnends.cnc.CncJob;
import net.minecraft.client.Minecraft;
import com.mervyn.createoddsnends.jacquard.Design;
import com.mervyn.createoddsnends.jacquard.DesignSync;
import com.mervyn.createoddsnends.jacquard.Jacquard;
import com.mervyn.createoddsnends.jacquard.JacquardHeadBlockEntity;
import com.mervyn.createoddsnends.jacquard.JacquardWork;
import com.mervyn.createoddsnends.organ.Organ;
import com.mervyn.createoddsnends.turned.TurnedBlockItem;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

// JEI only loads this when it's installed. EMI has its own plugin (TracerEmi) and skips this one.
@JeiPlugin
public class TracerJei implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new TurningCategory(registration.getJeiHelpers().getGuiHelper()),
                new PunchingCategory(registration.getJeiHelpers().getGuiHelper()),
                new WeavingCategory(registration.getJeiHelpers().getGuiHelper(), Examples.wool(Examples.DESIGN).size()),
                new CncCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(TurningCategory.TYPE, List.of(
                new TurningCategory.Turning(Examples.template(), Profile.BALUSTER, Examples.BLOCKS, "createoddsnends.jei.template"),
                new TurningCategory.Turning(Examples.master(), Profile.VASE, Examples.BLOCKS, "createoddsnends.jei.master")));

        registration.addIngredientInfo(Tracer.TEMPLATE_TABLE_ITEM.get(), Component.translatable("createoddsnends.jei.template_table"));

        ItemStack chain = Examples.chain();
        ItemStack tapestry = Examples.tapestry();
        ItemStack book = Examples.book();
        int rows = Examples.DESIGN.pixelHeight();
        registration.addRecipes(PunchingCategory.TYPE, List.of(
                new PunchingCategory.Punching(List.of(), Examples.blanks(rows), "createoddsnends.jei.blanks_design", chain, "createoddsnends.jei.punch_editor"),
                new PunchingCategory.Punching(List.of(chain, tapestry), Examples.blanks(rows), "createoddsnends.jei.blanks_design", chain, "createoddsnends.jei.punch_copy"),
                new PunchingCategory.Punching(List.of(), Examples.blanks(Examples.SCORE_PAGES), "createoddsnends.jei.blanks_score", book, "createoddsnends.jei.punch_score"),
                new PunchingCategory.Punching(List.of(book), Examples.blanks(Examples.SCORE_PAGES), "createoddsnends.jei.blanks_score", book, "createoddsnends.jei.punch_copy_score")));

        registration.addRecipes(CncCategory.TYPE, CncJob.recipes(Minecraft.getInstance().level));
        info(registration, Cnc.CNC_FRAME_ITEM.get(), "cnc");

        registration.addRecipes(WeavingCategory.TYPE, List.of(new WeavingCategory.Weaving(chain, Examples.wool(Examples.DESIGN), tapestry)));

        info(registration, Organ.BARREL_ORGAN_ITEM.get(), "barrel_organ");
        info(registration, Organ.WIND_CHEST_ITEM.get(), "wind_chest");
        info(registration, Organ.WIND_LINE_ITEM.get(), "wind_line");
        info(registration, Jacquard.CARD_PUNCH_ITEM.get(), "card_punch");
    }

    private static void info(IRecipeRegistration registration, Item item, String name) {
        registration.addIngredientInfo(item, Component.translatable("createoddsnends.jei.info." + name));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(TurningCategory.TYPE, Tracer.LATHE_HEADSTOCK_ITEM.get(), Tracer.LATHE_BED_ITEM.get(), Tracer.LATHE_TAILSTOCK_ITEM.get());
        registration.addRecipeCatalysts(PunchingCategory.TYPE, Jacquard.CARD_PUNCH_ITEM.get());
        registration.addRecipeCatalysts(CncCategory.TYPE, Cnc.CNC_PENDANT_ITEM.get(), Cnc.CNC_FRAME_ITEM.get(), Cnc.CNC_DRIVE_ITEM.get());
        for (String borrowed : List.of("sequenced_assembly", "deploying"))
            registration.getJeiHelpers().getRecipeType(ResourceLocation.fromNamespaceAndPath("create", borrowed))
                    .ifPresent(type -> registration.addRecipeCatalysts(type, Cnc.CNC_PENDANT_ITEM.get()));
        registration.addRecipeCatalysts(WeavingCategory.TYPE, Jacquard.JACQUARD_HEAD_ITEM.get(), Jacquard.LOOM_FRAME_ITEM.get(), Jacquard.LOOM_DRIVE_ITEM.get());
    }
}
