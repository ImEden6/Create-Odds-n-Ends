package com.mervyn.createoddsnends.jacquard.client;

import com.mervyn.createoddsnends.client.GuiScene;
import com.mervyn.createoddsnends.jacquard.Jacquard;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;

import mezz.jei.api.gui.drawable.IDrawable;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.core.Direction;

// The card punch on a JEI page feeding blanks through one after another until the chain is eight long, then starting over.
public class AnimatedPunch implements IDrawable {
    private static final int CARD = 40, CHAIN = 8;

    @Override
    public int getWidth() {
        return 50;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public void draw(GuiGraphics graphics, int x, int y) {
        float time = AnimationTickHolder.getRenderTime();
        int n = (int) (time / CARD) % (CHAIN + 1);
        double t = time % CARD / CARD;
        boolean punching = n < CHAIN;

        GuiScene.begin(graphics, x + 25, y + 32, 22, 25, -35);
        graphics.pose().translate(-.5, -.3, -.5);
        GuiScene.block(graphics, Jacquard.CARD_PUNCH.get().defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.SOUTH), 0, 0, 0);
        CardPunchRenderer.draw(graphics.pose(), graphics.bufferSource().getBuffer(Sheets.solidBlockSheet()), true, LightTexture.FULL_BRIGHT,
                time * .2f, punching ? t : 0, n, CHAIN + 2 - n, punching);
        GuiScene.end(graphics);
    }
}
