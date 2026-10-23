package com.mervyn.createoddsnends.jacquard;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.organ.Organ;
import com.mervyn.createoddsnends.organ.Score;
import com.mervyn.createoddsnends.organ.Scores;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.item.ItemHelper;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class CardPunchBlockEntity extends KineticBlockEntity {
    final ItemStackHandler source = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return (stack.is(Jacquard.PUNCHED_CARD_CHAIN.get()) || stack.is(Jacquard.TAPESTRY.get())) && stack.has(Jacquard.DESIGN.get())
                    || stack.is(Organ.BOOK_MUSIC.get()) && stack.has(Organ.SCORE.get());
        }

        // a new piece to copy wins over an edit saved earlier
        @Override
        protected void onContentsChanged(int slot) {
            int key = sourceKey();
            if (key >= 0 && key != lastSource) {
                pending = -1;
                pendingScore = false;
                lastSource = key;
            }
            changed();
        }
    };
    final ItemStackHandler blanks = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(Jacquard.BLANK_CARD.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            changed();
        }
    };
    final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            changed();
        }
    };

    // what funnels, hoppers and arms see: blanks go in, chains come out
    final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return (slot == 0 ? blanks : output).getStackInSlot(0);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 ? blanks.insertItem(0, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 1 ? output.extractItem(0, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && blanks.isItemValid(0, stack);
        }
    };

    private int pending = -1;
    private boolean pendingScore;
    private int lastSource = -1;
    // what's being punched and its size, so the client can draw and describe it. A design's size
    // is in blocks; a score's is its rows and pages.
    private int job = -1;
    private boolean jobScore;
    private int jobWidth, jobHeight;
    // cards a score has used up so far. It takes them as it goes, a long one needs more than a stack
    private int consumed;
    private float worked;
    private boolean punching;

    public CardPunchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1);
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied = JacquardWork.PUNCH_STRESS;
        return JacquardWork.PUNCH_STRESS;
    }

    private void changed() {
        if (level != null && !level.isClientSide)
            notifyUpdate();
    }

    private int sourceId() {
        return source.getStackInSlot(0).getOrDefault(Jacquard.DESIGN.get(), -1);
    }

    private boolean sourceIsScore() {
        return source.getStackInSlot(0).is(Organ.BOOK_MUSIC.get());
    }

    private int sourceKey() {
        int id = sourceIsScore() ? source.getStackInSlot(0).getOrDefault(Organ.SCORE.get(), -1) : sourceId();
        return id < 0 ? -1 : id * 2 + (sourceIsScore() ? 1 : 0);
    }

    public int pending() {
        return pending;
    }

    public boolean pendingIsScore() {
        return pending >= 0 && pendingScore;
    }

    void setPending(int id) {
        pending = id;
        pendingScore = false;
        notifyUpdate();
    }

    public void setPendingScore(int id) {
        pending = id;
        pendingScore = true;
        notifyUpdate();
    }

    public int cardsNeeded() {
        return jobScore ? jobHeight : jobHeight * 16;
    }

    // Same split as the lathe: the server owns the count and syncs when punching starts or stops,
    // the client counts along in between and makes the noise.
    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide || level.getServer() == null) {
            if (punching) {
                float before = worked;
                worked = Math.min(worked + Math.abs(getSpeed()), cardsNeeded() * JacquardWork.CARD_WORK);
                if (strikes(worked) > strikes(before))
                    strike();
            }
            return;
        }

        int sourceKey = sourceKey();
        boolean isScore = pending >= 0 ? pendingScore : sourceKey >= 0 && (sourceKey & 1) == 1;
        int id = pending >= 0 ? pending : sourceKey < 0 ? -1 : sourceKey / 2;
        Design design = id < 0 || isScore ? null : Designs.get(level.getServer()).get(id);
        Score score = id < 0 || !isScore ? null : Scores.get(level.getServer()).get(id);
        if (id != job || isScore != jobScore) {
            for (; consumed > 0; consumed -= Math.min(consumed, 64))
                Block.popResource(level, worldPosition, blanks.insertItem(0, new ItemStack(Jacquard.BLANK_CARD.get(), Math.min(consumed, 64)), false));
            job = id;
            jobScore = isScore;
            jobWidth = design != null ? design.width() : score != null ? score.rowCount() : 0;
            jobHeight = design != null ? design.height() : score != null ? score.pages() : 0;
            worked = 0;
            sendData();
        }
        boolean enough = jobScore ? blanks.getStackInSlot(0).getCount() >= 1 : blanks.getStackInSlot(0).getCount() >= cardsNeeded();
        boolean can = (design != null || score != null) && getSpeed() != 0 && output.getStackInSlot(0).isEmpty() && enough;
        if (can != punching) {
            punching = can;
            sendData();
        }
        if (!punching)
            return;
        worked = Math.min(worked + Math.abs(getSpeed()), cardsNeeded() * JacquardWork.CARD_WORK);
        if (jobScore && punched() > consumed) {
            blanks.extractItem(0, 1, false);
            consumed++;
        }
        if (worked >= cardsNeeded() * JacquardWork.CARD_WORK)
            finish();
    }

    // the head comes down a little before half way through each card
    private static int strikes(float worked) {
        return (int) Math.floor(worked / JacquardWork.CARD_WORK - 0.47);
    }

    private void finish() {
        ItemStack made;
        if (jobScore) {
            made = new ItemStack(Organ.BOOK_MUSIC.get());
            made.set(Organ.SCORE.get(), job);
        } else {
            blanks.extractItem(0, cardsNeeded(), false);
            made = new ItemStack(Jacquard.PUNCHED_CARD_CHAIN.get());
            made.set(Jacquard.DESIGN.get(), job);
            made.set(Jacquard.CARDS.get(), cardsNeeded());
        }
        output.setStackInSlot(0, made);
        consumed = 0;
        worked = 0;
        punching = false;
        sendData();
    }

    private void strike() {
        RandomSource random = level.random;
        level.playLocalSound(worldPosition, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 0.4f, 1.4f + random.nextFloat() * 0.2f, false);
        ItemParticleOption paper = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.PAPER));
        for (int i = 0; i < 4; i++)
            level.addParticle(paper, worldPosition.getX() + 0.5 + random.nextGaussian() * 0.1, worldPosition.getY() + 0.58,
                    worldPosition.getZ() + 0.5 + random.nextGaussian() * 0.1, random.nextGaussian() * 0.04, 0.06, random.nextGaussian() * 0.04);
    }

    // how far the card under the head is, 0 to 1
    public double progress(float partialTick) {
        if (!punching)
            return 0;
        double w = worked + Math.abs(getSpeed()) * partialTick;
        return Math.min(w / JacquardWork.CARD_WORK - punched(), 0.9999);
    }

    public int punched() {
        return Math.min((int) (worked / JacquardWork.CARD_WORK), cardsNeeded());
    }

    public boolean isPunching() {
        return punching;
    }

    public int blanksLeft() {
        return blankCount() - (punching && !jobScore ? punched() : 0);
    }

    public int blankCount() {
        return blanks.getStackInSlot(0).getCount();
    }

    @Nullable
    public ItemStack finished() {
        ItemStack out = output.getStackInSlot(0);
        return out.isEmpty() ? null : out;
    }

    @Override
    public void destroy() {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, source);
        ItemHelper.dropContents(level, worldPosition, blanks);
        ItemHelper.dropContents(level, worldPosition, output);
        for (; consumed > 0; consumed -= Math.min(consumed, 64))
            Block.popResource(level, worldPosition, new ItemStack(Jacquard.BLANK_CARD.get(), Math.min(consumed, 64)));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        lang().translate("punch.goggles.title").forGoggles(tooltip);
        if (job < 0) {
            lang().translate("punch.goggles.nothing").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return true;
        }
        String kind = jobScore ? "punch.goggles.score_" : "punch.goggles.";
        lang().translate(kind + (pending >= 0 ? "pending" : "copy"), jobWidth, jobHeight)
                .style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        lang().translate(jobScore ? "punch.goggles.pages" : "punch.goggles.cards", punched(), cardsNeeded()).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        int missing = cardsNeeded() - blankCount() - (jobScore ? consumed : 0);
        if (missing > 0)
            lang().translate("punch.goggles.missing", missing).style(ChatFormatting.RED).forGoggles(tooltip, 1);
        return true;
    }

    private static LangBuilder lang() {
        return new LangBuilder(Tracer.MODID);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Source", source.serializeNBT(registries));
        tag.put("Blanks", blanks.serializeNBT(registries));
        tag.put("Output", output.serializeNBT(registries));
        tag.putInt("Pending", pending);
        tag.putBoolean("PendingScore", pendingScore);
        tag.putInt("Job", job);
        tag.putBoolean("JobScore", jobScore);
        tag.putInt("Consumed", consumed);
        tag.putInt("JobWidth", jobWidth);
        tag.putInt("JobHeight", jobHeight);
        tag.putFloat("Worked", worked);
        tag.putBoolean("Punching", punching);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        source.deserializeNBT(registries, tag.getCompound("Source"));
        blanks.deserializeNBT(registries, tag.getCompound("Blanks"));
        output.deserializeNBT(registries, tag.getCompound("Output"));
        pending = tag.contains("Pending") ? tag.getInt("Pending") : -1;
        pendingScore = tag.getBoolean("PendingScore");
        job = tag.contains("Job") ? tag.getInt("Job") : -1;
        jobScore = tag.getBoolean("JobScore");
        consumed = tag.getInt("Consumed");
        jobWidth = tag.getInt("JobWidth");
        jobHeight = tag.getInt("JobHeight");
        worked = tag.getFloat("Worked");
        punching = tag.getBoolean("Punching");
        lastSource = sourceKey();
    }
}
