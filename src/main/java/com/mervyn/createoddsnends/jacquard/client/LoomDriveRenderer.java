package com.mervyn.createoddsnends.jacquard.client;

import com.mervyn.createoddsnends.jacquard.LoomDriveBlock;
import com.mervyn.createoddsnends.jacquard.LoomDriveBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class LoomDriveRenderer extends SafeBlockEntityRenderer<LoomDriveBlockEntity> {
    public LoomDriveRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(LoomDriveBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        Direction input = LoomDriveBlock.input(state);
        float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), input.getAxis())
                * -input.getAxisDirection().getStep();
        ms.pushPose();
        ms.translate(.5, 0, .5);
        ms.mulPose(Axis.YP.rotationDegrees(-state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING).toYRot()));
        ms.translate(-.5, 0, -.5);
        ms.scale(1 / 16f, 1 / 16f, 1 / 16f);
        ms.translate(8, 0, 8);
        Mesh m = new Mesh(ms, buffer.getBuffer(RenderType.solid()), light, false);
        m.rotX(angle, 8, 0);
        m.shaft(0, -8, -2, -2, -5, 2, 2);
        ms.popPose();
    }
}
