package com.mervyn.coolcreatestuff.organ.client;

import com.mervyn.coolcreatestuff.organ.BarrelOrganBlock;
import com.mervyn.coolcreatestuff.organ.BarrelOrganBlockEntity;
import com.mervyn.coolcreatestuff.organ.Score;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

// The organ's moving parts, ported from buildOrgan(). Partials are authored in the preview's
// coordinates moved to the block (x + 8, z + 8, +z the front), so the pivots below are the
// preview's plus 8 on x and z.
public class BarrelOrganRenderer extends SafeBlockEntityRenderer<BarrelOrganBlockEntity> {
    private static final int MAX_STACK = 32;

    public BarrelOrganRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(BarrelOrganBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(BarrelOrganBlock.HORIZONTAL_FACING);
        VertexConsumer vc = buffer.getBuffer(RenderType.solid());
        ms.pushPose();
        ms.translate(.5, 0, .5);
        ms.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        ms.translate(-.5, 0, -.5);

        for (int i = 0; i < 9; i++) {
            float lit = be.pipeLit[i];
            ms.pushPose();
            ms.translate(0, lit * 0.25f / 16, 0);
            draw(lit > 0.35f ? OrganPartials.PIPES_LIT[i] : OrganPartials.PIPES[i], state, ms, vc, light);
            ms.popPose();
        }

        double since = (be.getLevel().getGameTime() - be.drumHit + partialTicks) / 20.0;
        double swing = since < 0.12 ? -0.55 * (1 - since / 0.12) : 0;
        ms.pushPose();
        pivot(ms, Axis.ZP, (float) (-0.2 + swing), 13.1f, 11.1f, 0);
        draw(OrganPartials.BEATER, state, ms, vc, light);
        ms.popPose();

        float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), facing.getAxis());
        ms.pushPose();
        pivot(ms, Axis.XP, angle, 0, 18, 8);
        draw(OrganPartials.CRANK, state, ms, vc, light);
        ms.popPose();
        ms.pushPose();
        pivot(ms, Axis.ZP, angle * facing.getAxisDirection().getStep(), 8, 8, 0);
        draw(OrganPartials.SHAFT, state, ms, vc, light);
        ms.popPose();

        Score score = be.hasBook() ? be.score() : null;
        if (score != null)
            book(be, score, partialTicks, state, ms, vc, light);
        ms.popPose();
    }

    // the book: shelf, input stack, the page going through the reading head, the stack folding up
    // behind and the page dropping onto it
    private static void book(BarrelOrganBlockEntity be, Score score, float partialTicks, BlockState state, PoseStack ms, VertexConsumer vc, int light) {
        draw(OrganPartials.SHELF, state, ms, vc, light);
        int pages = score.pages();
        int played, remain;
        float f;
        if (be.isFinished()) {
            played = pages;
            remain = 0;
            f = 0;
        } else {
            float pos = be.shownRow(partialTicks) % score.rowCount();
            played = (int) Math.floor(pos / Score.ROWS_PER_PAGE);
            f = (pos % Score.ROWS_PER_PAGE) / Score.ROWS_PER_PAGE;
            remain = pages - played - 1;
        }
        for (int i = 0; i < Math.min(remain, MAX_STACK); i++)
            page(i, -15 + (i % 2 != 0 ? 0.3f : -0.3f), 12 + i * 0.35f, 0, state, ms, vc, light);
        for (int i = 0; i < Math.min(played, MAX_STACK); i++)
            page(i, i % 2 != 0 ? 0.3f : -0.3f, i * 0.35f, -16, state, ms, vc, light);
        if (be.isFinished())
            return;
        float top = Math.min(Math.max(0, remain), MAX_STACK) * 0.35f;
        page(played, Mth.lerp(f, -14, -1), 12 + top, 0, state, ms, vc, light);
        if (f > 0.6f) {
            float ff = (f - 0.6f) / 0.4f;
            page(played + 5, 0, Mth.lerp(ff, 11.5f, Math.min(played, MAX_STACK) * 0.35f), Mth.lerp(ff, -5.5f, -16), state, ms, vc, light);
        }
    }

    private static void page(int i, float x, float y, float z, BlockState state, PoseStack ms, VertexConsumer vc, int light) {
        ms.pushPose();
        ms.translate(x / 16, y / 16, z / 16);
        draw(OrganPartials.PAGES[i % 6], state, ms, vc, light);
        ms.popPose();
    }

    private static void pivot(PoseStack ms, Axis axis, float radians, float x, float y, float z) {
        ms.translate(x / 16, y / 16, z / 16);
        ms.mulPose(axis.rotation(radians));
        ms.translate(-x / 16, -y / 16, -z / 16);
    }

    private static void draw(PartialModel model, BlockState state, PoseStack ms, VertexConsumer vc, int light) {
        CachedBuffers.partial(model, state).light(light).renderInto(ms, vc);
    }
}
