package com.mervyn.coolcreatestuff;

import net.minecraft.commands.Commands;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// debug only, every feature hangs its commands off /coolstuffs
public final class CoolStuffsCommands {
    private CoolStuffsCommands() {}

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("coolstuffs")
                .requires(source -> source.hasPermission(2))
                .then(TracerCommands.node(event.getBuildContext())));
    }
}
