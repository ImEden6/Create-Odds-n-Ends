package com.mervyn.createoddsnends.cnc;

import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FORMED;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mervyn.createoddsnends.Tracer;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.foundation.sound.SoundScapes.AmbienceGroup;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

public class CncBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int SLOTS = 9;

    final ItemStackHandler input = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            stale = true;
            changed();
        }
    };
    final ItemStackHandler output = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            stale = true;
            changed();
        }
    };

    @Nullable
    private CncJob job;
    @Nullable
    private ResourceLocation jobId;
    private boolean stale = true;
    private float progress;
    private int duration = CncJob.DEFAULT_DURATION;
    private boolean running;
    private ItemStack making = ItemStack.EMPTY;

    // client only: 0 parked, 1 following the toolpath, and how far along the toolpath it is in ticks.
    // The toolpath runs on its own clock rather than job progress so it keeps the same pace whatever the recipe length.
    float engaged;
    float lastEngaged;
    float clock;
    float lastClock;
    float door;
    float lastDoor;
    int doorHold;
    // opened by a player on the front, saved; the mill won't cut with them open
    private boolean doorsOpen;

    public CncBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    // rotation comes in through the drive at the far end
    public float speed() {
        if (!getBlockState().getValue(FORMED))
            return 0;
        BlockPos drive = CncStructure.drive(worldPosition, getBlockState().getValue(CncFrameBlock.FACING));
        return level.getBlockEntity(drive) instanceof CncDriveBlockEntity be ? be.getSpeed() : 0;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        BlockPos pos = getBlockPos();
        return new AABB(pos).inflate(5, 3, 5);
    }

    private void changed() {
        if (level != null && !level.isClientSide)
            notifyUpdate();
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) {
            tickAudio();
            tickDoor();
            lastEngaged = engaged;
            engaged = Mth.approach(engaged, running ? 1 : 0, .05f);
            lastClock = clock;
            if (running) {
                progress = Math.min(progress + Math.abs(speed()) / 32, duration);
                clock += Mth.clamp(Math.abs(speed()) / 64, .5f, 2);
            }
            return;
        }

        if (stale)
            refresh();
        boolean can = canRun();
        if (can != running) {
            running = can;
            sendData();
        }
        if (!running)
            return;
        progress += Math.abs(speed()) / 32;
        if (progress >= duration)
            finish();
    }

    private void refresh() {
        stale = false;
        CncJob found = getBlockState().getValue(FORMED) ? CncJob.find(level, input) : null;
        if (found == null || !found.id().equals(jobId))
            progress = 0;
        job = found;
        jobId = found == null ? null : found.id();
        duration = found == null ? CncJob.DEFAULT_DURATION : found.duration();
        making = found == null ? ItemStack.EMPTY : found.showcase();
        sendData();
    }

    private boolean canRun() {
        return job != null && !doorsOpen && speed() != 0 && getBlockState().getValue(FORMED) && fits(job);
    }

    private boolean fits(CncJob job) {
        for (List<ItemStack> stacks : job.possible()) {
            ItemStackHandler copy = new ItemStackHandler(SLOTS);
            for (int i = 0; i < SLOTS; i++)
                copy.setStackInSlot(i, output.getStackInSlot(i).copy());
            for (ItemStack stack : stacks)
                if (!ItemHandlerHelper.insertItemStacked(copy, stack, false).isEmpty())
                    return false;
        }
        return true;
    }

    private void finish() {
        CncJob done = job;
        progress = 0;
        if (done != null && done.take(input, false))
            for (ItemStack rolled : done.roll(level.random))
                ItemHandlerHelper.insertItemStacked(output, rolled, false);
        // pick the next job straight away so the client never sees a stop between two runs
        jobId = null;
        refresh();
        running = canRun();
        sendData();
    }

    public boolean isRunning() {
        return running;
    }

    public float progress(float partialTicks) {
        return Math.min(1, (progress + (running ? Math.abs(speed()) / 32 * partialTicks : 0)) / duration);
    }

    public float clock(float partialTicks) {
        return Mth.lerp(partialTicks, lastClock, clock);
    }

    public float door(float partialTicks) {
        float t = Mth.lerp(partialTicks, lastDoor, door);
        return t * t * (3 - 2 * t);
    }

    private void tickDoor() {
        lastDoor = door;
        if (doorHold > 0 && --doorHold == 0 && !doorsOpen)
            level.playLocalSound(worldPosition, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, .5f, 1.2f, false);
        boolean open = doorsOpen || doorHold > 0;
        door = Mth.approach(door, open ? 1 : 0, open ? .15f : .08f);
    }

    // a player loading or emptying it by hand swings the doors open for a moment, funnels don't
    void openDoors() {
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id != 1)
            return super.triggerEvent(id, param);
        if (level.isClientSide) {
            if (doorHold == 0 && door < .5f)
                level.playLocalSound(worldPosition, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, .5f, 1.2f, false);
            doorHold = 30;
        }
        return true;
    }

    public InteractionResult toggleDoors(Player player) {
        if (!level.isClientSide) {
            doorsOpen = !doorsOpen;
            level.playSound(null, worldPosition, doorsOpen ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, .6f, 1.2f);
            sendData();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public float engaged(float partialTicks) {
        return Mth.lerp(partialTicks, lastEngaged, engaged);
    }

    // what sits on the table: the part being cut, or the last finished piece waiting to be taken out
    public ItemStack workpiece() {
        if (running && !making.isEmpty())
            return making;
        for (int i = 0; i < SLOTS; i++)
            if (!output.getStackInSlot(i).isEmpty())
                return output.getStackInSlot(i);
        for (int i = 0; i < SLOTS; i++)
            if (!input.getStackInSlot(i).isEmpty())
                return input.getStackInSlot(i);
        return ItemStack.EMPTY;
    }

    public ItemInteractionResult useItem(Player player, ItemStack held) {
        if (held.isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            ItemStack left = ItemHandlerHelper.insertItemStacked(input, held.copy(), false);
            player.setItemInHand(player.getUsedItemHand(), left);
            openDoors();
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    public InteractionResult useEmptyHand(Player player) {
        ItemStackHandler from = player.isSecondaryUseActive() ? input : output;
        boolean any = false;
        for (int i = 0; i < SLOTS; i++)
            if (!from.getStackInSlot(i).isEmpty()) {
                any = true;
                if (!level.isClientSide)
                    player.getInventory().placeItemBackInInventory(from.extractItem(i, 64, false));
            }
        if (any && !level.isClientSide)
            openDoors();
        return any ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    @Override
    public void destroy() {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, input);
        ItemHelper.dropContents(level, worldPosition, output);
    }

    @OnlyIn(Dist.CLIENT)
    private void tickAudio() {
        if (running && speed() != 0)
            SoundScapes.play(AmbienceGroup.MILLING, worldPosition, Mth.clamp(Math.abs(speed()) / 256f + .8f, .8f, 1.3f));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        lang().translate("cnc.goggles.title").forGoggles(tooltip);
        if (!getBlockState().getValue(FORMED)) {
            lang().translate("cnc.goggles.unformed").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return true;
        }
        if (doorsOpen)
            lang().translate("cnc.goggles.doors_open").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        if (making.isEmpty()) {
            lang().translate("cnc.goggles.idle").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return true;
        }
        lang().translate("cnc.goggles.progress", Mth.floor(100 * progress / duration)).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        lang().add(making.getHoverName().copy()).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        return true;
    }

    private static LangBuilder lang() {
        return new LangBuilder(Tracer.MODID);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Input", input.serializeNBT(registries));
        tag.put("Output", output.serializeNBT(registries));
        tag.putFloat("Progress", progress);
        tag.putInt("Duration", duration);
        tag.putBoolean("Running", running);
        tag.putBoolean("DoorsOpen", doorsOpen);
        if (!making.isEmpty())
            tag.put("Making", making.save(registries));
        if (jobId != null)
            tag.putString("Job", jobId.toString());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        input.deserializeNBT(registries, tag.getCompound("Input"));
        output.deserializeNBT(registries, tag.getCompound("Output"));
        boolean wasRunning = running;
        running = tag.getBoolean("Running");
        doorsOpen = tag.getBoolean("DoorsOpen");
        if (!clientPacket || !running || !wasRunning || tag.getFloat("Progress") < progress - duration / 2f)
            progress = tag.getFloat("Progress");
        duration = Math.max(1, tag.getInt("Duration"));
        making = tag.contains("Making") ? ItemStack.parseOptional(registries, tag.getCompound("Making")) : ItemStack.EMPTY;
        jobId = tag.contains("Job") ? ResourceLocation.tryParse(tag.getString("Job")) : null;
        if (!clientPacket) {
            stale = true;
            job = null;
        }
    }
}
