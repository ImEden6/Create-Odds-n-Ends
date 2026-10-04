package com.mervyn.coolcreatestuff.jacquard.client;

import com.mervyn.coolcreatestuff.jacquard.LoomDriveBlock;
import com.mervyn.coolcreatestuff.jacquard.LoomDriveBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

// Create's half shaft on the drive's input face; the shafts inside the loom are the head's job.
public class LoomDriveRenderer extends SafeBlockEntityRenderer<LoomDriveBlockEntity> {
    public LoomDriveRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(LoomDriveBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        KineticBlockEntityRenderer.standardKineticRotationTransform(
                CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, LoomDriveBlock.input(state)), be, light)
                .renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }
}
