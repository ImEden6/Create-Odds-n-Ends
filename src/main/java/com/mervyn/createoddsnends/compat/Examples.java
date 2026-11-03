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

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

// What the recipe viewers show for the machines that have no recipe files, shared by the JEI and EMI plugins
final class Examples {
    static final int SCORE_PAGES = 8;
    static final List<Block> BLOCKS = List.of(Blocks.OAK_LOG, Blocks.STONE, Blocks.QUARTZ_BLOCK);
    static final Design DESIGN = JacquardHeadBlockEntity.DEBUG_DESIGN;

    private Examples() {}

    static ItemStack template() {
        ItemStack template = new ItemStack(Tracer.PROFILE_TEMPLATE.get());
        template.set(Tracer.PROFILE.get(), Profile.BALUSTER);
        return template;
    }

    static ItemStack master() {
        return TurnedBlockItem.of(Profile.VASE, Blocks.SPRUCE_PLANKS);
    }

    static ItemStack chain() {
        return designed(Jacquard.PUNCHED_CARD_CHAIN.get());
    }

    static ItemStack tapestry() {
        return designed(Jacquard.TAPESTRY.get());
    }

    static ItemStack book() {
        return new ItemStack(Organ.BOOK_MUSIC.get());
    }

    static ItemStack blanks(int count) {
        return new ItemStack(Jacquard.BLANK_CARD.get(), count);
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
}
