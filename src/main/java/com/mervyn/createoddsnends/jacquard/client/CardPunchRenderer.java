package com.mervyn.createoddsnends.jacquard.client;

import com.mervyn.createoddsnends.jacquard.CardPunchBlock;
import com.mervyn.createoddsnends.jacquard.CardPunchBlockEntity;
import com.mervyn.createoddsnends.jacquard.Jacquard;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

// The punch's moving parts, ported from buildPunch(), plus a shaft stub on the input side.
// Stacks and the head follow the real card count.
public class CardPunchRenderer extends SafeBlockEntityRenderer<CardPunchBlockEntity> {
    public CardPunchRenderer(BlockEntityRendererProvider.Context context) {}

    private static double ease(double t) {
        return t * t * (3 - 2 * t);
    }

    @Override
    protected void renderSafe(CardPunchBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        Direction input = CardPunchBlock.input(state);
        float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), input.getAxis())
                * -input.getAxisDirection().getStep();

        double t = be.progress(partialTicks);
        int needed = be.cardsNeeded(), done = be.punched();
        ItemStack finished = be.finished();
        if (finished != null && !be.isPunching())
            done = needed = finished.getOrDefault(Jacquard.CARDS.get(), 16);
        // a 64 card chain would stand two blocks tall, so long ones are drawn squashed
        int n = needed <= 22 ? done : done * 22 / needed;
        int blanks = Math.min(14, Math.max(0, be.blankCount() - (be.isPunching() ? be.punched() : 0)));
        Direction facing = state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
        ms.pushPose();
        ms.translate(.5, 0, .5);
        ms.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        ms.translate(-.5, 0, -.5);
        ms.scale(1 / 16f, 1 / 16f, 1 / 16f);
        ms.translate(8, 0, 8);

        Mesh m = new Mesh(ms, buffer.getBuffer(RenderType.solid()), light, false);
        TextureAtlasSprite steel = Mesh.sprite("minecraft:block/iron_block");
        TextureAtlasSprite brass = Mesh.sprite("create:block/brass_block");
        TextureAtlasSprite paper = Mesh.sprite("createoddsnends:block/blank_card");

        m.push();
        m.rotX(angle, 8, 0);
        m.shaft(0, -8, -2, -2, -5.5f, 2, 2);
        m.pop();

        double down = t >= 0.38 && t < 0.55 ? Math.sin((t - 0.38) / 0.17 * Math.PI) : 0;
        float hy = (float) (11.5 - 2 * down);
        m.box(steel, -0.6f, hy + 3, -0.6f, 0.6f, 16, 0.6f);        m.box(brass, -4, hy, -2.5f, 4, hy + 3, 2.5f);
        for (int i = 0; i < 4; i++)
            m.box(steel, -3 + i * 2, hy - 0.8f, -0.4f, -2.4f + i * 2, hy, 0.4f);

        // blank stack behind, punched chain folding up in front
        for (int i = 0; i < blanks; i++)
            m.box(paper, -6, i * 0.5f, -21, 6, i * 0.5f + 0.45f, -11);
        for (int i = 0; i < n; i++) {
            float o = i % 2 != 0 ? 0.4f : -0.4f;
            m.box(card(i), -6 + o, i * 0.5f, 11, 6 + o, i * 0.5f + 0.45f, 21);
        }

        if (!be.isPunching()) {
            ms.popPose();
            return;
        }
        // the card travelling through
        double cz, cy = 9.05;
        TextureAtlasSprite tex = t > 0.47 ? card(n) : paper;
        if (t < 0.35)
            cz = Mth.lerp(ease(t / 0.35), -16, 0);
        else if (t < 0.55)
            cz = 0;
        else if (t < 0.88)
            cz = Mth.lerp(ease((t - 0.55) / 0.33), 0, 16);
        else {
            cz = 16;
            cy = Mth.lerp(ease((t - 0.88) / 0.12), 9.05, n * 0.5);
        }
        m.box(tex, -6, (float) cy, (float) cz - 5, 6, (float) cy + 0.45f, (float) cz + 5);
        ms.popPose();
    }

    private static TextureAtlasSprite card(int i) {
        return Mesh.sprite("createoddsnends:block/punched_card_" + i % 6);
    }
}
