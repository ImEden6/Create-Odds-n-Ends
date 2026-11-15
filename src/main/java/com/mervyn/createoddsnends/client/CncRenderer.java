package com.mervyn.createoddsnends.client;

import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FACING;
import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FORMED;

import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.cnc.CncBlockEntity;
import com.mervyn.createoddsnends.cnc.CncDoors;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class CncRenderer extends SafeBlockEntityRenderer<CncBlockEntity> {
    public static final PartialModel SADDLE = of("saddle");
    public static final PartialModel TABLE = of("table");
    public static final PartialModel HEAD = of("head");
    public static final PartialModel TOWER = of("tower");
    public static final PartialModel GREEN = of("lamp_green");
    public static final PartialModel AMBER = of("lamp_amber");
    public static final PartialModel RED = of("lamp_red");
    public static final PartialModel DRIVE_STUB = of("drive_stub");
    public static final PartialModel DOOR_A = of("door_a");
    public static final PartialModel DOOR_B = of("door_b");

    private static final float PARK_X = 32, PARK_Y = 26, PARK_HEAD = 36;
    static final float PATH_SPEED = .6f, TIP_Z = 31, TABLE_TOP = 18, TIP_DROP = 8;

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
        if (!state.getValue(FORMED))
            return;

        ms.pushPose();
        ms.translate(.5, .5, .5);
        ms.mulPose(Axis.YP.rotationDegrees(-state.getValue(FACING).toYRot() + 180));
        ms.translate(-.5, -1.5, -.5);

        ItemStack work = be.workpiece();
        CncToolpath path = work.isEmpty() ? null : CncToolpath.of(work, be.getLevel());
        float[] tip = path == null ? new float[] {0, 4, 0} : path.at(be.clock(partialTicks) * PATH_SPEED);
        float on = be.engaged(partialTicks);
        float tx = Mth.lerp(on, PARK_X, 32 + tip[0]);
        float sy = Mth.lerp(on, PARK_Y, TIP_Z - tip[2]);
        float hz = Mth.lerp(on, PARK_HEAD, TABLE_TOP + TIP_DROP + tip[1]);

        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
        part(SADDLE, state, ms, vb, light, 40, 0, sy - 8);
        part(TABLE, state, ms, vb, light, 80 - tx - 8, 0, sy - 8);
        part(HEAD, state, ms, vb, light, 40, hz - 8, 8);
        part(TOWER, state, ms, vb, light, 0, 48, 0);
        PartialModel lit = be.isRunning() ? GREEN : be.speed() != 0 ? AMBER : RED;
        for (PartialModel lamp : new PartialModel[] {GREEN, AMBER, RED})
            CachedBuffers.partial(lamp, state)
                    .translate(0, 3, 0)
                    .light(lamp == lit ? LightTexture.FULL_BRIGHT : light)
                    .color(lamp == lit ? 0xFFFFFF : 0x505050)
                    .renderInto(ms, vb);

        float open = CncDoors.OPEN_DEGREES * be.door(partialTicks);
        VertexConsumer glass = buffer.getBuffer(RenderType.translucent());
        door(DOOR_A, state, ms, glass, light, CncDoors.HINGE_A, 56, -open);
        door(DOOR_B, state, ms, glass, light, CncDoors.HINGE_B, 16, open);

        List<ItemStack> tools = be.tools();
        for (int i = 0; i < tools.size() && i < 6; i++) {
            double a = i * Math.PI / 3;
            ms.pushPose();
            ms.translate((80 - 10 - 4.5 * Math.cos(a)) / 16, 25 / 16f, (24 + 4.5 * Math.sin(a)) / 16);
            ms.mulPose(Axis.ZP.rotationDegrees(135));
            ms.scale(.3f, .3f, .3f);
            Minecraft.getInstance().getItemRenderer().renderStatic(tools.get(i), ItemDisplayContext.FIXED, light, overlay, ms, buffer, be.getLevel(), 0);
            ms.popPose();
        }

        if (path != null) {
            ms.pushPose();
            ms.translate((80 - tx) / 16, (TABLE_TOP + (path.flat ? .25f : CncToolpath.SIZE / 2)) / 16, sy / 16);
            ms.scale(.5f, .5f, .5f);
            if (path.flat)
                ms.mulPose(Axis.XP.rotationDegrees(-90));
            Minecraft.getInstance().getItemRenderer().renderStatic(work, ItemDisplayContext.NONE, light, overlay, ms, buffer, be.getLevel(), 0);
            ms.popPose();
        }
        ms.popPose();
    }

    private static void door(PartialModel model, BlockState state, PoseStack ms, VertexConsumer vb, int light, float hinge, float shift, float angle) {
        ms.pushPose();
        ms.translate(hinge / 16, 0, CncDoors.HINGE_Z / 16);
        ms.mulPose(Axis.YP.rotationDegrees(angle));
        ms.translate(-hinge / 16, 0, -CncDoors.HINGE_Z / 16);
        part(model, state, ms, vb, light, shift, 16, 0);
        ms.popPose();
    }

    private static void part(PartialModel model, BlockState state, PoseStack ms, VertexConsumer vb, int light, float x, float y, float z) {
        CachedBuffers.partial(model, state)
                .translate(x / 16, y / 16, z / 16)
                .light(light)
                .renderInto(ms, vb);
    }
}
