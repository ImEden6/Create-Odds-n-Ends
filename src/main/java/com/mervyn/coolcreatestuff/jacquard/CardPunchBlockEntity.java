package com.mervyn.coolcreatestuff.jacquard;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class CardPunchBlockEntity extends KineticBlockEntity {
    // Placeholder: a client-only loop punching cards forever, one per 48/|speed| seconds, in place
    // of real punching progress later. Mirrors the preview's `punch` state object.
    private double n, t;

    public CardPunchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1);
    }

    // updatePunch()
    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide)
            return;
        double dt = 1 / 20.0;
        t += dt * Math.abs(getSpeed()) / 48;
        while (t >= 1) {
            t -= 1;
            n++;
            if (n > 22)
                n = 0;
        }
    }

    public double progress(float partialTick) {
        return Math.min(t + Math.abs(getSpeed()) / 48 / 20 * partialTick, 0.9999);
    }

    public int punched() {
        return (int) n;
    }
}
