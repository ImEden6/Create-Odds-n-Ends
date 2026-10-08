package com.mervyn.createoddsnends.organ;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlockEntity;
import com.simibubi.create.content.decoration.steamWhistle.WhistleExtenderBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamJetParticleData;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Blows while its organ is sounding a matching note. The organ feeds it every tick it plays, so a
// chest that stops being fed for any reason lets go on its own.
public class WindChestBlockEntity extends SmartBlockEntity {
    // measured from Create's whistle samples: small, medium and large are F#5, F#4 and F#3 with
    // nothing extended, and each extension drops a semitone
    private static final int[] WHISTLE_BASE_MIDI = {78, 66, 54};

    FilteringBehaviour voice;
    ScrollOptionBehaviour<MatchMode> match;
    private boolean blowing;
    private long fedAt, wantedAt;

    public WindChestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        voice = new FilteringBehaviour(this, new VoiceSlot()).withPredicate(stack -> stack.getItem() instanceof DyeItem)
                .withCallback(stack -> WindNetwork.changed());
        behaviours.add(voice);
        match = new ScrollOptionBehaviour<>(MatchMode.class, Component.translatable("createoddsnends.organ.match"), this, new MatchSlot());
        behaviours.add(match);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition).expandTowards(0, 14, 0);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!level.isClientSide) {
            WindNetwork.changed();
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (level != null && !level.isClientSide)
            WindNetwork.changed();
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) {
            if (blowing && level.getGameTime() % 4 == 0)
                steam();
        } else if (blowing && level.getGameTime() - fedAt > 1) {
            setBlowing(false);
        }
    }

    private void steam() {
        WhistleBlockEntity whistle = whistle();
        if (whistle != null)
            level.addParticle(new SteamJetParticleData(1), worldPosition.getX() + .5, whistleTop(whistle), worldPosition.getZ() + .5, 0, 1, 0);
    }

    // world height of the rim at the top of the stack: a single extension's rim is halfway up its
    // block, every other one overhangs the block above
    public double whistleTop(WhistleBlockEntity whistle) {
        BlockPos top = whistle.getBlockPos();
        float rim = 18;
        BlockState above = level.getBlockState(top.above());
        while (AllBlocks.STEAM_WHISTLE_EXTENSION.has(above)) {
            top = top.above();
            rim = above.getValue(WhistleExtenderBlock.SHAPE) == WhistleExtenderBlock.WhistleExtenderShape.SINGLE ? 10 : 18;
            above = level.getBlockState(top.above());
        }
        return top.getY() + rim / 16f;
    }

    public boolean isBlowing() {
        return blowing;
    }

    // Ponder has no organ feeding its chests, so scenes set this by hand
    public void showBlowing(boolean on) {
        blowing = on;
    }

    private void setBlowing(boolean on) {
        if (on == blowing)
            return;
        blowing = on;
        sendData();
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    @Nullable
    public DyeColor voiceColour() {
        return voice.getFilter().getItem() instanceof DyeItem dye ? dye.getDyeColor() : null;
    }

    @Nullable
    public WhistleBlockEntity whistle() {
        BlockPos above = worldPosition.above();
        BlockState state = level.getBlockState(above);
        return WindChestBlock.whistleOnTop(state) && level.getBlockEntity(above) instanceof WhistleBlockEntity whistle ? whistle : null;
    }

    private static int midi(WhistleBlockEntity whistle) {
        int id = whistle.getPitchId();
        return WHISTLE_BASE_MIDI[id / 100] - id % 100;
    }

    // grid is Score.grid(), offset the start of the row being played
    void feed(int[] grid, int offset) {
        long now = level.getGameTime();
        fedAt = now;
        DyeColor filter = voiceColour();
        WhistleBlockEntity whistle = whistle();
        int target = whistle == null ? -1 : midi(whistle);
        boolean octaves = match.get() == MatchMode.OCTAVE;
        boolean wanted = false;
        for (int column = 0; column < Score.COLUMNS && !wanted; column++) {
            int cell = grid[offset + column];
            if (cell == 0 || filter != null && cell - 1 != filter.getId())
                continue;
            if (target < 0)
                wanted = true;
            else if (column >= 2) {
                int midi = column + Score.LOW_MIDI;
                wanted = octaves ? Math.floorMod(midi - target, 12) == 0 : midi == target;
            }
        }
        // two organs can share a chest, so one that doesn't want this note can't release it
        if (wanted) {
            wantedAt = now;
            setBlowing(true);
        } else if (now - wantedAt > 1) {
            setBlowing(false);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (clientPacket)
            tag.putBoolean("Blowing", blowing);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (clientPacket)
            blowing = tag.getBoolean("Blowing");
    }

    private static class VoiceSlot extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 9, 16.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction == state.getValue(WindChestBlock.FACING);
        }
    }

    private static class MatchSlot extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 9, 16.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction == state.getValue(WindChestBlock.FACING).getOpposite();
        }
    }
}
