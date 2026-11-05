package com.mervyn.createoddsnends.cnc;

import java.util.List;

import com.mervyn.createoddsnends.Tracer;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public final class CncShowcase {
    public static final List<String> VARIANTS = List.of("hopper", "chute", "funnel", "arm", "arm_front");

    private CncShowcase() {}

    public static void all(ServerPlayer player, BlockPos origin, Direction ahead) {
        Direction facing = ahead.getOpposite();
        BlockPos pendant = origin.relative(ahead, 7).above();
        for (int i = 0; i < VARIANTS.size(); i++)
            build(player, pendant.relative(facing.getCounterClockWise(), i * 11), facing, VARIANTS.get(i));
    }

    static void build(ServerPlayer player, BlockPos pendant, Direction facing, String variant) {
        ServerLevel level = player.serverLevel();
        CncStructure.showcase(player, pendant, facing);
        if (level.getBlockEntity(pendant) instanceof CncBlockEntity be)
            be.input.setStackInSlot(0, ItemStack.EMPTY);
        BlockPos roof = CncStructure.at(pendant, facing, 1, 2, 1).above();
        BlockPos outHatch = CncStructure.at(pendant, facing, 0, 0, 1);
        Direction out = facing.getCounterClockWise();

        if (!variant.startsWith("arm")) {
            BlockPos funnel = outHatch.relative(out);
            level.setBlock(funnel, AllBlocks.ANDESITE_FUNNEL.getDefaultState().setValue(FunnelBlock.FACING, out).setValue(FunnelBlock.EXTRACTING, true), Block.UPDATE_ALL);
            BlockPos catcher = funnel.below();
            level.setBlock(catcher, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN), Block.UPDATE_ALL);
            level.setBlock(catcher.below(), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        }

        switch (variant) {
            case "hopper" -> {
                level.setBlock(roof, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN), Block.UPDATE_ALL);
                chest(level, roof.above(), facing);
            }
            case "chute" -> {
                level.setBlock(roof, AllBlocks.CHUTE.getDefaultState(), Block.UPDATE_ALL);
                level.setBlock(roof.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN), Block.UPDATE_ALL);
                chest(level, roof.above(2), facing);
            }
            case "funnel" -> {
                level.setBlock(roof, AllBlocks.ANDESITE_FUNNEL.getDefaultState().setValue(FunnelBlock.FACING, Direction.UP), Block.UPDATE_ALL);
                for (ItemStack stack : parts()) {
                    ItemEntity item = new ItemEntity(level, roof.getX() + .5, roof.getY() + 1.2, roof.getZ() + .5, stack);
                    item.setDeltaMovement(0, 0, 0);
                    level.addFreshEntity(item);
                }
            }
            default -> arm(level, pendant, facing, variant.equals("arm_front"));
        }
    }

    private static List<ItemStack> parts() {
        return List.of(new ItemStack(AllBlocks.SHAFT.get(), 16), new ItemStack(Items.OAK_PLANKS, 16));
    }

    private static void chest(ServerLevel level, BlockPos pos, Direction facing) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), Block.UPDATE_ALL);
        IItemHandler chest = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.DOWN);
        if (chest != null)
            for (ItemStack stack : parts())
                insert(chest, stack);
    }

    private static void insert(IItemHandler handler, ItemStack stack) {
        for (int i = 0; i < handler.getSlots() && !stack.isEmpty(); i++)
            stack = handler.insertItem(i, stack, false);
    }

    private static void arm(ServerLevel level, BlockPos pendant, Direction facing, boolean front) {
        BlockPos feeder = CncStructure.at(pendant, facing, 1, 0, 0).relative(facing, 3);
        BlockPos emptier = feeder.relative(facing.getCounterClockWise(), 3);
        BlockPos shafts = feeder.relative(facing.getClockWise());
        BlockPos planks = feeder.relative(facing);
        BlockPos done = emptier.relative(facing);
        BlockPos in = front ? CncStructure.at(pendant, facing, 1, 1, 0) : CncStructure.at(pendant, facing, 1, 2, 1);
        BlockPos out = front ? in : CncStructure.at(pendant, facing, 0, 0, 1);

        for (BlockPos depot : List.of(shafts, planks, done))
            level.setBlock(depot, AllBlocks.DEPOT.getDefaultState(), Block.UPDATE_ALL);
        List<ItemStack> parts = parts();
        if (level.getBlockEntity(shafts) instanceof DepotBlockEntity depot)
            depot.setHeldItem(parts.get(0));
        if (level.getBlockEntity(planks) instanceof DepotBlockEntity depot)
            depot.setHeldItem(parts.get(1));

        ListTag feeding = new ListTag();
        feeding.add(point(feeder, shafts, "create:depot", "TAKE"));
        feeding.add(point(feeder, planks, "create:depot", "TAKE"));
        feeding.add(point(feeder, in, Tracer.MODID + ":cnc", "DEPOSIT"));
        placeArm(level, feeder, facing, feeding);
        ListTag emptying = new ListTag();
        emptying.add(point(emptier, out, Tracer.MODID + ":cnc", "TAKE"));
        emptying.add(point(emptier, done, "create:depot", "DEPOSIT"));
        placeArm(level, emptier, facing, emptying);
    }

    private static void placeArm(ServerLevel level, BlockPos arm, Direction facing, ListTag points) {
        BlockState armState = AllBlocks.MECHANICAL_ARM.getDefaultState();
        level.setBlock(arm, armState, Block.UPDATE_ALL);
        BlockEntity placed = level.getBlockEntity(arm);
        if (placed != null) {
            CompoundTag tag = placed.saveWithFullMetadata(level.registryAccess());
            tag.put("InteractionPoints", points);
            level.removeBlockEntity(arm);
            BlockEntity loaded = BlockEntity.loadStatic(arm, armState, tag, level.registryAccess());
            if (loaded != null)
                level.setBlockEntity(loaded);
        }
        BlockPos cog = arm.relative(facing.getOpposite());
        level.setBlock(cog, AllBlocks.COGWHEEL.getDefaultState(), Block.UPDATE_ALL);
        BlockPos motor = cog.below();
        level.setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.UP), Block.UPDATE_ALL);
        if (level.getBlockEntity(motor) instanceof CreativeMotorBlockEntity be)
            be.generatedSpeed.setValue(64);
    }

    private static CompoundTag point(BlockPos arm, BlockPos at, String type, String mode) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", type);
        tag.put("Pos", NbtUtils.writeBlockPos(at.subtract(arm)));
        tag.putString("Mode", mode);
        return tag;
    }
}
