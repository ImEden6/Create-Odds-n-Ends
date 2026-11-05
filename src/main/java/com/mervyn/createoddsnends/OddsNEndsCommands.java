package com.mervyn.createoddsnends;

import com.mervyn.createoddsnends.cnc.CncShowcase;
import com.mervyn.createoddsnends.cnc.CncStructure;
import com.mervyn.createoddsnends.jacquard.JacquardCommands;
import com.mervyn.createoddsnends.lathe.LatheStructure;
import com.mervyn.createoddsnends.organ.OrganCommands;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class OddsNEndsCommands {
    private OddsNEndsCommands() {}

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("oddsnends")
                .requires(source -> source.hasPermission(2))
                .then(TracerCommands.node(event.getBuildContext()))
                .then(JacquardCommands.node())
                .then(OrganCommands.node())
                .then(Commands.literal("showcase")
                        .executes(ctx -> {
                            showcase(ctx.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.literal("cnc")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    CncShowcase.all(player, player.blockPosition(), player.getDirection());
                                    return 1;
                                }))));
    }

    private static void showcase(ServerPlayer player) {
        BlockPos anchor = player.blockPosition();
        Direction ahead = player.getDirection();
        Direction left = ahead.getCounterClockWise();
        JacquardCommands.showcase(player, anchor, ahead);
        OrganCommands.showcase(player, anchor.relative(ahead, 5).relative(ahead.getClockWise(), 13), ahead.getOpposite());
        LatheStructure.showcase(player, anchor.relative(ahead, 4).relative(left, 12).above(), ahead.getClockWise());
        CncStructure.showcase(player, anchor.relative(ahead, 6).relative(left, 24).above(), ahead.getOpposite());
    }
}
