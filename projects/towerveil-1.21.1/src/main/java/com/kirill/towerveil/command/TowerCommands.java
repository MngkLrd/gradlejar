package com.kirill.towerveil.command;

import com.kirill.towerveil.gameplay.FarmlandTransition;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TowerCommands {
    private TowerCommands() { }

    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("towerveil")
                .then(Commands.literal("return").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    boolean success = FarmlandTransition.returnPlayer(player);
                    if (!success) player.displayClientMessage(
                            Component.literal("Tower Veil: no saved return point or you are not in the field."), true);
                    return success ? 1 : 0;
                })));
    }
}
