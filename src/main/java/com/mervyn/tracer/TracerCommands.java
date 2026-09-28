package com.mervyn.tracer;

import com.mervyn.tracer.lathe.HeadstockBlockEntity;
import com.mervyn.tracer.lathe.LathePartBlock;
import com.mervyn.tracer.lathe.LatheStructure;
import com.mervyn.tracer.turned.TurnedBlockEntity;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.DataResult;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// debug only, for checking visuals before there's any real way to make a profile
public final class TracerCommands {
    private static final SimpleCommandExceptionType CREATIVE_ONLY = new SimpleCommandExceptionType(Component.literal("creative mode only"));
    private static final SimpleCommandExceptionType NO_TARGET = new SimpleCommandExceptionType(Component.literal("look at a block first"));
    private static final SimpleCommandExceptionType NOT_A_LATHE = new SimpleCommandExceptionType(Component.literal("that's not part of a formed lathe"));

    private TracerCommands() {}

    static void register(RegisterCommandsEvent event) {
        CommandBuildContext context = event.getBuildContext();
        event.getDispatcher().register(Commands.literal("tracer")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("preview")
                        .then(Commands.argument("radii", StringArgumentType.word())
                                .then(Commands.argument("block", BlockStateArgument.block(context))
                                        .executes(TracerCommands::preview))))
                .then(Commands.literal("lathe")
                        .then(Commands.argument("radii", StringArgumentType.word())
                                .then(Commands.argument("block", BlockStateArgument.block(context))
                                        .executes(TracerCommands::lathe)))));
    }

    private static int preview(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = creativePlayer(ctx);
        Profile profile = profile(ctx);
        Block source = BlockStateArgument.getBlock(ctx, "block").getState().getBlock();
        BlockHitResult hit = target(player);

        ServerLevel level = player.serverLevel();
        BlockPos pos = hit.getBlockPos();
        if (!level.getBlockState(pos).canBeReplaced())
            pos = pos.relative(hit.getDirection());

        level.setBlockAndUpdate(pos, Tracer.TURNED_BLOCK.get().defaultBlockState());
        if (level.getBlockEntity(pos) instanceof TurnedBlockEntity be)
            be.setShape(profile, source);
        BlockPos placed = pos;
        ctx.getSource().sendSuccess(() -> Component.literal("placed turned block at " + placed.toShortString()), false);
        return 1;
    }

    private static int lathe(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = creativePlayer(ctx);
        Profile profile = profile(ctx);
        Block source = BlockStateArgument.getBlock(ctx, "block").getState().getBlock();
        BlockHitResult hit = target(player);

        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(hit.getBlockPos());
        if (!state.hasProperty(LathePartBlock.FORMED) || !state.getValue(LathePartBlock.FORMED))
            throw NOT_A_LATHE.create();
        BlockPos controller = LatheStructure.findController(level, hit.getBlockPos(), state);
        if (controller == null || !(level.getBlockEntity(controller) instanceof HeadstockBlockEntity be))
            throw NOT_A_LATHE.create();

        be.setShape(profile, source);
        ctx.getSource().sendSuccess(() -> Component.literal("lathe at " + controller.toShortString() + " now shows that profile"), false);
        return 1;
    }

    private static ServerPlayer creativePlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!player.isCreative())
            throw CREATIVE_ONLY.create();
        return player;
    }

    private static Profile profile(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        DataResult<Profile> result = Profile.parse(StringArgumentType.getString(ctx, "radii"));
        if (result.isError())
            throw new SimpleCommandExceptionType(Component.literal(result.error().get().message())).create();
        return result.getOrThrow();
    }

    private static BlockHitResult target(ServerPlayer player) throws CommandSyntaxException {
        HitResult hit = player.pick(player.blockInteractionRange(), 0, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() == HitResult.Type.MISS)
            throw NO_TARGET.create();
        return blockHit;
    }
}
