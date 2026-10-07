package com.mervyn.createoddsnends.jacquard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.jetbrains.annotations.Nullable;

import com.mervyn.createoddsnends.Tracer;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;

// Controller of a formed loom. Holds the card chain, the thread and the woven piece, and weaves
// at the speed the drive hands it.
public class JacquardHeadBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    // stands in for the warp layout when there's no chain, nothing of it gets woven
    public static final Design DEBUG_DESIGN = DesignGen.generate(3, 2, 3, 5, "kilim");
    // where a row's first pick holds when its thread runs out: shed open, shuttle still in its box
    private static final double GATE = 0.2;

    final ItemStackHandler chain = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(Jacquard.PUNCHED_CARD_CHAIN.get()) && stack.has(Jacquard.DESIGN.get());
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
    // per dye, in design pixels
    private final int[] thread = new int[16];

    private float speed;
    // the piece on the loom: which design, which pick, how far through it
    private int pieceDesign = -1;
    private int pick;
    private double t;
    private boolean rowPaid, weaving, waiting;
    // client only, turns the drive shafts
    private double spin;

    public JacquardHeadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition).inflate(3);
    }

    private void changed() {
        if (level != null && !level.isClientSide)
            notifyUpdate();
    }

    public float getSpeed() {
        return speed;
    }

    void setSpeed(float speed) {
        if (this.speed == speed)
            return;
        this.speed = speed;
        sendData();
    }

    public int getDesignId() {
        return chain.getStackInSlot(0).getOrDefault(Jacquard.DESIGN.get(), -1);
    }

    // debug command and showcase: loads a chain for the design
    public void setDesignId(int designId) {
        ItemStack stack = new ItemStack(Jacquard.PUNCHED_CARD_CHAIN.get());
        stack.set(Jacquard.DESIGN.get(), designId);
        chain.setStackInSlot(0, stack);
    }

    @Nullable
    private Design design() {
        int id = getDesignId();
        if (level.isClientSide || level.getServer() == null)
            return DesignSync.client(id);
        return id < 0 ? null : Designs.get(level.getServer()).get(id);
    }

    public boolean hasDesign() {
        return design() != null;
    }

    public Design shownDesign() {
        Design design = design();
        return design != null ? design : DEBUG_DESIGN;
    }

    public static Item wool(int colour) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(DyeColor.byId(colour).getName() + "_wool"));
    }

    public static int woolColour(ItemStack stack) {
        for (int i = 0; i < 16; i++)
            if (stack.is(wool(i)))
                return i;
        return -1;
    }

    // takes as much of the wool as fits under the cap, hands back the rest
    ItemStack addThread(ItemStack stack, boolean simulate) {
        int colour = woolColour(stack);
        if (colour < 0)
            return stack;
        int fits = Math.min(stack.getCount(), (JacquardWork.THREAD_CAP - thread[colour]) / JacquardWork.THREAD_PER_WOOL);
        if (fits <= 0)
            return stack;
        if (!simulate) {
            thread[colour] += fits * JacquardWork.THREAD_PER_WOOL;
            changed();
        }
        return stack.copyWithCount(stack.getCount() - fits);
    }

    public void fillThread() {
        Arrays.fill(thread, JacquardWork.THREAD_CAP);
        changed();
    }

    // pixels of each dye in woven row w (0 = the design's bottom)
    private static int[] rowNeeds(Design d, int w) {
        int[] needs = new int[16];
        int y = d.pixelHeight() - 1 - w;
        for (int x = 0; x < d.pixelWidth(); x++)
            needs[d.dye(x, y)]++;
        return needs;
    }

    private boolean hasThread(int[] needs) {
        for (int c = 0; c < 16; c++)
            if (thread[c] < needs[c])
                return false;
        return true;
    }

    private boolean outputRoom(int id) {
        ItemStack out = output.getStackInSlot(0);
        return out.isEmpty() || out.is(Jacquard.TAPESTRY.get()) && out.getCount() < out.getMaxStackSize()
                && out.getOrDefault(Jacquard.DESIGN.get(), -1) == id;
    }

    // Both sides run this. The server owns the piece and syncs on start, stop, pause and resume;
    // speed changes sync through setSpeed. The client weaves along in between, including waiting
    // at a row whose thread it knows has run out, so the two only differ by rounding.
    @Override
    public void tick() {
        super.tick();
        if (!getBlockState().getValue(LoomPartBlock.FORMED))
            return;
        if (level.isClientSide)
            spin += Math.abs(speed) / 60 * Math.PI * 2 / 20;
        int id = getDesignId();
        if (id != pieceDesign) {
            pieceDesign = id;
            pick = 0;
            t = 0;
            rowPaid = waiting = false;
        }
        Design design = design();
        if (!level.isClientSide) {
            boolean can = design != null && speed != 0 && (pick > 0 || t > 0 || outputRoom(id));
            if (can != weaving) {
                weaving = can;
                sendData();
            }
        }
        if (weaving && design != null)
            weave(design);
    }

    private void weave(Design design) {
        Picks p = picks();
        double left = Math.abs(speed) / JacquardWork.PICK_RPM / 20;
        while (left > 0) {
            int row = p.row()[pick];
            boolean gate = p.rowStart()[row] == pick && !rowPaid;
            if (gate && t >= GATE) {
                int[] needs = rowNeeds(design, row);
                if (!hasThread(needs)) {
                    setWaiting(true);
                    return;
                }
                for (int c = 0; c < 16; c++)
                    thread[c] -= needs[c];
                rowPaid = true;
                setWaiting(false);
                continue;
            }
            double step = Math.min(left, (gate ? GATE : 1) - t);
            t += step;
            left -= step;
            if (t >= 1) {
                t = 0;
                pick++;
                rowPaid = false;
                if (pick >= p.total()) {
                    finishPiece();
                    return;
                }
            }
        }
    }

    private void setWaiting(boolean waiting) {
        if (this.waiting == waiting)
            return;
        this.waiting = waiting;
        if (!level.isClientSide)
            sendData();
    }

    // the cloth is cut off and rolled up as a tapestry, the chain stays for the next one
    private void finishPiece() {
        pick = 0;
        t = 0;
        rowPaid = false;
        if (level.isClientSide)
            return;
        ItemStack piece = new ItemStack(Jacquard.TAPESTRY.get());
        piece.set(Jacquard.DESIGN.get(), getDesignId());
        output.insertItem(0, piece, false);
        sendData();
    }

    public ItemInteractionResult useItem(Player player, ItemStack held) {
        if (chain.isItemValid(0, held)) {
            if (!level.isClientSide) {
                ItemStack old = chain.getStackInSlot(0);
                chain.setStackInSlot(0, held.consumeAndReturn(1, player));
                if (!old.isEmpty())
                    player.getInventory().placeItemBackInInventory(old);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (woolColour(held) >= 0) {
            ItemStack left = addThread(held, true);
            if (left.getCount() == held.getCount())
                return ItemInteractionResult.CONSUME;
            if (!level.isClientSide) {
                left = addThread(held, false);
                if (!player.hasInfiniteMaterials())
                    held.setCount(left.getCount());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public InteractionResult useEmptyHand(Player player) {
        ItemStackHandler from = player.isSecondaryUseActive() ? chain : output;
        if (from.getStackInSlot(0).isEmpty())
            return InteractionResult.PASS;
        if (!level.isClientSide)
            player.getInventory().placeItemBackInInventory(from.extractItem(0, 64, false));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // whole wool comes back out, part used thread is lost
    @Override
    public void destroy() {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, chain);
        ItemHelper.dropContents(level, worldPosition, output);
        for (int c = 0; c < 16; c++)
            for (int count = thread[c] / JacquardWork.THREAD_PER_WOOL; count > 0; count -= 64)
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), new ItemStack(wool(c), Math.min(64, count)));
    }

    // dev showcase only: puts the loom part way through the design so screenshots always have
    // woven cloth in them
    public void showcaseJump(double fraction) {
        pick = (int) Math.floor(picks().total() * fraction);
        t = 0;
        rowPaid = false;
        sendData();
    }

    // A multi-colour row can't be woven in one pick: each row takes one pick per colour in it,
    // the weft of that colour floating on the face wherever the row has it. Rows are woven from
    // the design's bottom up, colours in dye order, and there's one card per pick.
    public record Picks(int[] rowStart, int[] row, int[] weft, int[] palette) {
        public int total() {
            return weft.length;
        }

        static Picks of(Design d) {
            int Wp = d.pixelWidth(), Hp = d.pixelHeight();
            int[] rowStart = new int[Hp + 1];
            List<Integer> rows = new ArrayList<>(), wefts = new ArrayList<>();
            boolean[] used = new boolean[16];
            for (int w = 0; w < Hp; w++) {
                rowStart[w] = wefts.size();
                boolean[] here = new boolean[16];
                for (int x = 0; x < Wp; x++)
                    here[d.dye(x, Hp - 1 - w)] = true;
                for (int c = 0; c < 16; c++)
                    if (here[c]) {
                        rows.add(w);
                        wefts.add(c);
                        used[c] = true;
                    }
            }
            rowStart[Hp] = wefts.size();
            return new Picks(rowStart, rows.stream().mapToInt(Integer::intValue).toArray(), wefts.stream().mapToInt(Integer::intValue).toArray(),
                    IntStream.range(0, 16).filter(c -> used[c]).toArray());
        }
    }

    @Nullable
    private Design picksFor;
    @Nullable
    private Picks picks;

    public Picks picks() {
        Design design = shownDesign();
        if (picks == null || picksFor != design) {
            picks = Picks.of(design);
            picksFor = design;
        }
        return picks;
    }

    // row is the woven row (0 = the design's bottom), weft the dye this pick throws, lastWeft the
    // one before it, for the drop box to move from
    public record Phase(double shed, double beater, double R, double sx, int dir, double sp, boolean flying, double cardS, int row,
                        int weft, int lastWeft, double boxShift, double spin) {}

    private static double ease(double t) {
        return t * t * (3 - 2 * t);
    }

    // loomPhase() on the real pick, moved on by partialTick while weaving. Changed from the
    // preview: it steps per pick instead of per row, a row lands on the cloth when its last pick
    // is beaten in, and the shuttle travels to +-19.5 so it parks in a shuttle box. With no chain
    // the loom rests with nothing on it.
    public Phase phase(float partialTick) {
        Picks picks = picks();
        boolean idle = !hasDesign();
        int p = Math.min(pick, picks.total() - 1);
        int row = picks.row()[p];
        boolean gate = picks.rowStart()[row] == p && !rowPaid;
        double t = this.t;
        if (weaving && !waiting && !idle)
            t = Math.min(t + Math.abs(speed) / JacquardWork.PICK_RPM / 20 * partialTick, gate ? GATE : 0.9999);
        double spin = this.spin + Math.abs(speed) / 60 * Math.PI * 2 / 20 * partialTick;
        if (idle)
            t = 0;
        boolean lastOfRow = p == picks.rowStart()[row + 1] - 1;
        double shed = t < 0.2 ? ease(t / 0.2) : t < 0.75 ? 1 : t < 0.95 ? 1 - ease((t - 0.75) / 0.2) : 0;
        double back = -0.08, beat = 0.24;
        double beater = t < 0.55 ? back : t < 0.68 ? Mth.lerp(ease((t - 0.55) / 0.13), back, beat)
                : t < 0.85 ? Mth.lerp(ease((t - 0.68) / 0.17), beat, back) : back;
        double R = idle ? 0 : row + (lastOfRow ? Mth.clamp((t - 0.55) / 0.13, 0, 1) : 0);
        int dir = p % 2 == 0 ? 1 : -1;
        double sp = t < 0.2 ? 0 : t < 0.55 ? ease((t - 0.2) / 0.35) : 1;
        double sx = dir > 0 ? Mth.lerp(sp, -19.5, 19.5) : Mth.lerp(sp, 19.5, -19.5);
        double cardStep = ease(Mth.clamp((t - 0.85) / 0.15, 0, 1));
        double boxShift = ease(Mth.clamp(t / 0.2, 0, 1));
        return new Phase(shed, beater, R, sx, dir, sp, t >= 0.2 && t < 0.55, p + cardStep, row,
                picks.weft()[p], picks.weft()[Math.max(0, p - 1)], boxShift, spin);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        lang().translate("loom.goggles.title").forGoggles(tooltip);
        Design design = design();
        if (design == null) {
            lang().translate("loom.goggles.no_chain").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return true;
        }
        Picks picks = picks();
        int row = picks.row()[Math.min(pick, picks.total() - 1)];
        lang().translate("loom.goggles.design", design.width(), design.height()).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        lang().translate("loom.goggles.row", row + 1, design.pixelHeight()).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        lang().translate("loom.goggles.speed", String.format("%.1f", Math.abs(speed) / JacquardWork.PICK_RPM))
                .style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        int[] needs = rowNeeds(design, row);
        List<Component> missing = new ArrayList<>();
        for (int c : picks.palette()) {
            Component name = Component.translatable("color.minecraft." + DyeColor.byId(c).getName());
            lang().translate("loom.goggles.thread", name, String.format("%.1f", thread[c] / (float) JacquardWork.THREAD_PER_WOOL))
                    .style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 2);
            if (thread[c] == 0 || !rowPaid && pick == picks.rowStart()[row] && thread[c] < needs[c])
                missing.add(name);
        }
        for (Component name : missing)
            lang().translate("loom.goggles.missing", name).style(ChatFormatting.RED).forGoggles(tooltip, 1);
        if (pick == 0 && t == 0 && !outputRoom(getDesignId()))
            lang().translate("loom.goggles.output_full").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        return true;
    }

    private static LangBuilder lang() {
        return new LangBuilder(Tracer.MODID);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Chain", chain.serializeNBT(registries));
        tag.put("Output", output.serializeNBT(registries));
        tag.putIntArray("Thread", thread);
        tag.putFloat("Speed", speed);
        tag.putInt("Piece", pieceDesign);
        tag.putInt("Pick", pick);
        tag.putDouble("T", t);
        tag.putBoolean("RowPaid", rowPaid);
        tag.putBoolean("Weaving", weaving);
        tag.putBoolean("Waiting", waiting);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        chain.deserializeNBT(registries, tag.getCompound("Chain"));
        output.deserializeNBT(registries, tag.getCompound("Output"));
        int[] saved = tag.getIntArray("Thread");
        for (int c = 0; c < 16; c++)
            thread[c] = c < saved.length ? saved[c] : 0;
        speed = tag.getFloat("Speed");
        pieceDesign = tag.contains("Piece") ? tag.getInt("Piece") : -1;
        pick = tag.getInt("Pick");
        t = tag.getDouble("T");
        rowPaid = tag.getBoolean("RowPaid");
        weaving = tag.getBoolean("Weaving");
        waiting = tag.getBoolean("Waiting");
    }
}
