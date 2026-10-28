package com.mervyn.createoddsnends.client;

import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FACING;
import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FORMED;

import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.cnc.CncBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

// Draws the parts that move, in the same mirrored machine space tools/cnc.py builds the shell in:
// px, origin at the bottom front corner of the pendant column, one block below the pendant.
public class CncRenderer extends SafeBlockEntityRenderer<CncBlockEntity> {
    public static final PartialModel SADDLE = of("saddle");
    public static final PartialModel TABLE = of("table");
    public static final PartialModel HEAD = of("head");
    public static final PartialModel TOWER = of("tower");
    public static final PartialModel GREEN = of("lamp_green");
    public static final PartialModel AMBER = of("lamp_amber");
    public static final PartialModel RED = of("lamp_red");

    private static final float PARK_X = 32, PARK_Y = 26, PARK_HEAD = 36;
    private static final float PATH_TICKS = 240;

    public CncRenderer(BlockEntityRendererProvider.Context context) {}

    private static PartialModel of(String name) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "block/cnc/" + name));
    }

    public static void init() {}

    @Override
    public boolean shouldRenderOffScreen(CncBlockEntity be) {
        return true;
    }

    @Override
    protected void renderSafe(CncBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        KineticBlockEntityRenderer.standardKineticRotationTransform(
                CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, Direction.DOWN), be, light)
                .renderInto(ms, buffer.getBuffer(RenderType.solid()));
        if (!state.getValue(FORMED))
            return;

        ms.pushPose();
        ms.translate(.5, .5, .5);
        ms.mulPose(Axis.YP.rotationDegrees(-state.getValue(FACING).toYRot() + 180));
        ms.translate(-.5, -1.5, -.5);

        // a made up toolpath: the table sweeps side to side while the saddle steps in and out
        // and the head pecks down, eased in from the parked spot when a job starts
        float t = be.clock(partialTicks) / PATH_TICKS * Mth.TWO_PI;
        float on = be.engaged(partialTicks);
        float tx = Mth.lerp(on, PARK_X, 32 + 10 * Mth.sin(t * 3));
        float sy = Mth.lerp(on, PARK_Y, 27 + 3 * Mth.sin(t * 2 + 1));
        float hz = Mth.lerp(on, PARK_HEAD, 34 + 1.5f * (1 + Mth.cos(t * 7)));

        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
        part(SADDLE, state, ms, vb, light, 40, 0, sy - 8);
        part(TABLE, state, ms, vb, light, 80 - tx - 8, 0, sy - 8);
        part(HEAD, state, ms, vb, light, 40, hz - 8, 8);
        part(TOWER, state, ms, vb, light, 0, 48, 0);
        PartialModel lit = be.isRunning() ? GREEN : be.getSpeed() != 0 ? AMBER : RED;
        for (PartialModel lamp : new PartialModel[] {GREEN, AMBER, RED})
            CachedBuffers.partial(lamp, state)
                    .translate(0, 3, 0)
                    .light(lamp == lit ? LightTexture.FULL_BRIGHT : light)
                    .color(lamp == lit ? 0xFFFFFF : 0x505050)
                    .renderInto(ms, vb);

        ItemStack work = be.workpiece();
        if (!work.isEmpty()) {
            ms.pushPose();
            ms.translate((80 - tx) / 16, 22 / 16f, sy / 16);
            ms.scale(.5f, .5f, .5f);
            Minecraft.getInstance().getItemRenderer().renderStatic(work, ItemDisplayContext.FIXED, light, overlay, ms, buffer, be.getLevel(), 0);
            ms.popPose();
        }
        ms.popPose();
    }

    private static void part(PartialModel model, BlockState state, PoseStack ms, VertexConsumer vb, int light, float x, float y, float z) {
        CachedBuffers.partial(model, state)
                .translate(x / 16, y / 16, z / 16)
                .light(light)
                .renderInto(ms, vb);
    }
}
