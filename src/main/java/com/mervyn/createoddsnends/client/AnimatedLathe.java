package com.mervyn.createoddsnends.client;

import static com.mervyn.createoddsnends.lathe.LathePartBlock.FORMED;
import static com.simibubi.create.content.kinetics.base.HorizontalKineticBlock.HORIZONTAL_FACING;

import java.util.List;

import com.mervyn.createoddsnends.Profile;
import com.mervyn.createoddsnends.Tracer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

// The formed lathe on a JEI page turning the examples down to the recipe's profile, one block after another.
public class AnimatedLathe {
    private static final float SPEED = 32;
    private static final int CYCLE = 140;

    private final Profile profile;
    private final List<Block> blocks;

    public AnimatedLathe(Profile profile, List<Block> blocks) {
        this.profile = profile;
        this.blocks = blocks;
    }

    public void draw(GuiGraphics graphics, int x, int y) {
        float time = AnimationTickHolder.getRenderTime();
        int round = (int) (time / CYCLE);
        float into = time % CYCLE;
        // a short pause on the finished piece, then the cut from the tailstock end back to the chuck
        float layerPos = Profile.LAYERS * (1 - Mth.clamp((into - 15) / (CYCLE - 45), 0, 1));
        Block source = blocks.get(round % blocks.size());

        PoseStack ms = GuiScene.begin(graphics, x + 30, y + 30, 16, 25, -135);
        ms.translate(-.5, -.25, .5);

        BlockState head = Tracer.LATHE_HEADSTOCK.get().defaultBlockState().setValue(HORIZONTAL_FACING, Direction.NORTH).setValue(FORMED, true);
        BlockState tail = Tracer.LATHE_TAILSTOCK.get().defaultBlockState().setValue(HORIZONTAL_FACING, Direction.NORTH).setValue(FORMED, true);
        BlockState bed = Tracer.LATHE_BED.get().defaultBlockState().setValue(HORIZONTAL_FACING, Direction.NORTH).setValue(FORMED, true);
        GuiScene.block(graphics, head, 0, 0, 0);
        GuiScene.block(graphics, tail, 0, 0, -2);
        for (int z = 0; z > -3; z--)
            GuiScene.block(graphics, bed, 0, -1, z);

        float spin = Mth.DEG_TO_RAD * LatheLayout.spinDegrees(SPEED, time);
        ms.pushPose();
        HeadstockRenderer.spinAroundAxis(ms, spin);
        GuiScene.draw(graphics, GuiScene.partial(LathePartials.CHUCK, head));
        ms.popPose();

        ms.pushPose();
        ms.translate(0, 0, -2);
        HeadstockRenderer.spinAroundAxis(ms, spin);
        GuiScene.draw(graphics, GuiScene.partial(LathePartials.LIVE_CENTER, head));
        ms.popPose();

        int[] radii = new int[Profile.LAYERS];
        for (int i = 0; i < Profile.LAYERS; i++)
            radii[i] = i >= Mth.floor(layerPos) ? profile.radius(i) : Profile.MAX_RADIUS;
        BlockState block = source.defaultBlockState();
        ms.pushPose();
        ms.translate(0, 0, -1);
        HeadstockRenderer.spinAroundAxis(ms, spin);
        LatheLayout.layOnAxis(ms);
        HeadstockRenderer.quads(TurnedQuads.all(Profile.of(radii), block), block, null, null, ms, graphics.bufferSource(), LightTexture.FULL_BRIGHT);
        ms.popPose();

        LatheLayout.Carriage carriage = LatheLayout.carriage(profile, layerPos);
        ms.pushPose();
        ms.translate(0, 0, -1 + carriage.along());
        GuiScene.draw(graphics, GuiScene.partial(LathePartials.SADDLE, head));
        ms.translate(carriage.reach(), 0, 0);
        GuiScene.draw(graphics, GuiScene.partial(LathePartials.CROSS_SLIDE, head));
        GuiScene.draw(graphics, GuiScene.partial(LathePartials.TOOL_POST, head));
        GuiScene.draw(graphics, GuiScene.partial(LathePartials.CUTTER, head));
        ms.popPose();

        GuiScene.end(graphics);
    }
}
