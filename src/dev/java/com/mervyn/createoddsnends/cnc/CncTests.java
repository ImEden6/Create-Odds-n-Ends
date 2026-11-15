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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.Block;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
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

    private static BlockPos showcaseOrigin() {
        return base.relative(Direction.SOUTH, 20).below();
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
        mix.setStackInSlot(0, new ItemStack(Items.COPPER_BLOCK));
        mix.setStackInSlot(1, new ItemStack(Items.IRON_INGOT, 2));
        CncJob picked = CncJob.find(level, mix);
        check("most_specific_wins", picked != null && picked.id().getPath().equals("cnc_test"), String.valueOf(picked == null ? null : picked.id()));
        mix.setStackInSlot(1, new ItemStack(Items.IRON_INGOT));
        picked = CncJob.find(level, mix);
        check("subset_still_found", picked != null && picked.id().getPath().equals("cnc_test_subset"), String.valueOf(picked == null ? null : picked.id()));

        ItemStackHandler scrape = new ItemStackHandler(CncBlockEntity.SLOTS);
        scrape.setStackInSlot(0, new ItemStack(Items.EXPOSED_COPPER));
        picked = CncJob.find(level, scrape);
        check("deploying_tool_required", picked == null, String.valueOf(picked == null ? null : picked.id()));
        scrape.setStackInSlot(1, new ItemStack(Items.IRON_AXE));
        picked = CncJob.find(level, scrape);
        boolean took = picked != null && picked.take(scrape, false);
        check("deploying_tool_kept", took && scrape.getStackInSlot(0).isEmpty() && scrape.getStackInSlot(1).is(Items.IRON_AXE),
                (picked == null ? null : picked.id()) + " " + scrape.getStackInSlot(0) + " " + scrape.getStackInSlot(1));
        ItemStackHandler cog = new ItemStackHandler(CncBlockEntity.SLOTS);
        cog.setStackInSlot(0, new ItemStack(AllBlocks.SHAFT.get()));
        cog.setStackInSlot(1, new ItemStack(Items.OAK_PLANKS));
        picked = CncJob.find(level, cog);
        check("deploying_consumed", picked != null && picked.take(cog, false) && cog.getStackInSlot(1).isEmpty(), String.valueOf(picked == null ? null : picked.id()));

        // 0: a precision mechanism in one go, charged for a single loop of parts
        build(player, 0, Direction.NORTH, 256);
        CncBlockEntity seq = cnc(level, 0);
        seq.input.setStackInSlot(0, new ItemStack(AllItems.GOLDEN_SHEET.get()));
        seq.input.setStackInSlot(1, new ItemStack(AllBlocks.COGWHEEL.get()));
        seq.input.setStackInSlot(2, new ItemStack(AllBlocks.LARGE_COGWHEEL.get()));
        seq.input.setStackInSlot(3, new ItemStack(Items.IRON_NUGGET));

        // 1: fed through the roof hatch and emptied through the hatch on the pendant's end, nowhere else
        build(player, 1, Direction.NORTH, 256);
        IItemHandler roof = level.getCapability(Capabilities.ItemHandler.BLOCK, CncStructure.at(site(1), Direction.NORTH, 1, 2, 1), Direction.UP);
        IItemHandler outHatch = level.getCapability(Capabilities.ItemHandler.BLOCK, CncStructure.at(site(1), Direction.NORTH, 0, 0, 1), Direction.WEST);
        IItemHandler wall = level.getCapability(Capabilities.ItemHandler.BLOCK, CncStructure.at(site(1), Direction.NORTH, 3, 1, 1), Direction.EAST);
        IItemHandler roofSide = level.getCapability(Capabilities.ItemHandler.BLOCK, CncStructure.at(site(1), Direction.NORTH, 1, 2, 1), Direction.NORTH);
        check("io_hatches_only", roof != null && outHatch != null && wall == null && roofSide == null, roof + " " + outHatch + " " + wall + " " + roofSide);
        if (roof != null) {
            roof.insertItem(0, new ItemStack(Items.COPPER_BLOCK), false);
            roof.insertItem(1, new ItemStack(Items.IRON_INGOT, 2), false);
            check("io_rejects_junk", roof.insertItem(0, new ItemStack(Items.DIRT), true).getCount() == 1, "");
            check("io_roof_no_extract", roof.extractItem(0, 64, true).isEmpty(), "");
        }
        if (outHatch != null)
            check("io_no_insert_output", outHatch.insertItem(0, new ItemStack(Items.COPPER_BLOCK), true).getCount() == 1, "");

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

        // 11: doors opened by a player, the interlock keeps it from cutting
        build(player, 11, Direction.NORTH, 256);
        cnc(level, 11).input.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 4));
        cnc(level, 11).setDoors(true);

        // 12: by hand. Shut, an item only opens the doors; open, it goes in; an empty hand takes it
        // back out before anything is cut, then closes up once there's nothing left.
        build(player, 12, Direction.NORTH, 0);
        CncBlockEntity hand = cnc(level, 12);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBBLESTONE, 5));
        hand.useItem(player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        check("hand_shut_opens_only", hand.input.getStackInSlot(0).isEmpty() && player.getMainHandItem().getCount() == 5, hand.input.getStackInSlot(0).toString());
        hand.useItem(player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        check("hand_open_inserts", hand.input.getStackInSlot(0).getCount() == 5 && player.getMainHandItem().isEmpty(), hand.input.getStackInSlot(0).toString());
        hand.useEmptyHand(player);
        check("hand_takes_input_back", hand.input.getStackInSlot(0).isEmpty() && player.getInventory().countItem(Items.COBBLESTONE) >= 5, "");
        player.getInventory().clearOrCountMatchingItems(stack -> stack.is(Items.COBBLESTONE), -1, player.inventoryMenu.getCraftSlots());
        hand.useEmptyHand(player);
        boolean closed = !hand.getUpdateTag(level.registryAccess()).getBoolean("DoorsOpen");
        check("hand_empty_closes", closed, "");
        hand.pressDoorButton();
        check("pendant_opens", hand.getUpdateTag(level.registryAccess()).getBoolean("DoorsOpen"), "");
        hand.pressDoorButton();
        check("pendant_closes", !hand.getUpdateTag(level.registryAccess()).getBoolean("DoorsOpen"), "");

        // the open doors take clicks: aim at the middle of each, and just over the top of one
        for (int door = 0; door < 2; door++) {
            float hinge = door == 0 ? 72 : 24, toward = door == 0 ? -1 : 1;
            float angle = net.minecraft.util.Mth.DEG_TO_RAD * (door == 0 ? -CncDoors.OPEN_DEGREES : CncDoors.OPEN_DEGREES);
            float dx = toward * net.minecraft.util.Mth.cos(angle), dz = -toward * net.minecraft.util.Mth.sin(angle);
            org.joml.Matrix4f toWorld = new org.joml.Matrix4f().translate(site(12).getX() + .5f, site(12).getY() + .5f, site(12).getZ() + .5f)
                    .rotateY(net.minecraft.util.Mth.DEG_TO_RAD * (-Direction.NORTH.toYRot() + 180)).translate(-.5f, -1.5f, -.5f).scale(1 / 16f);
            org.joml.Vector3f mid = toWorld.transformPosition(new org.joml.Vector3f(hinge + 12 * dx, 25, 2 + 12 * dz));
            org.joml.Vector3f normal = toWorld.transformDirection(new org.joml.Vector3f(dz, 0, -dx)).normalize();
            net.minecraft.world.phys.Vec3 target = new net.minecraft.world.phys.Vec3(mid.x, mid.y, mid.z);
            net.minecraft.world.phys.Vec3 eye = target.add(normal.x * 2, normal.y * 2, normal.z * 2);
            net.minecraft.world.phys.Vec3 look = target.subtract(eye).normalize();
            check("door_click_" + door, CncDoors.crossesDoor(site(12), Direction.NORTH, eye, look, 10), eye.toString());
            net.minecraft.world.phys.Vec3 over = target.add(0, 1.5, 0).subtract(eye).normalize();
            check("door_miss_" + door, !CncDoors.crossesDoor(site(12), Direction.NORTH, eye, over, 10), "");
        }

        CncShowcase.all(player, showcaseOrigin(), Direction.SOUTH);

        // 10: automation. A hopper on the roof feeds it, and a mechanical arm point on a frame
        // finds the mill and moves items both ways. No rotation, so nothing gets used up.
        build(player, 10, Direction.NORTH, 0);
        BlockPos hopperPos = CncStructure.at(site(10), Direction.NORTH, 1, 2, 1).above();
        level.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN), Block.UPDATE_ALL);
        if (level.getBlockEntity(hopperPos) instanceof HopperBlockEntity hopper)
            hopper.setItem(0, new ItemStack(Items.COPPER_BLOCK, 2));
        build(player, 13, Direction.NORTH, 0);
        BlockPos face = CncStructure.at(site(13), Direction.NORTH, 1, 1, 0);
        ArmInteractionPoint front = ArmInteractionPoint.create(level, face, level.getBlockState(face));
        check("arm_front_point", front instanceof CncArmPoint, String.valueOf(front));
        if (front != null) {
            ItemStack left = front.insert(null, new ItemStack(Items.COPPER_BLOCK), false);
            check("arm_front_inserts", left.isEmpty(), left.toString());
            check("arm_front_opens_doors", cnc(level, 13).getUpdateTag(level.registryAccess()).getBoolean("ArmDoors"), "");
            check("arm_front_saves_shut", !cnc(level, 13).saveWithoutMetadata(level.registryAccess()).getBoolean("DoorsOpen"), "");
        }
        check("funnel_not_on_front", level.getCapability(Capabilities.ItemHandler.BLOCK, face, Direction.NORTH) == null, "");
        BlockPos side = CncStructure.at(site(10), Direction.NORTH, 2, 1, 2);
        check("arm_not_on_walls", ArmInteractionPoint.create(level, side, level.getBlockState(side)) == null, "");
        BlockPos top = CncStructure.at(site(10), Direction.NORTH, 2, 2, 1);
        BlockPos end = CncStructure.at(site(10), Direction.NORTH, 0, 0, 1);
        ArmInteractionPoint in = ArmInteractionPoint.create(level, top, level.getBlockState(top));
        ArmInteractionPoint out = ArmInteractionPoint.create(level, end, level.getBlockState(end));
        check("arm_point_type", in instanceof CncArmPoint && out instanceof CncArmPoint, in + " " + out);
        if (in != null && out != null) {
            ItemStack left = in.insert(null, new ItemStack(Items.COBBLESTONE, 3), false);
            check("arm_inserts", left.isEmpty() && cnc(level, 10).input.getStackInSlot(0).is(Items.COBBLESTONE), left.toString());
            cnc(level, 10).output.setStackInSlot(0, new ItemStack(Items.STONE_STAIRS, 2));
            ItemStack pulled = out.extract(null, 0, false);
            check("arm_extracts", pulled.is(Items.STONE_STAIRS) && pulled.getCount() == 2, pulled.toString());
            check("arm_no_insert_output", out.insert(null, new ItemStack(Items.COBBLESTONE), true).getCount() == 1, "");
        }

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

        IItemHandler outHatch = level.getCapability(Capabilities.ItemHandler.BLOCK, CncStructure.at(site(1), Direction.NORTH, 0, 0, 1), Direction.WEST);
        ItemStack pulled = ItemStack.EMPTY;
        if (outHatch != null)
            for (int s = 0; s < CncBlockEntity.SLOTS && pulled.isEmpty(); s++)
                pulled = outHatch.extractItem(s, 64, false);
        check("io_extract", pulled.is(AllItems.PRECISION_MECHANISM.get()), pulled.toString());

        check("save_finished", cnc(level, 2).input.getStackInSlot(0).isEmpty(), cnc(level, 2).input.getStackInSlot(0).toString());

        CncBlockEntity full = cnc(level, 8);
        check("full_output_waits", !full.isRunning() && full.input.getStackInSlot(0).getCount() == 1, "running " + full.isRunning());

        int dirt = 0;
        for (int i = 0; i < CncBlockEntity.SLOTS; i++)
            if (cnc(level, 10).input.getStackInSlot(i).is(Items.COPPER_BLOCK))
                dirt += cnc(level, 10).input.getStackInSlot(i).getCount();
        check("hopper_feeds", dirt == 2, String.valueOf(dirt));
        for (int i = 0; i < CncShowcase.VARIANTS.size(); i++) {
            BlockPos pendant = showcaseOrigin().relative(Direction.SOUTH, 7).above().relative(Direction.NORTH.getCounterClockWise(), i * 11);
            int cogs = 0;
            for (BlockPos pos : BlockPos.betweenClosed(pendant.offset(-9, -3, -9), pendant.offset(9, 4, 9))) {
                if (pos.equals(pendant))
                    continue;
                IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
                if (items != null)
                    for (int slot = 0; slot < items.getSlots(); slot++)
                        if (items.getStackInSlot(slot).is(AllBlocks.COGWHEEL.get().asItem()))
                            cogs += items.getStackInSlot(slot).getCount();
            }
            check("showcase_" + CncShowcase.VARIANTS.get(i), cogs > 0, cogs + " cogwheels out, depots " + java.util.stream.Stream.of(net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.WEST).map(d -> { BlockPos dp = CncStructure.at(pendant, Direction.NORTH, 1, 0, 0).relative(Direction.NORTH, 3).relative(d); IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, dp, null); return level.getBlockState(dp).getBlock() + "=" + (h == null ? "nocap" : h.getStackInSlot(0).toString()); }).toList() + ", loose " + level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pendant).inflate(9)).stream().map(e -> e.getItem() + "@" + e.blockPosition().subtract(pendant)).toList() + ", mill " + (level.getBlockEntity(pendant) instanceof CncBlockEntity m ? m.getUpdateTag(level.registryAccess()).toString() : "missing") + " arm " + (level.getBlockEntity(CncStructure.at(pendant, Direction.NORTH, 1, 0, 0).relative(Direction.NORTH, 3)) instanceof com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity arm ? arm.saveWithoutMetadata(level.registryAccess()).toString() : "none"));
        }

        CncBlockEntity shut = cnc(level, 11);
        boolean madeNothing = true;
        for (int i = 0; i < CncBlockEntity.SLOTS; i++)
            madeNothing &= shut.output.getStackInSlot(i).isEmpty();
        check("doors_pause", madeNothing && !shut.isRunning(), "running " + shut.isRunning());

        CncBlockEntity junk = cnc(level, 9);
        check("junk_idle", !junk.isRunning() && junk.input.getStackInSlot(0).getCount() == 3, "");

        Tracer.LOGGER.info("CNCTEST DONE {} failures {}", failures.size(), failures);
    }

    public static BlockPos showcaseCell(BlockPos pendant, Direction facing, int along, int up, int back) {
        return CncStructure.at(pendant, facing, along, up, back);
    }

    public static void showcaseDoors(ServerLevel level, BlockPos pendant) {
        if (level.getBlockEntity(pendant) instanceof CncBlockEntity be)
            be.setDoors(true);
    }
}
