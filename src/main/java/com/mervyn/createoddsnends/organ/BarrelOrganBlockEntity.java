package com.mervyn.createoddsnends.organ;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.organ.client.OrganPlayback;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

// Same split as the punch and the lathe: the server owns the row and syncs when playback starts,
// stops, changes speed, wraps or finishes, and the client counts along in between, playing the
// notes and moving the pages.
public class BarrelOrganBlockEntity extends KineticBlockEntity {
    public static final float STRESS = 2;

    final ItemStackHandler book = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(Organ.BOOK_MUSIC.get()) && stack.has(Organ.SCORE.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            row = 0;
            finished = false;
            running = false;
            if (level != null && !level.isClientSide)
                notifyUpdate();
        }
    };

    final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return book.getStackInSlot(0);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return book.insertItem(0, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return running ? ItemStack.EMPTY : book.extractItem(0, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return book.isItemValid(0, stack);
        }
    };

    ScrollOptionBehaviour<PlayMode> mode;
    float row;
    boolean finished;
    boolean running;
    private int pulse;

    public final float[] pipeLit = new float[9];
    public long drumHit = -100;
    public int lastRow = -1;
    public Score gridScore;
    public int[] grid;

    // what the last walk of the wind lines found, kept on both sides: the client plays the
    // instruments on these chests and only animates the cabinet for what they cover
    private List<BlockPos> chests = List.of();
    private int coveredVoices, instruments;
    private boolean kick, snare, rebuildQueued;
    public boolean sounding;

    public BarrelOrganBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public static IItemHandler books(Level level, BlockPos pos) {
        BlockPos at = level.getBlockState(pos).is(Organ.BARREL_ORGAN_TOP.get()) ? pos.below() : pos;
        return level.getBlockEntity(at) instanceof BarrelOrganBlockEntity be ? be.automation : null;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        mode = new ScrollOptionBehaviour<>(PlayMode.class, Component.translatable("createoddsnends.organ.mode"), this, new ModeSlot());
        behaviours.add(mode);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.5).expandTowards(0, 1.5, 0);
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied = STRESS;
        return STRESS;
    }

    public int scoreId() {
        return book.getStackInSlot(0).getOrDefault(Organ.SCORE.get(), -1);
    }

    @Nullable
    public Score score() {
        int id = scoreId();
        if (level.isClientSide || level.getServer() == null)
            return ScoreSync.client(id);
        return id < 0 ? null : Scores.get(level.getServer()).get(id);
    }

    public float rowsPerTick() {
        return Math.abs(getSpeed()) / 16f / 20f;
    }

    public boolean loops() {
        return mode != null && mode.get() == PlayMode.LOOP;
    }

    boolean pulsing() {
        return pulse > 0;
    }

    private boolean powered() {
        return level.hasNeighborSignal(worldPosition) || level.hasNeighborSignal(worldPosition.above());
    }

    @Override
    public void tick() {
        super.tick();
        Score score = score();
        if (level.isClientSide || level.getServer() == null) {
            if (running && score != null) {
                row += rowsPerTick();
                if (row >= score.rowCount())
                    row = loops() ? row - score.rowCount() : score.rowCount();
            }
            OrganPlayback.tick(this, score);
            return;
        }
        advance(score);
        feedChests(score);
    }

    private void advance(@Nullable Score score) {
        if (pulse > 0 && --pulse == 0)
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        boolean run = score != null && getSpeed() != 0 && !finished && !powered();
        if (run != running) {
            running = run;
            notifyUpdate();
        }
        if (!running)
            return;
        row += rowsPerTick();
        if (row < score.rowCount())
            return;
        if (loops()) {
            row = row - score.rowCount() < score.rowCount() ? row - score.rowCount() : 0;
        } else {
            row = score.rowCount();
            finished = true;
            running = false;
            pulse = 2;
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        notifyUpdate();
    }

    private void feedChests(@Nullable Score score) {
        if (!running || score == null || chests.isEmpty())
            return;
        if (gridScore != score) {
            gridScore = score;
            grid = score.grid();
        }
        int offset = Math.min(score.rowCount() - 1, (int) row) * Score.COLUMNS;
        for (BlockPos pos : chests)
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof WindChestBlockEntity chest)
                chest.feed(grid, offset);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!level.isClientSide && level.getServer() != null) {
            WindNetwork.track(this);
            queueRebuild();
        }
    }

    void queueRebuild() {
        MinecraftServer server = level.getServer();
        if (rebuildQueued || server == null)
            return;
        rebuildQueued = true;
        server.tell(new TickTask(server.getTickCount(), () -> {
            rebuildQueued = false;
            if (!isRemoved())
                rebuildNetwork();
        }));
    }

    private void rebuildNetwork() {
        survey(WindNetwork.walk(level, worldPosition));
        sendData();
    }

    // Ponder has no server to walk the lines, so its scenes ask once the chests are in place
    public void ponderNetwork() {
        survey(WindNetwork.walk(level, worldPosition));
    }

    private void survey(List<BlockPos> found) {
        chests = found;
        coveredVoices = 0;
        instruments = 0;
        kick = snare = false;
        for (BlockPos pos : found) {
            if (!(level.getBlockEntity(pos) instanceof WindChestBlockEntity chest))
                continue;
            BlockState top = level.getBlockState(pos.above());
            int voices = chest.voiceColour() == null ? 0xFFFF : 1 << chest.voiceColour().getId();
            if (top.getBlock() instanceof InstrumentBlock block) {
                instruments++;
                switch (block.instrument) {
                    case KICK_DRUM -> kick = true;
                    case SNARE_DRUM -> snare = true;
                    default -> coveredVoices |= voices;
                }
            } else if (WindChestBlock.whistleOnTop(top)) {
                instruments++;
                coveredVoices |= voices;
            }
        }
    }

    public List<BlockPos> chests() {
        return chests;
    }

    public boolean covers(int voice) {
        return (coveredVoices >> voice & 1) != 0;
    }

    public boolean drums(int column) {
        return column == 0 ? kick : column == 1 && snare;
    }

    public int chestCount() {
        return chests.size();
    }

    // the client's count, moved on to the frame being drawn
    public float shownRow(float partialTicks) {
        return running ? row + rowsPerTick() * partialTicks : row;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean hasBook() {
        return !book.getStackInSlot(0).isEmpty();
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (level != null && !level.isClientSide)
            sendData();
    }

    public ItemInteractionResult useBook(Player player, ItemStack held) {
        if (!book.isItemValid(0, held))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            ItemStack old = book.getStackInSlot(0);
            book.setStackInSlot(0, held.consumeAndReturn(1, player));
            if (!old.isEmpty())
                player.getInventory().placeItemBackInInventory(old);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    public InteractionResult takeBook(Player player) {
        if (!hasBook())
            return InteractionResult.PASS;
        if (!level.isClientSide)
            player.getInventory().placeItemBackInInventory(book.extractItem(0, 1, false));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void destroy() {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, book);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (level != null && level.isClientSide)
            OrganPlayback.stop(this);
        else if (level != null)
            WindNetwork.untrack(this);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        lang().translate("organ.goggles.title").forGoggles(tooltip);
        Score score = hasBook() ? score() : null;
        if (score == null) {
            lang().translate("organ.goggles.no_book").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        } else {
            float perSecond = Math.abs(getSpeed()) / 16f;
            String seconds = perSecond > 0 ? String.format("%.1f", score.rowCount() / perSecond) : "-";
            lang().translate("organ.goggles.length", score.rowCount(), seconds).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            int bar = Math.min(score.bars(), (int) (row / score.barLength()) + 1);
            lang().translate("organ.goggles.bar", bar, score.bars()).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            lang().translate("organ.goggles.mode", Component.translatable(mode.get().getTranslationKey())).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            String state = finished ? "finished" : running ? "playing" : "paused";
            lang().translate("organ.goggles." + state).style(finished ? ChatFormatting.GOLD : ChatFormatting.GRAY).forGoggles(tooltip, 1);
        }
        if (!chests.isEmpty())
            lang().translate("organ.goggles.chests", chests.size()).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        if (instruments == 0)
            lang().translate("organ.goggles.no_instruments").style(ChatFormatting.RED).forGoggles(tooltip, 1);
        else
            lang().translate("organ.goggles.instruments", instruments).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        return true;
    }

    private static LangBuilder lang() {
        return new LangBuilder(Tracer.MODID);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Book", book.serializeNBT(registries));
        tag.putFloat("Row", row);
        tag.putBoolean("Finished", finished);
        tag.putBoolean("Running", running);
        tag.putInt("Pulse", pulse);
        if (clientPacket) {
            tag.putLongArray("Chests", chests.stream().mapToLong(BlockPos::asLong).toArray());
            tag.putInt("Voices", coveredVoices);
            tag.putInt("Instruments", instruments);
            tag.putBoolean("Kick", kick);
            tag.putBoolean("Snare", snare);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        book.deserializeNBT(registries, tag.getCompound("Book"));
        row = tag.getFloat("Row");
        finished = tag.getBoolean("Finished");
        running = tag.getBoolean("Running");
        pulse = tag.getInt("Pulse");
        if (clientPacket) {
            chests = Arrays.stream(tag.getLongArray("Chests")).mapToObj(BlockPos::of).toList();
            coveredVoices = tag.getInt("Voices");
            instruments = tag.getInt("Instruments");
            kick = tag.getBoolean("Kick");
            snare = tag.getBoolean("Snare");
        }
    }

    private static class ModeSlot extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 10, 16.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction == state.getValue(BarrelOrganBlock.HORIZONTAL_FACING).getCounterClockWise();
        }
    }
}
