package com.mervyn.coolcreatestuff.jacquard;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

// Controller of a formed loom. The drive hands it the network speed; for now it only animates.
public class JacquardHeadBlockEntity extends SmartBlockEntity {
    public static final Design DEBUG_DESIGN = DesignGen.generate(3, 2, 3, 5, "kilim");

    private float speed;
    private int designId = -1;

    // Placeholder: a client-only loop weaving the shown design over and over, standing in for
    // real progress later. Fields mirror the preview's `loom` state object.
    private double k, t, hold, spin;

    public JacquardHeadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition).inflate(3);
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
        return designId;
    }

    public void setDesignId(int designId) {
        this.designId = designId;
        k = t = hold = 0;
        sendData();
    }

    public Design shownDesign() {
        Design design = designId < 0 ? null : DesignSync.client(designId);
        return design != null ? design : DEBUG_DESIGN;
    }

    // updateLoom(), one tick at a time. Counts picks rather than rows: see Picks.
    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide || !getBlockState().getValue(LoomPartBlock.FORMED))
            return;
        double dt = 1 / 20.0;
        int total = picks().total();
        spin += Math.abs(speed) / 60 * Math.PI * 2 * dt;
        if (k >= total) {
            hold += dt;
            if (hold > 2.5)
                k = t = hold = 0;
            return;
        }
        t += dt * Math.abs(speed) / 32;
        while (t >= 1) {
            t -= 1;
            k++;
            if (k >= total) {
                k = total;
                t = 0;
                break;
            }
        }
    }

    // dev showcase only: puts the client loop part way through the design so screenshots always
    // have woven cloth in them
    public void showcaseJump(double fraction) {
        k = Math.floor(picks().total() * fraction);
        t = hold = 0;
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

    // loomPhase(), with the loop advanced by partialTick so it moves smoothly between ticks.
    // Changed from the preview: it steps per pick instead of per row, a row lands on the cloth when
    // its last pick is beaten in, and the shuttle travels to +-19.5 so it parks in a shuttle box.
    public Phase phase(float partialTick) {
        Picks picks = picks();
        int total = picks.total(), Hp = shownDesign().pixelHeight();
        double rate = Math.abs(speed) / 32 / 20;
        double k = this.k, t = this.t + (this.k >= total ? 0 : rate * partialTick);
        if (t >= 1 && k < total) {
            t -= 1;
            k++;
        }
        double spin = this.spin + Math.abs(speed) / 60 * Math.PI * 2 / 20 * partialTick;
        boolean done = k >= total;
        if (done)
            t = 0;
        int p = (int) Math.min(k, total - 1);
        int row = picks.row()[p];
        boolean lastOfRow = p == picks.rowStart()[row + 1] - 1;
        double shed = done ? 0 : t < 0.2 ? ease(t / 0.2) : t < 0.75 ? 1 : t < 0.95 ? 1 - ease((t - 0.75) / 0.2) : 0;
        double back = -0.08, beat = 0.24;
        double beater = done ? back : t < 0.55 ? back : t < 0.68 ? Mth.lerp(ease((t - 0.55) / 0.13), back, beat)
                : t < 0.85 ? Mth.lerp(ease((t - 0.68) / 0.17), beat, back) : back;
        double R = done ? Hp : row + (lastOfRow ? Mth.clamp((t - 0.55) / 0.13, 0, 1) : 0);
        int dir = p % 2 == 0 ? 1 : -1;
        double sp = done ? 1 : t < 0.2 ? 0 : t < 0.55 ? ease((t - 0.2) / 0.35) : 1;
        double sx = dir > 0 ? Mth.lerp(sp, -19.5, 19.5) : Mth.lerp(sp, 19.5, -19.5);
        double cardStep = done ? 0 : ease(Mth.clamp((t - 0.85) / 0.15, 0, 1));
        double boxShift = done ? 1 : ease(Mth.clamp(t / 0.2, 0, 1));
        return new Phase(shed, beater, R, sx, dir, sp, !done && t >= 0.2 && t < 0.55, k + cardStep, row,
                picks.weft()[p], picks.weft()[Math.max(0, p - 1)], boxShift, spin);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("Speed", speed);
        tag.putInt("Design", designId);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        speed = tag.getFloat("Speed");
        int design = tag.contains("Design") ? tag.getInt("Design") : -1;
        if (design != designId)
            k = t = hold = 0;
        designId = design;
    }
}
