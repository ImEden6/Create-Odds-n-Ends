package com.mervyn.coolcreatestuff.compat;

import java.util.List;

import com.mervyn.coolcreatestuff.Profile;
import com.mervyn.coolcreatestuff.Tracer;
import com.mervyn.coolcreatestuff.turned.TurnedBlockItem;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

// JEI only loads this when it's installed, and EMI reads JEI plugins, so nothing else references it
@JeiPlugin
public class TracerJei implements IModPlugin {
    private static final List<Block> EXAMPLES = List.of(Blocks.OAK_LOG, Blocks.STONE, Blocks.QUARTZ_BLOCK);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new TurningCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ItemStack template = new ItemStack(Tracer.PROFILE_TEMPLATE.get());
        template.set(Tracer.PROFILE.get(), Profile.BALUSTER);
        ItemStack master = TurnedBlockItem.of(Profile.VASE, Blocks.SPRUCE_PLANKS);
        registration.addRecipes(TurningCategory.TYPE, List.of(
                new TurningCategory.Turning(template, Profile.BALUSTER, EXAMPLES, "createcoolstuffs.jei.template"),
                new TurningCategory.Turning(master, Profile.VASE, EXAMPLES, "createcoolstuffs.jei.master")));

        registration.addIngredientInfo(Tracer.TEMPLATE_TABLE_ITEM.get(), Component.translatable("createcoolstuffs.jei.template_table"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(TurningCategory.TYPE, Tracer.LATHE_HEADSTOCK_ITEM.get(), Tracer.LATHE_BED_ITEM.get(), Tracer.LATHE_TAILSTOCK_ITEM.get());
    }
}
