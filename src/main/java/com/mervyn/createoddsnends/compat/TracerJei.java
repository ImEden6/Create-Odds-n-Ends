package com.mervyn.createoddsnends.compat;

import java.util.List;
import java.util.stream.IntStream;

import com.mervyn.createoddsnends.Profile;
import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.jacquard.Jacquard;
import com.mervyn.createoddsnends.jacquard.JacquardHeadBlockEntity;
import com.mervyn.createoddsnends.turned.TurnedBlockItem;

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
        registration.addRecipeCategories(new TurningCategory(registration.getJeiHelpers().getGuiHelper()),
                new PunchingCategory(registration.getJeiHelpers().getGuiHelper()),
                new WeavingCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ItemStack template = new ItemStack(Tracer.PROFILE_TEMPLATE.get());
        template.set(Tracer.PROFILE.get(), Profile.BALUSTER);
        ItemStack master = TurnedBlockItem.of(Profile.VASE, Blocks.SPRUCE_PLANKS);
        registration.addRecipes(TurningCategory.TYPE, List.of(
                new TurningCategory.Turning(template, Profile.BALUSTER, EXAMPLES, "createoddsnends.jei.template"),
                new TurningCategory.Turning(master, Profile.VASE, EXAMPLES, "createoddsnends.jei.master")));

        registration.addIngredientInfo(Tracer.TEMPLATE_TABLE_ITEM.get(), Component.translatable("createoddsnends.jei.template_table"));

        registration.addRecipes(PunchingCategory.TYPE, List.of(
                new PunchingCategory.Punching(List.of(), "createoddsnends.jei.punch_editor"),
                new PunchingCategory.Punching(List.of(new ItemStack(Jacquard.PUNCHED_CARD_CHAIN.get()), new ItemStack(Jacquard.TAPESTRY.get())),
                        "createoddsnends.jei.punch_copy")));
        List<ItemStack> wool = IntStream.range(0, 16).mapToObj(c -> new ItemStack(JacquardHeadBlockEntity.wool(c))).toList();
        registration.addRecipes(WeavingCategory.TYPE, List.of(new WeavingCategory.Weaving(wool)));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(TurningCategory.TYPE, Tracer.LATHE_HEADSTOCK_ITEM.get(), Tracer.LATHE_BED_ITEM.get(), Tracer.LATHE_TAILSTOCK_ITEM.get());
        registration.addRecipeCatalysts(PunchingCategory.TYPE, Jacquard.CARD_PUNCH_ITEM.get());
        registration.addRecipeCatalysts(WeavingCategory.TYPE, Jacquard.JACQUARD_HEAD_ITEM.get(), Jacquard.LOOM_FRAME_ITEM.get(), Jacquard.LOOM_DRIVE_ITEM.get());
    }
}
