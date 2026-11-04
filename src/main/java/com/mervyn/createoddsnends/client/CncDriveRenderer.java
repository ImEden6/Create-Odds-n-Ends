package com.mervyn.createoddsnends.client;

import com.mervyn.createoddsnends.cnc.CncDriveBlock;
import com.mervyn.createoddsnends.cnc.CncDriveBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;

import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class CncDriveRenderer extends KineticBlockEntityRenderer<CncDriveBlockEntity> {
    public CncDriveRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(CncDriveBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacing(CncRenderer.DRIVE_STUB, state, CncDriveBlock.shaftSide(state));
    }

    public static SimpleBlockEntityVisualizer.Factory<CncDriveBlockEntity> visual() {
        return (context, be, partialTick) -> new OrientedRotatingVisual<>(context, be, partialTick, Direction.SOUTH,
                CncDriveBlock.shaftSide(be.getBlockState()), Models.partial(CncRenderer.DRIVE_STUB));
    }
}
