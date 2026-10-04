package com.mervyn.coolcreatestuff.jacquard;

import java.util.Arrays;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

// debug only: /coolstuffs jacquard ...
public final class JacquardCommands {
    private static final SimpleCommandExceptionType BAD_MOTIF = new SimpleCommandExceptionType(Component.literal("motif is kilim, medallion or damask"));
    private static final SimpleCommandExceptionType NOT_A_LOOM = new SimpleCommandExceptionType(Component.literal("look at a formed loom first"));
    private static final SimpleCommandExceptionType NO_DESIGN = new SimpleCommandExceptionType(Component.literal("no design with that id"));

    private JacquardCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("jacquard")
                .then(Commands.literal("design")
                        .then(Commands.argument("seed", IntegerArgumentType.integer(1))
                                .then(Commands.argument("w", IntegerArgumentType.integer(1, Design.MAX_WIDTH))
                                        .then(Commands.argument("h", IntegerArgumentType.integer(1, Design.MAX_HEIGHT))
                                                .then(Commands.argument("colours", IntegerArgumentType.integer(1, 6))
                                                        .then(Commands.argument("motif", StringArgumentType.word())
                                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(DesignGen.MOTIFS, builder))
                                                                .executes(JacquardCommands::design)))))))
                .then(Commands.literal("setdesign")
                        .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                .executes(JacquardCommands::setDesign)))
                .then(Commands.literal("showcase")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            showcase(player, player.blockPosition(), player.getDirection());
                            return 1;
                        }));
    }

    // Builds everything Jacquard adds in front of the player, powered and loaded with a seed 3
    // design: the loom straight ahead, the card punch to the left, a wall hanging and a rug to the
    // right. Used by the dev showcase run to take screenshots without anyone at the keyboard.
    public static void showcase(ServerPlayer player, BlockPos anchor, Direction ahead) {
        ServerLevel level = player.serverLevel();
        Direction front = ahead.getOpposite(), left = ahead.getCounterClockWise(), right = ahead.getClockWise();
        BlockPos.betweenClosed(anchor.relative(ahead, 2).relative(left, 8), anchor.relative(ahead, 9).relative(right, 8).above(5))
                .forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));

        Design design = DesignGen.generate(3, 2, 3, 5, "kilim");
        int id = Designs.get(player.server).add(design);

        BlockPos head = anchor.relative(ahead, 5).above(2);
        for (LoomStructure.Spot spot : LoomStructure.SPOTS) {
            if (spot.block() == null)
                continue;
            level.setBlock(LoomStructure.at(head, front, spot.x(), spot.y(), spot.z()), spot.block().get().defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(head, Jacquard.JACQUARD_HEAD.get().defaultBlockState().setValue(LoomPartBlock.FACING, front), Block.UPDATE_ALL);
        LoomStructure.wrench(level, head, player);
        if (level.getBlockEntity(head) instanceof JacquardHeadBlockEntity be)
            be.setDesignId(id);
        BlockPos drive = LoomStructure.at(head, front, 0, 0, 1);
        motor(level, drive, LoomDriveBlock.input(level.getBlockState(drive)));

        BlockPos punch = anchor.relative(ahead, 3).relative(left, 5);
        level.setBlock(punch, Jacquard.CARD_PUNCH.get().defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, front), Block.UPDATE_ALL);
        motor(level, punch, CardPunchBlock.input(level.getBlockState(punch)));

        BlockPos wall = anchor.relative(ahead, 7).relative(right, 4);
        for (int across = 0; across < 4; across++)
            for (int up = 0; up < 4; up++)
                level.setBlock(wall.relative(right, across).above(up), Blocks.STONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);
        hang(level, wall.relative(front), right, 3, 3, front, ahead, id, design);
        hang(level, anchor.relative(ahead, 3).relative(right, 4), right, 3, 3, Direction.UP, ahead, id, design);

        player.setGameMode(GameType.CREATIVE);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, player.server);
        for (ItemStack stack : new ItemStack[] {new ItemStack(Jacquard.PUNCHED_CARD_CHAIN.get()), new ItemStack(Jacquard.TAPESTRY.get())}) {
            stack.set(Jacquard.DESIGN.get(), id);
            player.getInventory().placeItemBackInInventory(stack);
        }
    }

    private static void motor(ServerLevel level, BlockPos machine, Direction input) {
        BlockPos pos = machine.relative(input);
        level.setBlock(pos, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, input.getOpposite()), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof CreativeMotorBlockEntity be)
            be.generatedSpeed.setValue(96);
    }

    // tries a few spots along the surface until the tapestry fits
    private static void hang(ServerLevel level, BlockPos start, Direction along, int spanAlong, int spanUp, Direction face, Direction top, int id, Design design) {
        for (int a = 0; a < spanAlong; a++)
            for (int u = 0; u < spanUp; u++) {
                BlockPos pos = start.relative(along, a).relative(face.getAxis().isHorizontal() ? Direction.UP : top, u);
                TapestryEntity tapestry = new TapestryEntity(level, pos, face, top, id, design);
                if (tapestry.survives()) {
                    level.addFreshEntity(tapestry);
                    return;
                }
            }
    }

    private static int design(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String motif = StringArgumentType.getString(ctx, "motif");
        if (!Arrays.asList(DesignGen.MOTIFS).contains(motif))
            throw BAD_MOTIF.create();
        Design design = DesignGen.generate(IntegerArgumentType.getInteger(ctx, "seed"), IntegerArgumentType.getInteger(ctx, "w"),
                IntegerArgumentType.getInteger(ctx, "h"), IntegerArgumentType.getInteger(ctx, "colours"), motif);
        int id = Designs.get(player.server).add(design);
        for (ItemStack stack : new ItemStack[] {new ItemStack(Jacquard.PUNCHED_CARD_CHAIN.get()), new ItemStack(Jacquard.TAPESTRY.get())}) {
            stack.set(Jacquard.DESIGN.get(), id);
            player.getInventory().placeItemBackInInventory(stack);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("made design " + id), false);
        return id;
    }

    private static int setDesign(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int id = IntegerArgumentType.getInteger(ctx, "id");
        if (Designs.get(player.server).get(id) == null)
            throw NO_DESIGN.create();
        HitResult hit = player.pick(player.blockInteractionRange(), 0, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() == HitResult.Type.MISS)
            throw NOT_A_LOOM.create();
        BlockState state = player.level().getBlockState(blockHit.getBlockPos());
        if (!state.hasProperty(LoomPartBlock.FORMED) || !state.getValue(LoomPartBlock.FORMED))
            throw NOT_A_LOOM.create();
        BlockPos head = LoomStructure.findHead(blockHit.getBlockPos(), state);
        if (!(player.level().getBlockEntity(head) instanceof JacquardHeadBlockEntity be))
            throw NOT_A_LOOM.create();
        be.setDesignId(id);
        ctx.getSource().sendSuccess(() -> Component.literal("loom shows design " + id), false);
        return 1;
    }
}
