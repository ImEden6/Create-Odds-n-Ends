package com.mervyn.tracer.lathe;

import com.mervyn.tracer.Tracer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;

// What the lathe will take: a plain full cube with no block entity, unless a tag says otherwise.
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
        if (state.is(UNTURNABLE) || block instanceof EntityBlock)
            return false;
        return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }
}
