package dev.ataflore.catchtheblast.config;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.CommandDispatcher;

public final class BlastCommands {
    private BlastCommands() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("catchtheblast")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("terrain")
                .executes(ctx -> report(ctx.getSource(), false))
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(ctx -> {
                        BlastConfig.setTerrain(BoolArgumentType.getBool(ctx, "enabled"));
                        return report(ctx.getSource(), true);
                    }))));
    }
    private static int report(CommandSourceStack source, boolean broadcast) {
        boolean enabled = BlastConfig.terrainDamage();
        source.sendSuccess(() -> Component.translatable(enabled ? "command.catchtheblast.terrain.enabled" : "command.catchtheblast.terrain.disabled"), broadcast);
        return enabled ? 1 : 0;
    }
}
