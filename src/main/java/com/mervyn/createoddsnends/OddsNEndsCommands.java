package com.mervyn.createoddsnends;

import com.mervyn.createoddsnends.jacquard.JacquardCommands;
import com.mervyn.createoddsnends.organ.OrganCommands;

import net.minecraft.commands.Commands;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// debug only, every feature hangs its commands off /oddsnends
public final class OddsNEndsCommands {
    private OddsNEndsCommands() {}

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("oddsnends")
                .requires(source -> source.hasPermission(2))
                .then(TracerCommands.node(event.getBuildContext()))
                .then(JacquardCommands.node())
                .then(OrganCommands.node()));
    }
}
