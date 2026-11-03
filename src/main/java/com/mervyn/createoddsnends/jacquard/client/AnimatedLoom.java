package com.mervyn.createoddsnends.jacquard.client;

import static com.mervyn.createoddsnends.jacquard.LoomPartBlock.FACING;
import static com.mervyn.createoddsnends.jacquard.LoomPartBlock.FORMED;

import com.mervyn.createoddsnends.client.GuiScene;
import com.mervyn.createoddsnends.jacquard.Jacquard;
import com.mervyn.createoddsnends.jacquard.JacquardHeadBlockEntity;
import com.mervyn.createoddsnends.jacquard.LoomCell;
import com.mervyn.createoddsnends.jacquard.LoomFrameBlock;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

// The formed loom on a JEI page weaving the sample design, a pick every few ticks so the cloth visibly grows.
public class AnimatedLoom {
    private static final int PICK = 10;
    private static final JacquardHeadBlockEntity.Picks PICKS = JacquardHeadBlockEntity.Picks.of(JacquardHeadBlockEntity.DEBUG_DESIGN);

    public void draw(GuiGraphics graphics, int x, int y) {
        float time = AnimationTickHolder.getRenderTime();
        int pick = (int) (time / PICK) % PICKS.total();
        double t = time % PICK / PICK;
        JacquardHeadBlockEntity.Phase phase = JacquardHeadBlockEntity.phase(PICKS, pick, Math.min(t, .9999), time * .3, false);

        GuiScene.begin(graphics, x + 30, y + 28, 10.5f, 25, -30);
        graphics.pose().translate(-.5, .5, -1);
        BlockState frame = Jacquard.LOOM_FRAME.get().defaultBlockState().setValue(FACING, Direction.SOUTH).setValue(FORMED, true);
        for (LoomCell cell : LoomCell.values())
            GuiScene.block(graphics, frame.setValue(LoomFrameBlock.POSITION, cell), cell.x - 2, cell.y - 2, cell.z);
        GuiScene.block(graphics, Jacquard.JACQUARD_HEAD.get().defaultBlockState().setValue(FACING, Direction.SOUTH).setValue(FORMED, true), 0, 0, 0);
        GuiScene.block(graphics, Jacquard.LOOM_DRIVE.get().defaultBlockState().setValue(FACING, Direction.SOUTH).setValue(FORMED, true), -2, -2, 1);
        LoomRenderer.draw(graphics.pose(), graphics.bufferSource(), Sheets.solidBlockSheet(), true, LightTexture.FULL_BRIGHT, JacquardHeadBlockEntity.DEBUG_DESIGN,
                true, DesignTextures.DEBUG, phase, PICKS);
        GuiScene.end(graphics);
    }
}
