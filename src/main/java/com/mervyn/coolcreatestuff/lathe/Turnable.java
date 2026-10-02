package com.mervyn.coolcreatestuff.lathe;

import com.mervyn.coolcreatestuff.Tracer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import com.mervyn.coolcreatestuff.turned.TurnedBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class Turnable {
    public static final TagKey<Block> TURNABLE = tag("turnable");
    public static final TagKey<Block> UNTURNABLE = tag("unturnable");

    private Turnable() {}

    private static TagKey<Block> tag(String name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Tracer.MODID, name));
    }

    public static boolean allows(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item))
            return false;
        Block block = item.getBlock();
        BlockState state = block.defaultBlockState();
        if (state.is(TURNABLE))
            return true;
        if (block instanceof TurnedBlock || state.is(UNTURNABLE) || !stack.getComponentsPatch().isEmpty())
            return false;
        return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }
}
