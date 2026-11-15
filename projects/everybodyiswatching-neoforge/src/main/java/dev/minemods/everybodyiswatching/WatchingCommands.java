package dev.minemods.everybodyiswatching;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class WatchingCommands {
    private WatchingCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, WatchingEngine engine) {
        dispatcher.register(Commands.literal("watching")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("trigger").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    int count = engine.force(player);
                    context.getSource().sendSuccess(() -> Component.literal(count > 0
                            ? "Everybody Is Watching: gathering spectators=" + count
                            : "No eligible villagers within 48 blocks."), false);
                    return count > 0 ? 1 : 0;
                }))
                .then(Commands.literal("stare").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    int count = engine.forceStare(player);
                    context.getSource().sendSuccess(() -> Component.literal(count > 0
                            ? "Everybody Is Watching: immediate stare, spectators=" + count
                            : "No eligible villagers within 48 blocks."), false);
                    return count > 0 ? 1 : 0;
                }))
                .then(Commands.literal("status").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    context.getSource().sendSuccess(() -> Component.literal(engine.status(player)), false);
                    return 1;
                }))
                .then(Commands.literal("reset").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    engine.resetPlayer(player);
                    context.getSource().sendSuccess(() -> Component.literal(
                            "Village entry tracking reset. Next scan starts a new entry check."), false);
                    return 1;
                })));
    }
}
