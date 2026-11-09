package com.mervyn.coolcreatestuff.organ.client;

import com.mervyn.coolcreatestuff.organ.WindChestBlock;
import com.mervyn.coolcreatestuff.organ.WindChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;

// Not a SmartBlockEntityRenderer, that one would draw the filter's dye item on the front.
public class WindChestRenderer extends SafeBlockEntityRenderer<WindChestBlockEntity> {
    public WindChestRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(WindChestBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        DyeColor colour = be.voiceColour();
        if (colour == null)
            return;
        BlockState state = be.getBlockState();
        ms.pushPose();
        ms.translate(.5, 0, .5);
        ms.mulPose(Axis.YP.rotationDegrees(-state.getValue(WindChestBlock.FACING).toYRot()));
        ms.translate(-.5, 0, -.5);
        CachedBuffers.partial(OrganPartials.BADGE, state).color(colour.getTextureDiffuseColor()).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
        ms.popPose();
    }
}
