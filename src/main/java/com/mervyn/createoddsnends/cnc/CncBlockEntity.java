package com.mervyn.createoddsnends.cnc;

import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FORMED;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mervyn.createoddsnends.Tracer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
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

public class CncBlockEntity extends KineticBlockEntity {
    static final int SLOTS = 9;
    static final float STRESS_IMPACT = 8;

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

    // client only: 0 parked, 1 following the toolpath
    float engaged;
    float lastEngaged;

    public CncBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public float calculateStressApplied() {
        lastStressApplied = STRESS_IMPACT;
        return STRESS_IMPACT;
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
            lastEngaged = engaged;
            engaged = Mth.approach(engaged, running ? 1 : 0, .05f);
            if (running)
                progress = Math.min(progress + Math.abs(getSpeed()) / 32, duration);
            return;
        }

        if (stale) {
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
        boolean can = job != null && getSpeed() != 0 && getBlockState().getValue(FORMED) && fits(job);
        if (can != running) {
            running = can;
            sendData();
        }
        if (!running)
            return;
        progress += Math.abs(getSpeed()) / 32;
        if (progress >= duration)
            finish();
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
        running = false;
        if (done == null || !done.take(input, false)) {
            stale = true;
            return;
        }
        for (ItemStack rolled : done.roll(level.random))
            ItemHandlerHelper.insertItemStacked(output, rolled, false);
        stale = true;
        sendData();
    }

    public boolean isRunning() {
        return running;
    }

    public float progress(float partialTicks) {
        return Math.min(1, (progress + (running ? Math.abs(getSpeed()) / 32 * partialTicks : 0)) / duration);
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
        return any ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    @Override
    public void destroy() {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, input);
        ItemHelper.dropContents(level, worldPosition, output);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void tickAudio() {
        super.tickAudio();
        if (running && getSpeed() != 0)
            SoundScapes.play(AmbienceGroup.MILLING, worldPosition, Mth.clamp(Math.abs(getSpeed()) / 256f + .8f, .8f, 1.3f));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        lang().translate("cnc.goggles.title").forGoggles(tooltip);
        if (!getBlockState().getValue(FORMED)) {
            lang().translate("cnc.goggles.unformed").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return true;
        }
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
        progress = tag.getFloat("Progress");
        duration = Math.max(1, tag.getInt("Duration"));
        running = tag.getBoolean("Running");
        making = tag.contains("Making") ? ItemStack.parseOptional(registries, tag.getCompound("Making")) : ItemStack.EMPTY;
        jobId = tag.contains("Job") ? ResourceLocation.tryParse(tag.getString("Job")) : null;
        if (!clientPacket) {
            stale = true;
            job = null;
        }
    }
}
