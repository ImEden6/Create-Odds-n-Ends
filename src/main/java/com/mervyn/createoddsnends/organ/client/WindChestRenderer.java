package com.mervyn.createoddsnends.organ.client;

import com.mervyn.createoddsnends.organ.WindChestBlock;
import com.mervyn.createoddsnends.organ.WindChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlockEntity;
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
        WhistleBlockEntity whistle = be.isBlowing() ? be.whistle() : null;
        if (whistle != null)
            bore(be, whistle, ms, buffer, light);
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

    // Create's whistle has a solid cap, so a blowing one gets a dark opening laid over its top rim
    private static void bore(WindChestBlockEntity be, WhistleBlockEntity whistle, PoseStack ms, MultiBufferSource buffer, int light) {
        BlockState state = whistle.getBlockState();
        ms.pushPose();
        ms.translate(0, be.whistleTop(whistle) - be.getBlockPos().getY(), 0);
        CachedBuffers.partial(OrganPartials.BORES[state.getValue(WhistleBlock.SIZE).ordinal()], state).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
        ms.popPose();
    }
}
