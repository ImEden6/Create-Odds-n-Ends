package com.mervyn.createoddsnends.compat;

import java.util.ArrayList;
import java.util.List;

import com.mervyn.createoddsnends.Profile;
import com.mervyn.createoddsnends.Tracer;
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

// JEI only loads this when it's installed, and EMI reads JEI plugins, so nothing else references it
@JeiPlugin
public class TracerJei implements IModPlugin {
    private static final int EXAMPLE_PAGES = 8;
    private static final List<Block> EXAMPLES = List.of(Blocks.OAK_LOG, Blocks.STONE, Blocks.QUARTZ_BLOCK);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new TurningCategory(registration.getJeiHelpers().getGuiHelper()),
                new PunchingCategory(registration.getJeiHelpers().getGuiHelper()),
                new WeavingCategory(registration.getJeiHelpers().getGuiHelper(), wool(JacquardHeadBlockEntity.DEBUG_DESIGN).size()));
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

        ItemStack chain = designed(Jacquard.PUNCHED_CARD_CHAIN.get());
        ItemStack tapestry = designed(Jacquard.TAPESTRY.get());
        ItemStack book = new ItemStack(Organ.BOOK_MUSIC.get());
        Design design = JacquardHeadBlockEntity.DEBUG_DESIGN;
        int rows = design.pixelHeight();
        registration.addRecipes(PunchingCategory.TYPE, List.of(
                new PunchingCategory.Punching(List.of(), new ItemStack(Jacquard.BLANK_CARD.get(), rows), "createoddsnends.jei.blanks_design", chain, "createoddsnends.jei.punch_editor"),
                new PunchingCategory.Punching(List.of(chain, tapestry), new ItemStack(Jacquard.BLANK_CARD.get(), rows), "createoddsnends.jei.blanks_design", chain, "createoddsnends.jei.punch_copy"),
                new PunchingCategory.Punching(List.of(), new ItemStack(Jacquard.BLANK_CARD.get(), EXAMPLE_PAGES), "createoddsnends.jei.blanks_score", book, "createoddsnends.jei.punch_score"),
                new PunchingCategory.Punching(List.of(book), new ItemStack(Jacquard.BLANK_CARD.get(), EXAMPLE_PAGES), "createoddsnends.jei.blanks_score", book, "createoddsnends.jei.punch_copy_score")));

        registration.addRecipes(WeavingCategory.TYPE, List.of(new WeavingCategory.Weaving(chain, wool(design), tapestry)));

        info(registration, Organ.BARREL_ORGAN_ITEM.get(), "barrel_organ");
        info(registration, Organ.WIND_CHEST_ITEM.get(), "wind_chest");
        info(registration, Organ.WIND_LINE_ITEM.get(), "wind_line");
        info(registration, Jacquard.CARD_PUNCH_ITEM.get(), "card_punch");
    }

    private static void info(IRecipeRegistration registration, Item item, String name) {
        registration.addIngredientInfo(item, Component.translatable("createoddsnends.jei.info." + name));
    }

    private static ItemStack designed(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(Jacquard.DESIGN.get(), DesignSync.PONDER);
        return stack;
    }

    // a wool block is 256 pixels of thread, rounded up per colour
    static List<ItemStack> wool(Design design) {
        int[] pixels = new int[16];
        for (int y = 0; y < design.pixelHeight(); y++)
            for (int x = 0; x < design.pixelWidth(); x++)
                pixels[design.dye(x, y)]++;
        List<ItemStack> wool = new ArrayList<>();
        for (int c = 0; c < 16; c++)
            if (pixels[c] > 0)
                wool.add(new ItemStack(JacquardHeadBlockEntity.wool(c), (pixels[c] + JacquardWork.THREAD_PER_WOOL - 1) / JacquardWork.THREAD_PER_WOOL));
        return wool;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(TurningCategory.TYPE, Tracer.LATHE_HEADSTOCK_ITEM.get(), Tracer.LATHE_BED_ITEM.get(), Tracer.LATHE_TAILSTOCK_ITEM.get());
        registration.addRecipeCatalysts(PunchingCategory.TYPE, Jacquard.CARD_PUNCH_ITEM.get());
        registration.addRecipeCatalysts(WeavingCategory.TYPE, Jacquard.JACQUARD_HEAD_ITEM.get(), Jacquard.LOOM_FRAME_ITEM.get(), Jacquard.LOOM_DRIVE_ITEM.get());
    }
}
