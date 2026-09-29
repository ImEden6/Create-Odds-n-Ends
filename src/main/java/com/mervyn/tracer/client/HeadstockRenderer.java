package com.mervyn.tracer.client;

import static com.mervyn.tracer.lathe.LathePartBlock.FORMED;
import static com.simibubi.create.content.kinetics.base.HorizontalKineticBlock.HORIZONTAL_FACING;

import java.util.List;

import com.mervyn.tracer.lathe.HeadstockBlockEntity;
import com.mervyn.tracer.lathe.LatheStructure;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// Only used when Flywheel can't run; LatheVisual draws the same thing otherwise.
public class HeadstockRenderer extends SafeBlockEntityRenderer<HeadstockBlockEntity> {

    public HeadstockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public boolean shouldRenderOffScreen(HeadstockBlockEntity be) {
        return true;
    }

    @Override
    protected void renderSafe(HeadstockBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Level level = be.getLevel();
        if (VisualizationManager.supportsVisualization(level))
            return;

        BlockState state = be.getBlockState();
        Direction facing = state.getValue(HORIZONTAL_FACING);
        KineticBlockEntityRenderer.standardKineticRotationTransform(
                CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, facing.getOpposite()), be, light)
                .renderInto(ms, buffer.getBuffer(RenderType.solid()));

        if (!state.getValue(FORMED))
            return;

        BlockPos pos = be.getBlockPos();
        BlockPos slot = pos.relative(facing);
        BlockPos master = slot.relative(LatheStructure.back(facing), 2);
        int slotLight = LevelRenderer.getLightColor(level, slot);
        int masterLight = LevelRenderer.getLightColor(level, master);
        BlockState source = be.getSource().defaultBlockState();
        List<BakedQuad> shape = TurnedQuads.all(be.getProfile(), source);
        float time = AnimationTickHolder.getRenderTime(level);
        // model +z is the negative world axis for north and east facings
        float spin = Mth.DEG_TO_RAD * LatheLayout.spinDegrees(be.getSpeed(), time)
                * facing.getOpposite().getAxisDirection().getStep();

        ms.pushPose();
        LatheLayout.toModelSpace(ms, facing);

        ms.pushPose();
        spinAroundAxis(ms, spin);
        partial(LathePartials.CHUCK, state, ms, buffer, light);
        ms.popPose();

        ms.pushPose();
        ms.translate(0, 0, -2);
        spinAroundAxis(ms, spin);
        partial(LathePartials.LIVE_CENTER, state, ms, buffer, LevelRenderer.getLightColor(level, pos.relative(facing, 2)));
        ms.popPose();

        ms.pushPose();
        ms.translate(0, 0, -1);
        spinAroundAxis(ms, spin);
        LatheLayout.layOnAxis(ms);
        quads(shape, source, level, slot, ms, buffer, slotLight);
        ms.popPose();

        LatheLayout.Carriage carriage = LatheLayout.preview(be.getProfile(), time);

        ms.pushPose();
        ms.translate(0, 0, -1 + carriage.along());
        partial(LathePartials.SADDLE, state, ms, buffer, slotLight);
        ms.translate(carriage.reach(), 0, 0);
        partial(LathePartials.CROSS_SLIDE, state, ms, buffer, slotLight);
        partial(LathePartials.TOOL_POST, state, ms, buffer, slotLight);
        partial(LathePartials.CUTTER, state, ms, buffer, slotLight);
        if (be.hasTracer())
            partial(LathePartials.STYLUS, state, ms, buffer, slotLight);
        ms.popPose();

        if (be.hasTracer()) {
            ms.pushPose();
            ms.translate(-2, 0, -1);
            LatheLayout.layOnAxis(ms);
            quads(shape, source, level, master, ms, buffer, masterLight);
            ms.popPose();
        }

        ms.popPose();
    }

    private static void spinAroundAxis(PoseStack ms, float radians) {
        ms.translate(.5, .5, 0);
        ms.mulPose(Axis.ZP.rotation(radians));
        ms.translate(-.5, -.5, 0);
    }

    private static void partial(PartialModel model, BlockState state, PoseStack ms, MultiBufferSource buffer, int light) {
        CachedBuffers.partial(model, state).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    private static void quads(List<BakedQuad> quads, BlockState source, Level level, BlockPos at, PoseStack ms, MultiBufferSource buffer, int light) {
        PoseStack.Pose pose = ms.last();
        VertexConsumer vc = buffer.getBuffer(Sheets.cutoutBlockSheet());
        for (BakedQuad quad : quads) {
            float r = 1, g = 1, b = 1;
            if (quad.isTinted()) {
                int color = Minecraft.getInstance().getBlockColors().getColor(source, level, at, quad.getTintIndex());
                r = (color >> 16 & 255) / 255f;
                g = (color >> 8 & 255) / 255f;
                b = (color & 255) / 255f;
            }
            vc.putBulkData(pose, quad, r, g, b, 1, light, OverlayTexture.NO_OVERLAY);
        }
    }
}
