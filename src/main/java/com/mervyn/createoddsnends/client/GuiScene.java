package com.mervyn.createoddsnends.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

// Draws whole machines into a JEI page with the same models and partials the world uses.
// Units are blocks with y up, the same spaces the block entity renderers work in.
final class GuiScene {
    private GuiScene() {}

    static PoseStack begin(GuiGraphics graphics, float x, float y, float scale, float pitch, float yaw) {
        PoseStack ms = graphics.pose();
        ms.pushPose();
        ms.translate(x, y, 300);
        ms.scale(scale, -scale, scale);
        ms.mulPose(Axis.XP.rotationDegrees(pitch));
        ms.mulPose(Axis.YP.rotationDegrees(yaw));
        AnimatedKinetics.DEFAULT_LIGHTING.applyLighting();
        return ms;
    }

    static void end(GuiGraphics graphics) {
        graphics.flush();
        Lighting.setupFor3DItems();
        graphics.pose().popPose();
    }

    static void block(GuiGraphics graphics, BlockState state, float x, float y, float z) {
        PoseStack ms = graphics.pose();
        ms.pushPose();
        ms.translate(x, y, z);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, ms, graphics.bufferSource(), LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        ms.popPose();
    }

    static SuperByteBuffer partial(PartialModel model, BlockState state) {
        return CachedBuffers.partial(model, state).light(LightTexture.FULL_BRIGHT);
    }

    static void draw(GuiGraphics graphics, SuperByteBuffer buffer) {
        buffer.renderInto(graphics.pose(), graphics.bufferSource().getBuffer(Sheets.cutoutBlockSheet()));
    }

    static void drawGlass(GuiGraphics graphics, SuperByteBuffer buffer) {
        buffer.renderInto(graphics.pose(), graphics.bufferSource().getBuffer(Sheets.translucentCullBlockSheet()));
    }
}
