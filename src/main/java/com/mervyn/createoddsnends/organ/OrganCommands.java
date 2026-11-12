package com.mervyn.createoddsnends.organ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;

// debug only: /oddsnends organ score <seed> <style> <bars> <voices> <transpose>
public final class OrganCommands {
    private static final SimpleCommandExceptionType BAD_STYLE = new SimpleCommandExceptionType(Component.literal("style is march, waltz or carousel"));

    private OrganCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("organ")
                .then(Commands.literal("score")
                        .then(Commands.argument("seed", IntegerArgumentType.integer(1))
                                .then(Commands.argument("style", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ScoreGen.STYLES, builder))
                                        .then(Commands.argument("bars", IntegerArgumentType.integer(1, 64))
                                                .then(Commands.argument("voices", IntegerArgumentType.integer(1, 4))
                                                        .then(Commands.argument("transpose", IntegerArgumentType.integer(-12, 12))
                                                                .executes(OrganCommands::score)))))))
                .then(Commands.literal("birthday").executes(OrganCommands::birthday));
    }

    // happy birthday in C, 3/4 at four rows a beat: melody on blue, bass on green, chords on red,
    // drums on black
    private static final int[][] TUNE = {
            {0, 67, 3}, {3, 67, 1}, {4, 69, 4}, {8, 67, 4}, {12, 72, 4}, {16, 71, 8}, {24, 67, 3}, {27, 67, 1},
            {28, 69, 4}, {32, 67, 4}, {36, 74, 4}, {40, 72, 8}, {48, 67, 3}, {51, 67, 1}, {52, 79, 4}, {56, 76, 4},
            {60, 72, 4}, {64, 71, 4}, {68, 69, 4}, {72, 77, 3}, {75, 77, 1}, {76, 76, 4}, {80, 72, 4}, {84, 74, 4}, {88, 72, 12}};
    private static final int[][] BARS = {{48, 52, 55, 60}, {43, 50, 55, 59}, {43, 50, 55, 59}, {48, 52, 55, 60},
            {48, 52, 55, 60}, {41, 53, 57, 60}, {43, 50, 55, 59}, {48, 52, 55, 60}};

    private static Score birthdayScore() {
        List<Score.Note> notes = new ArrayList<>();
        for (int[] n : TUNE)
            notes.add(new Score.Note(n[0], n[1] - Score.LOW_MIDI, n[2], 11));
        for (int b = 0; b < BARS.length; b++) {
            int r = 4 + b * 12;
            boolean last = b == BARS.length - 1;
            notes.add(new Score.Note(r, BARS[b][0] - Score.LOW_MIDI, last ? 12 : 4, 13));
            notes.add(new Score.Note(r, 0, 1, 15));
            for (int beat = 1; beat < (last ? 2 : 3); beat++) {
                notes.add(new Score.Note(r + beat * 4, 1, 1, 15));
                for (int k = 1; k < 4; k++)
                    notes.add(new Score.Note(r + beat * 4, BARS[b][k] - Score.LOW_MIDI, last ? 8 : 3, 14));
            }
        }
        return new Score(100, 12, 0, notes);
    }

    private static int birthday(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack book = new ItemStack(Organ.BOOK_MUSIC.get());
        book.set(Organ.SCORE.get(), Scores.get(player.server).add(birthdayScore()));
        player.getInventory().placeItemBackInInventory(book);
        return 1;
    }

    private static int score(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String style = StringArgumentType.getString(ctx, "style");
        if (!Arrays.asList(ScoreGen.STYLES).contains(style))
            throw BAD_STYLE.create();
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Score score = ScoreGen.generate(IntegerArgumentType.getInteger(ctx, "seed"), style, IntegerArgumentType.getInteger(ctx, "bars"),
                IntegerArgumentType.getInteger(ctx, "voices"), IntegerArgumentType.getInteger(ctx, "transpose"));
        int id = Scores.get(player.server).add(score);
        ItemStack book = new ItemStack(Organ.BOOK_MUSIC.get());
        book.set(Organ.SCORE.get(), id);
        player.getInventory().placeItemBackInInventory(book);
        ctx.getSource().sendSuccess(() -> Component.literal("score " + id + ": " + score.rowCount() + " rows, " + score.notes().size() + " notes"), false);
        return id;
    }

    // dev showcase: a looping organ facing the camera, fed by a motor at the back, with a wind line
    // out to twelve chests, the front row carrying the five instruments, the rest whistles
    private static final List<DeferredBlock<InstrumentBlock>> INSTRUMENTS = List.of(Organ.REED_PIPE, Organ.WOOD_PIPE, Organ.REED_BOX, Organ.KICK_DRUM, Organ.SNARE_DRUM);

    public static void showcase(ServerPlayer player, BlockPos at, Direction front) {
        ServerLevel level = player.serverLevel();
        Direction back = front.getOpposite(), left = front.getClockWise(), right = front.getCounterClockWise();
        BlockPos.betweenClosed(at.relative(front, 3).relative(left, 4), at.relative(back, 4).relative(right, 11).above(5))
                .forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));

        level.setBlock(at, Organ.BARREL_ORGAN.get().defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, front), Block.UPDATE_ALL);
        level.setBlock(at.above(), Organ.BARREL_ORGAN_TOP.get().defaultBlockState().setValue(BarrelOrganTopBlock.FACING, front), Block.UPDATE_ALL);
        if (!Boolean.getBoolean("createoddsnends.nocabinet")) {
            BlockPos motor = at.relative(back);
            level.setBlock(motor, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, front), Block.UPDATE_ALL);
            if (level.getBlockEntity(motor) instanceof CreativeMotorBlockEntity be)
                be.generatedSpeed.setValue(96);
        }

        BlockState line = Organ.WIND_LINE.get().defaultBlockState();
        for (int i = 1; i <= 9; i++) {
            BlockPos pos = at.relative(right, i);
            level.setBlock(pos, line.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(left), true).setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(right), i < 9)
                    .setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(front), i > 3).setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(back), i > 3), Block.UPDATE_ALL);
        }
        for (int pc = 0; pc < 12; pc++) {
            BlockPos chest = at.relative(right, 4 + pc % 6).relative(pc < 6 ? back : front);
            level.setBlock(chest, Organ.WIND_CHEST.get().defaultBlockState().setValue(WindChestBlock.FACING, front), Block.UPDATE_ALL);
            if (pc >= 6 && pc < 9 && level.getBlockEntity(chest) instanceof WindChestBlockEntity be)
                be.voice.setFilter(new ItemStack(pc == 6 ? Items.BLUE_DYE : pc == 7 ? Items.GREEN_DYE : Items.RED_DYE));
            if (pc >= 6 && pc < 11) {
                level.setBlock(chest.above(), INSTRUMENTS.get(pc - 6).get().defaultBlockState().setValue(InstrumentBlock.FACING, front), Block.UPDATE_ALL);
                continue;
            }
            level.setBlock(chest.above(), AllBlocks.STEAM_WHISTLE.getDefaultState().setValue(WhistleBlock.FACING, front), Block.UPDATE_ALL);
            for (int grow = 0; grow < (11 - pc) / 2; grow++)
                WhistleBlock.incrementSize(level, chest.above());
        }

        Score score = Boolean.getBoolean("createoddsnends.birthday") ? birthdayScore() : ScoreGen.generate(5, "march", 8, 4, 0);
        ItemStack book = new ItemStack(Organ.BOOK_MUSIC.get());
        book.set(Organ.SCORE.get(), Scores.get(player.server).add(score));
        if (level.getBlockEntity(at) instanceof BarrelOrganBlockEntity organ) {
            organ.book.setStackInSlot(0, book);
            organ.mode.setValue(PlayMode.LOOP.ordinal());
        }
    }
}
