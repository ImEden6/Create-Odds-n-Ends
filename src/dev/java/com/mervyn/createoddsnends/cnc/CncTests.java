package com.mervyn.createoddsnends.cnc;

import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FORMED;

import java.util.ArrayList;
import java.util.List;

import com.mervyn.createoddsnends.Tracer;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

// Dev only, ODDSNENDS_CNCTEST=true. Builds a row of mills away from the showcase, pokes at them and
// logs one CNCTEST line per check.
@EventBusSubscriber(modid = Tracer.MODID)
public final class CncTests {
    static final boolean ENABLED = "true".equals(System.getenv("ODDSNENDS_CNCTEST"));
    private static final int START = 100;

    private static int tick;
    private static BlockPos base;
    private static final List<String> failures = new ArrayList<>();
    private static float savedProgress;

    private CncTests() {}

    @SubscribeEvent
    static void tick(ServerTickEvent.Post event) {
        if (!ENABLED)
            return;
        MinecraftServer server = event.getServer();
        if (server.getPlayerList().getPlayers().isEmpty())
            return;
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        ServerLevel level = player.serverLevel();
        tick++;
        if (tick == START) {
            base = player.blockPosition().relative(Direction.SOUTH, 40).above();
            setup(player, level);
        }
        if (base == null)
            return;
        if (tick == START + 40)
            midway(level);
        if (tick == START + 41)
            afterReload(level);
        if (tick == START + 500)
            finish(level);
    }

    private static BlockPos site(int i) {
        return base.relative(Direction.EAST, i * 9);
    }

    private static CncBlockEntity cnc(ServerLevel level, int i) {
        return (CncBlockEntity) level.getBlockEntity(site(i));
    }

    private static void build(ServerPlayer player, int i, Direction facing, int rpm) {
        CncStructure.showcase(player, site(i), facing);
        CncBlockEntity be = cnc(player.serverLevel(), i);
        for (int s = 0; s < CncBlockEntity.SLOTS; s++)
            be.input.setStackInSlot(s, ItemStack.EMPTY);
        if (player.serverLevel().getBlockEntity(CncStructure.drive(site(i), facing).relative(facing.getClockWise())) instanceof CreativeMotorBlockEntity motor)
            motor.generatedSpeed.setValue(rpm);
    }

    private static void check(String name, boolean ok, String detail) {
        Tracer.LOGGER.info("CNCTEST {} {} {}", ok ? "PASS" : "FAIL", name, detail);
        if (!ok)
            failures.add(name);
    }

