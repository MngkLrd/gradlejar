package dev.minemods.lastwitness.guard;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Operator-only diagnostics; does not summon entities or edit the world. */
public final class GuardCommands {
    private GuardCommands() { }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("lastwitness")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("guards").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    String message = GuardPatrols.diagnostics(player);
                    ctx.getSource().sendSuccess(() -> Component.literal(message), false);
                    return 1;
                })));
    }
}
