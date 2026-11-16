package com.mervyn.createoddsnends.cnc;

import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FORMED;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.client.MachineSound;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
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

    float engaged;
    float lastEngaged;
    float clock;
    float lastClock;
    float door;
    float lastDoor;
    private boolean doorsOpen;
    private boolean armOpen;
    private List<ItemStack> tools = List.of();
    private int armDoors;
    private int armWaiting;

    public CncBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

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

        if (armWaiting > 0)
            armWaiting--;
        if (armDoors > 0 && --armDoors == 0) {
            level.playSound(null, worldPosition, Tracer.CNC_DOOR_CLOSE.get(), SoundSource.BLOCKS, .6f, 1);
            sendData();
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
        return job != null && !doorsOpen && armDoors == 0 && !(armWaiting > 0 && progress == 0) && speed() != 0 && getBlockState().getValue(FORMED) && fits(job);
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
        boolean open = doorsOpen || armOpen;
        door = Mth.approach(door, open ? 1 : 0, open ? .15f : .08f);
    }

    void setDoors(boolean open) {
        if (open == doorsOpen || level.isClientSide)
            return;
        doorsOpen = open;
        CncDoors.track(level, worldPosition, open);
        level.playSound(null, worldPosition, (open ? Tracer.CNC_DOOR_OPEN : Tracer.CNC_DOOR_CLOSE).get(), SoundSource.BLOCKS, .6f, 1);
        sendData();
    }

    public List<ItemStack> tools() {
        return tools;
    }

    public float engaged(float partialTicks) {
        return Mth.lerp(partialTicks, lastEngaged, engaged);
    }

    public ItemStack workpiece() {
        if (running && !making.isEmpty())
            return making;
        for (int i = 0; i < SLOTS; i++)
            if (!output.getStackInSlot(i).isEmpty())
                return output.getStackInSlot(i);
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (!stack.isEmpty() && tools.stream().noneMatch(tool -> ItemStack.isSameItem(tool, stack)))
                return stack;
        }
        return ItemStack.EMPTY;
    }

    public ItemInteractionResult useItem(Player player, InteractionHand hand, ItemStack held) {
        if (held.isEmpty() || !CncJob.accepts(level, held))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            if (!doorsOpen)
                setDoors(true);
            else
                player.setItemInHand(hand, ItemHandlerHelper.insertItemStacked(input, held.copy(), false));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    public InteractionResult useEmptyHand(Player player) {
        if (!level.isClientSide) {
            if (!doorsOpen)
                setDoors(true);
            else if (player.isSecondaryUseActive() || !(giveBack(player, output) || giveBack(player, input)))
                setDoors(false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    boolean armCanReach() {
        return !running || progress == 0;
    }

    void armWaiting() {
        armWaiting = 40;
    }

    void armVisit() {
        armWaiting = 0;
        if (armDoors == 0 && !doorsOpen) {
            level.playSound(null, worldPosition, Tracer.CNC_DOOR_OPEN.get(), SoundSource.BLOCKS, .6f, 1);
            armDoors = 30;
            sendData();
        } else
            armDoors = Math.max(armDoors, 30);
    }

    public InteractionResult pressDoorButton() {
        setDoors(!doorsOpen);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean giveBack(Player player, ItemStackHandler from) {
        for (int i = SLOTS - 1; i >= 0; i--)
            if (!from.getStackInSlot(i).isEmpty()) {
                player.getInventory().placeItemBackInInventory(from.extractItem(i, 64, false));
                return true;
            }
        return false;
    }

    @Override
    public void initialize() {
        super.initialize();
        CncDoors.track(level, worldPosition, doorsOpen);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        CncDoors.track(level, worldPosition, false);
    }

    @Override
    public void remove() {
        super.remove();
        CncDoors.track(level, worldPosition, false);
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
            MachineSound.keep(this, Tracer.CNC_CUTTING.get(), speed());
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
        if (clientPacket)
            tag.putBoolean("ArmDoors", armDoors > 0);
        if (clientPacket && job != null) {
            ListTag shown = new ListTag();
            for (Ingredient tool : job.tools())
                for (int i = 0; i < SLOTS; i++)
                    if (tool.test(input.getStackInSlot(i))) {
                        shown.add(input.getStackInSlot(i).copyWithCount(1).save(registries));
                        break;
                    }
            tag.put("Tools", shown);
        }
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
        if (clientPacket) {
            armOpen = tag.getBoolean("ArmDoors");
            List<ItemStack> shown = new ArrayList<>();
            for (Tag tool : tag.getList("Tools", Tag.TAG_COMPOUND))
                ItemStack.parse(registries, tool).ifPresent(shown::add);
            tools = shown;
        }
        if (level != null)
            CncDoors.track(level, worldPosition, doorsOpen);
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