    private static void setup(ServerPlayer player, ServerLevel level) {
        ItemStackHandler mix = new ItemStackHandler(CncBlockEntity.SLOTS);
        mix.setStackInSlot(0, new ItemStack(AllItems.ANDESITE_ALLOY.get()));
        mix.setStackInSlot(1, new ItemStack(AllItems.IRON_SHEET.get(), 4));
        CncJob picked = CncJob.find(level, mix);
        check("most_specific_wins", picked != null && picked.id().getPath().equals("cnc/propeller"), String.valueOf(picked == null ? null : picked.id()));
        mix.setStackInSlot(1, new ItemStack(AllItems.IRON_SHEET.get(), 3));
        picked = CncJob.find(level, mix);
        check("subset_still_found", picked != null && picked.id().getPath().equals("cnc/whisk"), String.valueOf(picked == null ? null : picked.id()));

        // 0: a whole precision mechanism assembly in one go
        build(player, 0, Direction.NORTH, 256);
        CncBlockEntity seq = cnc(level, 0);
        seq.input.setStackInSlot(0, new ItemStack(AllItems.GOLDEN_SHEET.get()));
        seq.input.setStackInSlot(1, new ItemStack(AllBlocks.COGWHEEL.get(), 5));
        seq.input.setStackInSlot(2, new ItemStack(AllBlocks.LARGE_COGWHEEL.get(), 5));
        seq.input.setStackInSlot(3, new ItemStack(Items.IRON_NUGGET, 5));

        // 1: fed and emptied through the item capability on two different frames, like funnels would
        build(player, 1, Direction.NORTH, 256);
        IItemHandler side = level.getCapability(Capabilities.ItemHandler.BLOCK, CncStructure.at(site(1), Direction.NORTH, 3, 1, 1), Direction.EAST);
        check("io_slots", side != null && side.getSlots() == CncBlockEntity.SLOTS * 2, String.valueOf(side == null ? null : side.getSlots()));
        if (side != null) {
            side.insertItem(0, new ItemStack(Items.COPPER_BLOCK), false);
            side.insertItem(1, new ItemStack(Items.IRON_INGOT, 2), false);
            check("io_no_insert_output", !side.insertItem(CncBlockEntity.SLOTS, new ItemStack(Items.DIRT), true).isEmpty(), "");
        }

        // 2: saved and loaded mid job, should carry on rather than start over
        build(player, 2, Direction.NORTH, 32);
        CncBlockEntity save = cnc(level, 2);
        save.input.setStackInSlot(0, new ItemStack(Items.COPPER_BLOCK));
        save.input.setStackInSlot(1, new ItemStack(Items.IRON_INGOT, 2));

        // 3: break a frame, everything unforms; put it back and wrench, it forms again
        build(player, 3, Direction.NORTH, 32);
        BlockPos frame = CncStructure.at(site(3), Direction.NORTH, 2, 0, 2);
        level.setBlock(frame, Blocks.AIR.defaultBlockState(), 3);
        boolean pendantOff = !level.getBlockState(site(3)).getValue(FORMED);
        boolean framesOff = !level.getBlockState(CncStructure.at(site(3), Direction.NORTH, 0, 2, 0)).getValue(FORMED);
        check("break_unforms", pendantOff && framesOff, "pendant " + pendantOff + " frames " + framesOff);
        level.setBlock(frame, Cnc.CNC_FRAME.get().defaultBlockState(), 3);
        CncStructure.wrench(level, site(3), player);
        check("reform", level.getBlockState(site(3)).getValue(FORMED), "");
        BlockPos drive = CncStructure.drive(site(3), Direction.NORTH);
        level.setBlock(drive, Cnc.CNC_FRAME.get().defaultBlockState(), 3);
        boolean brokeOnDrive = !level.getBlockState(site(3)).getValue(FORMED);
        CncStructure.wrench(level, site(3), player);
        check("drive_required", brokeOnDrive && !level.getBlockState(site(3)).getValue(FORMED), "");
        level.setBlock(drive, Cnc.CNC_DRIVE.get().defaultBlockState(), 3);
        CncStructure.wrench(level, site(3), player);
        check("reform_with_drive", level.getBlockState(site(3)).getValue(FORMED), "");

        // 4..7: every facing, every frame finds its pendant
        Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (int f = 0; f < 4; f++) {
            build(player, 4 + f, facings[f], 32);
            int found = 0;
            for (int a = 0; a < CncStructure.WIDE; a++)
                for (int u = 0; u < CncStructure.HIGH; u++)
                    for (int b = 0; b < CncStructure.DEEP; b++) {
                        BlockPos pos = CncStructure.at(site(4 + f), facings[f], a, u, b);
                        if (site(4 + f).equals(CncStructure.findPendant(level, pos, level.getBlockState(pos))))
                            found++;
                    }
            check("facing_" + facings[f], found == 36, found + "/36");
        }

        // 8: output full, nothing should run or get eaten
        build(player, 8, Direction.NORTH, 256);
        CncBlockEntity full = cnc(level, 8);
        for (int s = 0; s < CncBlockEntity.SLOTS; s++)
            full.output.setStackInSlot(s, new ItemStack(Items.DIRT, 64));
        full.input.setStackInSlot(0, new ItemStack(Items.COPPER_BLOCK));
        full.input.setStackInSlot(1, new ItemStack(Items.IRON_INGOT, 2));

        // 9: junk that matches nothing just sits there
        build(player, 9, Direction.NORTH, 256);
        cnc(level, 9).input.setStackInSlot(0, new ItemStack(Items.DIRT, 3));

        for (String id : List.of("cnc_frame", "cnc_pendant"))
            check("crafting_" + id, level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Tracer.MODID, id)).isPresent(), "");
    }

    private static void midway(ServerLevel level) {
        CncBlockEntity save = cnc(level, 2);
        savedProgress = save.progress(0);
        CompoundTag tag = save.saveWithFullMetadata(level.registryAccess());
        level.removeBlockEntity(site(2));
        level.setBlockEntity(BlockEntity.loadStatic(site(2), level.getBlockState(site(2)), tag, level.registryAccess()));
        check("save_running_before", save.isRunning() && savedProgress > 0, "progress " + savedProgress);
    }

    private static void afterReload(ServerLevel level) {
        CncBlockEntity save = cnc(level, 2);
        check("save_kept_progress", save != null && save.progress(0) >= savedProgress, savedProgress + " -> " + (save == null ? null : save.progress(0) + " speed " + save.speed()));
    }

    private static void finish(ServerLevel level) {
        CncBlockEntity seq = cnc(level, 0);
        int outputs = 0;
        StringBuilder made = new StringBuilder();
        for (int s = 0; s < CncBlockEntity.SLOTS; s++) {
            ItemStack out = seq.output.getStackInSlot(s);
            outputs += out.getCount();
            if (!out.isEmpty())
                made.append(out).append(' ');
        }
        boolean inputGone = seq.input.getStackInSlot(0).isEmpty() && seq.input.getStackInSlot(1).isEmpty()
                && seq.input.getStackInSlot(2).isEmpty() && seq.input.getStackInSlot(3).isEmpty();
        check("sequenced_one_result", outputs == 1, made.toString());
        check("sequenced_ate_inputs", inputGone, "");

        IItemHandler side = level.getCapability(Capabilities.ItemHandler.BLOCK, CncStructure.at(site(1), Direction.NORTH, 0, 0, 2), Direction.WEST);
        ItemStack pulled = ItemStack.EMPTY;
        if (side != null)
            for (int s = CncBlockEntity.SLOTS; s < CncBlockEntity.SLOTS * 2 && pulled.isEmpty(); s++)
                pulled = side.extractItem(s, 64, false);
        check("io_extract", pulled.is(AllItems.PRECISION_MECHANISM.get()), pulled.toString());
        check("io_no_extract_input", side != null && side.extractItem(0, 64, true).isEmpty(), "");

        check("save_finished", cnc(level, 2).input.getStackInSlot(0).isEmpty(), cnc(level, 2).input.getStackInSlot(0).toString());

        CncBlockEntity full = cnc(level, 8);
        check("full_output_waits", !full.isRunning() && full.input.getStackInSlot(0).getCount() == 1, "running " + full.isRunning());

        CncBlockEntity junk = cnc(level, 9);
        check("junk_idle", !junk.isRunning() && junk.input.getStackInSlot(0).getCount() == 3, "");

        Tracer.LOGGER.info("CNCTEST DONE {} failures {}", failures.size(), failures);
    }

    public static void showcaseDoors(ServerLevel level, BlockPos pendant) {
        if (level.getBlockEntity(pendant) instanceof CncBlockEntity be)
            be.openDoors();
    }
}
