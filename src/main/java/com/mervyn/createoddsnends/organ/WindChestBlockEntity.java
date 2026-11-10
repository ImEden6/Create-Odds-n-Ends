package com.mervyn.createoddsnends.organ;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// Looks only for now: the voice filter on the front takes a dye. Nothing reads it yet.
public class WindChestBlockEntity extends SmartBlockEntity {
    FilteringBehaviour voice;

    public WindChestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        voice = new FilteringBehaviour(this, new VoiceSlot()).withPredicate(stack -> stack.getItem() instanceof DyeItem);
        behaviours.add(voice);
    }

    @Nullable
    public DyeColor voiceColour() {
        return voice.getFilter().getItem() instanceof DyeItem dye ? dye.getDyeColor() : null;
    }

    private static class VoiceSlot extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 9, 16.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction == state.getValue(WindChestBlock.FACING);
        }
    }
}
