package com.mervyn.createoddsnends.client;

import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FORMED;
import static com.mervyn.createoddsnends.cnc.CncFrameBlock.PART;

import com.mervyn.createoddsnends.cnc.Cnc;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class AnimatedCnc {
    private final ItemStack work;

    public AnimatedCnc(ItemStack work) {
        this.work = work;
    }

    public void draw(GuiGraphics graphics, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        PoseStack ms = GuiScene.begin(graphics, x + 40, y + 34, 13, 25, -150);
        ms.translate(-2.5, -1.6, -1.5);

        BlockState frame = Cnc.CNC_FRAME.get().defaultBlockState().setValue(FORMED, true);
        for (int i = 1; i <= 4; i++)
            for (int j = 0; j < 3; j++)
                for (int k = 0; k < 3; k++)
                    GuiScene.block(graphics, frame.setValue(PART, (i - 1) + 4 * (j + 3 * k)), i, j, k);
        GuiScene.block(graphics, Cnc.CNC_PENDANT.get().defaultBlockState().setValue(FORMED, true), 0, 1, 0);

        CncToolpath path = work.isEmpty() ? null : CncToolpath.of(work, mc.level);
        float[] tip = path == null ? new float[] {0, 4, 0} : path.at(AnimationTickHolder.getRenderTime() * CncRenderer.PATH_SPEED);
        float tx = 32 + tip[0];
        float sy = CncRenderer.TIP_Z - tip[2];
        float hz = CncRenderer.TABLE_TOP + CncRenderer.TIP_DROP + tip[1];

        GuiScene.draw(graphics, GuiScene.partial(CncRenderer.SADDLE, frame).translate(40 / 16f, 0, (sy - 8) / 16));
        GuiScene.draw(graphics, GuiScene.partial(CncRenderer.TABLE, frame).translate((80 - tx - 8) / 16, 0, (sy - 8) / 16));
        GuiScene.draw(graphics, GuiScene.partial(CncRenderer.HEAD, frame).translate(40 / 16f, (hz - 8) / 16, 8 / 16f));
        GuiScene.draw(graphics, GuiScene.partial(CncRenderer.TOWER, frame).translate(0, 3, 0));
        GuiScene.draw(graphics, GuiScene.partial(CncRenderer.GREEN, frame).translate(0, 3, 0));
        GuiScene.draw(graphics, GuiScene.partial(CncRenderer.AMBER, frame).translate(0, 3, 0).color(0x505050));
        GuiScene.draw(graphics, GuiScene.partial(CncRenderer.RED, frame).translate(0, 3, 0).color(0x505050));
        GuiScene.drawGlass(graphics, GuiScene.partial(CncRenderer.DOOR_A, frame).translate(56 / 16f, 1, 0));
        GuiScene.drawGlass(graphics, GuiScene.partial(CncRenderer.DOOR_B, frame).translate(16 / 16f, 1, 0));

        if (path != null) {
            ms.pushPose();
            ms.translate((80 - tx) / 16, (CncRenderer.TABLE_TOP + (path.flat ? .25f : CncToolpath.SIZE / 2)) / 16, sy / 16);
            ms.scale(.5f, .5f, .5f);
            if (path.flat)
                ms.mulPose(Axis.XP.rotationDegrees(-90));
            mc.getItemRenderer().renderStatic(work, ItemDisplayContext.NONE, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ms,
                    graphics.bufferSource(), mc.level, 0);
            ms.popPose();
        }
        GuiScene.end(graphics);
    }
}
