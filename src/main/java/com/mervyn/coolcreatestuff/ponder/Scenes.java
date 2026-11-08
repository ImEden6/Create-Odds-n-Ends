package com.mervyn.coolcreatestuff.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.items.ItemStackHandler;

final class Scenes {
    private Scenes() {}

    // Ponder keeps the old block entity when a state changes, along with its cached state
    static void form(CreateSceneBuilder scene, Selection parts, BooleanProperty formed, BlockPos... withBlockEntities) {
        scene.world().modifyBlocks(parts, state -> state.hasProperty(formed) ? state.setValue(formed, true) : state, false);
        for (BlockPos pos : withBlockEntities)
            scene.world().modifyBlockEntity(pos, BlockEntity.class, be -> be.setBlockState(be.getLevel().getBlockState(pos)));
    }

    static <T extends BlockEntity> void put(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, Class<T> type, String slot, ItemStack stack) {
        HolderLookup.Provider registries = scene.world().getHolderLookupProvider();
        ItemStackHandler handler = new ItemStackHandler(1);
        handler.setStackInSlot(0, stack.copy());
        scene.world().modifyBlockEntityNBT(util.select().position(pos), type, nbt -> nbt.put(slot, handler.serializeNBT(registries)));
    }
}
