package com.mervyn.createoddsnends.organ.client;

import com.mervyn.createoddsnends.organ.InstrumentBlock;
import com.mervyn.createoddsnends.organ.InstrumentBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

// The moving part of each instrument. Pivots are in model pixels, the same coords organ.py writes.
public class InstrumentRenderer extends SafeBlockEntityRenderer<InstrumentBlockEntity> {
    public InstrumentRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(InstrumentBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
        float hit = Mth.lerp(partialTicks, be.lastHit, be.hit), open = Mth.lerp(partialTicks, be.lastOpen, be.open);
        ms.pushPose();
        ms.translate(.5, 0, .5);
        ms.mulPose(Axis.YP.rotationDegrees(-state.getValue(InstrumentBlock.FACING).toYRot()));
        ms.translate(-.5, 0, -.5);
        switch (be.instrument()) {
            case KICK_DRUM -> {
                ms.translate(0, 1 / 16f, 14 / 16f);
                ms.mulPose(Axis.XP.rotation(0.4f * (1 - hit)));
                ms.translate(0, -1 / 16f, -14 / 16f);
                draw(OrganPartials.KICK_BEATER, state, ms, vc, light);
            }
            case SNARE_DRUM -> {
                ms.translate(14.2f / 16, 1.5f / 16, 0);
                ms.mulPose(Axis.ZP.rotation(-0.25f * (1 - hit)));
                ms.translate(-14.2f / 16, -1.5f / 16, 0);
                draw(OrganPartials.SNARE_STICKS, state, ms, vc, light);
            }
            case REED_PIPE -> {
                boolean lit = be.held > 0;
                ms.translate(0, 2 / 16f, 0);
                ms.scale(1, 1 - 0.05f * hit, 1);
                ms.translate(0, -2 / 16f, 0);
                draw(lit ? OrganPartials.REED_BODY_LIT : OrganPartials.REED_BODY, state, ms, vc, light);
                float flare = 0.3f * hit + 0.08f * open;
                ms.translate(.5, 14 / 16f, .5);
                ms.scale(1 + flare, 1 - 0.4f * flare, 1 + flare);
                ms.translate(-.5, -14 / 16f, -.5);
                draw(lit ? OrganPartials.REED_BELL_LIT : OrganPartials.REED_BELL, state, ms, vc, light);
            }
            case WOOD_PIPE -> {
                float time = be.getLevel().getGameTime() + partialTicks;
                ms.pushPose();
                ms.translate(0, (2.5f * open + 1.5f * hit + 0.3f * open * Mth.sin(time * 2.7f)) / 16, 0);
                draw(OrganPartials.WOOD_STOPPER, state, ms, vc, light);
                ms.popPose();
                ms.translate(0, 3 / 16f, 11 / 16f);
                ms.mulPose(Axis.XP.rotation(open * (0.35f + 0.15f * Mth.sin(time * 4.1f))));
                ms.translate(0, -3 / 16f, -11 / 16f);
                draw(OrganPartials.WOOD_LIP, state, ms, vc, light);
            }
            case REED_BOX -> {
                float squeeze = be.held > 0 ? 0.15f + 0.1f * hit : 0.2f * hit;
                ms.translate(0, 4 / 16f, 0);
                ms.scale(1, 1 - squeeze, 1);
                ms.translate(0, -4 / 16f, 0);
                draw(OrganPartials.BELLOWS, state, ms, vc, light);
            }
        }
        ms.popPose();
    }

    private static void draw(PartialModel model, BlockState state, PoseStack ms, VertexConsumer vc, int light) {
        CachedBuffers.partial(model, state).light(light).renderInto(ms, vc);
    }
}
